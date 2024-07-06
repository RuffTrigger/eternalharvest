package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class PlantGrowthManager {

    private static PlantGrowthManager instance;
    private JavaPlugin plugin;
    private Connection connection;
    private Map<Location, Plant> plants = new HashMap<>();
    private Map<Material, Integer> growthTimes = new HashMap<>();

    private PlantGrowthManager() {}

    public static PlantGrowthManager getInstance() {
        if (instance == null) {
            instance = new PlantGrowthManager();
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
        createDatabaseTable();
        loadGrowthTimesFromConfig();
        loadAllPlantData();
    }

    private void createDatabaseTable() {
        String sql = "CREATE TABLE IF NOT EXISTS plant_growth (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "growth_stage INTEGER NOT NULL, " +
                "last_updated LONG NOT NULL, " +
                "last_unloaded LONG NOT NULL)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
            plugin.getLogger().info("Created plant_growth table if not exists.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create plant_growth table", e);
        }
    }

    private void loadGrowthTimesFromConfig() {
        growthTimes.clear(); // Clear existing growth times to reload from config
        for (String key : plugin.getConfig().getConfigurationSection("growth-times").getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase());
            if (material != null) {
                growthTimes.put(material, plugin.getConfig().getInt("growth-times." + key));
            }
        }
        plugin.getLogger().info("Loaded growth times from config: " + growthTimes);
    }

    private void loadAllPlantData() {
        plants.clear(); // Clear existing plants to reload from database
        String sql = "SELECT * FROM plant_growth";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String locationStr = rs.getString("location");
                int growthStage = rs.getInt("growth_stage");
                long lastUpdated = rs.getLong("last_updated");
                long lastUnloaded = rs.getLong("last_unloaded");

                Location location = stringToLocation(locationStr);
                Plant plant = new Plant(id, type, location, growthStage, lastUpdated, lastUnloaded);
                plants.put(location, plant);
            }
            plugin.getLogger().info("Loaded " + plants.size() + " plants from database.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load plant data from database", e);
        }
    }

    public void updatePlantGrowth(Location location) {
        long currentTime = System.currentTimeMillis();
        Plant plant = plants.get(location);
        if (plant != null) {
            long elapsedTime = (currentTime - plant.getLastUnloaded()) / 1000; // convert to seconds
            Material material = Material.valueOf(plant.getType());
            int averageGrowthTime = growthTimes.getOrDefault(material, 1200); // Default 20 minutes
            int growthStages = (int) (elapsedTime / (averageGrowthTime / 7)); // 7 stages for crops

            if (growthStages > 0) {
                Block block = location.getBlock();
                if (block.getType() == material) {
                    // Update the plant's growth stage
                    BlockState state = block.getState();
                    if (state.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) state.getBlockData();
                        int newGrowthStage = Math.min(plant.getGrowthStage() + growthStages, ageable.getMaximumAge());
                        ageable.setAge(newGrowthStage);
                        state.setBlockData(ageable);
                        state.update(true);
                        plant.setGrowthStage(newGrowthStage);
                        plant.setLastUpdated(currentTime);
                        updatePlantInDatabase(plant);
                        plugin.getLogger().info("Plant at " + location + " updated to growth stage " + newGrowthStage);
                    }
                }
            }
        } else {
            plugin.getLogger().warning("Plant not found at location " + location);
        }
    }

    private void updatePlantInDatabase(Plant plant) {
        String sql = "UPDATE plant_growth SET growth_stage = ?, last_updated = ?, last_unloaded = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, plant.getGrowthStage());
            stmt.setLong(2, plant.getLastUpdated());
            stmt.setLong(3, plant.getLastUnloaded());
            stmt.setInt(4, plant.getId());
            stmt.executeUpdate();
            plugin.getLogger().info("Updated plant in database with ID " + plant.getId());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update plant in database", e);
        }
    }

    public void saveAllPlantData() {
        for (Plant plant : plants.values()) {
            updatePlantInDatabase(plant);
        }
        plugin.getLogger().info("Saved all plant data to database.");
    }

    public void addPlant(Plant plant) {
        plants.put(plant.getLocation(), plant);
        insertPlantIntoDatabase(plant);
        plugin.getLogger().info("Added plant to manager and database.");
    }

    private void insertPlantIntoDatabase(Plant plant) {
        String sql = "INSERT INTO plant_growth (type, location, growth_stage, last_updated, last_unloaded) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, plant.getType());
            stmt.setString(2, locationToString(plant.getLocation()));
            stmt.setInt(3, plant.getGrowthStage());
            stmt.setLong(4, plant.getLastUpdated());
            stmt.setLong(5, plant.getLastUnloaded());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    plant.setId(generatedKeys.getInt(1));
                }
            }
            plugin.getLogger().info("Inserted new plant into database with ID " + plant.getId());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert plant into database", e);
        }
    }

    private Location stringToLocation(String str) {
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
    }

    public void setLastUnloaded(Location location, long time) {
        Plant plant = plants.get(location);
        if (plant != null) {
            plant.setLastUnloaded(time);
            updatePlantInDatabase(plant);
            plugin.getLogger().info("Updated last unloaded time for plant at " + location + " to " + time);
        } else {
            plugin.getLogger().warning("Plant not found at location " + location);
        }
    }
}

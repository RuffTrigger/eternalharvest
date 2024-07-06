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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadGrowthTimesFromConfig() {
        for (String key : plugin.getConfig().getConfigurationSection("growth-times").getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase());
            if (material != null) {
                growthTimes.put(material, plugin.getConfig().getInt("growth-times." + key));
            }
        }
    }

    private void loadAllPlantData() {
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
        } catch (SQLException e) {
            e.printStackTrace();
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
                Block block = plant.getLocation().getBlock();
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
                    }
                }
            }
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAllPlantData() {
        for (Plant plant : plants.values()) {
            updatePlantInDatabase(plant);
        }
    }

    public void addPlant(Plant plant) {
        plants.put(plant.getLocation(), plant);
        insertPlantIntoDatabase(plant);
    }

    public void removePlant(Location location) {
        Plant removedPlant = plants.remove(location);
        if (removedPlant != null) {
            deletePlantFromDatabase(removedPlant);
        }
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

            plugin.getLogger().info("Inserted plant data for plant at " + plant.getLocation());
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to insert plant data for plant at " + plant.getLocation());
        }
    }

    private void deletePlantFromDatabase(Plant plant) {
        String sql = "DELETE FROM plant_growth WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, plant.getId());
            stmt.executeUpdate();
            plugin.getLogger().info("Deleted plant data for plant at " + plant.getLocation());
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to delete plant data for plant at " + plant.getLocation());
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
        }
    }
}

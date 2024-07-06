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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            Plant plant = plants.get(location);
            if (plant != null) {
                long elapsedTime = (currentTime - plant.getLastUpdated()) / 1000; // convert to seconds
                Material material = Material.matchMaterial(plant.getType());
                int growthTime = growthTimes.getOrDefault(material, 0);

                // Update growth stage if enough time has passed
                if (growthTime > 0) {
                    int newGrowthStage = plant.getGrowthStage() + (int) (elapsedTime / growthTime);
                    plant.setGrowthStage(newGrowthStage);

                    // Update block state if applicable
                    Block block = location.getBlock();
                    if (block.getType() == material && block.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) block.getBlockData();
                        ageable.setAge(Math.min(newGrowthStage, ageable.getMaximumAge()));
                        block.setBlockData(ageable);
                    }

                    plant.setLastUpdated(currentTime);
                    savePlantData(plant);
                }
            }
        });
    }

    public void addPlant(Plant plant) {
        plants.put(plant.getLocation(), plant);
        savePlantData(plant);
    }

    public void removePlant(Location location) {
        Plant plant = plants.remove(location);
        if (plant != null) {
            deletePlantData(plant);
        }
    }

    public void setLastUnloaded(Location location, long unloadTime) {
        Plant plant = plants.get(location);
        if (plant != null) {
            plant.setLastUnloaded(unloadTime);
            updatePlantData(plant);
        }
    }

    public void saveAllPlantData() {
        for (Plant plant : plants.values()) {
            savePlantData(plant);
        }
    }

    private void savePlantData(Plant plant) {
        String sql = "REPLACE INTO plant_growth (id, type, location, growth_stage, last_updated, last_unloaded) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, plant.getId());
            stmt.setString(2, plant.getType());
            stmt.setString(3, locationToString(plant.getLocation()));
            stmt.setInt(4, plant.getGrowthStage());
            stmt.setLong(5, plant.getLastUpdated());
            stmt.setLong(6, plant.getLastUnloaded());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updatePlantData(Plant plant) {
        String sql = "UPDATE plant_growth SET growth_stage = ?, last_updated = ?, last_unloaded = ? " +
                "WHERE id = ?";

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

    private void deletePlantData(Plant plant) {
        String sql = "DELETE FROM plant_growth WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, plant.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Location stringToLocation(String locationStr) {
        String[] parts = locationStr.split(",");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        return new Location(plugin.getServer().getWorld(parts[3]), x, y, z);
    }

    private String locationToString(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getWorld().getName();
    }
}

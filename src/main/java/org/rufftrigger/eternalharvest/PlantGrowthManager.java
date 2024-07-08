package org.rufftrigger.eternalharvest;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PlantGrowthManager {
    private static PlantGrowthManager instance = null;
    private Map<Integer, Plant> plantMap;
    private Logger logger;

    private PlantGrowthManager() {
        plantMap = new HashMap<>();
    }

    public static synchronized PlantGrowthManager getInstance() {
        if (instance == null) {
            instance = new PlantGrowthManager();
        }
        return instance;
    }

    public void initialize(Main plugin, Connection connection) {
        this.logger = plugin.getLogger();
        loadAllPlantData(connection);
    }

    public void addPlant(Plant plant) {
        plantMap.put(plant.getId(), plant);
        logger.info("Plant added to PlantGrowthManager: " + plant);
    }

    public Plant getPlant(int id) {
        return plantMap.get(id);
    }

    public void savePlantData(Plant plant, Connection connection) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT OR REPLACE INTO plants (id, type, world, x, y, z, growth_stage, last_updated, last_unloaded) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setInt(1, plant.getId());
                    stmt.setString(2, plant.getType());
                    stmt.setString(3, plant.getLocation().getWorld().getName());
                    stmt.setDouble(4, plant.getLocation().getX());
                    stmt.setDouble(5, plant.getLocation().getY());
                    stmt.setDouble(6, plant.getLocation().getZ());
                    stmt.setInt(7, plant.getGrowthStage());
                    stmt.setLong(8, plant.getLastUpdated());
                    stmt.setLong(9, plant.getLastUnloaded());
                    stmt.executeUpdate();
                    logger.info("Saved plant data: " + plant);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
    }

    public void saveAllPlantData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Plant plant : plantMap.values()) {
                    savePlantData(plant, Main.getPlugin(Main.class).getConnection());
                }
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
    }

    public void loadAllPlantData(Connection connection) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM plants");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String type = rs.getString("type");
                        String world = rs.getString("world");
                        double x = rs.getDouble("x");
                        double y = rs.getDouble("y");
                        double z = rs.getDouble("z");
                        int growthStage = rs.getInt("growth_stage");
                        long lastUpdated = rs.getLong("last_updated");
                        long lastUnloaded = rs.getLong("last_unloaded");

                        Plant plant = new Plant(id, type, new Location(Main.getPlugin(Main.class).getServer().getWorld(world), x, y, z), growthStage, lastUpdated, lastUnloaded);
                        plantMap.put(id, plant);
                        logger.info("Loaded plant: " + plant);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
    }

    public int getCurrentMaxId(Connection connection) {
        final int[] maxId = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT MAX(id) AS max_id FROM plants");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        maxId[0] = rs.getInt("max_id");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
        return maxId[0];
    }

    public void loadPlantsInChunk(Chunk chunk, Connection connection) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM plants WHERE world = ? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?")) {
                    stmt.setString(1, chunk.getWorld().getName());
                    stmt.setInt(2, chunk.getX() * 16);
                    stmt.setInt(3, chunk.getX() * 16 + 15);
                    stmt.setInt(4, chunk.getZ() * 16);
                    stmt.setInt(5, chunk.getZ() * 16 + 15);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String type = rs.getString("type");
                            double x = rs.getDouble("x");
                            double y = rs.getDouble("y");
                            double z = rs.getDouble("z");
                            int growthStage = rs.getInt("growth_stage");
                            long lastUpdated = rs.getLong("last_updated");
                            long lastUnloaded = rs.getLong("last_unloaded");

                            Location location = new Location(chunk.getWorld(), x, y, z);
                            Plant plant = new Plant(id, type, location, growthStage, lastUpdated, lastUnloaded);
                            plantMap.put(id, plant);
                            logger.info("Loaded plant from chunk: " + plant);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getPlugin(Main.class));
    }
}

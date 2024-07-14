package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private Connection connection;
    private Logger logger;

    public DatabaseManager() {
        this.logger = Main.getInstance().getLogger();
    }

    public void setupDatabase() {
        try {
            File dataFolder = Main.getInstance().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "plant_growth.db");
            if (!dbFile.exists()) {
                createNewDatabase(dbFile);
            }

            // Connect to the database
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Create table if not exists
            String createTableSQL = "CREATE TABLE IF NOT EXISTS plant_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "location TEXT," +
                    "material TEXT," +
                    "growth_time INTEGER," +
                    "plant_timestamp INTEGER," +
                    "growth_progress INTEGER DEFAULT 0," +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
                createTableStatement.executeUpdate();
            }

            logger.info("Database setup completed.");
        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Error setting up database.", e);
        }
    }

    private void createNewDatabase(File dbFile) throws IOException {
        try {
            if (dbFile.createNewFile()) {
                logger.info("Database file created: " + dbFile.getAbsolutePath());
            } else {
                throw new IOException("Failed to create database file.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating database file.", e);
            throw e;
        }
    }

    public void recordPlanting(final Location location, final Material material, final int growthTime) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement insertStatement = connection.prepareStatement(
                            "INSERT INTO plant_data (location, material, growth_time, plant_timestamp) VALUES (?, ?, ?, ?);"
                    );
                    insertStatement.setString(1, location.toString());
                    insertStatement.setString(2, material.toString());
                    insertStatement.setInt(3, growthTime);
                    insertStatement.setLong(4, System.currentTimeMillis() / 1000); // Store current time in seconds
                    insertStatement.executeUpdate();
                    insertStatement.close();
                    if (Main.getInstance().debug){
                        logger.info("Recorded planting: Material=" + material.toString() + ", Location=" + location.toString());
                    }

                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error recording planting.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void recordRemoval(final Location location, final Material material) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement deleteStatement = connection.prepareStatement(
                            "DELETE FROM plant_data WHERE location = ? AND material = ?;"
                    );
                    deleteStatement.setString(1, location.toString());
                    deleteStatement.setString(2, material.toString());
                    deleteStatement.executeUpdate();
                    deleteStatement.close();
                    if (Main.getInstance().debug){
                        logger.info("Recorded removal: Material=" + material.toString() + ", Location=" + location.toString());
                    }

                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error recording removal.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public List<PlantData> getAllPlants() {
        List<PlantData> plants = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM plant_data;");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                PlantData plant = new PlantData(
                        resultSet.getInt("id"),
                        resultSet.getString("location"),
                        Material.valueOf(resultSet.getString("material")),
                        resultSet.getInt("growth_time"),
                        resultSet.getLong("plant_timestamp"),
                        resultSet.getInt("growth_progress"),
                        resultSet.getTimestamp("last_updated").getTime()
                );
                plants.add(plant);
            }
            resultSet.close();
            statement.close();
            if (Main.getInstance().debug){
                logger.info("Retrieved " + plants.size() + " plants from database.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving plants from database.", e);
        }
        return plants;
    }

    public void updateGrowthProgress(int id, int growthProgress) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement updateStatement = connection.prepareStatement(
                            "UPDATE plant_data SET growth_progress = ?, last_updated = CURRENT_TIMESTAMP WHERE id = ?;"
                    );
                    updateStatement.setInt(1, growthProgress);
                    updateStatement.setInt(2, id);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                    if (Main.getInstance().debug){
                        logger.info("Updated growth progress for plant with ID=" + id + " to " + growthProgress + "%.");
                    }

                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error updating growth progress.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection.", e);
        }
    }

    public void maintainDatabase() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Query to find duplicates based on location
                    String query = "SELECT id, location FROM plant_data " +
                            "GROUP BY location " +
                            "HAVING COUNT(*) > 1";

                    PreparedStatement statement = connection.prepareStatement(query);
                    ResultSet resultSet = statement.executeQuery();

                    while (resultSet.next()) {
                        String location = resultSet.getString("location");
                        int idToKeep = -1;
                        long latestTimestamp = Long.MIN_VALUE;

                        // Find the latest entry among duplicates
                        PreparedStatement findDuplicatesStatement = connection.prepareStatement(
                                "SELECT id, plant_timestamp FROM plant_data WHERE location = ? ORDER BY plant_timestamp DESC LIMIT 1"
                        );
                        findDuplicatesStatement.setString(1, location);
                        ResultSet duplicatesResultSet = findDuplicatesStatement.executeQuery();

                        if (duplicatesResultSet.next()) {
                            idToKeep = duplicatesResultSet.getInt("id");
                            latestTimestamp = duplicatesResultSet.getLong("plant_timestamp");
                        }

                        duplicatesResultSet.close();
                        findDuplicatesStatement.close();

                        // Delete all except the latest entry
                        PreparedStatement deleteStatement = connection.prepareStatement(
                                "DELETE FROM plant_data WHERE location = ? AND id != ?"
                        );
                        deleteStatement.setString(1, location);
                        deleteStatement.setInt(2, idToKeep);
                        deleteStatement.executeUpdate();
                        deleteStatement.close();

                        if (Main.getInstance().debug) {
                            Main.getInstance().getLogger().info("Removed duplicates for location: " + location);
                        }
                    }

                    resultSet.close();
                    statement.close();
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error removing duplicates from database.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }
    public void VacuumDatabase() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement vacuumStatement = connection.prepareStatement("VACUUM;");
                    vacuumStatement.executeUpdate();
                    vacuumStatement.close();
                    logger.info("Database vacuumed to reduce file size.");
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error vacuuming database.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }
}

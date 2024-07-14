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
import java.util.ArrayList;
import java.util.List;
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
                    // Fetch all plant data from the database
                    List<PlantData> plants = getAllPlants();
                    for (PlantData plant : plants) {
                        Location location = LocationUtil.fromString(plant.getLocation());
                        if (location != null) {
                            Block block = location.getBlock();
                            if (block.getType() != plant.getMaterial() || !isAgeableSame(block, plant)) {
                                // If the block is not the same material or not the same ageable, remove the record
                                deletePlantDataById(plant.getId());
                                if (Main.getInstance().debug) {
                                    logger.info("Removed invalid plant data with ID=" + plant.getId() + " at location=" + plant.getLocation());
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error maintaining database.", e);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    private boolean isAgeableSame(Block block, PlantData plant) {
        if (!(block.getBlockData() instanceof Ageable)) {
            return true; // If the block is not ageable, it's considered the same for this check
        }
        Ageable ageable = (Ageable) block.getBlockData();
        int age = ageable.getAge();
        return age == plant.getGrowthProgress();
    }

    private void deletePlantDataById(int id) throws SQLException {
        PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM plant_data WHERE id = ?;");
        deleteStatement.setInt(1, id);
        deleteStatement.executeUpdate();
        deleteStatement.close();
    }
}

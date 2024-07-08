package org.rufftrigger.eternalharvest;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private Connection connection;
    private int currentMaxId;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Initialize the database connection asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                File databaseFile = new File(getDataFolder(), "plant_growth.db");

                // Check if the database file already exists
                if (!databaseFile.exists()) {
                    try {
                        if (databaseFile.createNewFile()) {
                            getLogger().info("Created new database file: " + databaseFile.getName());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        getLogger().severe("Failed to create database file: " + databaseFile.getName());
                        return;
                    }
                }

                // Connect to the database
                try {
                    String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
                    connection = DriverManager.getConnection(jdbcUrl);
                    getLogger().info("Database connected successfully.");
                } catch (SQLException e) {
                    e.printStackTrace();
                    getLogger().severe("Failed to connect to database.");
                    return;
                }

                // Ensure the database schema is up to date
                updateDatabaseSchema();

                // Retrieve the current maximum plant ID from the database
                currentMaxId = PlantGrowthManager.getInstance().getCurrentMaxId(connection);

                // Initialize PlantGrowthManager
                PlantGrowthManager.getInstance().initialize(Main.this, connection);

                // Register event listeners on the main thread
                getServer().getScheduler().runTask(Main.this, () -> {
                    getServer().getPluginManager().registerEvents(new PlantEventListener(Main.this), Main.this);
                    getServer().getPluginManager().registerEvents(new ChunkEventListener(Main.this), Main.this);
                    getLogger().info("EternalHarvest plugin has been enabled!");
                });

                // Start a task to check for updates from database periodically
                startUpdateTask();
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onDisable() {
        // Save all plant data before shutting down
        new BukkitRunnable() {
            @Override
            public void run() {
                PlantGrowthManager.getInstance().saveAllPlantData();
                getLogger().info("EternalHarvest has been disabled!");

                // Close the database connection
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                        getLogger().info("Database connection closed.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
    }

    public Connection getConnection() {
        return connection;
    }

    public synchronized int generateUniqueId() {
        return ++currentMaxId;
    }

    private void updateDatabaseSchema() {
        try {
            // Get database metadata
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if the 'plants' table exists
            boolean tableExists = false;
            ResultSet tables = metaData.getTables(null, null, "plants", null);
            if (tables.next()) {
                tableExists = true;
            }

            // If the 'plants' table doesn't exist, create it
            if (!tableExists) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("CREATE TABLE plants (" +
                            "id INTEGER PRIMARY KEY," +
                            "type TEXT NOT NULL," +
                            "world TEXT NOT NULL," +
                            "x DOUBLE NOT NULL," +
                            "y DOUBLE NOT NULL," +
                            "z DOUBLE NOT NULL," +
                            "growth_stage INTEGER NOT NULL," +
                            "last_updated LONG NOT NULL," +
                            "last_unloaded LONG NOT NULL" +
                            ")");
                    getLogger().info("Created 'plants' table in the database.");
                }
            }

            // Check if there's any schema update needed (e.g., adding columns for new features)
            // For simplicity, assume schema update only if new feature (e.g., ageable _SAPLING) is added later

            // Example: Adding a column for ageable saplings
            // Add this section if you need to update schema for new features

        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to update database schema: " + e.getMessage());
        }
    }

    public void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Query database for changes and update accordingly
                    String query = "SELECT id, growth_stage FROM plants WHERE last_updated > ?";
                    long lastCheckTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // Check changes in the last 24 hours

                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setLong(1, lastCheckTime);

                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int plantId = rs.getInt("id");
                                int newStage = rs.getInt("growth_stage");

                                // Update growth stage in PlantGrowthManager
                                PlantGrowthManager.getInstance().updateGrowthStage(plantId, newStage);
                                getLogger().info("Updated growth stage for plant ID " + plantId + " to " + newStage);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    getLogger().severe("Failed to query for plant updates: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L * 60 * 10); // Run every 10 minutes
    }
}

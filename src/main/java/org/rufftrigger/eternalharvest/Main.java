package org.rufftrigger.eternalharvest;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends JavaPlugin {

    private Connection connection;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Ensure plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Initialize database connection
        File databaseFile = new File(getDataFolder(), "plant_growth.db");

        // Check if database file exists, create if it doesn't
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

        // Connect to the SQLite database
        try {
            String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(jdbcUrl);
            getLogger().info("Database connected successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database.");
            return;
        }

        // Initialize ConfigManager, PlantGrowthManager, and register events
        ConfigManager configManager = new ConfigManager(this);
        PlantGrowthManager growthManager = PlantGrowthManager.getInstance();
        growthManager.initialize(this, connection);

        getServer().getPluginManager().registerEvents(new PlantEventListener(this, configManager, growthManager), this);

        // Start asynchronous task for growth updates
        int updateInterval = 20 * 60; // 20 ticks per second * 60 seconds = 1 minute
        new GrowthTask(this, configManager, growthManager).runTaskTimerAsynchronously(this, 0, updateInterval);

        getLogger().info("EternalHarvest plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save plant data before shutdown
        PlantGrowthManager.getInstance().saveAllPlantData();
        getLogger().info("EternalHarvest plugin has been disabled.");

        // Close database connection
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}

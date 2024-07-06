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
        // Ensure the plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Initialize the database connection
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

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlantEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkEventListener(this), this);
        getLogger().info("EternalHarvest plugin has been enabled!");

        // Initialize PlantGrowthManager
        PlantGrowthManager.getInstance().initialize(this, connection);
    }

    @Override
    public void onDisable() {
        // Save plant data before shutting down
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

    public Connection getConnection() {
        return connection;
    }
}

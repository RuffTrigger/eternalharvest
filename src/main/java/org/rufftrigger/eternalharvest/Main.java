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
        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File databaseFile = new File(getDataFolder(), "plant_growth.db");

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

        try {
            String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(jdbcUrl);
            getLogger().info("Database connected successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to database.");
            return;
        }

        getServer().getPluginManager().registerEvents(new PlantEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkEventListener(this), this);
        getLogger().info("EternalHarvest plugin has been enabled!");

        PlantGrowthManager.getInstance().initialize(this, connection);
    }

    @Override
    public void onDisable() {
        PlantGrowthManager.getInstance().saveAllPlantData();
        getLogger().info("EternalHarvest has been disabled!");

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

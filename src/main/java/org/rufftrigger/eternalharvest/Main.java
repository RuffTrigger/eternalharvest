package org.rufftrigger.eternalharvest;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends JavaPlugin {

    private Connection connection;

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Initialize the database connection
        try {
            // Replace "eternalharvest" with your desired folder name
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/eternalharvest/plant_growth.db");
            getLogger().info("Database connected successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlantEventListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkEventListener(this), this);
        getLogger().info("eternalharvest plugin has been enabled!");

        // Initialize PlantGrowthManager
        PlantGrowthManager.getInstance().initialize(this, connection);

    }

    @Override
    public void onDisable() {
        // Save plant data before shutting down
        PlantGrowthManager.getInstance().saveAllPlantData();
        getLogger().info("eternalharvest has been disabled!");

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

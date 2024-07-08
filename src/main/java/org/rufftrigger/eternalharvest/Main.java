package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static Main instance;
    private DatabaseManager databaseManager;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // Initialize database
        this.databaseManager = new DatabaseManager();
        this.databaseManager.setupDatabase(); // Ensure database schema is set up
        logger.info("Database initialized.");

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlantListener(databaseManager), this);
        logger.info("Event listeners registered.");

        // Start growth update task
        int updateIntervalSeconds = 300; // Update every 5 minutes
        new GrowthUpdateTask(databaseManager).runTaskTimerAsynchronously(this, 0, updateIntervalSeconds * 20); // Convert seconds to ticks
        logger.info("Growth update task started.");

        // Save default config if not exists
        this.saveDefaultConfig();
        logger.info("Configurations saved.");
    }

    @Override
    public void onDisable() {
        // Disable plugin logic here
        // Close database connection
        if (databaseManager != null) {
            databaseManager.closeConnection();
            logger.info("Database connection closed.");
        }

        logger.info("Plugin disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

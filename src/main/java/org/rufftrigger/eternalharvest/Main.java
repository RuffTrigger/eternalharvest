package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static Main instance;
    private DatabaseManager databaseManager;
    private Logger logger;
    private int updateIntervalSeconds;
    public boolean debug; // Variable to store debug mode status

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        // Save default config if not exists
        this.saveDefaultConfig();
        logger.info("Configurations saved.");

        // Load config values
        loadConfigValues();

        // Initialize database
        this.databaseManager = new DatabaseManager();
        this.databaseManager.setupDatabase();
        logger.info("Database initialized.");

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlantListener(databaseManager), this);
        logger.info("Event listeners registered.");

        // Start growth update task
        new GrowthUpdateTask(databaseManager).runTaskTimerAsynchronously(this, 0, updateIntervalSeconds * 20); // Convert seconds to ticks
        logger.info("Growth update task started with interval " + updateIntervalSeconds + " seconds.");
    }

    @Override
    public void onDisable() {
        // Close database connection
        if (databaseManager != null) {
            databaseManager.closeConnection();
            logger.info("Database connection closed.");
        }

        logger.info("Plugin disabled.");
    }

    private void loadConfigValues() {
        this.updateIntervalSeconds = getConfig().getInt("update-interval-seconds", 300); // Default to 300 seconds (5 minutes) if not specified
        this.debug = getConfig().getBoolean("debug", false); // Read debug mode value
        if (debug) {
            logger.info("Debug mode is enabled.");
        } else {
            logger.info("Debug mode is disabled.");
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

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
    private double beeHiveChance;
    private int minBeesPerHive;
    private int maxBeesPerHive;
    private int tallMangroveChange;
    private int maintenanceInterval;
    private int vacuumInterval;

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

        // Start maintenance task
        new MaintenanceTask(databaseManager).runTaskTimerAsynchronously(this, 0, maintenanceInterval * 20); // Convert seconds to ticks
        logger.info("Maintenance Task started with interval " + maintenanceInterval + " seconds.");

        // Start vacuumDatabase
        new VacuumDatabaseScheduler(databaseManager).runTaskTimerAsynchronously(this, 0, vacuumInterval * 20); // Convert seconds to ticks
        logger.info("Maintenance Task started with interval " + vacuumInterval + " seconds.");

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
        this.beeHiveChance = getConfig().getDouble("bee-hive-chance", 0.05); // Default to 5% chance if not specified
        this.minBeesPerHive = getConfig().getInt("min-bees-per-hive", 1); // Default to 1 bee if not specified
        this.maxBeesPerHive = getConfig().getInt("max-bees-per-hive", 3); // Default to 3 bees if not specified
        this.tallMangroveChange = getConfig().getInt("TALL_MANGROVE_CHANGE", 30); // Default will be set to 30
        this.maintenanceInterval = getConfig().getInt("maintenance-interval", 600); // Default to 5 minutes
        // Schedule database vacuuming task (e.g., every hour)
        this.vacuumInterval = getConfig().getInt("vacuum-interval", 72000); // Default to 1 hour (72000 ticks)

        if (debug) {
            logger.info("Debug mode is enabled.");
        } else {
            logger.info("Debug mode is disabled.");
        }
    }
    public double getBeeHiveChance() {
        return beeHiveChance;
    }

    public int getMinBeesPerHive() {
        return minBeesPerHive;
    }

    public int getMaxBeesPerHive() {
        return maxBeesPerHive;
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

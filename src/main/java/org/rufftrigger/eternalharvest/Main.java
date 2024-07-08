package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin {

    private static Main instance;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize database
        this.databaseManager = new DatabaseManager();
        this.databaseManager.setupDatabase(); // Ensure database schema is set up

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlantListener(databaseManager), this);

        // Start growth update task
        int updateIntervalSeconds = 300; // Update every 5 minutes
        new GrowthUpdateTask(databaseManager).runTaskTimerAsynchronously(this, 0, updateIntervalSeconds * 20); // Convert seconds to ticks

        // Save default config if not exists
        this.saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Disable plugin logic here
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static Main instance;
    private DatabaseManager databaseManager;
    private Logger logger;
    private int updateIntervalSeconds;
    public boolean debug;
    private double beeHiveChance;
    private int minBeesPerHive;
    private int maxBeesPerHive;
    private int tallMangroveChange;
    private int maintenanceInterval;
    private int vacuumInterval;
    private Map<Material, Integer> growthTimes = Collections.emptyMap();

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        saveDefaultConfig();
        logger.info("Configurations saved.");

        loadConfigValues();

        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        logger.info("Database initialized.");

        Bukkit.getPluginManager().registerEvents(new PlantListener(databaseManager), this);
        logger.info("Event listeners registered.");

        new GrowthUpdateTask(databaseManager).runTaskTimerAsynchronously(this, 0L, updateIntervalSeconds * 20L);
        logger.info("Growth update task started with interval " + updateIntervalSeconds + " seconds.");

        new MaintenanceTask(databaseManager).runTaskTimerAsynchronously(this, 0L, maintenanceInterval * 20L);
        logger.info("Maintenance Task started with interval " + maintenanceInterval + " seconds.");

        new VacuumDatabaseScheduler(databaseManager).runTaskTimerAsynchronously(this, 0L, vacuumInterval * 20L);
        logger.info("VacuumDatabaseScheduler started with interval " + vacuumInterval + " seconds.");

        logger.info("Plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
            logger.info("Database connection closed.");
        }

        logger.info("Plugin disabled.");
    }

    private void loadConfigValues() {
        updateIntervalSeconds = getConfig().getInt("update-interval-seconds", 300);
        debug = getConfig().getBoolean("debug", false);
        beeHiveChance = getConfig().getDouble("bee-hive-chance", 0.05);
        minBeesPerHive = getConfig().getInt("min-bees-per-hive", 1);
        maxBeesPerHive = getConfig().getInt("max-bees-per-hive", 3);
        tallMangroveChange = getConfig().getInt("TALL_MANGROVE_CHANGE", 30);
        maintenanceInterval = getConfig().getInt("maintenance-interval", 600);
        vacuumInterval = getConfig().getInt("vacuum-interval", 10800);
        growthTimes = loadGrowthTimes();

        if (debug) {
            logger.info("Debug mode is enabled.");
        } else {
            logger.info("Debug mode is disabled.");
        }
    }

    private Map<Material, Integer> loadGrowthTimes() {
        EnumMap<Material, Integer> loadedGrowthTimes = new EnumMap<>(Material.class);
        ConfigurationSection section = getConfig().getConfigurationSection("growth-times");

        if (section == null) {
            logger.warning("No growth-times section found in config.yml.");
            return Collections.emptyMap();
        }

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            }

            if (material == null) {
                logger.warning("Ignoring unknown material in growth-times: " + key);
                continue;
            }

            int growthTime = section.getInt(key, -1);
            if (growthTime <= 0) {
                logger.warning("Ignoring invalid growth time for " + key + ": " + growthTime);
                continue;
            }

            loadedGrowthTimes.put(material, growthTime);
        }

        logger.info("Loaded " + loadedGrowthTimes.size() + " plant growth time entries.");
        return Collections.unmodifiableMap(loadedGrowthTimes);
    }

    public int getGrowthTime(Material material) {
        return growthTimes.getOrDefault(material, -1);
    }

    public boolean isTrackedPlant(Material material) {
        return growthTimes.containsKey(material);
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

    public int GetTallMangroveChange() {
        return tallMangroveChange;
    }

    public static Main getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final DateTimeFormatter CONFIG_BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

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
    private boolean growthParticlesEnabled;
    private int growthParticleCount;
    private int randomGrowthOffsetSeconds;
    private Map<Material, Integer> growthTimes = Collections.emptyMap();

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        saveDefaultConfig();
        updateConfigIfOutdated();
        logger.info("Configurations loaded.");

        loadConfigValues();

        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        logger.info("Database initialized.");

        GrowthUpdateTask growthUpdateTask = new GrowthUpdateTask(databaseManager);
        Bukkit.getPluginManager().registerEvents(new PlantListener(databaseManager, growthUpdateTask), this);
        logger.info("Event listeners registered.");

        growthUpdateTask.runTaskTimerAsynchronously(this, 0L, updateIntervalSeconds * 20L);
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

    private void updateConfigIfOutdated() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
            reloadConfig();
            return;
        }

        FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
        int existingVersion = existingConfig.getInt("config-version", 0);
        if (existingVersion >= CURRENT_CONFIG_VERSION) {
            return;
        }

        File backupFile = new File(
                getDataFolder(),
                "config-v" + existingVersion + "-backup-" + LocalDateTime.now().format(CONFIG_BACKUP_TIMESTAMP) + ".yml"
        );

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Outdated config.yml detected. Backup created: " + backupFile.getName());

            saveResource("config.yml", true);
            reloadConfig();

            FileConfiguration updatedConfig = getConfig();
            restoreExistingConfigValues(existingConfig, updatedConfig, existingVersion);
            updatedConfig.set("config-version", CURRENT_CONFIG_VERSION);
            saveConfig();
            reloadConfig();

            logger.info("config.yml updated to version " + CURRENT_CONFIG_VERSION + " while preserving existing values.");
        } catch (IOException | IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Failed to update config.yml. Keeping existing config.", e);
            reloadConfig();
        }
    }

    private void restoreExistingConfigValues(FileConfiguration existingConfig, FileConfiguration updatedConfig, int existingVersion) {
        for (String path : existingConfig.getKeys(true)) {
            if (existingConfig.isConfigurationSection(path) || path.equals("config-version")) {
                continue;
            }

            if (path.equals("update-interval-seconds") && existingVersion == 0 && existingConfig.getInt(path) == 60) {
                continue;
            }

            updatedConfig.set(path, existingConfig.get(path));
        }
    }

    private void loadConfigValues() {
        updateIntervalSeconds = getConfig().getInt("update-interval-seconds", 15);
        debug = getConfig().getBoolean("debug", false);
        beeHiveChance = getConfig().getDouble("bee-hive-chance", 0.05);
        minBeesPerHive = getConfig().getInt("min-bees-per-hive", 1);
        maxBeesPerHive = getConfig().getInt("max-bees-per-hive", 3);
        tallMangroveChange = getConfig().getInt("TALL_MANGROVE_CHANGE", 30);
        maintenanceInterval = getConfig().getInt("maintenance-interval", 600);
        vacuumInterval = getConfig().getInt("vacuum-interval", 10800);
        growthParticlesEnabled = getConfig().getBoolean("growth-visuals.particles-on-growth", true);
        growthParticleCount = Math.max(0, getConfig().getInt("growth-visuals.particle-count", 4));
        randomGrowthOffsetSeconds = Math.max(0, getConfig().getInt("growth-visuals.random-growth-offset-seconds", 30));
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

    public boolean isGrowthParticlesEnabled() {
        return growthParticlesEnabled;
    }

    public int getGrowthParticleCount() {
        return growthParticleCount;
    }

    public int getRandomGrowthOffsetSeconds() {
        return randomGrowthOffsetSeconds;
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
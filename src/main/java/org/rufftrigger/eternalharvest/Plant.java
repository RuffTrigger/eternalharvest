package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;

public class Plant {

    private final int id;
    private final String type;
    private final Location location;
    private int growthStage;
    private long lastUpdated;
    private long lastUnloaded;

    private final FileConfiguration config; // Configuration object to access config.yml

    public Plant(int id, String type, Location location, int growthStage, long lastUpdated, long lastUnloaded) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.growthStage = growthStage;
        this.lastUpdated = lastUpdated;
        this.lastUnloaded = lastUnloaded;

        // Access the plugin's configuration file
        this.config = Main.getPlugin(Main.class).getConfig();
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastUnloaded() {
        return lastUnloaded;
    }

    public void setLastUnloaded(long lastUnloaded) {
        this.lastUnloaded = lastUnloaded;
    }

    public void startGrowthTimer() {
        new BukkitRunnable() {
            long startTime = System.currentTimeMillis();
            long growthTime = calculateGrowthTime();

            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = growthTime - elapsedTime;

                if (remainingTime <= 0) {
                    setGrowthStage(getGrowthStage() + 1);
                    setLastUpdated(System.currentTimeMillis());
                    cancel();
                    return;
                }

                int secondsRemaining = (int) (remainingTime / 1000) + 1;

                // Example effects and sounds
                location.getWorld().strikeLightningEffect(location);
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);

                // Example particle effect
                location.getWorld().spawnParticle(Particle.FIREWORK, location.add(0, 1, 0), 10);
            }
        }.runTaskTimer(Main.getPlugin(Main.class), 0L, 20L); // Run every second
    }

    private long calculateGrowthTime() {
        // Retrieve growth time from configuration based on plant type
        int defaultGrowthTime = 60; // Default growth time in seconds if not found in config.yml

        // Check if the growth time is defined in config.yml
        if (config.contains("growth-times." + type.toLowerCase())) {
            return config.getInt("growth-times." + type.toLowerCase()) * 1000; // Convert seconds to milliseconds
        } else {
            // Use default growth time if not defined in config.yml
            return defaultGrowthTime * 1000;
        }
    }
}

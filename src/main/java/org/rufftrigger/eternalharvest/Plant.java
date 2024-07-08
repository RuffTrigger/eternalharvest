package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;

public class Plant {

    private final int id;
    private final String type;
    private final Location location;
    private int growthStage;
    private long lastUpdated;
    private long lastUnloaded;

    public Plant(int id, String type, Location location, int growthStage, long lastUpdated, long lastUnloaded) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.growthStage = growthStage;
        this.lastUpdated = lastUpdated;
        this.lastUnloaded = lastUnloaded;
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
                location.getWorld().strikeLightningEffect(location); // Example effect, replace with desired visual indicator
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1); // Example sound, replace with desired sound

                // Example: Update signs or display information above plant
                // Replace with your implementation to display countdown timer
                location.getWorld().spawnParticle(Particle.FIREWORK, location.add(0, 1, 0), 10);
            }
        }.runTaskTimer(Main.getPlugin(Main.class), 0L, 20L); // Run every second
    }

    private long calculateGrowthTime() {
        // Example: Implement logic to calculate growth time based on growth stage and plant type
        // Replace with your implementation
        switch (growthStage) {
            case 1:
                return 60 * 1000; // 1 minute for stage 1
            case 2:
                return 120 * 1000; // 2 minutes for stage 2
            default:
                return 0; // Default, should not happen ideally
        }
    }
}

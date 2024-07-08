package org.rufftrigger.eternalharvest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class GrowthUpdateTask extends BukkitRunnable {

    private final DatabaseManager databaseManager;

    public GrowthUpdateTask(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        // Fetch all plant data from the database
        List<PlantData> plants = databaseManager.getAllPlants();

        // Calculate current time in seconds
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // Update growth for each plant
        for (PlantData plant : plants) {
            long plantTimeSeconds = plant.getPlantTimestamp();
            int growthTime = plant.getGrowthTime();

            // Calculate elapsed time since planting in seconds
            long elapsedTimeSeconds = currentTimeSeconds - plantTimeSeconds;

            // Calculate growth progress
            int growthProgress = (int) ((double) elapsedTimeSeconds / growthTime * 100);

            // Ensure growthProgress does not exceed 100%
            growthProgress = Math.min(growthProgress, 100);

            // Update growth progress in the database
            databaseManager.updateGrowthProgress(plant.getId(), growthProgress);

            // Apply growth progress in the game world
            applyGrowthToWorld(plant, growthProgress);
        }
    }

    private void applyGrowthToWorld(PlantData plant, int growthProgress) {
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            // Parse the location from the stored string
            Location location = LocationUtil.fromString(plant.getLocation());

            if (location != null) {
                Chunk chunk = location.getChunk();
                boolean wasLoaded = chunk.isLoaded();

                if (!wasLoaded) {
                    chunk.load();
                }

                Block block = location.getBlock();
                Material material = block.getType();

                // Handle crop growth
                if (material == plant.getMaterial() && block.getBlockData() instanceof Ageable) {
                    Ageable ageable = (Ageable) block.getBlockData();
                    int maxAge = ageable.getMaximumAge();
                    int newAge = (int) ((growthProgress / 100.0) * maxAge);
                    ageable.setAge(newAge);
                    block.setBlockData(ageable);
                    Main.getInstance().getLogger().info("Updated crop at " + location.toString() + " to growth progress " + growthProgress + "%.");
                }

                // Handle sapling growth using bonemeal
                if (isSapling(material) && growthProgress == 100) {
                    // Apply bonemeal until sapling grows into a tree
                    while (block.getType() == material && block.getBlockData() instanceof Sapling) {
                        block.getWorld().generateTree(block.getLocation(), TreeType.valueOf(material.name()));
                        Main.getInstance().getLogger().info("Forced tree growth at " + location.toString() + " using bonemeal.");
                    }
                }

                if (!wasLoaded) {
                    chunk.unload();
                }
            }
        });
    }

    private boolean isSapling(Material material) {
        return material.name().endsWith("_SAPLING");
    }

}

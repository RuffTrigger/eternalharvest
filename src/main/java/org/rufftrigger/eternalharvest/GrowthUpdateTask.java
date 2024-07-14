package org.rufftrigger.eternalharvest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

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

                if (block.getType() == plant.getMaterial()) {
                    if (block.getType().name().endsWith("_SAPLING")) {
                        // Handle sapling growth to tree
                        if (growthProgress >= 100) {
                            block.setType(Material.AIR); // Remove sapling
                            TreeType treeType = getTreeTypeFromMaterial(plant.getMaterial());
                            boolean treeGenerated = location.getWorld().generateTree(location, treeType);
                            if (treeGenerated) {
                                if (Main.getInstance().debug) {
                                    Main.getInstance().getLogger().info("The sapling at " + location.toString() + " has grown into a " + treeType.name() + " tree!");
                                }
                                // Remove the tree data from the database
                                databaseManager.recordRemoval(location, plant.getMaterial());

                                // Random chance for a bee hive to be added
                                if (Math.random() < Main.getInstance().getBeeHiveChance()) {
                                    for (BlockFace face : BlockFace.values()) {
                                        if (face == BlockFace.UP || face == BlockFace.DOWN) continue;
                                        Block potentialHiveLocation = block.getRelative(face);
                                        if (potentialHiveLocation.getType() == Material.AIR || Tag.LEAVES.isTagged(potentialHiveLocation.getType())) {
                                            if (potentialHiveLocation.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                                                potentialHiveLocation.setType(Material.BEE_NEST);
                                                int beeCount = new Random().nextInt((Main.getInstance().getMaxBeesPerHive() - Main.getInstance().getMinBeesPerHive()) + 1) + Main.getInstance().getMinBeesPerHive();
                                                for (int i = 0; i < beeCount; i++) {
                                                    Bee bee = (Bee) block.getWorld().spawnEntity(potentialHiveLocation.getLocation().add(0, -1, 0), EntityType.BEE);
                                                    bee.setHive(potentialHiveLocation.getLocation());
                                                    if (Main.getInstance().debug){
                                                        Main.getInstance().getLogger().info("Bee Hive including"+beeCount+" bee's, spawned at " + potentialHiveLocation.getLocation().toString() + " ");
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                Main.getInstance().getLogger().warning("Failed to grow tree at " + location.toString());
                                // Remove the tree data from the database
                                databaseManager.recordRemoval(location, plant.getMaterial());
                            }
                        }
                    } else if (block.getBlockData() instanceof Ageable) {
                        // Handle other ageable plants
                        Ageable ageable = (Ageable) block.getBlockData();
                        int maxAge = ageable.getMaximumAge();
                        int newAge = (int) ((growthProgress / 100.0) * maxAge);
                        ageable.setAge(newAge);
                        block.setBlockData(ageable);

                        Main.getInstance().getLogger().info("Updated Ageable block at " + location.toString() + " to growth progress " + growthProgress + "%.");
                    }
                }

                if (!wasLoaded) {
                    chunk.unload();
                }
            }
        });
    }

    private TreeType getTreeTypeFromMaterial(Material material) {
        switch (material) {
            case OAK_SAPLING:
                return TreeType.TREE;
            case BIRCH_SAPLING:
                return TreeType.BIRCH;
            case SPRUCE_SAPLING:
                return TreeType.REDWOOD;
            case JUNGLE_SAPLING:
                return TreeType.SMALL_JUNGLE;
            case ACACIA_SAPLING:
                return TreeType.ACACIA;
            case DARK_OAK_SAPLING:
                return TreeType.DARK_OAK;
            case CHERRY_SAPLING:
                return TreeType.CHERRY; // Assuming it's included in your Minecraft version or a mod
            default:
                return TreeType.TREE; // Default to oak sapling behavior
        }
    }
}

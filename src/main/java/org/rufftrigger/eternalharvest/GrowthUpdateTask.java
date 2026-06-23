package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GrowthUpdateTask extends BukkitRunnable {

    private final DatabaseManager databaseManager;
    private final Main plugin;

    public GrowthUpdateTask(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.plugin = Main.getInstance();
    }

    @Override
    public void run() {
        List<PlantData> plants = databaseManager.getAllPlants();
        if (plants.isEmpty()) {
            return;
        }

        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        Map<Integer, Integer> progressUpdates = new HashMap<>();
        List<GrowthUpdate> worldUpdates = new ArrayList<>(plants.size());

        for (PlantData plant : plants) {
            int growthTime = plant.getGrowthTime();
            if (growthTime <= 0) {
                continue;
            }

            long elapsedTimeSeconds = Math.max(0, currentTimeSeconds - plant.getPlantTimestamp());
            int growthProgress = elapsedTimeSeconds >= growthTime
                    ? 100
                    : (int) ((elapsedTimeSeconds * 100) / growthTime);

            if (growthProgress != plant.getGrowthProgress()) {
                progressUpdates.put(plant.getId(), growthProgress);
            }
            worldUpdates.add(new GrowthUpdate(plant, growthProgress));
        }

        databaseManager.updateGrowthProgressBatch(progressUpdates);

        if (!worldUpdates.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> applyGrowthToWorld(worldUpdates));
        }
    }

    private void applyGrowthToWorld(List<GrowthUpdate> updates) {
        for (GrowthUpdate update : updates) {
            applyGrowthToWorld(update.plant, update.growthProgress);
        }
    }

    private void applyGrowthToWorld(PlantData plant, int growthProgress) {
        Location location = LocationUtil.fromString(plant.getLocation());
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Block block = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (block.getType() != plant.getMaterial()) {
            return;
        }

        if (isSaplingLike(plant.getMaterial())) {
            if (growthProgress >= 100) {
                growSapling(location, block, plant);
            }
        } else if (block.getBlockData() instanceof Ageable ageable) {
            updateAgeableBlock(location, block, ageable, growthProgress);
        }
    }

    private void growSapling(Location location, Block block, PlantData plant) {
        block.setType(Material.AIR);
        TreeType treeType = getTreeTypeFromMaterial(plant.getMaterial());
        boolean treeGenerated = location.getWorld().generateTree(location, treeType);

        if (treeGenerated) {
            if (plugin.debug) {
                plugin.getLogger().info("The sapling at " + location + " has grown into a " + treeType.name() + " tree!");
            }
            maybePlaceBeeHive(location);
        } else if (plugin.debug) {
            plugin.getLogger().warning("Failed to grow tree at " + location);
        }

        removePlantRecord(location, plant.getMaterial());
    }

    private void updateAgeableBlock(Location location, Block block, Ageable ageable, int growthProgress) {
        int maxAge = ageable.getMaximumAge();
        int newAge = (int) ((growthProgress / 100.0) * maxAge);

        if (ageable.getAge() == newAge) {
            return;
        }

        ageable.setAge(newAge);
        block.setBlockData(ageable);

        if (plugin.debug) {
            plugin.getLogger().info("Updated Ageable block at " + location + " to growth progress " + growthProgress + "%.");
        }
    }

    private void removePlantRecord(Location location, Material material) {
        databaseManager.recordRemoval(location, material, success -> {
            if (success) {
                if (plugin.debug) {
                    plugin.getLogger().info("Successfully removed record for " + material + " at " + location);
                }
            } else {
                plugin.getLogger().warning("Failed to remove record for " + material + " at " + location);
            }
        });
    }

    private void maybePlaceBeeHive(Location location) {
        if (ThreadLocalRandom.current().nextDouble() >= plugin.getBeeHiveChance()) {
            return;
        }

        World world = location.getWorld();
        boolean hivePlaced = false;

        for (int dx = -2; dx <= 2 && !hivePlaced; dx++) {
            for (int dz = -2; dz <= 2 && !hivePlaced; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    Block potentialLeafBlock = world.getBlockAt(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz);
                    if (!Tag.LEAVES.isTagged(potentialLeafBlock.getType()) || potentialLeafBlock.getRelative(BlockFace.DOWN).getType() != Material.AIR) {
                        continue;
                    }

                    Block hiveBlock = potentialLeafBlock.getRelative(BlockFace.DOWN);
                    if (plugin.debug) {
                        plugin.getLogger().info("Placing hive at " + hiveBlock.getLocation());
                    }

                    hiveBlock.setType(Material.BEE_NEST);
                    spawnBees(hiveBlock);
                    hivePlaced = true;
                    break;
                }
            }
        }

        if (!hivePlaced && plugin.debug) {
            plugin.getLogger().info("No suitable location found for placing the bee hive.");
        }
    }

    private void spawnBees(Block hiveBlock) {
        int minBees = Math.min(plugin.getMinBeesPerHive(), plugin.getMaxBeesPerHive());
        int maxBees = Math.max(plugin.getMinBeesPerHive(), plugin.getMaxBeesPerHive());
        int beeCount = ThreadLocalRandom.current().nextInt(minBees, maxBees + 1);
        Location spawnLocation = hiveBlock.getLocation().add(0.5, 0, 0.5);

        for (int i = 0; i < beeCount; i++) {
            Bee bee = (Bee) hiveBlock.getWorld().spawnEntity(spawnLocation, EntityType.BEE);
            bee.setHive(hiveBlock.getLocation());
        }

        if (plugin.debug) {
            plugin.getLogger().info("Bee Hive including " + beeCount + " bees, spawned at " + hiveBlock.getLocation());
        }
    }

    private boolean isSaplingLike(Material material) {
        String materialName = material.name();
        return materialName.endsWith("_SAPLING") || material == Material.MANGROVE_PROPAGULE;
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
                return TreeType.CHERRY;
            case MANGROVE_PROPAGULE:
                int tallMangroveChance = plugin.GetTallMangroveChange();
                if (plugin.debug) {
                    plugin.getLogger().info("fetching tall Mangrove Change = " + tallMangroveChance);
                }
                if (ThreadLocalRandom.current().nextInt(100) < tallMangroveChance) {
                    if (plugin.debug) {
                        plugin.getLogger().info("Growing Tall Mangrove");
                    }
                    return TreeType.TALL_MANGROVE;
                }
                if (plugin.debug) {
                    plugin.getLogger().info("Growing a normal Mangrove");
                }
                return TreeType.MANGROVE;
            default:
                return TreeType.TREE;
        }
    }

    private static class GrowthUpdate {
        private final PlantData plant;
        private final int growthProgress;

        private GrowthUpdate(PlantData plant, int growthProgress) {
            this.plant = plant;
            this.growthProgress = growthProgress;
        }
    }
}

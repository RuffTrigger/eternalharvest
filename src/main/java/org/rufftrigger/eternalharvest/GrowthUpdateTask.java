package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Beehive;
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

    private static final int MAX_NATURAL_STACK_HEIGHT = 3;

    private final DatabaseManager databaseManager;
    private final Main plugin;

    public GrowthUpdateTask(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.plugin = Main.getInstance();
    }

    @Override
    public void run() {
        List<GrowthUpdate> worldUpdates = prepareGrowthUpdates(databaseManager.getAllPlants(), null, 0, 0);
        if (!worldUpdates.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> applyGrowthToWorld(worldUpdates));
        }
    }

    public void catchUpChunk(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        new BukkitRunnable() {
            @Override
            public void run() {
                List<GrowthUpdate> worldUpdates = prepareGrowthUpdates(databaseManager.getAllPlants(), worldName, chunkX, chunkZ);
                if (worldUpdates.isEmpty()) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null && world.isChunkLoaded(chunkX, chunkZ)) {
                        applyGrowthToWorld(worldUpdates);
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    private List<GrowthUpdate> prepareGrowthUpdates(List<PlantData> plants, String worldName, int chunkX, int chunkZ) {
        if (plants.isEmpty()) {
            return List.of();
        }

        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        Map<Integer, Integer> progressUpdates = new HashMap<>();
        List<GrowthUpdate> worldUpdates = new ArrayList<>(plants.size());

        for (PlantData plant : plants) {
            if (worldName != null && !LocationUtil.isInChunk(plant.getLocation(), worldName, chunkX, chunkZ)) {
                continue;
            }

            int growthTime = plant.getGrowthTime();
            if (growthTime <= 0) {
                continue;
            }

            long elapsedTimeSeconds = Math.max(0, currentTimeSeconds - plant.getPlantTimestamp());
            int growthProgress = calculateGrowthProgress(plant, elapsedTimeSeconds, growthTime);

            if (growthProgress != plant.getGrowthProgress()) {
                progressUpdates.put(plant.getId(), growthProgress);
            }
            worldUpdates.add(new GrowthUpdate(plant, growthProgress, elapsedTimeSeconds));
        }

        databaseManager.updateGrowthProgressBatch(progressUpdates);
        return worldUpdates;
    }

    private int calculateGrowthProgress(PlantData plant, long elapsedTimeSeconds, int growthTime) {
        if (elapsedTimeSeconds >= growthTime) {
            return 100;
        }

        long adjustedElapsedSeconds = elapsedTimeSeconds;
        int visualOffsetSeconds = getVisualOffsetSeconds(plant, growthTime);
        if (visualOffsetSeconds > 0) {
            adjustedElapsedSeconds = Math.min(growthTime - 1L, elapsedTimeSeconds + visualOffsetSeconds);
        }

        return (int) ((adjustedElapsedSeconds * 100) / growthTime);
    }

    private int getVisualOffsetSeconds(PlantData plant, int growthTime) {
        int configuredOffsetSeconds = plugin.getRandomGrowthOffsetSeconds();
        if (configuredOffsetSeconds <= 0 || growthTime <= 1) {
            return 0;
        }

        int maxOffsetSeconds = Math.min(configuredOffsetSeconds, growthTime - 1);
        int hash = (plant.getLocation() + "#" + plant.getId()).hashCode();
        return Math.floorMod(hash, maxOffsetSeconds + 1);
    }

    private void applyGrowthToWorld(List<GrowthUpdate> updates) {
        for (GrowthUpdate update : updates) {
            applyGrowthToWorld(update.plant, update.growthProgress, update.elapsedTimeSeconds);
        }
    }

    private void applyGrowthToWorld(PlantData plant, int growthProgress, long elapsedTimeSeconds) {
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

        if (isVerticalGrower(plant.getMaterial())) {
            updateVerticalGrower(location, block, plant, elapsedTimeSeconds);
        } else if (isSaplingLike(plant.getMaterial())) {
            if (growthProgress >= 100) {
                growSapling(location, block, plant);
            }
        } else if (block.getBlockData() instanceof Ageable ageable) {
            updateAgeableBlock(location, block, ageable, growthProgress);
        }
    }

    private void updateVerticalGrower(Location trackedLocation, Block trackedBlock, PlantData plant, long elapsedTimeSeconds) {
        int completedGrowthCycles = (int) (elapsedTimeSeconds / plant.getGrowthTime());
        if (completedGrowthCycles <= 0) {
            return;
        }

        Block baseBlock = getStackBase(trackedBlock, plant.getMaterial());
        int currentHeight = getStackHeight(baseBlock, plant.getMaterial());
        int growableBlocks = Math.min(completedGrowthCycles, MAX_NATURAL_STACK_HEIGHT - currentHeight);

        if (growableBlocks <= 0) {
            databaseManager.recordPlanting(trackedLocation, plant.getMaterial(), plant.getGrowthTime());
            return;
        }

        Block topBlock = baseBlock.getRelative(BlockFace.UP, currentHeight - 1);
        int grownBlocks = 0;
        for (int i = 0; i < growableBlocks; i++) {
            Block nextBlock = topBlock.getRelative(BlockFace.UP, i + 1);
            if (!canGrowInto(nextBlock)) {
                break;
            }

            nextBlock.setType(plant.getMaterial());
            spawnGrowthParticles(nextBlock.getLocation());
            grownBlocks++;
        }

        if (grownBlocks > 0 || currentHeight >= MAX_NATURAL_STACK_HEIGHT) {
            databaseManager.recordPlanting(trackedLocation, plant.getMaterial(), plant.getGrowthTime());
        }

        if (plugin.debug && grownBlocks > 0) {
            plugin.getLogger().info("Grew " + plant.getMaterial() + " stack at " + baseBlock.getLocation() + " by " + grownBlocks + " block(s).");
        }
    }

    private Block getStackBase(Block block, Material material) {
        Block baseBlock = block;
        while (baseBlock.getRelative(BlockFace.DOWN).getType() == material) {
            baseBlock = baseBlock.getRelative(BlockFace.DOWN);
        }
        return baseBlock;
    }

    private int getStackHeight(Block baseBlock, Material material) {
        int height = 0;
        Block currentBlock = baseBlock;
        while (currentBlock.getType() == material && height < MAX_NATURAL_STACK_HEIGHT) {
            height++;
            currentBlock = currentBlock.getRelative(BlockFace.UP);
        }
        return height;
    }

    private boolean canGrowInto(Block block) {
        return block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR || block.getType() == Material.VOID_AIR;
    }

    private void growSapling(Location location, Block block, PlantData plant) {
        block.setType(Material.AIR);
        TreeType treeType = getTreeTypeFromMaterial(plant.getMaterial());
        boolean treeGenerated = location.getWorld().generateTree(location, treeType);

        if (treeGenerated) {
            spawnGrowthParticles(location);
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
        int oldAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        int newAge = (int) ((growthProgress / 100.0) * maxAge);

        if (oldAge == newAge) {
            return;
        }

        ageable.setAge(newAge);
        block.setBlockData(ageable);

        if (newAge > oldAge) {
            spawnGrowthParticles(location);
        }

        if (plugin.debug) {
            plugin.getLogger().info("Updated Ageable block at " + location + " to growth progress " + growthProgress + "%.");
        }
    }

    private void spawnGrowthParticles(Location location) {
        if (!plugin.isGrowthParticlesEnabled() || plugin.getGrowthParticleCount() <= 0 || location.getWorld() == null) {
            return;
        }

        Location particleLocation = location.clone().add(0.5, 0.75, 0.5);
        location.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                particleLocation,
                plugin.getGrowthParticleCount(),
                0.25,
                0.25,
                0.25,
                0.01
        );
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
        Location spawnLocation = hiveBlock.getLocation().add(0.5, -0.25, 0.5);
        int storedBees = 0;

        if (hiveBlock.getState() instanceof Beehive beehive) {
            beehive.setMaxEntities(Math.max(beehive.getMaxEntities(), maxBees));

            for (int i = 0; i < beeCount; i++) {
                Bee bee = (Bee) hiveBlock.getWorld().spawnEntity(spawnLocation, EntityType.BEE);
                prepareBeeForHive(bee, hiveBlock);

                try {
                    beehive.addEntity(bee);
                    storedBees++;
                } catch (IllegalStateException e) {
                    if (plugin.debug) {
                        plugin.getLogger().warning("Bee nest was full while adding bees at " + hiveBlock.getLocation());
                    }
                }
            }

            beehive.update(true);
        } else {
            for (int i = 0; i < beeCount; i++) {
                Bee bee = (Bee) hiveBlock.getWorld().spawnEntity(spawnLocation, EntityType.BEE);
                prepareBeeForHive(bee, hiveBlock);
            }
        }

        if (plugin.debug) {
            plugin.getLogger().info("Bee nest at " + hiveBlock.getLocation() + " received " + beeCount + " bees (stored=" + storedBees + ").");
        }
    }

    private void prepareBeeForHive(Bee bee, Block hiveBlock) {
        bee.setHive(hiveBlock.getLocation());
        bee.setPersistent(true);
        bee.setRemoveWhenFarAway(false);
        bee.setAnger(0);
        bee.setCannotEnterHiveTicks(0);
    }

    private boolean isVerticalGrower(Material material) {
        return material == Material.SUGAR_CANE || material == Material.CACTUS;
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
        private final long elapsedTimeSeconds;

        private GrowthUpdate(PlantData plant, int growthProgress, long elapsedTimeSeconds) {
            this.plant = plant;
            this.growthProgress = growthProgress;
            this.elapsedTimeSeconds = elapsedTimeSeconds;
        }
    }
}
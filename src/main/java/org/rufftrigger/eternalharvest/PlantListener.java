package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class PlantListener implements Listener {

    private final DatabaseManager databaseManager;
    private final GrowthUpdateTask growthUpdateTask;
    private final Main plugin;

    public PlantListener(DatabaseManager databaseManager, GrowthUpdateTask growthUpdateTask) {
        this.databaseManager = databaseManager;
        this.growthUpdateTask = growthUpdateTask;
        this.plugin = Main.getInstance();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        growthUpdateTask.catchUpChunk(event.getChunk());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        int growthTime = plugin.getGrowthTime(material);

        if (growthTime != -1) {
            if (plugin.debug) {
                plugin.getLogger().info("Growth time (" + growthTime + ") was found for " + material.name().toLowerCase() + " at " + block.getLocation());
            }
            databaseManager.recordPlanting(block.getLocation(), material, growthTime);
        } else if (plugin.debug) {
            plugin.getLogger().info("Growth time was not found for " + material.name().toLowerCase() + " at " + block.getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        if (!plugin.isTrackedPlant(material)) {
            if (plugin.debug) {
                plugin.getLogger().info("Growth time was not found for " + material.name().toLowerCase() + " at " + block.getLocation());
            }
            return;
        }

        Location location = block.getLocation();
        databaseManager.recordRemoval(location, material, success -> {
            if (success) {
                if (plugin.debug) {
                    plugin.getLogger().info("Successfully removed record for " + material.name().toLowerCase() + " at " + location);
                }
            } else if (plugin.debug) {
                plugin.getLogger().info("No record found for " + material.name().toLowerCase() + " at " + location);
            }
        });
    }

    /**
     * Villager farming (harvest + replant) does not fire BlockBreak/BlockPlace.
     * It modifies blocks via EntityChangeBlockEvent.
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }

        Block block = event.getBlock();
        Material fromType = block.getType();
        Material toType = event.getTo();

        if (plugin.isTrackedPlant(fromType) && isAir(toType)) {
            if (plugin.debug) {
                plugin.getLogger().info("Villager harvested " + fromType + " at " + block.getLocation());
            }
            databaseManager.recordRemovalByLocation(block.getLocation());
            return;
        }

        if (isAir(fromType) && plugin.isTrackedPlant(toType)) {
            int growthTime = plugin.getGrowthTime(toType);
            if (growthTime != -1) {
                if (plugin.debug) {
                    plugin.getLogger().info("Villager planted " + toType + " at " + block.getLocation() + " (growthTime=" + growthTime + ")");
                }
                databaseManager.recordPlanting(block.getLocation(), toType, growthTime);
            }
        }
    }

    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleTrackedRemoval(block, "explosion");
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleTrackedRemoval(block, "explosion");
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        handleTrackedRemoval(event.getBlock(), "fire");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();

        if (block == null || block.getType() != Material.SWEET_BERRY_BUSH) {
            return;
        }

        Location location = block.getLocation();
        databaseManager.getMaterialAtLocation(location, material -> {
            if (material == Material.SWEET_BERRY_BUSH) {
                int growthTime = plugin.getGrowthTime(material);
                if (growthTime != -1) {
                    databaseManager.recordPlanting(location, material, growthTime);
                    if (plugin.debug) {
                        plugin.getLogger().info(material.name().toLowerCase() + " planting time and growth progress reset at " + location);
                    }
                }
            } else if (material != null) {
                plugin.getLogger().warning("Unexpected material fetched from database at location " + location);
            } else if (plugin.debug) {
                plugin.getLogger().info("No sweet berry bush record found at " + location);
            }
        });
    }

    private void handleTrackedRemoval(Block block, String cause) {
        Material material = block.getType();
        Location location = block.getLocation();

        if (plugin.debug) {
            plugin.getLogger().info(material.name().toLowerCase() + " was removed due to " + cause + " at " + location);
        }

        if (plugin.isTrackedPlant(material)) {
            databaseManager.recordRemovalByLocation(location);
        } else if (plugin.debug) {
            plugin.getLogger().info("Growth time was not found for " + material.name().toLowerCase() + " at " + location);
            plugin.getLogger().info(material.name().toLowerCase() + " was NOT removed from plant_growth.db due to " + cause);
        }
    }
}
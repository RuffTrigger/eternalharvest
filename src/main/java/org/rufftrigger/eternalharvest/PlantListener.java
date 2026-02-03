package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Level;

public class PlantListener implements Listener {

    private final DatabaseManager databaseManager;

    public PlantListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time (" + growthTime + " was found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            }
            databaseManager.recordPlanting(event.getBlock().getLocation(), material, growthTime);
        } else {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // First, check if a record exists for the block at the given location
            databaseManager.getMaterialAtLocation(event.getBlock().getLocation(), existingMaterial -> {
                if (existingMaterial != null && existingMaterial == material) {
                    // Proceed to remove the record if it exists
                    databaseManager.recordRemoval(event.getBlock().getLocation(), material, success -> {
                        if (success) {
                            if (Main.getInstance().debug) {
                                Main.getInstance().getLogger().info("Successfully removed record for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
                            }
                        } else {
                            Main.getInstance().getLogger().warning("Failed to remove record for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
                        }
                    });
                } else {
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info("No record found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
                    }
                }
            });
        } else {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            }
        }
    }

    /**
     * Villager farming (harvest + replant) does NOT fire BlockBreak/BlockPlace.
     * It modifies blocks via EntityChangeBlockEvent.
     *
     * We track:
     * - harvest: tracked crop -> AIR
     * - plant:   AIR -> tracked crop (usually age 0)
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) {
            return;
        }

        Block block = event.getBlock();
        Material fromType = block.getType();       // current type before change
        Material toType = event.getTo();           // new type after change

        // Harvest: tracked plant removed (crop -> air)
        if (isTrackedPlant(fromType) && isAir(toType)) {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Villager harvested " + fromType + " at " + block.getLocation());
            }
            databaseManager.recordRemovalByLocation(block.getLocation());
            return;
        }

        // Planting: villager plants a tracked crop (air -> crop)
        if (isAir(fromType) && isTrackedPlant(toType)) {
            int growthTime = Main.getInstance().getConfig().getInt("growth-times." + toType.toString().toLowerCase(), -1);
            if (growthTime != -1) {
                if (Main.getInstance().debug) {
                    Main.getInstance().getLogger().info("Villager planted " + toType + " at " + block.getLocation() + " (growthTime=" + growthTime + ")");
                }
                databaseManager.recordPlanting(block.getLocation(), toType, growthTime);
            }
        }
    }

    private boolean isTrackedPlant(Material material) {
        if (material == null) return false;
        return Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1) != -1;
    }

    private boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockExplosion(block);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockExplosion(block);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        handleBlockBurn(event.getBlock());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && block.getType() == Material.SWEET_BERRY_BUSH) {
            Location location = block.getLocation();

            // Fetch material from database based on location
            databaseManager.getMaterialAtLocation(location, material -> {
                // Ensure material fetched matches sweet berry bush to proceed
                if (material == Material.SWEET_BERRY_BUSH) {
                    try {
                        // Reset planting time and growth progress
                        long currentTimestamp = System.currentTimeMillis() / 1000;
                        int growthProgress = 0;

                        // Remove the existing record first
                        databaseManager.recordRemoval(location, material, success -> {
                            if (success) {
                                // Log and inform player about removal
                                if (Main.getInstance().debug) {
                                    Main.getInstance().getLogger().info(material.toString().toLowerCase() + " planting time and growth progress reset at " + location);
                                }

                                try {
                                    int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

                                    if (growthTime != -1) {
                                        // Record the new planting
                                        databaseManager.recordPlanting(location, material, growthTime);

                                    } else {
                                        Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + location.toString());
                                    }
                                } catch (Exception e) {
                                    Main.getInstance().getLogger().log(Level.SEVERE, "Error recording planting in database", e);

                                }
                            } else {
                                Main.getInstance().getLogger().log(Level.SEVERE, "Error removing existing record from database");

                            }
                        });

                    } catch (Exception e) {
                        Main.getInstance().getLogger().log(Level.SEVERE, "Error resetting plant in database", e);

                    }
                } else {
                    // Material fetched doesn't match expected material
                    Main.getInstance().getLogger().warning("Unexpected material fetched from database at location " + location.toString());

                }
            });
        }
    }

    private void handleBlockExplosion(Block block) {
        Material material = block.getType();
        if (Main.getInstance().debug) {
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed due to explosion at " + block.getLocation());
        }
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // Record removal in the database asynchronously
            databaseManager.recordRemovalByLocation(block.getLocation());
        } else {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + block.getLocation());
                Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT removed from plant_growth.db due to explosion");
            }
        }
    }

    private void handleBlockBurn(Block block) {
        Material material = block.getType();
        if (Main.getInstance().debug) {
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed due to fire at " + block.getLocation());
        }
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // Record removal in the database asynchronously
            databaseManager.recordRemovalByLocation(block.getLocation());
        } else {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + block.getLocation());
                Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT removed from plant_growth.db due to fire");
            }
        }
    }
}

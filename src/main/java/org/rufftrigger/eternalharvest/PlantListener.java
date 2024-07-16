package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
            databaseManager.recordPlanting(event.getBlock().getLocation(), material, growthTime);
        } else {
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            databaseManager.recordRemoval(event.getBlock().getLocation(), material);
        } else {
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location explosionLocation = block.getLocation();

            databaseManager.getMaterialAtLocation(explosionLocation, material -> {
                if (material != null) {
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + explosionLocation + " due to entity explosion");
                    }
                } else {
                    Main.getInstance().getLogger().info("No plant material found at " + explosionLocation + " in the database.");
                }

                databaseManager.removeAllPlantsAtLocation(explosionLocation, () -> {
                    // Optional: Callback after plants are removed
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info("All plants removed at " + explosionLocation + " due to entity explosion");
                    }
                });
            });
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location explosionLocation = block.getLocation();

            databaseManager.getMaterialAtLocation(explosionLocation, material -> {
                if (material != null) {
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + explosionLocation + " due to block explosion");
                    }
                } else {
                    Main.getInstance().getLogger().info("No plant material found at " + explosionLocation + " in the database.");
                }

                databaseManager.removeAllPlantsAtLocation(explosionLocation, () -> {
                    // Optional: Callback after plants are removed
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info("All plants removed at " + explosionLocation + " due to block explosion");
                    }
                });
            });
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Location burnLocation = event.getBlock().getLocation();

        databaseManager.getMaterialAtLocation(burnLocation, material -> {
            if (material != null) {
                if (Main.getInstance().debug) {
                    Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + burnLocation + " due to fire");
                }
            } else {
                Main.getInstance().getLogger().info("No plant material found at " + burnLocation + " in the database.");
            }

            databaseManager.removeAllPlantsAtLocation(burnLocation, () -> {
                // Optional: Callback after plants are removed
                if (Main.getInstance().debug) {
                    Main.getInstance().getLogger().info("All plants removed at " + burnLocation + " due to fire");
                }
            });
        });
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

                        // Update database: Reset planting time and growth progress
                        databaseManager.resetPlantingTimeAndProgress(location, currentTimestamp, growthProgress);

                        // Log and inform player
                        if (Main.getInstance().debug) {
                            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " planting time and growth progress reset at " + location);
                        }

                    } catch (Exception e) {
                        // Log the error
                        Main.getInstance().getLogger().log(Level.SEVERE, "Error resetting plant in database", e);
                        // Inform the player of the error
                        player.sendMessage(ChatColor.RED + "An error occurred while processing your action. Please try again later.");
                    }
                } else {
                    // Material fetched doesn't match expected material
                    Main.getInstance().getLogger().warning("Unexpected material fetched from database at location " + location.toString());
                    // Inform the player of the unexpected material
                    player.sendMessage(ChatColor.RED + "This sweet berry bush is not ready to be harvested yet.");
                }
            });
        }
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.block.Block;

import java.util.List;

public class PlantListener implements Listener {

    private final DatabaseManager databaseManager;

    public PlantListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();
        if (Main.getInstance().debug) {
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was placed at " + event.getBlock().getLocation() + " finding growth time");
        }
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info("Growth time (" + growthTime + " was found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            }
            // Record planting in the database asynchronously
            databaseManager.recordPlanting(event.getBlock().getLocation(), material, growthTime);
        }
        else{
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT added to plant_growth.db");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        if (Main.getInstance().debug) {
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + event.getBlock().getLocation());
        }
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // Record removal in the database asynchronously
            databaseManager.recordRemoval(event.getBlock().getLocation(), material);
        }
        else{
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + event.getBlock().getLocation());
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT Removed from plant_growth.db");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            // Get explosion location
            Location explosionLocation = block.getLocation();

            // Fetch material from database based on explosion location
            Material material = databaseManager.getMaterialAtLocation(explosionLocation);

            // Log the removal of material due to entity explosion
            if (material != null) {
                if (Main.getInstance().debug) {
                    Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + explosionLocation + " due to entity explosion");
                }
            } else {
                Main.getInstance().getLogger().info("No plant material found at " + explosionLocation + " in the database.");
            }

            // Remove all plant records at the explosion location
            databaseManager.removeAllPlantsAtLocation(explosionLocation);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            // Get explosion location
            Location explosionLocation = block.getLocation();

            // Fetch material from database based on explosion location
            Material material = databaseManager.getMaterialAtLocation(explosionLocation);

            // Log the removal of material due to block explosion
            if (material != null) {
                if (Main.getInstance().debug) {
                    Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + explosionLocation + " due to block explosion");
                }
            } else {
                Main.getInstance().getLogger().info("No plant material found at " + explosionLocation + " in the database.");
            }

            // Remove all plant records at the explosion location
            databaseManager.removeAllPlantsAtLocation(explosionLocation);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Location burnLocation = block.getLocation();

        // Fetch material from database based on burn location
        Material material = databaseManager.getMaterialAtLocation(burnLocation);

        if (material != null) {
            if (Main.getInstance().debug) {
                Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was removed at " + burnLocation + " due to fire");
            }
        } else {
            Main.getInstance().getLogger().info("No plant material found at " + burnLocation + " in the database.");
        }

        // Remove all plant records at the burn location
        databaseManager.removeAllPlantsAtLocation(burnLocation);
    }



    private void handleBlockExplosion(Block block) {
        Material material = block.getType();
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // Record removal in the database asynchronously
            databaseManager.recordRemoval(block.getLocation(), material);
        } else {
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + block.getLocation());
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT removed from plant_growth.db due to explosion");
        }
    }

}

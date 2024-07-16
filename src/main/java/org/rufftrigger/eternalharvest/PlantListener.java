package org.rufftrigger.eternalharvest;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.block.Block;

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

    private void handleBlockBurn(Block block) {
        Material material = block.getType();
        int growthTime = Main.getInstance().getConfig().getInt("growth-times." + material.toString().toLowerCase(), -1);

        if (growthTime != -1) {
            // Record removal in the database asynchronously
            databaseManager.recordRemoval(block.getLocation(), material);
        } else {
            Main.getInstance().getLogger().info("Growth time was not found for " + material.toString().toLowerCase() + " at " + block.getLocation());
            Main.getInstance().getLogger().info(material.toString().toLowerCase() + " was NOT removed from plant_growth.db due to fire");
        }
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlantEventListener implements Listener {

    private final JavaPlugin plugin;

    public PlantEventListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        plugin.getLogger().info("BlockPlaceEvent triggered: " + block.getType() + " at " + block.getLocation());

        if (block.getType() == Material.WHEAT || block.getType() == Material.CARROTS ||
                block.getType() == Material.POTATOES || block.getType() == Material.BEETROOTS ||
                block.getType().toString().endsWith("_SAPLING")) {

            Location location = block.getLocation();
            long currentTime = System.currentTimeMillis();

            plugin.getLogger().info("Planting detected: " + block.getType() + " at " + location + " at time " + currentTime);

            Plant plant = new Plant(0, block.getType().name(), location, 0, currentTime, currentTime);
            PlantGrowthManager.getInstance().addPlant(plant);

            plugin.getLogger().info("Plant added to PlantGrowthManager: " + plant);
        } else {
            plugin.getLogger().info("Block placed is not a tracked plant type: " + block.getType());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (block.getType() == Material.WHEAT || block.getType() == Material.CARROTS ||
                block.getType() == Material.POTATOES || block.getType() == Material.BEETROOTS ||
                block.getType().toString().endsWith("_SAPLING")) {

            plugin.getLogger().info("BlockBreakEvent triggered: " + block.getType() + " at " + location);

            PlantGrowthManager.getInstance().removePlant(location);

            plugin.getLogger().info("Plant removed from PlantGrowthManager at " + location);
        }
    }
}

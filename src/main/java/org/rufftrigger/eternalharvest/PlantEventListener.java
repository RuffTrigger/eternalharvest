package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class PlantEventListener implements Listener {

    private final JavaPlugin plugin;

    public PlantEventListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.WHEAT || block.getType() == Material.CARROTS || block.getType() == Material.POTATOES || block.getType() == Material.BEETROOTS || block.getType().toString().endsWith("_SAPLING")) {
            Location location = block.getLocation();
            long currentTime = System.currentTimeMillis();

            Plant plant = new Plant(0, block.getType().name(), location, 0, currentTime, currentTime);
            PlantGrowthManager.getInstance().addPlant(plant);

            plugin.getLogger().info("Added plant of type " + plant.getType() + " at " + location);
        }
    }
}

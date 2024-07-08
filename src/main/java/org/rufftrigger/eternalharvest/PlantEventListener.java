package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class PlantEventListener implements Listener {
    private final Main plugin;

    public PlantEventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();

        if (isTrackedPlantType(material)) {
            // Remove plant from database asynchronously
            new BukkitRunnable() {
                @Override
                public void run() {
                    PlantGrowthManager plantManager = PlantGrowthManager.getInstance();
                    Plant plant = plantManager.getPlantAtLocation(location);
                    if (plant != null) {
                        plantManager.removePlant(plant.getId());
                        plugin.getLogger().info("Removed plant data: " + plant);
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private boolean isTrackedPlantType(Material material) {
        // Check if the material is one of the tracked types
        switch (material) {
            case BEETROOTS:
            case CARROTS:
            case POTATOES:
            case WHEAT:
                return true;
            default:
                // Check if the material ends with "_SAPLING"
                return material.name().endsWith("_SAPLING");
        }
    }
}

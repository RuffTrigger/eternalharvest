package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlantEventListener implements Listener {
    private final Main plugin;

    public PlantEventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location location = event.getBlock().getLocation();
        Material material = event.getBlock().getType();
        String type = material.toString();

        if (isTrackedPlantType(material)) {
            long currentTime = System.currentTimeMillis();
            Plant plant = new Plant(type, location, 0, currentTime, currentTime, plugin);
            PlantGrowthManager.getInstance().addPlant(plant);
            PlantGrowthManager.getInstance().savePlantData(plant, plugin.getConnection());
            plugin.getLogger().info("Planting detected: " + plant);
        } else {
            plugin.getLogger().info("Block placed is not a tracked plant type: " + type);
        }
    }

    private boolean isTrackedPlantType(Material material) {
        switch (material) {
            case BEETROOTS:
            case CARROTS:
            case POTATOES:
            case WHEAT:
                return true;
            default:
                return material.toString().endsWith("_SAPLING");
        }
    }
}

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

        if (isTrackedPlantType(material)) {
            long currentTime = System.currentTimeMillis();
            Plant plant = new Plant(material.toString(), location, 0, currentTime, currentTime, plugin);
            savePlant(plant);
        } else {
            plugin.getLogger().info("Block placed is not a tracked plant type: " + material.toString());
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

    private void savePlant(Plant plant) {
        PlantGrowthManager plantManager = PlantGrowthManager.getInstance();
        plantManager.addPlant(plant);
        plantManager.savePlantData(plant, plugin.getConnection());
        plugin.getLogger().info("Planting detected: " + plant);
    }
}

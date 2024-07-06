package org.rufftrigger.eternalharvest;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkEventListener implements Listener {
    private final Main plugin;

    public ChunkEventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        plugin.getLogger().info("Chunk loaded at: " + chunk.getX() + ", " + chunk.getZ());
        loadPlantsFromDatabase(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        plugin.getLogger().info("Chunk unloaded at: " + chunk.getX() + ", " + chunk.getZ());
        savePlantsToDatabase(chunk);
    }

    private void loadPlantsFromDatabase(Chunk chunk) {
        // Implement this method to load plant data from the database for the chunk
        PlantGrowthManager.getInstance().loadAllPlantData(plugin.getConnection());
    }

    private void savePlantsToDatabase(Chunk chunk) {
        // Implement this method to save plant data to the database for the chunk
        PlantGrowthManager.getInstance().saveAllPlantData();
    }
}

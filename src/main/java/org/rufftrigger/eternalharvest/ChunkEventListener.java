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

        loadPlantsFromDatabase(chunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();


        // Check if the chunk is still loaded and accessible
        if (chunk.isLoaded()) {
            savePlantsToDatabase(chunk);
        } else {

        }
    }

    private void loadPlantsFromDatabase(Chunk chunk) {
        // Load plant data specific to the chunk from the database
        PlantGrowthManager.getInstance().loadPlantsInChunk(chunk, plugin.getConnection());
    }

    private void savePlantsToDatabase(Chunk chunk) {
        // Save plant data specific to the chunk to the database
        PlantGrowthManager.getInstance().savePlantsInChunk(chunk);
    }
}

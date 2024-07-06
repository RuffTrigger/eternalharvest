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
        PlantGrowthManager.getInstance().loadPlantsInChunk(chunk, plugin.getConnection());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Check if the chunk is still loaded and accessible
        if (isChunkLoaded(chunk)) {
            PlantGrowthManager.getInstance().savePlantsInChunk(chunk);
        } else {
            // Chunk is no longer accessible or loaded, handle accordingly
            plugin.getLogger().warning("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") unloaded but not accessible.");
        }
    }

    private boolean isChunkLoaded(Chunk chunk) {
        // Check if the chunk is still loaded and accessible
        return chunk.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ());
    }
}

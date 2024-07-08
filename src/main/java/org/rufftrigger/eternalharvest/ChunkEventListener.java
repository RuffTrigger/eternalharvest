package org.rufftrigger.eternalharvest;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

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
}

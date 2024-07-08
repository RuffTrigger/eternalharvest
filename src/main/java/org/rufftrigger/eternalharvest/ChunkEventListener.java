package org.rufftrigger.eternalharvest;

import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ChunkEventListener implements Listener {
    private final Main plugin;

    public ChunkEventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // Load plants in chunk asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                PlantGrowthManager.getInstance().loadPlantsInChunk(chunk, plugin.getConnection());
            }
        }.runTaskAsynchronously(plugin);
    }
}

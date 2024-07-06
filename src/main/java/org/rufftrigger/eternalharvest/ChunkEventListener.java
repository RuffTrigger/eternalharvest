package org.rufftrigger.eternalharvest;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkEventListener implements Listener {

    private final JavaPlugin plugin;

    public ChunkEventListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        long unloadTime = System.currentTimeMillis();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Location location = chunk.getBlock(x, 0, z).getLocation();
                PlantGrowthManager.getInstance().setLastUnloaded(location, unloadTime);
                plugin.getLogger().info("Unloaded time set for plant at " + location.toString());
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Location location = chunk.getBlock(x, 0, z).getLocation();
                PlantGrowthManager.getInstance().updatePlantGrowth(location);
                plugin.getLogger().info("Plant growth updated at " + location.toString());
            }
        }
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ChunkEventListener implements Listener {

    private final JavaPlugin plugin;

    public ChunkEventListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        long unloadTime = System.currentTimeMillis();

        plugin.getLogger().info("Chunk unloading at " + chunk.getX() + ", " + chunk.getZ() + ". Unload time: " + unloadTime);

        // Save unload time for all plants in this chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Location location = chunk.getBlock(x, 0, z).getLocation();
                PlantGrowthManager.getInstance().setLastUnloaded(location, unloadTime);
                plugin.getLogger().info("Set last unloaded time for plant at " + location);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        plugin.getLogger().info("Chunk loading at " + chunk.getX() + ", " + chunk.getZ());

        // Update plant growth for all plants in this chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Location location = chunk.getBlock(x, 0, z).getLocation();
                PlantGrowthManager.getInstance().updatePlantGrowth(location);
                plugin.getLogger().info("Updated plant growth for plant at " + location);
            }
        }
    }
}

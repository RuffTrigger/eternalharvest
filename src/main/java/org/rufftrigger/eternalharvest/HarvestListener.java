package org.rufftrigger.eternalharvest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Level;

public class HarvestListener implements Listener {

    private final DatabaseManager databaseManager;

    public HarvestListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && block.getType() == Material.SWEET_BERRY_BUSH) {
            Location location = block.getLocation();

            // Fetch material from database based on location
            Material material = databaseManager.getMaterialAtLocation(location);

            // Ensure material fetched matches sweet berry bush to proceed
            if (material == Material.SWEET_BERRY_BUSH) {
                try {
                    // Update growth progress to 0%
                    if (Main.getInstance().debug) {
                        Main.getInstance().getLogger().info(material.toString().toLowerCase() + " growth progress was updated to 0% at " + location);
                    }
                    databaseManager.recordPlanting(location, material, 0);

                } catch (Exception e) {
                    // Log the error
                    Main.getInstance().getLogger().log(Level.SEVERE, "Error updating growth progress in database", e);
                    // Inform the player of the error
                    player.sendMessage(ChatColor.RED + "An error occurred while processing your action. Please try again later.");
                }
            } else {
                // Material fetched doesn't match expected material
                Main.getInstance().getLogger().warning("Unexpected material fetched from database at location " + location.toString());
                // Inform the player of the unexpected material
                player.sendMessage(ChatColor.RED + "This sweet berry bush is not ready to be harvested yet.");
            }
        }
    }
}

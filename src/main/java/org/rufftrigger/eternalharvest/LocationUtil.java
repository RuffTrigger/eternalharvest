package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtil {

    public static Location fromString(String locationString) {
        try {
            String[] parts = locationString.replace("Location{world=", "").replace("}", "").split(",");
            String worldName = parts[0].split("=")[1];
            double x = Double.parseDouble(parts[1].split("=")[1]);
            double y = Double.parseDouble(parts[2].split("=")[1]);
            double z = Double.parseDouble(parts[3].split("=")[1]);

            return new Location(Bukkit.getWorld(worldName), x, y, z);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Error parsing location string: " + locationString);
            return null;
        }
    }
}

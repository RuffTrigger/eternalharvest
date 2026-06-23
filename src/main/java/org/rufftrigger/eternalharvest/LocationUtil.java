package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocationUtil {

    private static final Pattern LEGACY_LOCATION_PATTERN = Pattern.compile(
            ".*world=(?:CraftWorld\\{name=)?([^},]+).*?x=([-+]?\\d+(?:\\.\\d+)?(?:[Ee][-+]?\\d+)?),y=([-+]?\\d+(?:\\.\\d+)?(?:[Ee][-+]?\\d+)?),z=([-+]?\\d+(?:\\.\\d+)?(?:[Ee][-+]?\\d+)?).*"
    );

    private LocationUtil() {
    }

    public static String toKey(Location location) {
        if (location == null || location.getWorld() == null) {
            logWarning("Cannot serialize a location without a world.");
            return null;
        }

        return toKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static String normalizeKey(String locationString) {
        if (locationString == null || locationString.isBlank()) {
            return locationString;
        }

        LocationParts stableParts = parseStableKey(locationString);
        if (stableParts != null) {
            return toKey(stableParts.worldName, stableParts.x, stableParts.y, stableParts.z);
        }

        Matcher matcher = LEGACY_LOCATION_PATTERN.matcher(locationString);
        if (matcher.matches()) {
            return toKey(
                    matcher.group(1),
                    parseBlockCoordinate(matcher.group(2)),
                    parseBlockCoordinate(matcher.group(3)),
                    parseBlockCoordinate(matcher.group(4))
            );
        }

        logWarning("Could not normalize location string: " + locationString);
        return locationString;
    }

    public static Location fromString(String locationString) {
        LocationParts parts = parseStableKey(normalizeKey(locationString));
        if (parts == null) {
            logWarning("Error parsing location string: " + locationString);
            return null;
        }

        World world = Bukkit.getWorld(parts.worldName);
        if (world == null) {
            logWarning("World is not loaded for stored location: " + parts.worldName);
            return null;
        }

        return new Location(world, parts.x, parts.y, parts.z);
    }

    private static String toKey(String worldName, int x, int y, int z) {
        return worldName + ";" + x + ";" + y + ";" + z;
    }

    private static LocationParts parseStableKey(String locationString) {
        String[] parts = locationString.split(";", -1);
        if (parts.length != 4) {
            return null;
        }

        try {
            return new LocationParts(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseBlockCoordinate(String coordinate) {
        return (int) Math.floor(Double.parseDouble(coordinate));
    }

    private static void logWarning(String message) {
        Main plugin = Main.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    private static class LocationParts {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;

        private LocationParts(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;

public class PlantData {

    private final int id;
    private final String location; // Keep this as String for storage
    private final Material material;
    private final int growthTime;
    private final long plantTimestamp;
    private final int growthProgress;
    private final long lastUpdated;

    public PlantData(int id, String location, Material material, int growthTime, long plantTimestamp, int growthProgress, long lastUpdated) {
        this.id = id;
        this.location = location;
        this.material = material;
        this.growthTime = growthTime;
        this.plantTimestamp = plantTimestamp;
        this.growthProgress = growthProgress;
        this.lastUpdated = lastUpdated;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public Location getLocationObject() {
        return LocationUtil.fromString(location);
    }

    public Material getMaterial() {
        return material;
    }

    public int getGrowthTime() {
        return growthTime;
    }

    public long getPlantTimestamp() {
        return plantTimestamp;
    }

    public int getGrowthProgress() {
        return growthProgress;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}

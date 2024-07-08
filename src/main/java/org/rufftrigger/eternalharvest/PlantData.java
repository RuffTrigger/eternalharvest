package org.rufftrigger.eternalharvest;

import org.bukkit.Material;

public class PlantData {

    private int id;
    private String location;
    private Material material;
    private int growthTime;
    private long plantTimestamp;
    private long lastUpdated;

    public PlantData(int id, String location, Material material, int growthTime, long plantTimestamp, long lastUpdated) {
        this.id = id;
        this.location = location;
        this.material = material;
        this.growthTime = growthTime;
        this.plantTimestamp = plantTimestamp;
        this.lastUpdated = lastUpdated;
    }

    public int getId() {
        return id;
    }

    public String getLocation() {
        return location;
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

    public long getLastUpdated() {
        return lastUpdated;
    }
}

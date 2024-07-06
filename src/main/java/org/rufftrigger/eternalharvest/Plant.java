package org.rufftrigger.eternalharvest;

import org.bukkit.Location;

public class Plant {

    private int id;
    private String type;
    private Location location;
    private int growthStage;
    private long lastUpdated;
    private long lastUnloaded;

    public Plant(int id, String type, Location location, int growthStage, long lastUpdated, long lastUnloaded) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.growthStage = growthStage;
        this.lastUpdated = lastUpdated;
        this.lastUnloaded = lastUnloaded;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public long getLastUnloaded() {
        return lastUnloaded;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setLastUnloaded(long lastUnloaded) {
        this.lastUnloaded = lastUnloaded;
    }

    @Override
    public String toString() {
        return "Plant{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", location=" + location +
                ", growthStage=" + growthStage +
                ", lastUpdated=" + lastUpdated +
                ", lastUnloaded=" + lastUnloaded +
                '}';
    }
}

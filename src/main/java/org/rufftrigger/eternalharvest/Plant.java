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

    public Plant(String type, Location location, int growthStage, long lastUpdated, long lastUnloaded, Main plugin) {
        this.id = plugin.generateUniqueId();
        this.type = type;
        this.location = location;
        this.growthStage = growthStage;
        this.lastUpdated = lastUpdated;
        this.lastUnloaded = lastUnloaded;
    }

    // Getters and setters...

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

package org.rufftrigger.eternalharvest;

import org.bukkit.scheduler.BukkitRunnable;

public class MaintenanceTask extends BukkitRunnable {

    private final DatabaseManager databaseManager;

    public MaintenanceTask(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        databaseManager.maintainDatabase();
    }
}

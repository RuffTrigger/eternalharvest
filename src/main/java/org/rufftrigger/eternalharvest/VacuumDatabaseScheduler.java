package org.rufftrigger.eternalharvest;

import org.bukkit.scheduler.BukkitRunnable;

public class VacuumDatabaseScheduler extends BukkitRunnable {

    private final DatabaseManager databaseManager;

    public VacuumDatabaseScheduler(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        // Implement your database vacuuming logic here
        databaseManager.vacuumDatabase();
    }
}

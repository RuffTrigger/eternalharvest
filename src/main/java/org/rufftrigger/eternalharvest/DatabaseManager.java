package org.rufftrigger.eternalharvest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.rufftrigger.eternalharvest.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private Connection connection;

    public void setupDatabase() {
        // Implement your database setup logic here (e.g., SQLite or MySQL connection)
        // Example for SQLite:
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + Main.getInstance().getDataFolder() + "/data.db");
            // Create tables if necessary
            PreparedStatement createTableStatement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS plant_data (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "location TEXT," +
                            "material TEXT," +
                            "growth_time INTEGER," +
                            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ");"
            );
            createTableStatement.executeUpdate();
            createTableStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordPlanting(final org.bukkit.Location location, final Material material, final int growthTime) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement insertStatement = connection.prepareStatement(
                            "INSERT INTO plant_data (location, material, growth_time) VALUES (?, ?, ?);"
                    );
                    insertStatement.setString(1, location.toString());
                    insertStatement.setString(2, material.toString());
                    insertStatement.setInt(3, growthTime);
                    insertStatement.executeUpdate();
                    insertStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void recordRemoval(final org.bukkit.Location location, final Material material) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement deleteStatement = connection.prepareStatement(
                            "DELETE FROM plant_data WHERE location = ? AND material = ?;"
                    );
                    deleteStatement.setString(1, location.toString());
                    deleteStatement.setString(2, material.toString());
                    deleteStatement.executeUpdate();
                    deleteStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }
}

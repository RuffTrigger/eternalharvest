package org.rufftrigger.eternalharvest;

import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;

    public void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + Main.getInstance().getDataFolder() + "/data.db");

            // Create table if not exists
            PreparedStatement createTableStatement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS plant_data (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "location TEXT," +
                            "material TEXT," +
                            "growth_time INTEGER," +
                            "plant_timestamp INTEGER," +
                            "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
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
                            "INSERT INTO plant_data (location, material, growth_time, plant_timestamp) VALUES (?, ?, ?, ?);"
                    );
                    insertStatement.setString(1, location.toString());
                    insertStatement.setString(2, material.toString());
                    insertStatement.setInt(3, growthTime);
                    insertStatement.setLong(4, System.currentTimeMillis() / 1000); // Store current time in seconds
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

    public List<PlantData> getAllPlants() throws SQLException {
        List<PlantData> plants = new ArrayList<>();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM plant_data;");
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            PlantData plant = new PlantData(
                    resultSet.getInt("id"),
                    resultSet.getString("location"),
                    Material.valueOf(resultSet.getString("material")),
                    resultSet.getInt("growth_time"),
                    resultSet.getLong("plant_timestamp"),
                    resultSet.getTimestamp("last_updated").getTime()
            );
            plants.add(plant);
        }
        resultSet.close();
        statement.close();
        return plants;
    }

    public void updateGrowthProgress(int id, int growthProgress) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    PreparedStatement updateStatement = connection.prepareStatement(
                            "UPDATE plant_data SET growth_progress = ? WHERE id = ?;"
                    );
                    updateStatement.setInt(1, growthProgress);
                    updateStatement.setInt(2, id);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }
}

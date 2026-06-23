package org.rufftrigger.eternalharvest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private Connection connection;
    private final Logger logger;
    private final Object dbLock = new Object();
    private final AtomicInteger activeStatements = new AtomicInteger(0);

    public DatabaseManager() {
        logger = Main.getInstance().getLogger();
    }

    public void setupDatabase() {
        try {
            File dataFolder = Main.getInstance().getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                throw new IOException("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
            }

            File dbFile = new File(dataFolder, "plant_growth.db");
            if (!dbFile.exists()) {
                createNewDatabase(dbFile);
            }

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            configureConnection();
            createTable();
            migrateLocationKeys();
            int duplicatesRemoved = removeDuplicateLocations();
            createIndexes();

            if (duplicatesRemoved > 0) {
                logger.info("Removed " + duplicatesRemoved + " duplicate plant records during database setup.");
            }
            logger.info("Database setup completed.");
        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Error setting up database.", e);
        }
    }

    private void configureConnection() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000;");
            statement.execute("PRAGMA journal_mode = WAL;");
            statement.execute("PRAGMA synchronous = NORMAL;");
        }
    }

    private void createTable() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS plant_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "location TEXT NOT NULL," +
                "material TEXT NOT NULL," +
                "growth_time INTEGER NOT NULL," +
                "plant_timestamp INTEGER NOT NULL," +
                "growth_progress INTEGER DEFAULT 0," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
            createTableStatement.executeUpdate();
        }
    }

    private void createIndexes() throws SQLException {
        try (PreparedStatement indexStatement = connection.prepareStatement(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_plant_data_location_unique ON plant_data(location);"
        )) {
            indexStatement.executeUpdate();
        }
    }

    private void migrateLocationKeys() throws SQLException {
        List<LocationMigration> migrations = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement("SELECT id, location FROM plant_data;");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String currentLocation = resultSet.getString("location");
                String normalizedLocation = LocationUtil.normalizeKey(currentLocation);

                if (normalizedLocation != null && !normalizedLocation.equals(currentLocation)) {
                    migrations.add(new LocationMigration(id, normalizedLocation));
                }
            }
        }

        if (migrations.isEmpty()) {
            return;
        }

        try (PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE plant_data SET location = ? WHERE id = ?;"
        )) {
            for (LocationMigration migration : migrations) {
                updateStatement.setString(1, migration.locationKey);
                updateStatement.setInt(2, migration.id);
                updateStatement.addBatch();
            }
            updateStatement.executeBatch();
        }

        logger.info("Migrated " + migrations.size() + " plant location keys.");
    }

    private int removeDuplicateLocations() throws SQLException {
        List<Integer> duplicateIds = new ArrayList<>();
        Set<String> seenLocations = new HashSet<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, location FROM plant_data ORDER BY location ASC, plant_timestamp DESC, id DESC;"
        ); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String location = resultSet.getString("location");

                if (!seenLocations.add(location)) {
                    duplicateIds.add(id);
                }
            }
        }

        if (duplicateIds.isEmpty()) {
            return 0;
        }

        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM plant_data WHERE id = ?;")) {
            for (int duplicateId : duplicateIds) {
                deleteStatement.setInt(1, duplicateId);
                deleteStatement.addBatch();
            }
            deleteStatement.executeBatch();
        }

        return duplicateIds.size();
    }

    private void createNewDatabase(File dbFile) throws IOException {
        try {
            if (dbFile.createNewFile()) {
                logger.info("Database file created: " + dbFile.getAbsolutePath());
            } else {
                throw new IOException("Failed to create database file.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating database file.", e);
            throw e;
        }
    }

    public void recordPlanting(final Location location, final Material material, final int growthTime) {
        final String locationKey = LocationUtil.toKey(location);
        if (locationKey == null) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                activeStatements.incrementAndGet();
                synchronized (dbLock) {
                    try {
                        int updatedRows;
                        try (PreparedStatement updateStatement = connection.prepareStatement(
                                "UPDATE plant_data SET material = ?, growth_time = ?, plant_timestamp = ?, " +
                                        "growth_progress = 0, last_updated = CURRENT_TIMESTAMP WHERE location = ?;"
                        )) {
                            updateStatement.setString(1, material.toString());
                            updateStatement.setInt(2, growthTime);
                            updateStatement.setLong(3, System.currentTimeMillis() / 1000);
                            updateStatement.setString(4, locationKey);
                            updatedRows = updateStatement.executeUpdate();
                        }

                        if (updatedRows == 0) {
                            try (PreparedStatement insertStatement = connection.prepareStatement(
                                    "INSERT INTO plant_data (location, material, growth_time, plant_timestamp, growth_progress) " +
                                            "VALUES (?, ?, ?, ?, 0);"
                            )) {
                                insertStatement.setString(1, locationKey);
                                insertStatement.setString(2, material.toString());
                                insertStatement.setInt(3, growthTime);
                                insertStatement.setLong(4, System.currentTimeMillis() / 1000);
                                insertStatement.executeUpdate();
                            }
                        }

                        if (Main.getInstance().debug) {
                            logger.info("Recorded planting: Material=" + material + ", Location=" + locationKey);
                        }
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, "Error recording planting.", e);
                    } finally {
                        activeStatements.decrementAndGet();
                    }
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void recordRemoval(final Location location, final Material material, Consumer<Boolean> callback) {
        final String locationKey = LocationUtil.toKey(location);
        if (locationKey == null) {
            if (callback != null) {
                callback.accept(false);
            }
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = false;
                activeStatements.incrementAndGet();
                synchronized (dbLock) {
                    try (PreparedStatement deleteStatement = connection.prepareStatement(
                            "DELETE FROM plant_data WHERE location = ? AND material = ?;"
                    )) {
                        deleteStatement.setString(1, locationKey);
                        deleteStatement.setString(2, material.toString());
                        success = deleteStatement.executeUpdate() > 0;

                        if (Main.getInstance().debug) {
                            logger.info("Recorded removal: Material=" + material + ", Location=" + locationKey + ", Success=" + success);
                        }
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, "Error recording removal.", e);
                    } finally {
                        activeStatements.decrementAndGet();
                    }
                }

                if (callback != null) {
                    callback.accept(success);
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void recordRemovalByLocation(final Location location) {
        final String locationKey = LocationUtil.toKey(location);
        if (locationKey == null) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                activeStatements.incrementAndGet();
                synchronized (dbLock) {
                    try (PreparedStatement deleteStatement = connection.prepareStatement(
                            "DELETE FROM plant_data WHERE location = ?;"
                    )) {
                        int rowsAffected = deleteStatement.executeUpdate();

                        if (Main.getInstance().debug) {
                            logger.info("Recorded removal by location: Location=" + locationKey + ", Rows=" + rowsAffected);
                        }
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, "Error recording removal by location.", e);
                    } finally {
                        activeStatements.decrementAndGet();
                    }
                }
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public List<PlantData> getAllPlants() {
        List<PlantData> plants = new ArrayList<>();
        synchronized (dbLock) {
            activeStatements.incrementAndGet();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, location, material, growth_time, plant_timestamp, growth_progress, last_updated FROM plant_data;"
            ); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Material material;
                    try {
                        material = Material.valueOf(resultSet.getString("material"));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Skipping plant record with unknown material: " + resultSet.getString("material"));
                        continue;
                    }

                    Timestamp lastUpdated = resultSet.getTimestamp("last_updated");
                    PlantData plant = new PlantData(
                            resultSet.getInt("id"),
                            resultSet.getString("location"),
                            material,
                            resultSet.getInt("growth_time"),
                            resultSet.getLong("plant_timestamp"),
                            resultSet.getInt("growth_progress"),
                            lastUpdated == null ? 0L : lastUpdated.getTime()
                    );
                    plants.add(plant);
                }

                if (Main.getInstance().debug) {
                    logger.info("Retrieved " + plants.size() + " plants from database.");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error retrieving plants from database.", e);
            } finally {
                activeStatements.decrementAndGet();
            }
        }
        return plants;
    }

    public void updateGrowthProgress(int id, int growthProgress) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateGrowthProgressBatch(Collections.singletonMap(id, growthProgress));
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void updateGrowthProgressBatch(Map<Integer, Integer> progressById) {
        if (progressById.isEmpty()) {
            return;
        }

        activeStatements.incrementAndGet();
        synchronized (dbLock) {
            boolean originalAutoCommit = true;
            try {
                originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try (PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE plant_data SET growth_progress = ?, last_updated = CURRENT_TIMESTAMP WHERE id = ?;"
                )) {
                    for (Map.Entry<Integer, Integer> entry : progressById.entrySet()) {
                        updateStatement.setInt(1, entry.getValue());
                        updateStatement.setInt(2, entry.getKey());
                        updateStatement.addBatch();
                    }
                    updateStatement.executeBatch();
                }

                connection.commit();

                if (Main.getInstance().debug) {
                    logger.info("Updated growth progress for " + progressById.size() + " plants.");
                }
            } catch (SQLException e) {
                rollbackQuietly();
                logger.log(Level.SEVERE, "Error updating growth progress batch.", e);
            } finally {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Error restoring database auto-commit mode.", e);
                }
                activeStatements.decrementAndGet();
            }
        }
    }

    public void resetPlantingTimeAndProgress(Location location, long currentTimestamp, int growthProgress) throws SQLException {
        String locationKey = LocationUtil.toKey(location);
        if (locationKey == null) {
            return;
        }

        synchronized (dbLock) {
            activeStatements.incrementAndGet();
            try (PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE plant_data SET plant_timestamp = ?, growth_progress = ?, last_updated = CURRENT_TIMESTAMP WHERE location = ?;"
            )) {
                updateStatement.setLong(1, currentTimestamp);
                updateStatement.setInt(2, growthProgress);
                updateStatement.setString(3, locationKey);
                updateStatement.executeUpdate();
            } finally {
                activeStatements.decrementAndGet();
            }
        }
    }

    public void getMaterialAtLocation(final Location location, Consumer<Material> callback) {
        final String locationKey = LocationUtil.toKey(location);
        if (locationKey == null) {
            callback.accept(null);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Material material = null;
                activeStatements.incrementAndGet();
                synchronized (dbLock) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT material FROM plant_data WHERE location = ?;"
                    )) {
                        statement.setString(1, locationKey);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (resultSet.next()) {
                                material = Material.valueOf(resultSet.getString("material"));
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Stored material is no longer valid at " + locationKey + ".", e);
                    } catch (SQLException e) {
                        logger.log(Level.SEVERE, "Error fetching material at location.", e);
                    } finally {
                        activeStatements.decrementAndGet();
                    }
                }

                callback.accept(material);
            }
        }.runTaskAsynchronously(Main.getInstance());
    }

    public void closeConnection() {
        synchronized (dbLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    logger.info("Database connection closed.");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error closing database connection.", e);
            }
        }
    }

    public void maintainDatabase() {
        activeStatements.incrementAndGet();
        synchronized (dbLock) {
            try {
                int removed = removeDuplicateLocations();
                if (removed > 0 || Main.getInstance().debug) {
                    logger.info("Database maintenance removed " + removed + " duplicate plant records.");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error removing duplicates from database.", e);
            } finally {
                activeStatements.decrementAndGet();
            }
        }
    }

    public void vacuumDatabase() {
        while (activeStatements.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        synchronized (dbLock) {
            try (PreparedStatement vacuumStatement = connection.prepareStatement("VACUUM;")) {
                vacuumStatement.executeUpdate();
                logger.info("Database vacuumed to reduce file size.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error vacuuming database.", e);
            }
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            logger.log(Level.SEVERE, "Error rolling back database transaction.", rollbackException);
        }
    }

    private static class LocationMigration {
        private final int id;
        private final String locationKey;

        private LocationMigration(int id, String locationKey) {
            this.id = id;
            this.locationKey = locationKey;
        }
    }
}

package com.mygolem.storage;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GolemRepository implements AutoCloseable {

    private final Connection connection;

    private GolemRepository(Connection connection) throws SQLException {
        this.connection = connection;
        configure();
        migrate();
    }

    public static GolemRepository open(Path path) throws SQLException {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new SQLException("Unable to create SQLite directory for " + path, exception);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        return new GolemRepository(connection);
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("PRAGMA foreign_keys=ON");
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS golems (
                        id TEXT PRIMARY KEY,
                        owner TEXT NOT NULL,
                        entity_uuid TEXT,
                        world TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        center_world TEXT NOT NULL,
                        center_x REAL NOT NULL,
                        center_y REAL NOT NULL,
                        center_z REAL NOT NULL,
                        center_yaw REAL NOT NULL,
                        center_pitch REAL NOT NULL,
                        chest_world TEXT,
                        chest_x REAL,
                        chest_y REAL,
                        chest_z REAL,
                        chest_yaw REAL,
                        chest_pitch REAL,
                        active INTEGER NOT NULL,
                        backpack TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
        }
    }

    public synchronized void save(GolemRecord record) throws SQLException {
        String sql = """
                INSERT INTO golems (
                    id, owner, entity_uuid, world, x, y, z, yaw, pitch,
                    center_world, center_x, center_y, center_z, center_yaw, center_pitch,
                    chest_world, chest_x, chest_y, chest_z, chest_yaw, chest_pitch,
                    active, backpack, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    owner = excluded.owner,
                    entity_uuid = excluded.entity_uuid,
                    world = excluded.world,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch,
                    center_world = excluded.center_world,
                    center_x = excluded.center_x,
                    center_y = excluded.center_y,
                    center_z = excluded.center_z,
                    center_yaw = excluded.center_yaw,
                    center_pitch = excluded.center_pitch,
                    chest_world = excluded.chest_world,
                    chest_x = excluded.chest_x,
                    chest_y = excluded.chest_y,
                    chest_z = excluded.chest_z,
                    chest_yaw = excluded.chest_yaw,
                    chest_pitch = excluded.chest_pitch,
                    active = excluded.active,
                    backpack = excluded.backpack,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.id().toString());
            statement.setString(2, record.owner().toString());
            statement.setString(3, record.entityUuid() == null ? null : record.entityUuid().toString());
            writeLocation(statement, 4, record.location());
            writeLocation(statement, 10, record.center());
            writeNullableLocation(statement, 16, record.chest());
            statement.setInt(22, record.active() ? 1 : 0);
            statement.setString(23, record.backpack().serialize());
            statement.setLong(24, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    public synchronized void delete(UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM golems WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        }
    }

    public synchronized List<GolemRecord> loadAll() throws SQLException {
        List<GolemRecord> records = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM golems");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                records.add(read(result));
            }
        }
        return records;
    }

    private GolemRecord read(ResultSet result) throws SQLException {
        String entityRaw = result.getString("entity_uuid");
        return new GolemRecord(
                UUID.fromString(result.getString("id")),
                UUID.fromString(result.getString("owner")),
                entityRaw == null || entityRaw.isBlank() ? null : UUID.fromString(entityRaw),
                readLocation(result, ""),
                readLocation(result, "center_"),
                readNullableLocation(result, "chest_"),
                result.getInt("active") == 1,
                BackpackSnapshot.deserialize(result.getString("backpack"))
        );
    }

    private static void writeLocation(PreparedStatement statement, int start, StoredLocation location) throws SQLException {
        statement.setString(start, location.world());
        statement.setDouble(start + 1, location.x());
        statement.setDouble(start + 2, location.y());
        statement.setDouble(start + 3, location.z());
        statement.setFloat(start + 4, location.yaw());
        statement.setFloat(start + 5, location.pitch());
    }

    private static void writeNullableLocation(PreparedStatement statement, int start, StoredLocation location) throws SQLException {
        if (location == null) {
            for (int offset = 0; offset < 6; offset++) {
                statement.setObject(start + offset, null);
            }
            return;
        }
        writeLocation(statement, start, location);
    }

    private static StoredLocation readLocation(ResultSet result, String prefix) throws SQLException {
        return new StoredLocation(
                result.getString(prefix + "world"),
                result.getDouble(prefix + "x"),
                result.getDouble(prefix + "y"),
                result.getDouble(prefix + "z"),
                result.getFloat(prefix + "yaw"),
                result.getFloat(prefix + "pitch")
        );
    }

    private static StoredLocation readNullableLocation(ResultSet result, String prefix) throws SQLException {
        String world = result.getString(prefix + "world");
        if (world == null || world.isBlank()) {
            return null;
        }
        return readLocation(result, prefix);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}

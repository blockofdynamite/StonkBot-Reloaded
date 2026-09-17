package com.stonkbot.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private final Connection connection;

    public Database(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            initSchema();
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Could not open database at " + file, ex);
        }
    }

    private Database(Connection connection) {
        this.connection = connection;
    }

    public static Database inMemory() {
        try {
            Database db = new Database(DriverManager.getConnection("jdbc:sqlite::memory:"));
            db.initSchema();
            return db;
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not open in-memory database", ex);
        }
    }

    private void initSchema() {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        guild_id   TEXT NOT NULL,
                        user_id    TEXT NOT NULL,
                        cash       REAL NOT NULL,
                        created_at TEXT NOT NULL DEFAULT (datetime('now')),
                        PRIMARY KEY (guild_id, user_id)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS holdings (
                        guild_id TEXT NOT NULL,
                        user_id  TEXT NOT NULL,
                        symbol   TEXT NOT NULL,
                        shares   REAL NOT NULL,
                        avg_cost REAL NOT NULL,
                        PRIMARY KEY (guild_id, user_id, symbol)
                    )
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not initialize database schema", ex);
        }
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}

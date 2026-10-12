package com.yourname.universalleaderboards.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class SQLiteStorage extends AbstractSqlStorage {

    private final File dataFolder;

    public SQLiteStorage(File dataFolder, Executor asyncExecutor, Logger logger) {
        super(asyncExecutor, logger);
        this.dataFolder = dataFolder;
    }

    @Override
    public void initialize() throws Exception {
        File dbFile = new File(dataFolder, "database/leaderboards.db");
        dbFile.getParentFile().mkdirs();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only safely supports one writer connection.
        config.setPoolName("AvenoxLeaderboard-SQLite");
        this.dataSource = new HikariDataSource(config);

        super.initialize();
    }

    @Override
    protected String createTableStatement() {
        return "CREATE TABLE IF NOT EXISTS leaderboard_entries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "board_id TEXT NOT NULL," +
                "uuid TEXT NOT NULL," +
                "username TEXT," +
                "display_name TEXT," +
                "value REAL NOT NULL," +
                "raw_value TEXT," +
                "created_at INTEGER," +
                "updated_at INTEGER," +
                "period_type TEXT NOT NULL," +
                "period_start INTEGER NOT NULL," +
                "UNIQUE(board_id, uuid, period_type, period_start)" +
                ")";
    }

    @Override
    protected String upsertStatement() {
        // created_at is intentionally absent from the DO UPDATE SET clause below, so the
        // original insert time is preserved across every subsequent refresh cycle.
        return "INSERT INTO leaderboard_entries (board_id, uuid, username, display_name, value, raw_value, updated_at, period_type, period_start, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(board_id, uuid, period_type, period_start) DO UPDATE SET " +
                "username = excluded.username, display_name = excluded.display_name, " +
                "value = excluded.value, raw_value = excluded.raw_value, updated_at = excluded.updated_at";
    }

    @Override
    protected String incrementStatement() {
        // SQLite upsert lets the DO UPDATE clause reference both the pre-existing row (via
        // the table name) and the proposed values (via `excluded`), so `value` here means
        // the row's current stored value, not the new one being inserted.
        return "INSERT INTO leaderboard_entries (board_id, uuid, username, display_name, value, raw_value, updated_at, period_type, period_start, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(board_id, uuid, period_type, period_start) DO UPDATE SET " +
                "username = excluded.username, display_name = excluded.display_name, " +
                "value = leaderboard_entries.value + excluded.value, " +
                "raw_value = excluded.raw_value, updated_at = excluded.updated_at";
    }
}

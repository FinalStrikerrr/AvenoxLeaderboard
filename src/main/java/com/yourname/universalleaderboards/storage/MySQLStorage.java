package com.yourname.universalleaderboards.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class MySQLStorage extends AbstractSqlStorage {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;
    private final boolean ssl;

    public MySQLStorage(String host, int port, String database, String username, String password,
                         int poolSize, boolean ssl, Executor asyncExecutor, Logger logger) {
        super(asyncExecutor, logger);
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.ssl = ssl;
    }

    @Override
    public void initialize() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(Math.max(2, poolSize));
        config.setPoolName("AvenoxLeaderboard-MySQL");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.dataSource = new HikariDataSource(config);

        super.initialize();
    }

    @Override
    protected String createTableStatement() {
        return "CREATE TABLE IF NOT EXISTS leaderboard_entries (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "board_id VARCHAR(64) NOT NULL," +
                "uuid CHAR(36) NOT NULL," +
                "username VARCHAR(32)," +
                "display_name VARCHAR(64)," +
                "value DOUBLE NOT NULL," +
                "raw_value VARCHAR(64)," +
                "created_at BIGINT," +
                "updated_at BIGINT," +
                "period_type VARCHAR(16) NOT NULL," +
                "period_start BIGINT NOT NULL," +
                "UNIQUE KEY uniq_entry (board_id, uuid, period_type, period_start)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    @Override
    protected String upsertStatement() {
        // created_at is intentionally absent from the ON DUPLICATE KEY UPDATE clause below,
        // so the original insert time is preserved across every subsequent refresh cycle.
        return "INSERT INTO leaderboard_entries (board_id, uuid, username, display_name, value, raw_value, updated_at, period_type, period_start, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE username = VALUES(username), display_name = VALUES(display_name), " +
                "value = VALUES(value), raw_value = VALUES(raw_value), updated_at = VALUES(updated_at)";
    }

    @Override
    protected String incrementStatement() {
        // In MySQL's ON DUPLICATE KEY UPDATE, an unqualified column name means the row's
        // current stored value, and VALUES(column) means the proposed insert value - so
        // `value = value + VALUES(value)` is the standard atomic-increment-on-upsert idiom.
        return "INSERT INTO leaderboard_entries (board_id, uuid, username, display_name, value, raw_value, updated_at, period_type, period_start, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE username = VALUES(username), display_name = VALUES(display_name), " +
                "value = value + VALUES(value), raw_value = VALUES(raw_value), updated_at = VALUES(updated_at)";
    }
}

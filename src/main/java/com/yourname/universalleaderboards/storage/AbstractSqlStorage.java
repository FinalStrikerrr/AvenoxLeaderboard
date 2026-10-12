package com.yourname.universalleaderboards.storage;

import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Shared SQL implementation for both SQLite and MySQL backends. The two differ only in
 * DataSource configuration and a couple of dialect-specific statements (upsert syntax),
 * which subclasses override.
 */
public abstract class AbstractSqlStorage implements StorageProvider {

    protected HikariDataSource dataSource;
    protected final Executor asyncExecutor;
    protected final Logger logger;
    protected final Consumer<String> errorLogger;

    protected AbstractSqlStorage(Executor asyncExecutor, Logger logger) {
        this.asyncExecutor = asyncExecutor;
        this.logger = logger;
        this.errorLogger = msg -> logger.warning("[Storage] " + msg);
    }

    protected abstract String createTableStatement();

    protected abstract String upsertStatement();

    /** Same shape as upsertStatement() but adds to the existing value instead of overwriting it. */
    protected abstract String incrementStatement();

    @Override
    public void initialize() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableStatement());
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_board_period ON leaderboard_entries(board_id, period_type, period_start)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_uuid ON leaderboard_entries(uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_value ON leaderboard_entries(value)");
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Void> upsertEntry(String boardId, String periodType, long periodStart, LeaderboardEntry entry) {
        return upsertEntries(boardId, periodType, periodStart, List.of(entry));
    }

    @Override
    public CompletableFuture<Void> upsertEntries(String boardId, String periodType, long periodStart, List<LeaderboardEntry> entries) {
        return CompletableFuture.runAsync(() -> {
            if (entries.isEmpty()) return;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(upsertStatement())) {
                connection.setAutoCommit(false);
                try {
                    for (LeaderboardEntry entry : entries) {
                        ps.setString(1, boardId);
                        ps.setString(2, entry.getUuid().toString());
                        ps.setString(3, entry.getUsername());
                        ps.setString(4, entry.getDisplayName());
                        ps.setDouble(5, entry.getValue());
                        ps.setString(6, entry.getRawValue());
                        ps.setLong(7, entry.getUpdatedAt());
                        ps.setString(8, periodType);
                        ps.setLong(9, periodStart);
                        // created_at: only meaningful on first insert - the dialect-specific
                        // upsertStatement() intentionally omits this column from its UPDATE
                        // clause so an existing row's original created_at is preserved.
                        ps.setLong(10, entry.getUpdatedAt());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    connection.commit();
                } catch (SQLException batchError) {
                    connection.rollback();
                    throw batchError;
                }
            } catch (SQLException e) {
                errorLogger.accept("Failed to upsert entries for board " + boardId + ": " + e.getMessage());
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> incrementEntries(String boardId, String periodType, long periodStart, List<IncrementDelta> deltas) {
        return CompletableFuture.runAsync(() -> {
            if (deltas.isEmpty()) return;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(incrementStatement())) {
                connection.setAutoCommit(false);
                try {
                    long now = System.currentTimeMillis();
                    for (IncrementDelta delta : deltas) {
                        ps.setString(1, boardId);
                        ps.setString(2, delta.uuid().toString());
                        ps.setString(3, delta.username());
                        ps.setString(4, delta.displayName());
                        ps.setDouble(5, delta.delta());
                        ps.setString(6, String.valueOf(delta.delta()));
                        ps.setLong(7, now);
                        ps.setString(8, periodType);
                        ps.setLong(9, periodStart);
                        ps.setLong(10, now);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    connection.commit();
                } catch (SQLException batchError) {
                    connection.rollback();
                    throw batchError;
                }
            } catch (SQLException e) {
                errorLogger.accept("Failed to increment entries for board " + boardId + ": " + e.getMessage());
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<List<LeaderboardEntry>> getTopEntries(String boardId, String periodType, long periodStart, boolean descending, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LeaderboardEntry> results = new ArrayList<>();
            String sql = "SELECT uuid, username, display_name, value, raw_value, updated_at FROM leaderboard_entries " +
                    "WHERE board_id = ? AND period_type = ? AND period_start = ? " +
                    "ORDER BY value " + (descending ? "DESC" : "ASC") + " LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, boardId);
                ps.setString(2, periodType);
                ps.setLong(3, periodStart);
                ps.setInt(4, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                errorLogger.accept("Failed to fetch top entries for board " + boardId + ": " + e.getMessage());
            }
            return results;
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<LeaderboardEntry> getEntry(String boardId, String periodType, long periodStart, UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid, username, display_name, value, raw_value, updated_at FROM leaderboard_entries " +
                    "WHERE board_id = ? AND period_type = ? AND period_start = ? AND uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, boardId);
                ps.setString(2, periodType);
                ps.setLong(3, periodStart);
                ps.setString(4, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapRow(rs);
                }
            } catch (SQLException e) {
                errorLogger.accept("Failed to fetch entry for " + uuid + " on board " + boardId + ": " + e.getMessage());
            }
            return null;
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Integer> getRank(String boardId, String periodType, long periodStart, UUID uuid, boolean descending) {
        return getEntry(boardId, periodType, periodStart, uuid).thenComposeAsync(entry -> {
            if (entry == null) return CompletableFuture.completedFuture(-1);
            String comparator = descending ? ">" : "<";
            String sql = "SELECT COUNT(*) FROM leaderboard_entries WHERE board_id = ? AND period_type = ? AND period_start = ? AND value " + comparator + " ?";
            return CompletableFuture.supplyAsync(() -> {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, boardId);
                    ps.setString(2, periodType);
                    ps.setLong(3, periodStart);
                    ps.setDouble(4, entry.getValue());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getInt(1) + 1;
                    }
                } catch (SQLException e) {
                    errorLogger.accept("Failed to compute rank for " + uuid + " on board " + boardId + ": " + e.getMessage());
                }
                return -1;
            }, asyncExecutor);
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Long> getEntryCount(String boardId, String periodType, long periodStart) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM leaderboard_entries WHERE board_id = ? AND period_type = ? AND period_start = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, boardId);
                ps.setString(2, periodType);
                ps.setLong(3, periodStart);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            } catch (SQLException e) {
                errorLogger.accept("Failed to count entries for board " + boardId + ": " + e.getMessage());
            }
            return 0L;
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> removeEntry(String boardId, String periodType, long periodStart, UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM leaderboard_entries WHERE board_id = ? AND period_type = ? AND period_start = ? AND uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, boardId);
                ps.setString(2, periodType);
                ps.setLong(3, periodStart);
                ps.setString(4, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                errorLogger.accept("Failed to remove entry " + uuid + " on board " + boardId + ": " + e.getMessage());
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> resetBoard(String boardId, String periodType) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM leaderboard_entries WHERE board_id = ? AND period_type = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, boardId);
                ps.setString(2, periodType);
                ps.executeUpdate();
            } catch (SQLException e) {
                errorLogger.accept("Failed to reset board " + boardId + ": " + e.getMessage());
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> resetAll() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM leaderboard_entries");
            } catch (SQLException e) {
                errorLogger.accept("Failed to reset all leaderboards: " + e.getMessage());
            }
        }, asyncExecutor);
    }

    private LeaderboardEntry mapRow(ResultSet rs) throws SQLException {
        return new LeaderboardEntry(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getDouble("value"),
                rs.getString("raw_value"),
                rs.getLong("updated_at")
        );
    }
}

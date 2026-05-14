package org.stellarvan.stellarDuelBridge.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;

public final class SQLiteStorageProvider implements StorageProvider {

    private final StellarDuelBridge plugin;
    private final ConfigManager configManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "StellarDuelBridge-SQLite");
        thread.setDaemon(true);
        return thread;
    });
    private Connection connection;

    public SQLiteStorageProvider(StellarDuelBridge plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void init() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Unable to create plugin data folder.");
            }
            File databaseFile = new File(plugin.getDataFolder(), configManager.getDuelSettings().sqliteFile());
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS duel_schema (
                      id INTEGER PRIMARY KEY CHECK (id = 1),
                      version INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """);
                statement.execute("""
                    INSERT OR IGNORE INTO duel_schema (id, version, updated_at)
                    VALUES (1, 1, strftime('%s','now'))
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS duel_players (
                      uuid TEXT PRIMARY KEY,
                      name TEXT NOT NULL,
                      wins INTEGER NOT NULL DEFAULT 0,
                      losses INTEGER NOT NULL DEFAULT 0,
                      draws INTEGER NOT NULL DEFAULT 0,
                      quits INTEGER NOT NULL DEFAULT 0,
                      current_streak INTEGER NOT NULL DEFAULT 0,
                      best_streak INTEGER NOT NULL DEFAULT 0,
                      total_matches INTEGER NOT NULL DEFAULT 0,
                      total_duration_seconds INTEGER NOT NULL DEFAULT 0,
                      last_mode TEXT,
                      last_match_at INTEGER,
                      created_at INTEGER NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_players_name ON duel_players(name)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_players_wins ON duel_players(wins DESC)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_players_best_streak ON duel_players(best_streak DESC)");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS duel_matches (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      arena_id TEXT NOT NULL,
                      mode TEXT NOT NULL,
                      player_one_uuid TEXT NOT NULL,
                      player_one_name TEXT NOT NULL,
                      player_two_uuid TEXT NOT NULL,
                      player_two_name TEXT NOT NULL,
                      winner_uuid TEXT,
                      winner_name TEXT,
                      loser_uuid TEXT,
                      loser_name TEXT,
                      result TEXT NOT NULL,
                      end_reason TEXT NOT NULL,
                      started_at INTEGER NOT NULL,
                      ended_at INTEGER NOT NULL,
                      duration_seconds INTEGER NOT NULL,
                      created_at INTEGER NOT NULL
                    )
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_matches_player_one ON duel_matches(player_one_uuid)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_matches_player_two ON duel_matches(player_two_uuid)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_matches_winner ON duel_matches(winner_uuid)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_duel_matches_started_at ON duel_matches(started_at DESC)");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize SQLite storage.", exception);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to close SQLite connection: " + exception.getMessage());
        }
    }

    @Override
    public CompletableFuture<DuelStats> loadStats(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM duel_players WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        DuelStats stats = new DuelStats(uuid, resultSet.getString("name"));
                        stats.setWins(resultSet.getInt("wins"));
                        stats.setLosses(resultSet.getInt("losses"));
                        stats.setDraws(resultSet.getInt("draws"));
                        stats.setQuits(resultSet.getInt("quits"));
                        stats.setCurrentStreak(resultSet.getInt("current_streak"));
                        stats.setBestStreak(resultSet.getInt("best_streak"));
                        stats.setTotalMatches(resultSet.getInt("total_matches"));
                        stats.setTotalDurationSeconds(resultSet.getInt("total_duration_seconds"));
                        stats.setLastMode(resultSet.getString("last_mode"));
                        stats.setLastMatchAt(resultSet.getLong("last_match_at"));
                        stats.setCreatedAt(resultSet.getLong("created_at"));
                        stats.setUpdatedAt(resultSet.getLong("updated_at"));
                        stats.setName(name);
                        return stats;
                    }
                }
                return new DuelStats(uuid, name);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load duel stats for " + uuid, exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveStats(DuelStats stats) {
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis() / 1000L;
            if (stats.getCreatedAt() <= 0L) {
                stats.setCreatedAt(now);
            }
            stats.setUpdatedAt(now);
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO duel_players (
                  uuid, name, wins, losses, draws, quits, current_streak, best_streak, total_matches,
                  total_duration_seconds, last_mode, last_match_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                  name = excluded.name,
                  wins = excluded.wins,
                  losses = excluded.losses,
                  draws = excluded.draws,
                  quits = excluded.quits,
                  current_streak = excluded.current_streak,
                  best_streak = excluded.best_streak,
                  total_matches = excluded.total_matches,
                  total_duration_seconds = excluded.total_duration_seconds,
                  last_mode = excluded.last_mode,
                  last_match_at = excluded.last_match_at,
                  updated_at = excluded.updated_at
                """)) {
                statement.setString(1, stats.getUuid().toString());
                statement.setString(2, stats.getName());
                statement.setInt(3, stats.getWins());
                statement.setInt(4, stats.getLosses());
                statement.setInt(5, stats.getDraws());
                statement.setInt(6, stats.getQuits());
                statement.setInt(7, stats.getCurrentStreak());
                statement.setInt(8, stats.getBestStreak());
                statement.setInt(9, stats.getTotalMatches());
                statement.setInt(10, stats.getTotalDurationSeconds());
                statement.setString(11, stats.getLastMode());
                statement.setLong(12, stats.getLastMatchAt());
                statement.setLong(13, stats.getCreatedAt());
                statement.setLong(14, stats.getUpdatedAt());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save duel stats for " + stats.getUuid(), exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> recordMatch(MatchRecord record) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO duel_matches (
                  arena_id, mode, player_one_uuid, player_one_name, player_two_uuid, player_two_name,
                  winner_uuid, winner_name, loser_uuid, loser_name, result, end_reason,
                  started_at, ended_at, duration_seconds, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                statement.setString(1, record.arenaId());
                statement.setString(2, record.mode());
                statement.setString(3, record.playerOneUuid());
                statement.setString(4, record.playerOneName());
                statement.setString(5, record.playerTwoUuid());
                statement.setString(6, record.playerTwoName());
                statement.setString(7, record.winnerUuid());
                statement.setString(8, record.winnerName());
                statement.setString(9, record.loserUuid());
                statement.setString(10, record.loserName());
                statement.setString(11, record.result());
                statement.setString(12, record.endReason());
                statement.setLong(13, record.startedAt());
                statement.setLong(14, record.endedAt());
                statement.setInt(15, record.durationSeconds());
                statement.setLong(16, record.createdAt());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to record duel match.", exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<DuelStats>> getTopWins(int limit) {
        return loadLeaderboard("SELECT * FROM duel_players ORDER BY wins DESC, best_streak DESC LIMIT ?", limit);
    }

    @Override
    public CompletableFuture<List<DuelStats>> getTopStreak(int limit) {
        return loadLeaderboard("SELECT * FROM duel_players ORDER BY best_streak DESC, wins DESC LIMIT ?", limit);
    }

    private CompletableFuture<List<DuelStats>> loadLeaderboard(String sql, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<DuelStats> results = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        DuelStats stats = new DuelStats(UUID.fromString(resultSet.getString("uuid")), resultSet.getString("name"));
                        stats.setWins(resultSet.getInt("wins"));
                        stats.setLosses(resultSet.getInt("losses"));
                        stats.setDraws(resultSet.getInt("draws"));
                        stats.setQuits(resultSet.getInt("quits"));
                        stats.setCurrentStreak(resultSet.getInt("current_streak"));
                        stats.setBestStreak(resultSet.getInt("best_streak"));
                        stats.setTotalMatches(resultSet.getInt("total_matches"));
                        stats.setTotalDurationSeconds(resultSet.getInt("total_duration_seconds"));
                        stats.setLastMode(resultSet.getString("last_mode"));
                        stats.setLastMatchAt(resultSet.getLong("last_match_at"));
                        stats.setCreatedAt(resultSet.getLong("created_at"));
                        stats.setUpdatedAt(resultSet.getLong("updated_at"));
                        results.add(stats);
                    }
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load duel leaderboard.", exception);
            }
            return results;
        }, executor);
    }
}

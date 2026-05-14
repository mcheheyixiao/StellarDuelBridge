package org.stellarvan.stellarDuelBridge.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MySQLStorageProvider implements StorageProvider {

    private static <T> CompletableFuture<T> unsupported() {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("MySQL storage is not implemented in V1."));
    }

    @Override
    public void init() {
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<DuelStats> loadStats(UUID uuid, String name) {
        return unsupported();
    }

    @Override
    public CompletableFuture<Void> saveStats(DuelStats stats) {
        return unsupported();
    }

    @Override
    public CompletableFuture<Void> recordMatch(MatchRecord record) {
        return unsupported();
    }

    @Override
    public CompletableFuture<List<DuelStats>> getTopWins(int limit) {
        return unsupported();
    }

    @Override
    public CompletableFuture<List<DuelStats>> getTopStreak(int limit) {
        return unsupported();
    }
}

package org.stellarvan.stellarDuelBridge.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    void init();

    void close();

    CompletableFuture<DuelStats> loadStats(UUID uuid, String name);

    CompletableFuture<Void> saveStats(DuelStats stats);

    CompletableFuture<Void> recordMatch(MatchRecord record);

    CompletableFuture<List<DuelStats>> getTopWins(int limit);

    CompletableFuture<List<DuelStats>> getTopStreak(int limit);

    default CompletableFuture<Integer> countPairMatchesSince(UUID playerOne, UUID playerTwo, long sinceEpochSecond) {
        return CompletableFuture.completedFuture(0);
    }

    default CompletableFuture<Integer> getDailyPositiveHonor(UUID playerId, long dayStartEpochSecond, long dayEndEpochSecond) {
        return CompletableFuture.completedFuture(0);
    }
}

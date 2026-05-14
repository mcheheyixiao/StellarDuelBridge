package org.stellarvan.stellarDuelBridge.storage;

public record MatchRecord(
    String arenaId,
    String mode,
    String playerOneUuid,
    String playerOneName,
    String playerTwoUuid,
    String playerTwoName,
    String winnerUuid,
    String winnerName,
    String loserUuid,
    String loserName,
    String result,
    String endReason,
    long startedAt,
    long endedAt,
    int durationSeconds,
    long createdAt
) {
}

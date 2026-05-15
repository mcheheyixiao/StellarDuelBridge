package org.stellarvan.stellarDuelBridge.storage;

import java.util.UUID;

public final class DuelStats {

    private final UUID uuid;
    private String name;
    private int wins;
    private int losses;
    private int draws;
    private int quits;
    private int currentStreak;
    private int bestStreak;
    private int honor;
    private int prestige;
    private int totalMatches;
    private int totalDurationSeconds;
    private String lastMode;
    private long lastMatchAt;
    private long createdAt;
    private long updatedAt;

    public DuelStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        long now = System.currentTimeMillis() / 1000L;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getQuits() {
        return quits;
    }

    public void setQuits(int quits) {
        this.quits = quits;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public int getHonor() {
        return honor;
    }

    public void setHonor(int honor) {
        this.honor = honor;
    }

    public int getPrestige() {
        return prestige;
    }

    public void setPrestige(int prestige) {
        this.prestige = prestige;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public void setTotalDurationSeconds(int totalDurationSeconds) {
        this.totalDurationSeconds = totalDurationSeconds;
    }

    public String getLastMode() {
        return lastMode;
    }

    public void setLastMode(String lastMode) {
        this.lastMode = lastMode;
    }

    public long getLastMatchAt() {
        return lastMatchAt;
    }

    public void setLastMatchAt(long lastMatchAt) {
        this.lastMatchAt = lastMatchAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

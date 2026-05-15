package org.stellarvan.stellarDuelBridge.placeholder;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.storage.DuelStats;

public final class StellarDuelExpansion extends PlaceholderExpansion {

    private static final long CACHE_TTL_MILLIS = 5000L;

    private final StellarDuelBridge plugin;
    private final Map<UUID, CachedStats> cache = new ConcurrentHashMap<>();

    public StellarDuelExpansion(StellarDuelBridge plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "stellarduel";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "0";
        }
        UUID playerId = player.getUniqueId();
        String playerName = player.getName() == null ? playerId.toString() : player.getName();
        DuelStats stats = getCachedStats(playerId, playerName);
        String identifier = params.toLowerCase(Locale.ROOT);
        return switch (identifier) {
            case "honor" -> Integer.toString(stats.getHonor());
            case "prestige" -> Integer.toString(stats.getPrestige());
            case "wins" -> Integer.toString(stats.getWins());
            case "losses" -> Integer.toString(stats.getLosses());
            case "draws" -> Integer.toString(stats.getDraws());
            case "total" -> Integer.toString(stats.getTotalMatches());
            case "streak" -> Integer.toString(stats.getCurrentStreak());
            case "best_streak" -> Integer.toString(stats.getBestStreak());
            case "winrate" -> formatWinRate(stats.getWins(), stats.getLosses(), stats.getDraws());
            default -> null;
        };
    }

    private DuelStats getCachedStats(UUID playerId, String playerName) {
        long now = System.currentTimeMillis();
        CachedStats cached = cache.get(playerId);
        if (cached == null) {
            DuelStats initial = new DuelStats(playerId, playerName);
            cache.put(playerId, new CachedStats(initial, now, true));
            requestRefresh(playerId, playerName);
            return initial;
        }
        if (now - cached.loadedAtMillis() > CACHE_TTL_MILLIS && !cached.refreshing()) {
            cache.put(playerId, new CachedStats(cached.stats(), cached.loadedAtMillis(), true));
            requestRefresh(playerId, playerName);
        }
        return cached.stats();
    }

    private void requestRefresh(UUID playerId, String playerName) {
        plugin.getStorageProvider().loadStats(playerId, playerName).whenComplete((stats, throwable) -> {
            if (throwable != null || stats == null) {
                CachedStats previous = cache.get(playerId);
                if (previous != null) {
                    cache.put(playerId, new CachedStats(previous.stats(), previous.loadedAtMillis(), false));
                }
                return;
            }
            cache.put(playerId, new CachedStats(stats, System.currentTimeMillis(), false));
        });
    }

    private String formatWinRate(int wins, int losses, int draws) {
        int total = wins + losses + draws;
        if (total <= 0) {
            return "0.0%";
        }
        double rate = wins * 100.0D / total;
        return String.format(Locale.US, "%.1f%%", rate);
    }

    private record CachedStats(DuelStats stats, long loadedAtMillis, boolean refreshing) {
    }
}

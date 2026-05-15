package org.stellarvan.stellarDuelBridge.compat;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public final class DuelDeathMarker {

    public static final String DUEL_KILL_KEY = "stellar_duel_kill";
    public static final String DUEL_DEATH_KEY = "stellar_duel_death";
    private static final long MARK_TTL_TICKS = 80L;

    private DuelDeathMarker() {
    }

    public static void markDuelDeath(Plugin plugin, Player victim, Player killer) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(victim, "victim");
        markPlayer(plugin, victim);
        if (killer != null) {
            markPlayer(plugin, killer);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            clear(plugin, victim);
            if (killer != null) {
                clear(plugin, killer);
            }
        }, MARK_TTL_TICKS);
    }

    public static boolean isDuelDeath(Plugin plugin, Player player) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        return hasMarker(plugin, player, DUEL_DEATH_KEY) || hasMarker(plugin, player, DUEL_KILL_KEY);
    }

    public static void clear(Plugin plugin, Player player) {
        Objects.requireNonNull(plugin, "plugin");
        if (player == null) {
            return;
        }
        player.removeMetadata(DUEL_KILL_KEY, plugin);
        player.removeMetadata(DUEL_DEATH_KEY, plugin);
    }

    private static void markPlayer(Plugin plugin, Player player) {
        player.setMetadata(DUEL_KILL_KEY, new FixedMetadataValue(plugin, true));
        player.setMetadata(DUEL_DEATH_KEY, new FixedMetadataValue(plugin, true));
    }

    private static boolean hasMarker(Plugin plugin, Player player, String key) {
        for (MetadataValue metadataValue : player.getMetadata(key)) {
            if (metadataValue != null && metadataValue.getOwningPlugin() == plugin && metadataValue.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}

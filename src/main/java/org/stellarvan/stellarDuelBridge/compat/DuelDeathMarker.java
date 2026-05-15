package org.stellarvan.stellarDuelBridge.compat;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class DuelDeathMarker {

    private static final String DUEL_KILL_KEY_LITERAL = "stellarduelbridge:stellar_duel_kill";
    private static final String DUEL_DEATH_KEY_LITERAL = "stellarduelbridge:stellar_duel_death";
    private static volatile NamespacedKey DUEL_KILL_KEY;
    private static volatile NamespacedKey DUEL_DEATH_KEY;
    private static final byte MARKED = (byte) 1;
    private static final long MARK_TTL_TICKS = 80L;

    private DuelDeathMarker() {
    }

    public static void markDuelDeath(Plugin plugin, Player victim, Player killer) {
        ensureKeysInitialized(plugin);
        Objects.requireNonNull(victim, "victim");
        markKill(victim);
        markDeath(victim);
        if (killer != null) {
            markKill(killer);
            markDeath(killer);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            clear(victim);
            if (killer != null) {
                clear(killer);
            }
        }, MARK_TTL_TICKS);
    }

    public static boolean isDuelDeath(Plugin plugin, Player player) {
        ensureKeysInitialized(plugin);
        Objects.requireNonNull(player, "player");
        return hasMarker(player, requireDeathKey()) || isDuelKill(player);
    }

    public static void markKill(Player player) {
        Objects.requireNonNull(player, "player");
        player.getPersistentDataContainer().set(requireKillKey(), PersistentDataType.BYTE, MARKED);
    }

    public static void markDeath(Player player) {
        Objects.requireNonNull(player, "player");
        player.getPersistentDataContainer().set(requireDeathKey(), PersistentDataType.BYTE, MARKED);
    }

    public static boolean isDuelKill(Player player) {
        Objects.requireNonNull(player, "player");
        return hasMarker(player, requireKillKey());
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        PersistentDataContainer dataContainer = player.getPersistentDataContainer();
        dataContainer.remove(requireKillKey());
        dataContainer.remove(requireDeathKey());
    }

    private static boolean hasMarker(Player player, NamespacedKey key) {
        Byte marker = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return marker != null && marker == MARKED;
    }

    private static void ensureKeysInitialized(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        ensureKeysInitialized();
    }

    private static void ensureKeysInitialized() {
        if (DUEL_KILL_KEY != null && DUEL_DEATH_KEY != null) {
            return;
        }
        synchronized (DuelDeathMarker.class) {
            if (DUEL_KILL_KEY == null) {
                DUEL_KILL_KEY = Objects.requireNonNull(NamespacedKey.fromString(DUEL_KILL_KEY_LITERAL), "duel kill key");
            }
            if (DUEL_DEATH_KEY == null) {
                DUEL_DEATH_KEY = Objects.requireNonNull(NamespacedKey.fromString(DUEL_DEATH_KEY_LITERAL), "duel death key");
            }
        }
    }

    private static NamespacedKey requireKillKey() {
        ensureKeysInitialized();
        return Objects.requireNonNull(DUEL_KILL_KEY, "duel kill key");
    }

    private static NamespacedKey requireDeathKey() {
        ensureKeysInitialized();
        return Objects.requireNonNull(DUEL_DEATH_KEY, "duel death key");
    }
}

package org.stellarvan.stellarDuelBridge.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.util.LocationSerializer;

public final class ArenaManager {

    private final StellarDuelBridge plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final ArenaSelector selector = new ArenaSelector();
    private long cooldownAfterMatchSeconds;
    private boolean avoidRepeat;
    private String lastArenaId;

    public ArenaManager(StellarDuelBridge plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    public void loadArenas() {
        this.arenas.clear();
        FileConfiguration config = configManager.getArenasConfig();
        this.cooldownAfterMatchSeconds = Math.max(0L, config.getLong("arenas.cooldown-after-match-seconds", 15L));
        this.avoidRepeat = config.getBoolean("arenas.avoid-repeat", true);
        ConfigurationSection list = config.getConfigurationSection("arenas.list");
        if (list == null) {
            return;
        }
        for (String id : list.getKeys(false)) {
            ConfigurationSection arenaSection = list.getConfigurationSection(id);
            Arena arena = new Arena(id);
            arena.setEnabled(arenaSection.getBoolean("enabled", false));
            arena.setWorldName(arenaSection.getString("world"));
            arena.setRegionName(arenaSection.getString("region", id));
            arena.setSpawn1(LocationSerializer.read(arenaSection.getConfigurationSection("spawn-1"), arena.getWorldName()));
            arena.setSpawn2(LocationSerializer.read(arenaSection.getConfigurationSection("spawn-2"), arena.getWorldName()));
            arena.setSpectator(LocationSerializer.read(arenaSection.getConfigurationSection("spectator"), arena.getWorldName()));
            arenas.put(id.toLowerCase(), arena);
        }
    }

    public void saveArenas() {
        FileConfiguration config = configManager.getArenasConfig();
        config.set("arenas.list", null);
        ConfigurationSection listSection = config.createSection("arenas.list");
        arenas.values().stream().sorted(Comparator.comparing(Arena::getId)).forEach(arena -> {
            ConfigurationSection arenaSection = listSection.createSection(arena.getId());
            arenaSection.set("enabled", arena.isEnabled());
            arenaSection.set("world", arena.getWorldName());
            arenaSection.set("region", arena.getRegionName());
            if (arena.getSpawn1() != null) {
                LocationSerializer.write(arenaSection.createSection("spawn-1"), arena.getSpawn1());
            }
            if (arena.getSpawn2() != null) {
                LocationSerializer.write(arenaSection.createSection("spawn-2"), arena.getSpawn2());
            }
            if (arena.getSpectator() != null) {
                LocationSerializer.write(arenaSection.createSection("spectator"), arena.getSpectator());
            }
        });
        configManager.saveArenasConfig();
    }

    public Arena createArena(String id, Location referenceLocation) {
        String key = id.toLowerCase();
        if (arenas.containsKey(key)) {
            return null;
        }
        Arena arena = new Arena(id);
        if (referenceLocation != null && referenceLocation.getWorld() != null) {
            arena.setWorldName(referenceLocation.getWorld().getName());
        }
        arena.setRegionName(id);
        arenas.put(key, arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String id) {
        Arena removed = arenas.remove(id.toLowerCase());
        if (removed == null) {
            return false;
        }
        saveArenas();
        return true;
    }

    public Arena getArena(String id) {
        if (id == null) {
            return null;
        }
        return arenas.get(id.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public int getArenaCount() {
        return arenas.size();
    }

    public int getEnabledArenaCount() {
        return (int) arenas.values().stream().filter(Arena::isEnabled).count();
    }

    public boolean setSpawn(String id, int index, Location location) {
        Arena arena = getArena(id);
        if (arena == null) {
            return false;
        }
        arena.setWorldName(location.getWorld() == null ? arena.getWorldName() : location.getWorld().getName());
        if (index == 1) {
            arena.setSpawn1(location);
        } else {
            arena.setSpawn2(location);
        }
        saveArenas();
        return true;
    }

    public boolean setSpectator(String id, Location location) {
        Arena arena = getArena(id);
        if (arena == null) {
            return false;
        }
        arena.setWorldName(location.getWorld() == null ? arena.getWorldName() : location.getWorld().getName());
        arena.setSpectator(location);
        saveArenas();
        return true;
    }

    public boolean enableArena(String id) {
        Arena arena = getArena(id);
        if (arena == null) {
            return false;
        }
        arena.setEnabled(true);
        saveArenas();
        return true;
    }

    public boolean disableArena(String id) {
        Arena arena = getArena(id);
        if (arena == null) {
            return false;
        }
        arena.setEnabled(false);
        saveArenas();
        return true;
    }

    public Arena getAvailableArena() {
        List<Arena> candidates = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.getState() == ArenaState.AVAILABLE) {
                candidates.add(arena);
            }
        }
        Arena selected = selector.selectRandom(candidates, lastArenaId, avoidRepeat);
        if (selected != null) {
            lastArenaId = selected.getId();
        }
        return selected;
    }

    public void markOccupied(String id) {
        Arena arena = getArena(id);
        if (arena != null) {
            arena.setOccupied(true);
            arena.setLastUsedAt(System.currentTimeMillis() / 1000L);
        }
    }

    public void releaseArena(String id) {
        Arena arena = getArena(id);
        if (arena != null) {
            arena.setOccupied(false);
            arena.setCooldownUntil((System.currentTimeMillis() / 1000L) + cooldownAfterMatchSeconds);
        }
    }

    public void releaseAll() {
        arenas.values().forEach(arena -> {
            arena.setOccupied(false);
            arena.setCooldownUntil(0L);
        });
    }
}

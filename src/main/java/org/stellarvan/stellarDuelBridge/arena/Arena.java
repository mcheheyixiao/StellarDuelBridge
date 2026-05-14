package org.stellarvan.stellarDuelBridge.arena;

import java.time.Instant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Arena {

    private final String id;
    private boolean enabled;
    private String worldName;
    private String regionName;
    private Location spawn1;
    private Location spawn2;
    private Location spectator;
    private boolean occupied;
    private long lastUsedAt;
    private long cooldownUntil;

    public Arena(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Location getSpawn1() {
        return spawn1 == null ? null : spawn1.clone();
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1 == null ? null : spawn1.clone();
    }

    public Location getSpawn2() {
        return spawn2 == null ? null : spawn2.clone();
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2 == null ? null : spawn2.clone();
    }

    public Location getSpectator() {
        return spectator == null ? null : spectator.clone();
    }

    public void setSpectator(Location spectator) {
        this.spectator = spectator == null ? null : spectator.clone();
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public long getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(long lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public long getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public World getWorld() {
        return worldName == null ? null : Bukkit.getWorld(worldName);
    }

    public boolean isReady() {
        return worldName != null && !worldName.isBlank() && spawn1 != null && spawn2 != null;
    }

    public ArenaState getState() {
        if (!enabled) {
            return ArenaState.DISABLED;
        }
        if (!isReady()) {
            return ArenaState.INCOMPLETE;
        }
        if (getWorld() == null) {
            return ArenaState.WORLD_UNAVAILABLE;
        }
        if (occupied) {
            return ArenaState.OCCUPIED;
        }
        if (cooldownUntil > Instant.now().getEpochSecond()) {
            return ArenaState.COOLDOWN;
        }
        return ArenaState.AVAILABLE;
    }
}

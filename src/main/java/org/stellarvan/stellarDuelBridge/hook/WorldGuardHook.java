package org.stellarvan.stellarDuelBridge.hook;

public final class WorldGuardHook {

    private final boolean available;

    public WorldGuardHook(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

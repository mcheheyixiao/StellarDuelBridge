package org.stellarvan.stellarDuelBridge.hook;

public final class PvPManagerHook {

    private final boolean available;

    public PvPManagerHook(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

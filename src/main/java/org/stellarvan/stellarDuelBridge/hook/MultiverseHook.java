package org.stellarvan.stellarDuelBridge.hook;

public final class MultiverseHook {

    private final boolean available;

    public MultiverseHook(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

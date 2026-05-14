package org.stellarvan.stellarDuelBridge.hook;

public final class PlaceholderAPIHook {

    private final boolean available;

    public PlaceholderAPIHook(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

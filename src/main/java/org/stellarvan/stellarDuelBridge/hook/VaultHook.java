package org.stellarvan.stellarDuelBridge.hook;

public final class VaultHook {

    private final boolean available;

    public VaultHook(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

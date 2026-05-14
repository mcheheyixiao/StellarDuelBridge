package org.stellarvan.stellarDuelBridge.duel;

public enum DuelMode {
    REAL_GEAR,
    FAIR_KIT,
    EMPTY_RITUAL;

    public static DuelMode fromButtonKey(String key) {
        return switch (key.toLowerCase()) {
            case "real-gear" -> REAL_GEAR;
            case "fair-kit" -> FAIR_KIT;
            case "empty-ritual" -> EMPTY_RITUAL;
            default -> null;
        };
    }
}

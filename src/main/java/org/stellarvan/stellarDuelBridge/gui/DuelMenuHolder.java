package org.stellarvan.stellarDuelBridge.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.stellarvan.stellarDuelBridge.duel.DuelMode;

public final class DuelMenuHolder implements InventoryHolder {

    private final DuelMenuType menuType;
    private final UUID sessionId;
    private final UUID viewerId;
    private DuelMode selectedMode;
    private Inventory inventory;

    public DuelMenuHolder(DuelMenuType menuType, UUID sessionId, UUID viewerId, DuelMode selectedMode) {
        this.menuType = menuType;
        this.sessionId = sessionId;
        this.viewerId = viewerId;
        this.selectedMode = selectedMode;
    }

    public DuelMenuType getMenuType() {
        return menuType;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public DuelMode getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(DuelMode selectedMode) {
        this.selectedMode = selectedMode;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

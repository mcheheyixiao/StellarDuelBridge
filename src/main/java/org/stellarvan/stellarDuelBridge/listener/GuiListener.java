package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.gui.DuelMenuHolder;

public final class GuiListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public GuiListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DuelMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        duelSessionManager.handleMenuClick(player, holder, event.getRawSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof DuelMenuHolder holder)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            duelSessionManager.handleMenuClose(player, holder);
        }
    }
}

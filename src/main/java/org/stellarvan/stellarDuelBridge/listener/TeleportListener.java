package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class TeleportListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public TeleportListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!duelSessionManager.isInDuel(event.getPlayer().getUniqueId())) {
            return;
        }
        if (duelSessionManager.isInternalTeleport(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
            || event.getCause() == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            return;
        }
        event.setCancelled(true);
    }
}

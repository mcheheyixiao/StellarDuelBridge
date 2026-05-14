package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class MoveListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public MoveListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!duelSessionManager.isCountdownFrozen(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        Location locked = from.clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        event.setTo(locked);
    }
}

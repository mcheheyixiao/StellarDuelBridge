package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class WorldChangeListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public WorldChangeListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (duelSessionManager.isInDuel(event.getPlayer().getUniqueId()) && !duelSessionManager.isInternalTeleport(event.getPlayer().getUniqueId())) {
            duelSessionManager.handleIllegalWorldChange(event.getPlayer());
        }
    }
}

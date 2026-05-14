package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class QuitListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public QuitListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        duelSessionManager.handlePlayerQuit(event.getPlayer());
    }
}

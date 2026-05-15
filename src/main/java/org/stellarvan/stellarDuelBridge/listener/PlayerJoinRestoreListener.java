package org.stellarvan.stellarDuelBridge.listener;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.snapshot.SnapshotService;

public final class PlayerJoinRestoreListener implements Listener {

    private final SnapshotService snapshotService;
    private final DuelSessionManager duelSessionManager;

    public PlayerJoinRestoreListener(SnapshotService snapshotService, DuelSessionManager duelSessionManager) {
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService");
        this.duelSessionManager = Objects.requireNonNull(duelSessionManager, "duelSessionManager");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (snapshotService.needsDeferredRestore(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(duelSessionManager.getPlugin(), () -> duelSessionManager.applyDeferredRestore(event.getPlayer()), 1L);
        }
    }
}

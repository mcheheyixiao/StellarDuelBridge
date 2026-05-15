package org.stellarvan.stellarDuelBridge.listener;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.snapshot.SnapshotService;

public final class RespawnListener implements Listener {

    private final SnapshotService snapshotService;
    private final DuelSessionManager duelSessionManager;

    public RespawnListener(SnapshotService snapshotService, DuelSessionManager duelSessionManager) {
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService");
        this.duelSessionManager = Objects.requireNonNull(duelSessionManager, "duelSessionManager");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (snapshotService.needsDeferredRestore(event.getPlayer().getUniqueId())) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(duelSessionManager.getPlugin(), () -> duelSessionManager.applyDeferredRestore(player), 1L);
        }
    }

}

package org.stellarvan.stellarDuelBridge.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class DurabilityListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public DurabilityListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = Objects.requireNonNull(duelSessionManager, "duelSessionManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (!duelSessionManager.isInDuel(event.getPlayer().getUniqueId())) {
            return;
        }
        if (duelSessionManager.getPlugin().getConfigManager().getDuelSettings().combatSettings().allowDurabilityLoss()) {
            return;
        }
        event.setCancelled(true);
    }
}

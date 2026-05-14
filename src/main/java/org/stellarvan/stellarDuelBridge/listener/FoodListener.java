package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class FoodListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public FoodListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && duelSessionManager.isCountdownFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}

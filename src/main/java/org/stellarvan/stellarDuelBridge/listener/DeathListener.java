package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class DeathListener implements Listener {

    private final DuelSessionManager duelSessionManager;
    private final ConfigManager configManager;

    public DeathListener(DuelSessionManager duelSessionManager, ConfigManager configManager) {
        this.duelSessionManager = duelSessionManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!duelSessionManager.isInDuel(player.getUniqueId())) {
            return;
        }
        if (configManager.getDuelSettings().combatSettings().keepInventory()) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        }
        if (configManager.getDuelSettings().combatSettings().clearDrops()) {
            event.getDrops().clear();
        }
        if (configManager.getDuelSettings().combatSettings().clearExpDrops()) {
            event.setDroppedExp(0);
        }
        event.deathMessage(null);
        duelSessionManager.handlePlayerDeath(player);
    }
}

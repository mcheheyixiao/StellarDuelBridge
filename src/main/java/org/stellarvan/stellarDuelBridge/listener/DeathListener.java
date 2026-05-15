package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.compat.DuelDeathMarker;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSession;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class DeathListener implements Listener {

    private final StellarDuelBridge plugin;
    private final DuelSessionManager duelSessionManager;
    private final ConfigManager configManager;

    public DeathListener(StellarDuelBridge plugin, DuelSessionManager duelSessionManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.duelSessionManager = duelSessionManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!duelSessionManager.isInDuel(player.getUniqueId())) {
            return;
        }
        Player killer = resolveOpponent(player);
        DuelDeathMarker.markDuelDeath(plugin, player, killer);
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

    private Player resolveOpponent(Player victim) {
        DuelSession session = duelSessionManager.getSession(victim.getUniqueId());
        if (session == null) {
            return victim.getKiller();
        }
        if (session.getOpponent(victim.getUniqueId()) == null) {
            return victim.getKiller();
        }
        return Bukkit.getPlayer(session.getOpponent(victim.getUniqueId()));
    }
}

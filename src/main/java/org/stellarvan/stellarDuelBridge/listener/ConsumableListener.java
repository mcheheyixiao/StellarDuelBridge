package org.stellarvan.stellarDuelBridge.listener;

import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class ConsumableListener implements Listener {

    private final DuelSessionManager duelSessionManager;
    private final MessageManager messageManager;

    public ConsumableListener(DuelSessionManager duelSessionManager, MessageManager messageManager) {
        this.duelSessionManager = Objects.requireNonNull(duelSessionManager, "duelSessionManager");
        this.messageManager = Objects.requireNonNull(messageManager, "messageManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!duelSessionManager.isInDuel(player.getUniqueId())) {
            return;
        }
        if (duelSessionManager.getPlugin().getConfigManager().getDuelSettings().combatSettings().allowConsumables()) {
            return;
        }
        if (!isBlockedConsumable(event.getItem().getType())) {
            return;
        }
        event.setCancelled(true);
        messageManager.sendMessage(player, "errors.consumables-disabled");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion thrownPotion)) {
            return;
        }
        if (!(thrownPotion.getShooter() instanceof Player player)) {
            return;
        }
        if (!duelSessionManager.isInDuel(player.getUniqueId())) {
            return;
        }
        if (duelSessionManager.getPlugin().getConfigManager().getDuelSettings().combatSettings().allowConsumables()) {
            return;
        }
        Material type = thrownPotion.getItem().getType();
        if (type != Material.SPLASH_POTION && type != Material.LINGERING_POTION) {
            return;
        }
        event.setCancelled(true);
        messageManager.sendMessage(player, "errors.consumables-disabled");
    }

    private boolean isBlockedConsumable(Material material) {
        return material.isEdible() || material == Material.POTION;
    }
}

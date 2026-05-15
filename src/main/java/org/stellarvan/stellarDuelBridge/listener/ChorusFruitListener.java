package org.stellarvan.stellarDuelBridge.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class ChorusFruitListener implements Listener {

    private final DuelSessionManager duelSessionManager;
    private final MessageManager messageManager;

    public ChorusFruitListener(DuelSessionManager duelSessionManager, MessageManager messageManager) {
        this.duelSessionManager = Objects.requireNonNull(duelSessionManager, "duelSessionManager");
        this.messageManager = Objects.requireNonNull(messageManager, "messageManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT) {
            return;
        }
        if (!duelSessionManager.isInDuel(event.getPlayer().getUniqueId())) {
            return;
        }
        if (duelSessionManager.getPlugin().getConfigManager().getDuelSettings().combatSettings().allowChorusFruit()) {
            return;
        }
        event.setCancelled(true);
        messageManager.sendMessage(event.getPlayer(), "errors.chorus-fruit-disabled");
    }
}

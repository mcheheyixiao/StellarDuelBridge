package org.stellarvan.stellarDuelBridge.listener;

import java.util.Locale;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.util.PermissionNodes;

public final class CommandBlockListener implements Listener {

    private final DuelSessionManager duelSessionManager;
    private final ConfigManager configManager;

    public CommandBlockListener(DuelSessionManager duelSessionManager, ConfigManager configManager) {
        this.duelSessionManager = duelSessionManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!duelSessionManager.isInDuel(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getPlayer().hasPermission(PermissionNodes.BYPASS_COMMAND_BLOCK)) {
            return;
        }
        String command = event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage();
        String root = command.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (configManager.getDuelSettings().combatSettings().blockedCommands().stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(root::equals)) {
            event.setCancelled(true);
            duelSessionManager.sendError(event.getPlayer(), "errors.command-blocked");
        }
    }
}

package org.stellarvan.stellarDuelBridge.command;

import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.arena.ArenaManager;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.hook.HookManager;
import org.stellarvan.stellarDuelBridge.storage.StorageProvider;

public record CommandContext(
    StellarDuelBridge plugin,
    ConfigManager configManager,
    MessageManager messageManager,
    HookManager hookManager,
    ArenaManager arenaManager,
    DuelSessionManager duelSessionManager,
    StorageProvider storageProvider
) {
}

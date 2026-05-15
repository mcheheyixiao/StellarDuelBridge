package org.stellarvan.stellarDuelBridge;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import org.stellarvan.stellarDuelBridge.arena.ArenaManager;
import org.stellarvan.stellarDuelBridge.command.CommandContext;
import org.stellarvan.stellarDuelBridge.command.DuelAdminCommand;
import org.stellarvan.stellarDuelBridge.command.DuelCommand;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;
import org.stellarvan.stellarDuelBridge.hook.HookManager;
import org.stellarvan.stellarDuelBridge.listener.CommandBlockListener;
import org.stellarvan.stellarDuelBridge.listener.CombatListener;
import org.stellarvan.stellarDuelBridge.listener.ConsumableListener;
import org.stellarvan.stellarDuelBridge.listener.ChorusFruitListener;
import org.stellarvan.stellarDuelBridge.listener.DeathListener;
import org.stellarvan.stellarDuelBridge.listener.DuelDeathCompatibilityListener;
import org.stellarvan.stellarDuelBridge.listener.DurabilityListener;
import org.stellarvan.stellarDuelBridge.listener.EnderPearlListener;
import org.stellarvan.stellarDuelBridge.listener.FoodListener;
import org.stellarvan.stellarDuelBridge.listener.GuiListener;
import org.stellarvan.stellarDuelBridge.listener.MoveListener;
import org.stellarvan.stellarDuelBridge.listener.PlayerJoinRestoreListener;
import org.stellarvan.stellarDuelBridge.listener.QuitListener;
import org.stellarvan.stellarDuelBridge.listener.RespawnListener;
import org.stellarvan.stellarDuelBridge.listener.TeleportListener;
import org.stellarvan.stellarDuelBridge.listener.WorldChangeListener;
import org.stellarvan.stellarDuelBridge.placeholder.StellarDuelExpansion;
import org.stellarvan.stellarDuelBridge.snapshot.SnapshotService;
import org.stellarvan.stellarDuelBridge.storage.SQLiteStorageProvider;
import org.stellarvan.stellarDuelBridge.storage.StorageProvider;

public final class StellarDuelBridge extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private HookManager hookManager;
    private StorageProvider storageProvider;
    private ArenaManager arenaManager;
    private SnapshotService snapshotService;
    private DuelSessionManager duelSessionManager;
    private StellarDuelExpansion stellarDuelExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledResource("gui.yml");
        saveBundledResource("messages.yml");
        saveBundledResource("arenas.yml");

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.hookManager = new HookManager(this);
        this.snapshotService = new SnapshotService(this);
        this.storageProvider = createStorageProvider();
        this.storageProvider.init();
        this.arenaManager = new ArenaManager(this, configManager, messageManager);
        this.arenaManager.loadArenas();
        this.duelSessionManager = new DuelSessionManager(
            this,
            configManager,
            messageManager,
            hookManager,
            arenaManager,
            snapshotService,
            storageProvider
        );
        registerPlaceholderExpansion();

        registerCommands();
        registerListeners();
        recoverPendingRestoresForOnlinePlayers();
        logStartupSummary();
    }

    @Override
    public void onDisable() {
        if (duelSessionManager != null) {
            duelSessionManager.shutdown();
        }
        if (stellarDuelExpansion != null) {
            stellarDuelExpansion.unregister();
            stellarDuelExpansion = null;
        }
        if (snapshotService != null) {
            snapshotService.shutdown();
        }
        if (arenaManager != null) {
            arenaManager.releaseAll();
        }
        if (storageProvider != null) {
            storageProvider.close();
        }
        getLogger().info("StellarDuelBridge disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public SnapshotService getSnapshotService() {
        return snapshotService;
    }

    public DuelSessionManager getDuelSessionManager() {
        return duelSessionManager;
    }

    private void saveBundledResource(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Unable to create plugin data folder.");
        }
        java.io.File target = new java.io.File(getDataFolder(), resourcePath);
        if (!target.exists()) {
            saveResource(resourcePath, false);
        }
    }

    private StorageProvider createStorageProvider() {
        String storageType = configManager.getDuelSettings().storageType();
        if ("MYSQL".equalsIgnoreCase(storageType)) {
            if (configManager.getDuelSettings().mysqlFallbackToSqlite()) {
                getLogger().warning("MySQL is not implemented in V1. Falling back to SQLite.");
                return new SQLiteStorageProvider(this, configManager);
            }
            throw new IllegalStateException("MySQL storage is not implemented in V1 and fallback is disabled.");
        }
        return new SQLiteStorageProvider(this, configManager);
    }

    private void registerCommands() {
        CommandContext context = new CommandContext(
            this,
            configManager,
            messageManager,
            hookManager,
            arenaManager,
            duelSessionManager,
            storageProvider
        );
        DuelCommand duelCommand = new DuelCommand(context);
        DuelAdminCommand duelAdminCommand = new DuelAdminCommand(context);
        registerCommand("duel", duelCommand);
        registerCommand("dueladmin", duelAdminCommand);
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command not defined in plugin.yml: " + name);
        }
        if (executor instanceof DuelCommand duelCommand) {
            command.setExecutor(duelCommand);
            command.setTabCompleter(duelCommand);
        } else if (executor instanceof DuelAdminCommand duelAdminCommand) {
            command.setExecutor(duelAdminCommand);
            command.setTabCompleter(duelAdminCommand);
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this, duelSessionManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new DuelDeathCompatibilityListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new TeleportListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new EnderPearlListener(duelSessionManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new ChorusFruitListener(duelSessionManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new ConsumableListener(duelSessionManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new DurabilityListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlockListener(duelSessionManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new WorldChangeListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new FoodListener(duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new RespawnListener(snapshotService, duelSessionManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinRestoreListener(snapshotService, duelSessionManager), this);
    }

    private void registerPlaceholderExpansion() {
        if (!hookManager.getPlaceholderAPIHook().isAvailable()) {
            return;
        }
        if (!getConfig().getBoolean("integration.placeholderapi.enabled", true)) {
            return;
        }
        this.stellarDuelExpansion = new StellarDuelExpansion(this);
        if (stellarDuelExpansion.register()) {
            getLogger().info("Registered PlaceholderAPI expansion: %stellarduel_*%");
        } else {
            getLogger().warning("Failed to register PlaceholderAPI expansion.");
            stellarDuelExpansion = null;
        }
    }

    private void recoverPendingRestoresForOnlinePlayers() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (UUID playerId : snapshotService.getPendingRestorePlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                duelSessionManager.applyDeferredRestore(player);
            }
        }, 20L);
    }

    private void logStartupSummary() {
        getLogger().info("StellarDuelBridge v" + getPluginMeta().getVersion() + " enabled on Paper " + getServer().getMinecraftVersion());
        getLogger().info("Hooks: PvPManager=" + hookManager.getPvPManagerHook().isAvailable()
            + ", Multiverse-Core=" + hookManager.getMultiverseHook().isAvailable()
            + ", WorldGuard=" + hookManager.getWorldGuardHook().isAvailable()
            + ", PlaceholderAPI=" + hookManager.getPlaceholderAPIHook().isAvailable());
        getLogger().info("Storage=" + configManager.getDuelSettings().storageType()
            + ", Arenas=" + arenaManager.getArenaCount()
            + ", Enabled=" + arenaManager.getEnabledArenaCount());
    }
}

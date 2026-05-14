package org.stellarvan.stellarDuelBridge.hook;

import org.bukkit.Bukkit;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;

public final class HookManager {

    private final StellarDuelBridge plugin;
    private final PvPManagerHook pvPManagerHook;
    private final MultiverseHook multiverseHook;
    private final WorldGuardHook worldGuardHook;
    private final VaultHook vaultHook;
    private final PlaceholderAPIHook placeholderAPIHook;

    public HookManager(StellarDuelBridge plugin) {
        this.plugin = plugin;
        this.pvPManagerHook = new PvPManagerHook(isPluginEnabled("PvPManager"));
        this.multiverseHook = new MultiverseHook(isPluginEnabled("Multiverse-Core"));
        this.worldGuardHook = new WorldGuardHook(isPluginEnabled("WorldGuard"));
        this.vaultHook = new VaultHook(isPluginEnabled("Vault"));
        this.placeholderAPIHook = new PlaceholderAPIHook(isPluginEnabled("PlaceholderAPI"));
        logHookStatus();
    }

    public PvPManagerHook getPvPManagerHook() {
        return pvPManagerHook;
    }

    public MultiverseHook getMultiverseHook() {
        return multiverseHook;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }

    private boolean isPluginEnabled(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    private void logHookStatus() {
        plugin.getLogger().info("Hook detection: PvPManager=" + pvPManagerHook.isAvailable()
            + ", Multiverse-Core=" + multiverseHook.isAvailable()
            + ", WorldGuard=" + worldGuardHook.isAvailable()
            + ", Vault=" + vaultHook.isAvailable()
            + ", PlaceholderAPI=" + placeholderAPIHook.isAvailable());
    }
}

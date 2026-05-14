package org.stellarvan.stellarDuelBridge.snapshot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.config.DuelSettings;
import org.stellarvan.stellarDuelBridge.duel.DuelMode;

public final class SnapshotService {

    private final StellarDuelBridge plugin;
    private final Map<UUID, DeferredRestore> deferredRestores = new ConcurrentHashMap<>();

    public SnapshotService(StellarDuelBridge plugin) {
        this.plugin = plugin;
    }

    public PlayerSnapshot capture(Player player) {
        return PlayerSnapshot.capture(player);
    }

    public void restore(Player player, PlayerSnapshot snapshot, DuelSettings settings, DuelMode mode) {
        boolean restoreInventory = mode != DuelMode.REAL_GEAR || settings.realGearSettings().restoreInventoryAfterMatch();
        boolean restorePotions = mode == DuelMode.FAIR_KIT
            ? settings.fairKitSettings().restorePotionEffectsAfterMatch()
            : mode == DuelMode.EMPTY_RITUAL || settings.realGearSettings().restorePotionEffectsAfterMatch();

        player.closeInventory();
        player.getInventory().clear();
        if (restoreInventory) {
            player.getInventory().setContents(snapshot.getContents());
            player.getInventory().setArmorContents(snapshot.getArmorContents());
            player.getInventory().setItemInOffHand(snapshot.getOffHand());
        } else {
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
        }
        player.setGameMode(snapshot.getGameMode());
        player.setAllowFlight(snapshot.isAllowFlight());
        player.setFlying(snapshot.isAllowFlight() && snapshot.isFlying());
        player.setHealth(Math.min(snapshot.getHealth(), player.getMaxHealth()));
        player.setFoodLevel(snapshot.getFoodLevel());
        player.setSaturation(snapshot.getSaturation());
        player.setLevel(snapshot.getLevel());
        player.setExp(snapshot.getExp());
        player.setTotalExperience(snapshot.getTotalExp());
        player.setFireTicks(snapshot.getFireTicks());
        player.setFreezeTicks(snapshot.getFreezeTicks());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (restorePotions) {
            for (PotionEffect effect : snapshot.getPotionEffects()) {
                player.addPotionEffect(effect, true);
            }
        }
        player.updateInventory();
    }

    public Location determineReturnLocation(PlayerSnapshot snapshot, DuelSettings settings, Location configuredReturnLocation) {
        if ("LOBBY".equalsIgnoreCase(settings.returnMode()) && configuredReturnLocation != null && configuredReturnLocation.getWorld() != null) {
            return configuredReturnLocation.clone();
        }
        return snapshot.getOriginalLocation();
    }

    public void queueDeferredRestore(UUID playerId, PlayerSnapshot snapshot, Location returnLocation, DuelMode mode) {
        deferredRestores.put(playerId, new DeferredRestore(snapshot, returnLocation, mode));
    }

    public DeferredRestore consumeDeferredRestore(UUID playerId) {
        return deferredRestores.remove(playerId);
    }

    public boolean hasDeferredRestore(UUID playerId) {
        return deferredRestores.containsKey(playerId);
    }

    public record DeferredRestore(PlayerSnapshot snapshot, Location returnLocation, DuelMode mode) {
    }
}

package org.stellarvan.stellarDuelBridge.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public final class PlayerSnapshot {

    private final UUID uuid;
    private final String playerName;
    private final Location originalLocation;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final double health;
    private final double maxHealth;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final int totalExp;
    private final int fireTicks;
    private final int freezeTicks;
    private final List<PotionEffect> potionEffects;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack offHand;

    public PlayerSnapshot(
        UUID uuid,
        String playerName,
        Location originalLocation,
        GameMode gameMode,
        boolean allowFlight,
        boolean flying,
        double health,
        double maxHealth,
        int foodLevel,
        float saturation,
        int level,
        float exp,
        int totalExp,
        int fireTicks,
        int freezeTicks,
        List<PotionEffect> potionEffects,
        ItemStack[] contents,
        ItemStack[] armorContents,
        ItemStack offHand
    ) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.originalLocation = originalLocation.clone();
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.health = health;
        this.maxHealth = maxHealth;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.totalExp = totalExp;
        this.fireTicks = fireTicks;
        this.freezeTicks = freezeTicks;
        this.potionEffects = new ArrayList<>(potionEffects);
        this.contents = cloneItems(contents);
        this.armorContents = cloneItems(armorContents);
        this.offHand = offHand == null ? null : offHand.clone();
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(
            player.getUniqueId(),
            player.getName(),
            player.getLocation(),
            player.getGameMode(),
            player.getAllowFlight(),
            player.isFlying(),
            player.getHealth(),
            player.getMaxHealth(),
            player.getFoodLevel(),
            player.getSaturation(),
            player.getLevel(),
            player.getExp(),
            player.getTotalExperience(),
            player.getFireTicks(),
            player.getFreezeTicks(),
            new ArrayList<>(player.getActivePotionEffects()),
            player.getInventory().getContents(),
            player.getInventory().getArmorContents(),
            player.getInventory().getItemInOffHand()
        );
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Location getOriginalLocation() {
        return originalLocation.clone();
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public boolean isAllowFlight() {
        return allowFlight;
    }

    public boolean isFlying() {
        return flying;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public int getLevel() {
        return level;
    }

    public float getExp() {
        return exp;
    }

    public int getTotalExp() {
        return totalExp;
    }

    public int getFireTicks() {
        return fireTicks;
    }

    public int getFreezeTicks() {
        return freezeTicks;
    }

    public List<PotionEffect> getPotionEffects() {
        return new ArrayList<>(potionEffects);
    }

    public ItemStack[] getContents() {
        return cloneItems(contents);
    }

    public ItemStack[] getArmorContents() {
        return cloneItems(armorContents);
    }

    public ItemStack getOffHand() {
        return offHand == null ? null : offHand.clone();
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] clone = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            clone[index] = items[index] == null ? null : items[index].clone();
        }
        return clone;
    }
}

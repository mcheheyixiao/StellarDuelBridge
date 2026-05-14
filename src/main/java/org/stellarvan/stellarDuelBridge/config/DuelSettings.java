package org.stellarvan.stellarDuelBridge.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.stellarvan.stellarDuelBridge.duel.DuelMode;
import org.stellarvan.stellarDuelBridge.util.ItemStackSerializer;

public final class DuelSettings {

    private final String displayName;
    private final int inviteExpireSeconds;
    private final int requestCooldownSeconds;
    private final int countdownSeconds;
    private final int maxDurationSeconds;
    private final String returnMode;
    private final boolean allowCrossWorldRequest;
    private final boolean requireSameWorldRequest;
    private final boolean preventDuelInDisabledWorlds;
    private final boolean preventDuelWhileInCombat;
    private final boolean preventDuelWhileFlying;
    private final boolean preventDuelWhileVanished;
    private final boolean preventSelfDuel;
    private final boolean preventSameIpDuel;
    private final String storageType;
    private final String sqliteFile;
    private final boolean mysqlFallbackToSqlite;
    private final List<String> disabledRequestWorlds;
    private final List<String> arenaWorldPrefixes;
    private final DuelMode defaultMode;
    private final boolean allowPlayerSelection;
    private final ModeSettings realGearSettings;
    private final FairKitSettings fairKitSettings;
    private final EmptyRitualSettings emptyRitualSettings;
    private final CombatSettings combatSettings;
    private final RewardSettings rewardSettings;
    private final boolean debugEnabled;

    private DuelSettings(
        String displayName,
        int inviteExpireSeconds,
        int requestCooldownSeconds,
        int countdownSeconds,
        int maxDurationSeconds,
        String returnMode,
        boolean allowCrossWorldRequest,
        boolean requireSameWorldRequest,
        boolean preventDuelInDisabledWorlds,
        boolean preventDuelWhileInCombat,
        boolean preventDuelWhileFlying,
        boolean preventDuelWhileVanished,
        boolean preventSelfDuel,
        boolean preventSameIpDuel,
        String storageType,
        String sqliteFile,
        boolean mysqlFallbackToSqlite,
        List<String> disabledRequestWorlds,
        List<String> arenaWorldPrefixes,
        DuelMode defaultMode,
        boolean allowPlayerSelection,
        ModeSettings realGearSettings,
        FairKitSettings fairKitSettings,
        EmptyRitualSettings emptyRitualSettings,
        CombatSettings combatSettings,
        RewardSettings rewardSettings,
        boolean debugEnabled
    ) {
        this.displayName = displayName;
        this.inviteExpireSeconds = inviteExpireSeconds;
        this.requestCooldownSeconds = requestCooldownSeconds;
        this.countdownSeconds = countdownSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.returnMode = returnMode;
        this.allowCrossWorldRequest = allowCrossWorldRequest;
        this.requireSameWorldRequest = requireSameWorldRequest;
        this.preventDuelInDisabledWorlds = preventDuelInDisabledWorlds;
        this.preventDuelWhileInCombat = preventDuelWhileInCombat;
        this.preventDuelWhileFlying = preventDuelWhileFlying;
        this.preventDuelWhileVanished = preventDuelWhileVanished;
        this.preventSelfDuel = preventSelfDuel;
        this.preventSameIpDuel = preventSameIpDuel;
        this.storageType = storageType;
        this.sqliteFile = sqliteFile;
        this.mysqlFallbackToSqlite = mysqlFallbackToSqlite;
        this.disabledRequestWorlds = disabledRequestWorlds;
        this.arenaWorldPrefixes = arenaWorldPrefixes;
        this.defaultMode = defaultMode;
        this.allowPlayerSelection = allowPlayerSelection;
        this.realGearSettings = realGearSettings;
        this.fairKitSettings = fairKitSettings;
        this.emptyRitualSettings = emptyRitualSettings;
        this.combatSettings = combatSettings;
        this.rewardSettings = rewardSettings;
        this.debugEnabled = debugEnabled;
    }

    public static DuelSettings from(FileConfiguration config) {
        ConfigurationSection settings = config.getConfigurationSection("settings");
        ConfigurationSection storage = config.getConfigurationSection("storage");
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        ConfigurationSection modes = config.getConfigurationSection("modes");
        ConfigurationSection combat = config.getConfigurationSection("combat");
        ConfigurationSection rewards = config.getConfigurationSection("rewards");

        ConfigurationSection realGear = modes.getConfigurationSection("real-gear");
        ConfigurationSection fairKit = modes.getConfigurationSection("fair-kit");
        ConfigurationSection emptyRitual = modes.getConfigurationSection("empty-ritual");

        return new DuelSettings(
            settings.getString("display-name", "荣誉决斗"),
            Math.max(1, settings.getInt("invite-expire-seconds", 60)),
            Math.max(0, settings.getInt("request-cooldown-seconds", 15)),
            Math.max(1, settings.getInt("countdown-seconds", 5)),
            Math.max(1, settings.getInt("max-duration-seconds", 300)),
            settings.getString("return-mode", "ORIGINAL_LOCATION").toUpperCase(),
            settings.getBoolean("allow-cross-world-request", true),
            settings.getBoolean("require-same-world-request", false),
            settings.getBoolean("prevent-duel-in-disabled-worlds", true),
            settings.getBoolean("prevent-duel-while-in-combat", true),
            settings.getBoolean("prevent-duel-while-flying", true),
            settings.getBoolean("prevent-duel-while-vanished", true),
            settings.getBoolean("prevent-self-duel", true),
            settings.getBoolean("prevent-same-ip-duel", false),
            storage.getString("type", "SQLITE").toUpperCase(),
            storage.getString("sqlite.file", "duel-stats.db"),
            storage.getBoolean("mysql.fallback-to-sqlite-if-unavailable", true),
            worlds.getStringList("disabled-request-worlds"),
            worlds.getStringList("arena-world-prefixes"),
            parseMode(modes.getString("default", "REAL_GEAR")),
            modes.getBoolean("allow-player-selection", true),
            new ModeSettings(
                realGear.getBoolean("enabled", true),
                realGear.getString("display-name", "真实装备模式"),
                realGear.getBoolean("restore-inventory-after-match", true),
                realGear.getBoolean("restore-durability", true),
                realGear.getBoolean("restore-consumables", true),
                realGear.getBoolean("clear-potion-effects-on-start", true),
                realGear.getBoolean("restore-potion-effects-after-match", false)
            ),
            parseFairKitSettings(fairKit),
            parseEmptyRitualSettings(emptyRitual),
            new CombatSettings(
                combat.getBoolean("keep-inventory", true),
                combat.getBoolean("clear-drops", true),
                combat.getBoolean("clear-exp-drops", true),
                combat.getBoolean("freeze-during-countdown", true),
                Math.max(0, combat.getInt("invulnerable-after-teleport-ticks", 60)),
                combat.getBoolean("allow-durability-loss", false),
                combat.getBoolean("allow-consumables", true),
                combat.getBoolean("allow-bow", true),
                combat.getBoolean("allow-crossbow", true),
                combat.getBoolean("allow-trident", false),
                combat.getBoolean("allow-ender-pearl", false),
                combat.getBoolean("allow-chorus-fruit", false),
                combat.getStringList("blocked-commands")
            ),
            new RewardSettings(
                rewards.getBoolean("enabled", false),
                rewards.getStringList("winner-commands"),
                rewards.getStringList("loser-commands"),
                rewards.getStringList("draw-commands")
            ),
            config.getBoolean("debug.enabled", false)
        );
    }

    private static DuelMode parseMode(String mode) {
        try {
            return DuelMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DuelMode.REAL_GEAR;
        }
    }

    private static FairKitSettings parseFairKitSettings(ConfigurationSection section) {
        String defaultKit = section.getString("default-kit", "default");
        Map<String, KitDefinition> kits = new HashMap<>();
        ConfigurationSection kitsSection = section.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(key);
                Map<Integer, ItemStack> contents = new HashMap<>();
                for (Map<?, ?> rawItem : kitSection.getMapList("items")) {
                    Object slotObject = rawItem.get("slot");
                    if (!(slotObject instanceof Number slotNumber)) {
                        continue;
                    }
                    contents.put(slotNumber.intValue(), ItemStackSerializer.fromConfig(mapToSection(rawItem)));
                }
                ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
                kits.put(
                    key,
                    new KitDefinition(
                        kitSection.getString("display-name", key),
                        contents,
                        armorSection == null ? null : readArmorPiece(armorSection, "helmet"),
                        armorSection == null ? null : readArmorPiece(armorSection, "chestplate"),
                        armorSection == null ? null : readArmorPiece(armorSection, "leggings"),
                        armorSection == null ? null : readArmorPiece(armorSection, "boots")
                    )
                );
            }
        }
        return new FairKitSettings(
            section.getBoolean("enabled", true),
            section.getString("display-name", "公平 Kit 模式"),
            section.getBoolean("clear-potion-effects-on-start", true),
            section.getBoolean("restore-potion-effects-after-match", false),
            defaultKit,
            Collections.unmodifiableMap(kits)
        );
    }

    private static EmptyRitualSettings parseEmptyRitualSettings(ConfigurationSection section) {
        ItemStack weapon = null;
        ConfigurationSection weaponSection = section.getConfigurationSection("weapon");
        if (weaponSection != null) {
            weapon = ItemStackSerializer.fromConfig(weaponSection);
        }
        return new EmptyRitualSettings(
            section.getBoolean("enabled", true),
            section.getString("display-name", "空手仪式模式"),
            section.getBoolean("give-basic-weapon", true),
            weapon
        );
    }

    private static ItemStack readArmorPiece(ConfigurationSection section, String key) {
        ConfigurationSection armor = section.getConfigurationSection(key);
        if (armor == null) {
            return null;
        }
        return ItemStackSerializer.fromConfig(armor);
    }

    private static ConfigurationSection mapToSection(Map<?, ?> values) {
        YamlConfiguration configuration = new YamlConfiguration();
        values.forEach((key, value) -> configuration.set(String.valueOf(key), value));
        return configuration;
    }

    public String displayName() {
        return displayName;
    }

    public int inviteExpireSeconds() {
        return inviteExpireSeconds;
    }

    public int requestCooldownSeconds() {
        return requestCooldownSeconds;
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public int maxDurationSeconds() {
        return maxDurationSeconds;
    }

    public String returnMode() {
        return returnMode;
    }

    public boolean allowCrossWorldRequest() {
        return allowCrossWorldRequest;
    }

    public boolean requireSameWorldRequest() {
        return requireSameWorldRequest;
    }

    public boolean preventDuelInDisabledWorlds() {
        return preventDuelInDisabledWorlds;
    }

    public boolean preventDuelWhileInCombat() {
        return preventDuelWhileInCombat;
    }

    public boolean preventDuelWhileFlying() {
        return preventDuelWhileFlying;
    }

    public boolean preventDuelWhileVanished() {
        return preventDuelWhileVanished;
    }

    public boolean preventSelfDuel() {
        return preventSelfDuel;
    }

    public boolean preventSameIpDuel() {
        return preventSameIpDuel;
    }

    public String storageType() {
        return storageType;
    }

    public String sqliteFile() {
        return sqliteFile;
    }

    public boolean mysqlFallbackToSqlite() {
        return mysqlFallbackToSqlite;
    }

    public List<String> disabledRequestWorlds() {
        return disabledRequestWorlds;
    }

    public List<String> arenaWorldPrefixes() {
        return arenaWorldPrefixes;
    }

    public DuelMode defaultMode() {
        return defaultMode;
    }

    public boolean allowPlayerSelection() {
        return allowPlayerSelection;
    }

    public ModeSettings realGearSettings() {
        return realGearSettings;
    }

    public FairKitSettings fairKitSettings() {
        return fairKitSettings;
    }

    public EmptyRitualSettings emptyRitualSettings() {
        return emptyRitualSettings;
    }

    public CombatSettings combatSettings() {
        return combatSettings;
    }

    public RewardSettings rewardSettings() {
        return rewardSettings;
    }

    public boolean debugEnabled() {
        return debugEnabled;
    }

    public boolean isModeEnabled(DuelMode mode) {
        return switch (mode) {
            case REAL_GEAR -> realGearSettings.enabled();
            case FAIR_KIT -> fairKitSettings.enabled();
            case EMPTY_RITUAL -> emptyRitualSettings.enabled();
        };
    }

    public String getModeDisplayName(DuelMode mode) {
        return switch (mode) {
            case REAL_GEAR -> realGearSettings.displayName();
            case FAIR_KIT -> fairKitSettings.displayName();
            case EMPTY_RITUAL -> emptyRitualSettings.displayName();
        };
    }

    public record ModeSettings(
        boolean enabled,
        String displayName,
        boolean restoreInventoryAfterMatch,
        boolean restoreDurability,
        boolean restoreConsumables,
        boolean clearPotionEffectsOnStart,
        boolean restorePotionEffectsAfterMatch
    ) {
    }

    public record FairKitSettings(
        boolean enabled,
        String displayName,
        boolean clearPotionEffectsOnStart,
        boolean restorePotionEffectsAfterMatch,
        String defaultKit,
        Map<String, KitDefinition> kits
    ) {
    }

    public record EmptyRitualSettings(
        boolean enabled,
        String displayName,
        boolean giveBasicWeapon,
        ItemStack weapon
    ) {
    }

    public record CombatSettings(
        boolean keepInventory,
        boolean clearDrops,
        boolean clearExpDrops,
        boolean freezeDuringCountdown,
        int invulnerableAfterTeleportTicks,
        boolean allowDurabilityLoss,
        boolean allowConsumables,
        boolean allowBow,
        boolean allowCrossbow,
        boolean allowTrident,
        boolean allowEnderPearl,
        boolean allowChorusFruit,
        List<String> blockedCommands
    ) {
    }

    public record RewardSettings(
        boolean enabled,
        List<String> winnerCommands,
        List<String> loserCommands,
        List<String> drawCommands
    ) {
    }

    public record KitDefinition(
        String displayName,
        Map<Integer, ItemStack> contents,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots
    ) {
        public ItemStack getHelmet() {
            return helmet == null ? null : helmet.clone();
        }

        public ItemStack getChestplate() {
            return chestplate == null ? null : chestplate.clone();
        }

        public ItemStack getLeggings() {
            return leggings == null ? null : leggings.clone();
        }

        public ItemStack getBoots() {
            return boots == null ? null : boots.clone();
        }

        public Map<Integer, ItemStack> cloneContents() {
            Map<Integer, ItemStack> clone = new HashMap<>();
            contents.forEach((slot, stack) -> clone.put(slot, stack == null ? null : stack.clone()));
            return clone;
        }
    }
}

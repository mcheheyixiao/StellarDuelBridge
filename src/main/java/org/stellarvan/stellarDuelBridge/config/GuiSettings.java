package org.stellarvan.stellarDuelBridge.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.stellarvan.stellarDuelBridge.gui.MenuButton;
import org.stellarvan.stellarDuelBridge.util.ItemStackSerializer;

public final class GuiSettings {

    private final DuelConfirmSettings duelConfirmSettings;
    private final boolean itemsAdderEnabled;
    private final String titleTextureKey;
    private final boolean useCustomModelData;

    private GuiSettings(
        DuelConfirmSettings duelConfirmSettings,
        boolean itemsAdderEnabled,
        String titleTextureKey,
        boolean useCustomModelData
    ) {
        this.duelConfirmSettings = duelConfirmSettings;
        this.itemsAdderEnabled = itemsAdderEnabled;
        this.titleTextureKey = titleTextureKey;
        this.useCustomModelData = useCustomModelData;
    }

    public static GuiSettings from(FileConfiguration config) {
        ConfigurationSection duelConfirm = config.getConfigurationSection("duel-confirm");
        int size = duelConfirm.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 27;
        }
        Map<String, MenuButton> buttons = new HashMap<>();
        ConfigurationSection buttonsSection = duelConfirm.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            for (String key : buttonsSection.getKeys(false)) {
                ConfigurationSection buttonSection = buttonsSection.getConfigurationSection(key);
                buttons.put(
                    key,
                    new MenuButton(
                        key,
                        buttonSection.getInt("slot", 0),
                        ItemStackSerializer.fromConfig(buttonSection)
                    )
                );
            }
        }
        ItemStack filler = ItemStackSerializer.fromConfig(duelConfirm.getConfigurationSection("filler"));
        ConfigurationSection itemsAdder = config.getConfigurationSection("itemsadder");
        return new GuiSettings(
            new DuelConfirmSettings(
                duelConfirm.getString("title", "<dark_gray>荣誉决斗"),
                size,
                duelConfirm.getBoolean("fill-empty-slots", true),
                filler,
                Collections.unmodifiableMap(buttons)
            ),
            itemsAdder.getBoolean("enabled", false),
            itemsAdder.getString("title-texture-key", ""),
            itemsAdder.getBoolean("use-custom-model-data", true)
        );
    }

    public DuelConfirmSettings duelConfirmSettings() {
        return duelConfirmSettings;
    }

    public boolean itemsAdderEnabled() {
        return itemsAdderEnabled;
    }

    public String titleTextureKey() {
        return titleTextureKey;
    }

    public boolean useCustomModelData() {
        return useCustomModelData;
    }

    public record DuelConfirmSettings(
        String title,
        int size,
        boolean fillEmptySlots,
        ItemStack filler,
        Map<String, MenuButton> buttons
    ) {
    }
}

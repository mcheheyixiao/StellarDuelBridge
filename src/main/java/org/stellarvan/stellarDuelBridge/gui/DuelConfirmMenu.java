package org.stellarvan.stellarDuelBridge.gui;

import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.config.GuiSettings;
import org.stellarvan.stellarDuelBridge.duel.DuelMode;
import org.stellarvan.stellarDuelBridge.util.MiniMessageUtil;

public final class DuelConfirmMenu {

    private final ConfigManager configManager;

    public DuelConfirmMenu(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void open(Player player, UUID sessionId, DuelMode selectedMode) {
        GuiSettings.DuelConfirmSettings settings = configManager.getGuiSettings().duelConfirmSettings();
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuType.DUEL_CONFIRM, sessionId, player.getUniqueId(), selectedMode);
        Component title = MiniMessageUtil.deserialize(settings.title());
        Inventory inventory = Bukkit.createInventory(holder, settings.size(), title);
        holder.setInventory(inventory);

        if (settings.fillEmptySlots()) {
            ItemStack filler = settings.filler() == null ? null : settings.filler().clone();
            if (filler != null) {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    inventory.setItem(slot, filler.clone());
                }
            }
        }

        for (Map.Entry<String, MenuButton> entry : settings.buttons().entrySet()) {
            MenuButton button = entry.getValue();
            ItemStack item = button.itemStack().clone();
            DuelMode mode = DuelMode.fromButtonKey(button.key());
            if (mode != null && mode == selectedMode) {
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            inventory.setItem(button.slot(), item);
        }

        player.openInventory(inventory);
    }
}

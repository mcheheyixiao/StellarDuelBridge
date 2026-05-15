package org.stellarvan.stellarDuelBridge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.stellarvan.stellarDuelBridge.duel.DuelSession;
import org.stellarvan.stellarDuelBridge.util.MiniMessageUtil;

public final class DuelConfirmMenu {

    private final ConfigManager configManager;

    public DuelConfirmMenu(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void open(Player player, DuelSession session) {
        GuiSettings.DuelConfirmSettings settings = configManager.getGuiSettings().duelConfirmSettings();
        DuelMenuHolder holder = new DuelMenuHolder(DuelMenuType.DUEL_CONFIRM, session.getSessionId(), player.getUniqueId(), session.getSelectedMode());
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
            if (mode != null && mode == session.getSelectedMode()) {
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            if ("info".equalsIgnoreCase(button.key())) {
                appendContractSummary(item, session);
            }
            inventory.setItem(button.slot(), item);
        }

        player.openInventory(inventory);
    }

    private void appendContractSummary(ItemStack item, DuelSession session) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        String arena = session.getArenaId() == null ? "待分配" : session.getArenaId();
        String mode = configManager.getDuelSettings().getModeDisplayName(session.getSelectedMode());
        lore.add(MiniMessageUtil.deserialize(""));
        lore.add(MiniMessageUtil.deserialize("<gray>挑战者: <yellow>" + session.getPlayerOneName() + "</yellow>"));
        lore.add(MiniMessageUtil.deserialize("<gray>应战者: <yellow>" + session.getPlayerTwoName() + "</yellow>"));
        lore.add(MiniMessageUtil.deserialize("<gray>模式: <yellow>" + mode + "</yellow>"));
        lore.add(MiniMessageUtil.deserialize("<gray>竞技场: <yellow>" + arena + "</yellow>"));
        lore.add(MiniMessageUtil.deserialize("<gray>确认状态: <yellow>" + session.getPlayerOneName() + " " + toMark(session.isChallengerConfirmed()) + "</yellow>"));
        lore.add(MiniMessageUtil.deserialize("<gray>确认状态: <yellow>" + session.getPlayerTwoName() + " " + toMark(session.isTargetConfirmed()) + "</yellow>"));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private String toMark(boolean confirmed) {
        return confirmed ? "√" : "×";
    }
}

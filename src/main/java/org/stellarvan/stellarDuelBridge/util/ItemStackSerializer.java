package org.stellarvan.stellarDuelBridge.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemStackSerializer {

    private ItemStackSerializer() {
    }

    public static ItemStack fromConfig(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = stack.getItemMeta();
        if (section.contains("name")) {
            meta.displayName(MiniMessageUtil.deserialize(section.getString("name", "")));
        }
        if (section.contains("lore")) {
            List<String> rawLore = section.getStringList("lore");
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>(rawLore.size());
            for (String line : rawLore) {
                lore.add(MiniMessageUtil.deserialize(line));
            }
            meta.lore(lore);
        }
        int customModelData = section.getInt("custom-model-data", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }
}

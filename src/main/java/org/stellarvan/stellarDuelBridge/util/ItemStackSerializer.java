package org.stellarvan.stellarDuelBridge.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemStackSerializer {

    private static final Method GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD = findMethod(ItemMeta.class, "getCustomModelDataComponent");
    private static final Method SET_CUSTOM_MODEL_DATA_COMPONENT_METHOD = resolveSetCustomModelDataComponentMethod();
    private static final Method SET_CUSTOM_MODEL_DATA_FLOATS_METHOD = resolveSetCustomModelDataFloatsMethod();
    private static final Method LEGACY_SET_CUSTOM_MODEL_DATA_METHOD = findMethod(ItemMeta.class, "setCustomModelData", Integer.class);

    private ItemStackSerializer() {
    }

    public static ItemStack fromConfig(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
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
            applyCustomModelData(meta, customModelData);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private static void applyCustomModelData(ItemMeta meta, int customModelData) {
        if (GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD != null
            && SET_CUSTOM_MODEL_DATA_COMPONENT_METHOD != null
            && SET_CUSTOM_MODEL_DATA_FLOATS_METHOD != null) {
            try {
                Object component = GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD.invoke(meta);
                if (component != null) {
                    SET_CUSTOM_MODEL_DATA_FLOATS_METHOD.invoke(component, List.of((float) customModelData));
                    SET_CUSTOM_MODEL_DATA_COMPONENT_METHOD.invoke(meta, component);
                    return;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall through to legacy API.
            }
        }
        if (LEGACY_SET_CUSTOM_MODEL_DATA_METHOD != null) {
            try {
                LEGACY_SET_CUSTOM_MODEL_DATA_METHOD.invoke(meta, customModelData);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to set custom model data.", exception);
            }
        }
    }

    private static Method resolveSetCustomModelDataComponentMethod() {
        if (GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD == null) {
            return null;
        }
        return findMethod(ItemMeta.class, "setCustomModelDataComponent", GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD.getReturnType());
    }

    private static Method resolveSetCustomModelDataFloatsMethod() {
        if (GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD == null) {
            return null;
        }
        return findMethod(GET_CUSTOM_MODEL_DATA_COMPONENT_METHOD.getReturnType(), "setFloats", List.class);
    }

    private static Method findMethod(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }
}

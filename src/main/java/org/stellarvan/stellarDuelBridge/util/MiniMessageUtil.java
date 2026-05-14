package org.stellarvan.stellarDuelBridge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MiniMessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MiniMessageUtil() {
    }

    public static Component deserialize(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("<") && input.contains(">")) {
            return MINI_MESSAGE.deserialize(input);
        }
        return LEGACY.deserialize(input);
    }

    public static String serialize(Component component) {
        return MINI_MESSAGE.serialize(component);
    }
}

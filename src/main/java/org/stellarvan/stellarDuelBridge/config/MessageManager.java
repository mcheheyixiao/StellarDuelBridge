package org.stellarvan.stellarDuelBridge.config;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.util.MiniMessageUtil;

public final class MessageManager {

    private final StellarDuelBridge plugin;
    private FileConfiguration messagesConfig;
    private Component prefix;

    public MessageManager(StellarDuelBridge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        this.prefix = MiniMessageUtil.deserialize(messagesConfig.getString("prefix", ""));
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Collections.emptyMap(), true);
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, path, placeholders, true);
    }

    public void sendRawMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, path, placeholders, false);
    }

    public void sendLines(CommandSender sender, String path) {
        List<String> lines = messagesConfig.getStringList(path);
        for (String line : lines) {
            sender.sendMessage(prefix.append(MiniMessageUtil.deserialize(line)));
        }
    }

    public Component component(String path) {
        return component(path, Collections.emptyMap(), true);
    }

    public Component component(String path, Map<String, String> placeholders) {
        return component(path, placeholders, true);
    }

    public String raw(String path) {
        return messagesConfig.getString(path, path);
    }

    public List<String> rawList(String path) {
        return messagesConfig.getStringList(path);
    }

    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders, boolean includePrefix) {
        String raw = messagesConfig.getString(path);
        if (raw == null || raw.isBlank()) {
            return;
        }
        Component message = MiniMessageUtil.deserialize(applyPlaceholders(raw, placeholders));
        sender.sendMessage(includePrefix ? prefix.append(message) : message);
    }

    private Component component(String path, Map<String, String> placeholders, boolean includePrefix) {
        String raw = messagesConfig.getString(path, path);
        Component message = MiniMessageUtil.deserialize(applyPlaceholders(raw, placeholders));
        return includePrefix ? prefix.append(message) : message;
    }

    private String applyPlaceholders(String input, Map<String, String> placeholders) {
        String resolved = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }
}

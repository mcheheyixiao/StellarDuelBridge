package org.stellarvan.stellarDuelBridge.config;

import java.io.File;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.util.LocationSerializer;

public final class ConfigManager {

    private final StellarDuelBridge plugin;
    private File guiFile;
    private File arenasFile;
    private FileConfiguration guiConfig;
    private FileConfiguration arenasConfig;
    private DuelSettings duelSettings;
    private GuiSettings guiSettings;

    public ConfigManager(StellarDuelBridge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.guiFile = new File(plugin.getDataFolder(), "gui.yml");
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        this.arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        this.duelSettings = DuelSettings.from(plugin.getConfig());
        this.guiSettings = GuiSettings.from((YamlConfiguration) guiConfig);
    }

    public DuelSettings getDuelSettings() {
        return duelSettings;
    }

    public GuiSettings getGuiSettings() {
        return guiSettings;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    public FileConfiguration getArenasConfig() {
        return arenasConfig;
    }

    public void saveArenasConfig() {
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + exception.getMessage());
        }
    }

    public void saveMainConfig() {
        plugin.saveConfig();
    }

    public Location getReturnLocation() {
        return LocationSerializer.read(plugin.getConfig().getConfigurationSection("settings.return-location"), plugin.getConfig().getString("settings.return-location.world"));
    }

    public void setReturnLocation(Location location) {
        plugin.getConfig().set("settings.return-location", null);
        var section = plugin.getConfig().createSection("settings.return-location");
        section.set("world", location.getWorld() == null ? null : location.getWorld().getName());
        LocationSerializer.write(section, location);
        saveMainConfig();
        reload();
    }
}

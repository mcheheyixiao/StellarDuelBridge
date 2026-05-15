package org.stellarvan.stellarDuelBridge.snapshot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.duel.DuelMode;
import org.stellarvan.stellarDuelBridge.util.LocationSerializer;

public final class PendingRestoreManager {

    private final StellarDuelBridge plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "StellarDuelBridge-PendingRestore");
        thread.setDaemon(true);
        return thread;
    });
    private Connection connection;

    public PendingRestoreManager(StellarDuelBridge plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new IllegalStateException("Unable to create plugin data folder.");
            }
            File dbFile = new File(plugin.getDataFolder(), "pending_restores.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS pending_restores (
                      player_uuid TEXT PRIMARY KEY,
                      payload TEXT NOT NULL,
                      updated_at INTEGER NOT NULL
                    )
                    """);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize pending restore database.", exception);
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to close pending restore database: " + exception.getMessage());
        }
    }

    public CompletableFuture<Void> upsert(UUID playerId, PlayerSnapshot snapshot, Location returnLocation, DuelMode mode) {
        return CompletableFuture.runAsync(() -> {
            String payload = serializePayload(snapshot, returnLocation, mode);
            long now = System.currentTimeMillis() / 1000L;
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pending_restores(player_uuid, payload, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                  payload = excluded.payload,
                  updated_at = excluded.updated_at
                """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, payload);
                statement.setLong(3, now);
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save pending restore for " + playerId, exception);
            }
        }, executor);
    }

    public CompletableFuture<Void> remove(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM pending_restores WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to remove pending restore for " + playerId, exception);
            }
        }, executor);
    }

    public CompletableFuture<PendingRestoreRecord> load(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT payload FROM pending_restores WHERE player_uuid = ?")) {
                statement.setString(1, playerId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return deserializePayload(playerId, resultSet.getString("payload"));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to read pending restore for " + playerId, exception);
            }
        }, executor);
    }

    public CompletableFuture<List<PendingRestoreRecord>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<PendingRestoreRecord> records = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT player_uuid, payload FROM pending_restores");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID playerId = UUID.fromString(resultSet.getString("player_uuid"));
                    records.add(deserializePayload(playerId, resultSet.getString("payload")));
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load pending restores.", exception);
            }
            return records;
        }, executor);
    }

    private String serializePayload(PlayerSnapshot snapshot, Location returnLocation, DuelMode mode) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("player_name", snapshot.getPlayerName());
        config.set("mode", mode.name());

        ConfigurationSection original = config.createSection("original_location");
        original.set("world", snapshot.getOriginalLocation().getWorld() == null ? null : snapshot.getOriginalLocation().getWorld().getName());
        LocationSerializer.write(original, snapshot.getOriginalLocation());

        ConfigurationSection returnSection = config.createSection("return_location");
        returnSection.set("world", returnLocation == null || returnLocation.getWorld() == null ? null : returnLocation.getWorld().getName());
        if (returnLocation != null) {
            LocationSerializer.write(returnSection, returnLocation);
        }

        config.set("game_mode", snapshot.getGameMode().name());
        config.set("allow_flight", snapshot.isAllowFlight());
        config.set("flying", snapshot.isFlying());
        config.set("health", snapshot.getHealth());
        config.set("max_health", snapshot.getMaxHealth());
        config.set("food_level", snapshot.getFoodLevel());
        config.set("saturation", snapshot.getSaturation());
        config.set("level", snapshot.getLevel());
        config.set("exp", snapshot.getExp());
        config.set("total_exp", snapshot.getTotalExp());
        config.set("fire_ticks", snapshot.getFireTicks());
        config.set("freeze_ticks", snapshot.getFreezeTicks());

        List<Map<String, Object>> potionData = new ArrayList<>();
        for (PotionEffect effect : snapshot.getPotionEffects()) {
            potionData.add(Map.of(
                "type", effect.getType().getKey().toString(),
                "duration", effect.getDuration(),
                "amplifier", effect.getAmplifier(),
                "ambient", effect.isAmbient(),
                "particles", effect.hasParticles(),
                "icon", effect.hasIcon()
            ));
        }
        config.set("potions", potionData);
        config.set("inventory.contents", serializeItems(snapshot.getContents()));
        config.set("inventory.armor", serializeItems(snapshot.getArmorContents()));
        config.set("inventory.offhand", snapshot.getOffHand() == null ? null : encodeItem(snapshot.getOffHand()));
        return config.saveToString();
    }

    private PendingRestoreRecord deserializePayload(UUID playerId, String payload) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid pending restore payload for " + playerId, exception);
        }

        Location originalLocation = LocationSerializer.read(
            config.getConfigurationSection("original_location"),
            config.getString("original_location.world")
        );
        if (originalLocation == null) {
            originalLocation = plugin.getServer().getWorlds().isEmpty()
                ? new Location(null, 0, 0, 0)
                : plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }

        List<PotionEffect> potions = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList("potions")) {
            Object typeKey = raw.get("type");
            if (!(typeKey instanceof String key)) {
                continue;
            }
            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.fromString(key));
            if (type == null) {
                continue;
            }
            int duration = asInt(raw.get("duration"), 0);
            int amplifier = asInt(raw.get("amplifier"), 0);
            boolean ambient = asBoolean(raw.get("ambient"));
            boolean particles = asBoolean(raw.get("particles"));
            boolean icon = asBoolean(raw.get("icon"));
            potions.add(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
        }

        PlayerSnapshot snapshot = new PlayerSnapshot(
            playerId,
            config.getString("player_name", playerId.toString()),
            originalLocation,
            GameMode.valueOf(config.getString("game_mode", GameMode.SURVIVAL.name())),
            config.getBoolean("allow_flight"),
            config.getBoolean("flying"),
            config.getDouble("health"),
            config.getDouble("max_health"),
            config.getInt("food_level"),
            (float) config.getDouble("saturation"),
            config.getInt("level"),
            (float) config.getDouble("exp"),
            config.getInt("total_exp"),
            config.getInt("fire_ticks"),
            config.getInt("freeze_ticks"),
            potions,
            deserializeItems(config.getStringList("inventory.contents")),
            deserializeItems(config.getStringList("inventory.armor")),
            decodeItem(config.getString("inventory.offhand"))
        );

        DuelMode mode = DuelMode.valueOf(config.getString("mode", DuelMode.REAL_GEAR.name()));
        Location returnLocation = LocationSerializer.read(
            config.getConfigurationSection("return_location"),
            config.getString("return_location.world")
        );
        return new PendingRestoreRecord(playerId, snapshot, returnLocation, mode);
    }

    private List<String> serializeItems(ItemStack[] items) {
        List<String> data = new ArrayList<>(items.length);
        for (ItemStack item : items) {
            data.add(item == null ? "null" : encodeItem(item));
        }
        return data;
    }

    private ItemStack[] deserializeItems(List<String> data) {
        ItemStack[] items = new ItemStack[data.size()];
        for (int index = 0; index < data.size(); index++) {
            items[index] = decodeItem(data.get(index));
        }
        return items;
    }

    private String encodeItem(ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    private ItemStack decodeItem(String encoded) {
        if (encoded == null || encoded.equals("null")) {
            return null;
        }
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }

    public record PendingRestoreRecord(UUID playerId, PlayerSnapshot snapshot, Location returnLocation, DuelMode mode) {
    }
}

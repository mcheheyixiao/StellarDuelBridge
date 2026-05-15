package org.stellarvan.stellarDuelBridge.listener;

import java.lang.reflect.Method;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.stellarvan.stellarDuelBridge.compat.DuelDeathMarker;

public final class DuelDeathCompatibilityListener implements Listener {

    private static final String[] TOWNY_PLAYER_DEATH_EVENT_CLASSES = {
        "com.palmergames.bukkit.towny.event.TownyPlayerDeathEvent",
        "com.palmergames.bukkit.towny.event.player.TownyPlayerDeathEvent"
    };
    private final Plugin plugin;

    public DuelDeathCompatibilityListener(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        registerTownyDeathEvent();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!DuelDeathMarker.isDuelDeath(plugin, event.getPlayer())) {
            return;
        }
        event.deathMessage(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!DuelDeathMarker.isDuelDeath(plugin, player)) {
            return;
        }
        event.setDroppedExp(0);
    }

    @SuppressWarnings("unchecked")
    private void registerTownyDeathEvent() {
        if (Bukkit.getPluginManager().getPlugin("Towny") == null) {
            return;
        }
        for (String className : TOWNY_PLAYER_DEATH_EVENT_CLASSES) {
            try {
                Class<?> rawClass = Class.forName(className);
                if (!Event.class.isAssignableFrom(rawClass)) {
                    continue;
                }
                Class<? extends Event> eventClass = (Class<? extends Event>) rawClass;
                Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    this,
                    EventPriority.HIGHEST,
                    this::handleTownyDeathEvent,
                    plugin,
                    true
                );
                plugin.getLogger().info("Towny death compatibility hook registered for " + className + ".");
                return;
            } catch (ClassNotFoundException ignored) {
                // Try next known class path.
            }
        }
    }

    private void handleTownyDeathEvent(Listener listener, Event event) throws EventException {
        if (!isMarkedTownyDuelDeath(event)) {
            return;
        }
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
        suppressTownyMessages(event);
    }

    private boolean isMarkedTownyDuelDeath(Event event) {
        Player player = findTownyPlayer(event, "getPlayer");
        if (player == null) {
            player = findTownyPlayer(event, "getVictim");
        }
        if (player != null && DuelDeathMarker.isDuelDeath(plugin, player)) {
            return true;
        }
        Player killer = findTownyPlayer(event, "getKiller");
        return killer != null && DuelDeathMarker.isDuelDeath(plugin, killer);
    }

    private Player findTownyPlayer(Event event, String methodName) {
        try {
            Method method = event.getClass().getMethod(methodName);
            Object value = method.invoke(event);
            if (value instanceof Player player) {
                return player;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private void suppressTownyMessages(Event event) {
        for (Method method : event.getClass().getMethods()) {
            if (!method.getName().startsWith("set")) {
                continue;
            }
            if (!method.getName().toLowerCase().contains("message")) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            try {
                if (parameterType == String.class) {
                    method.invoke(event, "");
                } else if ("net.kyori.adventure.text.Component".equals(parameterType.getName())) {
                    method.invoke(event, new Object[] {null});
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}

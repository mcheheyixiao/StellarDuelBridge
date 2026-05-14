package org.stellarvan.stellarDuelBridge.command;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.stellarvan.stellarDuelBridge.arena.Arena;
import org.stellarvan.stellarDuelBridge.util.PermissionNodes;

public final class DuelAdminCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("create", "delete", "setspawn", "setspectator", "setreturn", "enable", "disable", "list", "reload");

    private final CommandContext context;

    public DuelAdminCommand(CommandContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionNodes.COMMAND_ADMIN)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return true;
        }
        if (args.length == 0) {
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setspectator" -> handleSetSpectator(sender, args);
            case "setreturn" -> handleSetReturn(sender);
            case "enable" -> handleEnable(sender, args, true);
            case "disable" -> handleEnable(sender, args, false);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && List.of("delete", "setspawn", "setspectator", "enable", "disable").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(context.arenaManager().getArenas().stream().map(Arena::getId).sorted().toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(List.of("1", "2"), args[2]);
        }
        return List.of();
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_CREATE)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (!(sender instanceof Player player) || args.length < 2) {
            context.messageManager().sendMessage(sender, "errors.player-only");
            return;
        }
        Arena arena = context.arenaManager().createArena(args[1], player.getLocation());
        if (arena == null) {
            context.messageManager().sendMessage(sender, "errors.arena-exists");
            return;
        }
        context.messageManager().sendMessage(sender, "admin.arena-created", Map.of("arena", arena.getId()));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_DELETE)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2 || !context.arenaManager().deleteArena(args[1])) {
            context.messageManager().sendMessage(sender, "errors.unknown-arena");
            return;
        }
        context.messageManager().sendMessage(sender, "admin.arena-deleted", Map.of("arena", args[1]));
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_SET_SPAWN)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (!(sender instanceof Player player) || args.length < 3) {
            context.messageManager().sendMessage(sender, "errors.player-only");
            return;
        }
        int spawnIndex;
        try {
            spawnIndex = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            context.messageManager().sendMessage(sender, "errors.invalid-spawn-index");
            return;
        }
        if (spawnIndex != 1 && spawnIndex != 2) {
            context.messageManager().sendMessage(sender, "errors.invalid-spawn-index");
            return;
        }
        if (!context.arenaManager().setSpawn(args[1], spawnIndex, player.getLocation())) {
            context.messageManager().sendMessage(sender, "errors.unknown-arena");
            return;
        }
        context.messageManager().sendMessage(sender, "admin.spawn-set", Map.of("arena", args[1], "spawn", Integer.toString(spawnIndex)));
    }

    private void handleSetSpectator(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_SET_SPECTATOR)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (!(sender instanceof Player player) || args.length < 2) {
            context.messageManager().sendMessage(sender, "errors.player-only");
            return;
        }
        if (!context.arenaManager().setSpectator(args[1], player.getLocation())) {
            context.messageManager().sendMessage(sender, "errors.unknown-arena");
            return;
        }
        context.messageManager().sendMessage(sender, "admin.spectator-set", Map.of("arena", args[1]));
    }

    private void handleSetReturn(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_SET_RETURN)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            context.messageManager().sendMessage(sender, "errors.player-only");
            return;
        }
        context.configManager().setReturnLocation(player.getLocation());
        context.messageManager().sendMessage(sender, "admin.return-set");
    }

    private void handleEnable(CommandSender sender, String[] args, boolean enabled) {
        String permission = enabled ? PermissionNodes.ADMIN_ENABLE : PermissionNodes.ADMIN_DISABLE;
        if (!sender.hasPermission(permission)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        if (args.length < 2) {
            context.messageManager().sendMessage(sender, "errors.unknown-arena");
            return;
        }
        boolean changed = enabled ? context.arenaManager().enableArena(args[1]) : context.arenaManager().disableArena(args[1]);
        if (!changed) {
            context.messageManager().sendMessage(sender, "errors.unknown-arena");
            return;
        }
        context.messageManager().sendMessage(sender, enabled ? "admin.arena-enabled" : "admin.arena-disabled", Map.of("arena", args[1]));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_LIST)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        context.messageManager().sendRawMessage(sender, "admin.arena-list-header", Map.of());
        for (Arena arena : context.arenaManager().getArenas()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arena.getId());
            placeholders.put("enabled", Boolean.toString(arena.isEnabled()));
            placeholders.put("world", arena.getWorldName() == null ? "-" : arena.getWorldName());
            placeholders.put("state", arena.getState().name());
            context.messageManager().sendRawMessage(sender, "admin.arena-list-line", placeholders);
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.ADMIN_RELOAD)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return;
        }
        context.configManager().reload();
        context.messageManager().reload();
        context.arenaManager().loadArenas();
        context.messageManager().sendMessage(sender, "admin.reloaded");
    }

    private List<String> filter(List<String> candidates, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}

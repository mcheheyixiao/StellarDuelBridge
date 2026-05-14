package org.stellarvan.stellarDuelBridge.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.stellarvan.stellarDuelBridge.storage.DuelStats;
import org.stellarvan.stellarDuelBridge.util.PermissionNodes;
import org.stellarvan.stellarDuelBridge.util.TimeUtil;

public final class DuelCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("accept", "deny", "cancel", "leave", "stats", "help");

    private final CommandContext context;

    public DuelCommand(CommandContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionNodes.COMMAND_DUEL)) {
            context.messageManager().sendMessage(sender, "errors.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.messageManager().sendMessage(sender, "errors.player-only");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            context.messageManager().sendRawMessage(player, "help.header", Map.of());
            context.messageManager().sendLines(player, "help.lines");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "accept" -> context.duelSessionManager().acceptInvite(player);
            case "deny" -> context.duelSessionManager().denyInvite(player);
            case "cancel" -> context.duelSessionManager().cancelInvite(player);
            case "leave" -> context.duelSessionManager().leaveDuel(player);
            case "stats" -> handleStats(player, args);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    context.messageManager().sendMessage(player, "errors.player-not-found");
                    return true;
                }
                context.duelSessionManager().sendInvite(player, target);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>(SUBCOMMANDS);
            Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                .sorted(Comparator.naturalOrder())
                .forEach(candidates::add);
            return filter(candidates, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList(), args[1]);
        }
        return List.of();
    }

    private void handleStats(Player sender, String[] args) {
        OfflinePlayer target = sender;
        if (args.length > 1) {
            target = Bukkit.getOfflinePlayer(args[1]);
        }
        String targetName = target.getName() == null ? args.length > 1 ? args[1] : sender.getName() : target.getName();
        context.storageProvider().loadStats(target.getUniqueId(), targetName).whenComplete((stats, throwable) -> {
            Bukkit.getScheduler().runTask(context.plugin(), () -> {
                if (throwable != null) {
                    context.plugin().getLogger().severe("Failed to load duel stats: " + throwable.getMessage());
                    context.messageManager().sendMessage(sender, "errors.database-error");
                    return;
                }
                sendStats(sender, stats);
            });
        });
    }

    private void sendStats(Player sender, DuelStats stats) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", stats.getName());
        placeholders.put("wins", Integer.toString(stats.getWins()));
        placeholders.put("losses", Integer.toString(stats.getLosses()));
        placeholders.put("draws", Integer.toString(stats.getDraws()));
        placeholders.put("quits", Integer.toString(stats.getQuits()));
        placeholders.put("streak", Integer.toString(stats.getCurrentStreak()));
        placeholders.put("best_streak", Integer.toString(stats.getBestStreak()));
        placeholders.put("total_matches", Integer.toString(stats.getTotalMatches()));
        placeholders.put("duration", TimeUtil.formatDurationSeconds(stats.getTotalDurationSeconds()));
        context.messageManager().sendRawMessage(sender, "stats.header", placeholders);
        context.messageManager().sendRawMessage(sender, "stats.line-1", placeholders);
        context.messageManager().sendRawMessage(sender, "stats.line-2", placeholders);
        context.messageManager().sendRawMessage(sender, "stats.line-3", placeholders);
    }

    private List<String> filter(List<String> candidates, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().toList();
    }
}

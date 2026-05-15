package org.stellarvan.stellarDuelBridge.duel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.ZoneId;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.stellarvan.stellarDuelBridge.StellarDuelBridge;
import org.stellarvan.stellarDuelBridge.arena.Arena;
import org.stellarvan.stellarDuelBridge.arena.ArenaManager;
import org.stellarvan.stellarDuelBridge.config.ConfigManager;
import org.stellarvan.stellarDuelBridge.config.DuelSettings;
import org.stellarvan.stellarDuelBridge.config.MessageManager;
import org.stellarvan.stellarDuelBridge.gui.DuelConfirmMenu;
import org.stellarvan.stellarDuelBridge.gui.DuelMenuHolder;
import org.stellarvan.stellarDuelBridge.gui.MenuButton;
import org.stellarvan.stellarDuelBridge.hook.HookManager;
import org.stellarvan.stellarDuelBridge.snapshot.PlayerSnapshot;
import org.stellarvan.stellarDuelBridge.snapshot.SnapshotService;
import org.stellarvan.stellarDuelBridge.storage.DuelStats;
import org.stellarvan.stellarDuelBridge.storage.MatchRecord;
import org.stellarvan.stellarDuelBridge.storage.StorageProvider;

public final class DuelSessionManager {

    private static final long LOCAL_COMBAT_TAG_MILLIS = TimeUnit.SECONDS.toMillis(15);

    private final StellarDuelBridge plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final HookManager hookManager;
    private final ArenaManager arenaManager;
    private final SnapshotService snapshotService;
    private final StorageProvider storageProvider;
    private final DuelConfirmMenu duelConfirmMenu;
    private final Map<UUID, DuelInvite> incomingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, DuelInvite> outgoingInvites = new ConcurrentHashMap<>();
    private final Map<UUID, DuelSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, DuelSession> sessionsById = new ConcurrentHashMap<>();
    private final Set<UUID> internalTeleports = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> localCombatTags = new ConcurrentHashMap<>();
    private volatile boolean acceptingNewDuels = true;

    public DuelSessionManager(
        StellarDuelBridge plugin,
        ConfigManager configManager,
        MessageManager messageManager,
        HookManager hookManager,
        ArenaManager arenaManager,
        SnapshotService snapshotService,
        StorageProvider storageProvider
    ) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.hookManager = hookManager;
        this.arenaManager = arenaManager;
        this.snapshotService = snapshotService;
        this.storageProvider = storageProvider;
        this.duelConfirmMenu = new DuelConfirmMenu(configManager);
    }

    public StellarDuelBridge getPlugin() {
        return plugin;
    }

    public boolean isInDuel(UUID playerId) {
        DuelSession session = playerSessions.get(playerId);
        return session != null && session.getState() != DuelState.CLEANUP;
    }

    public boolean isInternalTeleport(UUID playerId) {
        return internalTeleports.contains(playerId);
    }

    public boolean isCountdownFrozen(UUID playerId) {
        DuelSession session = playerSessions.get(playerId);
        return session != null
            && session.getState() == DuelState.COUNTDOWN
            && configManager.getDuelSettings().combatSettings().freezeDuringCountdown();
    }

    public DuelSession getSession(UUID playerId) {
        return playerSessions.get(playerId);
    }

    public DuelSession getSessionById(UUID sessionId) {
        return sessionsById.get(sessionId);
    }

    public void sendError(CommandSender sender, String path) {
        messageManager.sendMessage(sender, path);
    }

    public void recordExternalCombat(Player attacker, Player victim) {
        if (!configManager.getDuelSettings().preventDuelWhileInCombat()) {
            return;
        }
        long until = System.currentTimeMillis() + LOCAL_COMBAT_TAG_MILLIS;
        localCombatTags.put(attacker.getUniqueId(), until);
        localCombatTags.put(victim.getUniqueId(), until);
    }

    public boolean isDamageAllowed(Player attacker, Player victim, Entity damageSource) {
        DuelSession attackerSession = playerSessions.get(attacker.getUniqueId());
        DuelSession victimSession = playerSessions.get(victim.getUniqueId());
        if (attackerSession == null || victimSession == null) {
            return false;
        }
        if (!attackerSession.getSessionId().equals(victimSession.getSessionId())) {
            return false;
        }
        DuelSession session = attackerSession;
        if (session.getState() != DuelState.FIGHTING) {
            return false;
        }
        if (!Objects.equals(session.getOpponent(attacker.getUniqueId()), victim.getUniqueId())) {
            return false;
        }
        if (!Objects.equals(attacker.getWorld().getName(), victim.getWorld().getName())) {
            return false;
        }
        if (damageSource instanceof Projectile projectile) {
            return isProjectileAllowed(projectile);
        }
        return true;
    }

    public void sendInvite(Player challenger, Player target) {
        DuelSettings settings = configManager.getDuelSettings();
        if (!acceptingNewDuels) {
            return;
        }
        pruneInvite(target.getUniqueId());
        pruneOutgoing(challenger.getUniqueId());
        if (!validateInvite(challenger, target, settings)) {
            return;
        }
        long now = System.currentTimeMillis();
        DuelInvite invite = new DuelInvite(
            UUID.randomUUID(),
            challenger.getUniqueId(),
            target.getUniqueId(),
            challenger.getName(),
            target.getName(),
            now,
            now + TimeUnit.SECONDS.toMillis(settings.inviteExpireSeconds())
        );
        incomingInvites.put(target.getUniqueId(), invite);
        outgoingInvites.put(challenger.getUniqueId(), invite);
        requestCooldowns.put(challenger.getUniqueId(), now + TimeUnit.SECONDS.toMillis(settings.requestCooldownSeconds()));
        messageManager.sendMessage(challenger, "duel.invite-sent", Map.of("target", target.getName()));
        messageManager.sendMessage(target, "duel.invite-received", Map.of("challenger", challenger.getName()));
    }

    public void acceptInvite(Player target) {
        pruneInvite(target.getUniqueId());
        DuelInvite invite = incomingInvites.remove(target.getUniqueId());
        if (invite == null) {
            messageManager.sendMessage(target, "errors.no-pending-invite");
            return;
        }
        outgoingInvites.remove(invite.getChallenger());
        Player challenger = Bukkit.getPlayer(invite.getChallenger());
        if (challenger == null || !challenger.isOnline()) {
            messageManager.sendMessage(target, "errors.player-offline-during-setup");
            return;
        }
        if (isInDuel(challenger.getUniqueId()) || isInDuel(target.getUniqueId())) {
            messageManager.sendMessage(target, "errors.already-in-duel");
            return;
        }

        DuelSession session = new DuelSession(
            UUID.randomUUID(),
            challenger.getUniqueId(),
            target.getUniqueId(),
            challenger.getName(),
            target.getName(),
            configManager.getDuelSettings().defaultMode()
        );
        session.setState(DuelState.MODE_SELECTING);
        session.setChallengerConfirmed(false);
        session.setTargetConfirmed(false);
        registerSession(session);
        messageManager.sendMessage(challenger, "duel.accepted");
        messageManager.sendMessage(target, "duel.accepted");
        openContractMenus(session);
        scheduleContractTimeout(session);
    }

    public void denyInvite(Player target) {
        pruneInvite(target.getUniqueId());
        DuelInvite invite = incomingInvites.remove(target.getUniqueId());
        if (invite == null) {
            messageManager.sendMessage(target, "errors.no-pending-invite");
            return;
        }
        outgoingInvites.remove(invite.getChallenger());
        messageManager.sendMessage(target, "duel.invite-denied", Map.of("challenger", invite.getChallengerName()));
        Player challenger = Bukkit.getPlayer(invite.getChallenger());
        if (challenger != null) {
            messageManager.sendMessage(challenger, "duel.invite-cancelled");
        }
    }

    public void cancelInvite(Player challenger) {
        DuelInvite invite = outgoingInvites.remove(challenger.getUniqueId());
        if (invite == null) {
            messageManager.sendMessage(challenger, "errors.no-pending-invite");
            return;
        }
        incomingInvites.remove(invite.getTarget());
        messageManager.sendMessage(challenger, "duel.invite-cancelled");
        Player target = Bukkit.getPlayer(invite.getTarget());
        if (target != null) {
            messageManager.sendMessage(target, "duel.invite-cancelled");
        }
    }

    public void leaveDuel(Player player) {
        DuelSession session = playerSessions.get(player.getUniqueId());
        if (session == null) {
            messageManager.sendMessage(player, "errors.no-active-duel");
            return;
        }
        UUID opponent = session.getOpponent(player.getUniqueId());
        if (opponent == null) {
            cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            return;
        }
        concludeWithWinner(session, opponent, player.getUniqueId(), DuelEndReason.SURRENDER);
    }

    public void handleMenuClick(Player player, DuelMenuHolder holder, int rawSlot) {
        DuelSession session = sessionsById.get(holder.getSessionId());
        if (session == null || session.getState() != DuelState.MODE_SELECTING) {
            player.closeInventory();
            return;
        }
        if (!holder.getViewerId().equals(player.getUniqueId())) {
            return;
        }
        MenuButton clickedButton = resolveButtonBySlot(rawSlot);
        if (clickedButton == null) {
            return;
        }
        DuelMode mode = DuelMode.fromButtonKey(clickedButton.key());
        if (mode != null) {
            if (!configManager.getDuelSettings().allowPlayerSelection()) {
                return;
            }
            if (!configManager.getDuelSettings().isModeEnabled(mode)) {
                messageManager.sendMessage(player, "errors.invalid-mode");
                return;
            }
            session.setSelectedMode(mode);
            session.setChallengerConfirmed(false);
            session.setTargetConfirmed(false);
            holder.setSelectedMode(mode);
            messageManager.sendMessage(player, "duel.mode-selected", Map.of("mode", configManager.getDuelSettings().getModeDisplayName(mode)));
            refreshContractMenus(session);
            return;
        }
        switch (clickedButton.key().toLowerCase(Locale.ROOT)) {
            case "confirm" -> {
                session.setPlayerConfirmed(player.getUniqueId(), true);
                if (session.areBothConfirmed()) {
                    closeContractMenus(session);
                    beginSessionPreparation(session);
                } else {
                    refreshContractMenus(session);
                    messageManager.sendMessage(player, "duel.contract-waiting");
                }
            }
            case "deny" -> {
                closeContractMenus(session);
                cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            }
            default -> {
            }
        }
    }

    public void handleMenuClose(Player player, DuelMenuHolder holder) {
        DuelSession session = sessionsById.get(holder.getSessionId());
        if (session == null || session.getState() != DuelState.MODE_SELECTING) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelSession latest = sessionsById.get(holder.getSessionId());
            if (latest == null || latest.getState() != DuelState.MODE_SELECTING || !player.isOnline()) {
                return;
            }
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof DuelMenuHolder)) {
                duelConfirmMenu.open(player, latest);
            }
        }, 1L);
    }

    public void handlePlayerDeath(Player player) {
        DuelSession session = playerSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        UUID opponent = session.getOpponent(player.getUniqueId());
        if (opponent == null) {
            cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            return;
        }
        concludeWithWinner(session, opponent, player.getUniqueId(), DuelEndReason.DEATH);
    }

    public void handlePlayerQuit(Player player) {
        DuelSession session = playerSessions.get(player.getUniqueId());
        if (session == null) {
            outgoingInvites.remove(player.getUniqueId());
            incomingInvites.remove(player.getUniqueId());
            return;
        }
        UUID opponent = session.getOpponent(player.getUniqueId());
        if (opponent == null) {
            cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            return;
        }
        concludeWithWinner(session, opponent, player.getUniqueId(), DuelEndReason.QUIT);
    }

    public void handleIllegalWorldChange(Player player) {
        DuelSession session = playerSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        UUID opponent = session.getOpponent(player.getUniqueId());
        if (opponent == null) {
            cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            return;
        }
        concludeWithWinner(session, opponent, player.getUniqueId(), DuelEndReason.QUIT);
    }

    public void applyDeferredRestore(Player player) {
        SnapshotService.DeferredRestore restore = snapshotService.consumeDeferredRestore(player.getUniqueId());
        if (restore == null) {
            return;
        }
        try {
            snapshotService.restore(player, restore.snapshot(), configManager.getDuelSettings(), restore.mode());
            snapshotService.clearPendingRestore(player.getUniqueId());
            returnPlayer(player, restore.returnLocation());
        } catch (Exception exception) {
            snapshotService.queueDeferredRestore(player.getUniqueId(), restore.snapshot(), restore.returnLocation(), restore.mode());
            plugin.getLogger().severe("Failed to apply deferred restore for " + player.getName() + ": " + exception.getMessage());
        }
    }

    public void shutdown() {
        acceptingNewDuels = false;
        incomingInvites.clear();
        outgoingInvites.clear();
        Set<UUID> sessionIds = new HashSet<>(sessionsById.keySet());
        for (UUID sessionId : sessionIds) {
            DuelSession session = sessionsById.get(sessionId);
            if (session != null) {
                cancelSession(session, DuelEndReason.PLUGIN_DISABLE);
            }
        }
    }

    private boolean validateInvite(Player challenger, Player target, DuelSettings settings) {
        if (settings.preventSelfDuel() && challenger.getUniqueId().equals(target.getUniqueId())) {
            messageManager.sendMessage(challenger, "errors.self-duel");
            return false;
        }
        if (isInDuel(challenger.getUniqueId())) {
            messageManager.sendMessage(challenger, "errors.already-in-duel");
            return false;
        }
        if (isInDuel(target.getUniqueId())) {
            messageManager.sendMessage(challenger, "errors.target-in-duel");
            return false;
        }
        if (incomingInvites.containsKey(target.getUniqueId()) || outgoingInvites.containsKey(challenger.getUniqueId())) {
            messageManager.sendMessage(challenger, "errors.invite-pending");
            return false;
        }
        if (!challenger.hasPermission("stellarduelbridge.bypass.cooldown")) {
            long cooldownUntil = requestCooldowns.getOrDefault(challenger.getUniqueId(), 0L);
            if (cooldownUntil > System.currentTimeMillis()) {
                long seconds = Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(cooldownUntil - System.currentTimeMillis()));
                messageManager.sendMessage(challenger, "errors.cooldown", Map.of("seconds", Long.toString(seconds)));
                return false;
            }
        }
        if (settings.preventDuelInDisabledWorlds() && !challenger.hasPermission("stellarduelbridge.bypass.disabled-world")) {
            if (settings.disabledRequestWorlds().stream().anyMatch(world -> world.equalsIgnoreCase(challenger.getWorld().getName()))) {
                messageManager.sendMessage(challenger, "errors.disabled-world");
                return false;
            }
        }
        if (settings.requireSameWorldRequest() || !settings.allowCrossWorldRequest()) {
            if (!challenger.getWorld().equals(target.getWorld())) {
                messageManager.sendMessage(challenger, "errors.same-world-required");
                return false;
            }
        }
        if (settings.preventDuelWhileFlying() && (challenger.isFlying() || target.isFlying())) {
            messageManager.sendMessage(challenger, "errors.flying");
            return false;
        }
        if (settings.preventDuelWhileVanished() && (isVanished(challenger) || isVanished(target))) {
            messageManager.sendMessage(challenger, "errors.vanished");
            return false;
        }
        if (settings.preventSameIpDuel() && challenger.getAddress() != null && target.getAddress() != null
            && Objects.equals(challenger.getAddress().getAddress(), target.getAddress().getAddress())) {
            messageManager.sendMessage(challenger, "errors.same-ip");
            return false;
        }
        if (settings.preventDuelWhileInCombat() && (isInLocalCombat(challenger) || isInLocalCombat(target))) {
            messageManager.sendMessage(challenger, "errors.in-combat");
            return false;
        }
        return true;
    }

    private boolean isInLocalCombat(Player player) {
        return localCombatTags.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
    }

    private void pruneInvite(UUID targetId) {
        DuelInvite invite = incomingInvites.get(targetId);
        if (invite != null && invite.isExpired(System.currentTimeMillis())) {
            incomingInvites.remove(targetId);
            outgoingInvites.remove(invite.getChallenger());
        }
    }

    private void pruneOutgoing(UUID challengerId) {
        DuelInvite invite = outgoingInvites.get(challengerId);
        if (invite != null && invite.isExpired(System.currentTimeMillis())) {
            outgoingInvites.remove(challengerId);
            incomingInvites.remove(invite.getTarget());
        }
    }

    private void registerSession(DuelSession session) {
        sessionsById.put(session.getSessionId(), session);
        playerSessions.put(session.getPlayerOne(), session);
        playerSessions.put(session.getPlayerTwo(), session);
    }

    private void unregisterSession(DuelSession session) {
        sessionsById.remove(session.getSessionId());
        playerSessions.remove(session.getPlayerOne(), session);
        playerSessions.remove(session.getPlayerTwo(), session);
        session.setState(DuelState.CLEANUP);
    }

    private MenuButton resolveButtonBySlot(int slot) {
        return configManager.getGuiSettings().duelConfirmSettings().buttons().values().stream()
            .filter(button -> button.slot() == slot)
            .findFirst()
            .orElse(null);
    }

    private void openContractMenus(DuelSession session) {
        Player challenger = Bukkit.getPlayer(session.getPlayerOne());
        Player target = Bukkit.getPlayer(session.getPlayerTwo());
        if (challenger != null && challenger.isOnline()) {
            duelConfirmMenu.open(challenger, session);
        }
        if (target != null && target.isOnline()) {
            duelConfirmMenu.open(target, session);
        }
    }

    private void refreshContractMenus(DuelSession session) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            DuelSession live = sessionsById.get(session.getSessionId());
            if (live == null || live.getState() != DuelState.MODE_SELECTING) {
                return;
            }
            openContractMenus(live);
        });
    }

    private void closeContractMenus(DuelSession session) {
        Player challenger = Bukkit.getPlayer(session.getPlayerOne());
        Player target = Bukkit.getPlayer(session.getPlayerTwo());
        closeContractMenuIfOpen(challenger, session.getSessionId());
        closeContractMenuIfOpen(target, session.getSessionId());
    }

    private void closeContractMenuIfOpen(Player player, UUID sessionId) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof DuelMenuHolder holder)) {
            return;
        }
        if (!holder.getSessionId().equals(sessionId)) {
            return;
        }
        player.closeInventory();
    }

    private void scheduleContractTimeout(DuelSession session) {
        int timeoutSeconds = configManager.getDuelSettings().contractConfirmTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            return;
        }
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelSession live = sessionsById.get(session.getSessionId());
            if (live == null || live.getState() != DuelState.MODE_SELECTING || live.areBothConfirmed()) {
                return;
            }
            closeContractMenus(live);
            Player challenger = Bukkit.getPlayer(live.getPlayerOne());
            Player target = Bukkit.getPlayer(live.getPlayerTwo());
            if (challenger != null) {
                messageManager.sendMessage(challenger, "errors.contract-confirm-timeout");
            }
            if (target != null) {
                messageManager.sendMessage(target, "errors.contract-confirm-timeout");
            }
            cancelSession(live, DuelEndReason.ADMIN_CANCEL);
        }, timeoutSeconds * 20L);
        session.setContractTimeoutTask(timeoutTask);
    }

    private void beginSessionPreparation(DuelSession session) {
        if (session.getState() != DuelState.MODE_SELECTING) {
            return;
        }
        if (!session.areBothConfirmed()) {
            return;
        }
        BukkitTask contractTask = session.getContractTimeoutTask();
        if (contractTask != null) {
            contractTask.cancel();
            session.setContractTimeoutTask(null);
        }
        Player playerOne = Bukkit.getPlayer(session.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(session.getPlayerTwo());
        if (playerOne == null || playerTwo == null || !playerOne.isOnline() || !playerTwo.isOnline()) {
            if (playerOne != null) {
                messageManager.sendMessage(playerOne, "errors.player-offline-during-setup");
            }
            if (playerTwo != null) {
                messageManager.sendMessage(playerTwo, "errors.player-offline-during-setup");
            }
            cancelSession(session, DuelEndReason.ADMIN_CANCEL);
            return;
        }

        Arena arena = arenaManager.getAvailableArena();
        if (arena == null) {
            messageManager.sendMessage(playerOne, "errors.arena-not-found");
            messageManager.sendMessage(playerTwo, "errors.arena-not-found");
            cancelSession(session, DuelEndReason.NO_ARENA);
            return;
        }
        World world = arena.getWorld();
        if (world == null) {
            messageManager.sendMessage(playerOne, "errors.arena-world-not-loaded");
            messageManager.sendMessage(playerTwo, "errors.arena-world-not-loaded");
            cancelSession(session, DuelEndReason.NO_ARENA);
            return;
        }

        session.setState(DuelState.PREPARING);
        session.setArenaId(arena.getId());
        session.setMode(session.getSelectedMode());
        session.setPlayerOneIp(resolvePlayerIp(playerOne));
        session.setPlayerTwoIp(resolvePlayerIp(playerTwo));
        session.setPlayerOneSnapshot(snapshotService.capture(playerOne));
        session.setPlayerTwoSnapshot(snapshotService.capture(playerTwo));
        Location playerOneReturn = snapshotService.determineReturnLocation(
            session.getPlayerOneSnapshot(),
            configManager.getDuelSettings(),
            configManager.getReturnLocation()
        );
        Location playerTwoReturn = snapshotService.determineReturnLocation(
            session.getPlayerTwoSnapshot(),
            configManager.getDuelSettings(),
            configManager.getReturnLocation()
        );
        snapshotService.registerPendingRestore(session.getPlayerOne(), session.getPlayerOneSnapshot(), playerOneReturn, session.getMode());
        snapshotService.registerPendingRestore(session.getPlayerTwo(), session.getPlayerTwoSnapshot(), playerTwoReturn, session.getMode());
        arenaManager.markOccupied(arena.getId());
        messageManager.sendMessage(playerOne, "duel.preparing", Map.of("arena", arena.getId()));
        messageManager.sendMessage(playerTwo, "duel.preparing", Map.of("arena", arena.getId()));

        preparePlayerForMode(playerOne, session.getMode());
        preparePlayerForMode(playerTwo, session.getMode());
        startArenaTeleport(session, playerOne, playerTwo, arena);
    }

    private void preparePlayerForMode(Player player, DuelMode mode) {
        switch (mode) {
            case REAL_GEAR -> {
                if (configManager.getDuelSettings().realGearSettings().clearPotionEffectsOnStart()) {
                    clearPotionEffects(player);
                }
            }
            case FAIR_KIT -> {
                clearInventory(player);
                if (configManager.getDuelSettings().fairKitSettings().clearPotionEffectsOnStart()) {
                    clearPotionEffects(player);
                }
                DuelSettings.KitDefinition kit = configManager.getDuelSettings().fairKitSettings().kits()
                    .get(configManager.getDuelSettings().fairKitSettings().defaultKit());
                if (kit != null) {
                    PlayerInventory inventory = player.getInventory();
                    kit.cloneContents().forEach(inventory::setItem);
                    inventory.setHelmet(kit.getHelmet());
                    inventory.setChestplate(kit.getChestplate());
                    inventory.setLeggings(kit.getLeggings());
                    inventory.setBoots(kit.getBoots());
                }
            }
            case EMPTY_RITUAL -> {
                clearInventory(player);
                if (configManager.getDuelSettings().emptyRitualSettings().giveBasicWeapon()
                    && configManager.getDuelSettings().emptyRitualSettings().weapon() != null) {
                    player.getInventory().setItem(0, configManager.getDuelSettings().emptyRitualSettings().weapon().clone());
                }
            }
        }
        player.setFallDistance(0F);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFoodLevel(Math.max(player.getFoodLevel(), 20));
        player.setSaturation(Math.max(player.getSaturation(), 20.0F));
        player.updateInventory();
    }

    private void startArenaTeleport(DuelSession session, Player playerOne, Player playerTwo, Arena arena) {
        session.setState(DuelState.TELEPORTING);
        Location spawn1 = arena.getSpawn1();
        Location spawn2 = arena.getSpawn2();
        internalTeleports.add(playerOne.getUniqueId());
        internalTeleports.add(playerTwo.getUniqueId());

        CompletableFuture<Void> preload = CompletableFuture.allOf(preloadChunk(spawn1), preloadChunk(spawn2));
        preload
            .thenCompose(ignored -> {
                CompletableFuture<Boolean> firstTeleport = playerOne.teleportAsync(spawn1);
                CompletableFuture<Boolean> secondTeleport = playerTwo.teleportAsync(spawn2);
                return CompletableFuture.allOf(firstTeleport, secondTeleport)
                    .thenApply(v -> Boolean.TRUE.equals(firstTeleport.join()) && Boolean.TRUE.equals(secondTeleport.join()));
            })
            .whenComplete((success, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                internalTeleports.remove(playerOne.getUniqueId());
                internalTeleports.remove(playerTwo.getUniqueId());
                DuelSession liveSession = sessionsById.get(session.getSessionId());
                if (liveSession == null || liveSession.getState() != DuelState.TELEPORTING) {
                    return;
                }
                if (throwable != null || !Boolean.TRUE.equals(success) || !playerOne.isOnline() || !playerTwo.isOnline()) {
                    cancelSession(session, DuelEndReason.TELEPORT_FAILED);
                    return;
                }
                int invulnerableTicks = configManager.getDuelSettings().combatSettings().invulnerableAfterTeleportTicks();
                playerOne.setNoDamageTicks(invulnerableTicks);
                playerTwo.setNoDamageTicks(invulnerableTicks);
                beginCountdown(session, playerOne, playerTwo);
            }));
    }

    private CompletableFuture<Chunk> preloadChunk(Location location) {
        if (location == null || location.getWorld() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return location.getWorld().getChunkAtAsync(location);
    }

    private void beginCountdown(DuelSession session, Player playerOne, Player playerTwo) {
        session.setState(DuelState.COUNTDOWN);
        int countdownSeconds = configManager.getDuelSettings().countdownSeconds();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = countdownSeconds;

            @Override
            public void run() {
                DuelSession liveSession = sessionsById.get(session.getSessionId());
                if (liveSession == null || liveSession.getState() != DuelState.COUNTDOWN) {
                    BukkitTask runningTask = session.getCountdownTask();
                    if (runningTask != null) {
                        runningTask.cancel();
                    }
                    return;
                }
                if (!playerOne.isOnline() || !playerTwo.isOnline()) {
                    UUID loser = !playerOne.isOnline() ? playerOne.getUniqueId() : playerTwo.getUniqueId();
                    concludeWithWinner(session, session.getOpponent(loser), loser, DuelEndReason.QUIT);
                    return;
                }
                if (remaining <= 0) {
                    startFight(session, playerOne, playerTwo);
                    return;
                }
                Map<String, String> placeholders = Map.of("seconds", Integer.toString(remaining));
                messageManager.sendMessage(playerOne, "duel.countdown", placeholders);
                messageManager.sendMessage(playerTwo, "duel.countdown", placeholders);
                remaining--;
            }
        }, 0L, 20L);
        session.setCountdownTask(task);
    }

    private void startFight(DuelSession session, Player playerOne, Player playerTwo) {
        session.cancelTasks();
        session.setState(DuelState.FIGHTING);
        session.setStartedAt(System.currentTimeMillis() / 1000L);
        playerOne.setNoDamageTicks(0);
        playerTwo.setNoDamageTicks(0);
        messageManager.sendMessage(playerOne, "duel.started");
        messageManager.sendMessage(playerTwo, "duel.started");
        scheduleTimeout(session);
    }

    private void scheduleTimeout(DuelSession session) {
        int seconds = configManager.getDuelSettings().maxDurationSeconds();
        if (seconds <= 0) {
            return;
        }
        session.setTimeoutTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelSession liveSession = sessionsById.get(session.getSessionId());
            if (liveSession == null || liveSession.getState() != DuelState.FIGHTING) {
                return;
            }
            concludeDraw(session, DuelEndReason.TIMEOUT);
        }, seconds * 20L));
    }

    private boolean isProjectileAllowed(Projectile projectile) {
        DuelSettings.CombatSettings combat = configManager.getDuelSettings().combatSettings();
        if (projectile instanceof Trident) {
            return combat.allowTrident();
        }
        if (projectile instanceof org.bukkit.entity.AbstractArrow) {
            return combat.allowBow() || combat.allowCrossbow();
        }
        return true;
    }

    private void concludeWithWinner(DuelSession session, UUID winnerId, UUID loserId, DuelEndReason reason) {
        DuelResult result = session.getPlayerOne().equals(winnerId) ? DuelResult.PLAYER_ONE_WIN : DuelResult.PLAYER_TWO_WIN;
        completeSession(session, result, winnerId, loserId, reason, true);
    }

    private void concludeDraw(DuelSession session, DuelEndReason reason) {
        completeSession(session, DuelResult.DRAW, null, null, reason, true);
    }

    private void cancelSession(DuelSession session, DuelEndReason reason) {
        completeSession(session, DuelResult.CANCELLED, null, null, reason, false);
    }

    private void completeSession(DuelSession session, DuelResult result, UUID winnerId, UUID loserId, DuelEndReason reason, boolean record) {
        DuelSession liveSession = sessionsById.get(session.getSessionId());
        if (liveSession == null) {
            return;
        }
        if (session.getState() == DuelState.ENDING || session.getState() == DuelState.RETURNING || session.getState() == DuelState.CLEANUP) {
            return;
        }
        session.cancelTasks();
        session.setState(DuelState.ENDING);
        session.setWinner(winnerId);
        session.setLoser(loserId);
        session.setEndReason(reason);
        session.setEndedAt(System.currentTimeMillis() / 1000L);
        if (session.getArenaId() != null) {
            arenaManager.releaseArena(session.getArenaId());
        }

        Player playerOne = Bukkit.getPlayer(session.getPlayerOne());
        Player playerTwo = Bukkit.getPlayer(session.getPlayerTwo());
        sendOutcomeMessages(session, result, playerOne, playerTwo);
        if (record && result != DuelResult.CANCELLED) {
            persistResult(session, result);
        }
        executeRewards(session, result);
        session.setState(DuelState.RETURNING);
        unregisterSession(session);
        if (playerOne != null) {
            processParticipantReturn(playerOne, session.getPlayerOneSnapshot(), session.getMode());
        } else if (session.getPlayerOneSnapshot() != null && session.getMode() != null) {
            queueDeferredReturn(session.getPlayerOne(), session.getPlayerOneSnapshot(), session.getMode());
        }
        if (playerTwo != null) {
            processParticipantReturn(playerTwo, session.getPlayerTwoSnapshot(), session.getMode());
        } else if (session.getPlayerTwoSnapshot() != null && session.getMode() != null) {
            queueDeferredReturn(session.getPlayerTwo(), session.getPlayerTwoSnapshot(), session.getMode());
        }
    }

    private void sendOutcomeMessages(DuelSession session, DuelResult result, Player playerOne, Player playerTwo) {
        if (session.getEndReason() == DuelEndReason.SURRENDER && session.getLoser() != null) {
            String loserName = session.getPlayerOne().equals(session.getLoser()) ? session.getPlayerOneName() : session.getPlayerTwoName();
            if (playerOne != null) {
                messageManager.sendMessage(playerOne, "duel.surrender", Map.of("player", loserName));
            }
            if (playerTwo != null) {
                messageManager.sendMessage(playerTwo, "duel.surrender", Map.of("player", loserName));
            }
        }
        if (session.getEndReason() == DuelEndReason.QUIT && session.getLoser() != null) {
            String loserName = session.getPlayerOne().equals(session.getLoser()) ? session.getPlayerOneName() : session.getPlayerTwoName();
            if (playerOne != null) {
                messageManager.sendMessage(playerOne, "duel.quit-lose", Map.of("player", loserName));
            }
            if (playerTwo != null) {
                messageManager.sendMessage(playerTwo, "duel.quit-lose", Map.of("player", loserName));
            }
        }
        switch (result) {
            case PLAYER_ONE_WIN, PLAYER_TWO_WIN -> {
                String winnerName = session.getPlayerOne().equals(session.getWinner()) ? session.getPlayerOneName() : session.getPlayerTwoName();
                String loserName = session.getPlayerOne().equals(session.getLoser()) ? session.getPlayerOneName() : session.getPlayerTwoName();
                Map<String, String> placeholders = Map.of("winner", winnerName, "loser", loserName);
                if (playerOne != null) {
                    messageManager.sendMessage(playerOne, "duel.victory", placeholders);
                }
                if (playerTwo != null) {
                    messageManager.sendMessage(playerTwo, "duel.victory", placeholders);
                }
            }
            case DRAW -> {
                if (playerOne != null) {
                    messageManager.sendMessage(playerOne, "duel.draw");
                }
                if (playerTwo != null) {
                    messageManager.sendMessage(playerTwo, "duel.draw");
                }
            }
            case CANCELLED -> {
                if (session.getEndReason() == DuelEndReason.TELEPORT_FAILED) {
                    if (playerOne != null) {
                        messageManager.sendMessage(playerOne, "errors.teleport-failed");
                    }
                    if (playerTwo != null) {
                        messageManager.sendMessage(playerTwo, "errors.teleport-failed");
                    }
                }
            }
        }
    }

    private void executeRewards(DuelSession session, DuelResult result) {
        DuelSettings.RewardSettings rewards = configManager.getDuelSettings().rewardSettings();
        if (!rewards.enabled()) {
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("winner", session.getWinner() == null ? "" : session.getPlayerOne().equals(session.getWinner()) ? session.getPlayerOneName() : session.getPlayerTwoName());
        placeholders.put("loser", session.getLoser() == null ? "" : session.getPlayerOne().equals(session.getLoser()) ? session.getPlayerOneName() : session.getPlayerTwoName());
        placeholders.put("player_one", session.getPlayerOneName());
        placeholders.put("player_two", session.getPlayerTwoName());
        placeholders.put("mode", session.getMode() == null ? "" : session.getMode().name());
        placeholders.put("arena", session.getArenaId() == null ? "" : session.getArenaId());
        List<String> commands = switch (result) {
            case PLAYER_ONE_WIN, PLAYER_TWO_WIN -> rewards.winnerCommands();
            case DRAW -> rewards.drawCommands();
            case CANCELLED -> List.of();
        };
        List<String> loserCommands = result == DuelResult.PLAYER_ONE_WIN || result == DuelResult.PLAYER_TWO_WIN ? rewards.loserCommands() : List.of();
        runRewardCommands(commands, placeholders);
        runRewardCommands(loserCommands, placeholders);
    }

    private void runRewardCommands(List<String> commands, Map<String, String> placeholders) {
        for (String command : commands) {
            String resolved = command;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
    }

    private void processParticipantReturn(Player player, PlayerSnapshot snapshot, DuelMode mode) {
        if (snapshot == null || mode == null) {
            return;
        }
        Location target = snapshotService.determineReturnLocation(snapshot, configManager.getDuelSettings(), configManager.getReturnLocation());
        if (player.isDead() || !player.isOnline()) {
            snapshotService.queueDeferredRestore(player.getUniqueId(), snapshot, target, mode);
            return;
        }
        try {
            snapshotService.restore(player, snapshot, configManager.getDuelSettings(), mode);
            snapshotService.clearPendingRestore(player.getUniqueId());
            returnPlayer(player, target);
        } catch (Exception exception) {
            snapshotService.queueDeferredRestore(player.getUniqueId(), snapshot, target, mode);
            plugin.getLogger().severe("Failed to restore snapshot for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private void queueDeferredReturn(UUID playerId, PlayerSnapshot snapshot, DuelMode mode) {
        Location target = snapshotService.determineReturnLocation(snapshot, configManager.getDuelSettings(), configManager.getReturnLocation());
        snapshotService.queueDeferredRestore(playerId, snapshot, target, mode);
    }

    private void returnPlayer(Player player, Location target) {
        if (target == null || target.getWorld() == null) {
            messageManager.sendMessage(player, "duel.returned");
            return;
        }
        internalTeleports.add(player.getUniqueId());
        player.teleportAsync(target).whenComplete((success, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
            internalTeleports.remove(player.getUniqueId());
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                player.teleport(target);
            }
            messageManager.sendMessage(player, "duel.returned");
        }));
    }

    private void persistResult(DuelSession session, DuelResult result) {
        int duration = Math.max(0, (int) (session.getEndedAt() - Math.max(session.getCreatedAt() / 1000L, session.getStartedAt())));
        String winnerName = session.getWinner() == null ? null : session.getPlayerOne().equals(session.getWinner()) ? session.getPlayerOneName() : session.getPlayerTwoName();
        String loserName = session.getLoser() == null ? null : session.getPlayerOne().equals(session.getLoser()) ? session.getPlayerOneName() : session.getPlayerTwoName();
        long now = System.currentTimeMillis() / 1000L;
        long dayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long dayEnd = dayStart + TimeUnit.DAYS.toSeconds(1);
        long pairWindowStart = now - TimeUnit.HOURS.toSeconds(24);

        CompletableFuture<DuelStats> playerOneStatsFuture = storageProvider.loadStats(session.getPlayerOne(), session.getPlayerOneName());
        CompletableFuture<DuelStats> playerTwoStatsFuture = storageProvider.loadStats(session.getPlayerTwo(), session.getPlayerTwoName());
        CompletableFuture<Integer> pairCountFuture = storageProvider.countPairMatchesSince(session.getPlayerOne(), session.getPlayerTwo(), pairWindowStart);
        CompletableFuture<Integer> playerOneDailyHonorFuture = storageProvider.getDailyPositiveHonor(session.getPlayerOne(), dayStart, dayEnd);
        CompletableFuture<Integer> playerTwoDailyHonorFuture = storageProvider.getDailyPositiveHonor(session.getPlayerTwo(), dayStart, dayEnd);

        CompletableFuture.allOf(
            playerOneStatsFuture,
            playerTwoStatsFuture,
            pairCountFuture,
            playerOneDailyHonorFuture,
            playerTwoDailyHonorFuture
        ).thenCompose(ignored -> {
            DuelStats playerOneStats = playerOneStatsFuture.join();
            DuelStats playerTwoStats = playerTwoStatsFuture.join();
            int pairCount = pairCountFuture.join();
            int playerOneDailyHonor = playerOneDailyHonorFuture.join();
            int playerTwoDailyHonor = playerTwoDailyHonorFuture.join();

            HonorDelta honorDelta = computeHonorDeltas(session, result, pairCount, playerOneDailyHonor, playerTwoDailyHonor);
            updateStats(playerOneStats, playerTwoStats, session, result, duration, honorDelta);

            MatchRecord record = new MatchRecord(
                session.getArenaId() == null ? "unknown" : session.getArenaId(),
                session.getMode() == null ? configManager.getDuelSettings().defaultMode().name() : session.getMode().name(),
                session.getPlayerOne().toString(),
                session.getPlayerOneName(),
                session.getPlayerTwo().toString(),
                session.getPlayerTwoName(),
                session.getWinner() == null ? null : session.getWinner().toString(),
                winnerName,
                session.getLoser() == null ? null : session.getLoser().toString(),
                loserName,
                result.name(),
                session.getEndReason().name(),
                session.getStartedAt() > 0L ? session.getStartedAt() : session.getCreatedAt() / 1000L,
                session.getEndedAt(),
                duration,
                honorDelta.playerOneDelta(),
                honorDelta.playerTwoDelta(),
                now
            );

            return storageProvider.saveStats(playerOneStats)
                .thenCompose(v -> storageProvider.saveStats(playerTwoStats))
                .thenCompose(v -> storageProvider.recordMatch(record));
        })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Failed to persist duel result: " + throwable.getMessage());
                return null;
            });
    }

    private void updateStats(
        DuelStats playerOneStats,
        DuelStats playerTwoStats,
        DuelSession session,
        DuelResult result,
        int duration,
        HonorDelta honorDelta
    ) {
        long now = System.currentTimeMillis() / 1000L;
        playerOneStats.setName(session.getPlayerOneName());
        playerTwoStats.setName(session.getPlayerTwoName());
        playerOneStats.setLastMode(session.getMode().name());
        playerTwoStats.setLastMode(session.getMode().name());
        playerOneStats.setLastMatchAt(now);
        playerTwoStats.setLastMatchAt(now);
        playerOneStats.setTotalMatches(playerOneStats.getTotalMatches() + 1);
        playerTwoStats.setTotalMatches(playerTwoStats.getTotalMatches() + 1);
        playerOneStats.setTotalDurationSeconds(playerOneStats.getTotalDurationSeconds() + duration);
        playerTwoStats.setTotalDurationSeconds(playerTwoStats.getTotalDurationSeconds() + duration);

        switch (result) {
            case PLAYER_ONE_WIN -> {
                playerOneStats.setWins(playerOneStats.getWins() + 1);
                playerTwoStats.setLosses(playerTwoStats.getLosses() + 1);
                playerOneStats.setCurrentStreak(playerOneStats.getCurrentStreak() + 1);
                playerOneStats.setBestStreak(Math.max(playerOneStats.getBestStreak(), playerOneStats.getCurrentStreak()));
                playerTwoStats.setCurrentStreak(0);
                if (session.getEndReason() == DuelEndReason.QUIT) {
                    playerTwoStats.setQuits(playerTwoStats.getQuits() + 1);
                }
            }
            case PLAYER_TWO_WIN -> {
                playerTwoStats.setWins(playerTwoStats.getWins() + 1);
                playerOneStats.setLosses(playerOneStats.getLosses() + 1);
                playerTwoStats.setCurrentStreak(playerTwoStats.getCurrentStreak() + 1);
                playerTwoStats.setBestStreak(Math.max(playerTwoStats.getBestStreak(), playerTwoStats.getCurrentStreak()));
                playerOneStats.setCurrentStreak(0);
                if (session.getEndReason() == DuelEndReason.QUIT) {
                    playerOneStats.setQuits(playerOneStats.getQuits() + 1);
                }
            }
            case DRAW -> {
                playerOneStats.setDraws(playerOneStats.getDraws() + 1);
                playerTwoStats.setDraws(playerTwoStats.getDraws() + 1);
                playerOneStats.setCurrentStreak(0);
                playerTwoStats.setCurrentStreak(0);
            }
            case CANCELLED -> {
            }
        }
        int prestigeThreshold = configManager.getDuelSettings().honorSettings().prestigeThreshold();
        applyHonorWithPrestige(playerOneStats, honorDelta.playerOneDelta(), prestigeThreshold);
        applyHonorWithPrestige(playerTwoStats, honorDelta.playerTwoDelta(), prestigeThreshold);
    }

    private HonorDelta computeHonorDeltas(
        DuelSession session,
        DuelResult result,
        int pairMatchesInLast24Hours,
        int playerOneDailyHonor,
        int playerTwoDailyHonor
    ) {
        DuelSettings.HonorSettings honorSettings = configManager.getDuelSettings().honorSettings();
        if (!honorSettings.enable()) {
            return new HonorDelta(0, 0);
        }
        String playerOneIp = session.getPlayerOneIp();
        String playerTwoIp = session.getPlayerTwoIp();
        if (playerOneIp != null && playerTwoIp != null && playerOneIp.equals(playerTwoIp)) {
            return new HonorDelta(0, 0);
        }

        int playerOneDelta = 0;
        int playerTwoDelta = 0;
        switch (result) {
            case PLAYER_ONE_WIN -> {
                playerOneDelta = honorSettings.winReward();
                playerTwoDelta = switch (session.getEndReason()) {
                    case SURRENDER -> honorSettings.surrenderPenalty();
                    case QUIT -> honorSettings.disconnectPenalty();
                    default -> honorSettings.lossReward();
                };
            }
            case PLAYER_TWO_WIN -> {
                playerTwoDelta = honorSettings.winReward();
                playerOneDelta = switch (session.getEndReason()) {
                    case SURRENDER -> honorSettings.surrenderPenalty();
                    case QUIT -> honorSettings.disconnectPenalty();
                    default -> honorSettings.lossReward();
                };
            }
            case DRAW, CANCELLED -> {
                playerOneDelta = 0;
                playerTwoDelta = 0;
            }
        }

        playerOneDelta = applyPairDecay(playerOneDelta, pairMatchesInLast24Hours);
        playerTwoDelta = applyPairDecay(playerTwoDelta, pairMatchesInLast24Hours);
        playerOneDelta = applyDailyCap(playerOneDelta, playerOneDailyHonor, honorSettings.dailyCap());
        playerTwoDelta = applyDailyCap(playerTwoDelta, playerTwoDailyHonor, honorSettings.dailyCap());
        return new HonorDelta(playerOneDelta, playerTwoDelta);
    }

    private int applyPairDecay(int delta, int pairMatchesInLast24Hours) {
        if (delta <= 0) {
            return delta;
        }
        double multiplier = switch (pairMatchesInLast24Hours) {
            case 0 -> 1.0D;
            case 1 -> 0.5D;
            case 2 -> 0.25D;
            default -> 0.0D;
        };
        return (int) Math.floor(delta * multiplier);
    }

    private int applyDailyCap(int delta, int usedHonor, int dailyCap) {
        if (delta <= 0) {
            return delta;
        }
        if (dailyCap <= 0) {
            return 0;
        }
        int remaining = dailyCap - usedHonor;
        if (remaining <= 0) {
            return 0;
        }
        return Math.min(delta, remaining);
    }

    private void applyHonorWithPrestige(DuelStats stats, int honorDelta, int prestigeThreshold) {
        int updatedHonor = Math.max(0, stats.getHonor() + honorDelta);
        int prestige = stats.getPrestige();
        while (updatedHonor >= prestigeThreshold) {
            updatedHonor -= prestigeThreshold;
            prestige++;
        }
        stats.setHonor(updatedHonor);
        stats.setPrestige(prestige);
    }

    private record HonorDelta(int playerOneDelta, int playerTwoDelta) {
    }

    private String resolvePlayerIp(Player player) {
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            return null;
        }
        return player.getAddress().getAddress().getHostAddress();
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    private void clearPotionEffects(Player player) {
        List<PotionEffect> effects = new ArrayList<>(player.getActivePotionEffects());
        for (PotionEffect effect : effects) {
            player.removePotionEffect(effect.getType());
        }
    }
}

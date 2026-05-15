package org.stellarvan.stellarDuelBridge.duel;

import java.util.UUID;
import org.bukkit.scheduler.BukkitTask;
import org.stellarvan.stellarDuelBridge.snapshot.PlayerSnapshot;

public final class DuelSession {

    private final UUID sessionId;
    private final UUID playerOne;
    private final UUID playerTwo;
    private final String playerOneName;
    private final String playerTwoName;
    private final long createdAt;
    private String arenaId;
    private DuelMode mode;
    private DuelState state;
    private long startedAt;
    private long endedAt;
    private DuelMode selectedMode;
    private UUID winner;
    private UUID loser;
    private DuelEndReason endReason;
    private PlayerSnapshot playerOneSnapshot;
    private PlayerSnapshot playerTwoSnapshot;
    private String playerOneIp;
    private String playerTwoIp;
    private boolean challengerConfirmed;
    private boolean targetConfirmed;
    private BukkitTask contractTimeoutTask;
    private BukkitTask countdownTask;
    private BukkitTask timeoutTask;

    public DuelSession(UUID sessionId, UUID playerOne, UUID playerTwo, String playerOneName, String playerTwoName, DuelMode selectedMode) {
        this.sessionId = sessionId;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.playerOneName = playerOneName;
        this.playerTwoName = playerTwoName;
        this.createdAt = System.currentTimeMillis();
        this.state = DuelState.INVITED;
        this.selectedMode = selectedMode;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getPlayerOne() {
        return playerOne;
    }

    public UUID getPlayerTwo() {
        return playerTwo;
    }

    public String getPlayerOneName() {
        return playerOneName;
    }

    public String getPlayerTwoName() {
        return playerTwoName;
    }

    public String getArenaId() {
        return arenaId;
    }

    public void setArenaId(String arenaId) {
        this.arenaId = arenaId;
    }

    public DuelMode getMode() {
        return mode;
    }

    public void setMode(DuelMode mode) {
        this.mode = mode;
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public DuelMode getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(DuelMode selectedMode) {
        this.selectedMode = selectedMode;
    }

    public UUID getWinner() {
        return winner;
    }

    public void setWinner(UUID winner) {
        this.winner = winner;
    }

    public UUID getLoser() {
        return loser;
    }

    public void setLoser(UUID loser) {
        this.loser = loser;
    }

    public DuelEndReason getEndReason() {
        return endReason;
    }

    public void setEndReason(DuelEndReason endReason) {
        this.endReason = endReason;
    }

    public PlayerSnapshot getPlayerOneSnapshot() {
        return playerOneSnapshot;
    }

    public void setPlayerOneSnapshot(PlayerSnapshot playerOneSnapshot) {
        this.playerOneSnapshot = playerOneSnapshot;
    }

    public PlayerSnapshot getPlayerTwoSnapshot() {
        return playerTwoSnapshot;
    }

    public void setPlayerTwoSnapshot(PlayerSnapshot playerTwoSnapshot) {
        this.playerTwoSnapshot = playerTwoSnapshot;
    }

    public String getPlayerOneIp() {
        return playerOneIp;
    }

    public void setPlayerOneIp(String playerOneIp) {
        this.playerOneIp = playerOneIp;
    }

    public String getPlayerTwoIp() {
        return playerTwoIp;
    }

    public void setPlayerTwoIp(String playerTwoIp) {
        this.playerTwoIp = playerTwoIp;
    }

    public boolean isChallengerConfirmed() {
        return challengerConfirmed;
    }

    public void setChallengerConfirmed(boolean challengerConfirmed) {
        this.challengerConfirmed = challengerConfirmed;
    }

    public boolean isTargetConfirmed() {
        return targetConfirmed;
    }

    public void setTargetConfirmed(boolean targetConfirmed) {
        this.targetConfirmed = targetConfirmed;
    }

    public boolean areBothConfirmed() {
        return challengerConfirmed && targetConfirmed;
    }

    public boolean isPlayerConfirmed(UUID playerId) {
        if (playerOne.equals(playerId)) {
            return challengerConfirmed;
        }
        if (playerTwo.equals(playerId)) {
            return targetConfirmed;
        }
        return false;
    }

    public void setPlayerConfirmed(UUID playerId, boolean confirmed) {
        if (playerOne.equals(playerId)) {
            challengerConfirmed = confirmed;
        } else if (playerTwo.equals(playerId)) {
            targetConfirmed = confirmed;
        }
    }

    public BukkitTask getContractTimeoutTask() {
        return contractTimeoutTask;
    }

    public void setContractTimeoutTask(BukkitTask contractTimeoutTask) {
        this.contractTimeoutTask = contractTimeoutTask;
    }

    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }

    public BukkitTask getTimeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(BukkitTask timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

    public boolean isParticipant(UUID playerId) {
        return playerOne.equals(playerId) || playerTwo.equals(playerId);
    }

    public UUID getOpponent(UUID playerId) {
        if (playerOne.equals(playerId)) {
            return playerTwo;
        }
        if (playerTwo.equals(playerId)) {
            return playerOne;
        }
        return null;
    }

    public String getOpponentName(UUID playerId) {
        if (playerOne.equals(playerId)) {
            return playerTwoName;
        }
        if (playerTwo.equals(playerId)) {
            return playerOneName;
        }
        return null;
    }

    public void cancelTasks() {
        if (contractTimeoutTask != null) {
            contractTimeoutTask.cancel();
            contractTimeoutTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }
}

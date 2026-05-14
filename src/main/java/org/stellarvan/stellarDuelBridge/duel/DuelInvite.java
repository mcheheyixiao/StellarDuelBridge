package org.stellarvan.stellarDuelBridge.duel;

import java.util.UUID;

public final class DuelInvite {

    private final UUID inviteId;
    private final UUID challenger;
    private final UUID target;
    private final String challengerName;
    private final String targetName;
    private final long createdAt;
    private final long expireAt;

    public DuelInvite(UUID inviteId, UUID challenger, UUID target, String challengerName, String targetName, long createdAt, long expireAt) {
        this.inviteId = inviteId;
        this.challenger = challenger;
        this.target = target;
        this.challengerName = challengerName;
        this.targetName = targetName;
        this.createdAt = createdAt;
        this.expireAt = expireAt;
    }

    public UUID getInviteId() {
        return inviteId;
    }

    public UUID getChallenger() {
        return challenger;
    }

    public UUID getTarget() {
        return target;
    }

    public String getChallengerName() {
        return challengerName;
    }

    public String getTargetName() {
        return targetName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long now) {
        return now >= expireAt;
    }
}

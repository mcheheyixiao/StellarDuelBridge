package org.stellarvan.stellarDuelBridge.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ArenaSelector {

    public Arena selectRandom(List<Arena> candidates, String lastArenaId, boolean avoidRepeat) {
        if (candidates.isEmpty()) {
            return null;
        }
        List<Arena> working = new ArrayList<>(candidates);
        if (avoidRepeat && lastArenaId != null && working.size() > 1) {
            working.removeIf(arena -> arena.getId().equalsIgnoreCase(lastArenaId));
            if (working.isEmpty()) {
                working = new ArrayList<>(candidates);
            }
        }
        return working.get(ThreadLocalRandom.current().nextInt(working.size()));
    }
}

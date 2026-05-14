package org.stellarvan.stellarDuelBridge.listener;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.stellarvan.stellarDuelBridge.duel.DuelSessionManager;

public final class CombatListener implements Listener {

    private final DuelSessionManager duelSessionManager;

    public CombatListener(DuelSessionManager duelSessionManager) {
        this.duelSessionManager = duelSessionManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        boolean attackerInDuel = duelSessionManager.isInDuel(attacker.getUniqueId());
        boolean victimInDuel = duelSessionManager.isInDuel(victim.getUniqueId());
        if (!attackerInDuel && !victimInDuel) {
            duelSessionManager.recordExternalCombat(attacker, victim);
            return;
        }
        if (!duelSessionManager.isDamageAllowed(attacker, victim, event.getDamager())) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (entity instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            return player;
        }
        if (entity instanceof Trident trident && trident.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}

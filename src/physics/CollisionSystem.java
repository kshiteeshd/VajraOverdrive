package physics;

import boss.BossEntity;
import enemy.EnemyEntity;
import entity.*;
import gfx.FxLayer;
import player.PlayerShip;
import score.ScoreManager;

import java.util.ArrayList;
import java.util.List;

public class CollisionSystem {

    public static void checkCollisions(List<Entity> entities) {

        List<Entity> snapshot = new ArrayList<>(entities);

        PlayerShip player = null;
        for (Entity e : snapshot)
            if (e instanceof PlayerShip p) { player = p; break; }

        // ── Projectile collisions ─────────────────────────────────────
        for (Entity e : snapshot) {
            if (!(e instanceof ProjectileEntity p)) continue;

            for (Entity target : snapshot) {

                // Player bullet → enemy
                if (p.isFromPlayer() && target instanceof EnemyEntity enemy) {
                    if (!p.removable && p.getBounds().intersects(enemy.getBounds())) {
                        enemy.takeDamage(p.getDamage());
                        ScoreManager.get().enemyHit();
                        if (enemy.isRemovable())
                            ScoreManager.get().enemyKilledWithValue(
                                    enemy.getScoreValue());
                        p.removable = true;
                        break;
                    }
                }

                // Enemy bullet → player
                if (!p.isFromPlayer() && target instanceof PlayerShip pl) {
                    if (!p.removable && p.getBounds().intersects(pl.getBounds())) {
                        pl.takeDamage(p.getDamage());
                        ScoreManager.get().playerDamaged();
                        p.removable = true;
                        break;
                    }
                }
            }
        }

        // ── Enemy body → player ───────────────────────────────────────
        if (player != null) {
            for (Entity e : snapshot) {
                if (e instanceof EnemyEntity enemy && !enemy.removable) {
                    if (enemy.getBounds().intersects(player.getBounds())) {
                        player.takeDamage(10);
                        ScoreManager.get().playerDamaged();
                        ScoreManager.get().enemyKilledWithValue(
                                enemy.getScoreValue());
                        enemy.removable = true;
                    }
                }
            }
        }
    }

    /**
     * Checks player bullet collisions against the active boss.
     * Boss is not in EntityManager's entity list so needs separate handling.
     */
    public static void checkBossCollisions(BossEntity boss,
                                           List<Entity> entities) {
        if (boss == null || boss.isRemovable()) return;

        for (Entity e : new ArrayList<>(entities)) {
            if (!(e instanceof ProjectileEntity p)) continue;
            if (!p.isFromPlayer()) continue;
            if (p.removable) continue;

            if (p.getBounds().intersects(boss.getBounds())) {
                boss.takeDamage(p.getDamage());
                ScoreManager.get().enemyHit();
                p.removable = true;
            }
        }
    }
}
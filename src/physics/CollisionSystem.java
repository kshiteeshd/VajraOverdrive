package physics;

import boss.BossEntity;
import enemy.EnemyEntity;
import entity.*;
import player.PlayerShip;
import score.ScoreManager;
import ui.hud.NebulaHUD;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all combat collisions.
 *
 * ── Changes in this version ───────────────────────────────────────────
 *
 * PIERCING BULLETS:
 *   Player bullets with modifier == PIERCING are no longer marked
 *   removable on first hit. They pass through enemies and continue
 *   until they exit the combat area.
 *
 *   The check is a single modifier read per collision — zero overhead
 *   for normal bullets (they take the same code path as before,
 *   just with a modifier == NORMAL condition that resolves immediately).
 *
 *   Piercing bullets CAN still be consumed by boss hits — bosses are
 *   a single high-value target and the player is committing a shot
 *   to it deliberately. This keeps boss fights honest.
 *
 * STRUCTURE:
 *   Unchanged from Batch 3 version. Pre-allocated lists are reused
 *   each frame. Boss collision is a separate method called from
 *   GameCanvas.
 *
 * ── How piercing works end-to-end ────────────────────────────────────
 *   1. WeaponSystem.firePiercing() calls pool(), then p.setModifier(PIERCING).
 *   2. This method checks getModifier() != NORMAL before setRemovable(true).
 *   3. Piercing bullet keeps flying, can hit multiple enemies in one pass.
 *   4. Bullet is still removed when it exits the combat area bounds
 *      via ProjectileEntity.update()'s out-of-bounds check.
 */
public class CollisionSystem {

    // Pre-allocated lists — reused every frame to avoid GC pressure
    private static final List<ProjectileEntity> playerBullets = new ArrayList<>(100);
    private static final List<ProjectileEntity> enemyBullets  = new ArrayList<>(100);
    private static final List<EnemyEntity>      enemies       = new ArrayList<>(50);

    // ── Main collision check ──────────────────────────────────────────
    public static void checkCollisions(List<Entity> entities, NebulaHUD hud) {
        playerBullets.clear();
        enemyBullets.clear();
        enemies.clear();
        PlayerShip player = null;

        // ── Partition entities by type ────────────────────────────────
        for (Entity e : entities) {
            if (e instanceof PlayerShip ps) {
                player = ps;
            } else if (e instanceof ProjectileEntity p) {
                if (p.isFromPlayer()) playerBullets.add(p);
                else                  enemyBullets.add(p);
            } else if (e instanceof EnemyEntity en) {
                enemies.add(en);
            }
        }

        // ── Player bullets → enemies ──────────────────────────────────
        for (int i = 0; i < playerBullets.size(); i++) {
            ProjectileEntity bullet = playerBullets.get(i);
            // Normal bullets skip once removable. Piercing bullets never
            // become removable from hits, so they keep checking.
            if (bullet.isRemovable()) continue;

            boolean isPiercing = bullet.getModifier()
                    == ProjectileEntity.ProjectileModifier.PIERCING;

            for (int j = 0; j < enemies.size(); j++) {
                EnemyEntity enemy = enemies.get(j);
                if (enemy.isRemovable()) continue;

                if (bullet.getBounds().intersects(enemy.getBounds())) {
                    enemy.takeDamage(bullet.getDamage());
                    ScoreManager.get().enemyHit();

                    if (enemy.isRemovable()) {
                        float killX = (float)(enemy.x + enemy.width  / 2.0);
                        float killY = (float)(enemy.y + enemy.height / 2.0);
                        ScoreManager.get().setLastKillPos(killX, killY);
                        ScoreManager.get().enemyKilledWithValue(enemy.getScoreValue());
                    }

                    // KEY CHANGE: piercing bullets are not consumed on hit.
                    // Normal bullets are tagged removable and break out
                    // (no point checking more enemies for a spent bullet).
                    if (!isPiercing) {
                        bullet.setRemovable(true);
                        break;
                    }
                    // Piercing: no break — continue checking remaining enemies
                }
            }
        }

        // ── Enemy bullets → player ────────────────────────────────────
        if (player != null) {
            for (int i = 0; i < enemyBullets.size(); i++) {
                ProjectileEntity bullet = enemyBullets.get(i);
                if (bullet.isRemovable()) continue;

                if (bullet.getBounds().intersects(player.getBounds())) {
                    player.takeDamage(bullet.getDamage());
                    ScoreManager.get().playerDamaged();
                    bullet.setRemovable(true);
                }
            }
        }

        // ── Enemy body → player ───────────────────────────────────────
        // Contact damage — enemy is removed on touch (they don't bounce)
        if (player != null) {
            for (int i = 0; i < enemies.size(); i++) {
                EnemyEntity enemy = enemies.get(i);
                if (enemy.isRemovable()) continue;

                if (enemy.getBounds().intersects(player.getBounds())) {
                    player.takeDamage(10);
                    ScoreManager.get().playerDamaged();

                    float killX = (float)(enemy.x + enemy.width  / 2.0);
                    float killY = (float)(enemy.y + enemy.height / 2.0);
                    ScoreManager.get().setLastKillPos(killX, killY);
                    ScoreManager.get().enemyKilledWithValue(enemy.getScoreValue());

                    enemy.setRemovable(true);
                }
            }
        }
    }

    // ── Overload — NebulaHUD optional ────────────────────────────────
    public static void checkCollisions(List<Entity> entities) {
        checkCollisions(entities, null);
    }

    // ── Boss collision check — called separately from GameCanvas ──────
    /**
     * Player bullets hitting the active boss.
     *
     * Piercing bullets ARE consumed by the boss — the boss is a single
     * deliberate target. This is intentional design: piercing is good
     * against dense formations, not a free pass against bosses.
     */
    public static void checkBossCollisions(BossEntity boss, List<Entity> entities) {
        if (boss == null || boss.isRemovable()) return;

        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (!(e instanceof ProjectileEntity p)) continue;
            if (!p.isFromPlayer() || p.isRemovable()) continue;

            if (p.getBounds().intersects(boss.getBounds())) {
                boss.takeDamage(p.getDamage());
                ScoreManager.get().enemyHit();
                // Always consume on boss hit — piercing included
                p.setRemovable(true);
            }
        }
    }
}
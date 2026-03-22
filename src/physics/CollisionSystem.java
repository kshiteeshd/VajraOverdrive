package physics;

import boss.BossEntity;
import enemy.EnemyEntity;
import entity.*;
import gfx.FxLayer;
import player.PlayerShip;
import score.ScoreManager;
import ui.hud.NebulaHUD;

import java.util.ArrayList;
import java.util.List;

public class CollisionSystem {

    /**
     * Main collision check — partitions entities into typed lists first
     * so we only cross-check bullets against enemies (not every entity
     * against every other entity).
     *
     * Previously O(n²) over all entities.
     * Now O(bullets × enemies + bullets × 1) which at typical counts
     * (15 enemies, 8 bullets) goes from ~529 checks to ~120.
     */
    public static void checkCollisions(List<Entity> entities,
                                       NebulaHUD hud) {
        // ── Partition ─────────────────────────────────────────────────
        List<ProjectileEntity> playerBullets = new ArrayList<>();
        List<ProjectileEntity> enemyBullets  = new ArrayList<>();
        List<EnemyEntity>      enemies       = new ArrayList<>();
        PlayerShip             player        = null;

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
        for (ProjectileEntity bullet : playerBullets) {
            if (bullet.removable) continue;
            for (EnemyEntity enemy : enemies) {
                if (enemy.removable) continue;
                if (bullet.getBounds().intersects(enemy.getBounds())) {
                    enemy.takeDamage(bullet.getDamage());
                    ScoreManager.get().enemyHit();

                    if (enemy.isRemovable()) {
                        // Feed kill world-position to ScoreManager so
                        // the floater appears at the right place
                        float killX = (float)(enemy.x + enemy.width  / 2.0);
                        float killY = (float)(enemy.y + enemy.height / 2.0);
                        ScoreManager.get().setLastKillPos(killX, killY);
                        ScoreManager.get().enemyKilledWithValue(
                                enemy.getScoreValue());
                    }

                    bullet.removable = true;
                    break;
                }
            }
        }

        // ── Enemy bullets → player ────────────────────────────────────
        if (player != null) {
            for (ProjectileEntity bullet : enemyBullets) {
                if (bullet.removable) continue;
                if (bullet.getBounds().intersects(player.getBounds())) {
                    player.takeDamage(bullet.getDamage());
                    ScoreManager.get().playerDamaged();
                    bullet.removable = true;
                }
            }
        }

        // ── Enemy body → player ───────────────────────────────────────
        if (player != null) {
            for (EnemyEntity enemy : enemies) {
                if (enemy.removable) continue;
                if (enemy.getBounds().intersects(player.getBounds())) {
                    player.takeDamage(10);
                    ScoreManager.get().playerDamaged();

                    float killX = (float)(enemy.x + enemy.width  / 2.0);
                    float killY = (float)(enemy.y + enemy.height / 2.0);
                    ScoreManager.get().setLastKillPos(killX, killY);
                    ScoreManager.get().enemyKilledWithValue(
                            enemy.getScoreValue());

                    enemy.removable = true;
                }
            }
        }
    }

    /**
     * Backward-compat overload — used during LEVEL_TRANSITION where
     * we don't have a HUD reference handy. Floaters won't spawn but
     * nothing breaks.
     */
    public static void checkCollisions(List<Entity> entities) {
        checkCollisions(entities, null);
    }

    /**
     * Boss collision — player bullets vs boss.
     * Boss is not in EntityManager so this is called separately
     * from WaveManager.update().
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
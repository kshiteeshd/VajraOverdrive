package physics;

import boss.BossEntity;
import enemy.EnemyEntity;
import entity.*;
import player.PlayerShip;
import score.ScoreManager;
import ui.hud.NebulaHUD;

import java.util.ArrayList;
import java.util.List;

public class CollisionSystem {

    // PRE-ALLOCATED LISTS: Eliminates GC stuttering by reusing memory
    private static final List<ProjectileEntity> playerBullets = new ArrayList<>(100);
    private static final List<ProjectileEntity> enemyBullets  = new ArrayList<>(100);
    private static final List<EnemyEntity>      enemies       = new ArrayList<>(50);

    public static void checkCollisions(List<Entity> entities, NebulaHUD hud) {
        // Clear lists instead of reallocating them
        playerBullets.clear();
        enemyBullets.clear();
        enemies.clear();
        PlayerShip player = null;

        // ── Partition ─────────────────────────────────────────────────
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
            if (bullet.isRemovable()) continue;

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

                    bullet.setRemovable(true); // Tag for pooling/removal
                    break;
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

    public static void checkCollisions(List<Entity> entities) {
        checkCollisions(entities, null);
    }

    public static void checkBossCollisions(BossEntity boss, List<Entity> entities) {
        if (boss == null || boss.isRemovable()) return;

        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if (!(e instanceof ProjectileEntity p)) continue;
            if (!p.isFromPlayer() || p.isRemovable()) continue;

            if (p.getBounds().intersects(boss.getBounds())) {
                boss.takeDamage(p.getDamage());
                ScoreManager.get().enemyHit();
                p.setRemovable(true);
            }
        }
    }
}
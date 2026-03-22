// ─────────────────────────────────────────────────────────────────────
// boss/types/ApexBoss.java  — WHITE region, level 25
// ─────────────────────────────────────────────────────────────────────
package boss.types;

import boss.BossEntity;
import boss.BossStats;
import enemy.data.EnemyRegistry;
import enemy.data.EnemyType;
import entity.EntityManager;
import gfx.FxLayer;
import gfx.ScreenShake;
import player.PlayerShip;

import java.awt.*;

/**
 * WHITE region boss — final boss.
 * Phase 0: spread shots.
 * Phase 1: aimed + spread. Spawns BasicEnemy_T2 minions.
 * Phase 2: full spread + aimed + minions. Faster movement.
 * Phase 3: berserk — rapid all patterns. Spawns T2 fast enemies.
 */
public class ApexBoss extends BossEntity {

    private static final BossStats STATS = new BossStats(
            "APEX",
            1000,
            88, 68,
            25000,
            new Color(220, 220, 255),
            new float[]{ 0.75f, 0.50f, 0.25f },   // 4 phases
            2.5,
            400,
            1000,
            7
    );

    private long lastMinionSpawn  = 0;
    private static final long MINION_INTERVAL_P1 = 6000;
    private static final long MINION_INTERVAL_P3 = 3000;

    // Used to cycle through multiple attack patterns
    private int attackCycle = 0;

    public ApexBoss(double x, double y,
                    EntityManager em, PlayerShip player) {
        super(x, y, STATS, em, player);
    }

    @Override
    protected void updatePhase(int currentPhase) {
        if (currentPhase < 1) return;
        long now      = System.currentTimeMillis();
        long interval = (currentPhase >= 3)
                ? MINION_INTERVAL_P3
                : MINION_INTERVAL_P1;
        if (now - lastMinionSpawn > interval) {
            spawnMinions(currentPhase);
            lastMinionSpawn = now;
        }
    }

    private void spawnMinions(int currentPhase) {
        String typeName = (currentPhase >= 3)
                ? "FastEnemy_T2" : "BasicEnemy_T2";
        EnemyType type = EnemyRegistry.getByName(typeName);
        if (type == null) return;
        entityManager.add(type.create(
                config.CombatArea.LEFT_BOUND + 20, -30, entityManager));
        entityManager.add(type.create(
                config.CombatArea.RIGHT_BOUND - 40, -30, entityManager));
        if (currentPhase >= 3) {
            // Extra center minion in berserk phase
            entityManager.add(type.create(
                    x + width / 2.0, -30, entityManager));
        }
    }

    @Override
    protected void firePattern(int currentPhase) {
        attackCycle++;
        switch (currentPhase) {
            case 0 -> fireSpread(5, 14, stats.bulletDamage, 4.5);
            case 1 -> {
                if (attackCycle % 2 == 0)
                    fireSpread(5, 14, stats.bulletDamage, 4.5);
                else
                    fireAimed(stats.bulletDamage + 2, 5.5);
            }
            case 2 -> {
                fireSpread(7, 10, stats.bulletDamage, 5.0);
                if (attackCycle % 3 == 0)
                    fireAimed(stats.bulletDamage + 2, 6.0);
            }
            case 3 -> {
                // Berserk — everything at once
                fireSpread(9, 8, stats.bulletDamage, 5.5);
                fireAimed(stats.bulletDamage + 3, 6.5);
                fireStraight(stats.bulletDamage + 2, 12, 12, 4.0);
            }
        }
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().deathBurst(cx, cy, stats.color);
        FxLayer.get().particles(cx, cy, 30, Color.WHITE, 6.0f, 900);
        if (newPhase >= 3) ScreenShake.large();
    }

    @Override
    protected void renderBody(Graphics2D g) {
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        // Pulse brightness based on phase
        Color bodyColor = switch (phase) {
            case 0  -> new Color(160, 160, 200);
            case 1  -> new Color(180, 180, 220);
            case 2  -> new Color(200, 200, 240);
            default -> new Color(230, 230, 255);
        };

        // Outer wing span — wide angular shape
        int[] wx = {
                cx,
                (int)x,                (int)x + 16,
                cx - 10,               cx + 10,
                (int)x+width - 16,     (int)x+width,
                cx
        };
        int[] wy = {
                (int)y + height,
                (int)y + 24,           (int)y + 24,
                (int)y,                (int)y,
                (int)y + 24,           (int)y + 24,
                (int)y + height
        };
        g.setColor(bodyColor);
        g.fillPolygon(wx, wy, 8);

        // Inner hull
        g.setColor(bodyColor.brighter());
        int[] hx = { cx, cx - 20, cx - 16, cx, cx + 16, cx + 20 };
        int[] hy = { (int)y+height-4, cy+8, (int)y+4,
                (int)y,         (int)y+4, cy+8 };
        g.fillPolygon(hx, hy, 6);

        // Phase 3 berserk glow ring
        if (phase >= 3) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.25f));
            g.setColor(Color.WHITE);
            g.drawOval((int)x - 10, (int)y - 10, width + 20, height + 20);
            g.setComposite(old);
        }

        // Multi-cannon array
        g.setColor(new Color(80, 80, 120));
        int[] cannonX = { cx - 24, cx - 8, cx + 8, cx + 18 };
        for (int bx : cannonX) {
            g.fillRect(bx, (int)y + height - 2, 6, 8);
        }

        // Core — bright white
        g.setColor(Color.WHITE);
        g.fillOval(cx - 8, cy - 6, 16, 12);
        g.setColor(new Color(180, 180, 255));
        g.fillOval(cx - 5, cy - 3, 10, 8);

        g.setColor(new Color(80, 80, 140));
        g.drawPolygon(wx, wy, 8);
    }
}
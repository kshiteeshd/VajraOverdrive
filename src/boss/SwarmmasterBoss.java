// ─────────────────────────────────────────────────────────────────────
// boss/types/SwarmmasterBoss.java  — YELLOW region, level 10
// ─────────────────────────────────────────────────────────────────────
package boss.types;

import boss.BossEntity;
import boss.BossStats;
import enemy.data.EnemyRegistry;
import enemy.data.EnemyType;
import entity.EntityManager;
import entity.ProjectileEntity;
import gfx.FxLayer;
import player.PlayerShip;

import java.awt.*;

/**
 * YELLOW region boss.
 * Phase 0: fires 3-shot spread + rapid aimed shots.
 * Phase 1: faster movement, 5-shot spread, spawns FastEnemy minions
 *          periodically from sides.
 */
public class SwarmmasterBoss extends BossEntity {

    private static final BossStats STATS = new BossStats(
            "SWARMMASTER",
            550,
            72, 52,
            8000,
            new Color(255, 210, 60),
            new float[]{ 0.50f },
            2.2,
            380,
            1200,
            3
    );

    private long lastMinionSpawn = 0;
    private static final long MINION_INTERVAL = 5000; // ms between minion spawns

    public SwarmmasterBoss(double x, double y,
                           EntityManager em, PlayerShip player) {
        super(x, y, STATS, em, player);
    }

    @Override
    protected void updatePhase(int currentPhase) {
        // Phase 1 — periodically spawn fast enemy minions
        if (currentPhase < 1) return;
        long now = System.currentTimeMillis();
        if (now - lastMinionSpawn > MINION_INTERVAL) {
            spawnMinions();
            lastMinionSpawn = now;
        }
    }

    private void spawnMinions() {
        EnemyType fastType = EnemyRegistry.getByName("FastEnemy");
        if (fastType == null) return;
        // Spawn one from each side
        entityManager.add(fastType.create(
                config.CombatArea.LEFT_BOUND + 10, -20, entityManager));
        entityManager.add(fastType.create(
                config.CombatArea.RIGHT_BOUND - 30, -20, entityManager));
    }

    @Override
    protected void firePattern(int currentPhase) {
        if (currentPhase == 0) {
            fireSpread(3, 16, stats.bulletDamage, 4.0);
        } else {
            fireSpread(5, 12, stats.bulletDamage, 4.5);
            fireAimed(stats.bulletDamage + 1, 5.0);
        }
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().particles(cx, cy, 20, stats.color, 5.5f, 700);
    }

    @Override
    protected void renderBody(Graphics2D g) {
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        Color bodyColor = (phase >= 1)
                ? new Color(255, 200, 20)
                : new Color(200, 160, 20);

        // Hexagonal body — command ship
        int r = Math.min(width, height) / 2 - 2;
        int[] hx = new int[6];
        int[] hy = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            hx[i] = (int)(cx + r * Math.cos(angle));
            hy[i] = (int)(cy + r * Math.sin(angle));
        }
        g.setColor(bodyColor);
        g.fillPolygon(hx, hy, 6);

        // Inner plate
        int r2 = r - 8;
        int[] ix = new int[6];
        int[] iy = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            ix[i] = (int)(cx + r2 * Math.cos(angle));
            iy[i] = (int)(cy + r2 * Math.sin(angle));
        }
        g.setColor(new Color(255, 240, 100));
        g.fillPolygon(ix, iy, 6);

        // Rotating indicator dots (visual only — no real rotation state needed)
        g.setColor(new Color(100, 60, 0));
        g.fillOval(cx - 5, cy - 5, 10, 10);
        g.setColor(new Color(255, 255, 180));
        g.fillOval(cx - 3, cy - 3, 6, 6);

        // Wing struts
        g.setColor(new Color(160, 120, 0));
        g.fillRect((int)x, cy - 3, 10, 6);
        g.fillRect((int)x + width - 10, cy - 3, 10, 6);

        g.setColor(new Color(80, 50, 0));
        g.drawPolygon(hx, hy, 6);
    }
}
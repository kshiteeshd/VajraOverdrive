// ─────────────────────────────────────────────────────────────────────
// boss/types/IroncladBoss.java  — BLUE region, level 15
// ─────────────────────────────────────────────────────────────────────
package boss.types;

import boss.BossEntity;
import boss.BossStats;
import entity.EntityManager;
import gfx.FxLayer;
import player.PlayerShip;

import java.awt.*;

/**
 * BLUE region boss.
 * Phase 0: shielded — only takes 50% damage. Fires slow heavy shots.
 * Phase 1: shield breaks, becomes faster. Adds aimed shots.
 * Phase 2: berserk — fires full spread every shot.
 */
public class IroncladBoss extends BossEntity {

    private static final BossStats STATS = new BossStats(
            "IRONCLAD",
            700,
            80, 60,
            12000,
            new Color(80, 160, 255),
            new float[]{ 0.66f, 0.33f },   // 3 phases
            1.5,
            340,
            1600,
            6
    );

    private boolean shieldActive = true;

    public IroncladBoss(double x, double y,
                        EntityManager em, PlayerShip player) {
        super(x, y, STATS, em, player);
    }

    @Override
    public void takeDamage(int dmg) {
        // Phase 0 shield — absorbs 50% of incoming damage
        if (shieldActive) dmg = Math.max(1, dmg / 2);
        super.takeDamage(dmg);
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        if (newPhase == 1) {
            shieldActive = false;    // shield breaks at phase 1
            FxLayer.get().deathBurst(cx, cy, new Color(120, 200, 255));
        } else if (newPhase == 2) {
            FxLayer.get().particles(cx, cy, 30, stats.color, 6.0f, 800);
        }
    }

    @Override
    protected void firePattern(int currentPhase) {
        switch (currentPhase) {
            case 0 -> fireStraight(stats.bulletDamage, 14, 14, 3.0);
            case 1 -> {
                fireStraight(stats.bulletDamage, 12, 12, 3.5);
                fireAimed(stats.bulletDamage, 5.0);
            }
            case 2 -> {
                // Berserk — full 7-shot spread
                fireSpread(7, 10, stats.bulletDamage, 5.0);
            }
        }
    }

    @Override
    protected void renderBody(Graphics2D g) {
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        // Phase colors
        Color bodyColor = switch (phase) {
            case 0  -> new Color(60,  120, 220);
            case 1  -> new Color(80,  160, 255);
            default -> new Color(140, 200, 255);
        };

        // Heavy rectangular hull
        g.setColor(bodyColor);
        g.fillRect((int)x + 6, (int)y, width - 12, height);

        // Side armor plates
        g.setColor(bodyColor.darker());
        g.fillRect((int)x,           (int)y + 10, 12, height - 20);
        g.fillRect((int)x+width-12,  (int)y + 10, 12, height - 20);

        // Phase 0 shield — blue outer ring
        if (shieldActive) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.35f));
            g.setColor(new Color(100, 180, 255));
            g.drawOval((int)x - 8, (int)y - 8, width + 16, height + 16);
            g.drawOval((int)x - 5, (int)y - 5, width + 10, height + 10);
            g.setComposite(old);
        }

        // Cannon barrels (bottom center)
        g.setColor(new Color(40, 80, 140));
        g.fillRect(cx - 14, (int)y + height - 4, 8, 10);
        g.fillRect(cx + 6,  (int)y + height - 4, 8, 10);

        // Core
        Color coreCol = (phase >= 2)
                ? new Color(200, 240, 255)
                : new Color(120, 180, 255);
        g.setColor(coreCol);
        g.fillOval(cx - 10, cy - 8, 20, 16);

        // Outline
        g.setColor(new Color(20, 60, 120));
        g.drawRect((int)x + 6, (int)y, width - 12, height);
    }
}
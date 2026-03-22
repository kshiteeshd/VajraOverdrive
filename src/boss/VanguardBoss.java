// ─────────────────────────────────────────────────────────────────────
// boss/types/VanguardBoss.java   — RED region, level 5
// ─────────────────────────────────────────────────────────────────────
package boss.types;

import boss.BossEntity;
import boss.BossStats;
import entity.EntityManager;
import gfx.FxLayer;
import gfx.ScreenShake;
import player.PlayerShip;

import java.awt.*;

/**
 * RED region boss.
 * Phase 0: sweeps horizontally, fires 3-shot spread.
 * Phase 1: fires 5-shot spread + aimed center shot simultaneously.
 */
public class VanguardBoss extends BossEntity {

    private static final BossStats STATS = new BossStats(
            "VANGUARD",
            400,                          // HP
            64, 48,                       // w, h
            5000,                         // score
            new Color(255, 70, 70),       // RED
            new float[]{ 0.50f },         // 1 phase threshold → 2 phases
            1.8,                          // sweep speed
            360,                          // sweep amplitude
            1400,                         // fire rate ms phase 0
            4                             // bullet damage
    );

    public VanguardBoss(double x, double y,
                        EntityManager em, PlayerShip player) {
        super(x, y, STATS, em, player);
    }

    @Override
    protected void firePattern(int currentPhase) {
        if (currentPhase == 0) {
            fireSpread(3, 18, stats.bulletDamage, 4.5);
        } else {
            // Phase 1 — wider spread + aimed shot
            fireSpread(5, 14, stats.bulletDamage, 4.5);
            fireAimed(stats.bulletDamage + 2, 5.5);
        }
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        // Charge flash — burst of red particles on phase break
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().particles(cx, cy, 24, stats.color, 5.0f, 600);
    }

    @Override
    protected void renderBody(Graphics2D g) {
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        // Phase tint — brighter red in phase 1
        Color bodyColor = (phase >= 1)
                ? new Color(255, 40,  40)
                : new Color(200, 30, 30);

        // Main body — heavy arrowhead pointing down
        int[] bx = { cx,
                (int)x,          (int)x + 12,
                (int)x + 12,     cx,
                (int)x+width-12, (int)x+width-12,
                (int)x+width };
        int[] by = { (int)y + height,
                (int)y + height/2, (int)y + height/2,
                (int)y,            (int)y,
                (int)y,            (int)y + height/2,
                (int)y + height/2 };
        g.setColor(bodyColor);
        g.fillPolygon(bx, by, 8);

        // Cockpit dome
        g.setColor(new Color(255, 120, 120));
        g.fillOval(cx - 14, (int)y + 4, 28, 18);

        // Engine glow lines
        g.setColor(new Color(255, 200, 200, 180));
        g.fillRect((int)x + 8,         (int)y + height - 8, 12, 4);
        g.fillRect((int)x + width - 20, (int)y + height - 8, 12, 4);

        // Core — bright when in phase 1
        Color coreColor = (phase >= 1)
                ? new Color(255, 255, 200)
                : new Color(255, 180, 180);
        g.setColor(coreColor);
        g.fillOval(cx - 6, cy - 4, 12, 10);

        // Outline
        g.setColor(new Color(140, 10, 10));
        g.drawPolygon(bx, by, 8);
    }
}
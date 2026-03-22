// ─────────────────────────────────────────────────────────────────────
// boss/types/PhantomBoss.java  — GREEN region, level 20
// ─────────────────────────────────────────────────────────────────────
package boss.types;

import boss.BossEntity;
import boss.BossStats;
import config.CombatArea;
import entity.EntityManager;
import gfx.FxLayer;
import gfx.ScreenShake;
import player.PlayerShip;

import java.awt.*;

/**
 * GREEN region boss.
 * Phase 0: normal sweep + aimed shots.
 * Phase 1: teleports to a random X position every 4 seconds.
 * Phase 2: brief invisibility during teleport, fires triple burst.
 */
public class PhantomBoss extends BossEntity {

    private static final BossStats STATS = new BossStats(
            "PHANTOM",
            600,
            68, 54,
            15000,
            new Color(60, 220, 100),
            new float[]{ 0.60f, 0.30f },
            2.0,
            360,
            1100,
            5
    );

    private long    lastTeleport  = 0;
    private static final long TELEPORT_INTERVAL = 4000;
    private boolean invisible     = false;
    private long    invisEndTime  = 0;
    private static final long INVIS_DURATION = 800;

    public PhantomBoss(double x, double y,
                       EntityManager em, PlayerShip player) {
        super(x, y, STATS, em, player);
    }

    @Override
    protected void updatePhase(int currentPhase) {
        if (currentPhase < 1) return;

        long now = System.currentTimeMillis();

        // End invisibility
        if (invisible && now > invisEndTime) {
            invisible = false;
        }

        // Teleport tick
        if (now - lastTeleport > TELEPORT_INTERVAL) {
            teleport(currentPhase);
            lastTeleport = now;
        }
    }

    private void teleport(int currentPhase) {
        double range  = CombatArea.WIDTH - width - 40;
        double newX   = CombatArea.LEFT_BOUND + 20
                + Math.random() * range;
        x = newX;
        FxLayer.get().particles(
                x + width / 2.0, y + height / 2.0,
                16, stats.color, 4.0f, 400);
        ScreenShake.small();

        // Phase 2 — go invisible during teleport
        if (currentPhase >= 2) {
            invisible    = true;
            invisEndTime = System.currentTimeMillis() + INVIS_DURATION;
        }
    }

    @Override
    protected void firePattern(int currentPhase) {
        if (invisible) return;  // can't shoot while invisible
        switch (currentPhase) {
            case 0 -> fireAimed(stats.bulletDamage, 5.0);
            case 1 -> {
                fireAimed(stats.bulletDamage, 5.5);
                fireSpread(3, 12, stats.bulletDamage - 1, 4.5);
            }
            case 2 -> {
                // Triple aimed burst
                fireAimed(stats.bulletDamage, 6.0);
                fireAimed(stats.bulletDamage, 5.5);
                fireSpread(5, 10, stats.bulletDamage, 5.0);
            }
        }
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        FxLayer.get().particles(
                x + width / 2.0, y + height / 2.0,
                22, stats.color, 5.0f, 600);
    }

    @Override
    protected void renderBody(Graphics2D g) {
        // Invisible in phase 2 teleport window
        if (invisible) {
            // Just draw a faint outline so player isn't totally blind
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.15f));
            g.setColor(stats.color);
            g.drawRect((int)x, (int)y, width, height);
            g.setComposite(old);
            return;
        }

        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        Color bodyColor = switch (phase) {
            case 0  -> new Color(30,  160, 60);
            case 1  -> new Color(50,  200, 80);
            default -> new Color(80,  255, 120);
        };

        // Stealth-wing silhouette — thin swept wings
        int[] wx = { cx - 4, (int)x,     (int)x + 10,
                cx - 4, cx + 4,
                (int)x+width-10, (int)x+width, cx + 4 };
        int[] wy = { (int)y, cy + 10,   cy + 10,
                (int)y+height-6, (int)y+height-6,
                cy + 10, cy + 10,  (int)y };
        g.setColor(bodyColor);
        g.fillPolygon(wx, wy, 8);

        // Spine
        g.setColor(new Color(140, 255, 160));
        g.fillRect(cx - 3, (int)y, 6, height);

        // Eyes / sensors
        g.setColor(new Color(200, 255, 200));
        g.fillOval(cx - 8, (int)y + 8, 6, 6);
        g.fillOval(cx + 2,  (int)y + 8, 6, 6);

        // Phase 2 — bright edge glow
        if (phase >= 2) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.3f));
            g.setColor(new Color(100, 255, 140));
            g.drawPolygon(wx, wy, 8);
            g.setComposite(old);
        }

        g.setColor(new Color(20, 80, 40));
        g.drawPolygon(wx, wy, 8);
    }
}
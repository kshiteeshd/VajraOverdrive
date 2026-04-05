package player;

import entity.EntityManager;
import entity.ProjectileEntity;
import entity.ProjectilePool;

import java.awt.*;

/**
 * Manages all player firing logic — rate limiting, heat accumulation,
 * and overheat lockout.
 *
 * PlayerShip holds one instance and delegates all fire decisions here.
 * The heat bar is rendered as a small overlay above the ship muzzle.
 *
 * Heat model:
 *   Each shot adds HEAT_PER_SHOT. Heat decays every frame by HEAT_DECAY.
 *   At OVERHEAT_THRESHOLD (1.0) the weapon locks. It stays locked until
 *   heat falls back to COOL_THRESHOLD (0.25), giving the player time to
 *   cool off before they can resume firing.
 */
public class WeaponSystem {

    // ── Tuning ────────────────────────────────────────────────────────
    private static final long   FIRE_RATE_MS       = 160;   // ms between shots
    private static final float  HEAT_PER_SHOT      = 0.12f; // heat added per shot
    private static final float  HEAT_DECAY         = 0.008f;// heat lost per frame (~60 fps)
    private static final float  OVERHEAT_THRESHOLD = 1.0f;
    private static final float  COOL_THRESHOLD     = 0.25f; // unlock at this level
    private static final double BULLET_SPEED       = 10.0;

    // ── State ─────────────────────────────────────────────────────────
    private float   heat        = 0f;
    private boolean overheated  = false;
    private long    lastFireTime = 0;

    // ── Per-frame update — decay heat, clear lockout ──────────────────
    public void update() {
        heat = Math.max(0f, heat - HEAT_DECAY);
        if (overheated && heat <= COOL_THRESHOLD) {
            overheated = false;
        }
    }

    // ── Gate — PlayerShip calls this before firing ────────────────────
    public boolean canFire(long now) {
        return !overheated && (now - lastFireTime >= FIRE_RATE_MS);
    }

    // ── Shoot — spawns one bullet, accumulates heat ───────────────────
    public void fire(double shipX, double shipY, int shipWidth, EntityManager em) {
        double cx      = shipX + shipWidth / 2.0 - 2;
        double muzzleY = shipY + 2;

        ProjectileEntity bullet = ProjectilePool.get(
                cx, muzzleY,
                0, -BULLET_SPEED,
                5, 12, 5, true,
                ProjectileEntity.BulletType.PLAYER
        );
        em.add(bullet);

        heat = Math.min(OVERHEAT_THRESHOLD, heat + HEAT_PER_SHOT);
        if (heat >= OVERHEAT_THRESHOLD) {
            overheated = true;
        }
        lastFireTime = System.currentTimeMillis();
    }

    // ── Accessors for HUD / PlayerShip ───────────────────────────────
    public float   getHeat()       { return heat;       }
    public boolean isOverheated()  { return overheated; }

    // ── Heat bar overlay ──────────────────────────────────────────────
    // Drawn above the muzzle; hidden when heat < 5%.
    public void renderOverlay(Graphics2D g, double shipX, double shipY,
                              int shipWidth, int shipHeight) {
        if (heat < 0.05f) return;

        int barW = 24;
        int barH = 4;
        int barX = (int)(shipX + shipWidth / 2.0 - barW / 2.0);
        int barY = (int)(shipY - 10);

        // Background
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
        g.setColor(new Color(30, 30, 30));
        g.fillRect(barX, barY, barW, barH);

        // Fill — cyan → orange → red based on heat
        Color fillColor = heatColor(heat);
        int fillW = (int)(barW * heat);
        if (fillW > 0) {
            g.setColor(fillColor);
            g.fillRect(barX, barY, fillW, barH);
        }

        g.setComposite(old);

        // Overheat label
        if (overheated) {
            g.setFont(new Font("Courier New", Font.BOLD, 7));
            FontMetrics fm = g.getFontMetrics();
            String label = "OVERHEAT";
            int lx = (int)(shipX + shipWidth / 2.0 - fm.stringWidth(label) / 2.0);
            g.setColor(new Color(255, 40, 40));
            g.drawString(label, lx, barY - 2);
        }
    }

    private static Color heatColor(float h) {
        if (h <= 0.7f) {
            // cyan (0, 220, 255) → orange (255, 140, 0)
            float t = h / 0.7f;
            return new Color(
                    (int)(0   + t * 255),
                    (int)(220 - t * 80),
                    (int)(255 - t * 255)
            );
        } else {
            // orange (255, 140, 0) → red (255, 30, 30)
            float t = (h - 0.7f) / 0.3f;
            return new Color(
                    255,
                    (int)(140 - t * 110),
                    (int)(t * 30)
            );
        }
    }
}

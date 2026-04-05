package player;

import java.awt.*;

/**
 * Manages the player's energy shield (X key).
 *
 * While the shield is active it absorbs all incoming damage with no
 * HP reduction, at the cost of draining shield charge. The shield
 * cannot be activated below MIN_ACTIVATE charge, preventing players
 * from flickering it on for a single free-absorb frame.
 *
 * Recharge is intentionally slow (~15 seconds from empty) so the
 * player must think about when to spend it.
 *
 * Charge values are 0.0–1.0. The HUD reads getCharge() and maps it
 * to the SHD bar in NebulaHUD.
 */
public class ShieldSystem {

    // ── Tuning ────────────────────────────────────────────────────────
    private static final float DRAIN_PER_FRAME    = 0.012f; // ~5 s full drain
    private static final float RECHARGE_PER_FRAME = 0.004f; // ~15 s full recharge
    private static final float MIN_ACTIVATE       = 0.15f;  // can't activate below 15%
    private static final float HIT_DRAIN          = 0.15f;  // extra drain per absorbed hit

    // ── State ─────────────────────────────────────────────────────────
    private float   charge  = 1.0f;
    private boolean active  = false;

    // ── Per-frame update ──────────────────────────────────────────────
    public void update(boolean shieldKeyHeld) {
        if (shieldKeyHeld && (active || charge >= MIN_ACTIVATE)) {
            // Activate / stay active while key held and charge remains
            active = true;
            charge = Math.max(0f, charge - DRAIN_PER_FRAME);
            if (charge <= 0f) active = false;   // force off when depleted
        } else {
            active = false;
            charge = Math.min(1f, charge + RECHARGE_PER_FRAME);
        }
    }

    // ── Damage intercept — called from PlayerShip.takeDamage() ────────
    // Returns true if the shield absorbed the hit (no HP deducted).
    public boolean absorbDamage(int dmg) {
        if (!active) return false;
        charge = Math.max(0f, charge - HIT_DRAIN);
        if (charge <= 0f) active = false;
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public boolean isActive()   { return active;  }
    public float   getCharge()  { return charge;  }

    // ── Shield bubble overlay ──────────────────────────────────────────
    // Drawn around the ship while the shield is active.
    public void renderOverlay(Graphics2D g, double shipX, double shipY,
                              int shipWidth, int shipHeight) {
        if (!active) return;

        long now      = System.currentTimeMillis();
        float flicker = 0.3f + 0.05f * (float) Math.sin(now * 0.025f);
        Composite old = g.getComposite();

        // Outer glow ring
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(new Color(80, 220, 255));
        g.drawOval((int)(shipX - 12), (int)(shipY - 9),
                shipWidth + 24, shipHeight + 18);

        // Inner translucent bubble
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, flicker));
        g.setColor(new Color(80, 220, 255));
        g.drawOval((int)(shipX - 8), (int)(shipY - 6),
                shipWidth + 16, shipHeight + 12);

        // Fill — very faint interior tint
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
        g.fillOval((int)(shipX - 8), (int)(shipY - 6),
                shipWidth + 16, shipHeight + 12);

        g.setComposite(old);
    }
}

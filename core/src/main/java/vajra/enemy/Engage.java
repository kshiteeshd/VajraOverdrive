package vajra.enemy;

import vajra.ship.Ship;

/** THE THREAT MODEL AND THE ENGAGE BUDGET of Vitala's units (docs/VITALA_REDESIGN.md section 1).
 *
 *  <p><b>The threat model</b>: what a unit knows about the pilot's hull, read from the hull itself: where it will be when a shot arrives (so every tell is aimed AHEAD of it), how fast it
 *  is going, whether it is standing still (it is punished with ground laid under it), whether it is running (it is led harder), and whether its shield is up.
 *
 *  <p><b>The engage budget</b>: a crowd stays readable because only a few units are COMMITTED to an attack at any moment (a tell, a run, a burst, a seeding); the rest orbit and hold their
 *  formation until a place comes free. A leader may take one more than the rest. A place is held while the unit's act runs and for a short while after it, and is let go when it has not been
 *  used, so no unit starves and none hoards. */
public final class Engage {
    public static final int SLOTS = 4;
    private static final float HOLD = 1.0f;
    private final Enemy[] holders = new Enemy[SLOTS + 2];

    /** Where the hull will be in `t` seconds, written into out[0..1]: its velocity carried on, its turning ignored (the hull steers and brakes faster than a shot flies), the reach capped. */
    public static void lead(Ship h, float t, float[] out) {
        float k = Math.min(t, 1.1f);
        out[0] = h.x + h.vx * k;
        out[1] = h.y + h.vy * k;
    }

    /** The speed of the hull, px/s. */
    public static float speed(Ship h) {
        return (float) Math.hypot(h.vx, h.vy);
    }

    /** Is the hull standing still (or nearly): the one that can be pinned with ground laid beneath it. */
    public static boolean still(Ship h) {
        return speed(h) < 45f;
    }

    /** How fast the hull is closing on a body, px/s (negative: running away from it). */
    public static float closing(Ship h, Enemy e) {
        float dx = e.x - h.x, dy = e.y - h.y, d = Math.max(1f, (float) Math.hypot(dx, dy));
        return (h.vx * dx + h.vy * dy) / d;
    }

    /** The angle at which a round of speed `speed` leaves a body at (ex, ey) to meet the hull where it will be (a plain aim when the hull is still). */
    public static float aim(Ship h, float ex, float ey, float speed) {
        float d = Math.max(1f, (float) Math.hypot(h.x - ex, h.y - ey));
        float[] p = new float[2];
        lead(h, d / Math.max(1f, speed), p);
        float d2 = Math.max(1f, (float) Math.hypot(p[0] - ex, p[1] - ey));
        lead(h, d2 / Math.max(1f, speed), p);                                     // once more, with the flight time of the first guess
        return (float) Math.atan2(p[1] - ey, p[0] - ex);
    }

    // ---- the budget ----------------------------------------------------------------------------------------------------------------------

    private boolean committed(Enemy e) {
        return e != null && !e.dead && (e.vState != 0 || e.commitT > 0f);
    }

    /** How many units hold a place now. */
    public int taken() {
        int n = 0;
        for (int i = 0; i < holders.length; i++) {
            if (holders[i] != null && !committed(holders[i])) holders[i] = null;
            if (holders[i] != null) n++;
        }
        return n;
    }

    /** Ask for a place. True: the unit may begin its act (and holds the place for it); false: it holds its formation and tries again at its next turn. */
    public boolean request(Enemy e) {
        for (Enemy h : holders) if (h == e) { e.commitT = Math.max(e.commitT, HOLD); return true; }
        int n = taken(), cap = SLOTS + (e.rank >= 1 || e.elite ? 1 : 0);
        if (n >= cap) return false;
        for (int i = 0; i < holders.length; i++)
            if (holders[i] == null) {
                holders[i] = e;
                e.commitT = HOLD;
                return true;
            }
        return false;
    }

    public void clear() {
        java.util.Arrays.fill(holders, null);
    }
}

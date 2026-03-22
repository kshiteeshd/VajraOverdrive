package gfx;

/**
 * Singleton screen shake system.
 *
 * Usage:
 *   ScreenShake.trigger(8, 300);   // intensity 8px, 300ms duration
 *
 * GameCanvas reads getOffsetX() / getOffsetY() each frame and shifts
 * the drawImage call by that amount.
 *
 * Multiple triggers stack — the strongest active shake wins.
 */
public class ScreenShake {

    private static final ScreenShake instance = new ScreenShake();
    public static ScreenShake get() { return instance; }

    private float intensity  = 0;
    private long  endTime    = 0;
    private float offsetX    = 0;
    private float offsetY    = 0;

    private static final java.util.Random RNG = new java.util.Random();

    private ScreenShake() {}

    // ── Trigger ───────────────────────────────────────────────────────
    /**
     * @param intensity  max pixel displacement (4 = subtle, 12 = heavy)
     * @param durationMs how long the shake lasts in milliseconds
     */
    public static void trigger(float intensity, long durationMs) {
        ScreenShake s = get();
        // Stack — take the strongest
        if (intensity > s.intensity) s.intensity = intensity;
        long end = System.currentTimeMillis() + durationMs;
        if (end > s.endTime) s.endTime = end;
    }

    // ── Convenience levels ────────────────────────────────────────────
    public static void small()  { trigger(4,  180); }
    public static void medium() { trigger(7,  260); }
    public static void large()  { trigger(12, 380); }

    // ── Update (call once per frame) ──────────────────────────────────
    public void update() {
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            intensity = 0;
            offsetX   = 0;
            offsetY   = 0;
            return;
        }

        // Decay intensity over time
        float remaining = (endTime - now) / (float) Math.max(endTime - now + 1, 1);
        float current   = intensity * remaining;

        offsetX = (RNG.nextFloat() * 2 - 1) * current;
        offsetY = (RNG.nextFloat() * 2 - 1) * current;
    }

    // ── Read ──────────────────────────────────────────────────────────
    public int getOffsetX() { return (int) offsetX; }
    public int getOffsetY() { return (int) offsetY; }
    public boolean isActive() {
        return System.currentTimeMillis() < endTime;
    }
}
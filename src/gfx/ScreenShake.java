package gfx;

/**
 * Singleton screen shake system.
 *
 * FIX: intensity decay was broken — the formula
 *   (endTime - now) / Max(endTime - now + 1, 1)
 * always evaluated to ~1.0 because numerator ≈ denominator.
 * Shake never decayed; it stayed at full intensity then cut to
 * zero hard. Fixed by recording totalDuration at trigger time
 * and decaying as remaining/total.
 */
public class ScreenShake {

    private static final ScreenShake instance = new ScreenShake();
    public static ScreenShake get() { return instance; }

    private float intensity     = 0;
    private long  endTime       = 0;
    private long  totalDuration = 0;   // FIX: track duration set at trigger
    private float offsetX       = 0;
    private float offsetY       = 0;

    private static final java.util.Random RNG = new java.util.Random();

    private ScreenShake() {}

    // ── Trigger ───────────────────────────────────────────────────────
    public static void trigger(float intensity, long durationMs) {
        ScreenShake s = get();
        if (intensity > s.intensity) s.intensity = intensity;
        long end = System.currentTimeMillis() + durationMs;
        if (end > s.endTime) {
            s.endTime = end;
            // FIX: record the full duration so decay is calculated correctly
            s.totalDuration = durationMs;
        }
    }

    public static void small()  { trigger(4,  180); }
    public static void medium() { trigger(7,  260); }
    public static void large()  { trigger(12, 380); }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        long now = System.currentTimeMillis();
        if (now >= endTime || totalDuration == 0) {
            intensity     = 0;
            offsetX       = 0;
            offsetY       = 0;
            totalDuration = 0;
            return;
        }

        // FIX: decay from full intensity to zero over the duration
        // remaining/total goes from 1.0 → 0.0 as the shake expires
        float remaining = (endTime - now) / (float) totalDuration;
        float current   = intensity * remaining;

        offsetX = (RNG.nextFloat() * 2 - 1) * current;
        offsetY = (RNG.nextFloat() * 2 - 1) * current;
    }

    // ── Read ──────────────────────────────────────────────────────────
    public int     getOffsetX()  { return (int) offsetX; }
    public int     getOffsetY()  { return (int) offsetY; }
    public boolean isActive()    { return System.currentTimeMillis() < endTime; }
}
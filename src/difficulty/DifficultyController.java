package difficulty;

/**
 * Per-enemy-type difficulty scaling.
 *
 * Each enemy type has its own speed and fire rate curve so
 * harder enemy types don't become unfairly fast at high levels.
 *
 * Scaling reference:
 *
 *   BASIC   level 1: speed x1.03  fireRate x1.02
 *           level 25: speed x1.75  fireRate x1.50
 *
 *   FAST    level 1: speed x1.02  fireRate x1.03
 *           level 25: speed x1.50  fireRate x1.75
 *           (fast enemies scale fire rate more than speed)
 *
 *   TANK    level 1: speed x1.01  fireRate x1.01
 *           level 25: speed x1.40  fireRate x1.30
 *           (tanks stay slow — scaling is gentler)
 *
 *   SNIPER  level 1: speed x1.00  fireRate x1.02
 *           level 25: speed x1.20  fireRate x1.60
 *           (snipers barely move faster but fire much faster)
 */
public class DifficultyController {

    public enum EnemyClass { BASIC, FAST, TANK, SNIPER }

    // ── Per-class profiles ────────────────────────────────────────────

    public static DifficultyProfile getDifficulty(int level) {
        return getDifficulty(level, EnemyClass.BASIC);
    }

    public static DifficultyProfile getDifficulty(int level,
                                                  EnemyClass cls) {
        // Clamp level 1-25
        int l = Math.max(1, Math.min(level, 25));

        double speed;
        double fireRate;
        int    fleetBonus;

        switch (cls) {

            case FAST -> {
                // Scales fire rate aggressively, speed moderately
                speed      = 1.0 + l * 0.020;   // 1.02 – 1.50
                fireRate   = 1.0 + l * 0.030;   // 1.03 – 1.75
                fleetBonus = l / 6;
            }

            case TANK -> {
                // Gentle on both — tanks are already dangerous
                speed      = 1.0 + l * 0.016;   // 1.016 – 1.40
                fireRate   = 1.0 + l * 0.012;   // 1.012 – 1.30
                fleetBonus = l / 7;
            }

            case SNIPER -> {
                // Barely faster, fires much faster at high levels
                speed      = 1.0 + l * 0.008;   // 1.008 – 1.20
                fireRate   = 1.0 + l * 0.024;   // 1.024 – 1.60
                fleetBonus = l / 6;
            }

            default -> {   // BASIC
                speed      = 1.0 + l * 0.030;   // 1.03 – 1.75
                fireRate   = 1.0 + l * 0.020;   // 1.02 – 1.50
                fleetBonus = l / 5;
            }
        }

        return new DifficultyProfile(speed, fireRate, fleetBonus);
    }

    // ── Convenience: shoot delay in ms for a given class + level ──────
    /**
     * Returns the shoot delay in milliseconds for an enemy class
     * at the given level. FleetController uses this directly.
     *
     *   Base delays:
     *     BASIC  1800ms  →  ~900ms at level 25
     *     FAST   1200ms  →  ~480ms at level 25
     *     TANK   2400ms  →  ~1400ms at level 25
     *     SNIPER 2200ms  →  ~800ms at level 25
     */
    public static long shootDelay(int level, EnemyClass cls) {
        double base;
        switch (cls) {
            case FAST   -> base = 1200.0;
            case TANK   -> base = 2400.0;
            case SNIPER -> base = 2200.0;
            default     -> base = 1800.0;
        }
        double fireRate = getDifficulty(level, cls).fireRateMultiplier;
        long   delay    = (long)(base / fireRate);

        // Per-class minimums — never goes below these
        long min;
        switch (cls) {
            case FAST   -> min = 300;
            case TANK   -> min = 900;
            case SNIPER -> min = 600;
            default     -> min = 400;
        }
        return Math.max(delay, min);
    }
}
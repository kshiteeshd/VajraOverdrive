package boss;

import java.awt.Color;

/**
 * Immutable data container for a boss.
 * Each boss has multiple phases — phaseHealthThresholds defines
 * what fraction of max HP triggers each phase change.
 *
 * Example: thresholds = {0.66f, 0.33f} means:
 *   phase 0 = full HP down to 66%
 *   phase 1 = 66% down to 33%
 *   phase 2 = 33% down to 0%
 */
public class BossStats {

    public final String  name;
    public final int     maxHealth;
    public final int     width;
    public final int     height;
    public final int     scoreValue;
    public final Color   color;          // region color for FX + HUD bar
    public final float[] phaseThresholds; // descending, e.g. {0.66f, 0.33f}

    // Movement
    public final double  moveSpeed;       // horizontal sweep speed
    public final int     sweepAmplitude;  // max X offset from center

    // Fire
    public final long    fireRateMs;      // ms between shots in phase 0
    public final int     bulletDamage;

    public BossStats(
            String  name,
            int     maxHealth,
            int     width,
            int     height,
            int     scoreValue,
            Color   color,
            float[] phaseThresholds,
            double  moveSpeed,
            int     sweepAmplitude,
            long    fireRateMs,
            int     bulletDamage
    ) {
        this.name             = name;
        this.maxHealth        = maxHealth;
        this.width            = width;
        this.height           = height;
        this.scoreValue       = scoreValue;
        this.color            = color;
        this.phaseThresholds  = phaseThresholds;
        this.moveSpeed        = moveSpeed;
        this.sweepAmplitude   = sweepAmplitude;
        this.fireRateMs       = fireRateMs;
        this.bulletDamage     = bulletDamage;
    }
}
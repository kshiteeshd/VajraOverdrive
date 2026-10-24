package vajra.world;

/** THE FIRE VENTS of Vitala (docs/VITALA_PLAN.md section 5): holes in the wall that breathe a jet of flame ACROSS the river on a fixed beat.
 *  One clock: 1.2 s of warning glow, 0.8 s of flame, 1.0 s of cool, 3 s in all. A band's vents share the clock, so the pilot learns the beat
 *  (the Smelters teach it); the Hearth Deep runs two clocks half a beat apart. A lit shrine quiets the band: the vents hold cool. */
public final class Vents {
    public static final float PERIOD = 3f, WARN = 1.2f, FLAME = 0.8f;
    /** The jet's reach from the wall and half its width, world px. */
    public static final float LENGTH = 300f, HALF = 28f;
    public static final int COOL = 0, WARNING = 1, FLAMING = 2;

    /** {x, y, angle (radians, the way the jet runs), clock offset in seconds}. */
    public float[][] at = new float[0][];
    /** The Altar's rim vents ({x, y, angle, offset, 1}) breathe only while the boss is in his Kindling phase. */
    public boolean arenaOn;

    public int count() { return at.length; }

    /** Where in the 3 s beat vent i stands. */
    private float beat(int i, float clock) {
        float t = (clock + at[i][3]) % PERIOD;
        return t < 0 ? t + PERIOD : t;
    }

    /** COOL, WARNING or FLAMING at `clock`; a quieted band holds COOL. */
    public int state(int i, float clock, boolean quiet) {
        if (quiet || (at[i].length > 4 && at[i][4] > 0f && !arenaOn)) return COOL;
        float t = beat(i, clock);
        return t < WARN ? WARNING : t < WARN + FLAME ? FLAMING : COOL;
    }

    /** 0..1 through the current state (the warning glow builds, the flame rises and dies). */
    public float progress(int i, float clock) {
        float t = beat(i, clock);
        if (t < WARN) return t / WARN;
        if (t < WARN + FLAME) return (t - WARN) / FLAME;
        return (t - WARN - FLAME) / (PERIOD - WARN - FLAME);
    }

    /** Is the point (x, y), with radius r, inside vent i's jet right now. */
    public boolean burns(int i, float x, float y, float r, float clock, boolean quiet) {
        if (state(i, clock, quiet) != FLAMING) return false;
        float[] v = at[i];
        float dx = x - v[0], dy = y - v[1], c = (float) Math.cos(v[2]), s = (float) Math.sin(v[2]);
        float along = dx * c + dy * s, across = -dx * s + dy * c;
        return along > -r && along < LENGTH + r && Math.abs(across) < HALF + r;
    }
}

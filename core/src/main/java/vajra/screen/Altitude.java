package vajra.screen;

/** HOW HIGH THE HULL FLIES, place by place. The camera hangs {@link #D} above the flight plane looking straight down; the
 *  flight plane is `fly` above the ground, and `fly` changes along the world so the pilot feels far from the land over the
 *  meadow and the dream-circle (the ground small and slow beneath) and close to it in the mirror-wood and the mansion (the
 *  ground large and quick, the treetops above the hull). A point at height z projects at
 *
 *  <pre>scale(z) = D / (D + fly - z)</pre>
 *
 *  relative to the flight plane (z = fly gives 1). Nothing about play depends on it: aim, collision and AI live on the plane. */
final class Altitude {
    private Altitude() {}

    /** The camera's height above the flight plane. */
    static final float D = 900f;

    /** (y, fly) control points along the world, climbing from the spawn; eased between with a smoothstep. */
    private static final float[][] STOPS = {
            {0, 430}, {800, 400},          // Bhu Loka: high over the meadow, the world small and far
            {1250, 170}, {1700, 120},      // dropping into the mountains and through the pass: close to the rock
            {2300, 270}, {3800, 300},      // Svarna Dvara: a level cruise
            {4350, 105}, {7300, 90},       // Maya Vana: low among the trees, the canopy over the hull
            {8000, 360}, {11500, 380},     // Svapna Mandala: high, to read the circle
            {12300, 220}, {17000, 200},    // Bala Mukha: the throat
            {17700, 100}, {20600, 90},     // Bala's mansion: low, between the pillars
    };

    /** VITALA lies on top of Atala (y from 20600): a far cruise down the shaft, a level cruise over the Mouth, low in the forge-channels, high over
     *  the Court, low in the rapids, and mid-height over the Altar. Appended to Atala's own stops, so one smoothstep runs the whole world. */
    private static final float[][] VITALA = {
            {20600 + 400, 440}, {20600 + 2700, 460},
            {20600 + 3600, 260}, {20600 + 7000, 280},
            {20600 + 8000, 140}, {20600 + 12600, 120},
            {20600 + 13800, 340}, {20600 + 18000, 360},
            {20600 + 19200, 130}, {20600 + 23400, 110},
            {20600 + 24400, 230}, {20600 + 27000, 220},
    };

    private static final float[][] ALL = concat(STOPS, VITALA);

    private static float[][] concat(float[][] a, float[][] b) {
        float[][] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** Height of the flight plane above the ground at world height y (the whole campaign world: Bhu Loka, Atala, Vitala). */
    static float fly(float y) {
        float[][] st = ALL;
        if (y <= st[0][0]) return st[0][1];
        for (int i = 1; i < st.length; i++) {
            if (y <= st[i][0]) {
                float t = (y - st[i - 1][0]) / (st[i][0] - st[i - 1][0]);
                t = t * t * (3f - 2f * t);
                return st[i - 1][1] + (st[i][1] - st[i - 1][1]) * t;
            }
        }
        return st[st.length - 1][1];
    }

    /** Projection scale of a point at height z (above the ground) when the plane is at `fly`. */
    static float scale(float z, float fly) { return D / (D + fly - z); }

    /** The ground's own scale at `fly`. */
    static float ground(float fly) { return scale(0f, fly); }
}

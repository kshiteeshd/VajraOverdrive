package vajra.world;

/** THE HATAKI: the river of liquid gold that runs the length of Vitala (docs/VITALA_PLAN.md). One polyline with a width and a flow, as pure
 *  functions of the WORLD height, so the tile generator carves its cavern round it, the far floor paints it, the renderer draws it and the physics
 *  carries hulls on it from the same numbers. Vitala sits ON TOP of Atala in one continuous world: its floor is {@link #Y0}, the ceiling of Bala's
 *  arena, and its 27,000 px are the loka's ten thousand yojanas. The river runs UP the world (the pilot is driven to the top), meandering about
 *  the world axis and pinned to it at every gate so the doors line up. */
public final class Hataki {
    private Hataki() {}

    /** Where Vitala begins in the campaign world (the top of Atala's last arena), and how long it is: 27,000 px = 10,000 yojanas. */
    public static final float Y0 = 20600f, LENGTH = 27000f;
    public static final float YOJANA_PX = LENGTH / 10000f;
    public static final float WIDTH = 5200f, HEIGHT = Y0 + LENGTH;
    /** SUTALA, COMING SOON: past Bhairava's gold door a passage runs on into a closed hall and stops - the next loka is not built yet. The world's top
     *  is the hall's roof; the yojana count still ends at {@link #HEIGHT} (10,000 at the door). */
    public static final float SUTALA_LEN = 2400f, TOP = HEIGHT + SUTALA_LEN;
    public static final float HALL_X = 2600f, HALL_Y = HEIGHT + 1500f;
    /** The world axis (the same x = 2600 as Atala's gates): map columns 0..160 at origin 24 centre on it. */
    public static final float CX = 2600f;
    /** Band boundaries, bottom up: the Plunge, Hataki Mouth, the Smelters, the Bhuta Court, the Hearth Deep, the Altar. */
    public static final float[] START = {Y0, Y0 + 3000f, Y0 + 7600f, Y0 + 13200f, Y0 + 18600f, Y0 + 24000f};
    public static final float[] END = {Y0 + 3000f, Y0 + 7600f, Y0 + 13200f, Y0 + 18600f, Y0 + 24000f, HEIGHT};
    public static final String[] NAMES = {"The Plunge", "Hataki Mouth", "The Smelters", "The Bhuta Court", "The Hearth Deep", "Altar of Bhairava"};
    /** Where the river starts (it rises in the pool the hull lands in) and where the Altar's cataract takes it. */
    public static final float SOURCE_Y = Y0 + 40f, SINK_Y = Y0 + 24600f;
    /** The river is born at SOURCE_Y out of the dark, a thread that widens to its full breadth over this many px. */
    public static final float MOUTH_LEN = 620f;

    /** 0 at the source, 1 once the river has opened out. */
    public static float opened(float y) { return smooth((y - SOURCE_Y) / MOUTH_LEN); }
    /** The Altar: its centre, the hearth the boss keeps. */
    public static final float ALTAR_X = CX, ALTAR_Y = Y0 + 25500f;

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** 0 at every gate, 1 well clear of one: the river and its cavern come back to the axis at each door. */
    static float freeness(float y) {
        float m = 1f;
        for (int i = 1; i < START.length; i++) m = Math.min(m, smooth((Math.abs(y - START[i]) - 160f) / 480f));
        m = Math.min(m, smooth((y - Y0 - 250f) / 700f));                    // and from the bottom: the river is born on the axis
        return m;
    }

    /** The river's centre line at a height. */
    public static float centre(float y) {
        float amp = (y < START[2] ? 420f : y < START[3] ? 640f : y < START[4] ? 540f : 660f) * freeness(y);
        return CX + amp * (float) Math.sin(y / 1150f + 0.7f) + 0.28f * amp * (float) Math.sin(y / 430f + 2.1f);
    }

    /** Half the river's width at a height: broad in the Mouth, a channel in the Smelters, a pool in the Court, a throat in the Hearth. */
    public static float half(float y) {
        float h;
        if (y < START[1]) h = 150f;
        else if (y < START[2]) h = 190f;
        else if (y < START[3]) h = 100f;
        else if (y < START[4]) h = 150f;
        else if (y < START[5]) h = 85f;
        else h = 110f;
        return h * (1f + 0.22f * (float) Math.sin(y / 260f)) * (0.10f + 0.90f * opened(y));
    }

    /** Is the world's height inside Vitala at all. */
    public static boolean in(float y) { return y >= Y0; }

    /** Is there river at this height (the Plunge falls into it; the cataract ends it). */
    public static boolean flows(float y) { return y >= SOURCE_Y && y <= SINK_Y; }

    /** True when a point is on the river: the gold. */
    public static boolean on(float x, float y) {
        return flows(y) && Math.abs(x - centre(y)) < half(y);
    }

    /** How fast the current runs at a height, px per second: gentle in the pools, hard in the rapids of the Hearth Deep. */
    public static float speed(float y) {
        if (!flows(y)) return 0f;
        if (y < START[2]) return 34f;
        if (y < START[3]) return 70f;
        if (y < START[4]) return 42f;
        return 150f;
    }

    /** Is the current FORCED here (it can carry a hull against its will) or only a drift: only the Hearth Deep's rapids force. */
    public static boolean forced(float y) { return y >= START[4] + 200f && y < SINK_Y; }

    /** The flow at a point in px/s, written into out[0..1]: along the river's own tangent, strongest on the centre line, nothing off the gold. */
    public static void flow(float x, float y, float[] out) {
        out[0] = out[1] = 0f;
        if (!on(x, y)) return;
        float tx = centre(y + 8f) - centre(y - 8f), ty = 16f, len = (float) Math.sqrt(tx * tx + ty * ty);
        float edge = 1f - Math.abs(x - centre(y)) / half(y);
        float s = speed(y) * (0.45f + 0.55f * edge);
        out[0] = tx / len * s;
        out[1] = ty / len * s;
    }

    /** The Vitala band (0..5) a world height belongs to, or -1 below Vitala (that is Atala). */
    public static int band(float y) {
        if (y < Y0) return -1;
        int b = 0;
        for (int i = 0; i < START.length; i++) if (y >= START[i]) b = i;
        return b;
    }

    /** Yojanas climbed into Vitala at a world height (0 at its floor, 10,000 at the top of the Altar). */
    public static int yojana(float y) {
        return Math.round(Math.max(0f, Math.min(LENGTH, y - Y0)) / YOJANA_PX);
    }
}

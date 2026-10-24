package vajra.world;

/** THE LAND UNDER THE SHIP: one row per band of the world (docs/WORLD_BIBLE.md has the why). A band's biome is its soil,
 *  its water, its trees, its road, a style of ground pattern, and the light it lies in. {@link GroundMap} paints the
 *  rows; the renderer reads the light and the mist. Pure data: nothing here knows about GL.
 *
 *  <p>Bands follow {@code World.regionStart}: Bhu Loka (the sunlit surface, "Avatarana"), then the five bands of Atala.
 *  A later loka adds its bands and one row each. */
public final class Biome {
    public static final int PLAIN = 0, MANDALA = 1, RIBS = 2, MOSAIC = 3;

    public final String name;
    public final float[] soilDark, soilMid, soilLight;     // the ground, ramped by a height field
    public final float[] waterDeep, waterShine;
    public final float[] canopyDark, canopyLight;
    public final float[] road, roadEdge;
    public final float[] accent;                           // glints on the ground and the motes in the air
    public final float[] ambient;                          // the light multiplier over the whole frame
    public final float[] mist;                             // r, g, b, alpha of the drifting mist
    public final float water;                              // 0..~0.4: how much of the land is water
    public final float grove;                              // 0..1: how much of the land is trees
    public final float roadWidth;                          // world px; 0 = no road
    public final int style;
    public final boolean lotus;                            // lotus pads float on the water
    public final boolean mirror;                           // the water is a mirror: broad pale reflections

    private Biome(String name, float[] soil, float[] water, float[] canopy, float[] road, float[] accent,
                  float[] ambient, float[] mist, float waterAmt, float grove, float roadWidth, int style,
                  boolean lotus, boolean mirror) {
        this.name = name;
        this.soilDark = rgb(soil, 0);
        this.soilMid = rgb(soil, 3);
        this.soilLight = rgb(soil, 6);
        this.waterDeep = rgb(water, 0);
        this.waterShine = rgb(water, 3);
        this.canopyDark = rgb(canopy, 0);
        this.canopyLight = rgb(canopy, 3);
        this.road = rgb(road, 0);
        this.roadEdge = rgb(road, 3);
        this.accent = accent;
        this.ambient = ambient;
        this.mist = mist;
        this.water = waterAmt;
        this.grove = grove;
        this.roadWidth = roadWidth;
        this.style = style;
        this.lotus = lotus;
        this.mirror = mirror;
    }

    private static float[] rgb(float[] a, int at) { return new float[] {a[at], a[at + 1], a[at + 2]}; }

    private static float[] c(float r, float g, float b) { return new float[] {r, g, b}; }

    /** The bands. Soil follows the Vishnu Purana's list laid on the Bhagavata's order (Atala white, Vitala black,
     *  Sutala purple, Talatala yellow, Mahatala sand, Rasatala stone, Patala gold); the surface is real earth. */
    public static final Biome[] BANDS = {
            // 0 Bhu Loka - sunlit sandstone: the floor is stone like the walls (no green anywhere), outcrops where trees were
            new Biome("Bhu Loka",
                    new float[] {0.40f, 0.35f, 0.27f, 0.55f, 0.48f, 0.36f, 0.70f, 0.62f, 0.46f},
                    new float[] {0.30f, 0.26f, 0.20f, 0.62f, 0.56f, 0.44f},
                    new float[] {0.26f, 0.23f, 0.19f, 0.58f, 0.52f, 0.42f},
                    new float[] {0.62f, 0.52f, 0.34f, 0.44f, 0.35f, 0.21f},
                    c(1f, 0.88f, 0.45f), c(0.62f, 0.60f, 0.55f), new float[] {1f, 0.95f, 0.85f, 0.08f},
                    0.0f, 0.5f, 56, PLAIN, false, false),
            // 1 Svarna Dvara - the golden door: ivory stone like the wall tiles, a gold-paved road
            new Biome("Svarna Dvara",
                    new float[] {0.52f, 0.49f, 0.41f, 0.66f, 0.62f, 0.52f, 0.80f, 0.76f, 0.62f},
                    new float[] {0.30f, 0.28f, 0.22f, 0.66f, 0.62f, 0.50f},
                    new float[] {0.34f, 0.31f, 0.25f, 0.70f, 0.66f, 0.54f},
                    new float[] {0.88f, 0.70f, 0.26f, 0.56f, 0.40f, 0.12f},
                    c(1f, 0.9f, 0.5f), c(0.30f, 0.29f, 0.27f), new float[] {1f, 0.9f, 0.7f, 0.09f},
                    0.0f, 0.22f, 70, PLAIN, false, false),
            // 2 Maya Vana - cold stone, silver mirror pools that show another sky
            new Biome("Maya Vana",
                    new float[] {0.16f, 0.20f, 0.22f, 0.24f, 0.30f, 0.32f, 0.36f, 0.43f, 0.44f},
                    new float[] {0.40f, 0.48f, 0.62f, 0.88f, 0.93f, 1.00f},
                    new float[] {0.12f, 0.16f, 0.18f, 0.30f, 0.38f, 0.40f},
                    new float[] {0.46f, 0.40f, 0.56f, 0.25f, 0.22f, 0.36f},
                    c(0.82f, 0.72f, 1f), c(0.13f, 0.17f, 0.25f), new float[] {0.7f, 0.9f, 1f, 0.11f},
                    0.26f, 0.7f, 44, PLAIN, false, true),
            // 3 Svapna Mandala - violet stone drawn with golden rings, pools of hataka gold (no lotus pads)
            new Biome("Svapna Mandala",
                    new float[] {0.20f, 0.17f, 0.30f, 0.30f, 0.25f, 0.44f, 0.44f, 0.37f, 0.58f},
                    new float[] {0.30f, 0.20f, 0.52f, 0.92f, 0.72f, 0.96f},
                    new float[] {0.16f, 0.14f, 0.24f, 0.36f, 0.32f, 0.50f},
                    new float[] {0.70f, 0.55f, 0.85f, 0.40f, 0.30f, 0.56f},
                    c(1f, 0.82f, 0.5f), c(0.17f, 0.14f, 0.25f), new float[] {0.9f, 0.8f, 1f, 0.11f},
                    0.30f, 0.0f, 0, MANDALA, false, false),
            // 4 Bala Mukha - the yawn: ribbed maroon stone, amber edges
            new Biome("Bala Mukha",
                    new float[] {0.30f, 0.12f, 0.16f, 0.46f, 0.20f, 0.22f, 0.64f, 0.34f, 0.28f},
                    new float[] {0.50f, 0.16f, 0.12f, 1.00f, 0.55f, 0.25f},
                    new float[] {0.20f, 0.10f, 0.12f, 0.44f, 0.20f, 0.18f},
                    new float[] {0.80f, 0.50f, 0.20f, 0.50f, 0.28f, 0.10f},
                    c(1f, 0.62f, 0.3f), c(0.20f, 0.12f, 0.12f), new float[] {1f, 0.7f, 0.6f, 0.12f},
                    0.0f, 0.10f, 0, RIBS, false, false),
            // 5 Bala's court - an ivory and gold mosaic floor under a ruler who believes it is his
            new Biome("Bala's Court",
                    new float[] {0.60f, 0.56f, 0.48f, 0.80f, 0.76f, 0.66f, 0.92f, 0.88f, 0.76f},
                    new float[] {0.40f, 0.30f, 0.10f, 1.00f, 0.82f, 0.40f},
                    new float[] {0.30f, 0.26f, 0.12f, 0.60f, 0.50f, 0.20f},
                    new float[] {0.86f, 0.66f, 0.22f, 0.52f, 0.36f, 0.10f},
                    c(1f, 0.9f, 0.55f), c(0.26f, 0.22f, 0.18f), new float[] {1f, 0.9f, 0.7f, 0.10f},
                    0.0f, 0.0f, 0, MOSAIC, false, false),
    };

    /** VITALA (docs/VITALA_PLAN.md): black basalt veined with gold, a warm black light, the Hataki's gold for water. Soil is near-black with an ember
     *  undertone; "water" is the gold of the river (painted along Hataki.centre, not by noise); the accent is the ember. */
    public static final Biome[] VITALA = {
            // 0 The Plunge - the shaft: cold black, the first gold glow far below
            new Biome("The Plunge",
                    new float[] {0.045f, 0.042f, 0.050f, 0.085f, 0.075f, 0.080f, 0.140f, 0.115f, 0.115f},
                    new float[] {0.40f, 0.22f, 0.06f, 1.00f, 0.72f, 0.28f},
                    new float[] {0.05f, 0.05f, 0.06f, 0.12f, 0.10f, 0.10f},
                    new float[] {0.80f, 0.55f, 0.20f, 0.45f, 0.28f, 0.08f},
                    c(1f, 0.55f, 0.22f), c(0.19f, 0.15f, 0.16f), new float[] {0.8f, 0.35f, 0.15f, 0.10f},
                    0.0f, 0.0f, 0, PLAIN, false, false),
            // 1 Hataki Mouth - where the river is born: broad gold, slag flags, the safe band
            new Biome("Hataki Mouth",
                    new float[] {0.060f, 0.052f, 0.052f, 0.110f, 0.092f, 0.088f, 0.180f, 0.140f, 0.120f},
                    new float[] {0.45f, 0.26f, 0.06f, 1.00f, 0.78f, 0.30f},
                    new float[] {0.06f, 0.05f, 0.05f, 0.14f, 0.11f, 0.09f},
                    new float[] {0.90f, 0.62f, 0.20f, 0.50f, 0.30f, 0.07f},
                    c(1f, 0.70f, 0.28f), c(0.30f, 0.22f, 0.17f), new float[] {1f, 0.72f, 0.38f, 0.09f},
                    0.0f, 0.0f, 0, PLAIN, false, false),
            // 2 The Smelters - forge channels: the heat deepens, the light goes red
            new Biome("The Smelters",
                    new float[] {0.075f, 0.045f, 0.040f, 0.140f, 0.080f, 0.068f, 0.230f, 0.125f, 0.092f},
                    new float[] {0.50f, 0.20f, 0.04f, 1.00f, 0.62f, 0.18f},
                    new float[] {0.07f, 0.04f, 0.04f, 0.16f, 0.09f, 0.07f},
                    new float[] {0.95f, 0.50f, 0.14f, 0.55f, 0.24f, 0.05f},
                    c(1f, 0.52f, 0.18f), c(0.27f, 0.17f, 0.13f), new float[] {1f, 0.50f, 0.22f, 0.12f},
                    0.0f, 0.0f, 0, PLAIN, false, false),
            // 3 The Bhuta Court - Hara-Bhava's hall: ghost-lit, violet-black, gold rings drawn on the floor
            new Biome("The Bhuta Court",
                    new float[] {0.050f, 0.045f, 0.075f, 0.092f, 0.082f, 0.135f, 0.150f, 0.130f, 0.215f},
                    new float[] {0.40f, 0.30f, 0.10f, 1.00f, 0.80f, 0.40f},
                    new float[] {0.05f, 0.05f, 0.08f, 0.12f, 0.11f, 0.17f},
                    new float[] {0.80f, 0.70f, 0.40f, 0.42f, 0.34f, 0.18f},
                    c(0.75f, 0.85f, 1f), c(0.20f, 0.17f, 0.24f), new float[] {0.7f, 0.8f, 1f, 0.12f},
                    0.0f, 0.0f, 0, MANDALA, false, false),
            // 4 The Hearth Deep - Bhairava's own fires: ribbed forge-floor, the light white-hot at the seams
            new Biome("The Hearth Deep",
                    new float[] {0.090f, 0.040f, 0.030f, 0.170f, 0.075f, 0.052f, 0.280f, 0.120f, 0.075f},
                    new float[] {0.55f, 0.18f, 0.03f, 1.00f, 0.55f, 0.12f},
                    new float[] {0.09f, 0.04f, 0.03f, 0.18f, 0.08f, 0.05f},
                    new float[] {1.00f, 0.45f, 0.10f, 0.60f, 0.22f, 0.04f},
                    c(1f, 0.45f, 0.14f), c(0.30f, 0.15f, 0.10f), new float[] {1f, 0.42f, 0.18f, 0.14f},
                    0.0f, 0.0f, 0, RIBS, false, false),
            // 5 The Altar - the sacrificial hearth: a black and gold mosaic under a fire that was never allowed to die
            new Biome("The Altar",
                    new float[] {0.080f, 0.064f, 0.050f, 0.150f, 0.118f, 0.082f, 0.260f, 0.200f, 0.128f},
                    new float[] {0.55f, 0.35f, 0.08f, 1.00f, 0.85f, 0.40f},
                    new float[] {0.08f, 0.06f, 0.05f, 0.18f, 0.14f, 0.09f},
                    new float[] {0.95f, 0.72f, 0.22f, 0.55f, 0.38f, 0.08f},
                    c(1f, 0.78f, 0.35f), c(0.30f, 0.21f, 0.15f), new float[] {1f, 0.65f, 0.3f, 0.12f},
                    0.0f, 0.0f, 0, MOSAIC, false, false),
    };
    private static final float[][] VITALA_DEEP = {
            {0.10f, 0.03f, 0.03f}, {0.20f, 0.07f, 0.03f}, {0.28f, 0.07f, 0.02f},
            {0.14f, 0.07f, 0.20f}, {0.34f, 0.08f, 0.02f}, {0.36f, 0.20f, 0.05f}};

    /** Every band of the campaign world, bottom up: Bhu Loka and Atala's five (BANDS), then Vitala's six. */
    public static final Biome[] ALL = concat(BANDS, VITALA);

    private static Biome[] concat(Biome[] a, Biome[] b) {
        Biome[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** Is this one of Vitala's rows (black basalt, gold seams). */
    public static boolean isVitala(Biome b) {
        for (Biome v : VITALA) if (v == b) return true;
        return false;
    }

    public static Biome of(int band) { return ALL[Math.max(0, Math.min(ALL.length - 1, band))]; }

    /** Which band a world y falls in: the last start at or below it. */
    public static int band(float y, float[] starts) {
        int b = 0;
        for (int i = 0; i < starts.length; i++) if (y >= starts[i]) b = i;
        return b;
    }

    /** How far either side of a boundary the light eases from one band's to the next (world px). */
    public static final float FADE = 500f;

    /** The frame's ambient light at a height: this band's, drawn toward its neighbour's near a boundary. */
    public static float[] ambient(float y, float[] starts, float[] out) {
        return mix(y, starts, out, true);
    }

    /** The mist's r, g, b, alpha at a height, eased the same way. */
    public static float[] mist(float y, float[] starts, float[] out) {
        return mix(y, starts, out, false);
    }

    /** How many layers of wall stand behind the play layer in each band: ten everywhere (the plan's per-band counts were cut to ten
     *  for speed and for a shallower shaft). */
    private static final float[] STACK = {10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f};
    /** The colour of the deepest layer of each band's stack (the tint runs from white at the front to this at the back). */
    private static final float[][] DEEP = {
            {0.08f, 0.11f, 0.26f}, {0.26f, 0.20f, 0.11f}, {0.04f, 0.16f, 0.18f},
            {0.16f, 0.10f, 0.28f}, {0.26f, 0.09f, 0.09f}, {0.32f, 0.27f, 0.14f}};
    private static final float[][] DEEP_ALL = concatDeep(DEEP, VITALA_DEEP);

    private static float[][] concatDeep(float[][] a, float[][] b) {
        float[][] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** The surface has none: the stack opens with the ground, from nothing at y 1000 to whole at y 1500. */
    public static final float STACK_FROM = 1000f, STACK_FULL = 1500f;

    /** How many layers stand behind the play layer at a height (fractional: the farthest one fades in). Eased across bands like the
     *  light, and zero on the open meadow. */
    public static float layers(float y, float[] starts) {
        int b = band(y, starts), o = b;
        float t = 0;
        if (b > 0 && y - starts[b] < FADE) {
            o = b - 1;
            t = 0.5f * (1f - (y - starts[b]) / FADE);
        } else if (b < starts.length - 1 && starts[b + 1] - y < FADE) {
            o = b + 1;
            t = 0.5f * (1f - (starts[b + 1] - y) / FADE);
        }
        float n = STACK[Math.min(b, STACK.length - 1)] + (STACK[Math.min(o, STACK.length - 1)] - STACK[Math.min(b, STACK.length - 1)]) * t;
        float open = Math.max(0f, Math.min(1f, (y - STACK_FROM) / (STACK_FULL - STACK_FROM)));
        return n * open * open * (3f - 2f * open);
    }

    /** How strongly band `band` holds at height y: 1 inside it, easing to 0 over FADE across either boundary. */
    public static float bandWeight(float y, float[] starts, int band) {
        float lo = starts[band];
        float in = smooth01((y - (lo - FADE / 2f)) / FADE);
        if (band + 1 >= starts.length) return in;
        float out = smooth01((y - (starts[band + 1] - FADE / 2f)) / FADE);
        return in * (1f - out);
    }

    private static float smooth01(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** The deepest layer's colour at a height, eased like everything else. */
    public static float[] deepTint(float y, float[] starts, float[] out) {
        int b = band(y, starts), o = b;
        float t = 0;
        if (b > 0 && y - starts[b] < FADE) {
            o = b - 1;
            t = 0.5f * (1f - (y - starts[b]) / FADE);
        } else if (b < starts.length - 1 && starts[b + 1] - y < FADE) {
            o = b + 1;
            t = 0.5f * (1f - (starts[b + 1] - y) / FADE);
        }
        float[][] dr = DEEP_ALL;
        float[] a = dr[Math.min(b, dr.length - 1)], c = dr[Math.min(o, dr.length - 1)];
        for (int i = 0; i < 3; i++) out[i] = a[i] + (c[i] - a[i]) * t;
        return out;
    }

    private static float[] mix(float y, float[] starts, float[] out, boolean light) {
        int b = band(y, starts), o = b;
        float t = 0;
        if (b > 0 && y - starts[b] < FADE) {
            o = b - 1;
            t = 0.5f * (1f - (y - starts[b]) / FADE);
        } else if (b < starts.length - 1 && starts[b + 1] - y < FADE) {
            o = b + 1;
            t = 0.5f * (1f - (starts[b + 1] - y) / FADE);
        }
        float[] a = light ? of(b).ambient : of(b).mist, c = light ? of(o).ambient : of(o).mist;
        for (int i = 0; i < out.length && i < a.length; i++) out[i] = a[i] + (c[i] - a[i]) * t;
        return out;
    }
}

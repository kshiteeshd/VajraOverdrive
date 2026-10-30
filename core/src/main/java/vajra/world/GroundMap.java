package vajra.world;

/** THE LAND, PAINTED. A pure function from a point of the world to a colour: soil ramped by a height field, a pattern of
 *  the band's style (mandala rings, throat ribs, a mosaic court), water with shores and ripples (lotus pads on a dream
 *  lake), a winding road, groves of trees that cast a shadow on the ground, and the odd gem-glint. Nothing is random at
 *  run time: the same seed paints the same land, so a chunk painted twice is the same chunk and the seams between
 *  chunks cannot show. One texel is {@link #TEXEL} world px; the renderer paints it in chunks as the camera nears.
 *
 *  <p>See docs/WORLD_BIBLE.md for what each band is meant to be. */
public final class GroundMap {
    public static final int TEXEL = 4;
    /** The one seed the world's land is painted from (the renderer and the minimap's thumbnail both use it). */
    public static final long SEED = 0xA7A1A5L;

    private final int seed;
    private final float worldW, worldH;
    private final float[] starts;

    public GroundMap(long seed, float worldW, float worldH, float[] regionStart) {
        this.seed = (int) (seed ^ (seed >>> 32));
        this.worldW = worldW;
        this.worldH = worldH;
        this.starts = regionStart.clone();
    }

    /** The band a point belongs to: the true boundaries, frayed by noise so a border reads as terrain, not a ruled line. */
    public int bandAt(float wx, float wy) {
        float jitter = (fbm(wx * 0.0035f, wy * 0.0035f, seed + 90, 3) - 0.5f) * 520f;
        return Biome.band(wy + jitter, starts);
    }

    /** Fill a square chunk of `size` x `size` texels whose lower-left texel is (tx0, ty0); `out` is row-major, row 0 the BOTTOM
     *  row, each entry 0xAARRGGBB. */
    public void fillChunk(int tx0, int ty0, int size, int[] out) {
        for (int j = 0; j < size; j++)
            for (int i = 0; i < size; i++) out[j * size + i] = argb(tx0 + i, ty0 + j);
    }

    /** One texel's colour, 0xFF000000 | rgb. */
    public int argb(int tx, int ty) { return 0xFF000000 | (texel(tx, ty) & 0xFFFFFF); }

    /** One texel of the land: the colour in the low 24 bits and its HEIGHT (0..255 world units above the ground) in the top
     *  byte. Heights come in cut planes (terraces), never smooth slopes: the raycaster draws the steps as 3D. */
    public int texel(int tx, int ty) {
        float wx = (tx + 0.5f) * TEXEL, wy = (ty + 0.5f) * TEXEL;
        Biome b = Biome.of(bandAt(wx, wy));
        float[] c = new float[4];
        paint(b, tx, ty, wx, wy, c);
        return (Math.max(0, Math.min(255, (int) c[3])) << 24) | (to8(c[0]) << 16) | (to8(c[1]) << 8) | to8(c[2]);
    }

    /** The relief at a point in world units, before props: terraced mountains flanking the pass, low hills on the surface,
     *  the throat's ribs, the mandala's raised rings, the court's steps. */
    public float relief(Biome b, float wx, float wy) {
        float h = 0f;
        if (b == Biome.BANDS[0] || wy < 2400f) {
            float side = Math.abs(wx - worldW * 0.5f);
            float pass = 560f + 700f * smooth(Math.abs(wy - 1600f) / 900f);               // the pass is narrowest at the boundary
            float mask = smooth((side - pass + 120f) / 520f) * smooth((wy - 450f) / 600f) * (1f - smooth((wy - 1850f) / 500f));
            if (mask > 0f) {
                float r = fbm(wx * 0.0028f, wy * 0.0028f, seed + 30, 4);
                float m = 215f * mask * (0.25f + 1.1f * r);
                h = Math.max(h, (float) Math.floor(m / 13f) * 13f);                      // terraces: planes cut at every 13 units
            }
            if (b == Biome.BANDS[0]) {
                float hill = 34f * fbm(wx * 0.0041f, wy * 0.0041f, seed + 31, 3);
                h = Math.max(h, (float) Math.floor(hill / 7f) * 7f);
            }
        }
        switch (b.style) {
            case Biome.RIBS: {
                float v = (float) Math.sin(wy * 0.021f + (float) Math.sin(wx * 0.0031f) * 2.6f);
                if (v > 0.45f) h = Math.max(h, (float) Math.floor((v - 0.45f) / 0.55f * 5f) * 8f);
                break;
            }
            case Biome.MANDALA: {
                float dx = wx - worldW * 0.5f, dy = wy - mandalaCentreY(wy), r = (float) Math.sqrt(dx * dx + dy * dy);
                if (r <= 1150f && r >= 150f) {
                    float ring = r / 150f, f = Math.abs(ring - Math.round(ring));
                    if (f * 150f < 4f) h = Math.max(h, 9f);
                }
                break;
            }
            case Biome.MOSAIC: {
                float dx = wx - worldW * 0.5f, dy = wy - throneY(wy), r = (float) Math.sqrt(dx * dx + dy * dy);
                if (r < 760f) h = Math.max(h, (float) Math.floor((760f - r) / 152f) * 7f);   // the dais: steps up toward the throne
                break;
            }
            default: break;
        }
        return h;
    }

    /** The centre of the last band's dais: Bala's court sits 1500 into its band, the Altar's hearth is where the map says. */
    private float throneY(float wy) { return wy >= Hataki.Y0 ? Hataki.ALTAR_Y : starts[Math.min(5, starts.length - 1)] + 1500f; }

    private static float smooth(float t) {
        t = clamp(t);
        return t * t * (3f - 2f * t);
    }

    /** The rock of a raised stretch of ground: brown low, grey mid, pale-capped high, each terrace edged darker. */
    private void rockColour(float h, float[] c) {
        float low = clamp(h / 55f);
        float[] rl = {0.40f, 0.35f, 0.30f}, rm = {0.54f, 0.52f, 0.50f}, rc = {0.88f, 0.89f, 0.93f};
        float[] r = new float[3];
        mixInto(r, rl, rm, clamp((h - 35f) / 80f));
        for (int i = 0; i < 3; i++) r[i] += (rc[i] - r[i]) * clamp((h - 135f) / 60f);
        for (int i = 0; i < 3; i++) c[i] += (r[i] - c[i]) * low;
    }

    private static int to8(float v) { return Math.max(0, Math.min(255, (int) (v * 255f + 0.5f))); }

    /** Where the road runs at a height: a long slow weave with a short one on top. */
    public float roadCentre(float wy) {
        float free = worldW * 0.5f + (float) Math.sin(wy / 900f + 1.3f) * 900f + (float) Math.sin(wy / 330f) * 160f;
        float pin = 1f - clamp(Math.abs(wy - 1600f) / 900f);        // the road runs through the pass in the boulders
        pin = pin * pin * (3f - 2f * pin);
        return free + (worldW * 0.5f - free) * pin;
    }

    public static final int LAND = 0, WATER = 1, ROAD = 2;

    /** What a point is: open land, water or road (props keep off the last two). */
    public int surface(float wx, float wy) {
        Biome b = Biome.of(bandAt(wx, wy));
        if (b.roadWidth > 0f && Math.abs(wx - roadCentre(wy)) < b.roadWidth * 0.5f + 6f) return ROAD;
        if (Hataki.flows(wy) && Math.abs(wx - Hataki.centre(wy)) < Hataki.half(wy) + 6f) return WATER;
        if (b.water > 0f && fbm(wx * 0.0028f + 13f, wy * 0.0028f + 5f, seed + 4, 3) < b.water + 0.03f) return WATER;
        return LAND;
    }

    /** Where the mandala of the band's period is centred, for the dream-pillars. */
    public float mandalaCentreY(float wy) {
        // each mandala band repeats its circle every 2400 px from its own floor: Svapna Mandala in Atala, the Bhuta Court in Vitala
        float y0 = starts[Math.min(3, starts.length - 1)], period = 2400f;
        if (starts.length > 9 && wy >= starts[9]) y0 = starts[9];
        return y0 + (float) Math.floor((wy - y0) / period) * period + period * 0.5f;
    }

    // ---- the painting -------------------------------------------------------------------------------------------

    private void paint(Biome b, int tx, int ty, float wx, float wy, float[] c) {
        float n1 = fbm(wx * 0.0046f, wy * 0.0046f, seed + 1, 4);
        float n2 = fbm(wx * 0.021f, wy * 0.021f, seed + 2, 3);
        float hgt = 0.62f * n1 + 0.38f * n2;
        float t = clamp((hgt - 0.28f) / 0.44f);
        if (t < 0.5f) mixInto(c, b.soilDark, b.soilMid, t * 2f);
        else mixInto(c, b.soilMid, b.soilLight, (t - 0.5f) * 2f);
        // CALM STONE: no speckle, no slabs, no seams: the floor is read by large soft shapes of light and shade (a slow mottling and a long strata), never by lines
        float mott = 0.90f + 0.20f * fbm(wx * 0.0021f, wy * 0.0021f, seed + 44, 3);
        float strata = 1f + 0.035f * (float) Math.sin(wy * 0.0105f + n1 * 6f);
        c[0] *= mott * strata;
        c[1] *= mott * strata;
        c[2] *= mott * strata;

        float rim = 0f;                                  // set where the ground is raised or edged (drawn after the soil)
        switch (b.style) {
            case Biome.MANDALA: rim = mandala(b, wx, wy, c); break;
            case Biome.RIBS: ribs(b, wx, wy, c); break;
            case Biome.MOSAIC: mosaic(b, wx, wy, c); break;
            default: break;
        }
        if (b == Biome.BANDS[0]) chasm(wy, n2, c);       // the surface splits into the hole as it nears the gate

        boolean onWater = false, onRoad = false;
        if (b.water > 0f) {
            float fw = fbm(wx * 0.0028f + 13f, wy * 0.0028f + 5f, seed + 4, 3);
            if (fw < b.water) {
                onWater = true;
                float depth = clamp((b.water - fw) / b.water * 1.7f);
                mixInto(c, b.waterShine, b.waterDeep, depth);
                float ripple = (float) Math.sin((wx * 0.9f + wy * 0.5f) * 0.06f + n2 * 8f);
                if (ripple > 0.88f) lift(c, b.waterShine, 0.45f);
                if (b.mirror) { // a mirror: broad pale sheets of reflection that slide as the point moves
                    float sheet = (float) Math.sin(wx * 0.012f - wy * 0.007f + n1 * 5f);
                    if (sheet > 0.55f) lift(c, b.waterShine, 0.28f + 0.3f * (sheet - 0.55f));
                }
                if (b.lotus) lotus(wx, wy, tx, ty, c);
            } else if (fw < b.water + 0.03f) {
                lift(c, b.soilLight, 0.55f);               // the shore
            }
        }
        if (Hataki.flows(wy)) { // THE HATAKI: liquid gold, brightest on its centre, streaked along its own flow, a hot rim on the bank
            float d = Math.abs(wx - Hataki.centre(wy)), hw = Hataki.half(wy);
            if (d < hw) {
                onWater = true;
                mixInto(c, b.waterShine, b.waterDeep, clamp(d / hw) * 0.8f);
                float born = Hataki.opened(wy);                                       // out of the dark: the gold is black where it is born and kindles as it opens
                if (born < 1f) mixInto(c, c.clone(), new float[] {0.03f, 0.015f, 0.012f, 1f}, (1f - born) * (1f - born) * 0.92f);
                float streak = (float) Math.sin(wy * 0.045f + (float) Math.sin(wx * 0.03f + n2 * 5f) * 2.4f);
                if (streak > 0.82f) lift(c, new float[] {1f, 0.96f, 0.70f}, 0.55f);
            } else if (d < hw + 14f) {
                lift(c, b.accent, 0.5f * (1f - (d - hw) / 14f));
            }
        }
        if (b.roadWidth > 0f) {
            float cx = roadCentre(wy);
            float d = Math.abs(wx - cx), half = b.roadWidth * 0.5f;
            if (d < half) {
                onRoad = true;
                boolean edge = d > half - 5f;
                float slab = ((int) Math.floor(wy / 24f) & 1) == 0 ? 0f : 0.06f;
                mixInto(c, b.road, b.roadEdge, edge ? 1f : (rnd(tx, ty, seed + 6) * 0.25f));
                c[0] += slab;
                c[1] += slab;
                c[2] += slab;
            }
        }
        if (!onWater && rnd(tx, ty, seed + 7) < 0.0014f) lift(c, b.accent, 0.9f); // a glint of gem-dust
        if (rim > 0f) lift(c, b.accent, rim);
        float h = onWater ? 0f : relief(b, wx, wy);
        if (h > 0f && b.style == Biome.PLAIN) rockColour(h, c);
        grade(b, c, wx, wy);                              // the loka's own light over all of it, the rock too
        c[3] = h;
    }

    /** THE LOKA'S OWN LIGHT on the finished colour: Atala (bila-svarga, "heavens in holes", sunless and lit by jewels) is read cool and quiet, its stone pale in moonlight; Vitala is basalt and
     *  ash, warm and dark, and the only gold in it is the river. The surface (Bhu Loka) is left as it was. */
    private void grade(Biome b, float[] c, float wx, float wy) {
        if (b == Biome.BANDS[0]) return;
        float luma = 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
        float sat, mul, tr, tg, tb;
        if (Biome.isVitala(b)) { sat = 0.80f; mul = 0.80f; tr = 1.04f; tg = 0.98f; tb = 0.96f; }
        else { sat = 0.70f; mul = b == Biome.BANDS[1] ? 0.92f : 0.60f; tr = 0.92f; tg = 0.99f; tb = 1.09f; }       // (the golden door is the one lit threshold: ivory, a little cooled)
        float vig = 1f - 0.10f * (float) Math.pow(Math.abs(wx / worldW - 0.5f) * 2f, 2.0);        // the hall falls into darkness toward its walls
        c[0] = (luma + (c[0] - luma) * sat) * mul * tr * vig;
        c[1] = (luma + (c[1] - luma) * sat) * mul * tg * vig;
        c[2] = (luma + (c[2] - luma) * sat) * mul * tb * vig;
    }

    /** The golden rings of the dream-circle: concentric circles and spokes drawn into the ground, a new mandala every 2400 px. */
    private float mandala(Biome b, float wx, float wy, float[] c) {
        float cy = mandalaCentreY(wy);
        float dx = wx - worldW * 0.5f, dy = wy - cy, r = (float) Math.sqrt(dx * dx + dy * dy);
        if (r > 1150f) return 0f;
        float ring = r / 150f, f = Math.abs(ring - Math.round(ring));
        float line = 0f;
        if (f * 150f < 2.2f && Math.round(ring) >= 1) line = 0.30f;                    // faint rings only: the spokes are gone
        float lit = 1f + 0.08f * (1f - r / 1150f);            // the court inside the rings is a shade brighter
        c[0] *= lit;
        c[1] *= lit;
        c[2] *= lit;
        return line;
    }

    /** The ribs of the throat: bands that run across the land and bow with a slow wave, amber on the crests. */
    private void ribs(Biome b, float wx, float wy, float[] c) {
        float v = (float) Math.sin(wy * 0.021f + (float) Math.sin(wx * 0.0031f) * 2.6f);
        float shade = 0.93f + 0.07f * v;
        c[0] *= shade;
        c[1] *= shade;
        c[2] *= shade;
        if (v > 0.93f) lift(c, b.accent, 0.12f);
    }

    /** Bala's court: an ivory and gold checker of 80 px slabs, edged, with concentric gold rings round the dais. */
    private void mosaic(Biome b, float wx, float wy, float[] c) {
        int cx = (int) Math.floor(wx / 80f), cy = (int) Math.floor(wy / 80f);
        // (the checker of slabs is gone: one calm polished floor, with the rings inlaid in it)
        float dx = wx - worldW * 0.5f, dy = wy - throneY(wy);
        float r = (float) Math.sqrt(dx * dx + dy * dy);
        if (r < 760f) {
            float ring = r / 120f, f = Math.abs(ring - Math.round(ring));
            if (f * 120f < 2f) lift(c, b.road, 0.28f * (1f - r / 760f));
        }
    }

    /** Where the surface nears the gate it darkens and cracks: the ground is about to open. */
    private void chasm(float wy, float n2, float[] c) {
        float gate = starts.length > 1 ? starts[1] : 1600f;
        float from = gate - 420f;
        if (wy < from) return;
        float t = clamp((wy - from) / 420f);
        t = t * t * (0.55f + 0.45f * n2);
        c[0] += (0.13f - c[0]) * t;
        c[1] += (0.11f - c[1]) * t;
        c[2] += (0.12f - c[2]) * t;
    }

    /** Lotus pads on a dream lake: round green pads with a notch, a pink or white flower on some. */
    private void lotus(float wx, float wy, int tx, int ty, float[] c) {
        final int cell = 9;
        int cx = Math.floorDiv(tx, cell), cy = Math.floorDiv(ty, cell);
        if (rnd(cx, cy, seed + 20) > 0.5f) return;
        float rad = 2.2f + 1.4f * rnd(cx, cy, seed + 21);
        float px = cx * cell + rad + 1f + rnd(cx, cy, seed + 22) * (cell - 2 * rad - 2f);
        float py = cy * cell + rad + 1f + rnd(cx, cy, seed + 23) * (cell - 2 * rad - 2f);
        float dx = tx + 0.5f - px, dy = ty + 0.5f - py, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d >= rad) return;
        c[0] = 0.18f + 0.10f * (1f - d / rad);
        c[1] = 0.46f + 0.18f * (1f - d / rad);
        c[2] = 0.30f;
        if (d < 1.1f && rnd(cx, cy, seed + 24) < 0.55f) {            // the flower
            boolean pink = rnd(cx, cy, seed + 25) < 0.6f;
            c[0] = 1f;
            c[1] = pink ? 0.66f : 0.96f;
            c[2] = pink ? 0.82f : 0.92f;
        }
    }

    // ---- noise ---------------------------------------------------------------------------------------------------

    private static int hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** A value in [0, 1) fixed by the cell and the seed. */
    public static float rnd(int x, int y, int s) { return (hash(x, y, s) & 0xFFFFFF) / 16777216f; }

    private static float vnoise(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3f - 2f * fx);
        fy = fy * fy * (3f - 2f * fy);
        float a = rnd(xi, yi, s), b = rnd(xi + 1, yi, s), c = rnd(xi, yi + 1, s), d = rnd(xi + 1, yi + 1, s);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    /** Fractal value noise, roughly in [0, 1] and centred near 0.5. */
    public static float fbm(float x, float y, int s, int octaves) {
        float sum = 0f, amp = 0.5f, norm = 0f;
        for (int o = 0; o < octaves; o++) {
            sum += vnoise(x, y, s + o * 31) * amp;
            norm += amp;
            x *= 2.03f;
            y *= 2.03f;
            amp *= 0.5f;
        }
        return sum / norm;
    }

    private static float clamp(float v) { return v < 0f ? 0f : v > 1f ? 1f : v; }

    private static void mixInto(float[] out, float[] a, float[] b, float t) {
        t = clamp(t);
        for (int i = 0; i < 3; i++) out[i] = a[i] + (b[i] - a[i]) * t;
    }

    private static void lift(float[] c, float[] toward, float t) {
        for (int i = 0; i < 3; i++) c[i] += (toward[i] - c[i]) * t;
    }
}

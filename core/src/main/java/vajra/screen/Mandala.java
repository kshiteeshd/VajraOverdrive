package vajra.screen;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import vajra.world.World;

/** THE MANDALAS: what opens round the hull while an astra is held (the invocation), one per astra, drawn as additive light inside an open shape pass.
 *
 *  <p>Every mandala is a LIST OF ELEMENTS (rings, polygons, petals, glyphs...) and the charge builds them ONE BY ONE: each element has its own window of
 *  the charge, and inside it is DRAWN ON - a ring sweeps round from its start, a polygon runs along its edges, petals open one after another - with a
 *  bright head at the tip while it forms. Once an element stands, ENERGY pulses through it: a wave of brightness runs around the rings and out along the
 *  petals (see {@link #flow}), and the main rings carry runners, small lights circling. The whole tightens toward the hull as the charge builds; at a full
 *  charge it flares, and rings of light spill outward.
 *
 *  <p>Styles are the astra kinds ({@code Astra.AKASHA..BRAHMA}) plus {@link #PASHUPATA}, drawn already for the day its astra exists. */
final class Mandala {
    private Mandala() {}

    static final int PASHUPATA = 6;
    private static final float TAU = MathUtils.PI2;

    // palettes: primary, accent
    private static final float[][] PRIMARY = {{0.74f, 0.55f, 1f}, {0.45f, 0.95f, 0.88f}, {1f, 0.34f, 0.12f}, {0.30f, 0.65f, 1f}, {0.88f, 0.64f, 0.22f}, {1f, 0.84f, 0.3f}, {0.86f, 0.88f, 1f}};
    private static final float[][] ACCENT = {{1f, 0.86f, 0.5f}, {0.9f, 1f, 1f}, {1f, 0.86f, 0.5f}, {0.7f, 0.95f, 1f}, {1f, 0.9f, 0.55f}, {1f, 1f, 1f}, {1f, 0.8f, 0.3f}};
    private static final float[] EMBER = {1f, 0.58f, 0.18f}, WHITE = {1f, 1f, 1f}, RED = {1f, 0.2f, 0.2f};

    // scratch for one frame (the renderer is single-threaded)
    private static final float[] PX = new float[16], PY = new float[16];

    /** The whole mandala. `k` is the charge 0..1, `t` the world clock, `full` a charge at its peak. */
    static void draw(ShapeRenderer s, float cx, float cy, float k, float t, boolean full, int style) {
        float R = 128f - 62f * k;
        float pulse = full ? 0.82f + 0.18f * MathUtils.sin(t * 14f) : 1f;
        float base = (0.34f + 0.5f * k) * pulse;
        float[] P = PRIMARY[style], A = ACCENT[style];
        switch (style) {
            case 0: akasha(s, cx, cy, R, k, t, base, P, A); break;
            case 1: vayu(s, cx, cy, R, k, t, base, P, A); break;
            case 2: agni(s, cx, cy, R, k, t, base, P, A); break;
            case 3: jala(s, cx, cy, R, k, t, base, P, A); break;
            case 4: prithvi(s, cx, cy, R, k, t, base, P, A); break;
            case PASHUPATA: pashupata(s, cx, cy, R, k, t, base, P, A); break;
            default: brahma(s, cx, cy, R, k, t, base, P, A); break;
        }
        if (full) { // the full mantra: the whole flares and rings of light spill out
            for (int i = 0; i < 2; i++) {
                float ph = (t * 1.8f + i * 0.5f) % 1f;
                col(s, i == 0 ? P : A, (i == 0 ? 0.6f : 0.35f) * (1f - ph));
                ring(s, cx, cy, R + ph * 56f, R, 1f, 0f, 72, i == 0 ? 2f : 1.2f, 0f, t, 0);
            }
        }
    }

    // ---- the elements: each draws itself on over its window of the charge, then pulses -----------------------------------------------

    /** Progress of an element over its window [a, b] of the charge. */
    private static float st(float k, float a, float b) { return MathUtils.clamp((k - a) / (b - a), 0f, 1f); }

    /** The pulse of energy through a stood element: a wave running outward (rN: 0 at the heart to 1 at the rim) and round (angle a). 0.45 .. 1.4. */
    static float flow(float t, float rN, float a) { return 0.45f + 0.95f * (0.5f + 0.5f * MathUtils.sin(t * 6.5f - rN * 7f + a * 1.5f)); }

    private static void col(ShapeRenderer s, float[] c, float a) { s.setColor(c[0], c[1], c[2], MathUtils.clamp(a, 0f, 1f)); }

    /** A ring drawn on from angle a0 over fraction p; energy pulses through it once whole; `dash` > 0 skips alternate runs of segments. The colour and
     *  alpha are set by the caller through {@link #col} (the energy multiplies the alpha). */
    private static void ring(ShapeRenderer s, float cx, float cy, float r, float R, float p, float a0, int n, float w, float energy, float t, int dash) {
        if (p <= 0f) return;
        float ca = s.getColor().a;
        int m = Math.max(1, Math.round(n * p));
        float rn = r / R;
        float px = cx + r * MathUtils.cos(a0), py = cy + r * MathUtils.sin(a0);
        for (int i = 1; i <= m; i++) {
            float a = a0 + i * TAU / n, x = cx + r * MathUtils.cos(a), y = cy + r * MathUtils.sin(a);
            if (dash == 0 || (i / dash) % 2 == 0) {
                if (energy > 0f) s.getColor().a = MathUtils.clamp(ca * (1f - energy + energy * flow(t, rn, a)), 0f, 1f);
                s.rectLine(px, py, x, y, w);
            }
            px = x;
            py = y;
        }
        s.getColor().a = ca;
        if (p < 1f) head(s, px, py, w);
    }

    /** The bright tip of something still forming. */
    private static void head(ShapeRenderer s, float x, float y, float w) {
        float r = s.getColor().r, g = s.getColor().g, b = s.getColor().b;
        s.setColor(1f, 1f, 1f, 0.9f);
        s.circle(x, y, w * 1.4f + 1.2f, 8);
        s.setColor(r, g, b, 0.45f);
        s.circle(x, y, w * 3f + 3f, 10);
    }

    /** A circling light on a ring: `n` runners, each a small bloom. */
    private static void runners(ShapeRenderer s, float cx, float cy, float r, float t, int n, float speed, float[] c, float a) {
        for (int i = 0; i < n; i++) {
            float ang = t * speed + i * TAU / n, x = cx + r * MathUtils.cos(ang), y = cy + r * MathUtils.sin(ang);
            s.setColor(c[0], c[1], c[2], 0.35f * a);
            s.circle(x, y, 4.2f, 8);
            s.setColor(1f, 1f, 1f, 0.9f * a);
            s.circle(x, y, 1.5f, 6);
        }
    }

    /** A closed polygon drawn on along its edges over fraction p, energy running round it. The vertices are in PX/PY. */
    private static void poly(ShapeRenderer s, int n, float p, float w, float energy, float t, float R, float cx, float cy) {
        if (p <= 0f) return;
        float ca = s.getColor().a;
        float run = p * n;
        for (int i = 0; i < n; i++) {
            float f = MathUtils.clamp(run - i, 0f, 1f);
            if (f <= 0f) break;
            int j = (i + 1) % n;
            float x1 = PX[i] + (PX[j] - PX[i]) * f, y1 = PY[i] + (PY[j] - PY[i]) * f;
            if (energy > 0f) {
                float mx = (PX[i] + x1) * 0.5f - cx, my = (PY[i] + y1) * 0.5f - cy;
                s.getColor().a = MathUtils.clamp(ca * (1f - energy + energy * flow(t, (float) Math.sqrt(mx * mx + my * my) / R, MathUtils.atan2(my, mx))), 0f, 1f);
            }
            s.rectLine(PX[i], PY[i], x1, y1, w);
            if (f < 1f) head(s, x1, y1, w);
        }
        s.getColor().a = ca;
    }

    private static void ngon(float cx, float cy, float r, float rot, int n) {
        for (int i = 0; i < n; i++) {
            PX[i] = cx + r * MathUtils.cos(rot + i * TAU / n);
            PY[i] = cy + r * MathUtils.sin(rot + i * TAU / n);
        }
    }

    /** A lens-shaped petal from r0 to r1 at angle a, `half` radians wide at its fullest, drawn on over fraction p; its energy runs root to tip. */
    private static void petal(ShapeRenderer s, float cx, float cy, float a, float r0, float r1, float half, float p, float w, float energy, float t, float R) {
        if (p <= 0f) return;
        float ca = s.getColor().a;
        final int steps = 9;
        int total = steps * 2;
        int m = Math.max(1, Math.round(total * p));
        float lx = 0, ly = 0;
        for (int i = 0; i <= m; i++) {
            int u = i <= steps ? i : total - i;                        // up the left edge, back down the right
            float tt = (float) Math.pow(u / (float) steps, 0.75f);
            float sp = half * 4f * tt * (1f - tt) * (i <= steps ? -1f : 1f);
            float rr = r0 + (r1 - r0) * (u / (float) steps);
            float x = cx + rr * MathUtils.cos(a + sp), y = cy + rr * MathUtils.sin(a + sp);
            if (i > 0) {
                if (energy > 0f) s.getColor().a = MathUtils.clamp(ca * (1f - energy + energy * flow(t, rr / R, a)), 0f, 1f);
                s.rectLine(lx, ly, x, y, w);
            }
            lx = x;
            ly = y;
        }
        s.getColor().a = ca;
        if (p < 1f) head(s, lx, ly, w);
    }

    /** `n` petals opening one after another over fraction P. */
    private static void petals(ShapeRenderer s, float cx, float cy, int n, float rot, float r0, float r1, float half, float P, float w, float energy, float t, float R) {
        for (int i = 0; i < n; i++) petal(s, cx, cy, rot + i * TAU / n, r0, r1, half, MathUtils.clamp(P * n - i, 0f, 1f), w, energy, t, R);
    }

    /** A small four-point star (the Akasha diamond). */
    private static void star(ShapeRenderer s, float x, float y, float r, float rot, float[] c, float a) {
        s.setColor(c[0], c[1], c[2], a);
        for (int i = 0; i < 4; i++) {
            float a0 = rot + i * MathUtils.PI / 2f;
            s.rectLine(x, y, x + r * MathUtils.cos(a0), y + r * MathUtils.sin(a0), 1.3f);
        }
        s.circle(x, y, r * 0.22f, 6);
    }

    private static void bindu(ShapeRenderer s, float x, float y, float r, float[] c, float a, float t) {
        s.setColor(c[0], c[1], c[2], 0.3f * a);
        s.circle(x, y, r * 2.6f, 14);
        s.setColor(1f, 1f, 1f, a);
        s.circle(x, y, r * (1f + 0.25f * MathUtils.sin(t * 10f)), 10);
    }

    /** Embers (or whatever the style sheds) rising off the circle once the charge is high: `n` small lights drifting outward and up. */
    private static void sparks(ShapeRenderer s, float cx, float cy, float R, float t, int n, float[] c, float a) {
        for (int j = 0; j < n; j++) {
            float ph = (t * 0.55f + j * 0.0713f) % 1f, ang = j * 2.399963f;
            float rr = R * (0.55f + 0.45f * ph);
            s.setColor(c[0], c[1], c[2], 0.9f * (1f - ph) * a);
            s.circle(cx + rr * MathUtils.cos(ang), cy + rr * MathUtils.sin(ang) - 6f * ph, 1.3f * (1f - ph) + 0.4f, 6);
        }
    }

    // ---- the seven ---------------------------------------------------------------------------------------------------------------------

    /** AKASHA, the hunter: the old circle and two turning triangles, now with a ring of eight turning stars, a hexagram's inner ring and a twinkling heart. */
    private static void akasha(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, A, base * 1.3f);
        ring(s, cx, cy, R, R, st(k, 0f, 0.14f), 0f, 72, 1.5f, 0.6f, t, 0);
        col(s, A, base);
        ring(s, cx, cy, R * 0.62f, R, st(k, 0.10f, 0.26f), 1.2f, 56, 1f, 0.6f, t, 0);
        col(s, P, base * 1.7f);
        ngon(cx, cy, R, t * 0.6f, 3);
        poly(s, 3, st(k, 0.2f, 0.42f), 1.6f, 0.7f, t, R, cx, cy);
        ngon(cx, cy, R, MathUtils.PI / 3f - t * 0.6f, 3);
        poly(s, 3, st(k, 0.36f, 0.58f), 1.6f, 0.7f, t, R, cx, cy);
        col(s, A, base * 0.9f);
        ring(s, cx, cy, R * 0.80f, R, st(k, 0.5f, 0.66f), 0f, 64, 0.9f, 0.7f, t, 3);
        float st3 = st(k, 0.6f, 0.82f);
        for (int i = 0; i < 8; i++) {                                    // eight stars, one after another
            float f = MathUtils.clamp(st3 * 8f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = -t * 0.3f + i * TAU / 8f;
            star(s, cx + R * 0.80f * MathUtils.cos(a), cy + R * 0.80f * MathUtils.sin(a), (4.5f + 1.5f * MathUtils.sin(t * 8f + i)) * f, t * 2f + i, i % 2 == 0 ? A : P, base * 1.6f * f);
        }
        col(s, P, base * 1.1f);
        ngon(cx, cy, R * 0.3f, -t * 0.5f, 6);
        poly(s, 6, st(k, 0.7f, 0.88f), 1.1f, 0.7f, t, R, cx, cy);
        float s4 = st(k, 0.82f, 1f);
        if (s4 > 0f) {
            star(s, cx, cy, 12f * s4 * (0.8f + 0.2f * MathUtils.sin(t * 17f)), t, WHITE, 0.9f * s4);
            runners(s, cx, cy, R, t, 3, 1.4f, A, s4);
            sparks(s, cx, cy, R, t, 12, A, s4);
        }
    }

    /** VAYU, the gale: six curved blades spiralling in, a hexagon, a dashed ring of feather ticks, a swirl at the heart. */
    private static void vayu(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, P, base * 1.3f);
        ring(s, cx, cy, R, R, st(k, 0f, 0.14f), 0f, 72, 1.4f, 0.6f, t, 0);
        col(s, A, base);
        ring(s, cx, cy, R * 0.93f, R, st(k, 0.08f, 0.24f), 2f, 72, 0.9f, 0.6f, t, 2);
        // six spiral blades: each an arc whose radius shrinks as it turns, drawn on one after another
        float sp = st(k, 0.2f, 0.62f);
        col(s, P, base * 1.8f);
        for (int b = 0; b < 6; b++) {
            float f = MathUtils.clamp(sp * 6f - b, 0f, 1f);
            if (f <= 0f) break;
            float a0 = t * 0.5f + b * TAU / 6f, px = 0, py = 0;
            int n = Math.max(2, (int) (26 * f));
            for (int i = 0; i <= n; i++) {
                float u = i / 26f, a = a0 + u * 2.1f, r = R * (0.92f - 0.72f * u);
                float x = cx + r * MathUtils.cos(a), y = cy + r * MathUtils.sin(a);
                if (i > 0) {
                    s.getColor().a = MathUtils.clamp(base * 1.8f * (0.4f + 0.9f * (0.5f + 0.5f * MathUtils.sin(t * 7f - u * 7f + b))), 0f, 1f);
                    s.rectLine(px, py, x, y, 1.8f * (1f - u * 0.6f));
                }
                px = x;
                py = y;
            }
            if (f < 1f) head(s, px, py, 1.6f);
        }
        col(s, A, base * 1.1f);
        ngon(cx, cy, R * 0.36f, t * 0.6f, 6);
        poly(s, 6, st(k, 0.55f, 0.74f), 1.2f, 0.7f, t, R, cx, cy);
        float f3 = st(k, 0.66f, 0.86f);
        for (int i = 0; i < 12; i++) {                                   // twelve feather ticks on the middle ring
            float f = MathUtils.clamp(f3 * 12f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = -t * 0.4f + i * TAU / 12f;
            col(s, A, base * 1.4f * f);
            s.rectLine(cx + R * 0.5f * MathUtils.cos(a), cy + R * 0.5f * MathUtils.sin(a), cx + R * (0.5f + 0.1f * f) * MathUtils.cos(a + 0.18f), cy + R * (0.5f + 0.1f * f) * MathUtils.sin(a + 0.18f), 1.2f);
        }
        float s4 = st(k, 0.82f, 1f);
        if (s4 > 0f) {
            for (int i = 0; i < 3; i++) {                                // the swirl: three short arcs turning fast at the heart
                float a = t * 5f + i * TAU / 3f;
                col(s, WHITE, 0.8f * s4);
                s.rectLine(cx + 4f * MathUtils.cos(a), cy + 4f * MathUtils.sin(a), cx + 13f * MathUtils.cos(a + 0.9f), cy + 13f * MathUtils.sin(a + 0.9f), 1.5f);
            }
            bindu(s, cx, cy, 2.6f, P, s4, t);
            runners(s, cx, cy, R, t, 4, 2.4f, A, s4);
        }
    }

    /** AGNI, the pyre: see tools/enemy_art/mandala.py. Ticks, twelve lotus petals and twelve small ones, the fire triangle, the flame glyphs, an eight-petal heart. */
    private static void agni(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, P, base * 1.3f);
        ring(s, cx, cy, R, R, st(k, 0f, 0.12f), 0f, 80, 1.6f, 0.6f, t, 0);
        col(s, A, base * 0.9f);
        ring(s, cx, cy, R * 0.965f, R, st(k, 0.04f, 0.16f), 0.5f, 80, 0.8f, 0.6f, t, 0);
        float tk = st(k, 0.06f, 0.2f);
        for (int i = 0; i < 64; i++) {                                    // sixty-four ticks, drawn on round the rim
            if (i / 64f > tk) break;
            float a = t * 0.12f + i * TAU / 64f, ln = i % 8 == 0 ? 3.4f : 1.8f;
            col(s, i % 8 == 0 ? A : EMBER, base * (i % 8 == 0 ? 1.1f : 0.7f));
            s.rectLine(cx + R * MathUtils.cos(a), cy + R * MathUtils.sin(a), cx + (R + ln) * MathUtils.cos(a), cy + (R + ln) * MathUtils.sin(a), 1f);
        }
        col(s, EMBER, base * 0.7f);
        ring(s, cx, cy, R * 0.9f, R, st(k, 0.12f, 0.24f), -t * 0.25f, 64, 0.9f, 0.5f, t, 2);
        col(s, P, base * 1.5f);
        petals(s, cx, cy, 12, t * 0.18f, R * 0.62f, R * 0.88f, 0.17f, st(k, 0.16f, 0.4f), 1.3f, 0.8f, t, R);
        col(s, A, base);
        petals(s, cx, cy, 12, t * 0.18f + TAU / 24f, R * 0.60f, R * 0.74f, 0.10f, st(k, 0.28f, 0.48f), 1.1f, 0.8f, t, R);
        col(s, A, base * 0.9f);
        ring(s, cx, cy, R * 0.6f, R, st(k, 0.34f, 0.46f), 0f, 56, 1f, 0.6f, t, 0);
        float rot = t * 0.35f;
        col(s, P, base * 1.9f);
        ngon(cx, cy, R * 0.58f, rot - MathUtils.PI / 2f, 3);
        poly(s, 3, st(k, 0.42f, 0.6f), 2.3f, 0.7f, t, R, cx, cy);
        col(s, A, base * 0.8f);
        ngon(cx, cy, R * 0.58f, rot + MathUtils.PI / 2f, 3);
        poly(s, 3, st(k, 0.5f, 0.64f), 1f, 0.7f, t, R, cx, cy);
        col(s, EMBER, base * 1.2f);
        ngon(cx, cy, R * 0.30f, -rot * 1.4f, 6);
        poly(s, 6, st(k, 0.58f, 0.72f), 1.1f, 0.7f, t, R, cx, cy);
        float g = st(k, 0.62f, 0.8f);
        for (int i = 0; i < 8; i++) {                                     // eight flame glyphs on the petal ring, one after another
            float f = MathUtils.clamp(g * 8f - i, 0f, 1f);
            if (f <= 0f) break;
            glyph(s, cx, cy, t * 0.25f + i * TAU / 8f + 0.1f, R * 0.9f, (4.2f + 1.2f * MathUtils.sin(t * 9f + i)) * f, EMBER, base * 1.5f * f);
        }
        col(s, A, base * 1.3f);
        petals(s, cx, cy, 8, -t * 0.3f, R * 0.08f, R * 0.27f, 0.22f, st(k, 0.7f, 0.88f), 1f, 0.8f, t, R);
        float s4 = st(k, 0.84f, 1f);
        if (s4 > 0f) {
            bindu(s, cx, cy, 3.2f, P, s4, t);
            runners(s, cx, cy, R, t, 3, 1.6f, A, s4);
            sparks(s, cx, cy, R, t, 14, A, s4);
        }
    }

    /** A small teardrop flame standing on a ring, point outward. */
    private static void glyph(ShapeRenderer s, float cx, float cy, float a, float r, float size, float[] c, float al) {
        float x = cx + r * MathUtils.cos(a), y = cy + r * MathUtils.sin(a), ca = MathUtils.cos(a), sa = MathUtils.sin(a), nx = -sa, ny = ca;
        s.setColor(c[0], c[1], c[2], al);
        float tx = x + ca * size * 1.5f, ty = y + sa * size * 1.5f, lx = x + nx * size * 0.6f, ly = y + ny * size * 0.6f;
        float rx = x - nx * size * 0.6f, ry = y - ny * size * 0.6f, bx = x - ca * size * 0.4f, by = y - sa * size * 0.4f;
        s.rectLine(tx, ty, lx, ly, 1.1f);
        s.rectLine(lx, ly, bx, by, 1.1f);
        s.rectLine(bx, by, rx, ry, 1.1f);
        s.rectLine(rx, ry, tx, ty, 1.1f);
        s.setColor(1f, 1f, 1f, al * 0.6f);
        s.circle(x, y, size * 0.35f, 6);
    }

    /** JALA, the flood: a rippling rim, three ripple rings, a downward triangle (water), six crescents, six wave petals, a drop at the heart. */
    private static void jala(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        // the rim is a circle ridden by a wave: drawn point by point
        float rimP = st(k, 0f, 0.16f);
        col(s, P, base * 1.4f);
        if (rimP > 0f) {
            int n = 96, m = Math.max(1, Math.round(n * rimP));
            float px = 0, py = 0;
            for (int i = 0; i <= m; i++) {
                float a = i * TAU / n, r = R + 2.6f * MathUtils.sin(a * 12f - t * 5f);
                float x = cx + r * MathUtils.cos(a), y = cy + r * MathUtils.sin(a);
                if (i > 0) {
                    s.getColor().a = MathUtils.clamp(base * 1.4f * flow(t, 1f, a), 0f, 1f);
                    s.rectLine(px, py, x, y, 1.5f);
                }
                px = x;
                py = y;
            }
            if (rimP < 1f) head(s, px, py, 1.5f);
        }
        for (int i = 0; i < 3; i++) {                                     // three ripple rings, each born after the last
            col(s, A, base * (1.0f - 0.2f * i));
            ring(s, cx, cy, R * (0.86f - 0.17f * i), R, st(k, 0.1f + 0.1f * i, 0.24f + 0.1f * i), i * 1.3f, 64, 1f, 0.7f, t, i == 1 ? 2 : 0);
        }
        col(s, P, base * 1.9f);
        ngon(cx, cy, R * 0.62f, MathUtils.PI / 2f, 3);                    // the downward triangle: water
        poly(s, 3, st(k, 0.4f, 0.58f), 2f, 0.7f, t, R, cx, cy);
        col(s, A, base * 0.8f);
        ngon(cx, cy, R * 0.62f, -MathUtils.PI / 2f, 3);
        poly(s, 3, st(k, 0.5f, 0.64f), 0.9f, 0.7f, t, R, cx, cy);
        col(s, P, base * 1.4f);
        petals(s, cx, cy, 6, t * 0.2f + 0.26f, R * 0.30f, R * 0.66f, 0.30f, st(k, 0.56f, 0.78f), 1.2f, 0.8f, t, R);
        float c3 = st(k, 0.66f, 0.86f);
        for (int i = 0; i < 6; i++) {                                     // six crescents on the second ripple
            float f = MathUtils.clamp(c3 * 6f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = -t * 0.35f + i * TAU / 6f;
            col(s, A, base * 1.5f * f);
            float mx = cx + R * 0.69f * MathUtils.cos(a), my = cy + R * 0.69f * MathUtils.sin(a);
            for (int j = 0; j < 8; j++) {
                float a0 = a + 0.9f + j * 0.12f, a1 = a0 + 0.12f;
                float r0 = 8f * f, x0 = mx + r0 * MathUtils.cos(a0 + MathUtils.PI), y0 = my + r0 * MathUtils.sin(a0 + MathUtils.PI);
                float x1 = mx + r0 * MathUtils.cos(a1 + MathUtils.PI), y1 = my + r0 * MathUtils.sin(a1 + MathUtils.PI);
                s.rectLine(x0, y0, x1, y1, 1.2f);
            }
        }
        float s4 = st(k, 0.82f, 1f);
        if (s4 > 0f) {
            col(s, WHITE, 0.9f * s4);                                     // the drop: a circle and a point
            s.circle(cx, cy + 2f, 4f * s4, 12);
            s.triangle(cx - 3.6f * s4, cy + 3f, cx + 3.6f * s4, cy + 3f, cx, cy + 12f * s4);
            runners(s, cx, cy, R, t, 3, -1.6f, A, s4);
        }
    }

    /** PRITHVI, the mountain: a square (the bhupura) with four gates, a turned square, a ring, eight petals, four corner discs, a heavy heart. */
    private static void prithvi(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, P, base * 1.5f);
        ngon(cx, cy, R * 1.04f, MathUtils.PI / 4f, 4);                    // the outer square
        poly(s, 4, st(k, 0f, 0.16f), 2f, 0.6f, t, R, cx, cy);
        float gates = st(k, 0.1f, 0.26f);
        for (int i = 0; i < 4; i++) {                                     // a gate stands in the middle of each side
            float f = MathUtils.clamp(gates * 4f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = i * MathUtils.PI / 2f, r1 = R * 1.04f * 0.7071f, ca = MathUtils.cos(a), sa = MathUtils.sin(a);
            col(s, A, base * 1.5f * f);
            s.rectLine(cx + ca * r1, cy + sa * r1, cx + ca * (r1 + 14f * f), cy + sa * (r1 + 14f * f), 1.8f);
            s.rectLine(cx + ca * (r1 + 14f * f) - sa * 9f, cy + sa * (r1 + 14f * f) + ca * 9f, cx + ca * (r1 + 14f * f) + sa * 9f, cy + sa * (r1 + 14f * f) - ca * 9f, 1.8f);
        }
        col(s, A, base);
        ring(s, cx, cy, R * 0.80f, R, st(k, 0.2f, 0.34f), 0f, 72, 1.2f, 0.6f, t, 0);
        col(s, P, base * 1.4f);
        ngon(cx, cy, R * 0.80f, 0f, 4);                                   // the turned square, inside the ring: an eight-pointed star with the first
        poly(s, 4, st(k, 0.3f, 0.48f), 1.6f, 0.7f, t, R, cx, cy);
        col(s, A, base * 1.1f);
        petals(s, cx, cy, 8, t * 0.15f, R * 0.14f, R * 0.66f, 0.2f, st(k, 0.44f, 0.7f), 1.2f, 0.8f, t, R);
        float d = st(k, 0.62f, 0.82f);
        for (int i = 0; i < 4; i++) {                                     // a disc in each corner of the great square
            float f = MathUtils.clamp(d * 4f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = MathUtils.PI / 4f + i * MathUtils.PI / 2f + 0f;
            float x = cx + R * 1.04f * MathUtils.cos(a), y = cy + R * 1.04f * MathUtils.sin(a);
            col(s, P, base * 1.2f * f);
            s.circle(x, y, 5.5f * f, 10);
            col(s, WHITE, base * 1.2f * f);
            s.circle(x, y, 1.8f * f, 6);
        }
        col(s, P, base * 1.5f);
        ngon(cx, cy, R * 0.22f, MathUtils.PI / 4f, 4);
        poly(s, 4, st(k, 0.72f, 0.88f), 1.5f, 0.7f, t, R, cx, cy);
        float s4 = st(k, 0.84f, 1f);
        if (s4 > 0f) {
            s.setColor(P[0], P[1], P[2], 0.9f * s4);
            s.rect(cx - 5f * s4, cy - 5f * s4, 10f * s4, 10f * s4);
            bindu(s, cx, cy, 2f, A, s4, t);
            runners(s, cx, cy, R * 0.8f, t, 4, 1.0f, A, s4);
        }
    }

    /** PASHUPATA, the third eye: an almond eye round the hull with its iris, a trident standing in it, a ring of rays, a small red third eye over the brow. */
    private static void pashupata(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, P, base * 1.3f);
        ring(s, cx, cy, R, R, st(k, 0f, 0.14f), 0f, 72, 1.4f, 0.6f, t, 0);
        col(s, A, base);
        ring(s, cx, cy, R * 0.92f, R, st(k, 0.06f, 0.2f), 1f, 72, 0.9f, 0.6f, t, 3);
        float eye = st(k, 0.18f, 0.5f);
        if (eye > 0f) {                                                    // the almond: two arcs meeting at the corners, drawn from the corners inward
            col(s, P, base * 1.7f);
            int n = 36, m = Math.max(1, Math.round(n * eye));
            for (int side = -1; side <= 1; side += 2) {
                float px = 0, py = 0;
                for (int i = 0; i <= m; i++) {
                    float u = i / (float) n, x = cx - R * 0.78f + 2f * R * 0.78f * u, y = cy + side * R * 0.34f * 4f * u * (1f - u);
                    if (i > 0) {
                        s.getColor().a = MathUtils.clamp(base * 1.7f * flow(t, Math.abs(x - cx) / R, side), 0f, 1f);
                        s.rectLine(px, py, x, y, 1.6f);
                    }
                    px = x;
                    py = y;
                }
                if (eye < 1f) head(s, px, py, 1.6f);
            }
        }
        col(s, A, base * 1.3f);
        ring(s, cx, cy, R * 0.26f, R, st(k, 0.46f, 0.62f), 0f, 40, 1.4f, 0.6f, t, 0);   // the iris
        float rays = st(k, 0.54f, 0.78f);
        for (int i = 0; i < 16; i++) {                                    // sixteen rays round the eye
            float f = MathUtils.clamp(rays * 16f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = t * 0.2f + i * TAU / 16f;
            col(s, i % 2 == 0 ? A : P, base * 1.3f * f);
            float r0 = R * 0.86f, r1 = R * (0.86f + (i % 2 == 0 ? 0.1f : 0.05f) * f);
            s.rectLine(cx + r0 * MathUtils.cos(a), cy + r0 * MathUtils.sin(a), cx + r1 * MathUtils.cos(a), cy + r1 * MathUtils.sin(a), 1.2f);
        }
        // the trident, upright in the pupil
        float tr = st(k, 0.66f, 0.88f);
        if (tr > 0f) {
            col(s, A, base * 1.8f * tr);
            float len = R * 0.62f * tr;
            s.rectLine(cx, cy - len * 0.55f, cx, cy + len * 0.55f, 1.8f);                                       // the shaft
            s.rectLine(cx - len * 0.22f, cy + len * 0.28f, cx + len * 0.22f, cy + len * 0.28f, 1.6f);         // the cross-guard
            s.rectLine(cx - len * 0.22f, cy + len * 0.28f, cx - len * 0.22f, cy + len * 0.55f, 1.6f);        // the left prong
            s.rectLine(cx + len * 0.22f, cy + len * 0.28f, cx + len * 0.22f, cy + len * 0.55f, 1.6f);        // the right prong
            s.triangle(cx - 3f * tr, cy + len * 0.55f, cx + 3f * tr, cy + len * 0.55f, cx, cy + len * 0.72f);   // the centre point
        }
        float s4 = st(k, 0.84f, 1f);
        if (s4 > 0f) {                                                     // the third eye, over the brow, opening red
            float ey = cy + R * 0.62f;
            col(s, RED, 0.8f * s4);
            s.circle(cx, ey, 5f * s4 * (0.85f + 0.15f * MathUtils.sin(t * 12f)), 10);
            col(s, WHITE, 0.9f * s4);
            s.circle(cx, ey, 1.8f * s4, 6);
            runners(s, cx, cy, R, t, 3, 1.5f, A, s4);
        }
    }

    /** BRAHMA, the last arrow: a golden ring with five gems, a pentagon and its star, five petals each in an element's own colour, four small faces. */
    private static void brahma(ShapeRenderer s, float cx, float cy, float R, float k, float t, float base, float[] P, float[] A) {
        col(s, P, base * 1.4f);
        ring(s, cx, cy, R, R, st(k, 0f, 0.12f), 0f, 80, 1.6f, 0.6f, t, 0);
        col(s, A, base);
        ring(s, cx, cy, R * 0.94f, R, st(k, 0.05f, 0.16f), 0.4f, 80, 0.8f, 0.6f, t, 0);
        float gem = st(k, 0.1f, 0.3f);
        for (int i = 0; i < 5; i++) {                                     // five gems on the rim, each an element's colour
            float f = MathUtils.clamp(gem * 5f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = -MathUtils.PI / 2f + i * TAU / 5f + t * 0.1f;
            float[] e = World.TATVA_RGB[i];
            col(s, e, 0.5f * f * base * 2f);
            s.circle(cx + R * MathUtils.cos(a), cy + R * MathUtils.sin(a), 7f * f, 12);
            col(s, WHITE, 0.9f * f);
            s.circle(cx + R * MathUtils.cos(a), cy + R * MathUtils.sin(a), 2.3f * f, 8);
        }
        float pet = st(k, 0.22f, 0.62f);
        for (int i = 0; i < 5; i++) {                                     // five great petals, one per element, opening in turn
            float f = MathUtils.clamp(pet * 5f - i, 0f, 1f);
            if (f <= 0f) break;
            col(s, World.TATVA_RGB[i], base * 1.9f);
            petal(s, cx, cy, -MathUtils.PI / 2f + i * TAU / 5f + t * 0.1f, R * 0.18f, R * 0.88f, 0.34f, f, 1.5f, 0.8f, t, R);
        }
        col(s, A, base * 1.2f);
        ngon(cx, cy, R * 0.62f, -MathUtils.PI / 2f + t * 0.1f, 5);
        poly(s, 5, st(k, 0.5f, 0.68f), 1.2f, 0.7f, t, R, cx, cy);
        col(s, P, base * 1.5f);
        for (int i = 0; i < 5; i++) { // the star: each point to the point two on
            PX[i] = cx + R * 0.62f * MathUtils.cos(-MathUtils.PI / 2f + t * 0.1f + (i * 2 % 5) * TAU / 5f);
            PY[i] = cy + R * 0.62f * MathUtils.sin(-MathUtils.PI / 2f + t * 0.1f + (i * 2 % 5) * TAU / 5f);
        }
        poly(s, 5, st(k, 0.6f, 0.78f), 1.3f, 0.7f, t, R, cx, cy);
        float fc = st(k, 0.74f, 0.9f);
        for (int i = 0; i < 4; i++) {                                     // the four faces: four small rings at the quarters
            float f = MathUtils.clamp(fc * 4f - i, 0f, 1f);
            if (f <= 0f) break;
            float a = i * MathUtils.PI / 2f - t * 0.2f, x = cx + R * 0.36f * MathUtils.cos(a), y = cy + R * 0.36f * MathUtils.sin(a);
            col(s, A, base * 1.5f * f);
            ring(s, x, y, 6f * f, R, 1f, 0f, 16, 1.1f, 0f, t, 0);
            col(s, WHITE, base * 1.2f * f);
            s.circle(x, y, 1.4f * f, 6);
        }
        float s4 = st(k, 0.86f, 1f);
        if (s4 > 0f) {
            bindu(s, cx, cy, 3.4f, P, s4, t);
            runners(s, cx, cy, R, t, 5, 1.2f, A, s4);
            sparks(s, cx, cy, R, t, 16, A, s4);
        }
    }
}

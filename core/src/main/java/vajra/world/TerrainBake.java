package vajra.world;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** THE WHOLE LAND AS ONE HEIGHT MAP, baked once: every texel (4 world px) holds a colour and a height (0..255 world units
 *  above the ground), so the raycaster can march a ray through it and draw terrain with real parallax and real occlusion.
 *  Heights are CUT PLANES: a tree is three stacked discs, a pillar a plinth, a shaft and a capital, a boulder five
 *  steps, a mountain a terraced stack - flat tops, vertical sides, which the ray hits and shades as 3D without a single model.
 *
 *  <p>Layout: 4 bytes a texel, R G B then height; row 0 is world y = 0 (bottom), column 0 is x = 0. About 27 MB for the world,
 *  baked on all cores in well under a second. Pure: no GL here. */
public final class TerrainBake {
    public static final int TEXEL = GroundMap.TEXEL;

    public final int cols, rows;
    public final byte[] rgba;
    private final float worldW, worldH;

    public TerrainBake(GroundMap map, PropField props, float[][] boulders, float worldW, float worldH) {
        this.worldW = worldW;
        this.worldH = worldH;
        this.cols = (int) Math.ceil(worldW / TEXEL);
        this.rows = (int) Math.ceil(worldH / TEXEL);
        this.rgba = new byte[cols * rows * 4];
        IntStream.range(0, rows).parallel().forEach(ty -> {
            for (int tx = 0; tx < cols; tx++) {
                int t = map.texel(tx, ty), k = (ty * cols + tx) * 4;
                rgba[k] = (byte) (t >> 16);
                rgba[k + 1] = (byte) (t >> 8);
                rgba[k + 2] = (byte) t;
                rgba[k + 3] = (byte) (t >>> 24);
            }
        });
        List<PropField.Prop> list = new ArrayList<>();
        final float span = 512f;
        for (float y = 0; y < worldH; y += span)
            for (float x = 0; x < worldW; x += span) {
                list.clear();
                props.chunk(x, y, span, list);
                for (PropField.Prop p : list) stamp(p);
            }
        for (float[] b : boulders) boulder(b);
    }

    // ---- reading ----------------------------------------------------------------------------------

    /** The height at a world point, units. */
    public int height(float wx, float wy) {
        int tx = Math.max(0, Math.min(cols - 1, (int) (wx / TEXEL))), ty = Math.max(0, Math.min(rows - 1, (int) (wy / TEXEL)));
        return rgba[(ty * cols + tx) * 4 + 3] & 0xFF;
    }

    /** The colour at a world point as 0xRRGGBB. */
    public int colour(float wx, float wy) {
        int tx = Math.max(0, Math.min(cols - 1, (int) (wx / TEXEL))), ty = Math.max(0, Math.min(rows - 1, (int) (wy / TEXEL)));
        int k = (ty * cols + tx) * 4;
        return ((rgba[k] & 0xFF) << 16) | ((rgba[k + 1] & 0xFF) << 8) | (rgba[k + 2] & 0xFF);
    }

    // ---- stamping the things that stand on the land ----------------------------------------------

    /** A flat-topped disc: wherever it is higher than what is there, it writes its height and colour. */
    private void disc(float cx, float cy, float r, int h, float cr, float cg, float cb, float edge) {
        int x0 = Math.max(0, (int) ((cx - r) / TEXEL)), x1 = Math.min(cols - 1, (int) ((cx + r) / TEXEL));
        int y0 = Math.max(0, (int) ((cy - r) / TEXEL)), y1 = Math.min(rows - 1, (int) ((cy + r) / TEXEL));
        for (int ty = y0; ty <= y1; ty++)
            for (int tx = x0; tx <= x1; tx++) {
                float dx = (tx + 0.5f) * TEXEL - cx, dy = (ty + 0.5f) * TEXEL - cy, d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d > r) continue;
                int k = (ty * cols + tx) * 4;
                if ((rgba[k + 3] & 0xFF) > h) continue;
                float shade = 1f - edge * (d / r) * (d / r);                              // the rim of a plane sits a little darker
                rgba[k] = (byte) q(cr * shade);
                rgba[k + 1] = (byte) q(cg * shade);
                rgba[k + 2] = (byte) q(cb * shade);
                rgba[k + 3] = (byte) h;
            }
    }

    private static int q(float v) { return Math.max(0, Math.min(255, (int) (v * 255f + 0.5f))); }

    private static float mix(float a, float b, float t) { return a + (b - a) * t; }

    private void stamp(PropField.Prop p) {
        Biome b = Biome.of(p.band);
        float var = 0.9f + 0.2f * ((p.seed >>> 3) & 7) / 7f;
        switch (p.kind) {
            case PropField.TREE: { // three planes: a wide low crown, a narrower middle, a bright crest
                float R = 11f + 18f * p.size, H = 50f + 55f * p.size;
                float[] d = b.canopyDark, l = b.canopyLight;
                for (int i = 0; i < 3; i++) {
                    float f = i / 2f, rr = R * (i == 0 ? 1f : i == 1 ? 0.72f : 0.42f);
                    float t = (0.12f + 0.78f * f) * var;
                    disc(p.x, p.y, rr, (int) (H * (0.55f + 0.22f * i)), mix(d[0], l[0], t), mix(d[1], l[1], t), mix(d[2], l[2], t), 0.3f);
                }
                break;
            }
            case PropField.MUSHROOM: { // a stem hidden under the cap, a lavender cap in two planes
                float H = 30f + 70f * p.size, R = 10f + 10f * p.size;
                disc(p.x, p.y, R, (int) (H * 0.85f), 0.34f, 0.26f, 0.64f, 0.35f);
                disc(p.x, p.y, R * 0.55f, (int) H, 0.62f, 0.50f, 0.88f, 0.2f);
                break;
            }
            case PropField.LAMP: { // a gold post and a burning head
                disc(p.x, p.y, 5f, 100, 0.55f, 0.43f, 0.2f, 0f);
                disc(p.x, p.y, 9f, 118, 1f, 0.9f, 0.5f, 0f);
                break;
            }
            default: { // a pillar: plinth, shaft, capital, a bright crown
                float R = 15f;
                float[] c = {0.68f, 0.62f, 0.86f};
                disc(p.x, p.y, R * 1.4f, 14, 0.5f, 0.45f, 0.68f, 0.25f);
                disc(p.x, p.y, R, 170, c[0] * 0.9f, c[1] * 0.9f, c[2] * 0.9f, 0.15f);
                disc(p.x, p.y, R * 1.5f, 176, c[0], c[1], c[2], 0.2f);
                disc(p.x, p.y, R * 0.9f, 182, 0.95f, 0.82f, 0.62f, 0.1f);
                break;
            }
        }
    }

    /** A boulder: five steps, each smaller and higher, off-centre by a little so it is lumpy, in stone going paler as it rises. */
    private void boulder(float[] b) {
        float r = b[2], s = b[3], k = r / 50f;
        float[] radius = {1f, 0.86f, 0.68f, 0.5f, 0.3f};
        float[] height = {18f, 34f, 50f, 62f, 70f};
        for (int i = 0; i < 5; i++) {
            float ox = (GroundMap.rnd(i, 1, (int) (s * 9999)) - 0.5f) * r * 0.22f * i;
            float oy = (GroundMap.rnd(i, 2, (int) (s * 9999)) - 0.5f) * r * 0.22f * i;
            float t = i / 4f, warm = 0.04f * (s - 0.5f);
            disc(b[0] + ox, b[1] + oy, r * radius[i], (int) (height[i] * Math.max(0.7f, k)), 0.36f + 0.34f * t + warm, 0.34f + 0.33f * t, 0.31f + 0.31f * t, 0.35f);
        }
    }
}

package vajra.screen;

import vajra.world.GroundMap;
import vajra.world.World;

/** The whole world as a thumbnail, one sample per {@link #STEP} world px, baked once from {@link GroundMap}: what the
 *  minimaps are drawn from, so the dial shows the real land (meadow, white soil, mirror-wood, rings, ribs, court) and
 *  not a diagram of rock. About 100k samples; baking takes tens of milliseconds. */
final class WorldThumb {
    static final int STEP = 32;
    private static WorldThumb cached;

    private final int cols, rows;
    private final float[] rgb;
    private final boolean[] rock;
    private final boolean walled;

    private WorldThumb(World w) {
        cols = (int) Math.ceil(w.width / STEP) + 1;
        rows = (int) Math.ceil(w.height / STEP) + 1;
        rgb = new float[cols * rows * 3];
        rock = new boolean[cols * rows];
        walled = w.walled;
        GroundMap map = new GroundMap(GroundMap.SEED, w.width, w.height, w.regionStart);
        for (int j = 0; j < rows; j++)
            for (int i = 0; i < cols; i++) {
                int c = map.argb((i * STEP) / GroundMap.TEXEL, (j * STEP) / GroundMap.TEXEL);
                int k = (j * cols + i) * 3;
                rgb[k] = ((c >> 16) & 0xFF) / 255f;
                rgb[k + 1] = ((c >> 8) & 0xFF) / 255f;
                rgb[k + 2] = (c & 0xFF) / 255f;
                rock[j * cols + i] = w.walled && w.blockedPx(i * STEP, j * STEP);   // the wall tiles and the boulders, once
            }
    }

    /** The thumbnail for this world (baked on first use, kept while the world's size and bands stay the same). */
    static synchronized WorldThumb of(World w) {
        if (cached == null || cached.walled != w.walled || cached.cols != (int) Math.ceil(w.width / STEP) + 1 || cached.rows != (int) Math.ceil(w.height / STEP) + 1)
            cached = new WorldThumb(w);
        return cached;
    }

    /** True where the baked world has wall (or a boulder) at a world point. */
    boolean rock(float wx, float wy) {
        int i = Math.max(0, Math.min(cols - 1, Math.round(wx / STEP))), j = Math.max(0, Math.min(rows - 1, Math.round(wy / STEP)));
        return rock[j * cols + i];
    }

    /** The land's colour at a world point, into out[0..2]; points outside the world take the nearest edge. */
    void sample(float wx, float wy, float[] out) {
        int i = Math.max(0, Math.min(cols - 1, Math.round(wx / STEP))), j = Math.max(0, Math.min(rows - 1, Math.round(wy / STEP)));
        int k = (j * cols + i) * 3;
        out[0] = rgb[k];
        out[1] = rgb[k + 1];
        out[2] = rgb[k + 2];
    }
}

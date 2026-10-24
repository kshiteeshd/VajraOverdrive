package vajra.world;

import java.util.ArrayList;
import java.util.List;

/** THE BOUNDARY BETWEEN BHU LOKA AND ATALA: a belt of rocks and boulders across the whole width of the world with one pass
 *  through it (where the grand gate stood). Pure and seeded; the same list is the collision (World) and the picture
 *  (PropField), so what you see is what stops you. */
public final class Boundary {
    private Boundary() {}

    /** Half the width of the pass, px: wider than the old grand door (448), so the hull has room to spare. */
    public static final float PASS_HALF = 380f;
    /** The belt's rows lie within this many px of the boundary y. */
    public static final float DEPTH = 120f;

    /** Boulders as {x, y, radius, seed}, across [0, width] with a pass at the centre. */
    public static float[][] belt(float width, float y) {
        List<float[]> out = new ArrayList<>();
        float cx = width / 2f;
        int seed = 0xB0D1;
        for (int row = 0; row < 3; row++) {
            float rowY = y - 70f + row * 70f;
            float x = 20f + 25f * GroundMap.rnd(row, 0, seed);
            for (int i = 0; x < width; i++) {
                float r = 34f + 34f * GroundMap.rnd(i, row, seed + 1);
                float jx = x + (GroundMap.rnd(i, row, seed + 2) - 0.5f) * 30f;
                float jy = rowY + (GroundMap.rnd(i, row, seed + 3) - 0.5f) * 50f;
                // the pass: nothing inside it, and the rocks beside it shrink away from the edge
                if (Math.abs(jx - cx) > PASS_HALF + r) out.add(new float[] {jx, jy, r, GroundMap.rnd(i, row, seed + 4)});
                x += r * 1.45f + 6f;
            }
        }
        return out.toArray(new float[0][]);
    }

    /** True if (x, y) is inside any boulder grown by `pad`. */
    public static boolean hit(float[][] belt, float x, float y, float pad, float beltY) {
        if (Math.abs(y - beltY) > DEPTH + 120f) return false;
        for (float[] b : belt) {
            float dx = x - b[0], dy = y - b[1], r = b[2] * 0.85f + pad;
            if (dx * dx + dy * dy < r * r) return true;
        }
        return false;
    }
}

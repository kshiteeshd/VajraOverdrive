package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;
import vajra.world.GroundMap;
import vajra.world.World;

/** THE MIRAGE WOOD's living things (docs/ATALA_PLAN.md, step C), every one pinned to a LAYER of the stack so it slides at that layer's
 *  speed: lantern-blossoms (a stem and a glowing bulb that pulses in a wave running up the wood), giant mushrooms (a pale stem under
 *  a lavender cap lit from beneath), and the wisps - a swarm that gathers round the hull and scatters when it boosts. All of them
 *  are soft glows from one radial texture plus a white pixel for stems; none is a model. Sites are a pure hash of the layer and the
 *  cell, so the wood is the same every visit. */
final class MirageFx {
    /** The wood's band in world y (the sites live inside it; the caller fades them with the band's weight). */
    static final float Y0 = 4100f, Y1 = 7600f;
    private static final float CELL = 430f;

    private final Texture glow;
    private final Texture white;

    MirageFx(Texture glow) {
        this.glow = glow;
        Pixmap p = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        white = new Texture(p);
        p.dispose();
    }

    // ---- sites ----------------------------------------------------------------------------------

    /** The site in cell (i, j) of a layer: {x, y, kind (0 blossom, 1 mushroom), size 0..1, phase}, or null if the cell is bare. */
    static float[] site(int layer, int i, int j) {
        int seed = 0x3A9E + layer * 131;
        if (GroundMap.rnd(i, j, seed) > 0.42f) return null;
        float x = (i + 0.15f + 0.7f * GroundMap.rnd(i, j, seed + 1)) * CELL, y = (j + 0.15f + 0.7f * GroundMap.rnd(i, j, seed + 2)) * CELL;
        if (y < Y0 || y > Y1) return null;
        float kind = GroundMap.rnd(i, j, seed + 3) < (layer < 2 ? 0.7f : 0.4f) ? 0f : 1f;
        return new float[] {x, y, kind, GroundMap.rnd(i, j, seed + 4), GroundMap.rnd(i, j, seed + 5) * 6.28f};
    }

    /** The blossoms and mushrooms of one layer in view, drawn into an open batch (the layer's projection is set). `weight` is the
     *  band's strength here (0..1), `alpha` the layer's own fade. */
    void layer(SpriteBatch batch, float l, float b, float w, float h, int layer, float time, float weight, float alpha) {
        if (weight * alpha <= 0.02f) return;
        float dim = 1f - 0.05f * layer;
        int i0 = (int) Math.floor(l / CELL) - 1, i1 = (int) Math.floor((l + w) / CELL) + 1;
        int j0 = (int) Math.floor((b - 120f) / CELL), j1 = (int) Math.floor((b + h) / CELL) + 1;
        for (int j = j0; j <= j1; j++)
            for (int i = i0; i <= i1; i++) {
                float[] s = site(layer, i, j);
                if (s == null) continue;
                if (s[2] < 0.5f) blossom(batch, s, time, weight * alpha * dim);
                else mushroom(batch, s, time, weight * alpha * dim);
            }
    }

    /** A lantern-blossom: a thin dark stem, a bulb of light whose brightness rides a wave travelling up the wood, a hot core. */
    private void blossom(SpriteBatch batch, float[] s, float time, float a) {
        float x = s[0], y = s[1], hgt = 70f + 60f * s[3];
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 1.6f - y * 0.004f - x * 0.002f + s[4]);   // the wave
        batch.setColor(0.04f, 0.16f, 0.17f, 0.85f * a);
        batch.draw(white, x - 2f, y, 4f, hgt);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        boolean pink = s[4] > 3.1f;
        float r = pink ? 1f : 0.45f, g = pink ? 0.55f : 0.95f, bl = pink ? 0.8f : 0.9f;
        float size = 60f + 26f * pulse;
        batch.setColor(r, g, bl, 0.30f * (0.4f + 0.6f * pulse) * a);
        batch.draw(glow, x - size, y + hgt - size, size * 2f, size * 2f);
        batch.setColor(r, g, bl, 0.75f * (0.5f + 0.5f * pulse) * a);
        batch.draw(glow, x - 15f, y + hgt - 15f, 30f, 30f);
        batch.setColor(1f, 1f, 0.95f, 0.9f * pulse * a);
        batch.draw(glow, x - 7f, y + hgt - 7f, 14f, 14f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** A giant mushroom: a pale stem, a lavender cap, a bright glow under the cap. */
    private void mushroom(SpriteBatch batch, float[] s, float time, float a) {
        float x = s[0], y = s[1], hgt = 34f + 40f * s[3], cap = 56f + 44f * s[3];
        float breathe = 0.85f + 0.15f * (float) Math.sin(time * 1.1f + s[4]);
        batch.setColor(0.82f, 0.80f, 0.86f, 0.9f * a);
        batch.draw(white, x - 6f, y, 12f, hgt);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(0.70f, 0.52f, 1f, 0.34f * breathe * a);                    // the light from beneath
        batch.draw(glow, x - cap * 1.3f, y + hgt - cap * 0.9f, cap * 2.6f, cap * 1.8f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(0.38f, 0.28f, 0.66f, 0.95f * a);                           // the cap
        batch.draw(glow, x - cap, y + hgt - cap * 0.34f, cap * 2f, cap * 0.8f);
        batch.setColor(0.72f, 0.60f, 0.96f, 0.85f * a);
        batch.draw(glow, x - cap * 0.55f, y + hgt - cap * 0.1f, cap * 1.1f, cap * 0.42f);
    }

    // ---- the wisps --------------------------------------------------------------------------------

    static final int WISPS = 40;

    /** The swarm on one layer: WISPS lights circling the hull's own spot on that layer, gathered close while the hull flies and thrown
     *  out wide when it boosts (`scatter` 0..1). */
    void wisps(SpriteBatch batch, int layer, float shipX, float shipY, float time, float weight, float alpha, float scatter) {
        if (weight * alpha <= 0.02f) return;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < WISPS; i++) {
            float h1 = GroundMap.rnd(i, layer, 0x77), h2 = GroundMap.rnd(i, layer, 0x78), h3 = GroundMap.rnd(i, layer, 0x79);
            float radius = (36f + 230f * h1 * h1) * (1f + 2.6f * scatter);
            float ang = h2 * 6.2832f + time * (0.5f + 0.9f * h3) * (i % 2 == 0 ? 1f : -1f);
            float bob = 14f * (float) Math.sin(time * 1.3f + i);
            float x = shipX + (float) Math.cos(ang) * radius, y = shipY + (float) Math.sin(ang) * radius * 0.72f + bob;
            float tw = 0.45f + 0.55f * (0.5f + 0.5f * (float) Math.sin(time * (1.1f + 1.7f * h3) + i * 2.3f));
            boolean teal = i % 3 != 0;
            float size = 14f + 20f * h3;
            batch.setColor(teal ? 0.5f : 0.82f, teal ? 0.96f : 0.72f, teal ? 0.92f : 1f, 0.7f * tw * weight * alpha * (1f - 0.5f * scatter));
            batch.draw(glow, x - size / 2f, y - size / 2f, size, size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---- light ---------------------------------------------------------------------------------------

    /** Light pools for the blossoms and mushrooms on the PLAY layer near the view, into the lightmap list ({x, y, radius, r, g, b}). They
     *  are the wood's light: nothing else down here is lit. */
    void glows(World world, float l, float b, float w, float h, float time, float weight, List<float[]> out) {
        if (weight <= 0.05f) return;
        int i0 = (int) Math.floor(l / CELL) - 1, i1 = (int) Math.floor((l + w) / CELL) + 1;
        int j0 = (int) Math.floor((b - 120f) / CELL), j1 = (int) Math.floor((b + h) / CELL) + 1;
        for (int j = j0; j <= j1; j++)
            for (int i = i0; i <= i1; i++) {
                float[] s = site(0, i, j);
                if (s == null || world.blockedPx(s[0], s[1])) continue;
                boolean blossom = s[2] < 0.5f;
                float pulse = blossom ? 0.5f + 0.5f * (float) Math.sin(time * 1.6f - s[1] * 0.004f - s[0] * 0.002f + s[4]) : 0.85f + 0.15f * (float) Math.sin(time * 1.1f + s[4]);
                float hgt = blossom ? 70f + 60f * s[3] : 34f + 40f * s[3];
                float k = (blossom ? 0.8f : 0.7f) * weight * (0.45f + 0.55f * pulse);
                out.add(new float[] {s[0], s[1] + hgt, blossom ? 170f : 150f, (blossom ? 0.5f : 0.62f) * k, (blossom ? 0.95f : 0.45f) * k, (blossom ? 0.9f : 1f) * k});
            }
    }

    void dispose() { white.dispose(); }
}

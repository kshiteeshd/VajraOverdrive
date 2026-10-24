package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import vajra.world.Checkpoint;

/** THE DEEPA-STAMBHA: a checkpoint is a stone lamp-altar that is always DOING something. Four little sprite sheets painted pixel by pixel at
 *  start-up (no art files):
 *  <ul>
 *   <li><b>the altar</b>, 8 frames: a cut-stone octagon whose gold chakra ring TURNS - its eight spokes of plain stone walk round the inlay,
 *       fast while the lamp is cold, slowly once it burns - eight gold petals and a dark bowl;</li>
 *   <li><b>the flame-seed</b>, 6 frames: while the lamp is COLD a small winged flame-bud flutters round the empty bowl on a figure-of-eight,
 *       its two petal-wings opening and closing;</li>
 *   <li><b>the flame</b>: once the lamp is LIT the fire sheet's brazier (32 x 32, from above) burns in the bowl;</li>
 *   <li><b>the diya</b>, 4 frames: four tiny lamp-bearers circle the lit altar, each flame flickering on its own.</li>
 *  </ul>
 *  Soft glows add the light round them. A lit lamp is where you wake if you fall; it is also the world's light (the renderer throws a pool
 *  from it and a glow down the stack behind it). */
public class CheckpointImages {
    private static final int N = 64, SPARKS = 12;
    static final float SIZE = 72f;

    private final Texture[] altar = new Texture[8], seed = new Texture[6], diya = new Texture[4];
    private final FireSheet fire = new FireSheet();
    private final Texture glow, white;

    public CheckpointImages() {
        this(0);
    }

    /** `skin` 1 paints the same lamp-altar in Vitala's palette (basalt and molten gold). */
    public CheckpointImages(int skin) {
        TileImages.paintSkin = skin;
        for (int i = 0; i < altar.length; i++) altar[i] = paintAltar(i);
        for (int i = 0; i < seed.length; i++) seed[i] = paintSeed(i);
        for (int i = 0; i < diya.length; i++) diya[i] = paintDiya(i);
        TileImages.paintSkin = 0;
        glow = radial();
        Pixmap p = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        white = new Texture(p);
        p.dispose();
    }

    // ---- the altar, pixel by pixel --------------------------------------------------------------

    private static Texture paintAltar(int frame) {
        Pixmap p = new Pixmap(N, N, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                float dx = x + 0.5f - N / 2f, dy = N / 2f - (y + 0.5f);           // y up, for the light
                float ax = Math.abs(dx), ay = Math.abs(dy);
                float m = Math.max(Math.max(ax, ay), (ax + ay) * 0.7071f);       // distance in an octagon
                float r = (float) Math.hypot(dx, dy), ang = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (m > 30f) continue;
                float lit = clamp(0.5f + (-dx + dy) / 70f);                       // lit from the upper left
                float[] c;
                if (m > 28.2f) c = new float[] {0.16f, 0.13f, 0.10f};            // the outline
                else if (m > 24.5f) {                                             // the bevelled edge: pale on the lit side, dark away from it
                    float k = 0.55f + 0.5f * lit;
                    c = new float[] {0.58f * k, 0.53f * k, 0.43f * k};
                } else {                                                          // the top face: ivory stone, flecked
                    float fleck = (hash(x, y) - 0.5f) * 0.06f;
                    c = new float[] {0.70f + fleck, 0.66f + fleck, 0.55f + fleck};
                }
                if (m <= 24.5f) {
                    if (r >= 15.5f && r <= 18.2f) { // the gold chakra ring: its eight stone spokes walk round it, one frame at a time
                        float turned = ((ang + frame * 5.625f) % 45f + 45f) % 45f;
                        float a8 = Math.abs(turned - 22.5f);
                        if (a8 < 18f) c = new float[] {0.95f, 0.77f, 0.28f};
                        else if (r < 16.2f || r > 17.5f) c = new float[] {0.48f, 0.38f, 0.16f};
                    }
                    // eight gold petals between the inlay and the edge, breathing: each swells and shrinks a little, round the ring in turn
                    for (int k = 0; k < 8; k++) {
                        float a = (float) Math.toRadians(k * 45f + 22.5f);
                        float u = dx - (float) Math.cos(a) * 21.2f, v = dy - (float) Math.sin(a) * 21.2f;
                        float d = Math.abs(u) + Math.abs(v);
                        float swell = 3.0f + 0.6f * (float) Math.sin((frame - k) * Math.PI / 4.0);
                        if (d <= swell) c = d > swell - 0.8f ? new float[] {0.45f, 0.34f, 0.12f} : new float[] {0.98f, 0.82f, 0.34f};
                    }
                    if (r <= 13f) { // the bowl: a dark well with a gold lip and a glint
                        if (r > 11.4f) c = new float[] {0.90f, 0.72f, 0.26f};
                        else {
                            float depth = 0.6f + 0.4f * (r / 11.4f), side = clamp(0.5f + (dx - dy) / 30f);
                            c = new float[] {0.12f * depth * (0.7f + 0.5f * side), 0.09f * depth * (0.7f + 0.5f * side), 0.07f * depth};
                            if (r < 10f && dx < -3f && dy > 3f && r > 6f) c = new float[] {0.30f, 0.24f, 0.16f};
                        }
                    }
                }
                p.setColor(c[0], c[1], c[2], 1f);
                p.drawPixel(x, y);
            }
        return tex(p);
    }

    /** One frame of the flame-seed: a pink-white ember with two petal-wings that open and close (12 x 12 texels). */
    private static Texture paintSeed(int f) {
        Pixmap p = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        float open = 0.5f + 0.5f * (float) Math.sin(f * Math.PI / 3.0);            // 0 wings folded .. 1 wings out
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 12; x++) {
                float dx = x + 0.5f - 6f, dy = 6f - (y + 0.5f), r = (float) Math.hypot(dx, dy);
                float[] c = null;
                if (r < 1.7f) c = new float[] {1f, 0.97f, 0.96f};
                else if (r < 2.9f) c = new float[] {1f, 0.62f, 0.86f};
                // the wings: two thin petals out to either side, lifted when open
                float wx = Math.abs(dx), reach = 2.5f + 3.2f * open, lift = 1.6f * open;
                if (wx > 2f && wx < reach && Math.abs(dy - lift * (wx - 2f) / Math.max(0.1f, reach - 2f)) < 1.1f - 0.12f * (wx - 2f)) c = new float[] {1f, 0.74f, 0.92f};
                if (dy < -2f && dy > -4f && Math.abs(dx) < 0.7f && c == null) c = new float[] {1f, 0.5f, 0.78f};   // a thread of light trailing under it
                if (c != null) {
                    p.setColor(c[0], c[1], c[2], 1f);
                    p.drawPixel(x, y);
                }
            }
        return tex(p);
    }

    /** One frame of a tiny lamp-bearer: a gold bowl with a flickering flame (8 x 8 texels). */
    private static Texture paintDiya(int f) {
        Pixmap p = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        float[] gold = {0.95f, 0.77f, 0.28f}, dark = {0.45f, 0.32f, 0.10f};
        for (int x = 1; x <= 6; x++) { p.setColor(gold[0], gold[1], gold[2], 1f); p.drawPixel(x, 6); }
        for (int x = 2; x <= 5; x++) { p.setColor(dark[0], dark[1], dark[2], 1f); p.drawPixel(x, 7); }
        int h = 3 + (f % 2), lean = f == 1 ? 1 : f == 3 ? -1 : 0;
        for (int i = 0; i < h; i++) {
            p.setColor(i == 0 ? 1f : 1f, i == 0 ? 0.95f : 0.7f - 0.1f * i, i == 0 ? 0.75f : 0.2f, 1f);
            p.drawPixel(3 + (i > 1 ? lean : 0), 5 - i);
            if (i == 0) p.drawPixel(4, 5);
        }
        return tex(p);
    }

    private static Texture tex(Pixmap p) {
        TileImages.skin(p);
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return t;
    }

    private static Texture radial() {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++) {
                float d = (float) Math.hypot(x - 31.5f, y - 31.5f) / 32f, a = Math.max(0f, 1f - d);
                p.setColor(1f, 1f, 1f, a * a * (3f - 2f * a));
                p.drawPixel(x, y);
            }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        return t;
    }

    private static float hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65536f;
    }

    private static float clamp(float v) { return v < 0f ? 0f : v > 1f ? 1f : v; }

    // ---- drawing ----------------------------------------------------------------------------------

    /** The altar and its lamp, into an open batch. */
    public void draw(SpriteBatch batch, Checkpoint c, float time) {
        float h = SIZE / 2f;
        batch.setColor(0f, 0.01f, 0.03f, 0.45f);                                   // a soft shadow thrown down-right
        batch.draw(glow, c.x - h + 6f, c.y - h * 0.8f - 5f, SIZE * 1.1f, SIZE * 0.85f);
        batch.setColor(1f, 1f, 1f, 1f);
        int turn = (int) (time * (c.collected ? 5f : 10f)) & 7;                    // the chakra turns fast while cold, slowly once lit
        batch.draw(altar[turn], c.x - h, c.y - h, SIZE, SIZE);
        if (c.collected) litParts(batch, c, time);
        else coldParts(batch, c, time);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** The unlit lamp: a winged flame-seed flutters round the empty bowl on a figure-of-eight, a slow ring of diamonds turns round the altar. */
    private void coldParts(SpriteBatch batch, Checkpoint c, float time) {
        float sx = c.x + 21f * (float) Math.sin(time * 1.3f), sy = c.y + 13f + 9f * (float) Math.sin(time * 2.6f);
        float pulse = 0.65f + 0.35f * (float) Math.sin(time * 3.1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1f, 0.45f, 0.78f, 0.34f * pulse);                          // its halo
        batch.draw(glow, sx - 30f, sy - 30f, 60f, 60f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(seed[(int) (time * 12f) % seed.length], sx - 12f, sy - 12f, 24f, 24f);
        for (int i = 0; i < 12; i++) {                                              // the turning ring of diamonds
            float a = (float) Math.toRadians(i * 30f + time * 22f);
            float px = c.x + (float) Math.cos(a) * 46f, py = c.y + (float) Math.sin(a) * 46f;
            batch.setColor(1f, 0.55f, 0.82f, 0.30f + 0.25f * (0.5f + 0.5f * (float) Math.sin(time * 2f + i)));
            batch.draw(glow, px - 6f, py - 6f, 12f, 12f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** The lit lamp: a pixel flame licking in the bowl under a warm halo, four tiny lamp-bearers circling the altar, the petals taking the light. */
    private void litParts(SpriteBatch batch, Checkpoint c, float time) {
        float fl = 0.88f + 0.12f * (float) Math.sin(time * 9f + c.x) * (float) Math.sin(time * 5.3f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1f, 0.72f, 0.28f, 0.34f * fl);                               // the warm halo
        batch.draw(glow, c.x - 80f, c.y + 5f - 80f, 160f, 160f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
        fire.puff(batch, FireSheet.BRAZIER, (int) (time * 10f), c.x, c.y + 8f, 48f);          // the bowl, seen from above, burning
        for (int d = 0; d < 4; d++) {                                               // the lamp-bearers, in a slow ring, each flickering on its own
            float a = time * 0.7f + d * 1.5708f, dx = c.x + (float) Math.cos(a) * 27f, dy = c.y + (float) Math.sin(a) * 27f;
            batch.draw(diya[((int) (time * 8f) + d) % diya.length], dx - 8f, dy - 8f, 16f, 16f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1f, 0.86f, 0.4f, 0.55f * fl);                                // a core of light in the flame
        batch.draw(glow, c.x - 12f, c.y + 6f, 24f, 28f);
        for (int d = 0; d < 4; d++) {                                               // and a little light on each bearer
            float a = time * 0.7f + d * 1.5708f;
            batch.setColor(1f, 0.75f, 0.35f, 0.3f * fl);
            batch.draw(glow, c.x + (float) Math.cos(a) * 27f - 11f, c.y + (float) Math.sin(a) * 27f - 11f, 22f, 22f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Rising sparks behind the altar (shape pass): pink and spiralling while the lamp is cold, slow gold embers once it burns. */
    public void drawParticles(ShapeRenderer shapes, Checkpoint c, float time) {
        for (int i = 0; i < SPARKS; i++) {
            float u = (time / (c.collected ? 3.2f : 1.6f) + i * 0.083f) % 1f;
            float spread = (c.collected ? 10f : 16f) * u;
            float px = c.x + (float) Math.sin(u * 7f + i * 1.9f) * spread, py = c.y + 6f + u * (c.collected ? 54f : 66f);
            float a = (float) Math.sin(u * Math.PI);
            if (c.collected) shapes.setColor(1f, 0.74f, 0.3f, a * 0.8f);
            else shapes.setColor(1f, 0.5f, 0.8f, a * 0.75f);
            shapes.circle(px, py, 0.7f + (1f - u) * 0.9f, 6);
        }
    }

    public void dispose() {
        for (Texture t : altar) t.dispose();
        fire.dispose();
        for (Texture t : seed) t.dispose();
        for (Texture t : diya) t.dispose();
        glow.dispose();
        white.dispose();
    }
}

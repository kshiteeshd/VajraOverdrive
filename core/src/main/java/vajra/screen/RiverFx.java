package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.Hataki;
import vajra.world.World;

/** THE HATAKI, ALIVE: the liquid gold is full of things that move. Hundreds of glints and flecks are carried down-river at the river's own flow (faster in the middle, in the rapids,
 *  swinging with every bend), sparks lift off its face, slag drifts dark at the banks; and at its mouth, where it is born out of the dark a little way beyond where the pilot arrives, a well
 *  of black with mist rolling off it. Stateless: every particle is a pure function of the world's time, so it is the same on every machine and costs nothing to keep. */
final class RiverFx {
    private static final int N = 700, MIST = 16;
    private static final float PERIOD = 1600f;
    private final Texture soft;

    RiverFx() {
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++) {
                float dx = (x + 0.5f) / 32f - 1f, dy = (y + 0.5f) / 32f - 1f, r = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - r);
                a = a * a * (3f - 2f * a);
                pm.drawPixel(x, y, 0xFFFFFF00 | (int) (255 * a));
            }
        soft = new Texture(pm);
        pm.dispose();
        soft.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private static float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** Inside an open SpriteBatch, in world space. */
    void draw(SpriteBatch batch, World w, float l, float b, float vw, float vh) {
        if (!w.campaign || b + vh < Hataki.Y0 - 200f || b > Hataki.SINK_Y + 400f) return;
        float t = w.time();
        float dim = w.quietT > 0f ? 0.6f : 1f;
        int srcB = batch.getBlendSrcFunc(), dstB = batch.getBlendDstFunc();
        // the well at the mouth: black, with mist coming off it
        float sx = Hataki.centre(Hataki.SOURCE_Y), sy = Hataki.SOURCE_Y;
        if (sy > b - 400f && sy < b + vh + 400f) {
            batch.setColor(0.02f, 0.01f, 0.01f, 0.78f);
            batch.draw(soft, sx - 260f, sy - 190f, 520f, 300f);
            for (int i = 0; i < MIST; i++) {
                float h1 = hash(i, 1), h2 = hash(i, 2), h3 = hash(i, 3);
                float age = (t * (0.07f + 0.05f * h1) + h2) % 1f;
                float px = sx + (h3 - 0.5f) * 240f * (0.4f + age) + (float) Math.sin(t * 0.4f + i) * 18f, py = sy - 60f + age * 360f + h1 * 40f;
                float sz = 120f + 200f * age;
                batch.setColor(0.45f + 0.15f * (1f - age), 0.36f + 0.1f * (1f - age), 0.30f, 0.26f * (1f - age) * (0.4f + 0.6f * (float) Math.sin(Math.min(1f, age * 3f) * 1.57f)));
                batch.draw(soft, px - sz / 2f, py - sz / 2f, sz, sz);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);                    // and a low glow from inside it, where the gold is first born
            batch.setColor(1f, 0.6f, 0.18f, 0.28f * dim * (0.8f + 0.2f * (float) Math.sin(t * 1.3f)));
            batch.draw(soft, sx - 150f, sy - 30f, 300f, 220f);
            batch.setBlendFunction(srcB, dstB);
        }
        // the things in the river
        float y0 = b - 200f;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < N; i++) {
            float h1 = hash(i, 11), h2 = hash(i, 12), h3 = hash(i, 13), h4 = hash(i, 14);
            float yStart = h1 * PERIOD;
            float v = 0.6f + 0.55f * h4;
            float yl = yStart + t * Hataki.speed(Math.max(Hataki.SOURCE_Y, y0 + yStart % PERIOD)) * v;
            float y = y0 + Math.floorMod((int) (yl - y0), (int) PERIOD) + ((yl - y0) % 1f + 1f) % 1f;
            if (y > b + vh + 60f || !Hataki.flows(y)) continue;
            float hw = Hataki.half(y);
            float u = (h2 * 2f - 1f) * 0.93f;
            float x = Hataki.centre(y) + u * hw + (float) Math.sin(t * (0.9f + h3) + h3 * 20f) * 5f * (0.4f + Math.abs(u));
            if (x < l - 30f || x > l + vw + 30f) continue;
            float core = 1f - Math.abs(u);
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (2.5f + 3f * h3) + i);
            float rise = (h3 > 0.9f) ? 6f * ((t * 0.8f + h2) % 1f) : 0f;                         // a few spark off the surface and lift
            float sz = (3f + 7f * h4 * h4 + 4f * core) * (h3 > 0.9f ? 0.55f : 1f);
            float a = (0.10f + 0.33f * core * core) * tw * dim;
            batch.setColor(1f, 0.74f + 0.24f * core, 0.30f + 0.5f * core * core, a);
            batch.draw(soft, x - sz / 2f, y + rise - sz / 2f, sz, sz);
        }
        batch.setBlendFunction(srcB, dstB);
        // slag flecks: small dark crusts drifting at the edges, over the gold (normal blend)
        for (int i = 0; i < 90; i++) {
            float h1 = hash(i, 21), h2 = hash(i, 22), h4 = hash(i, 24);
            float yStart = h1 * PERIOD;
            float yl = yStart + t * Hataki.speed(Math.max(Hataki.SOURCE_Y, y0 + yStart % PERIOD)) * 0.5f;
            float y = y0 + Math.floorMod((int) (yl - y0), (int) PERIOD);
            if (y > b + vh + 60f || !Hataki.flows(y)) continue;
            float u = (h2 < 0.5f ? -1f : 1f) * (0.55f + 0.4f * h4);
            float x = Hataki.centre(y) + u * Hataki.half(y);
            if (x < l - 30f || x > l + vw + 30f) continue;
            float sz = 6f + 10f * h4;
            batch.setColor(0.16f, 0.07f, 0.04f, 0.5f);
            batch.draw(soft, x - sz / 2f, y - sz / 3f, sz, sz * 0.66f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    void dispose() { soft.dispose(); }
}

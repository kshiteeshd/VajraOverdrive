package vajra.screen;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.AshCloud;
import vajra.world.World;

/** BHAIRAVA'S SMOKE, drawn: a bank of soft grey puffs that turns slowly round the heart of the cloud, thickest inside, with a few wisps drifting off its edge. It lies on the
 *  floor (WorldRenderer draws it before the hulls and the rounds), so everything in it stays plainly visible; the grey mandala at its heart is Mandala.drawAsh. */
final class AshFx {
    private static final int N = 128, PUFFS = 150;
    private final Texture puff;

    AshFx() {
        Pixmap pm = new Pixmap(N, N, Pixmap.Format.RGBA8888);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float dx = (x + 0.5f) / N * 2f - 1f, dy = (y + 0.5f) / N * 2f - 1f, r = (float) Math.sqrt(dx * dx + dy * dy);
                float body = (float) Math.pow(Math.max(0f, 1f - r), 1.2f);
                float n = 0.5f * noise(x * 0.08f, y * 0.08f, 5) + 0.3f * noise(x * 0.18f, y * 0.18f, 9) + 0.2f * noise(x * 0.4f, y * 0.4f, 13);
                float a = Math.min(1f, body * (0.35f + 1.0f * n));
                float shade = 0.42f + 0.26f * n;
                int g = (int) (255 * shade);
                pm.drawPixel(x, y, (g << 24) | (g << 16) | (Math.min(255, g + 6) << 8) | (int) (255 * a));
            }
        }
        puff = new Texture(pm);
        pm.dispose();
        puff.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    private static float hash(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    private static float noise(float x, float y, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi, ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy);
        float a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy;
    }

    /** Inside an open SpriteBatch, in world space. */
    void draw(SpriteBatch batch, World w, float l, float b, float vw, float vh) {
        AshCloud c = w.ash;
        if (!c.active) return;
        float k = c.k(), rr = c.radiusNow();
        if (k <= 0.01f || c.x + rr < l - 200 || c.x - rr > l + vw + 200 || c.y + rr < b - 200 || c.y - rr > b + vh + 200) return;
        float t = w.clock;
        for (int i = 0; i < PUFFS; i++) {
            float h1 = hash(i, 1, 71), h2 = hash(i, 2, 71), h3 = hash(i, 3, 71), h4 = hash(i, 4, 71);
            float rad = (float) Math.sqrt(h1) * rr * 1.04f, ang = h2 * 6.2831853f + t * (0.05f + 0.07f * h3) * (h4 > 0.5f ? 1f : -1f) * (1.2f - rad / (rr * 1.1f));
            float px = c.x + (float) Math.cos(ang) * rad + (float) Math.sin(t * 0.3f + h4 * 6f) * 18f, py = c.y + (float) Math.sin(ang) * rad + (float) Math.cos(t * 0.27f + h3 * 6f) * 14f;
            float sz = (260f + 220f * h3) * (0.8f + 0.2f * (float) Math.sin(t * 0.5f + h1 * 9f));
            if (px + sz < l || px - sz > l + vw || py + sz < b || py - sz > b + vh) continue;
            float edge = 1f - 0.45f * Math.max(0f, rad / rr - 0.75f) / 0.3f;                    // thinner toward the rim
            batch.setColor(1f, 1f, 1f, Math.max(0f, 0.30f * k * edge));
            batch.draw(puff, px - sz / 2f, py - sz / 2f, sz, sz);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    void dispose() { puff.dispose(); }
}

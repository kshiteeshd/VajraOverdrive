package vajra.screen;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.World;

/** THE VEIL: the way to the next loka lies in cloud until its ruler has yielded. The cloud is a bank of soft puffs (two layers, drifting slowly) over everything above the top of the seal;
 *  it has no hard edge (the puffs thin toward the seal). When the ruler lets go, World.veilClear runs from 0 to 1 and the bank comes apart: the puffs swell, drift out to the sides and
 *  up, and thin to nothing, over seven seconds. Drawn in world space after the world, before the HUD. */
final class CloudVeil {
    private static final int N = 128;
    private final Texture puff;

    CloudVeil() {
        Pixmap pm = new Pixmap(N, N, Pixmap.Format.RGBA8888);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float dx = (x + 0.5f) / N * 2f - 1f, dy = (y + 0.5f) / N * 2f - 1f, r = (float) Math.sqrt(dx * dx + dy * dy);
                float body = Math.max(0f, 1f - r);
                body = (float) Math.pow(body, 1.35f);
                float n = 0.55f * noise(x * 0.09f, y * 0.09f, 3) + 0.30f * noise(x * 0.2f, y * 0.2f, 7) + 0.15f * noise(x * 0.45f, y * 0.45f, 11);
                float a = Math.min(1f, body * (0.45f + 0.9f * n));
                float shade = 0.80f + 0.20f * (1f - (dy * 0.5f + 0.5f)) + 0.05f * n;          // lit from above: the underside is greyer
                pm.drawPixel(x, y, ((int) (255 * Math.min(1f, shade * 0.96f)) << 24) | ((int) (255 * Math.min(1f, shade * 0.98f)) << 16) | ((int) (255 * Math.min(1f, shade)) << 8) | (int) (255 * a));
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

    /** Draw every veil that is not yet gone. `light` is 0..1 (how bright the ground is here: cloud is never whiter than the day it lies in). */
    void draw(SpriteBatch batch, World w, OrthographicCamera camera, float light) {
        float vw = camera.viewportWidth * camera.zoom, vh = camera.viewportHeight * camera.zoom;
        float l = camera.position.x - vw / 2f, b = camera.position.y - vh / 2f;
        boolean begun = false;
        for (int s = 0; s < 2; s++) {
            float top = w.veilY(s), clear = w.veilClear[s];
            if (top == Float.MAX_VALUE || clear >= 1f) continue;
            if (clear < 0f) clear = 0f;
            if (b + vh < top - 360f) continue;                                                   // the whole view is below the cloud
            if (!begun) {
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                begun = true;
            }
            float t = w.clock, c = clear;
            float tint = 0.62f + 0.38f * Math.max(0f, Math.min(1f, light));
            for (int layer = 0; layer < 2; layer++) {
                float cell = layer == 0 ? 210f : 150f;
                int x0 = (int) Math.floor((l - 420f) / cell), x1 = (int) Math.ceil((l + vw + 420f) / cell);
                float yLow = Math.max(b - 420f, top - 220f);
                int y0 = (int) Math.floor(yLow / cell), y1 = (int) Math.ceil((b + vh + 420f) / cell);
                for (int iy = y0; iy <= y1; iy++) {
                    for (int ix = x0; ix <= x1; ix++) {
                        float h1 = hash(ix, iy, 17 + layer * 5), h2 = hash(ix, iy, 29 + layer * 5), h3 = hash(ix, iy, 43), h4 = hash(ix, iy, 61);
                        float px = (ix + 0.5f) * cell + (h1 - 0.5f) * cell * 0.9f + (float) Math.sin(t * (0.10f + 0.06f * h3) + h4 * 6.28f) * 34f;
                        float py = (iy + 0.5f) * cell + (h2 - 0.5f) * cell * 0.9f + (float) Math.cos(t * 0.08f + h3 * 6.28f) * 16f;
                        float into = py - (top - 130f);
                        float edge = Math.max(0f, Math.min(1f, into / 330f));
                        edge = edge * edge * (3f - 2f * edge);
                        if (edge <= 0.01f) continue;
                        float size = (layer == 0 ? 420f : 300f) * (0.75f + 0.5f * h3);
                        float away = px < World.WORLD_CX ? -1f : 1f;
                        float spread = c * c * (700f + 900f * h4) * away;                       // the bank comes apart: out to the sides...
                        float lift = c * (260f + 380f * h2);                                    // ...and up
                        float a = 0.95f * edge * (float) Math.pow(Math.max(0f, 1f - c), 1.25f) * (layer == 0 ? 0.85f : 1f);
                        float sz = size * (1f + 0.55f * c);
                        float x = px + spread, y = py + lift;
                        if (x + sz < l || x - sz > l + vw || y + sz < b || y - sz > b + vh) continue;
                        batch.setColor(tint, tint, Math.min(1f, tint * 1.02f), a);
                        batch.draw(puff, x - sz / 2f, y - sz / 2f, sz, sz);
                    }
                }
            }
        }
        if (begun) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.end();
        }
    }

    void dispose() { puff.dispose(); }
}

package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.World;

/** VANIJA'S LOTUS BERTH: the shop is the bay-master's quay, cut in the same ivory stone and gold as the walls and painted in code
 *  (no art files). A carved stone lintel hangs on two pillars, its face the bay-master's own emblem - the TULA, the merchant's
 *  balance, two pans and a coin - with two hanging lamps and two gold posts whose six lamp-pips light one by one as a hull settles in
 *  (the same footprint the collision already uses, so the board and its legs are still solid). Under it, in the marked square, is the
 *  BERTH: a round stone dais with a lotus of eight gold petals. A hull that enters is drawn gently to its centre, the petals light in
 *  turn, and two gold CLAMPS close on its flanks (drawn over the hull); the hangar opens. Leave, and the clamps let go.
 *
 *  <p>Everything is a pure function of the shop's state and timer, drawn with one 64 px sign (7 frames of lamp-pips) drawn at 2x, one
 *  32 px dais drawn at 56 world px, and soft glows. */
public class ShopImages {
    private static final int S = 64;
    /** The berth's centre against the sign's centre (world px): the middle of the marked dock square. */
    public static final float BERTH_DX = 2f, BERTH_DY = -46f;
    private static final float PAD = 56f;

    private final Texture[][] sign = new Texture[7][16];   // [lamps lit][animation frame]: the sign's own sprite sheet
    private final Texture[] dais = new Texture[8];
    private final Texture glow, white;

    public ShopImages() {
        this(0);
    }

    /** `skin` 1 paints the same bay in Vitala's palette (the Gold Assay). */
    public ShopImages(int skin) {
        TileImages.paintSkin = skin;
        for (int i = 0; i < 7; i++) for (int f = 0; f < 16; f++) sign[i][f] = paintSign(i, f);
        for (int f = 0; f < 8; f++) dais[f] = paintDais(f);
        TileImages.paintSkin = 0;
        glow = radial();
        Pixmap p = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        white = new Texture(p);
        p.dispose();
    }

    // ---- painting ---------------------------------------------------------------------------------

    private static final float[] STONE = {0.78f, 0.74f, 0.62f}, INK = {0.14f, 0.11f, 0.08f}, GOLD = {0.95f, 0.77f, 0.28f},
            GOLD_D = {0.55f, 0.40f, 0.14f}, AMBER = {1f, 0.72f, 0.26f};

    private static void px(Pixmap p, int x, int y, float[] c, float k) {
        if (x < 0 || y < 0 || x >= S || y >= S) return;
        p.setColor(Math.min(1f, c[0] * k), Math.min(1f, c[1] * k), Math.min(1f, c[2] * k), 1f);
        p.drawPixel(x, y);
    }

    private static void rect(Pixmap p, int x0, int y0, int x1, int y1, float[] c, float k) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) px(p, x, y, c, k);
    }

    /** The sign with `lamps` of the six dock pips lit (0 = parked out, 6 = docked). Texel y runs DOWN; the sign's centre is texel (32, 32). */
    private static Texture paintSign(int lamps, int frame) {
        Pixmap p = new Pixmap(S, S, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        // the legs: stone pillars with gold bands and a lotus capital
        for (int side = 0; side < 2; side++) {
            int x0 = side == 0 ? 6 : 52;
            rect(p, x0, 40, x0 + 7, 60, INK, 1f);
            for (int y = 41; y <= 59; y++) for (int x = x0 + 1; x <= x0 + 6; x++) px(p, x, y, STONE, 1.05f - 0.14f * (x - x0) / 6f);
            rect(p, x0 - 1, 40, x0 + 8, 42, GOLD_D, 1f);
            rect(p, x0 - 1, 41, x0 + 8, 41, GOLD, 1f);
            rect(p, x0 + 1, 49, x0 + 6, 50, GOLD, 0.95f);
            rect(p, x0 + 1, 57, x0 + 6, 58, GOLD, 0.95f);
        }
        // the lintel: a chamfered stone board with a gold border and a cornice
        for (int y = 4; y <= 39; y++)
            for (int x = 4; x <= 59; x++) {
                int cx = Math.min(x - 4, 59 - x), cy = Math.min(y - 4, 39 - y);
                if (cx + cy < 3) continue;                                          // chamfered corners
                boolean edge = x == 4 || x == 59 || y == 4 || y == 39 || cx + cy == 3;
                if (edge) px(p, x, y, INK, 1f);
                else px(p, x, y, STONE, 1.07f - 0.22f * (y - 4) / 36f - 0.06f * (x - 4) / 56f);
            }
        for (int x = 7; x <= 56; x++) { px(p, x, 7, GOLD, 1f); px(p, x, 36, GOLD, 0.9f); }
        for (int y = 8; y <= 35; y++) { px(p, 7, y, GOLD, 1f); px(p, 56, y, GOLD_D, 1f); }
        for (int x = 5; x <= 58; x++) { px(p, x, 5, GOLD, 1f); px(p, x, 6, x % 6 < 3 ? GOLD_D : GOLD, 1f); }   // the cornice
        // the TULA, alive: the post is still, but the beam SEE-SAWS (a 16-frame cycle), the chains and pans ride its ends, and a gold coin
        // HOPS from pan to pan at the bottom of every swing while a pearl sits in the other
        rect(p, 31, 12, 32, 30, GOLD, 1f);
        rect(p, 26, 30, 37, 32, GOLD_D, 1f);
        rect(p, 27, 30, 36, 30, GOLD, 1f);
        px(p, 31, 10, GOLD, 1f); px(p, 32, 10, GOLD, 1f); rect(p, 30, 11, 33, 11, GOLD, 1f);
        int d = Math.round(2f * (float) Math.sin(frame * Math.PI / 8.0));             // the beam's tilt: + lowers the left end
        for (int x = 14; x <= 49; x++) {
            int yb = 14 + Math.round(d * (1f - 2f * (x - 14) / 35f));
            px(p, x, yb, GOLD, 1f);
            px(p, x, yb + 1, GOLD_D, 1f);
        }
        int panL = 25 + d, panR = 25 - d;
        for (int y = 16 + d; y <= panL - 1; y++) px(p, 15, y, GOLD_D, 1f);
        for (int y = 16 - d; y <= panR - 1; y++) px(p, 48, y, GOLD_D, 1f);
        int[] cxs = {15, 48}, pys = {panL, panR};
        for (int sd = 0; sd < 2; sd++) {
            int cx = cxs[sd], py = pys[sd];
            rect(p, cx - 6, py, cx + 6, py, GOLD, 1f);
            rect(p, cx - 5, py + 1, cx + 5, py + 1, GOLD_D, 1f);
            rect(p, cx - 3, py + 2, cx + 3, py + 2, GOLD_D, 1f);
        }
        float ph = frame / 16f;                                                         // where the coin is: left, hopping, right, hopping back
        float coinX, coinY;
        boolean hopping = (ph >= 0.375f && ph < 0.5f) || ph >= 0.875f;
        if (ph < 0.375f) { coinX = 15; coinY = panL - 3; }
        else if (ph < 0.5f) { float t = (ph - 0.375f) / 0.125f; coinX = 15 + 33 * t; coinY = panL - 3 + (panR - panL) * t - 8f * (float) Math.sin(Math.PI * t); }
        else if (ph < 0.875f) { coinX = 48; coinY = panR - 3; }
        else { float t = (ph - 0.875f) / 0.125f; coinX = 48 - 33 * t; coinY = panR - 3 + (panL - panR) * t - 8f * (float) Math.sin(Math.PI * t); }
        rect(p, Math.round(coinX) - 1, Math.round(coinY), Math.round(coinX) + 1, Math.round(coinY) + 2, GOLD, 1.05f);
        px(p, Math.round(coinX), Math.round(coinY) + 1, STONE, 1f);
        if (!hopping) {                                                                 // the pearl rests in whichever pan has no coin
            int side = ph < 0.5f ? 1 : 0;
            rect(p, cxs[side] - 1, pys[side] - 2, cxs[side] + 1, pys[side] - 1, new float[] {0.96f, 0.94f, 0.9f}, 1f);
        }
        // two hanging lamps on short chains
        for (int s = 0; s < 2; s++) {
            int cx = s == 0 ? 11 : 52;
            for (int y = 40; y <= 43; y++) px(p, cx, y, GOLD_D, 1f);
            rect(p, cx - 1, 44 + ((frame + s * 3) % 4 == 0 ? 1 : 0), cx + 1, 46, AMBER, 1f);   // the lamp's flame gutters
            px(p, cx, 45, new float[] {1f, 0.95f, 0.7f}, 1f);
        }
        // the dock posts: two gold lamp-posts, six pips each, lit as the berth fills
        for (int s = 0; s < 2; s++) {
            int x0 = s == 0 ? 19 : 42;
            rect(p, x0, 46, x0 + 4, 59, INK, 1f);
            rect(p, x0 + 1, 47, x0 + 3, 58, GOLD_D, 1f);
            for (int i = 0; i < 6; i++) {
                int y = 57 - 2 * i;
                boolean chase = lamps == 0 && i == (frame / 2) % 6;                                 // idle: one pip runs up each post, over and over
                if (i < lamps) px(p, x0 + 2, y, new float[] {1f, 0.95f, 0.6f}, 1f);
                else if (chase) px(p, x0 + 2, y, new float[] {1f, 0.72f, 0.3f}, 1f);
                else px(p, x0 + 2, y, new float[] {0.22f, 0.17f, 0.09f}, 1f);
            }
        }
        TileImages.skin(p);
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return t;
    }

    /** The dais: a round stone berth, studs on its rim, a lotus of eight gold petals, a dark gold mandala at the centre. 32 texels. */
    private static Texture paintDais(int frame) {
        final int n = 32;
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                float dx = x + 0.5f - n / 2f, dy = n / 2f - (y + 0.5f), r = (float) Math.hypot(dx, dy), ang = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (r > 15f) continue;
                float lit = Math.max(0f, Math.min(1f, 0.5f + (-dx + dy) / 36f));
                float[] c;
                if (r > 14f) c = INK;
                else if (r > 12f) c = new float[] {STONE[0] * (0.62f + 0.5f * lit), STONE[1] * (0.62f + 0.5f * lit), STONE[2] * (0.62f + 0.5f * lit)};
                else c = new float[] {STONE[0] * 0.9f, STONE[1] * 0.9f, STONE[2] * 0.9f};
                if (r <= 12f) {
                    // eight petals: each a long diamond along its own axis between r 5.5 and 11.5
                    for (int k = 0; k < 8; k++) {
                        float a = (float) Math.toRadians(k * 45f);
                        float ax = (float) Math.cos(a), ay = (float) Math.sin(a);
                        float wave = 0.82f + 0.22f * (float) Math.sin((frame - k) * Math.PI / 4.0);   // a wave of light walks round the petals
                        float along = dx * ax + dy * ay, across = Math.abs(-dx * ay + dy * ax);
                        if (along > 5.5f && along < 11.8f && across < 3.0f * (1f - Math.abs(along - 8.6f) / 3.4f) + 0.4f)
                            c = across > 2.0f * (1f - Math.abs(along - 8.6f) / 3.4f) ? GOLD_D : new float[] {GOLD[0] * (0.85f + 0.15f * lit) * wave, GOLD[1] * (0.85f + 0.15f * lit) * wave, GOLD[2] * wave};
                    }
                    if (r < 5f) { // the mandala: dark gold with a bright eye
                        c = r < 1.6f + 0.4f * (frame % 2) ? new float[] {1f, 0.9f, 0.55f} : (r > 3.6f ? GOLD_D : new float[] {0.20f, 0.15f, 0.08f});
                    }
                }
                if (r > 12.2f && r < 14f) { // eight gold studs on the rim
                    float a8 = Math.abs(((ang + 360f) % 45f) - 22.5f);
                    if (a8 < 4f && r > 12.8f && r < 13.6f) c = GOLD;
                }
                p.setColor(Math.min(1f, c[0]), Math.min(1f, c[1]), Math.min(1f, c[2]), 1f);
                p.drawPixel(x, y);
            }
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

    // ---- drawing ----------------------------------------------------------------------------------

    /** How far the berth has closed on a hull: 0 parked out, up to 1 docked. */
    static float seat(int state, float timer) {
        if (state == World.DOCKING) return Math.max(0f, Math.min(1f, timer / World.DOCK_TIME));
        return state == World.DOCKED || state == World.SITTING ? 1f : 0f;
    }

    /** The berth and the sign, into an open batch, under the hull. `time` is world time (the lamps flicker on it). */
    public void draw(SpriteBatch batch, float x, float y, int state, float timer, float time) {
        float k = seat(state, timer), cx = x + BERTH_DX, cy = y + BERTH_DY;
        batch.setColor(0f, 0.01f, 0.03f, 0.4f);                                       // a soft shadow under the sign
        batch.draw(glow, x - 62f, y - 30f, 124f, 70f);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(dais[((int) (time * 6f)) & 7], cx - PAD / 2f, cy - PAD / 2f, PAD, PAD);
        batch.draw(sign[Math.min(6, Math.round(k * 6f))][((int) (time * 8f) + (int) (x * 0.37f)) & 15], x - 64f, y - 64f, 128f, 128f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float fl = 0.88f + 0.12f * (float) Math.sin(time * 6f + x);
        for (int s = 0; s < 2; s++) {                                                  // the two hanging lamps
            batch.setColor(1f, 0.72f, 0.28f, 0.34f * fl);
            batch.draw(glow, x + (s == 0 ? -42f : 40f) - 22f, y - 22f - 14f, 44f, 44f);
        }
        for (int i = 0; i < 8; i++) {                                                   // the petals take the light in turn as it settles
            if (k * 8f <= i) continue;
            float a = (float) Math.toRadians(i * 45f), glowK = Math.min(1f, k * 8f - i);
            batch.setColor(1f, 0.82f, 0.38f, 0.5f * glowK * (state == World.DOCKING ? 1f : fl));
            batch.draw(glow, cx + (float) Math.cos(a) * 17f - 9f, cy + (float) Math.sin(a) * 17f - 9f, 18f, 18f);
        }
        if (k >= 1f) {                                                                  // docked: a steady warm bloom over the berth
            batch.setColor(1f, 0.78f, 0.34f, 0.26f * fl);
            batch.draw(glow, cx - 56f, cy - 56f, 112f, 112f);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    /** The two gold clamps, drawn OVER the hull: out wide while it is parked out, closing on its flanks as it seats. */
    public void drawClamps(SpriteBatch batch, float x, float y, int state, float timer) {
        float k = seat(state, timer);
        if (k <= 0.02f) return;
        float close = Math.max(0f, Math.min(1f, (k - 0.35f) / 0.65f)), cx = x + BERTH_DX, cy = y + BERTH_DY;
        for (int side = -1; side <= 1; side += 2) {
            float tip = cx + side * (30f - 15f * close);
            float x0 = side < 0 ? tip - 12f : tip;
            batch.setColor(0.14f, 0.11f, 0.08f, 1f);
            batch.draw(white, x0 - 1f, cy - 4f, 14f, 8f);
            batch.setColor(0.95f, 0.77f, 0.28f, 1f);
            batch.draw(white, x0, cy - 3f, 12f, 6f);
            batch.setColor(1f, 0.93f, 0.62f, 1f);
            batch.draw(white, x0 + (side < 0 ? 8f : 0f), cy - 3f, 4f, 2f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture[] row : sign) for (Texture t : row) t.dispose();
        for (Texture t : dais) t.dispose();
        glow.dispose();
        white.dispose();
    }
}

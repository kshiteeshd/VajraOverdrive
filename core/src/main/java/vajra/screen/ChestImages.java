package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.Chest;

/** THE TREASURE CHEST, drawn (docs/TREASURE_CHESTS.md): sprite sheets painted pixel by pixel at start-up, no art files, two skins.
 *  <ul>
 *   <li>Atala's: a <b>pearl chest</b> bound in gold with a lavender clasp-jewel;</li>
 *   <li>Vitala's: a <b>basalt chest</b> bound in molten gold with an ember for a clasp;</li>
 *  </ul>
 *  each with 6 idle frames (a glint crosses the lid, the jewel breathes) and 6 opening frames (the lid lifts and the light comes out), and, for
 *  Vitala, an <b>ember well</b> (3 frames of a molten pool in a basalt ring). */
public class ChestImages {
    private static final int N = 48;
    static final float SIZE = 64f;
    private final Texture[][] idle = new Texture[2][6], opening = new Texture[2][6];
    private final Texture[] well = new Texture[3];
    private final Texture glow;

    public ChestImages() {
        for (int sk = 0; sk < 2; sk++)
            for (int f = 0; f < 6; f++) {
                idle[sk][f] = paint(sk, f, 0f);
                opening[sk][f] = paint(sk, f, f / 5f + 0.0001f);
            }
        for (int f = 0; f < 3; f++) well[f] = paintWell(f);
        glow = radial();
    }

    private static float[] c(float r, float g, float b) { return new float[] {r, g, b}; }

    private static void px(Pixmap p, int x, int y, float[] c, float a) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        p.setColor(Math.min(1f, c[0]), Math.min(1f, c[1]), Math.min(1f, c[2]), a);
        p.drawPixel(x, N - 1 - y);
    }

    private static void rect(Pixmap p, int x0, int y0, int x1, int y1, float[] c) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) px(p, x, y, c, 1f);
    }

    /** One chest: skin 0 pearl, 1 basalt; `frame` the glint step; `open` 0 closed .. 1 wide open. */
    private static Texture paint(int skin, int frame, float open) {
        Pixmap p = new Pixmap(N, N, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        float[] body = skin == 0 ? c(0.92f, 0.90f, 0.84f) : c(0.30f, 0.25f, 0.28f);
        float[] shade = skin == 0 ? c(0.72f, 0.70f, 0.64f) : c(0.16f, 0.12f, 0.15f);
        float[] lit = skin == 0 ? c(1f, 0.99f, 0.94f) : c(0.55f, 0.45f, 0.45f);
        float[] band = skin == 0 ? c(1f, 0.82f, 0.25f) : c(1f, 0.62f, 0.12f);
        float[] bandHi = skin == 0 ? c(1f, 0.95f, 0.6f) : c(1f, 0.92f, 0.5f);
        float[] line = skin == 0 ? c(0.30f, 0.24f, 0.14f) : c(0.12f, 0.05f, 0.04f);
        float[] jewel = skin == 0 ? c(0.72f, 0.60f, 0.96f) : c(1f, 0.38f, 0.08f);
        int lift = Math.round(open * 9f);
        // the shadow under it
        for (int x = 7; x <= 40; x++) px(p, x, 3, c(0f, 0f, 0f), 0.35f);
        // the body: a box 34 wide, 18 tall, shaded dark at the foot, lit at the top edge
        rect(p, 7, 4, 40, 21, line);
        rect(p, 8, 5, 39, 20, body);
        rect(p, 8, 5, 39, 8, shade);
        rect(p, 8, 20, 39, 20, lit);
        // gold bands, a clasp plate
        for (int bx : new int[] {13, 33}) { rect(p, bx, 5, bx + 2, 20, band); rect(p, bx, 18, bx, 20, bandHi); }
        rect(p, 21, 10, 26, 20, band);
        rect(p, 22, 11, 25, 18, line);
        float pulse = 0.75f + 0.25f * (float) Math.sin(frame * Math.PI / 3.0);
        rect(p, 23, 12, 24, 16, new float[] {jewel[0] * pulse, jewel[1] * pulse, jewel[2] * pulse});
        if (open > 0f) {                                                   // the light that comes out of an open chest
            rect(p, 9, 21, 38, 21 + Math.min(2, lift / 3), skin == 0 ? c(1f, 0.95f, 0.7f) : c(1f, 0.75f, 0.25f));
            for (int k = 0; k < 7; k++) {
                int rx = 12 + k * 4;
                for (int ry = 22; ry < 22 + 4 + (k % 3) * 3 + lift; ry++) px(p, rx, ry, c(1f, 0.92f, 0.55f), Math.max(0.1f, 1f - (ry - 22) / 18f));
            }
        }
        // the lid: an arch 34 wide; closed it sits on the body, open it stands up behind it, seen edge-on
        if (open < 0.05f) {
            for (int y = 21; y <= 33; y++) {
                float t = (y - 21) / 12f, hw = 17f * (float) Math.sqrt(Math.max(0f, 1f - t * t * 0.9f));
                int x0 = Math.round(24 - hw), x1 = Math.round(23 + hw);
                for (int x = x0; x <= x1; x++) px(p, x, y, (x == x0 || x == x1 || y == 33) ? line : y > 30 ? lit : body, 1f);
            }
            for (int bx : new int[] {13, 33}) rect(p, bx, 22, bx + 2, 31, band);
            // the glint crosses the lid, left to right
            int gx = 10 + frame * 5;
            px(p, gx, 29, bandHi, 1f); px(p, gx - 1, 29, c(1f, 1f, 1f), 0.8f); px(p, gx + 1, 29, c(1f, 1f, 1f), 0.8f);
            px(p, gx, 30, c(1f, 1f, 1f), 0.8f); px(p, gx, 28, c(1f, 1f, 1f), 0.8f);
        } else {
            int base = 24 + lift;
            for (int y = base; y <= base + 8; y++) for (int x = 10; x <= 37; x++) px(p, x, y, (y == base + 8 || x == 10 || x == 37) ? line : y > base + 5 ? lit : shade, 1f);
            for (int bx : new int[] {14, 33}) rect(p, bx, base + 1, bx + 2, base + 7, band);
        }
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        p.dispose();
        return t;
    }

    private static Texture paintWell(int frame) {
        Pixmap p = new Pixmap(N, N, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                float dx = x + 0.5f - N / 2f, dy = N / 2f - (y + 0.5f), r = (float) Math.hypot(dx, dy * 1.1f);
                if (r > 21f) continue;
                if (r > 19f) p.setColor(0.10f, 0.05f, 0.04f, 1f);
                else if (r > 14f) p.setColor(0.22f + 0.08f * (dy > 0 ? 1 : 0), 0.18f, 0.2f, 1f);      // the basalt ring, lit from above
                else {                                                                                 // molten gold, stirring
                    float w = (float) Math.sin(r * 0.9 - frame * 2.1 + Math.atan2(dy, dx) * 2) * 0.5f + 0.5f;
                    p.setColor(1f, 0.55f + 0.4f * w, 0.1f + 0.35f * w * w, 1f);
                }
                p.drawPixel(x, y);
            }
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        p.dispose();
        return t;
    }

    private static Texture radial() {
        int n = 64;
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                float r = (float) Math.hypot(x + 0.5f - n / 2f, y + 0.5f - n / 2f) / (n / 2f);
                float a = Math.max(0f, 1f - r);
                p.setColor(1f, 1f, 1f, a * a);
                p.drawPixel(x, y);
            }
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        p.dispose();
        return t;
    }

    /** A chest, closed (idling) or opening and then open for good. */
    public void draw(SpriteBatch batch, Chest c, float time) {
        int sk = c.loka == 0 ? 0 : 1;
        Texture t = c.open ? opening[sk][Math.min(5, (int) (c.openT * 12f))] : idle[sk][((int) (time * 6f) + (int) (c.x * 0.13f)) % 6];
        float bob = c.open ? 0f : 1.5f * (float) Math.sin(time * 2.2f + c.x);
        batch.setColor(1, 1, 1, 1);
        batch.draw(t, c.x - SIZE / 2f, c.y - SIZE / 2f + 8f + bob, SIZE, SIZE);
    }

    /** An ember well; a drunk one is cold. */
    public void drawWell(SpriteBatch batch, float x, float y, boolean used, float time) {
        if (used) batch.setColor(0.35f, 0.3f, 0.3f, 1f);
        batch.draw(well[((int) (time * 5f)) % 3], x - 26f, y - 26f, 52f, 52f);
        batch.setColor(1, 1, 1, 1);
    }

    /** The glow a chest or a well throws into the lighting pass: {x, y, radius, r, g, b}; none once a chest is spent. */
    public void glows(java.util.List<Chest> chests, float[][] pockets, boolean[] used, float l, float b, float vw, float vh, float time, java.util.List<float[]> out) {
        for (int i = 0; i < chests.size(); i++) {
            Chest c = chests.get(i);
            if (c.x < l - 120 || c.x > l + vw + 120 || c.y < b - 120 || c.y > b + vh + 120) continue;
            float k = c.open ? Math.max(0f, 1f - c.openT / 5f) * 1.2f : 0.55f + 0.2f * (float) Math.sin(time * 3f + c.x);
            if (k <= 0.02f) continue;
            out.add(c.loka == 0 ? new float[] {c.x, c.y, 130f, 0.9f * k, 0.8f * k, 0.4f * k} : new float[] {c.x, c.y, 140f, 1f * k, 0.5f * k, 0.12f * k});
        }
        for (int i = 0; i < pockets.length; i++) {
            if (pockets[i][5] != Chest.WELL || used[i]) continue;
            if (pockets[i][0] < l - 150 || pockets[i][0] > l + vw + 150 || pockets[i][1] < b - 150 || pockets[i][1] > b + vh + 150) continue;
            out.add(new float[] {pockets[i][0], pockets[i][1], 200f, 1f, 0.55f, 0.15f});
        }
    }

    public void dispose() {
        for (Texture[] row : idle) for (Texture t : row) t.dispose();
        for (Texture[] row : opening) for (Texture t : row) t.dispose();
        for (Texture t : well) t.dispose();
        glow.dispose();
    }
}

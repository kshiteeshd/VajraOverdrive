package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.Vents;
import vajra.world.World;

/** THE FIRE VENTS, drawn (docs/VITALA_PLAN.md section 5): the jet is cut from the fire sheet ({@link FireSheet}), the mouth painted at start-up.
 *  <ul>
 *   <li><b>the jet</b>, 8 frames of a 32 x 32 tongue of fire, repeated down the vent's reach so it streams, turned to the way it runs;</li>
 *   <li><b>the mouth</b>, 4 frames: a black grille in the wall with a gold lip whose embers breathe.</li>
 *  </ul>
 *  While a vent WARNS the mouth glows red and a short tongue builds; while it FLAMES the jet runs the full width of the river, rises and dies. */
public class VentImages {
    private static final int MS = 24;
    private final Texture[] mouth = new Texture[4];
    private final FireSheet fire = new FireSheet();
    private final Texture glow;

    public VentImages() {
        for (int i = 0; i < mouth.length; i++) mouth[i] = paintMouth(i);
        glow = radial();
    }

    private static float hash(int x, int y, int k) {
        int n = x * 73856093 ^ y * 19349663 ^ k * 83492791;
        n = (n ^ (n >>> 13)) * 1274126177;
        return ((n ^ (n >>> 16)) & 0xFFFF) / 65535f;
    }

    private static Texture paintMouth(int frame) {
        Pixmap p = new Pixmap(MS, MS, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        p.setColor(0, 0, 0, 0);
        p.fill();
        for (int y = 0; y < MS; y++)
            for (int x = 0; x < MS; x++) {
                float dx = x + 0.5f - MS / 2f, dy = y + 0.5f - MS / 2f;
                float r = (float) Math.hypot(dx * 0.85f, dy);
                if (r > 11f) continue;
                if (r > 9.2f) p.setColor(0.12f, 0.07f, 0.05f, 1f);                          // the outline
                else if (r > 7.2f) p.setColor(0.95f, 0.65f, 0.15f, 1f);                     // the gold lip
                else {                                                                      // the grille over a dark throat
                    boolean bar = ((int) Math.floor(dy + 12f) % 4) == 0;
                    p.setColor(bar ? 0.28f : 0.06f, bar ? 0.18f : 0.03f, bar ? 0.12f : 0.03f, 1f);
                    if (hash(x, y, frame) > 0.93f) p.setColor(1f, 0.55f, 0.15f, 1f);        // an ember in the throat
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

    /** Draw every vent in view. `quiet`: the shrine's hush, every vent cool. */
    public void draw(SpriteBatch batch, World w, float l, float b, float viewW, float viewH) {
        Vents v = w.vents;
        boolean quiet = w.quietT > 0f;
        float time = w.time();
        for (int i = 0; i < v.count(); i++) {
            float[] a = v.at[i];
            if (a[0] + Vents.LENGTH < l || a[0] - Vents.LENGTH > l + viewW || a[1] + Vents.LENGTH < b || a[1] - Vents.LENGTH > b + viewH) continue;
            int st = v.state(i, w.clock, quiet);
            float p = v.progress(i, w.clock);
            float deg = (float) Math.toDegrees(a[2]);
            batch.setColor(1, 1, 1, 1);
            batch.draw(mouth[((int) (time * 6f) + i) & 3], a[0] - MS / 2f, a[1] - MS / 2f, MS / 2f, MS / 2f, MS, MS, 1.4f, 1.4f, deg, 0, 0, MS, MS, false, false);
            if (st == Vents.COOL) continue;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            if (st == Vents.WARNING) {                                                       // the mouth glows red, a short tongue builds
                float g = p * p;
                batch.setColor(1f, 0.35f, 0.08f, 0.55f * g);
                batch.draw(glow, a[0] - 52, a[1] - 52, 104, 104);
                softJet(batch, a, 30f + 80f * p, 14f, 0.25f + 0.3f * p, time, i, 0.35f);                // the warning: a low red glow in the wall and a short soft tongue building
            } else {                                                                         // the flame: rises fast, holds, dies back
                float rise = Math.min(1f, p * 5f), fall = p > 0.8f ? 1f - (p - 0.8f) / 0.2f : 1f;
                float len = Vents.LENGTH * rise;
                softJet(batch, a, len, Vents.HALF * 1.5f, 0.55f * fall, time, i, 1f);                    // the flame: a stream of soft fire, white-gold at the mouth, red at the tip
                batch.setColor(1f, 0.5f, 0.12f, 0.22f * fall);
                batch.draw(glow, a[0] - 64, a[1] - 64, 128, 128);
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(1, 1, 1, 1);
        }
    }

    /** FLAME COLUMNS ON THE LAYERS BEHIND (docs/VITALA_PLAN.md section 3): a vent's fire stands up the shaft too, a column pinned to the vent's own
     *  spot on layer k, so the fire is seen far down the stack long before the vent in front of the hull fires. Warning is a rising glow, flame a tall
     *  bright column, cool a few embers. Call inside an open batch whose projection is layer k's. */
    public void columns(SpriteBatch batch, World w, float l, float b, float vw, float vh, int k, float alpha) {
        Vents v = w.vents;
        boolean quiet = w.quietT > 0f;
        float time = w.time(), fall = alpha * Math.max(0.25f, 1f - 0.17f * k);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < v.count(); i++) {
            float[] a = v.at[i];
            if (a[0] < l - 160 || a[0] > l + vw + 160 || a[1] < b - 80 || a[1] > b + vh + 260) continue;
            int st = v.state(i, w.clock, quiet);
            float p = v.progress(i, w.clock), len, al;
            if (st == Vents.FLAMING) {
                float rise = Math.min(1f, p * 5f), die = p > 0.8f ? 1f - (p - 0.8f) / 0.2f : 1f;
                len = 170f * rise;
                al = 0.9f * die;
            } else if (st == Vents.WARNING) {
                len = 30f + 70f * p;
                al = 0.25f + 0.4f * p;
            } else {
                len = 16f + 6f * (float) Math.sin(time * 5f + i);
                al = 0.35f;
            }
            batch.setColor(1f, 0.7f, 0.3f, al * fall);
            fire.jet(batch, (int) (time * 14f) + i * 3, a[0], a[1], 90f, len, 40f, 1f);
            if (st != Vents.COOL) {
                batch.setColor(1f, 0.45f, 0.1f, 0.35f * al * fall);
                batch.draw(glow, a[0] - 70f, a[1] - 40f, 140f, 140f);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1, 1, 1, 1);
    }

    /** A jet as a stream of soft puffs, tapering and cooling down its length (white-gold at the mouth, orange, then a dull red at the tip), each swaying a little: it reads as fire moving, not as a stamped picture. */
    private void softJet(SpriteBatch batch, float[] a, float len, float width, float alpha, float time, int seed, float flame) {
        float c = (float) Math.cos(a[2]), s = (float) Math.sin(a[2]);
        int n = 22;
        for (int k = 0; k < n; k++) {
            float f = ((k + ((time * 5f + seed * 0.37f) % 1f)) / n) % 1f;
            float h = hash(k, seed, 7);
            float d = f * len;
            float sway = (float) Math.sin(time * 7f + k * 1.7f + seed) * width * 0.12f * f;
            float px = a[0] + c * d - s * sway, py = a[1] + s * d + c * sway;
            float r = width * (0.35f + 0.85f * f) * (0.8f + 0.4f * h);
            float al = alpha * (float) Math.pow(1f - f, 1.25f) * (0.7f + 0.3f * h);
            float rr = 1f, gg = 0.92f - 0.62f * f, bb = 0.55f - 0.52f * f;
            batch.setColor(rr, Math.max(0.2f, gg), Math.max(0.04f, bb), al);
            batch.draw(glow, px - r, py - r, r * 2f, r * 2f);
        }
    }

    private void drawJet(SpriteBatch batch, int frame, float[] a, float len, float width) {
        fire.jet(batch, frame, a[0], a[1], (float) Math.toDegrees(a[2]), len, width, 1f);
    }

    /** The glows the vents throw into the lighting pass: {x, y, radius, r, g, b}. */
    public void glows(World w, float l, float b, float viewW, float viewH, java.util.List<float[]> out) {
        Vents v = w.vents;
        boolean quiet = w.quietT > 0f;
        for (int i = 0; i < v.count(); i++) {
            float[] a = v.at[i];
            if (a[0] + Vents.LENGTH < l || a[0] - Vents.LENGTH > l + viewW || a[1] + 160 < b || a[1] - 160 > b + viewH) continue;
            int st = v.state(i, w.clock, quiet);
            float p = v.progress(i, w.clock);
            float c = (float) Math.cos(a[2]), s = (float) Math.sin(a[2]);
            if (st == Vents.WARNING) out.add(new float[] {a[0] + c * 30f, a[1] + s * 30f, 150f, 0.7f * p * p, 0.22f * p * p, 0.05f * p * p});
            else if (st == Vents.FLAMING) {
                float fall = p > 0.8f ? 1f - (p - 0.8f) / 0.2f : 1f;
                for (int k = 0; k < 3; k++)
                    out.add(new float[] {a[0] + c * (60f + 100f * k), a[1] + s * (60f + 100f * k), 200f, 0.55f * fall, 0.30f * fall, 0.09f * fall});
            }
        }
    }

    public void dispose() {
        fire.dispose();
        for (Texture t : mouth) t.dispose();
        glow.dispose();
    }
}

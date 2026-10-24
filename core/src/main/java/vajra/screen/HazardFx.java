package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.Hazards;
import vajra.world.World;

/** THE GROUND THAT BURNS, drawn (world/Hazards): patches of fire (Jvala's cinders, Agneya's trail, Bhairava's ash), Rekha's hairlines of gold that
 *  grow, wait and ignite, and the ash ring that closes on the pilot in Bhairava's fight. Flames are cut from the fire sheet ({@link FireSheet}: ground and hearth, 32 x 32, seen from above), a soft glow, and a white pixel stretched into lines - all drawn additively. */
public class HazardFx {
    private final FireSheet fire = new FireSheet();
    private final Texture glow, white;

    public HazardFx() {
        glow = radial();
        Pixmap p = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1);
        p.fill();
        white = new Texture(p);
        p.dispose();
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

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** A burst of ground fire seen from above, `size` across, its own phase per seed so no two lick together. */
    private void tongue(SpriteBatch batch, float x, float y, float size, float time, int seed, float a) {
        batch.setColor(1f, 0.95f, 0.8f, Math.min(1f, a * 1.3f));
        fire.puff(batch, (seed & 1) == 0 ? FireSheet.GROUND : FireSheet.HEARTH, (int) (time * 12f) + seed * 3, x, y, size);
    }

    /** Everything burning in view. */
    public void draw(SpriteBatch batch, World w, float l, float b, float vw, float vh) {
        Hazards hz = w.hazards;
        if (hz.patches.isEmpty() && hz.lines.isEmpty() && !hz.ring) return;
        float time = w.time();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 0; i < hz.patches.size(); i++) {
            Hazards.Patch p = hz.patches.get(i);
            if (p.x + p.r < l - 40 || p.x - p.r > l + vw + 40 || p.y + p.r < b - 40 || p.y - p.r > b + vh + 40) continue;
            float life = Math.min(1f, p.life / 1.2f) * Math.min(1f, (p.max - p.life) / 0.25f + 0.65f);
            batch.setColor(1f, 0.5f, 0.12f, 0.75f * life);
            batch.draw(glow, p.x - p.r * 1.7f, p.y - p.r * 1.7f, p.r * 3.4f, p.r * 3.4f);
            int n = Math.max(3, (int) (p.r / 9f));
            for (int k = 0; k < n; k++) {
                int hs = hash((int) p.x * 7 + k, (int) p.y * 13 + k);
                float ang = (hs & 1023) / 1024f * 6.2831853f, rad = p.r * 0.85f * (((hs >>> 10) & 1023) / 1024f);
                tongue(batch, p.x + (float) Math.cos(ang) * rad, p.y + (float) Math.sin(ang) * rad, 44f + (hs & 7) * 5f, time, k, life);
            }
        }
        for (int i = 0; i < hz.lines.size(); i++) {
            Hazards.Line ln = hz.lines.get(i);
            float g = Hazards.grown(ln);
            float ex = ln.x0 + (ln.x1 - ln.x0) * g, ey = ln.y0 + (ln.y1 - ln.y0) * g;
            float len = (float) Math.hypot(ex - ln.x0, ey - ln.y0);
            if (len < 1f) continue;
            float deg = (float) Math.toDegrees(Math.atan2(ey - ln.y0, ex - ln.x0));
            float th, a;
            if (ln.state == Hazards.FIRING) { th = 20f * (1f - ln.t / Hazards.FIRE_T * 0.4f); a = 1f; batch.setColor(1f, 0.7f, 0.2f, 0.85f); }
            else { th = 2.5f; a = ln.state == Hazards.ARMED ? 0.55f + 0.35f * (float) Math.sin(time * 30f) : 0.8f; batch.setColor(1f, 0.85f, 0.4f, a); }
            batch.draw(white, ln.x0, ln.y0 - th / 2f, 0f, th / 2f, len, th, 1f, 1f, deg, 0, 0, 2, 2, false, false);
            if (ln.state == Hazards.FIRING) {
                batch.setColor(1f, 1f, 0.85f, 1f);
                batch.draw(white, ln.x0, ln.y0 - 3f, 0f, 3f, len, 6f, 1f, 1f, deg, 0, 0, 2, 2, false, false);
                int n = (int) (len / 38f);
                for (int k = 0; k <= n; k++) {
                    float px = ln.x0 + (ex - ln.x0) * k / Math.max(1, n), py = ln.y0 + (ey - ln.y0) * k / Math.max(1, n);
                    tongue(batch, px, py, 48f, time, k, 1f);
                }
            }
        }
        if (hz.ring) drawRing(batch, hz, time, l, b, vw, vh);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1, 1, 1, 1);
    }

    /** The ash ring: a wall of flame standing on the edge of the safe floor, sampled every ~70 px and drawn where it is in view. */
    private void drawRing(SpriteBatch batch, Hazards hz, float time, float l, float b, float vw, float vh) {
        float r = hz.ringR;
        int n = Math.max(24, (int) (6.2831853f * r / 70f));
        for (int k = 0; k < n; k++) {
            float a = k * 6.2831853f / n;
            float x = hz.ringX + (float) Math.cos(a) * r, y = hz.ringY + (float) Math.sin(a) * r;
            if (x < l - 80 || x > l + vw + 80 || y < b - 80 || y > b + vh + 80) continue;
            batch.setColor(1f, 0.4f, 0.08f, 0.45f);
            batch.draw(glow, x - 90f, y - 90f, 180f, 180f);
            tongue(batch, x, y, 64f + 14f * (float) Math.sin(time * 5f + k), time, k, 0.95f);
        }
    }

    public void dispose() {
        fire.dispose();
        glow.dispose();
        white.dispose();
    }
}

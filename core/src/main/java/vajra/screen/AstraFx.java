package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import vajra.world.AstraCube;
import vajra.world.World;

/** How the astras LOOK, drawn additively so they burn rather than sit on the frame (the rite itself is vajra.world.Astra).
 *
 *  <p>While the mantra is held: a slow gold yantra (two counter-turning triangles in a ring) opens round the hull and
 *  tightens as the charge builds, and the held shards are glowing orbs on the shrinking circle, each dragging a short
 *  arc of light. Loosed, every shard is a bolt of its own element - a spinning star-diamond (Akasha), a thin gale with
 *  a pair of wind-wisps (Vayu), a guttering fireball (Agni), a teardrop with a rippling wake (Jala), a heavy stone
 *  shard shedding dust (Prithvi) - each with a tapered trail of light drawn from the positions it has flown through. */
final class AstraFx {
    private AstraFx() {}

    private static final float[] WHITE = {1f, 1f, 1f};

    /** All of it, inside an open Filled shape pass in world space. */
    static void draw(ShapeRenderer s, World w, float l, float b, float viewW, float viewH) {
        if (w.astra.isEmpty() && !w.winding()) return;
        s.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);          // additive: light, not paint
        float t = w.clock;
        if (w.winding()) yantra(s, w, t);
        for (AstraCube c : w.astra) {
            if (c.x < l - 80 || c.x > l + viewW + 80 || c.y < b - 80 || c.y > b + viewH + 80) continue;
            float[] el = World.TATVA_RGB[MathUtils.clamp(c.element, 0, World.TATVA_RGB.length - 1)];
            if (c.armed) bolt(s, c, el, t);
            else orb(s, w, c, el, t);
        }
        s.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---- the invocation ---------------------------------------------------------------------------

    /** The mandala that opens round the hull while the mantra is held: one per astra, its elements forming one by one with the charge (see Mandala). */
    private static void yantra(ShapeRenderer s, World w, float t) {
        float k = MathUtils.clamp(w.windK(), 0f, 1f);
        Mandala.draw(s, w.ship.x, w.ship.y, k, t, w.astraCharge >= vajra.world.Astra.WIND_FULL, MathUtils.clamp(w.astraKind, 0, 5));
    }

    private static void ring(ShapeRenderer s, float cx, float cy, float r, int n, float th) {
        float px = cx + r, py = cy;
        for (int i = 1; i <= n; i++) {
            float a = i * MathUtils.PI2 / n, x = cx + r * MathUtils.cos(a), y = cy + r * MathUtils.sin(a);
            s.rectLine(px, py, x, y, th);
            px = x;
            py = y;
        }
    }

    private static void triangle(ShapeRenderer s, float cx, float cy, float r, float rot, float th) {
        for (int i = 0; i < 3; i++) {
            float a0 = rot + i * MathUtils.PI2 / 3f, a1 = rot + (i + 1) * MathUtils.PI2 / 3f;
            s.rectLine(cx + r * MathUtils.cos(a0), cy + r * MathUtils.sin(a0),
                    cx + r * MathUtils.cos(a1), cy + r * MathUtils.sin(a1), th);
        }
    }

    /** A held shard: a glowing orb on the circle with a short arc of light behind it. */
    private static void orb(ShapeRenderer s, World w, AstraCube c, float[] el, float t) {
        float dx = c.x - w.ship.x, dy = c.y - w.ship.y;
        float k = MathUtils.clamp(w.windK(), 0f, 1f);
        float lift = 0.7f + 0.3f * MathUtils.sin(t * 8f + c.slot * 3f);
        for (int i = 6; i >= 1; i--) {                                    // the arc behind it: the same radius, earlier angles
            float back = -i * (0.05f + 0.07f * k), ca = MathUtils.cos(back), sa = MathUtils.sin(back);
            float x = w.ship.x + dx * ca - dy * sa, y = w.ship.y + dx * sa + dy * ca;
            s.setColor(el[0], el[1], el[2], 0.5f * (1f - i / 7f));
            s.circle(x, y, 3.4f * (1f - i / 8f), 8);
        }
        glow(s, c.x, c.y, 4.4f + 2f * k, el, lift);
    }

    // ---- the loosed bolt -------------------------------------------------------------------------

    private static void bolt(ShapeRenderer s, AstraCube c, float[] el, float t) {
        c.mark();
        float ang = MathUtils.atan2(c.vy, c.vx), ca = MathUtils.cos(ang), sa = MathUtils.sin(ang);
        float nx = -sa, ny = ca;                                         // across the flight
        int n = c.tn;
        float hw;
        switch (c.element) {
            case 1: hw = 3.2f; break;
            case 2: hw = 7f; break;
            case 4: hw = 8f; break;
            case 3: hw = 5.4f; break;
            default: hw = 5f;
        }
        // the trail: wide and faint under, narrow and hot on top, each step thinner than the last
        float px = c.x, py = c.y;
        for (int i = 0; i < n; i++) {
            float k = 1f - (i + 1f) / (n + 1f);
            float x = c.tx[i], y = c.ty[i];
            float off = 0, wide = hw * k;
            if (c.element == 3) off = MathUtils.sin(i * 1.4f - t * 20f) * 2.6f * (1f - k * 0.4f);       // the flood's wake ripples
            else if (c.element == 2) wide = hw * k * (0.8f + 0.5f * MathUtils.sin(t * 46f + i * 2.1f)); // fire gutters
            else if (c.element == 1) wide = hw * k * 0.7f;
            x += nx * off;
            y += ny * off;
            float[] cc = c.element == 2 ? ember(k) : el;
            s.setColor(cc[0], cc[1], cc[2], 0.26f * k);
            s.rectLine(px, py, x, y, wide * 2.6f + 1f);
            s.setColor(cc[0] + (1f - cc[0]) * 0.55f, cc[1] + (1f - cc[1]) * 0.55f, cc[2] + (1f - cc[2]) * 0.55f, 0.7f * k);
            s.rectLine(px, py, x, y, wide * 0.9f + 0.4f);
            px = x;
            py = y;
            if (c.element == 4 && i % 3 == 1) {                           // stone sheds dust
                s.setColor(0.8f, 0.7f, 0.5f, 0.3f * k);
                s.circle(x + nx * MathUtils.sin(i * 5f) * 4f, y + ny * MathUtils.sin(i * 5f) * 4f, 2f * k + 0.6f, 6);
            }
        }
        // the head
        glow(s, c.x, c.y, hw * 2.2f, el, 0.8f);
        switch (c.element) {
            case 0: { // Akasha: a spinning star-diamond that twinkles
                float spin = t * 6f + c.slot;
                diamond(s, c.x, c.y, hw * 1.7f, hw * 0.8f, spin, el, WHITE);
                float tw = 0.5f + 0.5f * MathUtils.sin(t * 17f + c.slot);
                s.setColor(1f, 1f, 1f, 0.8f * tw);
                s.rectLine(c.x - hw * 2.6f, c.y, c.x + hw * 2.6f, c.y, 1f);
                s.rectLine(c.x, c.y - hw * 2.6f, c.x, c.y + hw * 2.6f, 1f);
                break;
            }
            case 1: { // Vayu: a slim arrowhead with two wisps wheeling round it
                diamond(s, c.x, c.y, hw * 2.6f, hw * 0.7f, ang, el, WHITE);
                for (int i = 0; i < 2; i++) {
                    float a = t * 30f + i * MathUtils.PI;
                    s.setColor(el[0], el[1], el[2], 0.7f);
                    s.circle(c.x + ca * MathUtils.cos(a) * 1.5f + nx * MathUtils.sin(a) * 6f,
                            c.y + sa * MathUtils.cos(a) * 1.5f + ny * MathUtils.sin(a) * 6f, 1.6f, 6);
                }
                break;
            }
            case 2: { // Agni: a fireball with flickering tongues thrown back
                float[] hot = {1f, 0.86f, 0.5f};
                s.setColor(1f, 0.4f, 0.08f, 0.9f);
                s.circle(c.x, c.y, hw * (0.95f + 0.1f * MathUtils.sin(t * 40f)), 12);
                for (int i = -1; i <= 1; i++) {
                    float fl = 1f + 0.4f * MathUtils.sin(t * 33f + i * 2f + c.slot);
                    s.triangle(c.x + nx * i * hw * 0.7f, c.y + ny * i * hw * 0.7f,
                            c.x + nx * i * hw * 0.2f - ca * hw * 2.2f * fl, c.y + ny * i * hw * 0.2f - sa * hw * 2.2f * fl,
                            c.x + nx * (i * hw * 0.7f + hw * 0.5f), c.y + ny * (i * hw * 0.7f + hw * 0.5f));
                }
                s.setColor(hot[0], hot[1], hot[2], 1f);
                s.circle(c.x, c.y, hw * 0.5f, 10);
                break;
            }
            case 3: { // Jala: a teardrop, round at the front, drawn out behind
                s.setColor(el[0], el[1], el[2], 0.95f);
                s.circle(c.x + ca * hw * 0.3f, c.y + sa * hw * 0.3f, hw * 0.9f, 12);
                s.triangle(c.x + nx * hw * 0.85f, c.y + ny * hw * 0.85f, c.x - nx * hw * 0.85f, c.y - ny * hw * 0.85f,
                        c.x - ca * hw * 2.4f, c.y - sa * hw * 2.4f);
                s.setColor(1f, 1f, 1f, 0.85f);
                s.circle(c.x + ca * hw * 0.5f + nx * hw * 0.3f, c.y + sa * hw * 0.5f + ny * hw * 0.3f, hw * 0.28f, 6);
                break;
            }
            default: { // Prithvi: a stone shard, heavy and angular, tumbling slowly
                float spin = ang + MathUtils.sin(t * 5f + c.slot) * 0.4f;
                diamond(s, c.x, c.y, hw * 1.5f, hw * 1.05f, spin, new float[] {0.62f, 0.46f, 0.16f}, el);
                break;
            }
        }
    }

    private static float[] ember(float k) {
        return new float[] {1f, 0.25f + 0.45f * k, 0.05f + 0.15f * k};
    }

    /** A soft bloom: three stacked discs, the last white-hot. */
    private static void glow(ShapeRenderer s, float x, float y, float r, float[] el, float a) {
        s.setColor(el[0], el[1], el[2], 0.14f * a);
        s.circle(x, y, r * 2.1f, 16);
        s.setColor(el[0], el[1], el[2], 0.3f * a);
        s.circle(x, y, r * 1.3f, 14);
        s.setColor(el[0] + (1f - el[0]) * 0.5f, el[1] + (1f - el[1]) * 0.5f, el[2] + (1f - el[2]) * 0.5f, 0.8f * a);
        s.circle(x, y, r * 0.62f, 12);
        s.setColor(1f, 1f, 1f, a);
        s.circle(x, y, r * 0.28f, 8);
    }

    /** A four-point kite: `len` along its axis each way, `wid` across, filled in `body` with a `rim` core. */
    private static void diamond(ShapeRenderer s, float x, float y, float len, float wid, float rot, float[] body, float[] rim) {
        float ca = MathUtils.cos(rot), sa = MathUtils.sin(rot);
        s.setColor(body[0], body[1], body[2], 0.95f);
        s.triangle(x + ca * len, y + sa * len, x - sa * wid, y + ca * wid, x + sa * wid, y - ca * wid);
        s.triangle(x - ca * len * 0.8f, y - sa * len * 0.8f, x - sa * wid, y + ca * wid, x + sa * wid, y - ca * wid);
        s.setColor(rim[0], rim[1], rim[2], 0.85f);
        s.triangle(x + ca * len * 0.6f, y + sa * len * 0.6f, x - sa * wid * 0.4f, y + ca * wid * 0.4f, x + sa * wid * 0.4f, y - ca * wid * 0.4f);
    }
}

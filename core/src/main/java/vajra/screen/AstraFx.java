package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
    private static final Color tint = new Color(), tint2 = new Color();

    /** All of it, inside an open Filled shape pass in world space. */
    static void draw(ShapeRenderer s, World w, float l, float b, float viewW, float viewH) {
        if (w.astra.isEmpty() && !w.winding() && w.gazeT <= 0f && !w.flow.active() && !w.maya.active) return;
        s.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);          // additive: light, not paint
        float t = w.clock;
        if (w.winding()) yantra(s, w, t);
        if (w.maya.active && w.maya.tiltTime() < 0.3f) { s.flush(); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);                  // paint, not light: blood reads on any ground, so it is drawn over a darkening
                s.setColor(0.03f, 0f, 0.02f, 0.45f * w.maya.form()); s.circle(w.maya.x, w.maya.y, Mandala.balaRadius(w.maya.form()) * 1.22f, 56);
                Mandala.drawBala(s, w.maya.x, w.maya.y, w.maya.form(), t, w.maya.form() >= 1f);
                s.flush(); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE); }   // Bala's mandala, whose heart the glass cracks from
        float scale = 1f + 0.1f * vajra.world.Astra.TIER[MathUtils.clamp(w.astraKind, 0, vajra.world.Astra.KINDS - 1)];   // the deeper the astra, the greater its shards
        if (w.gazeT > 0f) gaze(s, w, t);
        if (w.flow.kind == vajra.world.AstraFlow.VAYU) funnel(s, w, t);
        if (w.flow.kind == vajra.world.AstraFlow.LIGHTNING) lightning(s, w, t);
        if (w.flow.kind == vajra.world.AstraFlow.LOTUS && w.flow.ship != null) {                // the mandala STANDS while the shower lasts, and the arrows fly
            if (w.flow.t < vajra.world.AstraFlow.SHOW_SECONDS) Mandala.draw(s, w.flow.ship.x, w.flow.ship.y, 1f, t, true, vajra.world.Astra.BRAHMA);
            arrows(s, w, l, b, viewW, viewH);
        }
        if (w.flow.kind == vajra.world.AstraFlow.BOULDER && w.flow.ship != null)             // the mandala STAYS while the rocks are thrown, pulsing
            Mandala.draw(s, w.flow.ship.x, w.flow.ship.y, 1f, t, true, vajra.world.Astra.PRITHVI);
        boolean hush = w.winding() && w.astraKind == vajra.world.Astra.BRAHMA;                // Brahma is forged in lines and one light: no glowing shards
        for (AstraCube c : w.astra) {
            if (hush && !c.armed) continue;
            if (c.x < l - 80 || c.x > l + viewW + 80 || c.y < b - 80 || c.y > b + viewH + 80) continue;
            float[] el = World.TATVA_RGB[MathUtils.clamp(c.element, 0, World.TATVA_RGB.length - 1)];
            if (c.armed) bolt(s, c, el, t, scale);
            else orb(s, w, c, el, t, scale);
        }
        s.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---- the invocation ---------------------------------------------------------------------------

    /** The mandala that opens round the hull while the mantra is held: one per astra, its elements forming one by one with the charge (see Mandala). */
    private static void yantra(ShapeRenderer s, World w, float t) {
        float k = MathUtils.clamp(w.windK(), 0f, 1f);
        Mandala.draw(s, w.ship.x, w.ship.y, k, t, w.astraCharge >= vajra.world.Astra.WIND_FULL, MathUtils.clamp(w.astraKind, 0, vajra.world.Astra.KINDS - 1));
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
    private static void orb(ShapeRenderer s, World w, AstraCube c, float[] el, float t, float scale) {
        float dx = c.x - w.ship.x, dy = c.y - w.ship.y;
        float k = MathUtils.clamp(w.windK(), 0f, 1f);
        float lift = 0.7f + 0.3f * MathUtils.sin(t * 8f + c.slot * 3f);
        float light = Mandala.light(k);                                    // glow and trails are the light of the charge: they come late
        for (int i = 6; i >= 1 && light > 0.02f; i--) {                   // the arc behind it: the same radius, earlier angles
            float back = -i * (0.05f + 0.07f * k), ca = MathUtils.cos(back), sa = MathUtils.sin(back);
            float x = w.ship.x + dx * ca - dy * sa, y = w.ship.y + dx * sa + dy * ca;
            s.setColor(el[0], el[1], el[2], 0.5f * (1f - i / 7f) * light);
            s.circle(x, y, 3.4f * (1f - i / 8f), 8);
        }
        float tang = MathUtils.atan2(dy, dx) + MathUtils.HALF_PI;                                      // heading round the circle
        shapedHead(s, c.x, c.y, tang, c.element, headSize(c.element) * scale * (0.75f + 0.45f * k), t, c.slot, el, light);   // the held shard wears its true shape
    }

    // ---- the loosed bolt -------------------------------------------------------------------------

    private static void bolt(ShapeRenderer s, AstraCube c, float[] el, float t, float scale) {
        c.mark();
        float ang = MathUtils.atan2(c.vy, c.vx), ca = MathUtils.cos(ang), sa = MathUtils.sin(ang);
        float nx = -sa, ny = ca;                                         // across the flight
        int n = c.tn;
        float hw = headSize(c.element) * scale;
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
        shapedHead(s, c.x, c.y, ang, c.element, hw, t, c.slot, el, 1f);
    }

    /** The head of a shard, shaped for its element - in flight and while it is held on the mandala alike: a spinning star-diamond (Akasha), a slim arrowhead
     *  with wheeling wisps (Vayu), a guttering fireball (Agni), a teardrop (Jala), a tumbling stone (Prithvi). `ang` is its heading, `hw` its size. */
    private static void shapedHead(ShapeRenderer s, float x, float y, float ang, int element, float hw, float t, float slot, float[] el, float light) {
        float ca = MathUtils.cos(ang), sa = MathUtils.sin(ang), nx = -sa, ny = ca;
        if (light > 0.02f) glow(s, x, y, hw * 2.2f, el, 0.8f * light);   // the bloom round a shard is light: only once the charge is high
        switch (element) {
            case 0: { // Akasha: a spinning star-diamond that twinkles
                float spin = t * 6f + slot;
                diamond(s, x, y, hw * 1.7f, hw * 0.8f, spin, el, WHITE);
                float tw = 0.5f + 0.5f * MathUtils.sin(t * 17f + slot);
                s.setColor(1f, 1f, 1f, 0.8f * tw);
                s.rectLine(x - hw * 2.6f, y, x + hw * 2.6f, y, 1f);
                s.rectLine(x, y - hw * 2.6f, x, y + hw * 2.6f, 1f);
                break;
            }
            case 1: { // Vayu: a slim arrowhead with two wisps wheeling round it
                diamond(s, x, y, hw * 2.6f, hw * 0.7f, ang, el, WHITE);
                for (int i = 0; i < 2; i++) {
                    float a = t * 30f + i * MathUtils.PI;
                    s.setColor(el[0], el[1], el[2], 0.7f);
                    s.circle(x + ca * MathUtils.cos(a) * 1.5f + nx * MathUtils.sin(a) * 6f,
                            y + sa * MathUtils.cos(a) * 1.5f + ny * MathUtils.sin(a) * 6f, 1.6f, 6);
                }
                break;
            }
            case 2: { // Agni: a fireball with flickering tongues thrown back
                float[] hot = {1f, 0.86f, 0.5f};
                s.setColor(1f, 0.4f, 0.08f, 0.9f);
                s.circle(x, y, hw * (0.95f + 0.1f * MathUtils.sin(t * 40f)), 12);
                for (int i = -1; i <= 1; i++) {
                    float fl = 1f + 0.4f * MathUtils.sin(t * 33f + i * 2f + slot);
                    s.triangle(x + nx * i * hw * 0.7f, y + ny * i * hw * 0.7f,
                            x + nx * i * hw * 0.2f - ca * hw * 2.2f * fl, y + ny * i * hw * 0.2f - sa * hw * 2.2f * fl,
                            x + nx * (i * hw * 0.7f + hw * 0.5f), y + ny * (i * hw * 0.7f + hw * 0.5f));
                }
                s.setColor(hot[0], hot[1], hot[2], 1f);
                s.circle(x, y, hw * 0.5f, 10);
                break;
            }
            case 3: { // Jala: a teardrop, round at the front, drawn out behind
                s.setColor(el[0], el[1], el[2], 0.95f);
                s.circle(x + ca * hw * 0.3f, y + sa * hw * 0.3f, hw * 0.9f, 12);
                s.triangle(x + nx * hw * 0.85f, y + ny * hw * 0.85f, x - nx * hw * 0.85f, y - ny * hw * 0.85f,
                        x - ca * hw * 2.4f, y - sa * hw * 2.4f);
                s.setColor(1f, 1f, 1f, 0.85f);
                s.circle(x + ca * hw * 0.5f + nx * hw * 0.3f, y + sa * hw * 0.5f + ny * hw * 0.3f, hw * 0.28f, 6);
                break;
            }
            default: { // Prithvi: a stone shard, heavy and angular, tumbling slowly
                float spin = ang + MathUtils.sin(t * 5f + slot) * 0.4f;
                diamond(s, x, y, hw * 1.5f, hw * 1.05f, spin, new float[] {0.62f, 0.46f, 0.16f}, el);
                break;
            }
        }
    }

    /** THE TORNADO'S BODY: three tapered spiral arms turning round the hull (drawn smooth, as light), a faint outer ring, a ring at the eye, and on the throw
     *  rings of wind running out. Strength follows the beats: it builds through the draw, holds at the eye, and thins as everything is thrown. */
    private static void funnel(ShapeRenderer s, World w, float t) {
        vajra.world.AstraFlow f = w.flow;
        if (f.ship == null) return;
        float R = f.radius, cx = f.ship.x, cy = f.ship.y;
        float a = f.t < vajra.world.AstraFlow.T_EYE ? MathUtils.clamp(f.t / 0.5f, 0f, 1f) : MathUtils.clamp(1f - (f.t - vajra.world.AstraFlow.T_EYE) / 0.9f, 0f, 1f);
        if (a <= 0.01f) return;
        float sc = R / 450f;
        for (int arm = 0; arm < 3; arm++) {
            float px = 0f, py = 0f;
            for (int i = 0; i <= 40; i++) {
                float u = i / 40f, r = R * (0.95f - 0.83f * u), th = -f.t * 2.4f + arm * MathUtils.PI2 / 3f + u * 3.4f;
                float x = cx + r * MathUtils.cos(th), y = cy + r * MathUtils.sin(th), wd = (3f + 9f * (float) Math.sin(Math.PI * u)) * sc;
                if (i > 0) {
                    s.setColor(0.58f, 0.66f, 0.78f, 0.10f * a);
                    s.rectLine(px, py, x, y, wd * 2.4f);
                    s.setColor(0.86f, 0.90f, 0.97f, 0.30f * a * (0.5f + 0.5f * u));
                    s.rectLine(px, py, x, y, Math.max(1f, wd * 0.45f));
                }
                px = x;
                py = y;
            }
        }
        s.setColor(0.70f, 0.78f, 0.88f, 0.18f * a);
        ring(s, cx, cy, R * 0.97f, 72, 2f);
        ring(s, cx, cy, R * 0.12f, 28, 2f);
        if (f.t >= vajra.world.AstraFlow.T_EYE)                            // the throw: rings of wind running outward
            for (int i = 0; i < 3; i++) {
                float ph = ((f.t - vajra.world.AstraFlow.T_EYE) * 1.4f + i / 3f) % 1f;
                s.setColor(0.84f, 0.89f, 0.96f, 0.5f * (1f - ph) * a);
                ring(s, cx, cy, R * (0.15f + 0.9f * ph), 72, 3f);
            }
    }

    // ---- AKASHA: the circular lightning ------------------------------------------------------------------------------------------------------

    /** A deterministic 0..1 from two ints (the jag of a bolt must not use a random generator: it is re-forked from its seed every 50 ms). */
    private static float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** One bolt: a jagged line in three layers (a wide violet glow, a mid layer, a white core) with a short fork from its middle. */
    private static void boltLine(ShapeRenderer s, float x0, float y0, float x1, float y1, int seed, boolean big, float alpha, int depth) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 4f) return;
        float nx = -dy / len, ny = dx / len, amp = len * (big ? 0.09f : 0.07f);
        int n = Math.max(5, Math.min(16, (int) (len / 28f)));
        float px = x0, py = y0;
        for (int i = 1; i <= n; i++) {
            float u = i / (float) n, off = i == n ? 0f : (hash(seed, i) - 0.5f) * 2f * amp * (float) Math.sin(Math.PI * u);
            float x = x0 + dx * u + nx * off, y = y0 + dy * u + ny * off;
            float w = big ? 1.4f : 1f;
            s.setColor(0.70f, 0.50f, 1f, 0.16f * alpha);
            s.rectLine(px, py, x, y, 9f * w);
            s.setColor(0.86f, 0.76f, 1f, 0.50f * alpha);
            s.rectLine(px, py, x, y, 3.8f * w);
            s.setColor(1f, 1f, 1f, 0.95f * alpha);
            s.rectLine(px, py, x, y, 1.5f * w);
            if (depth == 0 && i == n / 2 && hash(seed, 99) < 0.7f) {       // a fork from the middle, running off at an angle
                float fa = (hash(seed, 77) - 0.5f) * 1.4f, ca = MathUtils.cos(fa), sa = MathUtils.sin(fa), fl = len * 0.28f;
                float ux = dx / len, uy = dy / len;
                boltLine(s, x, y, x + (ux * ca - uy * sa) * fl, y + (ux * sa + uy * ca) * fl, seed + 31, false, alpha * 0.8f, 1);
            }
            px = x;
            py = y;
        }
    }

    /** The light of the lightning: a violet flash at the hull and a faint ring of glow with each wave (the bolts themselves are sprites: FlowFx). */
    private static void lightning(ShapeRenderer s, World w, float t) {
        vajra.world.AstraFlow f = w.flow;
        if (f.ship == null) return;
        float fade = 1f - MathUtils.clamp(f.t / 0.3f, 0f, 1f);                // a violet flash at the hull as it begins
        if (fade > 0f) { s.setColor(0.85f, 0.75f, 1f, 0.12f * fade); s.circle(f.ship.x, f.ship.y, f.radius * 0.35f, 32); }
    }

    /** BRAHMA's arrows, drawn like a real arrow: a long thin shaft in the element's colour with a white core and two bands near the head, a small barbed leaf of a head, and three
     *  feathers at the tail, each a row of angled barbs; a faint trail behind. */
    private static void arrows(ShapeRenderer s, World w, float l, float b, float vw, float vh) {
        for (vajra.world.AstraFlow.Arrow ar : w.flow.arrows) {
            if (ar.x < l - 400 || ar.x > l + vw + 400 || ar.y < b - 400 || ar.y > b + vh + 400) continue;
            float[] c = World.TATVA_RGB[ar.el];
            float dx = MathUtils.cos(ar.ang), dy = MathUtils.sin(ar.ang), nx = -dy, ny = dx, x = ar.x, y = ar.y;
            for (int j = 0; j < 6; j++) {                                    // a faint trail
                float f = 1f - j / 6f, d0 = 100f + j * 24f;
                s.setColor(c[0], c[1], c[2], 0.22f * f);
                s.rectLine(x - dx * d0, y - dy * d0, x - dx * (d0 + 24f), y - dy * (d0 + 24f), 1.6f * f + 0.5f);
            }
            s.setColor(c[0], c[1], c[2], 0.20f);                             // the shaft's glow, then the shaft, then its white core
            s.rectLine(x - dx * 92f, y - dy * 92f, x - dx * 6f, y - dy * 6f, 5f);
            s.setColor(c[0], c[1], c[2], 1f);
            s.rectLine(x - dx * 92f, y - dy * 92f, x - dx * 6f, y - dy * 6f, 1.9f);
            s.setColor(1f, 1f, 1f, 0.9f);
            s.rectLine(x - dx * 90f, y - dy * 90f, x - dx * 8f, y - dy * 8f, 0.7f);
            for (int band = 0; band < 2; band++) {                           // two bands near the head
                float d0 = 18f + band * 8f;
                s.setColor(1f, 1f, 1f, 0.95f);
                s.rectLine(x - dx * d0 + nx * 2.6f, y - dy * d0 + ny * 2.6f, x - dx * d0 - nx * 2.6f, y - dy * d0 - ny * 2.6f, 1.4f);
            }
            s.setColor(c[0], c[1], c[2], 1f);                                // the head: a small leaf with two barbs
            s.triangle(x + dx * 16f, y + dy * 16f, x - dx * 6f + nx * 4.6f, y - dy * 6f + ny * 4.6f, x - dx * 6f - nx * 4.6f, y - dy * 6f - ny * 4.6f);
            s.setColor(1f, 1f, 1f, 0.9f);
            s.triangle(x + dx * 13f, y + dy * 13f, x - dx * 2f + nx * 1.8f, y - dy * 2f + ny * 1.8f, x - dx * 2f - nx * 1.8f, y - dy * 2f - ny * 1.8f);
            s.setColor(c[0], c[1], c[2], 0.95f);
            for (int side = -1; side <= 1; side += 2) {                      // the feathers: barbs raked back from the shaft, three pairs along the tail
                for (int m = 0; m < 7; m++) {
                    float d0 = 96f - m * 4.6f, len = 9f - Math.abs(m - 3) * 0.9f;
                    s.rectLine(x - dx * d0, y - dy * d0, x - dx * (d0 + 7f) + nx * len * side, y - dy * (d0 + 7f) + ny * len * side, 1.1f);
                }
            }
            s.setColor(1f, 1f, 1f, 0.8f);
            for (int side = -1; side <= 1; side += 2)                        // the vane's edge
                s.rectLine(x - dx * 94f + nx * 5f * side, y - dy * 94f + ny * 5f * side, x - dx * 66f + nx * 7f * side, y - dy * 66f + ny * 7f * side, 0.6f);
        }
    }

    private static float headSize(int element) {
        switch (element) {
            case 1: return 3.2f;
            case 2: return 7f;
            case 4: return 8f;
            case 3: return 5.4f;
            default: return 5f;
        }
    }

    /** THE THIRD EYE'S BEAM: an almond eye opens over the hull and a beam burns from it along the nose - a hot white core, a gold body, a vermilion edge, rings of
     *  light running down its length and ash-white flares, wider and brighter at its root. It ignores rock, so it is drawn straight through it. */
    private static void gaze(ShapeRenderer s, World w, float t) {
        vajra.ship.Ship g = w.gazeShip;
        if (g == null) return;
        float open = MathUtils.clamp((vajra.world.Astra.GAZE_TIME - w.gazeT) / 0.3f, 0f, 1f) * MathUtils.clamp(w.gazeT / 0.4f, 0f, 1f);   // opens fast, closes slowly
        float ca = MathUtils.cos(g.facing), sa = MathUtils.sin(g.facing), nx = -sa, ny = ca;
        float len = vajra.world.Astra.GAZE_LEN * (0.3f + 0.7f * MathUtils.clamp((vajra.world.Astra.GAZE_TIME - w.gazeT) / 0.25f, 0f, 1f));
        float half = vajra.world.Astra.GAZE_HALF * (1f + 0.1f * vajra.world.Astra.TIER[vajra.world.Astra.PASHUPATA]) * (0.85f + 0.15f * MathUtils.sin(t * 40f));
        float x0 = g.x + ca * 18f, y0 = g.y + sa * 18f, x1 = g.x + ca * len, y1 = g.y + sa * len;
        // the body: seven tapered layers, wide and faint to narrow and hot, each a quad whose colour fades from the root to the far end (no seams)
        float rootW = half * 1.3f, tipW = half * 0.8f;
        for (int l = 0; l < 8; l++) {
            float f = l / 7f, k = 3.6f - 3.1f * f;
            Color c0 = tint.set(1f, 0.20f + 0.52f * f, 0.05f + 0.36f * f, (0.09f + 0.07f * f) * open);
            Color c1 = tint2.set(c0.r, c0.g, c0.b, c0.a * 0.25f);
            float rx = -ny * 0f + nx * rootW * k * 0.5f, ry = ny * rootW * k * 0.5f, tx = nx * tipW * k * 0.5f, ty = ny * tipW * k * 0.5f;
            s.triangle(x0 + rx, y0 + ry, x0 - rx, y0 - ry, x1 + tx, y1 + ty, c0, c0, c1);
            s.triangle(x0 - rx, y0 - ry, x1 - tx, y1 - ty, x1 + tx, y1 + ty, c0, c1, c1);
        }
        {
            Color c0 = tint.set(1f, 1f, 1f, 0.95f * open), c1 = tint2.set(1f, 0.95f, 0.8f, 0.2f * open);
            float rx = nx * rootW * 0.2f, ry = ny * rootW * 0.2f, tx = nx * tipW * 0.2f, ty = ny * tipW * 0.2f;      // the hot white core
            s.triangle(x0 + rx, y0 + ry, x0 - rx, y0 - ry, x1 + tx, y1 + ty, c0, c0, c1);
            s.triangle(x0 - rx, y0 - ry, x1 - tx, y1 - ty, x1 + tx, y1 + ty, c0, c1, c1);
        }
        final int segs = 28;
        for (int strand = 0; strand < 2; strand++) {                      // two strands of light twisting round the core
            float px = 0f, py = 0f;
            for (int i = 0; i <= segs * 2; i++) {
                float u = i / (float) (segs * 2), off = half * 1.1f * MathUtils.sin(u * 22f - t * 13f + strand * MathUtils.PI) * (1f - 0.5f * u);
                float x = x0 + (x1 - x0) * u + nx * off, y = y0 + (y1 - y0) * u + ny * off;
                if (i > 0) {
                    s.setColor(1f, 0.82f, 0.45f, 0.75f * (1f - 0.7f * u) * open);
                    s.rectLine(px, py, x, y, 1.7f);
                }
                px = x;
                py = y;
            }
        }
        for (int i = 0; i < 9; i++) {                                    // rings of light running out along the beam
            float ph = (t * 1.6f + i / 9f) % 1f, d = 30f + ph * (len - 30f), wd = half * (1.1f + 0.5f * ph);
            s.setColor(1f, 0.8f, 0.45f, 0.5f * (1f - ph) * open);
            s.rectLine(g.x + ca * d - nx * wd, g.y + sa * d - ny * wd, g.x + ca * d + nx * wd, g.y + sa * d + ny * wd, 2f);
        }
        for (int i = 0; i < 12; i++) {                                   // ash-white flares thrown off the edges
            float ph = (t * 2.2f + i * 0.0833f) % 1f, d = ph * len, side = (i % 2 == 0 ? 1f : -1f);
            s.setColor(1f, 0.9f, 0.7f, 0.7f * (1f - ph) * open);
            s.circle(g.x + ca * d + nx * side * (half * (0.9f + ph)), g.y + sa * d + ny * side * (half * (0.9f + ph)), 2.2f * (1f - ph) + 0.6f, 6);
        }
        // the eye over the hull: an almond, an iris, a red pupil
        float grand = 1f + 0.1f * vajra.world.Astra.TIER[vajra.world.Astra.PASHUPATA];
        float ew = 38f * grand * open, eh = 15f * grand * open;
        s.setColor(1f, 0.82f, 0.5f, 0.9f * open);
        for (int side = -1; side <= 1; side += 2) {                       // the almond lies across the beam, its two lids bowing along it
            float lx = 0f, ly = 0f;
            for (int i = 0; i <= 14; i++) {
                float u = i / 14f, across = -ew + 2f * ew * u, lid = side * eh * 4f * u * (1f - u);
                float x = g.x + nx * across + ca * lid, y = g.y + ny * across + sa * lid;
                if (i > 0) s.rectLine(lx, ly, x, y, 2f);
                lx = x;
                ly = y;
            }
        }
        s.setColor(1f, 0.3f, 0.15f, 0.9f * open);
        s.circle(g.x, g.y, 7f * open, 12);
        s.setColor(1f, 1f, 1f, 0.95f * open);
        s.circle(g.x, g.y, 2.6f * open, 8);
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

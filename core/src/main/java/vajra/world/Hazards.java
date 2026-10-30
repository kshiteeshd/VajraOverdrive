package vajra.world;

import java.util.ArrayList;

/** GROUND THAT BURNS and LINES THAT IGNITE: the two things Vitala's enemies and boss leave on the floor (docs/VITALA_ENEMIES.md).
 *  <ul>
 *   <li>a <b>patch</b> (Jvala the Cinder, Agneya's trail, Bhairava's ash) is a disc of fire that stands for a few seconds and scalds a hull inside it;
 *       they pile up, which is how Jvala kills by real estate;</li>
 *   <li>a <b>line</b> (Rekha) is a hairline drawn between two points: it GROWS from one end (drawing), WAITS as a thin gold thread (armed), then
 *       IGNITES all at once for a moment and burns whatever stands on it. Telegraph by geometry.</li>
 *  </ul>
 *  Pure data and rules: no GL, so the headless tests can pin them. */
public final class Hazards {
    public static final int MAX_PATCHES = 60;
    public static final float DRAW_T = 0.7f, ARMED_T = 1.0f, FIRE_T = 0.45f;
    public static final int DRAWING = 0, ARMED = 1, FIRING = 2;
    public static final float PATCH_DAMAGE = 7f, LINE_DAMAGE = 16f, LINE_HALF = 14f;

    /** Burning ground: centre, radius, seconds left of its life, and its life at birth. */
    public static final class Patch {
        public float x, y, r, life, max;
    }

    /** A hairline of fire from (x0, y0) to (x1, y1) and where it is in its three beats. */
    public static final class Line {
        public float x0, y0, x1, y1, t;
        public int state;
        boolean hit;
    }

    public final ArrayList<Patch> patches = new ArrayList<>();
    public final ArrayList<Line> lines = new ArrayList<>();
    private float hurtT;
    /** THE ASH RING (Bhairava): while on, the floor beyond ringR of (ringX, ringY) burns - the safe arena the fight shrinks. */
    public boolean ring;
    public float ringX, ringY, ringR;
    public static final float RING_DEPTH = 700f, RING_DAMAGE = 9f;

    public Patch patch(float x, float y, float r, float life) {
        if (patches.size() >= MAX_PATCHES) patches.remove(0);          // the oldest ground cools first
        Patch p = new Patch();
        p.x = x;
        p.y = y;
        p.r = r;
        p.life = p.max = life;
        patches.add(p);
        return p;
    }

    public Line line(float x0, float y0, float x1, float y1) {
        Line l = new Line();
        l.x0 = x0;
        l.y0 = y0;
        l.x1 = x1;
        l.y1 = y1;
        lines.add(l);
        return l;
    }

    /** How much of the line is drawn so far (0..1): it grows from its first end during DRAWING, then it is whole. */
    public static float grown(Line l) {
        return l.state == DRAWING ? Math.min(1f, l.t / DRAW_T) : 1f;
    }

    /** Distance from a point to the drawn part of the line. */
    public static float distance(Line l, float px, float py) {
        float g = grown(l);
        float ex = l.x0 + (l.x1 - l.x0) * g, ey = l.y0 + (l.y1 - l.y0) * g;
        float dx = ex - l.x0, dy = ey - l.y0, len2 = dx * dx + dy * dy;
        float u = len2 < 1e-3f ? 0f : Math.max(0f, Math.min(1f, ((px - l.x0) * dx + (py - l.y0) * dy) / len2));
        float cx = l.x0 + dx * u, cy = l.y0 + dy * u;
        return (float) Math.hypot(px - cx, py - cy);
    }

    /** Is this point standing in fire right now (a patch, or a line while it fires). */
    public boolean burns(float x, float y, float radius) {
        if (ring) {
            float rx = x - ringX, ry = y - ringY, d = (float) Math.sqrt(rx * rx + ry * ry);
            if (d > ringR && d < ringR + RING_DEPTH) return true;
        }
        for (int i = 0; i < patches.size(); i++) {
            Patch p = patches.get(i);
            float dx = x - p.x, dy = y - p.y, rr = p.r + radius;
            if (dx * dx + dy * dy < rr * rr) return true;
        }
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.state == FIRING && distance(l, x, y) < LINE_HALF + radius) return true;
        }
        return false;
    }

    public void update(World w, float dt) {
        for (int i = patches.size() - 1; i >= 0; i--) {
            Patch p = patches.get(i);
            p.life -= dt;
            if (p.life <= 0f) patches.remove(i);
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            Line l = lines.get(i);
            l.t += dt;
            if (l.state == DRAWING && l.t >= DRAW_T) { l.state = ARMED; l.t = 0f; }
            else if (l.state == ARMED && l.t >= ARMED_T) {
                l.state = FIRING;
                l.t = 0f;
                w.addEffect(Effect.flash(l.x0, l.y0, l.x1, l.y1, 1f, 0.8f, 0.3f));
            } else if (l.state == FIRING && l.t >= FIRE_T) lines.remove(i);
        }
        hurtT = Math.max(0f, hurtT - dt);
        if (ring && hurtT <= 0f) {
            float rx = w.ship.x - ringX, ry = w.ship.y - ringY, d = (float) Math.sqrt(rx * rx + ry * ry);
            if (d > ringR && d < ringR + RING_DEPTH) {
                w.ship.burn(RING_DAMAGE, w);
                hurtT = 0.4f;
            }
        }
        if (w.quietT > 0f || (patches.isEmpty() && lines.isEmpty())) return;
        // the hull: scalded every 0.4 s while it stands in a patch, once per ignition of a line it is on
        float hx = w.ship.x, hy = w.ship.y;
        for (int i = 0; i < patches.size() && hurtT <= 0f; i++) {
            Patch p = patches.get(i);
            float dx = hx - p.x, dy = hy - p.y;
            if (dx * dx + dy * dy < (p.r + 8f) * (p.r + 8f)) {
                w.ship.burn(PATCH_DAMAGE, w);
                hurtT = 0.4f;
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.state == FIRING && !l.hit && distance(l, hx, hy) < LINE_HALF + 8f) {
                l.hit = true;
                w.ship.burn(LINE_DAMAGE, w);
            }
        }
    }
}

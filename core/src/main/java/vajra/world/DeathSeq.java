package vajra.world;

import vajra.ship.Ship;

import java.util.Random;

/** THE HULL'S DEATH, as a scene (no Gdx in it): the pilot's hull does not blink from one place to another. It BREAKS UP where it was hit - the whole of it a drift of light - while the
 *  camera, held on the spot for a breath, leaves it and travels to the lamp the pilot wakes at; there the light gathers out of the air, closing on one point, and the hull is whole again.
 *
 *  <pre>
 *  0 ........ the hull comes apart (a burst, then a slow drift, the pieces fading by 2.2 s); the controls are dead, the hull is beyond harm and not drawn
 *  1.3 ...... the camera lets go of the wreck and travels to the lamp, 1.8 s
 *  3.0 ...... the hull is set at the lamp (still not drawn); the light gathers to it from a ring
 *  3.9 ...... the hull is whole again: a ring, a burst of light; the pilot has the controls back, with 2 s of grace
 *  4.4 ...... over
 *  </pre>
 *  The hull's own numbers (a life spent, the combo broken, hull restored) are the world's: Ship.damage does them at the moment of death, so a save written in the middle never holds a dead pilot. */
public final class DeathSeq {
    public static final float PAN_AT = 1.3f, PAN_LEN = 1.8f, GATHER_AT = 3.0f, APPEAR_AT = 3.9f, END = 4.4f;
    public static final int CAP = 520;

    public boolean active;
    public float t;
    public float fromX, fromY, toX, toY, facing;
    private boolean placed, appeared;

    // the light: position, velocity, age, life, size, colour; mode 0 drifts, 1 is drawn in to (tx, ty) from (sx, sy)
    public final float[] x = new float[CAP], y = new float[CAP], vx = new float[CAP], vy = new float[CAP], age = new float[CAP], life = new float[CAP], size = new float[CAP];
    public final float[] r = new float[CAP], g = new float[CAP], b = new float[CAP], sx = new float[CAP], sy = new float[CAP], swirl = new float[CAP];
    public final byte[] mode = new byte[CAP];
    public int n;
    private final Random rng = new Random(77);

    private static final float[][] PALETTE = {{0.55f, 0.85f, 1f}, {0.92f, 0.98f, 1f}, {0.35f, 0.62f, 1f}, {1f, 0.9f, 0.62f}, {0.7f, 0.95f, 1f}};

    /** The hull has been struck down at (x, y), facing `face`; it will wake at (ax, ay). */
    public void begin(float x, float y, float face, float ax, float ay) {
        active = true;
        t = 0f;
        fromX = x;
        fromY = y;
        toX = ax;
        toY = ay;
        facing = face;
        placed = appeared = false;
        n = 0;
        float ca = (float) Math.cos(face), sa = (float) Math.sin(face);
        for (int i = 0; i < 300; i++) {                                    // the hull's own shape: a long diamond, nose forward
            float u = rng.nextFloat() * 2f - 1f, v = (rng.nextFloat() * 2f - 1f) * (1f - Math.abs(u) * 0.7f);
            float lx = u * 22f, ly = v * 14f, px = x + lx * ca - ly * sa, py = y + lx * sa + ly * ca;
            float d = (float) Math.sqrt(lx * lx + ly * ly) + 1f, burst = 40f + 190f * rng.nextFloat();
            spawn(px, py, (lx * ca - ly * sa) / d * burst + (rng.nextFloat() - 0.5f) * 60f, (lx * sa + ly * ca) / d * burst + 20f + (rng.nextFloat() - 0.5f) * 60f,
                    1.0f + 1.4f * rng.nextFloat(), 2f + 3.5f * rng.nextFloat(), 0);
        }
    }

    private int spawn(float px, float py, float pvx, float pvy, float lf, float sz, int md) {
        if (n >= CAP) return -1;
        int i = n++;
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy; age[i] = 0f; life[i] = lf; size[i] = sz; mode[i] = (byte) md;
        float[] c = PALETTE[rng.nextInt(PALETTE.length)];
        r[i] = c[0]; g[i] = c[1]; b[i] = c[2];
        sx[i] = px; sy[i] = py;
        swirl[i] = (rng.nextBoolean() ? 1f : -1f) * (0.5f + rng.nextFloat());
        return i;
    }

    /** The scene's clock; `w` is told when the hull is set at the lamp and when it is whole. */
    public void step(float dt, World w) {
        if (!active) return;
        t += dt;
        if (!placed && t >= GATHER_AT) {
            placed = true;
            w.ship.x = toX;
            w.ship.y = toY;
            w.ship.vx = w.ship.vy = 0f;
            for (int i = 0; i < 260; i++) {                                // the light gathers: from a ring round the lamp, closing on it
                float a = rng.nextFloat() * 6.2831853f, d = 120f + 190f * rng.nextFloat();
                int p = spawn(toX + (float) Math.cos(a) * d, toY + (float) Math.sin(a) * d, 0f, 0f, APPEAR_AT - GATHER_AT - 0.05f + 0.05f * rng.nextFloat(), 2f + 3f * rng.nextFloat(), 1);
                if (p >= 0) { sx[p] = x[p]; sy[p] = y[p]; }
            }
        }
        if (!appeared && t >= APPEAR_AT) {
            appeared = true;
            w.ship.iFrames = Math.max(w.ship.iFrames, 2.0f);
            w.ship.invuln = 0f;
            w.addEffect(Effect.ring(toX, toY, 130, 0.55f, 0.85f, 1f));
            w.addEffect(Effect.ring(toX, toY, 60, 1f, 1f, 1f));
            for (int i = 0; i < 90; i++) {
                float a = rng.nextFloat() * 6.2831853f, sp = 60f + 220f * rng.nextFloat();
                spawn(toX, toY, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 0.5f + 0.5f * rng.nextFloat(), 2f + 3f * rng.nextFloat(), 0);
            }
            w.ship.trauma = Math.min(1f, w.ship.trauma + 0.35f);
        }
        if (!appeared) w.ship.invuln = Math.max(w.ship.invuln, 5f);        // beyond harm until it is whole
        for (int i = n - 1; i >= 0; i--) {
            age[i] += dt;
            if (mode[i] == 0) {
                float k = (float) Math.exp(-1.7f * dt);
                vx[i] *= k;
                vy[i] = vy[i] * k + 14f * dt;                              // the light rises a little as it goes
                x[i] += vx[i] * dt;
                y[i] += vy[i] * dt;
            } else {
                float u = Math.min(1f, age[i] / life[i]), e = u * u * (3f - 2f * u);
                float rem = (1f - e), ang = swirl[i] * 3.0f * rem * rem;   // it spirals in, the turn dying away as it arrives
                float dx = sx[i] - toX, dy = sy[i] - toY, c = (float) Math.cos(ang), s = (float) Math.sin(ang);
                x[i] = toX + (dx * c - dy * s) * rem;
                y[i] = toY + (dx * s + dy * c) * rem;
            }
            if (age[i] >= life[i]) { n--; copy(n, i); }
        }
        if (t >= END) active = false;
    }

    private void copy(int from, int to) {
        if (from == to) return;
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from]; age[to] = age[from]; life[to] = life[from]; size[to] = size[from];
        r[to] = r[from]; g[to] = g[from]; b[to] = b[from]; sx[to] = sx[from]; sy[to] = sy[from]; swirl[to] = swirl[from]; mode[to] = mode[from];
    }

    /** The hull is not drawn (and not heard from) from the moment it breaks until it is whole again. */
    public boolean hidden() { return active && !appeared; }

    /** The scene holds the controls from the moment the hull breaks until it is whole. */
    public boolean controlsDead() { return hidden(); }

    /** Where the camera is to look, and how strongly: on the wreck until PAN_AT, then travelling to the lamp. Writes x, y, weight. */
    public void focus(float[] out) {
        if (!active || t < PAN_AT || t >= GATHER_AT + 0.2f) { return; }
        float u = Math.min(1f, (t - PAN_AT) / PAN_LEN), e = u * u * (3f - 2f * u);
        out[0] = fromX + (toX - fromX) * e;
        out[1] = fromY + (toY - fromY) * e;
        out[2] = 1f;
    }
}

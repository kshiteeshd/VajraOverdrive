package vajra.world;

import java.util.ArrayList;
import java.util.Random;
import vajra.enemy.Enemy;
import vajra.ship.Ship;
import vajra.weapon.Bullet;

/** THE FLOW: the simulation behind two astras - Agni's PYRE BLOOM (eighteen streams of fire pour out of the hull, curl over, lean toward foes and burn what they
 *  touch) and Vayu's TORNADO (the air is drawn in, foes and enemy rounds are carried round the funnel, the wind cuts them down one by one, then everything is thrown
 *  outward). docs/ASTRA_FLOW_PLAN.md has the whole design.
 *
 *  <p>It is a pooled particle system on a flow field, stepped in the world's fixed tick (so it is deterministic from its seed and testable): a particle carries a
 *  position, heading, speed, age and a sprite row; the field is the rite's own rule (outward and swirling for fire, inward-and-round then outward for wind) plus a
 *  smooth wobble, and walls turn a particle aside instead of stopping it. Bodies are acted on by the same field as a force: a foe is CARRIED (it is moved by the
 *  flow, its brain asleep while it is caught), heavier foes lag, a boss is not caught at all. Damage and burning happen on the host only; a guest runs the
 *  same particles for the picture. Rendering is {@code screen/FlowFx} and {@code screen/AstraFx}. */
public final class AstraFlow {
    public static final int CAP = 700;
    public static final int NONE = 0, AGNI = 1, VAYU = 2;

    // ---- Agni ----------------------------------------------------------------------------------------------------------------------------
    public static final int STREAMS = 18;
    /** How far the fire runs, px (a screen). */
    public static final float REACH = 650f;
    public static final float BURN_TIME = 3f, BURN_DPS = 14f, CONTACT_DPS = 30f, SPREAD_R = 100f;
    // ---- Vayu ----------------------------------------------------------------------------------------------------------------------------
    /** The beats of the tornado, seconds from the release. */
    public static final float T_DRAW = 1.6f, T_EYE = 2.0f, T_THROW = 3.0f, T_END = 3.4f;
    /** The cuts: from this moment, one every CUT_GAP, each taking this share of the foe's health. */
    public static final float CUT_FROM = 0.55f, CUT_GAP = 0.2f, CUT_SHARE = 0.55f, CUT_UNTIL = 1.45f;
    public static final float THROW_IMPACT = 0.375f;

    // ---- particles (struct of arrays, no allocation per frame) -----------------------------------------------------------------------------
    public final float[] px = new float[CAP], py = new float[CAP], ph = new float[CAP], age = new float[CAP], life = new float[CAP], size = new float[CAP],
            spd = new float[CAP], aux = new float[CAP];
    public final int[] row = new int[CAP], fr = new int[CAP], stream = new int[CAP];
    /** 0 = fire sheet, 1 = wind sheet. */
    public final byte[] sheet = new byte[CAP];
    public final byte[] mode = new byte[CAP];                 // 0 fire, 1 wind drawn in, 2 wind thrown out
    public int n;

    /** A crescent of cutting wind, drawn for a moment where a foe was cut. */
    public static final class Blade { public float x, y, ang, t; }
    public final ArrayList<Blade> blades = new ArrayList<>();
    /** The ground the fire has crossed: x, y, seconds left. */
    public final ArrayList<float[]> scorch = new ArrayList<>();

    public int kind;
    public float t, radius, power, dm = 1f;
    public Ship ship;
    public int seat;
    private Random rng = new Random(1);
    private final float[] emit = new float[STREAMS];
    private float spawnAcc, scorchAcc;
    private int cuts;
    private float burnDm = 1f;
    private final ArrayList<Enemy> caught = new ArrayList<>();
    private final ArrayList<Enemy> cand = new ArrayList<>();
    private final java.util.HashSet<Enemy> struck = new java.util.HashSet<>(), cutDone = new java.util.HashSet<>();
    private boolean thrown;

    public boolean active() { return kind != NONE; }

    /** The held shards of the Vayu astra keep circling the hull until the throw. */
    public boolean holdsShards() { return kind == VAYU && t < T_EYE; }

    public static float mass(Enemy e) {
        if (e.boss) return 99f;
        if (e.elite) return 3f;
        if (e.rank == 1) return 1.8f;
        return e.armoured ? 1.6f : 1f;
    }

    private static boolean eligible(Enemy e) {
        return !e.dead && !e.dismissed && !e.voice && !e.ghost && !e.bossHidden;
    }

    // ---- starting a rite -------------------------------------------------------------------------------------------------------------------

    private void reset(Ship from, int seatId, float damageMult, float pow, long seed) {
        n = 0;
        t = 0f;
        ship = from;
        seat = seatId;
        dm = damageMult;
        power = pow;
        rng = new Random(seed);
        java.util.Arrays.fill(emit, 0f);
        spawnAcc = scorchAcc = 0f;
        cuts = 0;
        thrown = false;
        blades.clear();
        caught.clear();
        struck.clear();
        cutDone.clear();
        burnDm = damageMult;
    }

    public void startAgni(Ship from, int seatId, float damageMult, float pow, long seed) {
        reset(from, seatId, damageMult, pow, seed);
        kind = AGNI;
        radius = REACH;
    }

    public void startVayu(Ship from, int seatId, float damageMult, float pow, long seed) {
        reset(from, seatId, damageMult, pow, seed);
        kind = VAYU;
        radius = 450f + 200f * Math.max(0f, Math.min(1f, pow));
    }

    public void cancel() {
        kind = NONE;
        n = 0;
        for (Enemy e : caught) e.caught = 0f;
        caught.clear();
    }

    // ---- the step --------------------------------------------------------------------------------------------------------------------------

    public void step(World w, float dt) {
        tickBurn(w, dt);
        tickScorch(w, dt);
        for (int i = blades.size() - 1; i >= 0; i--) if ((blades.get(i).t += dt) > 0.3f) blades.remove(i);
        if (kind == NONE) return;
        if (ship == null) { cancel(); return; }
        t += dt;
        if (kind == AGNI) stepAgni(w, dt); else stepVayu(w, dt);
        for (int i = n - 1; i >= 0; i--) if (age[i] >= life[i]) remove(i);
        float end = kind == AGNI ? 3.1f : T_END;
        if (t > end && n == 0 && (kind != AGNI || t > 3.5f)) cancel();
    }

    private int spawn(float x, float y, float h, float sp, float lf, float sz, int rw, int sh, int md, int strm, float a) {
        if (n >= CAP) return -1;
        int i = n++;
        px[i] = x;
        py[i] = y;
        ph[i] = h;
        spd[i] = sp;
        age[i] = 0f;
        life[i] = lf;
        size[i] = sz;
        row[i] = rw;
        sheet[i] = (byte) sh;
        mode[i] = (byte) md;
        stream[i] = strm;
        fr[i] = rng.nextInt(8);
        aux[i] = a;
        return i;
    }

    private void remove(int i) {
        n--;
        if (i == n) return;
        px[i] = px[n]; py[i] = py[n]; ph[i] = ph[n]; age[i] = age[n]; life[i] = life[n]; size[i] = size[n]; spd[i] = spd[n]; aux[i] = aux[n];
        row[i] = row[n]; fr[i] = fr[n]; stream[i] = stream[n]; sheet[i] = sheet[n]; mode[i] = mode[n];
    }

    // ---- AGNI: the Pyre Bloom --------------------------------------------------------------------------------------------------------------

    private void stepAgni(World w, float dt) {
        float cx = ship.x, cy = ship.y;
        float span = 0.55f + 0.45f * power;                              // a short invocation is a smaller bloom
        float closeAt = 1.4f + 0.8f * span;                              // the emitters close in the order they opened, between 2.2 s and 3.0 s at full strength
        for (int i = 0; i < STREAMS; i++) {
            float open = 0.6f * i / STREAMS, close = closeAt + 0.8f * i / STREAMS;
            if (t < open || t > close) continue;
            emit[i] -= dt;
            while (emit[i] <= 0f) {
                emit[i] += 0.085f;
                float base = i * 6.2831853f / STREAMS + 0.25f * (float) Math.sin(t * 0.9f) + (i % 2 == 1 ? 0.07f : -0.07f);
                float lf = (1.25f + 0.3f * rng.nextFloat()) * (0.7f + 0.3f * span);
                int p = spawn(cx + (float) Math.cos(base) * 14f, cy + (float) Math.sin(base) * 14f, base, 520f + 160f * rng.nextFloat(), lf,
                        0.6f + 0.4f * Math.min(1f, t / 0.5f), rng.nextFloat() < 0.7f ? 0 : 1, 0, 0, i, i % 2 == 1 ? 1f : -1f);
                if (p >= 0 && rng.nextInt(6) == 0) spawn(px[p], py[p], base + (rng.nextFloat() - 0.5f), 360f + 200f * rng.nextFloat(), 0.8f, 0.7f, 2, 0, 3, i, 0f);
            }
        }
        // the foes the fire can reach this tick
        cand.clear();
        if (!w.clientSeat || true) {
            float reach2 = (REACH + 140f) * (REACH + 140f);
            for (Enemy e : w.enemies) {
                if (!eligible(e)) continue;
                float dx = e.x - cx, dy = e.y - cy;
                if (dx * dx + dy * dy < reach2) cand.add(e);
            }
        }
        int[] touch = TOUCH.length >= cand.size() ? TOUCH : (TOUCH = new int[cand.size() + 16]);
        java.util.Arrays.fill(touch, 0, cand.size(), 0);
        scorchAcc -= dt;
        for (int i = 0; i < n; i++) {
            if (mode[i] == 3) { stepEmber(i, dt); continue; }
            age[i] += dt;
            float u = age[i] / life[i];
            ph[i] += aux[i] * (0.5f * (float) Math.sin(age[i] * 5f + fr[i]) + 4.4f * (float) Math.pow(Math.max(0f, u - 0.55f), 1.4f)) * dt;   // it curls over as it ages: the hook
            // lean toward the nearest living foe within reach of the flame
            Enemy best = null;
            float bd = 280f * 280f;
            for (int k = 0; k < cand.size(); k++) {
                Enemy e = cand.get(k);
                float dx = e.x - px[i], dy = e.y - py[i], d = dx * dx + dy * dy;
                if (d < bd) { bd = d; best = e; }
            }
            if (best != null) {
                float want = (float) Math.atan2(best.y - py[i], best.x - px[i]);
                ph[i] += World.wrapAngle(want - ph[i]) * 2.0f * dt;
            }
            float sp = spd[i] * (1f - 0.5f * u) * dt;
            float nx = px[i] + (float) Math.cos(ph[i]) * sp, ny = py[i] + (float) Math.sin(ph[i]) * sp;
            if (w.blockedPx(nx, ny)) {                                    // the fire creeps along rock: the heading slides to the wall
                if (!w.blockedPx(nx, py[i])) { ny = py[i]; ph[i] = Math.cos(ph[i]) >= 0 ? 0f : (float) Math.PI; }
                else if (!w.blockedPx(px[i], ny)) { nx = px[i]; ph[i] = Math.sin(ph[i]) >= 0 ? 1.5707964f : -1.5707964f; }
                else { age[i] = life[i]; continue; }
            }
            px[i] = nx;
            py[i] = ny;
            float dd = (nx - cx) * (nx - cx) + (ny - cy) * (ny - cy);
            if (dd > (REACH + 80f) * (REACH + 80f)) { age[i] = life[i]; continue; }
            for (int k = 0; k < cand.size(); k++) {                        // what it touches
                Enemy e = cand.get(k);
                float hit = 12f + Math.max(e.w, e.h) * 0.35f;
                float dx = e.x - nx, dy = e.y - ny;
                if (dx * dx + dy * dy < hit * hit) touch[k]++;
            }
            if (scorchAcc <= 0f && age[i] > 0.3f && rng.nextInt(14) == 0) addScorch(nx, ny);
        }
        if (scorchAcc <= 0f) scorchAcc = 0.12f;
        if (w.clientSeat) return;                                          // the host owns every verdict
        for (int k = 0; k < cand.size(); k++) {
            if (touch[k] <= 0) continue;
            Enemy e = cand.get(k);
            ignite(e);
            e.hurt(CONTACT_DPS * dm * dt * Math.min(3, touch[k]) / 3f, w, seat);
        }
    }

    private static int[] TOUCH = new int[64];

    private void stepEmber(int i, float dt) {
        age[i] += dt;
        px[i] += (float) Math.cos(ph[i]) * spd[i] * (1f - age[i] / life[i]) * dt;
        py[i] += (float) Math.sin(ph[i]) * spd[i] * (1f - age[i] / life[i]) * dt;
        ph[i] += 1.2f * (float) Math.sin(age[i] * 9f + fr[i]) * dt;
    }

    private void addScorch(float x, float y) {
        if (scorch.size() >= 90) scorch.remove(0);
        scorch.add(new float[] {x, y, 4f});
    }

    private void ignite(Enemy e) {
        if (e.dead) return;
        e.burnT = BURN_TIME;                                               // a second touch refreshes the burn, it never stacks
        e.burnOwner = seat;
    }

    /** Burning, for every foe, whether or not the rite is still running: a foe that dies burning ignites its neighbours and pays a coin. */
    private void tickBurn(World w, float dt) {
        if (w.clientSeat) return;
        boolean any = false;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.burnT <= 0f || e.dead) { e.burnT = 0f; continue; }
            any = true;
            e.burnT -= dt;
            boolean killed = e.hurtPiercing(BURN_DPS * burnDm * dt, w, e.burnOwner);
            if (killed || e.dead) {
                e.burnT = 0f;
                if (e.burnOwner == 0) w.hangar.coins++;                    // every foe that dies burning pays a coin
                w.addEffect(Effect.ring(e.x, e.y, 30f, 1f, 0.5f, 0.15f));
                for (Enemy o : w.enemies) {
                    if (o == e || !eligible(o)) continue;
                    float dx = o.x - e.x, dy = o.y - e.y;
                    if (dx * dx + dy * dy < SPREAD_R * SPREAD_R) { o.burnT = Math.max(o.burnT, BURN_TIME); o.burnOwner = e.burnOwner; }
                }
            }
        }
    }

    private void tickScorch(World w, float dt) {
        for (int i = scorch.size() - 1; i >= 0; i--) {
            float[] s = scorch.get(i);
            s[2] -= dt;
            if (s[2] <= 0f) { scorch.remove(i); continue; }
            if (w.clientSeat) continue;
            for (Enemy e : w.enemies) {                                    // the ground keeps burning what stands on it (never the pilot)
                if (!eligible(e) || e.burnT > 1f) continue;
                float dx = e.x - s[0], dy = e.y - s[1];
                if (dx * dx + dy * dy < 36f * 36f) { e.burnT = Math.max(e.burnT, 1f); e.burnOwner = seat; }
            }
        }
    }

    // ---- VAYU: the Tornado -----------------------------------------------------------------------------------------------------------------

    private void stepVayu(World w, float dt) {
        float cx = ship.x, cy = ship.y, R = radius, S = R / 160f;
        boolean draw = t < T_DRAW, eye = t >= T_DRAW && t < T_EYE, throwing = t >= T_EYE && t < T_THROW;
        float k = Math.min(1f, t / T_DRAW);
        // the wind: gusts drawn in from everywhere, or thrown out
        spawnAcc += dt * (draw ? 150f : throwing ? 190f : eye ? 40f : 0f);
        while (spawnAcc >= 1f) {
            spawnAcc -= 1f;
            float a = rng.nextFloat() * 6.2831853f;
            if (draw) spawn(cx, cy, 0f, R * (0.65f + 0.35f * rng.nextFloat()), 1.2f, 1f, rng.nextFloat() < 0.55f ? 0 : 2, 1, 1, 0, a);
            else if (throwing) spawn(cx, cy, 0f, R * (0.1f + 0.15f * rng.nextFloat()), 0.85f, 1f, rng.nextFloat() < 0.65f ? 0 : 2, 1, 2, 0, a);
            else spawn(cx, cy, 0f, R * (0.15f + 0.1f * rng.nextFloat()), 0.6f, 0.8f, 2, 1, 1, 0, a);
        }
        for (int i = 0; i < n; i++) {
            age[i] += dt;
            float r = spd[i], th = aux[i], vr, vt;
            if (mode[i] == 1) {
                vr = -(70f + 150f * k + 90f * (1f - r / R)) * S;
                float dth = 1.5f + 3.4f * (1f - r / R);
                vt = r * dth;
                th += dth * dt;
                if (r + vr * dt < R * 0.06f) age[i] = life[i];
            } else {
                vr = (360f + 420f * age[i]) * S;
                vt = r * 1.0f;
                th += 1.0f * dt;
            }
            r += vr * dt;
            spd[i] = r;
            aux[i] = th;
            px[i] = cx + r * (float) Math.cos(th);
            py[i] = cy + r * (float) Math.sin(th);
            float vx = vr * (float) Math.cos(th) - vt * (float) Math.sin(th), vy = vr * (float) Math.sin(th) + vt * (float) Math.cos(th);
            ph[i] = (float) Math.atan2(vy, vx);
            size[i] = (0.9f + 0.4f * (float) Math.sin(Math.PI * Math.min(1f, age[i] / life[i]))) * S;
        }
        if (w.clientSeat) return;
        // the foes: caught while the air is drawn in, carried round the funnel (heavier ones lag), cut one by one, then thrown
        if (draw || eye) {
            float reach2 = R * R;
            for (Enemy e : w.enemies) {
                if (!eligible(e) || e.boss || caught.contains(e)) continue;
                float dx = e.x - cx, dy = e.y - cy;
                if (draw && dx * dx + dy * dy < reach2) caught.add(e);
            }
            for (Enemy e : caught) {
                if (e.dead) continue;
                float dx = e.x - cx, dy = e.y - cy, r = (float) Math.sqrt(dx * dx + dy * dy), th = (float) Math.atan2(dy, dx), m = mass(e);
                float kk = Math.min(1f, t / T_DRAW);
                if (draw) r = Math.max(90f, r - (120f + 260f * kk) / (float) Math.sqrt(m) * dt);
                th += (draw ? 1.4f + 3.6f * (1f - Math.min(1f, r / R)) : 1.2f) / (float) Math.pow(m, 0.3) * dt;
                carry(w, e, cx + r * (float) Math.cos(th), cy + r * (float) Math.sin(th));
            }
            // the cuts: nearest first, one every CUT_GAP
            while (draw && cuts < 6 && t >= CUT_FROM + CUT_GAP * cuts && t <= CUT_UNTIL + CUT_GAP) {
                Enemy nearest = null;
                float nd = Float.MAX_VALUE;
                for (Enemy e : caught) {
                    if (e.dead || cutDone.contains(e)) continue;                   // each foe is cut once: one by one
                    float dx = e.x - cx, dy = e.y - cy, d = dx * dx + dy * dy;
                    if (d < nd) { nd = d; nearest = e; }
                }
                if (nearest == null) break;
                cuts++;
                cutDone.add(nearest);
                Blade bl = new Blade();
                bl.x = nearest.x;
                bl.y = nearest.y;
                bl.ang = (float) Math.atan2(nearest.y - cy, nearest.x - cx) + 1.5707964f;
                blades.add(bl);
                nearest.hurtPiercing(CUT_SHARE * nearest.maxHp * Math.min(1.4f, dm), w, seat);
                if (nearest.dead) nearest.caught = 0f;
            }
            // enemy rounds inside the storm are drawn in, spun and harmless
            for (Bullet b : w.bullets) {
                if (!b.hostile || b.dead) continue;
                float dx = b.x - cx, dy = b.y - cy, d2 = dx * dx + dy * dy;
                if (d2 > reach2) continue;
                float d = (float) Math.sqrt(Math.max(1f, d2)), inx = -dx / d, iny = -dy / d, tx = -iny, ty = inx;
                float in = (260f + 200f * k) * (draw ? 1f : 0.2f), tan = 360f;
                b.vx = inx * in + tx * -tan;
                b.vy = iny * in + ty * -tan;
                b.damage = 0f;
                b.life = Math.max(b.life, 1.2f);
            }
        }
        if (!thrown && t >= T_EYE) {                                       // THE THROW
            thrown = true;
            for (Enemy e : caught) {
                if (e.dead) continue;
                float dx = e.x - cx, dy = e.y - cy, d = (float) Math.sqrt(Math.max(1f, dx * dx + dy * dy)), v = (620f + 180f * rng.nextFloat()) / (float) Math.sqrt(mass(e));
                e.flowVx = dx / d * v;
                e.flowVy = dy / d * v;
            }
            for (vajra.world.AstraCube c : w.astra) {                      // the astra's own held shards stream out with the wind
                if (c.armed) continue;
                float dx = c.x - cx, dy = c.y - cy;
                c.dress(c.element, dm);
                c.arm((float) Math.atan2(dy, dx), 900f, null);
                c.owner = seat;
            }
        }
        if (throwing || t >= T_THROW && t < T_END) {
            for (Enemy e : caught) {
                if (e.dead) continue;
                e.caught = 0.25f;
                float ox = e.x, oy = e.y;
                float nx = ox + e.flowVx * dt, ny = oy + e.flowVy * dt;
                carry(w, e, nx, ny);
                boolean blocked = Math.hypot(e.x - nx, e.y - ny) > 2f;
                if (blocked && !struck.contains(e)) {                       // it hit rock: the wall hurts more than the wind
                    struck.add(e);
                    e.hurt(THROW_IMPACT * e.maxHp, w, seat);
                    e.flowVx = e.flowVy = 0f;
                } else if (!struck.contains(e)) {
                    e.flowVx *= 1f - 0.5f * dt;
                    e.flowVy *= 1f - 0.5f * dt;
                }
            }
            for (Bullet b : w.bullets) {                                    // and every round in the storm goes out, away from the ship
                if (!b.hostile || b.dead) continue;
                float dx = b.x - cx, dy = b.y - cy, d2 = dx * dx + dy * dy;
                if (d2 > radius * radius * 1.4f) continue;
                float d = (float) Math.sqrt(Math.max(1f, d2));
                b.vx = dx / d * 900f;
                b.vy = dy / d * 900f;
                b.damage = 0f;
            }
        }
        if (t >= T_THROW && thrown) {
            for (Enemy e : caught) e.caught = 0f;
            caught.clear();
        }
    }

    /** Move a caught foe to (nx, ny) through the world's own collision, so the flow carries it round rock and never through it. */
    private void carry(World w, Enemy e, float nx, float ny) {
        float[] c = w.collide(nx - e.w / 2f, ny - e.h / 2f, e.w, e.h);
        e.x = c[0] + e.w / 2f;
        e.y = c[1] + e.h / 2f;
        e.vx = e.vy = 0f;
        e.caught = 0.25f;
        e.active = true;
    }
}

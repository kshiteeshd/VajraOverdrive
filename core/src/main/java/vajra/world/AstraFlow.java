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
    public static final int NONE = 0, AGNI = 1, VAYU = 2, LIGHTNING = 3, BOULDER = 4, LOTUS = 5;

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
    public final byte[] mode = new byte[CAP];                 // 0 fire, 1 wind drawn in, 2 wind thrown out, 4 boulder forming, 5 boulder, 6 chunk, 7 pebble (Prithvi)
    public final byte[] variant = new byte[CAP];              // a boulder's kind (1 round, 2 slab, 3 veined, 4 jagged) / a chunk's kind
    public int n;

    /** A crescent of cutting wind, drawn for a moment where a foe was cut. */
    public static final class Blade { public float x, y, ang, t; }
    public final ArrayList<Blade> blades = new ArrayList<>();
    /** The ground the fire has crossed: x, y, seconds left. */
    public final ArrayList<float[]> scorch = new ArrayList<>();

    // ---- Akasha: the circular lightning ----
    public static final float ZAP_DAMAGE = 85f, ZAP_STUN = 0.8f, ARC_REACH = 160f, ARC_SHARE = 0.5f;
    /** One bolt of lightning to draw: a jagged line from (x0, y0) to (x1, y1) that re-forks every 50 ms while it lives (the renderer builds the jag from `seed`). */
    public static final class Bolt { public float x0, y0, x1, y1, age, life; public int seed; public boolean big; }
    public final ArrayList<Bolt> bolts = new ArrayList<>();
    /** The foes the lightning has stunned and until when (the renderer crawls small arcs round them). */
    public final ArrayList<Object[]> zapped = new ArrayList<>();
    private static final class Strike { Enemy e; float at; boolean done; float mult; }
    private final ArrayList<Strike> strikes = new ArrayList<>();
    private int wave;
    private float zapAcc;
    /** WAVES of lightning: the ring leaves at WAVE_GAP intervals, each striking every foe in reach; the first is the heaviest. */
    public static final int WAVES = 5;
    public static final float WAVE_GAP = 0.4f, ZAP_RATE = 0.07f, ZAP_SPEED = 1400f, ZAP_HIT = 15f, SHOCK_SECONDS = 0.7f;
    private static final float[] WAVE_MULT = {1f, 0.45f, 0.35f, 0.3f, 0.25f};
    /** A small bolt thrown from the hull at a foe: it flies at ZAP_SPEED and shocks the foe where it lands. */
    public static final class Zap { public float x, y, ang, age; public Enemy e; }
    public final ArrayList<Zap> zaps = new ArrayList<>();
    /** Where a thrown bolt landed: x, y, seconds since. */
    public final ArrayList<float[]> sparks = new ArrayList<>();
    public int waveCount() { return wave; }

    // ---- Prithvi: the boulders ----
    public static final float T_ROCK = 3.0f, ROCK_DAMAGE = 70f, CHUNK_DAMAGE = 28f, PEBBLE_DAMAGE = 7f;
    /** A one-shot picture where something broke or dust rose: row 5 BREAK, 9 DUST, 10 BREAK SMALL of the earth sheet. */
    public static final class Burst { public float x, y, t, life, size; public int row; }
    public final ArrayList<Burst> bursts = new ArrayList<>();
    private float rockAcc;

    // ---- Brahma: the lotus ----
    public static final int PETALS = 5;
    /** A ruler loses at most this much of its health to the petals: they hurt it, they do not end it. */
    public static final float BOSS_FLOOR = 0.75f;
    public static final float PETAL_RANGE = 2200f, T_BLOOM = 0.9f, PETAL_SPEED = 900f, ORBIT_TIME = 0.48f, LOTUS_MAX = 10f;
    /** The five petals, one per element (Akasha, Vayu, Agni, Jala, Prithvi): position, heading, state (0 bloom, 1 seek, 2 circle, 3 return, 4 done), the foe it is on, and its own clocks. */
    public final float[] lx = new float[PETALS], ly = new float[PETALS], lh = new float[PETALS], lt = new float[PETALS], la = new float[PETALS], lr = new float[PETALS], lfade = new float[PETALS];
    public final int[] lstate = new int[PETALS], lhits = new int[PETALS];
    public final Enemy[] ltarget = new Enemy[PETALS];
    private final float[] lmote = new float[PETALS];
    private final int[] lstrike = new int[PETALS];

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
        wave = 0;
        zapAcc = 0f;
        zaps.clear();
        sparks.clear();
        strikes.clear();
        bolts.clear();
        zapped.clear();
        bursts.clear();
        rockAcc = 0f;
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

    /** AKASHA: waves of circular lightning that strike every foe near the hull, with small bolts thrown at the foes between the waves (docs/ASTRA_NEXT_PLAN.md section 2). */
    public void startLightning(Ship from, int seatId, float damageMult, float pow, long seed) {
        reset(from, seatId, damageMult, pow, seed);
        kind = LIGHTNING;
        radius = 360f + 160f * Math.max(0f, Math.min(1f, pow));
    }

    private void addBolt(float x0, float y0, float x1, float y1, float life, boolean big) {
        if (bolts.size() > 260) return;
        Bolt b = new Bolt();
        b.x0 = x0; b.y0 = y0; b.x1 = x1; b.y1 = y1;
        b.life = life;
        b.big = big;
        b.seed = rng.nextInt();
        bolts.add(b);
    }

    /** Puts `dur` seconds of shock on a foe (the renderer jitters it and lays sparks over it). */
    private void shock(Enemy e, float dur) {
        for (Object[] z : zapped) if (z[0] == e) { z[1] = Math.max((Float) z[1], t + dur); return; }
        zapped.add(new Object[] {e, t + dur});
    }

    private void launchWave(World w, int k) {
        int spokes = 12 + 2 * Math.min(k, 3);
        float a0 = rng.nextFloat() * 6.2831853f;
        for (int i = 0; i < spokes; i++) {                                 // the ring: bolts to every side, no two waves the same
            float a = a0 + i * 6.2831853f / spokes + (rng.nextFloat() - 0.5f) * 0.35f, len = radius * (0.55f + 0.45f * rng.nextFloat());
            addBolt(ship.x, ship.y, ship.x + (float) Math.cos(a) * len, ship.y + (float) Math.sin(a) * len, 0.55f, true);
        }
        ArrayList<Enemy> in = new ArrayList<>();
        for (Enemy e : w.enemies) {
            if (!eligible(e)) continue;
            float dx = e.x - ship.x, dy = e.y - ship.y;
            if (dx * dx + dy * dy <= radius * radius) in.add(e);
        }
        final float sx = ship.x, sy = ship.y;
        in.sort((p, q) -> Float.compare((p.x - sx) * (p.x - sx) + (p.y - sy) * (p.y - sy), (q.x - sx) * (q.x - sx) + (q.y - sy) * (q.y - sy)));
        for (int i = 0; i < in.size(); i++) {                              // nearest first, one every 12 ms
            Strike s = new Strike();
            s.e = in.get(i);
            s.at = t + 0.05f + 0.012f * Math.min(i, 40);
            s.mult = WAVE_MULT[Math.min(k, WAVE_MULT.length - 1)];
            strikes.add(s);
        }
    }

    private void stepLightning(World w, float dt) {
        for (int i = bolts.size() - 1; i >= 0; i--) if ((bolts.get(i).age += dt) > bolts.get(i).life) bolts.remove(i);
        for (int i = sparks.size() - 1; i >= 0; i--) if ((sparks.get(i)[2] += dt) > 0.28f) sparks.remove(i);
        while (wave < WAVES && t >= wave * WAVE_GAP) launchWave(w, wave++);
        if (t >= 0.15f && t <= WAVES * WAVE_GAP - 0.1f) {                  // small bolts thrown from the hull at foes, one every 70 ms
            zapAcc += dt;
            while (zapAcc >= ZAP_RATE) {
                zapAcc -= ZAP_RATE;
                if (zaps.size() >= 40) break;
                ArrayList<Enemy> pool = new ArrayList<>();
                for (Enemy e : w.enemies) {
                    if (!eligible(e)) continue;
                    float dx = e.x - ship.x, dy = e.y - ship.y;
                    if (dx * dx + dy * dy <= radius * radius * 1.7f) pool.add(e);
                }
                if (pool.isEmpty()) break;
                Zap z = new Zap();
                z.x = ship.x; z.y = ship.y;
                z.e = pool.get(rng.nextInt(pool.size()));
                z.ang = (float) Math.atan2(z.e.y - z.y, z.e.x - z.x);
                zaps.add(z);
            }
        }
        for (int i = zaps.size() - 1; i >= 0; i--) {                       // each flies at its foe and shocks it where it lands
            Zap z = zaps.get(i);
            z.age += dt;
            if (z.e.dead || z.age > 1.5f) { zaps.remove(i); continue; }
            float dx = z.e.x - z.x, dy = z.e.y - z.y, d = (float) Math.sqrt(dx * dx + dy * dy), step = ZAP_SPEED * dt;
            z.ang = (float) Math.atan2(dy, dx);
            if (d <= step + Math.max(z.e.w, z.e.h) * 0.3f) {
                sparks.add(new float[] {z.e.x, z.e.y, 0f});
                if (!w.clientSeat) z.e.hurt(ZAP_HIT * dm, w, seat);
                shock(z.e, SHOCK_SECONDS);
                zaps.remove(i);
            } else { z.x += dx / d * step; z.y += dy / d * step; }
        }
        for (Strike s : strikes) {
            if (s.done || t < s.at) continue;
            s.done = true;
            Enemy e = s.e;
            if (e.dead) continue;
            addBolt(ship.x, ship.y, e.x, e.y, 0.4f, false);
            shock(e, ZAP_STUN + 0.2f);
            if (!w.clientSeat) {
                e.hurt(ZAP_DAMAGE * dm * s.mult, w, seat);
                if (!e.boss && !e.dead) { e.caught = Math.max(e.caught, ZAP_STUN * (s.mult < 1f ? 0.6f : 1f)); e.active = true; }
                int arcs = 0;                                              // the lightning jumps from it to the two nearest foes within reach
                for (Enemy o : w.enemies) {
                    if (o == e || !eligible(o) || arcs >= 2) continue;
                    float dx = o.x - e.x, dy = o.y - e.y;
                    if (dx * dx + dy * dy > ARC_REACH * ARC_REACH) continue;
                    arcs++;
                    addBolt(e.x, e.y, o.x, o.y, 0.3f, false);
                    shock(o, 0.4f);
                    o.hurt(ZAP_DAMAGE * dm * s.mult * ARC_SHARE, w, seat);
                }
            }
        }
        for (int i = zapped.size() - 1; i >= 0; i--) {
            Enemy e = (Enemy) zapped.get(i)[0];
            float left = (Float) zapped.get(i)[1] - t;
            if (left <= 0f || e.dead) { e.shockT = 0f; zapped.remove(i); } else e.shockT = left;
        }
        boolean pending = false;
        for (Strike s : strikes) if (!s.done) { pending = true; break; }
        if (wave >= WAVES && t > WAVES * WAVE_GAP && !pending && bolts.isEmpty() && zaps.isEmpty() && sparks.isEmpty() && zapped.isEmpty()) cancel();
    }

    /** PRITHVI: boulders form out of the mandala and are hurled (docs/ASTRA_NEXT_PLAN.md section 3). The mandala stays while it lasts. */
    public void startBoulders(Ship from, int seatId, float damageMult, float pow, long seed) {
        reset(from, seatId, damageMult, pow, seed);
        kind = BOULDER;
        radius = Astra.mandalaRadius(Astra.PRITHVI, 1f);
    }

    /** BRAHMA: five lotus petals open round the hull and spread through the world, circling every foe in turn (docs/ASTRA_NEXT_PLAN.md section 4). */
    public void startLotus(Ship from, int seatId, float damageMult, float pow, long seed) {
        reset(from, seatId, damageMult, pow, seed);
        kind = LOTUS;
        radius = PETAL_RANGE;
        for (int i = 0; i < PETALS; i++) {
            lstate[i] = 0;
            lt[i] = 0f;
            lhits[i] = 0;
            ltarget[i] = null;
            lfade[i] = 0f;
            lmote[i] = 0f;
            lstrike[i] = 0;
            lx[i] = from.x;
            ly[i] = from.y;
            lh[i] = -1.5707964f + i * 6.2831853f / PETALS;
            la[i] = lh[i];
        }
    }

    public void cancel() {
        kind = NONE;
        n = 0;
        strikes.clear();
        bolts.clear();
        zaps.clear();
        sparks.clear();
        for (Object[] z : zapped) ((Enemy) z[0]).shockT = 0f;
        zapped.clear();
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
        for (int i = bursts.size() - 1; i >= 0; i--) if ((bursts.get(i).t += dt) > bursts.get(i).life) bursts.remove(i);
        if (kind == AGNI) stepAgni(w, dt); else if (kind == VAYU) stepVayu(w, dt); else if (kind == LIGHTNING) stepLightning(w, dt);
        else if (kind == BOULDER) stepBoulders(w, dt); else if (kind == LOTUS) { stepLotus(w, dt); return; }
        for (int i = n - 1; i >= 0; i--) if (age[i] >= life[i]) remove(i);
        float end = kind == AGNI ? 3.1f : kind == BOULDER ? T_ROCK + 0.3f : T_END;
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
        variant[i] = 0;
        return i;
    }

    private void remove(int i) {
        n--;
        if (i == n) return;
        px[i] = px[n]; py[i] = py[n]; ph[i] = ph[n]; age[i] = age[n]; life[i] = life[n]; size[i] = size[n]; spd[i] = spd[n]; aux[i] = aux[n];
        row[i] = row[n]; fr[i] = fr[n]; stream[i] = stream[n]; sheet[i] = sheet[n]; mode[i] = mode[n]; variant[i] = variant[n];
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
            if ((e.burnPuffT -= dt) <= 0f) {                               // it smokes as the hull does when it is low: the same puffs, but fire orange, not grey
                e.burnPuffT = 0.09f;
                float jitter = (w.fxRng.nextFloat() - 0.5f) * e.w * 0.5f;
                Effect puff = Effect.smoke(e.x + jitter, e.y + e.h * 0.15f, (w.fxRng.nextFloat() - 0.5f) * 30f, 26f + 22f * w.fxRng.nextFloat(),
                        1f, 0.50f + 0.22f * w.fxRng.nextFloat(), 0.10f);
                puff.fire = true;
                puff.radius = 50f;
                w.addEffect(puff);
            }
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

    // ---- PRITHVI: the Boulders -----------------------------------------------------------------------------------------------------------

    private int boulderCount() {
        int c = 0;
        for (int i = 0; i < n; i++) if (mode[i] == 4 || mode[i] == 5) c++;
        return c;
    }

    private void addBurst(float x, float y, int rw, float life, float sz) {
        if (bursts.size() > 90) return;
        Burst b = new Burst();
        b.x = x; b.y = y; b.row = rw; b.life = life; b.size = sz;
        bursts.add(b);
    }

    /** A boulder breaks into three to five chunks flying on in a cone about `base` (back off a wall, onward through a foe); a chunk into six to nine pebbles. */
    private void breakRock(int i, float base, int gen) {
        float x = px[i], y = py[i], sz = size[i];
        if (gen == 5) {
            addBurst(x, y, 5, 0.5f, sz);
            addBurst(x, y, 9, 0.6f, sz * 0.9f);
            int c = 3 + rng.nextInt(3);
            for (int k = 0; k < c && n < CAP - 8; k++) {
                int p = spawn(x, y, base + (rng.nextFloat() - 0.5f) * 2.4f, 280f + 190f * rng.nextFloat(), 0.9f, sz * (0.7f + 0.3f * rng.nextFloat()), 0, 3, 6, rng.nextBoolean() ? 1 : -1, rng.nextFloat() * 6.28f);
                if (p >= 0) { variant[p] = (byte) (rng.nextBoolean() ? 6 : 7); row[p] = variant[p]; }
            }
        } else {
            addBurst(x, y, 10, 0.45f, sz);
            int c = 6 + rng.nextInt(4);
            for (int k = 0; k < c && n < CAP - 4; k++) {
                int p = spawn(x, y, base + (rng.nextFloat() - 0.5f) * 3.2f, 200f + 190f * rng.nextFloat(), 0.5f, 1f, 8, 3, 7, 0, rng.nextFloat() * 6.28f);
                if (p >= 0) variant[p] = 8;
            }
        }
        age[i] = life[i];
    }

    private void stepBoulders(World w, float dt) {
        float cx = ship.x, cy = ship.y;
        float R = radius;
        cand.clear();
        for (Enemy e : w.enemies) {
            if (!eligible(e)) continue;
            float dx = e.x - cx, dy = e.y - cy;
            if (dx * dx + dy * dy < 1400f * 1400f) cand.add(e);
        }
        if (t < T_ROCK) {                                                  // a boulder forms every 60 ms at a point of the mandala, and is thrown
            rockAcc += dt * 26f;
            while (rockAcc >= 1f) {
                rockAcc -= 1f;
                if (boulderCount() >= 28 || n > CAP - 60) continue;
                float a = rng.nextFloat() * 6.2831853f, rr = R * (0.35f + 0.6f * rng.nextFloat());
                float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr, h;
                if (!cand.isEmpty() && rng.nextFloat() < 0.45f) {          // about half are thrown at a foe, with a little error
                    Enemy e = cand.get(rng.nextInt(cand.size()));
                    h = (float) Math.atan2(e.y - y, e.x - x) + (rng.nextFloat() - 0.5f) * 0.25f;
                } else h = a + (rng.nextFloat() - 0.5f) * 1.6f;            // the rest fly out of the mandala in any direction: into walls, and whatever is between
                int p = spawn(x, y, h, 520f + 240f * rng.nextFloat(), 99f, 0.7f + 0.8f * rng.nextFloat(), 0, 3, 4, rng.nextBoolean() ? 1 : -1, rng.nextFloat() * 6.28f);
                if (p >= 0) variant[p] = (byte) (1 + rng.nextInt(4));
            }
        }
        for (int i = 0; i < n; i++) {
            if (mode[i] < 4) continue;
            float dtl = dt;
            age[i] += dtl;
            aux[i] += stream[i] * (mode[i] == 7 ? 9f : mode[i] == 6 ? 7f : 3.2f) * dtl;       // tumbling
            if (mode[i] == 4) {                                            // forming: pebbles and dust draw together, then it goes
                if (age[i] >= 0.25f) { mode[i] = 5; age[i] = 0f; life[i] = 1.8f; row[i] = variant[i]; }
                continue;
            }
            float sp = spd[i] * dtl;
            float nx = px[i] + (float) Math.cos(ph[i]) * sp, ny = py[i] + (float) Math.sin(ph[i]) * sp;
            boolean hitWall = w.blockedPx(nx, ny);
            float hitR = mode[i] == 5 ? 20f * size[i] : mode[i] == 6 ? 12f * size[i] : 6f;
            Enemy hit = null;
            for (int k = 0; k < cand.size() && !hitWall; k++) {
                Enemy e = cand.get(k);
                float rr = hitR + Math.max(e.w, e.h) * 0.4f, dx = e.x - nx, dy = e.y - ny;
                if (dx * dx + dy * dy < rr * rr) { hit = e; break; }
            }
            if (!w.clientSeat) {                                           // an enemy round that touches a rock is destroyed
                for (Bullet b : w.bullets) {
                    if (!b.hostile || b.dead) continue;
                    float dx = b.x - nx, dy = b.y - ny, rr = hitR + 4f;
                    if (dx * dx + dy * dy < rr * rr) b.dead = true;
                }
            }
            if (hit != null) {                                             // the first foe it touches takes it, once
                float dmg = (mode[i] == 5 ? ROCK_DAMAGE * size[i] : mode[i] == 6 ? CHUNK_DAMAGE * size[i] : PEBBLE_DAMAGE) * dm;
                if (!w.clientSeat) hit.hurt(dmg, w, seat);
                if (mode[i] == 7) { age[i] = life[i]; addBurst(nx, ny, 9, 0.3f, 0.5f); continue; }
                px[i] = nx; py[i] = ny;
                breakRock(i, ph[i], mode[i]);
                continue;
            }
            if (hitWall) {
                if (mode[i] == 7) { age[i] = life[i]; continue; }
                breakRock(i, ph[i] + 3.1415927f, mode[i]);                 // it bursts back off the wall
                continue;
            }
            px[i] = nx;
            py[i] = ny;
            if (age[i] >= life[i] && mode[i] != 7) breakRock(i, ph[i], mode[i]);
            else if (mode[i] == 7) spd[i] *= 1f - 1.2f * dt;
        }
    }

    // ---- BRAHMA: the Lotus ---------------------------------------------------------------------------------------------------------------

    private void stepLotus(World w, float dt) {
        float cx = ship.x, cy = ship.y;
        boolean any = false;
        for (int i = 0; i < PETALS; i++) {                                 // fade and motes first (the petal's own picture lives in the renderer)
            if (lstate[i] == 4) continue;
            any = true;
            lt[i] += dt;
            lmote[i] -= dt;
            if (lmote[i] <= 0f && n < CAP - 4) {                            // petal flecks left behind
                lmote[i] = 0.03f;
                int p = spawn(lx[i], ly[i], lh[i] + 3.1415927f + (rng.nextFloat() - 0.5f), 40f, 0.5f, 0.8f, 5, 4, 0, i, 0f);
                if (p >= 0) variant[p] = (byte) i;
            }
            switch (lstate[i]) {
                case 0: {                                                   // BLOOM: the five unfold round the hull in a turning ring
                    float u = Math.min(1f, lt[i] / T_BLOOM), r = 70f * (1f - (1f - u) * (1f - u)), a = lh[i] + lt[i] * 0.8f;
                    lx[i] = cx + (float) Math.cos(a) * r;
                    ly[i] = cy + (float) Math.sin(a) * r;
                    la[i] = a;
                    if (lt[i] >= T_BLOOM) { lstate[i] = 1; lt[i] = 0f; lh[i] = a; }
                    break;
                }
                case 1: {                                                   // SEEK: straight at its foe, no arc, no gravity
                    if (ltarget[i] == null || ltarget[i].dead) ltarget[i] = pickTarget(w, i);
                    Enemy e = ltarget[i];
                    if (e == null) { lstate[i] = 3; lt[i] = 0f; break; }
                    float want = (float) Math.atan2(e.y - ly[i], e.x - lx[i]);
                    lh[i] += Math.max(-9f * dt, Math.min(9f * dt, World.wrapAngle(want - lh[i])));
                    float sp = PETAL_SPEED * Math.min(1f, 0.35f + lt[i] * 2f) * dt;
                    lx[i] += (float) Math.cos(lh[i]) * sp;
                    ly[i] += (float) Math.sin(lh[i]) * sp;
                    if ((e.x - lx[i]) * (e.x - lx[i]) + (e.y - ly[i]) * (e.y - ly[i]) < 95f * 95f) {
                        lstate[i] = 2;
                        lt[i] = 0f;
                        lstrike[i] = 0;
                        la[i] = (float) Math.atan2(ly[i] - e.y, lx[i] - e.x);
                        lfade[i] = (float) Math.hypot(lx[i] - e.x, ly[i] - e.y);                  // it circles from where it arrived, closing in
                    }
                    break;
                }
                case 2: {                                                   // CIRCLE: round the foe, closing in, striking three times
                    Enemy e = ltarget[i];
                    if (e == null || e.dead) { lstate[i] = 1; lt[i] = 0f; ltarget[i] = null; break; }
                    float u = Math.min(1f, lt[i] / ORBIT_TIME);
                    la[i] += 14f * dt;
                    lr[i] = lfade[i] + (38f - lfade[i]) * u;
                    lx[i] = e.x + (float) Math.cos(la[i]) * lr[i];
                    ly[i] = e.y + (float) Math.sin(la[i]) * lr[i];
                    lh[i] = la[i] + 1.5707964f;
                    if (lstrike[i] < 3 && lt[i] >= 0.12f + 0.16f * lstrike[i]) { lstrike[i]++; strikePetal(w, i, e); }
                    if (lt[i] >= ORBIT_TIME) { lhits[i]++; ltarget[i] = null; lstate[i] = 1; lt[i] = 0f; lh[i] = la[i] + 1.5707964f; }
                    break;
                }
                case 3: {                                                   // RETURN: nothing left: back to the hull, and fade
                    float want = (float) Math.atan2(cy - ly[i], cx - lx[i]);
                    lh[i] += Math.max(-9f * dt, Math.min(9f * dt, World.wrapAngle(want - lh[i])));
                    lx[i] += (float) Math.cos(lh[i]) * PETAL_SPEED * dt;
                    ly[i] += (float) Math.sin(lh[i]) * PETAL_SPEED * dt;
                    if ((cx - lx[i]) * (cx - lx[i]) + (cy - ly[i]) * (cy - ly[i]) < 50f * 50f) { lstate[i] = 4; lfade[i] = 0f; }
                    if (lt[i] > 4f) lstate[i] = 4;
                    break;
                }
                default:
            }
        }
        for (int i = n - 1; i >= 0; i--) {                                  // the flecks
            age[i] += dt;
            px[i] += (float) Math.cos(ph[i]) * spd[i] * dt;
            py[i] += (float) Math.sin(ph[i]) * spd[i] * dt;
            if (age[i] >= life[i]) remove(i);
        }
        if (!any || t > LOTUS_MAX) cancel();
    }

    /** The nearest living foe within range that no other petal is on (or, if every one is taken, the nearest). */
    private Enemy pickTarget(World w, int petal) {
        Enemy best = null, bestAny = null;
        float bd = Float.MAX_VALUE, bdAny = Float.MAX_VALUE;
        for (Enemy e : w.enemies) {
            if (!eligible(e)) continue;
            float dx = e.x - ship.x, dy = e.y - ship.y;
            if (dx * dx + dy * dy > PETAL_RANGE * PETAL_RANGE) continue;
            if (e.boss && e.hp <= e.maxHp * BOSS_FLOOR) continue;           // a ruler the petals have already hurt enough is left
            float ex = e.x - lx[petal], ey = e.y - ly[petal], d = ex * ex + ey * ey;
            if (d < bdAny) { bdAny = d; bestAny = e; }
            boolean taken = false;
            for (int k = 0; k < PETALS; k++) if (k != petal && ltarget[k] == e && lstate[k] != 4) { taken = true; break; }
            if (!taken && d < bd) { bd = d; best = e; }
        }
        return best != null ? best : bestAny;
    }

    /** One of a petal's three strikes in its circling: a third of the damage, and the petal's own element on the first. */
    private void strikePetal(World w, int petal, Enemy e) {
        float[] c = World.TATVA_RGB[petal];
        w.addEffect(Effect.ring(e.x, e.y, 30f, c[0], c[1], c[2]));
        w.addEffect(Effect.spark(e.x, e.y, (float) Math.cos(la[petal]) * 160f, (float) Math.sin(la[petal]) * 160f, 4f, 0.3f, 3f, c[0], c[1], c[2]));
        if (w.clientSeat) return;
        if (e.boss && e.hp <= e.maxHp * BOSS_FLOOR) return;
        float total = e.boss ? 0.04f * e.maxHp : Math.max(0.6f * e.maxHp, 220f);
        float mult = petal == 4 ? 1.4f : petal == 1 ? 1.25f : 1f;          // stone strikes heavily, the gale cuts deep
        e.hurt(total / 3f * mult * Math.min(1.5f, dm), w, seat);
        if (lstrike[petal] == 1 && !e.dead) {
            switch (petal) {
                case 0: if (!e.boss) { e.caught = Math.max(e.caught, 0.6f); e.active = true; } break;                       // Akasha: shock
                case 2: ignite(e); break;                                                                                  // Agni: it burns
                case 3: if (seat == 0) w.ship.hp = Math.min(w.ship.maxHp(), w.ship.hp + 0.015f * w.ship.maxHp()); break;   // Jala: the pilot is mended
                case 4: if (!e.boss) { e.caught = Math.max(e.caught, 0.35f); e.active = true; } break;                     // Prithvi: staggered
                default:
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

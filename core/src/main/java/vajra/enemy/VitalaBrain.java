package vajra.enemy;

import vajra.ship.Ship;
import vajra.weapon.WeaponDef;
import vajra.world.Effect;
import vajra.world.Hazards;
import vajra.world.World;

/** THE BRAIN OF VITALA (docs/VITALA_ENEMIES.md). Nothing here is Atala's: Atala's units park at a post or ring a leader and tap single rounds;
 *  Vitala's FLY FORMATIONS and attack in PATTERNS, and each of the six carries its own character from the original bestiary:
 *  <ul>
 *   <li><b>Agneya</b> (VAKRA slot), the Ember: a burning dart. It DART-RUNS - a telegraph, then a straight fast run through the hull's place that
 *       leaves a trail of burning ground - and throws ember bolts between runs;</li>
 *   <li><b>Jvala</b> (MAYAVI), the Cinder: never fires. It hangs back and drops burning patches where the hull is GOING; the patches pile up;</li>
 *   <li><b>Tamra</b> (BHAVI), the Ore: copper-plated (bolts from the front ring off, see {@link Enemy#armoured}), slow and heavy, it steps in front of
 *       the shooters behind it to body-block, rams what comes close and slams a ring when it is touched;</li>
 *   <li><b>Kila</b> (LANCE), the Nail: a sniper. A laser tell, then a burst of three thin fast needles, from far off;</li>
 *   <li><b>Dhuma</b> (AURIS), the Smoke: it goes translucent for 1.5 s (no body, cannot be targeted), drifts to a point close to the hull and
 *       reappears aiming at it with two bolts;</li>
 *   <li><b>Rekha</b> (HATAKA), the Line: it draws a hairline across the arena, waits, then ignites the whole line at once. Several Rekha draw
 *       PARALLEL lines at the leader's angle - a grid.</li>
 *  </ul>
 *  RANK IS KEPT. A unit with a commander obeys it: it takes the slot its leader's FORMATION gives it, fires when the leader's clock says so (the
 *  formation sets the stagger - a ripple down a line, a volley from a wedge, a relay along a column, alternate flanks of a pincer, a radial ring),
 *  routs when the leader falls and regroups under another. A leader (rank 1) or an elite picks the formation (each kind has two, cycled every two
 *  volleys), and hits harder (Enemy.RANK_DAMAGE).
 *
 *  <p>Formations: LINE abreast across the leader; WEDGE a V behind it; COLUMN a file along the leader's trail (the river road); PINCER two flanks
 *  closing round the hull; CIRCLE a ring round the HULL (Atala's rings turn round the leader). */
public final class VitalaBrain {
    private VitalaBrain() {}

    public static final int F_LINE = 0, F_WEDGE = 1, F_COLUMN = 2, F_PINCER = 3, F_CIRCLE = 4;
    public static final float VOLLEY = 3.4f;       // seconds between a formation's volleys
    public static final int TRAIL_N = 64;
    /** How far from its own post a Vitala unit may range: its leader's stalk is tied to the post by this, and every follower is tied to its own. */
    public static final float LEASH = 480f, FOLLOWER_LEASH = 600f;
    /** A court takes the fight to the hull only while the hull is this near its post; beyond it, it holds its formation at the post. */
    public static final float ENGAGE = 900f;
    private static final float TRAIL_DT = 0.15f, TURN = 5f, GAIN = 2.4f, ROUT_T = 2.5f, ROUT_SPEED = 190f;

    public static final WeaponDef EMBER = WeaponDef.mk("agneya ember").speed(340).damage(10).bulletSize(6).life(3.2f).colour(1f, 0.5f, 0.15f);
    public static final WeaponDef NEEDLE = WeaponDef.mk("kila needle").count(3).spreadDeg(7f).speed(780).damage(6).bulletSize(3).life(2.0f)
            .colour(1f, 0.88f, 0.45f);
    public static final WeaponDef SMOKE_BOLT = WeaponDef.mk("dhuma bolt").count(2).spreadDeg(9f).speed(400).damage(8).bulletSize(5).life(3f)
            .colour(0.72f, 0.78f, 0.95f);
    public static final WeaponDef SLAG = WeaponDef.mk("tamra slag").speed(250).damage(14).bulletSize(10).life(2.4f).colour(0.85f, 0.45f, 0.2f);
    public static final WeaponDef SLAG_PELLET = WeaponDef.mk("tamra ring").speed(230).damage(8).bulletSize(6).life(1.3f).colour(0.9f, 0.5f, 0.2f);

    /** Fire a gun's whole pattern (its count of rounds across its spread) at an angle, graded by the shooter's rank. */
    public static void fire(World w, Enemy e, WeaponDef gun, float aim) {
        float dmg = e.dmgScale * Enemy.RANK_DAMAGE[e.rankGrade()];
        for (int i = 0; i < gun.count; i++) {
            float off = gun.count == 1 ? 0f : (float) Math.toRadians(i * gun.spreadDeg / (gun.count - 1) - gun.spreadDeg / 2);
            w.spawnEnemyBullet(e.x, e.y, aim + off, gun, dmg);
        }
    }

    /** The two formations each kind flies, cycled every two volleys. */
    public static int[] formsOf(Enemy.Kind k) {
        switch (k) {
            case VAKRA: return new int[] {F_WEDGE, F_PINCER};
            case LANCE: return new int[] {F_LINE, F_WEDGE};
            case AURIS: return new int[] {F_COLUMN, F_PINCER};
            case BHAVI: return new int[] {F_LINE, F_CIRCLE};
            case MAYAVI: return new int[] {F_COLUMN, F_CIRCLE};
            case HATAKA: return new int[] {F_LINE, F_COLUMN};
            default: return new int[] {F_LINE, F_CIRCLE};
        }
    }

    /** How far from the hull each kind likes to fight, px. */
    public static float range(Enemy.Kind k) {
        switch (k) {
            case VAKRA: return 340f;
            case LANCE: return 540f;
            case AURIS: return 300f;
            case BHAVI: return 190f;
            case MAYAVI: return 480f;
            case HATAKA: return 380f;
            default: return 360f;
        }
    }

    /** When, inside a volley, slot k of a formation fires: a ripple, a volley, a relay, alternate flanks, a ring. */
    public static float stagger(int form, int slot) {
        switch (form) {
            case F_LINE: return 0.14f * slot;
            case F_WEDGE: return 0f;
            case F_COLUMN: return 0.22f * slot;
            case F_PINCER: return (slot & 1) == 0 ? 0f : VOLLEY * 0.5f;
            default: return 0f;
        }
    }

    private static Enemy leaderOf(Enemy e) {
        Enemy c = e.commander;
        return c != null && !c.dead && c.vitala ? c : null;
    }

    public static void think(Enemy e, World w, float dt, float t) {
        Ship hull = w.nearestHull(e.x, e.y);
        float dx = hull.x - e.x, dy = hull.y - e.y;
        float dist = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        float ux = dx / dist, uy = dy / dist;
        e.facing = (float) Math.atan2(dy, dx);
        e.clockT += dt;
        if (e.commander != null && e.commander.dead && !e.routed) {
            e.routed = true;
            e.routAge = 0f;
        }
        float cruise = e.speed * (e.rank > 0 ? 1.15f : 1f) * (e.hp < e.maxHp * 0.3f ? 1.25f : 1f);
        if (e.kind == Enemy.Kind.BHAVI) cruise *= 0.85f;                    // Tamra is heavy

        float gx = 0f, gy = 0f;
        if (e.dismissed) {                                                   // stood down by a fallen ruler: drift off, then go out
            e.dismissAge += dt;
            gx = -ux * ROUT_SPEED * 0.6f;
            gy = -uy * ROUT_SPEED * 0.6f;
            if (e.dismissAge > 4f) { e.dead = true; w.addEffect(Effect.ring(e.x, e.y, 60, 1f, 0.6f, 0.2f)); }
        } else if (e.routed) {
            e.routAge += dt;
            e.smoked = false;
            if (e.routAge > ROUT_T) {
                e.commander = findSenior(e, w);
                if (e.commander == null) { e.homeX = e.spawnX; e.homeY = e.spawnY; }
                e.routed = false;
            } else {
                float rx = e.x - e.homeX, ry = e.y - e.homeY, rd = (float) Math.hypot(rx, ry);
                if (rd < 1f) { rx = -dx; ry = -dy; rd = dist; }
                gx = rx / rd * ROUT_SPEED;
                gy = ry / rd * ROUT_SPEED;
            }
        } else if (e.hunt) {                                                 // a cache's ambush: straight at the hull
            gx = ux * cruise * 1.2f;
            gy = uy * cruise * 1.2f;
        } else {
            Enemy lead = leaderOf(e);
            if (lead == null) leadTick(e, w, dt);
            float[] g = G;
            g[0] = g[1] = Float.NaN;
            volleys(e, w, dt, hull, lead, dist, ux, uy);
            action(e, w, dt, hull, dist, ux, uy, g);
            if (!Float.isNaN(g[0])) {                                        // a personal action has the wheel (a dart run, a drift in smoke, a held line)
                gx = g[0];
                gy = g[1];
            } else if (lead != null) {
                float[] s = slot(e, w, lead, hull, t);
                float sx = s[0] - e.x, sy = s[1] - e.y, sd = (float) Math.hypot(sx, sy);
                float sp = Math.min(cruise * 1.7f, sd * GAIN);
                if (sd > 1f) { gx = sx / sd * sp; gy = sy / sd * sp; }
            } else {
                float[] s = stalk(e, w, hull, dist, ux, uy, cruise, t);
                gx = s[0];
                gy = s[1];
            }
            // free-fire cadence for the kinds that shoot between volleys
            betweenShots(e, w, hull, dist);
        }
        if (!e.routed && !e.hunt && !e.dismissed) {                          // the post pulls back whoever has gone too far from it
            float hx = e.x - e.spawnX, hy = e.y - e.spawnY, hd = (float) Math.sqrt(hx * hx + hy * hy);
            if (hd > FOLLOWER_LEASH) {
                gx -= hx / hd * (hd - FOLLOWER_LEASH) * 4f;
                gy -= hy / hd * (hd - FOLLOWER_LEASH) * 4f;
            }
        }
        float[] b = bend(e, w, gx, gy);
        dodge(e, w, dt);
        b[0] += e.dodgeVx;
        b[1] += e.dodgeVy;
        float k = Math.min(1f, TURN * dt);
        if (e.vState == 2 && e.kind == Enemy.Kind.VAKRA) k = 1f;            // a dart run is not steered, it is thrown
        e.vx += (b[0] - e.vx) * k;
        e.vy += (b[1] - e.vy) * k;
    }

    private static final float[] G = new float[2], S = new float[2], B = new float[2];

    // ---- dodging: a quick unit sidesteps a round that is coming at it --------------------------------------------------------------

    /** How hard each kind can throw itself aside, px/s (Tamra, armoured and slow, does not try). */
    private static float dodgePower(Enemy.Kind k) {
        switch (k) {
            case VAKRA: return 520f;
            case AURIS: return 460f;
            case LANCE: return 400f;
            case HATAKA: return 340f;
            case MAYAVI: return 240f;
            default: return 0f;
        }
    }

    /** A round of the pilot's that will pass through a unit's own width within 300 px makes it sidestep (a leader or an elite nearly always, the rank and file now and then,
     *  never twice in 0.45 s, and not in the middle of a dart run or in smoke). The sidestep is added to its flight and dies away. */
    private static void dodge(Enemy e, World w, float dt) {
        float k = (float) Math.exp(-5.5f * dt);
        e.dodgeVx *= k;
        e.dodgeVy *= k;
        float power = dodgePower(e.kind);
        if (power <= 0f || e.dodgeCd > 0f && (e.dodgeCd -= dt) > 0f) return;
        if (e.routed || e.smoked || e.vState == 2 || e.inRock) return;
        float skill = e.rank >= 1 || e.elite ? 0.9f : 0.5f, half = Math.max(10f, e.w * 0.5f);
        int look = Math.min(w.bullets.size(), 120);
        for (int i = 0; i < look; i++) {
            vajra.weapon.Bullet b = w.bullets.get(i);
            if (b.hostile || b.dead) continue;
            float sp = (float) Math.hypot(b.vx, b.vy);
            if (sp < 1f) continue;
            float ux = b.vx / sp, uy = b.vy / sp, rx = e.x - b.x, ry = e.y - b.y, along = rx * ux + ry * uy;
            if (along < 0f || along > 300f) continue;
            float cross = ux * ry - uy * rx;
            if (Math.abs(cross) > half + b.radius + 12f) continue;
            e.dodgeCd = 0.45f;
            if (w.fxRng.nextFloat() > skill) return;                            // it did not see it in time
            float side = cross >= 0f ? 1f : -1f;
            e.dodgeVx = -uy * side * power * (e.elite ? 1.2f : 1f);
            e.dodgeVy = ux * side * power * (e.elite ? 1.2f : 1f);
            return;
        }
    }

    // ---- the leader's clock: the formation it flies, the trail its column follows -------------------------------------------------

    private static void leadTick(Enemy e, World w, float dt) {
        e.formT += dt;
        if (e.trail == null) {
            e.trail = new float[TRAIL_N * 2];
            for (int i = 0; i < TRAIL_N; i++) { e.trail[2 * i] = e.x; e.trail[2 * i + 1] = e.y; }
        }
        e.trailT += dt;
        if (e.trailT >= TRAIL_DT) {
            e.trailT -= TRAIL_DT;
            e.trailAt = (e.trailAt + 1) % TRAIL_N;
            e.trail[2 * e.trailAt] = e.x;
            e.trail[2 * e.trailAt + 1] = e.y;
        }
        int[] forms = formsOf(e.kind);
        if (e.formIdx == 0 && e.formT < dt * 2f) e.form = forms[0];
        if (e.formT >= 2f * VOLLEY) {                                        // two volleys, then the other formation
            e.formT = 0f;
            e.formIdx++;
            e.form = forms[e.formIdx & 1];
        }
    }

    /** Where a follower belongs in its leader's formation. */
    private static float[] slot(Enemy e, World w, Enemy L, Ship hull, float t) {
        e.slotT -= 1f / 60f;
        if (e.slotT <= 0f) {                                                 // recount the formation twice a second
            e.slotT = 0.5f;
            int n = 0, k = 0;
            for (int i = 0; i < w.enemies.size(); i++) {
                Enemy o = w.enemies.get(i);
                if (o.dead || o.commander != L) continue;
                if (o == e) k = n;
                n++;
            }
            e.slot = k;
            e.slotN = Math.max(1, n);
        }
        int k = e.slot, n = e.slotN;
        float lx = hull.x - L.x, ly = hull.y - L.y, ld = Math.max(1f, (float) Math.hypot(lx, ly));
        float ux = lx / ld, uy = ly / ld, px = -uy, py = ux;
        float tx, ty;
        int form = L.form;
        if ((form == F_PINCER || form == F_CIRCLE) && Math.hypot(hull.x - L.spawnX, hull.y - L.spawnY) > ENGAGE) form = F_WEDGE;   // the hull is not here yet: hold the post
        switch (form) {
            case F_WEDGE: {
                int side = (k & 1) == 0 ? -1 : 1, rank = k / 2 + 1;
                tx = L.x - ux * rank * 52f + px * side * rank * 46f;
                ty = L.y - uy * rank * 52f + py * side * rank * 46f;
                break;
            }
            case F_COLUMN: {
                int idx = Math.floorMod(L.trailAt - (k + 1) * 5, TRAIL_N);
                tx = L.trail != null ? L.trail[2 * idx] : L.x;
                ty = L.trail != null ? L.trail[2 * idx + 1] : L.y;
                break;
            }
            case F_PINCER: {
                int side = (k & 1) == 0 ? -1 : 1;
                float frac = Math.min(1f, L.formT / 3.2f), r = 560f - 270f * frac;
                tx = hull.x + px * side * r - ux * (k / 2) * 46f;
                ty = hull.y + py * side * r - uy * (k / 2) * 46f;
                break;
            }
            case F_CIRCLE: {
                float a = k * 6.2831853f / n + L.clockT * 0.55f, r = 340f + 18f * (float) Math.sin(L.clockT + k);
                tx = hull.x + (float) Math.cos(a) * r;
                ty = hull.y + (float) Math.sin(a) * r;
                break;
            }
            default: {                                                       // F_LINE: abreast across the leader
                float off = k - (n - 1) / 2f;
                tx = L.x + px * off * 58f - ux * 28f;
                ty = L.y + py * off * 58f - uy * 28f;
                break;
            }
        }
        S[0] = tx;
        S[1] = ty;
        return S;
    }

    /** A leader's, or a loner's, own movement: fight at the kind's range, circling and strafing, held near its post; sentries keep the post. */
    private static float[] stalk(Enemy e, World w, Ship hull, float dist, float ux, float uy, float cruise, float t) {
        float tx, ty;
        float engaged = (float) Math.hypot(hull.x - e.spawnX, hull.y - e.spawnY);
        if (e.sentry || engaged > ENGAGE) {                                  // a post, or a hull still too far to be worth leaving it for
            tx = e.homeX + 30f * (float) Math.sin(t * 0.7f + e.phase);
            ty = e.homeY + 22f * (float) Math.cos(t * 0.5f + e.phase);
        } else if (e.kind == Enemy.Kind.BHAVI) {                             // Tamra: in front of the shooters, then at the hull
            Enemy ally = nearestShooter(e, w, 700f);
            if (ally != null && dist > 150f) {
                float ax = hull.x - ally.x, ay = hull.y - ally.y, ad = Math.max(1f, (float) Math.hypot(ax, ay));
                tx = ally.x + ax / ad * 110f;
                ty = ally.y + ay / ad * 110f;
            } else {
                tx = hull.x;
                ty = hull.y;
            }
        } else {
            float dirn = (e.phase > 3.14f) ? 1f : -1f;
            float speedA = e.kind == Enemy.Kind.LANCE ? 0.55f : e.kind == Enemy.Kind.MAYAVI ? 0.18f : 0.32f;
            float a = (float) Math.atan2(e.y - hull.y, e.x - hull.x) + dirn * speedA * (1f / 60f) * 24f;
            float r = range(e.kind) + 40f * (float) Math.sin(t * 0.6f + e.phase);
            tx = hull.x + (float) Math.cos(a) * r;
            ty = hull.y + (float) Math.sin(a) * r;
            if (e.kind == Enemy.Kind.MAYAVI && dist < range(e.kind) * 0.75f) {   // Jvala keeps away
                tx = e.x - ux * 200f;
                ty = e.y - uy * 200f;
            }
        }
        float leash = e.sentry ? 160f : LEASH;
        float hx = tx - e.homeX, hy = ty - e.homeY, hd = (float) Math.hypot(hx, hy);
        if (hd > leash) { tx = e.homeX + hx / hd * leash; ty = e.homeY + hy / hd * leash; }
        float sx = tx - e.x, sy = ty - e.y, sd = (float) Math.hypot(sx, sy);
        float sp = Math.min(cruise * 1.3f, sd * 1.8f);
        S[0] = sd > 1f ? sx / sd * sp : 0f;
        S[1] = sd > 1f ? sy / sd * sp : 0f;
        return S;
    }

    private static Enemy nearestShooter(Enemy e, World w, float range) {
        Enemy best = null;
        float bd = range * range;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy o = w.enemies.get(i);
            if (o == e || o.dead || !o.vitala || o.kind == Enemy.Kind.BHAVI || o.kind.keeper() || o.boss) continue;
            float dx = o.x - e.x, dy = o.y - e.y, d = dx * dx + dy * dy;
            if (d < bd) { bd = d; best = o; }
        }
        return best;
    }

    private static Enemy findSenior(Enemy e, World w) {
        Enemy best = null;
        float bd = 700f * 700f;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy o = w.enemies.get(i);
            if (o == e || o.dead || !o.vitala || o.rank < 1 || o.routed || o.boss) continue;
            float dx = o.x - e.x, dy = o.y - e.y, d = dx * dx + dy * dy;
            if (d < bd) { bd = d; best = o; }
        }
        return best;
    }

    // ---- the volley: when a formation fires, each kind does its own thing --------------------------------------------------------

    private static void volleys(Enemy e, World w, float dt, Ship hull, Enemy lead, float dist, float ux, float uy) {
        Enemy src = lead != null ? lead : e;
        int slot = lead != null ? e.slot : 0;
        float clock = src.clockT - stagger(src.form, slot);
        if (clock < 0f) return;
        int cyc = (int) Math.floor(clock / VOLLEY);
        if (cyc == e.cycle) return;
        e.cycle = cyc;
        if (e.sentry && dist > 760f) return;                                  // a post only answers what comes near it
        if (dist > 1300f || e.vState != 0) return;
        if (!w.clearLine(e.x, e.y, hull.x, hull.y) && e.kind != Enemy.Kind.HATAKA && e.kind != Enemy.Kind.MAYAVI) return;
        if (lead == null && e.kind == Enemy.Kind.HATAKA) e.vA = e.facing;      // a Rekha leader sets the angle the whole grid is drawn at
        switch (e.kind) {
            case VAKRA:                                                       // Agneya: the dart run begins with a tell
                e.vState = 1;
                e.vT = 0.5f;
                w.addEffect(Effect.flash(e.x, e.y, hull.x, hull.y, 1f, 0.55f, 0.15f));
                break;
            case LANCE:                                                       // Kila: a laser tell, then the burst
                e.vState = 1;
                e.vT = 0.42f;
                break;
            case AURIS: {                                                     // Dhuma: smoke, then a place close to the hull
                e.vState = 1;
                e.vT = 1.5f;
                e.smoked = true;
                float a = (float) Math.atan2(e.y - hull.y, e.x - hull.x) + (e.phase > 3f ? 1f : -1f) * (0.9f + 0.6f * (float) Math.sin(e.clockT));
                float r = 230f + 60f * (float) Math.abs(Math.sin(e.clockT * 2f));
                e.vX = hull.x + (float) Math.cos(a) * r;
                e.vY = hull.y + (float) Math.sin(a) * r;
                if (!w.openFor(e.vX, e.vY, 14f)) { e.vX = hull.x - ux * r; e.vY = hull.y - uy * r; }
                w.addEffect(Effect.ring(e.x, e.y, 26, 0.7f, 0.75f, 0.9f));
                break;
            }
            case BHAVI: {                                                     // Tamra: a slag shell, and the ring if it is touched
                float aim = e.facing;
                fire(w, e, SLAG, aim);
                if (dist < 170f) {
                    w.addEffect(Effect.ring(e.x, e.y, 90, 0.9f, 0.5f, 0.2f));
                    for (int i = 0; i < 8; i++)
                        w.spawnEnemyBullet(e.x, e.y, aim + i * 0.7853982f, SLAG_PELLET, e.dmgScale * Enemy.RANK_DAMAGE[e.rankGrade()]);
                }
                break;
            }
            case MAYAVI: {                                                    // Jvala: ground where the hull is GOING
                float px = hull.x + hull.vx * 0.9f, py = hull.y + hull.vy * 0.9f;
                w.hazards.patch(px, py, 52f, 6f);
                if (src.form == F_COLUMN || src.form == F_CIRCLE) {
                    w.hazards.patch(px + uy * 70f, py - ux * 70f, 40f, 5f);
                    w.hazards.patch(px - uy * 70f, py + ux * 70f, 40f, 5f);
                }
                w.addEffect(Effect.ring(px, py, 52, 1f, 0.5f, 0.15f));
                break;
            }
            case HATAKA: {                                                    // Rekha: a hairline in the grid's angle, held while it draws and arms
                float ang = lead != null ? lead.vA : e.facing;
                float len = Math.max(320f, Math.min(900f, dist + 420f));
                float ex = (float) Math.cos(ang), ey = (float) Math.sin(ang);
                len = Math.min(len, w.wallDist(e.x, e.y, ex, ey, len) - 10f);
                if (len > 120f) {
                    w.hazards.line(e.x, e.y, e.x + ex * len, e.y + ey * len);
                    e.vState = 2;
                    e.vT = Hazards.DRAW_T + Hazards.ARMED_T + 0.1f;
                }
                break;
            }
            default:
                break;
        }
    }

    // ---- personal actions that take the wheel -------------------------------------------------------------------------------------

    private static void action(Enemy e, World w, float dt, Ship hull, float dist, float ux, float uy, float[] g) {
        if (e.vState == 0) return;
        e.vT -= dt;
        switch (e.kind) {
            case VAKRA:
                if (e.vState == 1) {                                          // telegraph: nearly still, then lock the line
                    g[0] = ux * 20f;
                    g[1] = uy * 20f;
                    if (e.vT <= 0f) {
                        e.vState = 2;
                        e.vT = 0.55f;
                        float d = Math.max(1f, dist);
                        e.vX = ux * 780f;
                        e.vY = uy * 780f;
                        e.vT2 = 0f;
                    }
                } else {                                                      // the dart run: thrown straight, burning ground behind it
                    g[0] = e.vX;
                    g[1] = e.vY;
                    e.vT2 -= dt;
                    if (e.vT2 <= 0f) {
                        e.vT2 = 0.07f;
                        w.hazards.patch(e.x, e.y, 20f, 2.4f);
                    }
                    if (e.vT <= 0f) { e.vState = 0; e.fireTimer = 0.6f; }
                }
                break;
            case LANCE:
                g[0] = 0f;
                g[1] = 0f;
                if (e.vT > 0f) w.addEffect(Effect.flash(e.x, e.y, hull.x, hull.y, 1f, 0.9f, 0.5f));
                else {
                    fire(w, e, NEEDLE, e.facing);
                    e.vState = 0;
                }
                break;
            case AURIS: {
                float sx = e.vX - e.x, sy = e.vY - e.y, sd = (float) Math.hypot(sx, sy);
                float sp = Math.min(640f, sd * 5f);
                g[0] = sd > 1f ? sx / sd * sp : 0f;
                g[1] = sd > 1f ? sy / sd * sp : 0f;
                if (e.vT <= 0f) {                                             // out of the smoke, aiming at the hull
                    e.smoked = false;
                    e.vState = 0;
                    w.addEffect(Effect.ring(e.x, e.y, 30, 0.8f, 0.85f, 1f));
                    fire(w, e, SMOKE_BOLT, (float) Math.atan2(hull.y - e.y, hull.x - e.x));
                }
                break;
            }
            case HATAKA:                                                      // Rekha holds still while its line draws, arms and fires
                g[0] = 0f;
                g[1] = 0f;
                if (e.vT <= 0f) e.vState = 0;
                break;
            default:
                e.vState = 0;
                break;
        }
    }

    /** Agneya and Tamra shoot between volleys; the rest only act on the formation's clock. */
    private static void betweenShots(Enemy e, World w, Ship hull, float dist) {
        if (e.vState != 0 || e.fireTimer > 0f || e.routed || e.inRock) return;
        if (e.kind != Enemy.Kind.VAKRA) return;
        if (dist > 640f || !w.clearLine(e.x, e.y, hull.x, hull.y)) return;
        e.fireTimer = 2.2f * (e.hp < e.maxHp * 0.3f ? 0.7f : 1f);
        fire(w, e, EMBER, e.facing);
    }

    // ---- flying round rock ---------------------------------------------------------------------------------------------------------

    /** Bend a wish around rock: try the heading, then 35, 70 and 110 degrees either side, taking the first that is open 56 px ahead. */
    /** Steer a wish round rock (the same whiskers the brain uses): public so a unit flying home can use them too. */
    public static float[] steer(Enemy e, World w, float gx, float gy) {
        float[] r = bend(e, w, gx, gy);
        return new float[] {r[0], r[1]};
    }

    private static float[] bend(Enemy e, World w, float gx, float gy) {
        float sp = (float) Math.hypot(gx, gy);
        if (sp < 1f || e.phasing) { B[0] = gx; B[1] = gy; return B; }
        float a0 = (float) Math.atan2(gy, gx), half = Math.max(12f, e.w * 0.5f);
        float[] turns = {0f, 0.61f, -0.61f, 1.22f, -1.22f, 1.92f, -1.92f};
        for (float turn : turns) {
            float a = a0 + turn;
            float px = e.x + (float) Math.cos(a) * 56f, py = e.y + (float) Math.sin(a) * 56f;
            if (w.openFor(px, py, half)) {
                float s = turn == 0f ? sp : sp * 0.85f;
                B[0] = (float) Math.cos(a) * s;
                B[1] = (float) Math.sin(a) * s;
                return B;
            }
        }
        B[0] = gx * 0.2f;
        B[1] = gy * 0.2f;
        return B;
    }
}

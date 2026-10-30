package vajra.enemy;

import vajra.ship.Ship;
import vajra.weapon.WeaponDef;
import vajra.world.Effect;
import vajra.world.World;

/** BHAIRAVA, HERALD OF CINDERS (docs/VITALA_ENEMIES.md, docs/VITALA_REDESIGN.md): Hara at the cremation ground, the Lord of Burning Gold. His signature, from the original
 *  bestiary, is the ASH: burning ground you cannot cross, and a safe arena that SHRINKS over the fight.
 *
 *  <p><b>Four phases, gated.</b> His hull is cut at 75%, 50% and 25%. At each cut his hull STOPS (Enemy.hurt holds it there; no blow can take more than 6% of it), he is untouchable
 *  while the phase turns (a stagger, a roar that shoves the hull, a word), and only then does the next begin. No burst can skip a phase; the last ends in a yield.
 *  <ol>
 *   <li><b>Stoking</b>: ash charge, ember fan, hearth burst, ash prowl;</li>
 *   <li><b>Kindling</b>: + cinder rain, gold spit, the court is called, the rim vents open, the ash cloud;</li>
 *   <li><b>The Flood</b>: + the third eye, pillars of fire; the Hataki floods the hall;</li>
 *   <li><b>The Last Fire</b>: all of it, faster and chained, and the immolation (rings of embers with a gap to fly through).</li>
 *  </ol>
 *  <p><b>He reads the pilot</b>: a hull that hugs him is answered with the ring or a charge; one that keeps away with the fan, the spit or the eye; one that stands still with ash and pillars
 *  laid under it. He never repeats an act, and a long barrage makes his next act come sooner. Every act has a pose the renderer shows (Enemy.pose) before the blow. */
public final class BhairavaBrain {
    private BhairavaBrain() {}

    public static final float SAFE_MAX = 800f, SAFE_MIN = 300f;
    /** Gate heights (hull fractions): the hull stops at each in turn. */
    public static final float[] GATES = {0.75f, 0.50f, 0.25f, 0f};
    /** No single blow takes more than this share of his hull. */
    public static final float HIT_CAP = 0.06f;
    /** ...and no faster than this a second over a stretch: a bucket of this share of his hull that refills at REFILL of it a second. A burst cannot empty him; steady fire is not slowed. */
    public static final float BUCKET = 0.12f, REFILL = 0.07f;

    // poses (Enemy.pose): what the renderer draws him doing
    public static final int IDLE = 0, WINDUP = 1, LUNGE = 2, ROAR = 3, CAST = 4, STAGGER = 5;

    // acts (Enemy.vState)
    static final int HOVER = 0, CHARGE_TELL = 1, CHARGE = 2, FAN = 3, RAIN = 4, EYE_TELL = 5, EYE = 6, CLOUD = 7, BURST = 8, SPIT = 9, PILLARS = 10, IMMOLATE = 11, NOVA = 12, TURN = 20;
    static final int A_CHARGE = 0, A_FAN = 1, A_BURST = 2, A_PROWL = 3, A_RAIN = 4, A_SPIT = 5, A_EYE = 6, A_PILLARS = 7, A_IMMOLATE = 8, A_CLOUD = 9;

    private static final WeaponDef FAN_GUN = WeaponDef.mk("bhairava ember").count(5).spreadDeg(48f).speed(330).damage(11).bulletSize(7).life(3.4f).colour(1f, 0.45f, 0.12f);
    private static final WeaponDef RING_GUN = WeaponDef.mk("bhairava hearth").speed(290).damage(9).bulletSize(6).life(3.2f).colour(1f, 0.55f, 0.18f);
    private static final WeaponDef NOVA_GUN = WeaponDef.mk("bhairava nova").speed(250).damage(10).bulletSize(7).life(4.2f).colour(1f, 0.82f, 0.4f);
    private static final WeaponDef SPIT_GUN = WeaponDef.mk("bhairava gold").speed(300).damage(8).bulletSize(9).life(1.1f).colour(1f, 0.78f, 0.2f);

    private static final String[] TURN_LINES_2 = {"A THOUSAND PILGRIMS CAME TO THIS GROUND.", "EACH BEGGED ME IN A DIFFERENT TONGUE."};
    private static final String[] TURN_LINES_3 = {"THE RIVER REMEMBERS WHERE IT WAS BORN.", "LET IT CARRY YOU BACK."};
    private static final String[] TURN_LINES_4 = {"THE ASH RISES. LET IT TAKE YOUR NAME FIRST."};
    private static final String[][] CLOUD_LINES = {{"BREATHE. THE ASH WILL TEACH YOU PATIENCE."}, {"THE SMOKE REMEMBERS EVERY ONE WHO WALKED IT."}, {"SLOW, NOW. THE FIRE IS NOT IN A HURRY."}};

    /** The radius of the safe arena at a hull fraction: it closes as he falls. */
    public static float safeRadius(float hpFrac) {
        float f = Math.max(0f, Math.min(1f, hpFrac));
        return SAFE_MIN + (SAFE_MAX - SAFE_MIN) * (float) Math.pow(f, 0.8);
    }

    /** The phase a hull fraction belongs to (1..4), for a scene that starts mid-fight. */
    public static int phaseOf(float frac) {
        return frac > GATES[0] ? 1 : frac > GATES[1] ? 2 : frac > GATES[2] ? 3 : 4;
    }

    private static void pose(Enemy e, int p, float secs) {
        e.pose = p;
        e.poseT = secs;
    }

    /** A patch of ground that will burn, announced now and lit after `delay` (the tell is a ring on the spot). */
    private static void plant(Enemy e, float x, float y, float r, float delay) {
        if (e.pendN >= e.pend.length / 4) return;
        int i = e.pendN++ * 4;
        e.pend[i] = x;
        e.pend[i + 1] = y;
        e.pend[i + 2] = delay;
        e.pend[i + 3] = r;
    }

    private static void tickPending(Enemy e, World w, float dt) {
        for (int i = e.pendN - 1; i >= 0; i--) {
            int k = i * 4;
            e.pend[k + 2] -= dt;
            float left = e.pend[k + 2];
            if (((int) (left * 8f) & 1) == 0 && left > 0f) w.addEffect(Effect.ring(e.pend[k], e.pend[k + 1], e.pend[k + 3] * (0.6f + 0.4f * left), 1f, 0.45f, 0.12f));
            if (left <= 0f) {
                w.hazards.patch(e.pend[k], e.pend[k + 1], e.pend[k + 3], 6f);
                w.addEffect(Effect.ring(e.pend[k], e.pend[k + 1], e.pend[k + 3] * 1.6f, 1f, 0.8f, 0.3f));
                e.pendN--;
                System.arraycopy(e.pend, e.pendN * 4, e.pend, k, 4);
            }
        }
    }

    public static void think(Enemy e, World w, float dt, float t) {
        Ship hull = w.nearestHull(e.x, e.y);
        float dx = hull.x - e.x, dy = hull.y - e.y;
        float dist = Math.max(1f, (float) Math.hypot(dx, dy));
        float ux = dx / dist, uy = dy / dist;
        e.facing = (float) Math.atan2(dy, dx);
        e.clockT += dt;
        e.cloudCd -= dt;
        e.poseT -= dt;
        if (e.poseT <= 0f && e.vState != TURN) e.pose = IDLE;
        if (!e.gateInit) {                                 // a fight that begins mid-way (a scene, the arena) takes its phase from the hull
            e.gateInit = true;
            e.bossPhase = phaseOf(e.hp / e.maxHp);
            e.lastPhase = e.bossPhase;
            e.gateHp = GATES[e.bossPhase - 1] * e.maxHp;
            e.fireTimer = 1.4f;
        }
        float frac = e.hp / e.maxHp;
        tickPending(e, w, dt);
        // THE GATE: his hull has stopped at a cut; the phase turns
        if (e.gateHit && e.vState != TURN) {
            e.gateHit = false;
            e.vState = TURN;
            e.vT = 3.4f;
            e.vT2 = 0f;
            e.vN = 0;
            e.bossInvuln = true;
            pose(e, STAGGER, 1.0f);
            w.hazards.patches.clear();
        }
        // the ash ring: outside it the floor burns
        w.hazards.ring = true;
        w.hazards.ringX = w.pitX;
        w.hazards.ringY = w.pitY;
        w.hazards.ringR = safeRadius(frac) - (e.bossPhase == 4 ? 30f * (float) Math.abs(Math.sin(e.clockT)) : 0f);

        float gx = 0f, gy = 0f, cruise = 150f;
        switch (e.vState) {
            case HOVER: {                                  // prowl the hearth, a little closer each phase, leaning toward the hull
                float a = e.clockT * (0.30f + 0.06f * e.bossPhase), rad = 210f - 20f * e.bossPhase;
                float tx = w.pitX + (float) Math.cos(a) * rad + ux * 40f, ty = w.pitY + (float) Math.sin(a) * rad * 0.8f + uy * 40f;
                gx = (tx - e.x) * 1.4f;
                gy = (ty - e.y) * 1.4f;
                if (e.fireTimer <= 0f && dist < 1300f) choose(e, w, hull, dist, ux, uy);
                else e.fireTimer -= dt * (e.hitT > 0f ? 1.6f : 1f);
                break;
            }
            case CHARGE_TELL: {                            // rear back, flare, and mark the line of the run
                gx = -ux * 70f;
                gy = -uy * 70f;
                e.vT -= dt;
                float lx = hull.x + hull.vx * 0.45f, ly = hull.y + hull.vy * 0.45f;
                float ax = lx - e.x, ay = ly - e.y, al = Math.max(1f, (float) Math.hypot(ax, ay));
                e.vX = ax / al;
                e.vY = ay / al;
                w.addEffect(Effect.flash(e.x, e.y, e.x + e.vX * 900f, e.y + e.vY * 900f, 1f, 0.45f, 0.12f));
                if (e.vT <= 0f) {
                    e.vState = CHARGE;
                    e.vT = 0.78f;
                    e.vT2 = 0f;
                    e.vX *= 780f;
                    e.vY *= 780f;
                    pose(e, LUNGE, 0.8f);
                }
                break;
            }
            case CHARGE:
                gx = e.vX;
                gy = e.vY;
                e.vT -= dt;
                e.vT2 -= dt;
                if (e.vT2 <= 0f) { e.vT2 = 0.07f; w.hazards.patch(e.x, e.y, 36f, 7f); }
                if (e.vT <= 0f) {
                    if (--e.vN > 0) { e.vState = CHARGE_TELL; e.vT = 0.45f; pose(e, WINDUP, 0.45f); }
                    else rest(e, w);
                }
                break;
            case FAN:                                      // ember fans, aimed ahead of the hull, turning a little each volley
                e.vT -= dt;
                gx = -ux * 30f;
                gy = -uy * 30f;
                if (e.vT <= 0f) {
                    float lead = (dist / FAN_GUN.speed) * 0.7f;
                    float aim = (float) Math.atan2(hull.y + hull.vy * lead - e.y, hull.x + hull.vx * lead - e.x) + (e.vN % 2 == 0 ? 0.12f : -0.12f) * (e.bossPhase >= 3 ? 1f : 0f);
                    VitalaBrain.fire(w, e, FAN_GUN, aim);
                    pose(e, CAST, 0.25f);
                    if (--e.vN > 0) e.vT = 0.5f; else rest(e, w);
                }
                break;
            case BURST:                                    // the hearth bursts: a ring of embers
                e.vT -= dt;
                if (e.vT <= 0f) {
                    int n = e.bossPhase >= 4 ? 18 : 12;
                    for (int i = 0; i < n; i++) w.spawnEnemyBullet(e.x, e.y, e.vA + i * 6.2831853f / n, RING_GUN, e.dmgScale);
                    w.addEffect(Effect.ring(e.x, e.y, 120f, 1f, 0.6f, 0.2f));
                    pose(e, ROAR, 0.4f);
                    e.vA += 0.26f;
                    if (--e.vN > 0) e.vT = 0.7f; else rest(e, w);
                }
                break;
            case RAIN:                                     // cinder rain: ash marked in a closing ring round the hull, lit a beat later
                e.vT -= dt;
                if (e.vT <= 0f) {
                    for (int i = 0; i < 6; i++) {
                        float a = i * 1.0471976f + e.vA, r = e.vN * 55f;
                        plant(e, hull.x + hull.vx * 0.5f + (float) Math.cos(a) * r, hull.y + hull.vy * 0.5f + (float) Math.sin(a) * r, 34f, 0.7f);
                    }
                    pose(e, CAST, 0.3f);
                    e.vA += 0.5f;
                    e.vT = 0.4f;
                    if (--e.vN <= 0) rest(e, w);
                }
                break;
            case SPIT:                                     // gold spit: three globs lobbed ahead of the hull; each leaves ground that burns where it lands
                e.vT -= dt;
                if (e.vT <= 0f) {
                    for (int i = -1; i <= 1; i++) {
                        float tx = hull.x + hull.vx * 0.9f + i * 120f, ty = hull.y + hull.vy * 0.9f + (i % 2) * 70f;
                        float a = (float) Math.atan2(ty - e.y, tx - e.x);
                        w.spawnEnemyBullet(e.x, e.y, a, SPIT_GUN, e.dmgScale);
                        plant(e, tx, ty, 44f, 1.0f);
                    }
                    pose(e, CAST, 0.4f);
                    if (--e.vN > 0) e.vT = 0.8f; else rest(e, w);
                }
                break;
            case PILLARS:                                  // pillars of fire: marked where the hull will be, then they stand
                e.vT -= dt;
                if (e.vT <= 0f) {
                    for (int i = 0; i < 3 + e.bossPhase - 3; i++) {
                        float a = e.vA + i * 2.2f, r = 60f + 70f * i;
                        plant(e, hull.x + hull.vx * 0.8f + (float) Math.cos(a) * r, hull.y + hull.vy * 0.8f + (float) Math.sin(a) * r, 58f, 1.1f);
                    }
                    e.vA += 1.3f;
                    pose(e, CAST, 0.35f);
                    if (--e.vN > 0) e.vT = 0.9f; else rest(e, w);
                }
                break;
            case EYE_TELL:                                 // the third eye: a long tell, then the sweep
                e.vT -= dt;
                pose(e, CAST, 0.2f);
                w.addEffect(Effect.flash(e.x, e.y, e.x + (float) Math.cos(e.vA) * 900f, e.y + (float) Math.sin(e.vA) * 900f, 1f, 0.9f, 0.5f));
                if (e.vT <= 0f) { e.vState = EYE; e.vT = 1.5f; e.vT2 = 0f; e.vB = e.vB == 0f ? 1f : e.vB; }
                break;
            case EYE:                                      // the sweep (the last phase turns it back on the way and sweeps again)
                e.vT -= dt;
                e.vT2 -= dt;
                e.vA += 0.9f * e.vB * dt;
                if (e.vT2 <= 0f) {
                    e.vT2 = 0.1f;
                    w.laser(e.x, e.y, e.vA, 900f, 1f, 0.8f, 0.3f, 9f);
                }
                if (e.vT <= 0f) {
                    if (e.bossPhase >= 4 && e.vB > 0f) { e.vB = -1f; e.vT = 1.5f; } else rest(e, w);
                }
                break;
            case CLOUD:                                    // the ash gathers: he stands still while the smoke is called, then it spreads over the hall
                gx = 0f;
                gy = 0f;
                e.vT -= dt;
                pose(e, CAST, 0.2f);
                if (((int) (e.vT * 10f) & 1) == 0) w.addEffect(Effect.ring(e.x, e.y, 60f + 220f * (1f - e.vT / 1.3f), 0.7f, 0.72f, 0.78f));
                if (e.vT <= 0f) {
                    w.ash.start(w.pitX, w.pitY, e.bossPhase >= 3 ? 780f : 700f, e.bossPhase >= 3 ? 12f : 9f);
                    w.say(e, CLOUD_LINES[e.vN2 % CLOUD_LINES.length]);
                    e.cloudCd = e.bossPhase >= 3 ? 17f : 25f;
                    pose(e, ROAR, 0.5f);
                    rest(e, w);
                }
                break;
            case IMMOLATE:                                 // the immolation gathers: the ash is drawn in to him, the eye flares
                gx = 0f;
                gy = 0f;
                e.vT -= dt;
                pose(e, CAST, 0.2f);
                if (((int) (e.vT * 12f) & 1) == 0) w.addEffect(Effect.ring(e.x, e.y, 340f * (e.vT / 2.4f) + 30f, 1f, 0.7f, 0.25f));
                if (e.vT <= 0f) { e.vState = NOVA; e.vT = 0f; e.vN = 3; e.vA = (float) Math.atan2(uy, ux); }
                break;
            case NOVA:                                     // three rings of embers, each with a gap, the gap turning toward where the hull is
                gx = 0f;
                gy = 0f;
                e.vT -= dt;
                if (e.vT <= 0f) {
                    int n = 30;
                    float gap = (float) Math.atan2(uy, ux) + (e.vN - 2) * 0.6f;
                    for (int i = 0; i < n; i++) {
                        float a = i * 6.2831853f / n, d = Math.abs(World.wrapAngle(a - gap));
                        if (d < 0.38f) continue;
                        w.spawnEnemyBullet(e.x, e.y, a, NOVA_GUN, e.dmgScale);
                    }
                    w.addEffect(Effect.ring(e.x, e.y, 200f, 1f, 0.85f, 0.4f));
                    pose(e, ROAR, 0.5f);
                    e.vT = 0.75f;
                    if (--e.vN <= 0) rest(e, w);
                }
                break;
            default: {                                     // THE TURN between phases: he staggers, then he roars and the phase begins
                e.vT -= dt;
                gx = 0f;
                gy = 0f;
                if (e.vT > 2.4f) pose(e, STAGGER, 0.3f);
                else if (e.vT > 1.0f) {
                    pose(e, ROAR, 0.3f);
                    if (e.vN == 0) {                       // the roar: a ring that shoves the hull
                        e.vN = 1;
                        w.addEffect(Effect.ring(e.x, e.y, 520f, 1f, 0.8f, 0.4f));
                        w.addEffect(Effect.ring(e.x, e.y, 260f, 1f, 1f, 0.8f));
                        hull.vx += ux * 520f;
                        hull.vy += uy * 520f;
                        hull.trauma = Math.min(1f, hull.trauma + 0.7f);
                        w.snd(vajra.audio.Snd.BOSS_TELEGRAPH, e.x, e.y, 0, 0f);
                    }
                } else if (e.vN == 1) {                    // and the phase turns
                    e.vN = 2;
                    e.bossPhase = Math.min(4, e.bossPhase + 1);
                    e.lastPhase = e.bossPhase;
                    e.gateHp = GATES[e.bossPhase - 1] * e.maxHp;
                    w.say(e, e.bossPhase == 2 ? TURN_LINES_2 : e.bossPhase == 3 ? TURN_LINES_3 : TURN_LINES_4);
                    if (e.bossPhase == 2) summon(e, w);
                    if (e.bossPhase >= 2) e.cloudCd = Math.min(Math.max(e.cloudCd, 0f), 4f);
                }
                if (e.vT <= 0f) {
                    e.bossInvuln = false;
                    e.gateHit = false;
                    rest(e, w);
                }
                break;
            }
        }
        float k = Math.min(1f, 4f * dt);
        e.vx += (Math.max(-cruise * 6f, Math.min(cruise * 6f, gx)) - e.vx) * k;
        e.vy += (Math.max(-cruise * 6f, Math.min(cruise * 6f, gy)) - e.vy) * k;
    }

    private static void rest(Enemy e, World w) {
        e.vState = HOVER;
        float base = e.bossPhase == 1 ? 2.4f : e.bossPhase == 2 ? 1.9f : e.bossPhase == 3 ? 1.5f : 1.1f;
        e.fireTimer = base * (0.85f + 0.3f * w.fxRng.nextFloat());
    }

    /** The next act: read the hull, never repeat the last, and weight what the phase has. */
    private static void choose(Enemy e, World w, Ship hull, float dist, float ux, float uy) {
        int ph = e.bossPhase;
        if (ph >= 2 && e.cloudCd <= 0f && !w.ash.active && e.lastAct != A_CLOUD) {            // the ash cloud: a long tell, then the smoke fills the hall
            e.vState = CLOUD;
            e.vT = 1.3f;
            e.lastAct = A_CLOUD;
            return;
        }
        float hullSpeed = (float) Math.hypot(hull.vx, hull.vy);
        float[] score = new float[10];
        score[A_CHARGE] = 1.0f + (dist < 330f ? 0.8f : 0f) + (dist > 750f ? 0.4f : 0f);
        score[A_FAN] = 1.0f + (dist > 450f ? 0.8f : 0f);
        score[A_BURST] = 0.7f + (dist < 260f ? 1.6f : 0f);
        score[A_PROWL] = 0f;
        score[A_RAIN] = ph >= 2 ? 0.9f + (hullSpeed < 50f ? 1.0f : 0f) : 0f;
        score[A_SPIT] = ph >= 2 ? 0.9f + (dist > 520f ? 0.7f : 0f) : 0f;
        score[A_EYE] = ph >= 3 ? 0.9f + (dist > 500f ? 0.4f : 0f) : 0f;
        score[A_PILLARS] = ph >= 3 ? 0.9f + (hullSpeed < 60f ? 1.0f : 0f) : 0f;
        score[A_IMMOLATE] = ph >= 4 && e.immolateCd <= 0f ? 1.6f : 0f;
        score[e.lastAct] = 0f;                                                                // never the same act twice
        float total = 0f;
        for (float s : score) total += s;
        float roll = w.fxRng.nextFloat() * total;
        int pick = A_FAN;
        for (int i = 0; i < score.length; i++) {
            roll -= score[i];
            if (roll <= 0f && score[i] > 0f) { pick = i; break; }
        }
        e.lastAct = pick;
        e.vN2++;
        if (ph >= 2 && e.vN2 % 5 == 4) summon(e, w);
        switch (pick) {
            case A_CHARGE:
                e.vState = CHARGE_TELL;
                e.vT = 0.75f;
                e.vN = ph >= 4 ? 2 : 1;
                pose(e, WINDUP, 0.75f);
                break;
            case A_FAN:
                e.vState = FAN;
                e.vT = 0.35f;
                e.vN = ph >= 3 ? 3 : 2;
                pose(e, WINDUP, 0.35f);
                break;
            case A_BURST:
                e.vState = BURST;
                e.vT = 0.5f;
                e.vN = ph >= 3 ? 2 : 1;
                e.vA = 0f;
                pose(e, WINDUP, 0.5f);
                break;
            case A_RAIN:
                e.vState = RAIN;
                e.vT = 0.25f;
                e.vN = 6;
                e.vA = 0f;
                break;
            case A_SPIT:
                e.vState = SPIT;
                e.vT = 0.5f;
                e.vN = ph >= 4 ? 3 : 2;
                pose(e, WINDUP, 0.5f);
                break;
            case A_PILLARS:
                e.vState = PILLARS;
                e.vT = 0.5f;
                e.vN = ph >= 4 ? 3 : 2;
                e.vA = 0f;
                pose(e, WINDUP, 0.5f);
                break;
            case A_IMMOLATE:
                e.vState = IMMOLATE;
                e.vT = 2.4f;
                e.immolateCd = 22f;
                break;
            default:
                e.vState = EYE_TELL;
                e.vT = ph >= 4 ? 1.2f : 1.4f;
                e.vA = e.facing - 0.9f;
                e.vB = 1f;
                break;
        }
    }

    /** The court: a pair of Vitala elites round the hearth, his to command (cap four at a time). */
    private static void summon(Enemy e, World w) {
        int alive = 0;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy o = w.enemies.get(i);
            if (!o.dead && o.commander == e) alive++;
        }
        if (alive >= 4) return;
        w.summonElites(e.x, e.y, e);
    }
}

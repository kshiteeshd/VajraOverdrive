package vajra.enemy;

import vajra.ship.Ship;
import vajra.weapon.WeaponDef;
import vajra.world.Effect;
import vajra.world.World;

/** BHAIRAVA, HERALD OF CINDERS (docs/VITALA_ENEMIES.md): Hara at the cremation ground, the Lord of Burning Gold. His signature, from the original
 *  bestiary, is the ASH: burning ground you cannot cross, and a safe arena that SHRINKS over the fight (here the ring of ash closes in as his hull
 *  falls). Three phases by hull, each adding to the last:
 *  <ol>
 *   <li><b>Stoking</b> (above two thirds): the ASH CHARGE - a tell, then a run straight through the hull's place that lays a trail of ash (patches
 *       that stand seven seconds) - and the EMBER FAN, five bolts, twice;</li>
 *   <li><b>Kindling</b> (to one third): adds the CINDER RAIN - ash dropped in a closing ring round the hull - and calls his court (a pair of Vitala
 *       elites); the Altar's rim vents open (World);</li>
 *   <li><b>The Flood</b> (below): adds the THIRD EYE, a beam that sweeps the hall after a long tell; the ash ring closes faster and the Hataki
 *       floods the hall and carries every hull east (World.flood).</li>
 *  </ol>
 *  Rank rule kept: his retinue obeys him (they are Vitala units with him as commander) and rout when he falls. */
public final class BhairavaBrain {
    private BhairavaBrain() {}

    public static final float SAFE_MAX = 800f, SAFE_MIN = 340f;
    private static final WeaponDef FAN = WeaponDef.mk("bhairava ember").count(5).spreadDeg(48f).speed(330).damage(11).bulletSize(7).life(3.4f)
            .colour(1f, 0.45f, 0.12f);

    /** The radius of the safe arena at a hull fraction: it closes as he falls. */
    public static float safeRadius(float hpFrac) {
        float f = Math.max(0f, Math.min(1f, hpFrac));
        return SAFE_MIN + (SAFE_MAX - SAFE_MIN) * (float) Math.pow(f, 0.8);
    }

    public static void think(Enemy e, World w, float dt, float t) {
        Ship hull = w.nearestHull(e.x, e.y);
        float dx = hull.x - e.x, dy = hull.y - e.y;
        float dist = Math.max(1f, (float) Math.hypot(dx, dy));
        float ux = dx / dist, uy = dy / dist;
        e.facing = (float) Math.atan2(dy, dx);
        float frac = e.hp / e.maxHp;
        e.bossPhase = frac > 0.66f ? 1 : frac > 0.33f ? 2 : 3;
        e.clockT += dt;
        // the ash ring: outside it the floor burns
        w.hazards.ring = true;
        w.hazards.ringX = w.pitX;
        w.hazards.ringY = w.pitY;
        w.hazards.ringR = safeRadius(frac) - (e.bossPhase == 3 ? 40f * (float) Math.abs(Math.sin(e.clockT)) : 0f);

        float gx = 0f, gy = 0f, cruise = 150f;
        switch (e.vState) {
            case 0: {                                          // hover over the hearth, circling, until the next act
                float a = e.clockT * 0.35f;
                float tx = w.pitX + (float) Math.cos(a) * 190f, ty = w.pitY + (float) Math.sin(a) * 150f;
                gx = (tx - e.x) * 1.4f;
                gy = (ty - e.y) * 1.4f;
                if (e.fireTimer <= 0f && dist < 1100f) choose(e, w, hull, ux, uy);
                break;
            }
            case 1:                                            // ash charge: the tell
                gx = ux * 20f;
                gy = uy * 20f;
                e.vT -= dt;
                if (e.vT <= 0f) { e.vState = 2; e.vT = 0.75f; e.vX = ux * 700f; e.vY = uy * 700f; e.vT2 = 0f; }
                break;
            case 2:                                            // the charge, ash behind
                gx = e.vX;
                gy = e.vY;
                e.vT -= dt;
                e.vT2 -= dt;
                if (e.vT2 <= 0f) { e.vT2 = 0.08f; w.hazards.patch(e.x, e.y, 36f, 7f); }
                if (e.vT <= 0f) rest(e);
                break;
            case 3:                                            // ember fan: two volleys
                e.vT -= dt;
                if (e.vT <= 0f) {
                    VitalaBrain.fire(w, e, FAN, e.facing);
                    if (--e.vN > 0) e.vT = 0.55f; else rest(e);
                }
                break;
            case 4:                                            // cinder rain: ash in a closing ring round the hull
                e.vT -= dt;
                if (e.vT <= 0f) {
                    for (int i = 0; i < 6; i++) {
                        float a = i * 1.0471976f + e.vA, r = e.vN * 55f;
                        w.hazards.patch(hull.x + (float) Math.cos(a) * r, hull.y + (float) Math.sin(a) * r, 34f, 6f);
                    }
                    e.vA += 0.5f;
                    e.vT = 0.4f;
                    if (--e.vN <= 0) rest(e);
                }
                break;
            case 5:                                            // third eye: a long tell, then the sweep
                e.vT -= dt;
                w.addEffect(Effect.flash(e.x, e.y, e.x + (float) Math.cos(e.vA) * 900f, e.y + (float) Math.sin(e.vA) * 900f, 1f, 0.9f, 0.5f));
                if (e.vT <= 0f) { e.vState = 6; e.vT = 1.5f; e.vT2 = 0f; }
                break;
            default:                                           // the sweep
                e.vT -= dt;
                e.vT2 -= dt;
                e.vA += 0.9f * dt;
                if (e.vT2 <= 0f) {
                    e.vT2 = 0.1f;
                    w.laser(e.x, e.y, e.vA, 900f, 1f, 0.8f, 0.3f, 9f);
                }
                if (e.vT <= 0f) rest(e);
                break;
        }
        float k = Math.min(1f, 4f * dt);
        e.vx += (Math.max(-cruise * 5f, Math.min(cruise * 5f, gx)) - e.vx) * k;
        e.vy += (Math.max(-cruise * 5f, Math.min(cruise * 5f, gy)) - e.vy) * k;
    }

    private static void rest(Enemy e) {
        e.vState = 0;
        e.fireTimer = e.bossPhase == 1 ? 2.2f : e.bossPhase == 2 ? 1.6f : 1.2f;
    }

    /** The next act, from the phase: stoking charges and fans; kindling adds the rain and the court; the flood adds the eye. */
    private static void choose(Enemy e, World w, Ship hull, float ux, float uy) {
        int n = e.vN2++;
        int ph = e.bossPhase;
        int pick = ph == 1 ? n % 2 : ph == 2 ? n % 3 : n % 4;       // 0 charge, 1 fan, 2 rain, 3 eye
        if (ph >= 2 && n % 5 == 4) summon(e, w);
        switch (pick) {
            case 0:
                e.vState = 1;
                e.vT = 0.7f;
                w.addEffect(Effect.flash(e.x, e.y, hull.x, hull.y, 1f, 0.5f, 0.15f));
                break;
            case 1:
                e.vState = 3;
                e.vT = 0.3f;
                e.vN = 2;
                break;
            case 2:
                e.vState = 4;
                e.vT = 0.2f;
                e.vN = 6;
                e.vA = 0f;
                break;
            default:
                e.vState = 5;
                e.vT = 1.4f;
                e.vA = e.facing - 0.9f;
                break;
        }
    }

    /** The court: a pair of Vitala elites round the hearth, his to command (cap two courts at a time). */
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

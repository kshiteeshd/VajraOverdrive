package vajra.world;

import vajra.enemy.Engage;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;
import vajra.weapon.Bullet;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Vitala's units read the pilot (the threat model: tells are aimed ahead of the hull, a still hull is pinned, a running one is flanked) and only a few attack at once (the engage budget). */
class VitalaThreatTest {
    private static final ShipControls IDLE = new ShipControls() {
        public float thrustX() { return 0; }
        public float thrustY() { return 0; }
        public float rotate() { return 0; }
        public boolean fire() { return false; }
        public boolean boost() { return false; }
        public boolean shield() { return false; }
        public boolean mouseAim() { return false; }
        public float aimX() { return 0; }
        public float aimY() { return 0; }
    };

    private static World arena() {
        World w = World.vitala(0);
        w.enemies.clear();
        w.ship.invuln = 1e6f;
        float[] at = spot(w);
        w.ship.x = at[0];
        w.ship.y = at[1];
        w.ship.vx = w.ship.vy = 0f;
        w.settleBeats();
        w.cues.clear();
        return w;
    }

    /** A stretch of the river with open, clear air for 600 px ahead of it. */
    private static float[] spot(World w) {
        for (float y = Hataki.START[2] + 700f; y < Hataki.END[2] - 800f; y += 60f) {
            float x = Hataki.centre(y);
            boolean ok = true;
            for (float d = 0f; d <= 640f && ok; d += 40f) ok = w.openFor(x, y + d, 18f) && w.openFor(x + 90f, y + d, 18f) && w.openFor(x - 90f, y + d, 18f);
            if (ok) return new float[] {x, y};
        }
        throw new AssertionError("no clear stretch of river");
    }

    private static Enemy place(World w, Enemy.Kind k, float dx, float dy) {
        Enemy e = new Enemy(k, w.ship.x + dx, w.ship.y + dy);
        e.vitala = true;
        e.active = true;
        e.wakeRange = 3000f;
        e.hp = e.maxHp = 1e6f;
        w.enemies.add(e);
        return e;
    }

    @Test
    void aimLeadsAMovingHullAndIsPlainForAStillOne() {
        World w = arena();
        float direct = (float) Math.atan2(w.ship.y + 600f - w.ship.y, 0f);
        w.ship.vx = 0f;
        assertEquals((float) Math.atan2(w.ship.y - (w.ship.y + 600f), 0f - 0f), Engage.aim(w.ship, w.ship.x, w.ship.y + 600f, 780f), 1e-3f, "a still hull is aimed at plainly");
        w.ship.vx = 320f;
        float a = Engage.aim(w.ship, w.ship.x, w.ship.y + 600f, 780f);
        assertTrue(Math.cos(a) > 0.1f, "a hull flying east is aimed AHEAD of, to the east: " + Math.toDegrees(a));
        assertTrue(Engage.still(arena().ship) && !Engage.still(w.ship));
    }

    @Test
    void aSniperFiresAheadOfAMovingHullAndWideAfterAHit() {
        World w = arena();
        Enemy k = place(w, Enemy.Kind.LANCE, 0f, 560f);
        k.sentry = true;
        float[] first = null;
        for (int i = 0; i < 60 * 12 && first == null; i++) {
            w.ship.vx = 300f;
            w.ship.x = Math.min(w.ship.x, k.x + 60f);
            w.update(1 / 60f, IDLE);
            for (Bullet b : w.bullets) if (b.hostile && Math.hypot(b.vx, b.vy) > 700f) { first = new float[] {b.vx, b.vy}; break; }
        }
        assertNotNull(first, "the sniper fired");
        assertTrue(first[0] > 20f, "its needles are thrown east, ahead of a hull flying east: vx=" + first[0]);
        k.hurt(1f, w);
        assertTrue(k.spoilT > 1f, "a hit spoils its next aim");
    }

    @Test
    void aSmokeGoesToSmokeAtOnceWhenStruckWhileVisible() {
        World w = arena();
        Enemy d = place(w, Enemy.Kind.AURIS, 300f, 300f);
        w.update(1 / 60f, IDLE);
        assertFalse(d.smoked);
        d.hitFlash = 0.12f;
        for (int i = 0; i < 4; i++) w.update(1 / 60f, IDLE);
        assertTrue(d.smoked || d.vState == 1, "struck, it fades");
    }

    @Test
    void aCinderLaysGroundBetweenItselfAndAHullThatComesClose() {
        World w = arena();
        place(w, Enemy.Kind.MAYAVI, 0f, 200f);
        for (int i = 0; i < 20; i++) w.update(1 / 60f, IDLE);
        assertFalse(w.hazards.patches.isEmpty(), "it seeded the ground between them");
    }

    @Test
    void aStillHullIsRingedInByTheCinder() {
        World w = arena();
        Enemy j = place(w, Enemy.Kind.MAYAVI, 0f, 480f);
        j.sentry = true;
        int near = 0;
        for (int i = 0; i < 60 * 12; i++) {
            w.update(1 / 60f, IDLE);
            for (Hazards.Patch p : w.hazards.patches) if (Math.hypot(p.x - w.ship.x, p.y - w.ship.y) < 80f) near++;
        }
        assertTrue(near > 0, "ground was laid right under a hull that stood still");
    }

    @Test
    void onlyAFewAreCommittedToAnAttackAtOnce() {
        World w = arena();
        for (int i = 0; i < 14; i++) {
            float a = i * 6.2831853f / 14f;
            place(w, i % 2 == 0 ? Enemy.Kind.VAKRA : Enemy.Kind.LANCE, (float) Math.cos(a) * 520f, (float) Math.sin(a) * 520f);
        }
        float[] home = {w.ship.x, w.ship.y};
        int most = 0, anyCommit = 0;
        for (int i = 0; i < 60 * 14; i++) {
            w.ship.x = home[0];
            w.ship.y = home[1];
            w.update(1 / 60f, IDLE);
            int n = 0;
            for (Enemy e : w.enemies) if (!e.dead && e.vState != 0 && e.commitT > -1f) n++;
            most = Math.max(most, n);
            if (n > 0) anyCommit++;
        }
        assertTrue(anyCommit > 30, "they do attack");
        assertTrue(most <= Engage.SLOTS + 2, "no more than a few are in their acts at once: " + most);
    }

    @Test
    void theBudgetGivesPlacesOutAndTakesThemBack() {
        Engage b = new Engage();
        Enemy[] es = new Enemy[8];
        int got = 0;
        for (int i = 0; i < es.length; i++) { es[i] = new Enemy(Enemy.Kind.VAKRA, 0, 0); if (b.request(es[i])) got++; }
        assertEquals(Engage.SLOTS, got, "four places for the rank and file");
        Enemy lead = new Enemy(Enemy.Kind.VAKRA, 0, 0);
        lead.rank = 1;
        assertTrue(b.request(lead), "a leader may take one more");
        for (Enemy e : es) e.commitT = 0f;
        assertTrue(b.taken() <= 1, "unused places come free");
        assertTrue(b.request(es[7]));
    }
}

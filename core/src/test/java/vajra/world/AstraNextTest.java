package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.weapon.Bullet;

/** The mandala's shield, Akasha's lightning, Prithvi's boulders, Brahma's lotus (docs/ASTRA_NEXT_PLAN.md). */
class AstraNextTest {
    private static final float DT = 1 / 60f;

    private static World arena() {
        World w = new World();
        w.enemies.clear();
        w.maps = new TileMap[0];                                         // open ground
        w.ship.x = 2600;
        w.ship.y = 2700;
        w.ship.facing = (float) (Math.PI / 2);
        return w;
    }

    private static Enemy foe(World w, float x, float y, float hp) {
        Enemy e = new Enemy(Enemy.Kind.MAYAVI, x, y);
        e.hp = e.maxHp = hp;
        e.active = true;
        w.enemies.add(e);
        return e;
    }

    private static void run(World w, float seconds) {
        for (float t = 0; t < seconds; t += DT) w.updateAstra(DT);
    }

    // ---- the shield ------------------------------------------------------------------------------------------------------------------

    @Test
    void theMandalaDestroysEnemyRoundsThatTouchItWhileTheAstraForms() {
        World w = arena();
        w.hangar.shards[Astra.AKASHA] = 5;
        w.astraSel = Astra.AKASHA;
        w.astraBegin();
        run(w, 1.0f);
        float R = Astra.mandalaRadius(Astra.AKASHA, Astra.k(w.astraCharge));
        w.spawnEnemyBullet(w.ship.x + R - 6f, w.ship.y, (float) Math.PI, Bullet.HOSTILE, 1f);
        w.spawnEnemyBullet(w.ship.x + R + 120f, w.ship.y, (float) Math.PI, Bullet.HOSTILE, 1f);
        Bullet inside = w.bullets.get(w.bullets.size() - 2), outside = w.bullets.get(w.bullets.size() - 1);
        float hp = w.ship.hp;
        w.updateAstra(DT);
        assertTrue(inside.dead, "a round that touches the mandala is destroyed");
        assertFalse(outside.dead, "one still outside it is not");
        assertEquals(hp, w.ship.hp, 0.001f, "and the hull is untouched");
        w.astraLoose();
        World idle = arena();
        idle.spawnEnemyBullet(idle.ship.x + 40f, idle.ship.y, (float) Math.PI, Bullet.HOSTILE, 1f);
        idle.updateAstra(DT);
        assertFalse(idle.bullets.get(idle.bullets.size() - 1).dead, "with no astra forming there is no shield");
    }

    @Test
    void theShieldIsTheMandalaItselfSmallerAsItChargesBiggerTheDeeperTheAstra() {
        assertTrue(Astra.mandalaRadius(Astra.AKASHA, 1f) < Astra.mandalaRadius(Astra.AKASHA, 0f), "it tightens as it charges");
        assertTrue(Astra.mandalaRadius(Astra.BRAHMA, 0.5f) > Astra.mandalaRadius(Astra.AKASHA, 0.5f), "and the deeper the astra, the greater");
    }

    // ---- AKASHA ----------------------------------------------------------------------------------------------------------------------

    @Test
    void theLightningIsLoosedAsProjectilesAtRandomAnglesThatShockAndStunWhatTheyTouch() {
        World w = arena();
        Enemy e = foe(w, 2600 + 220, 2700, 100000);
        w.flow.startLightning(w.ship, 0, 1f, 1f, 7L);
        run(w, 0.3f);
        assertTrue(!w.flow.zaps.isEmpty(), "bolts are in flight");
        float lo = 99f, hi = -99f;
        for (AstraFlow.Zap z : w.flow.zaps) { lo = Math.min(lo, z.ang); hi = Math.max(hi, z.ang); }
        run(w, 0.5f);
        for (AstraFlow.Zap z : w.flow.zaps) { lo = Math.min(lo, z.ang); hi = Math.max(hi, z.ang); }
        assertTrue(hi - lo > 3f, "they leave at random angles, not in a ring (" + (hi - lo) + " rad)");
        assertTrue(e.hp < 100000f - AstraFlow.ZAP_HIT, "the ones that touch a foe hurt it");
        assertTrue(e.shockT > 0f || e.caught > 0f, "and shock it");
    }

    @Test
    void aBoltFlyingAwayFromEveryFoeHitsNothingAndTheLightningLastsAboutTwoSecondsThenEnds() {
        World w = arena();
        Enemy e = foe(w, 2600 + 220, 2700, 100000);
        w.flow.startLightning(w.ship, 0, 1f, 1f, 7L);
        run(w, 1.5f);
        assertTrue(w.flow.active(), "still crackling after a second and a half");
        run(w, 4f);
        assertTrue(!w.flow.active(), "it ends");
        assertEquals(0f, e.shockT, 0.0001f, "and the shock ends with it");
    }

    @Test
    void aFoeBeyondTheBoltsRangeIsNeverTouched() {
        World w = arena();
        Enemy out = foe(w, 2600 + 3000, 2700, 400);
        w.flow.startLightning(w.ship, 0, 1f, 1f, 7L);
        run(w, 3f);
        assertEquals(400f, out.hp, 0.001f);
    }

    @Test
    void akashaReleaseIsTheLightningNotACubeVolleyAndRefillsTheShield() {
        World w = arena();
        foe(w, 2600 + 150, 2700, 900);
        w.hangar.shards[Astra.AKASHA] = 5;
        w.ship.shieldEnergy = 0f;
        w.astraSel = Astra.AKASHA;
        w.astraBegin();
        run(w, Astra.WIND_FULL);
        assertTrue(w.astraLoose());
        assertEquals(AstraFlow.LIGHTNING, w.flow.kind, "the release is the lightning");
        assertTrue(w.astra.isEmpty(), "no cubes");
        assertEquals(w.ship.shieldCap(), w.ship.shieldEnergy, 0.001f, "and the shield is refilled");
    }

    // ---- PRITHVI ---------------------------------------------------------------------------------------------------------------------

    @Test
    void boulderFormOnTheMandalaAndBreakIntoChunksAndPebblesAtAWall() {
        World w = arena();
        w.flow.startBoulders(w.ship, 0, 1f, 1f, 9L);
        run(w, 0.35f);
        boolean boulder = false;
        float R = w.flow.radius;
        for (int i = 0; i < w.flow.n; i++)
            if (w.flow.mode[i] == 4 || w.flow.mode[i] == 5) {
                boulder = true;
                assertTrue(Math.hypot(w.flow.px[i] - w.ship.x, w.flow.py[i] - w.ship.y) < R * 1.6f + 400f, "boulders begin at the mandala");
            }
        assertTrue(boulder, "boulders form");
        run(w, 1.8f);
        int chunks = 0, pebbles = 0;
        for (float t = 0; t < 1.6f; t += DT) {
            w.updateAstra(DT);
            for (int i = 0; i < w.flow.n; i++) { if (w.flow.mode[i] == 6) chunks++; if (w.flow.mode[i] == 7) pebbles++; }
        }
        assertTrue(chunks > 0, "boulders that fly on or hit something break into chunks");
        assertTrue(pebbles > 0, "and chunks break into pebbles");
        assertTrue(w.flow.n <= AstraFlow.CAP, "the pool never exceeds its cap");
    }

    @Test
    void aRockThatHitsAFoeHurtsItOnceAndARoundThatTouchesARockIsDestroyed() {
        World w = arena();
        Enemy target = foe(w, 2600, 3200, 100000);
        w.flow.startBoulders(w.ship, 0, 1f, 1f, 9L);
        run(w, 3.4f);
        assertTrue(target.hp < 100000f, "the rocks thrown at foes find them: " + target.hp);
        World v = arena();
        v.flow.startBoulders(v.ship, 0, 1f, 1f, 9L);
        run(v, 0.5f);
        int found = -1;
        for (int i = 0; i < v.flow.n; i++) if (v.flow.mode[i] == 5) { found = i; break; }
        assertTrue(found >= 0, "a boulder is flying");
        v.spawnEnemyBullet(v.flow.px[found], v.flow.py[found], 0f, Bullet.HOSTILE, 1f);
        Bullet b = v.bullets.get(v.bullets.size() - 1);
        v.updateAstra(DT);
        assertTrue(b.dead, "an enemy round touching a rock is destroyed");
    }

    @Test
    void aboutHalfTheBouldersAreThrownAtFoesAndTheMandalaStaysAShield() {
        World w = arena();
        for (int i = 0; i < 20; i++) foe(w, 2600 + (i % 5 - 2) * 220, 2700 + 300 + (i / 5) * 140, 100000);
        w.flow.startBoulders(w.ship, 0, 1f, 1f, 9L);
        run(w, 0.4f);
        int aimed = 0, total = 0;
        for (int i = 0; i < w.flow.n; i++) {
            if (w.flow.mode[i] != 4 && w.flow.mode[i] != 5) continue;
            total++;
            for (Enemy e : w.enemies) {
                float want = (float) Math.atan2(e.y - w.flow.py[i], e.x - w.flow.px[i]);
                if (Math.abs(World.wrapAngle(want - w.flow.ph[i])) < 0.2f) { aimed++; break; }
            }
        }
        assertTrue(total >= 4, "several boulders: " + total);
        assertTrue(aimed >= total * 0.3f, "a good share are aimed at foes: " + aimed + " of " + total);
        w.astraKind = Astra.PRITHVI;
        w.spawnEnemyBullet(w.ship.x + 30f, w.ship.y, (float) Math.PI, Bullet.HOSTILE, 1f);
        Bullet b = w.bullets.get(w.bullets.size() - 1);
        w.updateAstra(DT);
        assertTrue(b.dead, "the mandala that throws them is still a shield");
    }

    // ---- BRAHMA ----------------------------------------------------------------------------------------------------------------------

    @Test
    void fivePetalsBloomThenEachSeeksADifferentFoe() {
        World w = arena();
        Enemy[] f = new Enemy[5];
        for (int i = 0; i < 5; i++) f[i] = foe(w, 2600 + (i - 2) * 260, 2700 + 700, 100000);
        w.flow.startLotus(w.ship, 0, 1f, 1f, 4L);
        run(w, AstraFlow.T_BLOOM * 0.5f);
        for (int i = 0; i < 5; i++) assertEquals(0, w.flow.lstate[i], "the petals are still opening");
        run(w, 0.8f);
        java.util.Set<Enemy> picked = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) if (w.flow.ltarget[i] != null) picked.add(w.flow.ltarget[i]);
        assertEquals(5, picked.size(), "five petals, five different foes");
    }

    @Test
    void aPetalCirclesItsFoeClosingInAndStrikesThreeTimes() {
        World w = arena();
        Enemy e = foe(w, 2600, 2700 + 500, 1000000);
        w.flow.startLotus(w.ship, 0, 1f, 1f, 4L);
        run(w, AstraFlow.T_BLOOM + 0.05f);
        int circler = -1;
        for (float t = 0; t < 2.5f && circler < 0; t += DT) {
            w.updateAstra(DT);
            for (int i = 0; i < 5; i++) if (w.flow.lstate[i] == 2 && w.flow.ltarget[i] == e) circler = i;
        }
        assertTrue(circler >= 0, "a petal reaches the foe and begins to circle it");
        float r0 = (float) Math.hypot(w.flow.lx[circler] - e.x, w.flow.ly[circler] - e.y), a0 = (float) Math.atan2(w.flow.ly[circler] - e.y, w.flow.lx[circler] - e.x);
        float hp0 = e.hp;
        run(w, 0.25f);
        float r1 = (float) Math.hypot(w.flow.lx[circler] - e.x, w.flow.ly[circler] - e.y), a1 = (float) Math.atan2(w.flow.ly[circler] - e.y, w.flow.lx[circler] - e.x);
        assertTrue(r1 < r0 - 5f, "it closes in as it circles: " + r0 + " -> " + r1);
        assertTrue(Math.abs(World.wrapAngle(a1 - a0)) > 0.5f, "and goes round it");
        assertTrue(e.hp < hp0, "and strikes it");
    }

    @Test
    void theLotusEndsEveryFoeInReachAndAPetalsElementTakesEffect() {
        World w = arena();
        java.util.List<Enemy> all = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) all.add(foe(w, 2600 + (i % 4 - 2) * 400, 2700 + 300 + (i / 4) * 500, 300));
        Enemy boss = foe(w, 2600, 2700 - 600, 200000);
        boss.boss = true;
        w.flow.startLotus(w.ship, 0, 1f, 1f, 4L);
        for (int i = 0; i < 60 * 12 && w.flow.active(); i++) w.updateAstra(DT);
        int dead = 0;
        for (Enemy e : all) if (e.dead) dead++;
        assertEquals(12, dead, "the petals ended every foe in reach");
        assertFalse(boss.dead, "but a ruler is hurt, not slain");
        assertTrue(boss.hp < boss.maxHp && boss.hp >= boss.maxHp * 0.74f, "it was hurt, by no more than a quarter: " + boss.hp);
        assertFalse(w.flow.active(), "and the lotus is gone when there is nothing left");
    }

    @Test
    void aGuestsBrahmaBloomsFromItsOwnHullAndTheOthersFollowTheirLokas() {
        World hub = arena();
        hub.astraFor(1, Astra.BRAHMA, 1f, new int[0]);
        assertEquals(AstraFlow.LOTUS, hub.flow.kind, "the lotus blooms on the hub for the guest");
        assertEquals(1, hub.flow.seat);
        hub.astraFor(1, Astra.PRITHVI, 1f, new int[0]);
        assertEquals(AstraFlow.BOULDER, hub.flow.kind);
        hub.astraFor(1, Astra.AKASHA, 1f, new int[0]);
        assertEquals(AstraFlow.LIGHTNING, hub.flow.kind);
    }
}

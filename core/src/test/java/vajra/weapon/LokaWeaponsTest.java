package vajra.weapon;

import vajra.enemy.Enemy;
import vajra.ship.ShipControls;
import vajra.world.Astra;
import vajra.world.World;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The twenty-one patterns of the lokas: each one's single idea, and the rule that only a hull's 2nd and 3rd variants forge astras. */
class LokaWeaponsTest {
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
        World w = new World();
        w.enemies.clear();
        w.bullets.clear();
        w.ship.facing = 0f;
        return w;
    }

    private static void run(World w, float s) {
        for (int i = 0; i < (int) (s / 0.016f); i++) w.update(0.016f, IDLE);
    }

    private static Enemy foeAt(World w, float dx, float dy) {
        Enemy e = new Enemy(Enemy.Kind.VAKRA, w.ship.x + dx, w.ship.y + dy);
        e.hp = e.maxHp = 400f;
        e.active = false;
        w.enemies.add(e);
        return e;
    }

    private static Bullet fire(World w, WeaponDef d) {
        w.spawnBullet(w.ship.x + 30, w.ship.y, 0f, d, 1f, 1f);
        return w.bullets.get(w.bullets.size() - 1);
    }

    @Test
    void thereAreThirtySixAndThreeToEachLoka() {
        assertEquals(36, WeaponDef.ALL.length);
        int[] per = new int[7];
        for (int i = WeaponDef.STANDARD; i < WeaponDef.ALL.length; i++) per[WeaponDef.ALL[i].minLoka]++;
        for (int l = 0; l < 7; l++) assertEquals(3, per[l], "loka " + l + " forges three of its own");
        java.util.HashSet<String> names = new java.util.HashSet<>();
        for (WeaponDef d : WeaponDef.ALL) assertTrue(names.add(d.name), "no two patterns share a name: " + d.name);
    }

    @Test
    void everyPatternHasItsCardAndItsBulletArt() {
        java.io.File assets = new java.io.File("../assets");
        for (WeaponDef d : WeaponDef.ALL) {
            if (!d.beam) assertTrue(new java.io.File(assets, vajra.screen.BulletImages.artPath(d.name)).exists(), d.name + " bullet");
            assertTrue(new java.io.File(assets, "weapon_display_images/" + vajra.screen.WeaponCards.fileKey(d.name) + ".png").exists(), d.name + " card");
        }
    }

    @Test
    void mirrorShotFiresAWeakerCopyOutOfTheTail() {
        World w = arena();
        w.ship.weapon.def = WeaponDef.MIRROR;
        w.ship.weapon.held(0.016f, 0f, w.ship, w);
        assertEquals(2, w.bullets.size());
        Bullet f = w.bullets.get(0), r = w.bullets.get(1);
        assertTrue(f.vx > 0 && r.vx < 0, "one forward, one back");
        assertEquals(f.damage * WeaponDef.MIRROR.rearDamage, r.damage, 0.01f);
    }

    @Test
    void theScytheTurnsAndComesHome() {
        World w = arena();
        Bullet b = fire(w, WeaponDef.SCYTHE);
        run(w, 0.5f);
        assertTrue(b.x > w.ship.x + 100, "it goes out");
        run(w, 1.6f);
        assertTrue(w.bullets.isEmpty() || Math.hypot(w.bullets.get(0).x - w.ship.x, w.bullets.get(0).y - w.ship.y) < 120, "and returns to the hull that threw it");
    }

    @Test
    void theWeaveSwingsAcrossItsLine() {
        World w = arena();
        Bullet b = fire(w, WeaponDef.WEAVE);
        float lo = 0, hi = 0;
        for (int i = 0; i < 60; i++) {
            b.update(w, 0.016f);
            lo = Math.min(lo, b.y - w.ship.y);
            hi = Math.max(hi, b.y - w.ship.y);
        }
        assertTrue(hi - lo > 40, "it wanders both sides of the line: " + (hi - lo));
    }

    @Test
    void emberAndVenomSetFoesAlightAndTheyKeepBurning() {
        for (WeaponDef d : new WeaponDef[] {WeaponDef.EMBER, WeaponDef.FANG}) {
            World w = arena();
            Enemy e = foeAt(w, 200, 0);
            w.enemies.get(0).hp = 400f;
            fire(w, d);
            run(w, 0.5f);
            assertTrue(e.dotT > 0f, d.name + " leaves the foe burning");
            float hp = e.hp;
            run(w, 1.0f);
            assertTrue(e.hp < hp - 2f, d.name + " keeps hurting after the round is gone: " + hp + " -> " + e.hp);
        }
    }

    @Test
    void tideSlowsAndQuakeFreezesAFoe() {
        World w = arena();
        Enemy e = foeAt(w, 150, 0);
        fire(w, WeaponDef.TIDE);
        run(w, 0.4f);
        assertTrue(e.slowT > 0f && e.slowK < 0.7f, "slowed");
        World q = arena();
        Enemy f = foeAt(q, 150, 0);
        fire(q, WeaponDef.QUAKE);
        run(q, 0.4f);
        assertTrue(f.stunT > 0f, "frozen in place");
    }

    @Test
    void boonShotMendsTheHullOnAHit() {
        World w = arena();
        foeAt(w, 150, 0);
        w.ship.hp = w.ship.maxHp() - 10f;
        float hp = w.ship.hp;
        fire(w, WeaponDef.BOON);
        run(w, 0.4f);
        assertTrue(w.ship.hp > hp, "the hull is mended");
    }

    @Test
    void theRingIsTwelveRoundsAllAround() {
        World w = arena();
        w.ship.weapon.def = WeaponDef.RIPPLE;
        w.ship.weapon.held(0.016f, 0f, w.ship, w);
        assertEquals(12, w.bullets.size());
    }

    @Test
    void arcChainJumpsToTheNearestOthers() {
        World w = arena();
        Enemy a = foeAt(w, 150, 0), b = foeAt(w, 150, 150), c = foeAt(w, 150, 300), far = foeAt(w, -900, 0);
        fire(w, WeaponDef.ARC);
        run(w, 0.3f);
        assertTrue(a.hp < 400f && b.hp < 400f && c.hp < 400f, "three foes in a line are all struck");
        assertEquals(400f, far.hp, 0.01f, "a foe out of reach is not");
    }

    @Test
    void theMineSettlesThenGoesOffWhenAFoeComesClose() {
        World w = arena();
        Bullet m = fire(w, WeaponDef.MINE);
        run(w, 1.2f);
        assertEquals(0f, Math.hypot(m.vx, m.vy), 1f, "it has stopped");
        Enemy e = foeAt(w, 0, 0);
        e.x = m.x + 40;
        e.y = m.y;
        run(w, 0.1f);
        assertTrue(e.hp < 400f, "and it went off");
    }

    @Test
    void theCloudLeavesBurningGroundAndTheWellDrawsFoesIn() {
        World w = arena();
        Enemy e = foeAt(w, 200, 0);
        fire(w, WeaponDef.CLOUD);
        run(w, 1.0f);
        assertFalse(w.zones.isEmpty(), "ground is left burning");
        run(w, 1.5f);
        assertTrue(e.hp < 380f, "whoever stands in it burns");

        World g = arena();
        Enemy far = foeAt(g, 400, 150);
        Bullet well = fire(g, WeaponDef.WELL);
        float d0 = (float) Math.hypot(far.x - well.x, far.y - well.y);
        for (int i = 0; i < 30; i++) {
            well.update(g, 0.016f);
            far.vx = far.vy = 0;
        }
        float d1 = (float) Math.hypot(far.x - well.x, far.y - well.y);
        assertTrue(d1 < d0 - 5f, "the foe is drawn toward it: " + d0 + " -> " + d1);
    }

    @Test
    void cinderShardAndPrismBreakIntoPieces() {
        World w = arena();
        foeAt(w, 120, 0);
        fire(w, WeaponDef.SHARD);
        run(w, 0.2f);
        assertTrue(w.bullets.size() >= 4, "a hit scatters shards: " + w.bullets.size());
        World p = arena();
        fire(p, WeaponDef.PRISM);
        run(p, 0.3f);
        assertEquals(3, p.bullets.size(), "the prism splits to three after a moment");
        World c = arena();
        fire(c, WeaponDef.CINDER);
        run(c, 0.85f);
        assertTrue(c.bullets.size() >= 6, "the cinder bursts into embers at the end of its flight");
    }

    @Test
    void theExecutionerHitsTheWoundedThreeTimesAsHard() {
        World w = arena();
        Enemy healthy = foeAt(w, 120, 0), hurt = foeAt(w, 120, 400);
        hurt.hp = hurt.maxHp * 0.3f;
        Bullet b = fire(w, WeaponDef.EXECUTE);
        assertEquals(b.damage, b.damageTo(healthy), 0.001f);
        assertEquals(b.damage * 3f, b.damageTo(hurt), 0.001f);
    }

    @Test
    void lastLightSwellsAsItTravels() {
        World w = arena();
        Bullet b = fire(w, WeaponDef.LASTLIGHT);
        float r0 = b.radius;
        run(w, 1f);
        assertTrue(w.bullets.isEmpty() || w.bullets.get(0).radius > r0 + 20f);
    }

    @Test
    void aRicochetBouncesOffRock() {
        World w = arena();
        Bullet b = fire(w, WeaponDef.RICOCHET);
        assertEquals(3, b.bouncesLeft);
    }

    @Test
    void aTemperedRoundStillWearsItsPatternsArt() {
        for (int i = 0; i < WeaponDef.ALL.length; i++) assertSame(WeaponDef.ALL[i], WeaponDef.ALL[i].ranked(2).art, WeaponDef.ALL[i].name);
    }

    // ---- the astra rule ----
    @Test
    void onlyTheSecondAndThirdVariantCanForgeAnAstra() {
        World w = new World();
        w.hangar.shards[Astra.AGNI] = 5;
        w.astraSel = Astra.AGNI;
        assertFalse(w.hangar.canForge(), "the base variant cannot");
        assertEquals(0, Astra.releases(w.hangar, Astra.AGNI), "its shards pay for nothing");
        assertFalse(w.astraBegin(), "and F does nothing");
        w.hangar.owned[0][1] = true;
        assertTrue(w.hangar.canForge(), "the 2nd variant can");
        assertTrue(w.astraBegin());
        World v = new World();
        v.hangar.owned[0][1] = true;
        v.hangar.owned[0][2] = true;
        assertTrue(v.hangar.canForge(), "and so can the 3rd");
    }

    @Test
    void theRuleHoldsForEveryHull() {
        for (vajra.ship.ShipDef d : vajra.ship.ShipDef.ALL) {
            vajra.ship.Hangar h = new vajra.ship.Hangar();
            h.owned[d.id][0] = true;
            h.shipIndex = d.id;
            java.util.Arrays.fill(h.shards, 9);
            assertFalse(h.canForge(), d.name + " base variant");
            assertEquals(0, Astra.releases(h, Astra.BRAHMA), d.name + " base cannot loose even the Brahmastra");
            h.owned[d.id][1] = true;
            assertTrue(h.canForge(), d.name + " 2nd variant");
        }
    }
}

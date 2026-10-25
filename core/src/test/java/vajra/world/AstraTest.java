package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.Hangar;

/** THE ASTRA RITE: hold F to invoke, let go to loose, five shards of the astra's element a release, paid at the
 *  loosing. Five elemental astras with their own character, and the Brahmastra of all five. */
class AstraTest {
    private static final float DT = 1 / 60f;

    /** A world with the ship at (2600, 2700) facing north and `n` living foes 300 px ahead, in view. */
    private static World arena(int n) {
        World w = new World();
        w.enemies.clear();
        w.ship.x = 2600;
        w.ship.y = 2700;
        w.ship.facing = (float) (Math.PI / 2);
        for (int i = 0; i < n; i++) w.enemies.add(foe(2600 + (i - n / 2) * 40, 3000));
        return w;
    }

    private static Enemy foe(float x, float y) {
        Enemy e = new Enemy(Enemy.Kind.MAYAVI, x, y);
        e.hp = e.maxHp = 90;
        e.active = true;
        return e;
    }

    private static void fuel(World w, int element, int shards) {
        w.hangar.shards[element] = shards;
    }

    /** Hold F for `seconds`, in frames. */
    private static void hold(World w, float seconds) {
        for (float t = 0; t < seconds; t += DT) w.updateAstra(DT);
    }

    // --- the fuel ------------------------------------------------------------------------------

    @Test
    void fiveShardsOfAnElementPayOneRelease() {
        Hangar h = new Hangar();
        for (int i = 1; i <= 4; i++) assertFalse(h.addShard(Astra.AKASHA), "shard " + i + " does not complete a set");
        assertTrue(h.addShard(Astra.AKASHA), "the fifth completes one");
        assertEquals(1, Astra.releases(h, Astra.AKASHA));
        for (int i = 0; i < 5; i++) h.addShard(Astra.AKASHA);
        assertEquals(2, Astra.releases(h, Astra.AKASHA), "five more, another");
    }

    @Test
    void thereIsNothingToForgeTheFirstFiveAreFuelAlready() {
        Hangar h = new Hangar();
        h.shards[Astra.AGNI] = 5;
        assertTrue(Astra.canLoose(h, Astra.AGNI), "no forge step, no stored charge: the pile is the fuel");
    }

    @Test
    void onlyTheRightElementPaysForAnAstra() {
        Hangar h = new Hangar();
        h.shards[Astra.AGNI] = 9;
        assertFalse(Astra.canLoose(h, Astra.AKASHA), "Agni shards do not fuel the Akasha astra");
        assertTrue(Astra.canLoose(h, Astra.AGNI));
    }

    @Test
    void payingTakesExactlyFiveOrNothing() {
        Hangar h = new Hangar();
        h.shards[Astra.JALA] = 7;
        assertTrue(Astra.pay(h, Astra.JALA));
        assertEquals(2, h.shards[Astra.JALA]);
        assertFalse(Astra.pay(h, Astra.JALA), "two shards cannot pay");
        assertEquals(2, h.shards[Astra.JALA], "and a refusal takes nothing");
    }

    @Test
    void theBrahmastraNeedsFiveOfEveryElementAndTakesThemAll() {
        Hangar h = new Hangar();
        for (int t = 0; t < Astra.ELEMENTS; t++) h.shards[t] = 5;
        h.shards[Astra.PRITHVI] = 4;
        assertFalse(Astra.canLoose(h, Astra.BRAHMA), "one short of one element");
        h.shards[Astra.PRITHVI] = 5;
        assertTrue(Astra.canLoose(h, Astra.BRAHMA));
        assertTrue(Astra.pay(h, Astra.BRAHMA));
        for (int t = 0; t < Astra.ELEMENTS; t++) assertEquals(0, h.shards[t], "five of element " + t);
    }

    @Test
    void sellingShardsSpendsTheAstra() {
        Hangar h = new Hangar();
        h.shards[Astra.AKASHA] = 5;
        assertTrue(Astra.canLoose(h, Astra.AKASHA));
        h.sellShards();
        assertFalse(Astra.canLoose(h, Astra.AKASHA), "sold, so it cannot fly");
    }

    // --- the invocation ------------------------------------------------------------------------

    @Test
    void holdingFBeginsAnInvocationAndPaysNothingYet() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        assertTrue(w.astraBegin());
        assertTrue(w.winding(), "the cage is gathering");
        assertEquals(5, w.hangar.shards[Astra.AKASHA], "nothing is paid at the start");
        assertFalse(w.astraBegin(), "one invocation at a time");
    }

    @Test
    void withNothingToPayItRefusesToBegin() {
        World w = arena(3);
        assertFalse(w.astraBegin());
        assertFalse(w.winding());
    }

    @Test
    void theCageGrowsTheLongerYouHold() {
        World w = arena(8);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        int early = heldCubes(w);
        hold(w, Astra.WIND_FULL);
        int full = heldCubes(w);
        assertTrue(early < full, "the cage fills as you hold: " + early + " -> " + full);
        assertEquals(Astra.CUBES[Astra.AKASHA], full, "and is whole at full strength");
    }

    @Test
    void theHullGoesHeavyWhileInvoking() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        w.ship.update(DT, vajra.ship.ShipControls.NONE, w);
        assertTrue(w.ship.winding, "the ship knows it is invoking (thrust is cut)");
        w.astraCancel();
        w.ship.update(DT, vajra.ship.ShipControls.NONE, w);
        assertFalse(w.ship.winding);
    }

    @Test
    void lettingGoTooSoonFaltersAndCostsNothing() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, Astra.WIND_MIN * 0.5f);
        assertFalse(w.astraLoose(), "the mantra faltered");
        assertEquals(5, w.hangar.shards[Astra.AKASHA], "free");
        assertEquals(0, w.astra.size(), "the cage is gone");
        assertTrue(w.astraCd > 0 && w.astraCd <= Astra.FALTER_LOCKOUT, "a short lockout stops a stuttering key");
    }

    @Test
    void lettingGoAfterTheMinimumLoosesItAndPaysThen() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 7);
        w.astraBegin();
        hold(w, 1.0f);
        assertTrue(w.astraLoose());
        assertEquals(2, w.hangar.shards[Astra.AKASHA], "five shards, paid at the loosing");
        assertFalse(w.winding());
        assertEquals(Astra.COOLDOWN, w.astraCd, 1e-4f);
        for (AstraCube c : w.astra) assertTrue(c.armed, "every cube is away");
    }

    @Test
    void aHullThatDiesWhileInvokingLosesNothing() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, 0.8f);
        w.ship.hp = 0;
        w.updateAstra(DT);
        assertFalse(w.winding(), "the invocation is dropped");
        assertEquals(5, w.hangar.shards[Astra.AKASHA], "and the shards are still there");
        assertTrue(w.astra.isEmpty());
    }

    @Test
    void aPauseOrAMenuCancelsForFree() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, 0.8f);
        w.astraCancel();
        assertFalse(w.winding());
        assertEquals(5, w.hangar.shards[Astra.AKASHA]);
        assertTrue(w.astra.isEmpty());
    }

    @Test
    void aSecondInvocationWaitsOutTheCooldown() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 10);
        w.astraBegin();
        hold(w, 1.0f);
        w.astraLoose();
        assertFalse(w.astraBegin(), "still cooling");
        for (float t = 0; t < Astra.COOLDOWN + 0.1f; t += DT) w.updateAstra(DT);
        assertTrue(w.astraBegin(), "ready again");
    }

    // --- the aim ---------------------------------------------------------------------------------

    @Test
    void theLocksAreTheFoesNearestTheLineOfTheNose() {
        World w = arena(0);
        Enemy ahead = foe(2600, 3000);           // dead ahead, 300 px
        Enemy behind = foe(2600, 2600);          // 100 px, but behind the nose
        w.enemies.add(behind);
        w.enemies.add(ahead);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, 1.0f);
        assertTrue(w.astraLockCount >= 2);
        assertSame(ahead, w.astraLocks[0], "the foe in front of the nose is locked first");
    }

    @Test
    void foesOutOfViewAreNeverLocked() {
        World w = arena(0);
        w.enemies.add(foe(2600, 6000)); // 3300 px away
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, 1.0f);
        assertEquals(0, w.astraLockCount);
    }

    @Test
    void withNothingLockedTheReleaseStillFliesAlongTheNose() {
        World w = arena(0);
        fuel(w, Astra.JALA, 5);
        w.astraBegin();
        hold(w, 1.2f);
        assertTrue(w.astraLoose(), "letting go looses the astra whether or not a foe is in front");
        assertEquals(0, w.hangar.shards[Astra.JALA], "and it is paid for");
        assertTrue(w.astra.stream().anyMatch(c -> c.armed), "the cubes are in flight");
        float y0 = w.astra.get(0).y;
        for (int i = 0; i < 20; i++) w.updateAstra(1 / 60f);
        assertTrue(Math.abs(w.astra.get(0).y - y0) > 20f, "flying on along the nose");
    }

    @Test
    void aCubeLoosedAtNothingFindsAFoeItMeetsAhead() {
        World w = arena(0);
        fuel(w, Astra.JALA, 5);
        w.astraBegin();
        hold(w, 1.2f);
        w.astraLoose();                                                  // nothing was locked
        Enemy late = foe(w.ship.x + (float) Math.cos(w.ship.facing) * 300f, w.ship.y + (float) Math.sin(w.ship.facing) * 300f);
        w.enemies.add(late);                                             // a foe steps into the way after the loose
        float hp = late.hp;
        for (int i = 0; i < 90 && late.hp >= hp; i++) w.updateAstra(1 / 60f);
        assertTrue(late.hp < hp, "a cube with nothing to chase turns on the foe it finds");
    }

    @Test
    void longerHoldsLooseMoreCubesAndHarderHits() {
        World quick = arena(12), full = arena(12);
        fuel(quick, Astra.JALA, 5);
        fuel(full, Astra.JALA, 5);
        quick.astraBegin();
        hold(quick, 0.6f);
        quick.astraLoose();
        full.astraBegin();
        hold(full, Astra.WIND_FULL);
        full.astraLoose();
        assertTrue(quick.astra.size() < full.astra.size(), "more cubes: " + quick.astra.size() + " vs " + full.astra.size());
        assertTrue(maxDamage(quick) < maxDamage(full), "and harder: " + maxDamage(quick) + " vs " + maxDamage(full));
    }

    @Test
    void everyCubeFliesAtALockedFoe() {
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.astraBegin();
        hold(w, Astra.WIND_FULL);
        w.astraLoose();
        for (AstraCube c : w.astra) assertNotNull(c.target, "no cube is loosed at nothing");
    }

    // --- the cubes ---------------------------------------------------------------------------------

    @Test
    void aCubeGoesThroughRockToItsTarget() {
        World w = arena(0);
        Enemy e = foe(2600, 6000);
        w.enemies.add(e);
        AstraCube c = new AstraCube(2600, 3000, (float) Math.PI / 2, 900f, e, 0);
        c.dress(Astra.AKASHA, 1f);
        float startY = c.y;
        for (int i = 0; i < 180; i++) c.think(w, DT, c.turn);
        assertTrue(c.y > startY + 2000f, "it crossed rock and open world alike (y " + startY + " -> " + c.y + ")");
    }

    @Test
    void aCubeDamagesTheEnemyItWasSentFor() {
        World w = arena(0);
        Enemy e = foe(2640, 3000);
        w.enemies.add(e);
        AstraCube c = new AstraCube(2600, 3000, 0f, 900f, e, 0);
        c.dress(Astra.AKASHA, 1f);
        float before = e.hp;
        for (int i = 0; i < 30 && e.hp == before; i++) c.think(w, DT, c.turn);
        assertTrue(e.hp < before, "the cube took " + before + " down to " + e.hp);
    }

    // --- the five characters ---------------------------------------------------------------------

    private static World invoke(int kind, int foes, float hold) {
        World w = arena(foes);
        for (int t = 0; t < Astra.ELEMENTS; t++) w.hangar.shards[t] = 5;
        w.astraSel = kind;
        assertTrue(w.astraBegin());
        hold(w, hold);
        assertTrue(w.astraLoose(), "it flew");
        return w;
    }

    @Test
    void akashaRefillsTheShieldAndVayuTheBoost() {
        World a = arena(3);
        fuel(a, Astra.AKASHA, 5);
        a.ship.shieldEnergy = 0;
        a.astraBegin();
        hold(a, 1.0f);
        a.astraLoose();
        assertEquals(a.ship.shieldCap(), a.ship.shieldEnergy, 1e-3f, "Akasha refills the shield");

        World v = arena(3);
        fuel(v, Astra.VAYU, 5);
        v.astraSel = Astra.VAYU;
        v.ship.boostEnergy = 0;
        v.astraBegin();
        hold(v, 1.0f);
        v.astraLoose();
        assertEquals(100f, v.ship.boostEnergy, 1e-3f, "Vayu refills the boost");
    }

    @Test
    void prithviGivesStoneSkin() {
        World w = arena(3);
        fuel(w, Astra.PRITHVI, 5);
        w.astraSel = Astra.PRITHVI;
        w.ship.iFrames = 0;
        w.astraBegin();
        hold(w, 1.0f);
        w.astraLoose();
        assertTrue(w.ship.iFrames >= Astra.STONE_SKIN - 1e-3f, "two seconds of stone skin");
    }

    @Test
    void noAstraMendsTheHullOrHandsBackALife() {
        for (int kind = 0; kind < Astra.KINDS; kind++) {
            World w = invoke(kind, 4, 1.0f);
            assertEquals(3, w.lives, "kind " + kind + " leaves the lives alone");
        }
        World w = arena(3);
        fuel(w, Astra.AKASHA, 5);
        w.ship.hp = 10;
        w.astraBegin();
        hold(w, 1.0f);
        w.astraLoose();
        assertEquals(10f, w.ship.hp, 1e-3f, "and the loosing does not heal the hull");
    }

    @Test
    void theFloodMendsTheHullWithEveryHit() {
        World w = arena(0);
        Enemy e = foe(2640, 3000);
        w.enemies.add(e);
        w.ship.hp = 20;
        AstraCube c = new AstraCube(2600, 3000, 0f, 900f, e, 0);
        c.dress(Astra.JALA, 1f);
        w.astra.add(c);
        for (int i = 0; i < 60 && !w.astra.isEmpty(); i++) w.updateAstra(DT);
        assertEquals(20f + Astra.HEAL_PER_HIT[Astra.JALA], w.ship.hp, 1e-3f, "one hit, one mend");
    }

    @Test
    void thePyreBurnsTheCrowdRoundTheFoeItStrikes() {
        World w = arena(0);
        Enemy target = foe(2640, 3000), neighbour = foe(2700, 3000);
        w.enemies.add(target);
        w.enemies.add(neighbour);
        AstraCube c = new AstraCube(2600, 3000, 0f, 850f, target, 0);
        c.dress(Astra.AGNI, 1f);
        w.astra.add(c);
        float before = neighbour.hp;
        for (int i = 0; i < 60 && !w.astra.isEmpty(); i++) w.updateAstra(DT);
        assertEquals(before - Astra.DAMAGE[Astra.AGNI] * Astra.SPLASH_SHARE, neighbour.hp, 1e-2f,
                "the neighbour took the splash share");
    }

    @Test
    void theGaleChainsFromFoeToFoe() {
        World w = arena(0);
        Enemy a = foe(2640, 3000), b = foe(2800, 3000);
        a.hp = a.maxHp = 40;
        b.hp = b.maxHp = 40;
        w.enemies.add(a);
        w.enemies.add(b);
        AstraCube c = new AstraCube(2600, 3000, 0f, 1500f, a, 0);
        c.dress(Astra.VAYU, 1f);
        w.astra.add(c);
        for (int i = 0; i < 120; i++) w.updateAstra(DT);
        assertTrue(a.dead, "the first foe fell");
        assertTrue(b.dead, "and the same cube carried on to the second");
    }

    @Test
    void theBrahmastraBloomsAsALotusAndPaysFiveOfEveryElement() {
        World w = invoke(Astra.BRAHMA, 6, Astra.WIND_FULL);
        assertEquals(vajra.world.AstraFlow.LOTUS, w.flow.kind, "the last arrow is a lotus: five petals");
        for (int t = 0; t < Astra.ELEMENTS; t++) assertEquals(0, w.hangar.shards[t], "five of each were paid");
    }

    // --- the choice -----------------------------------------------------------------------------

    @Test
    void xStepsOnlyToAstrasYouCanPay() {
        World w = arena(1);
        fuel(w, Astra.VAYU, 5);
        fuel(w, Astra.JALA, 5);
        w.astraSel = Astra.AKASHA;
        assertTrue(w.cycleAstra());
        assertEquals(Astra.VAYU, w.astraSel);
        assertTrue(w.cycleAstra());
        assertEquals(Astra.JALA, w.astraSel);
        assertTrue(w.cycleAstra());
        assertEquals(Astra.VAYU, w.astraSel, "and wraps");
    }

    @Test
    void ifTheChoiceCannotBePaidTheFirstThatCanIsLoaded() {
        World w = arena(1);
        fuel(w, Astra.AGNI, 5);
        w.astraSel = Astra.AKASHA;
        assertEquals(Astra.AGNI, w.loadedAstra());
        w.hangar.shards[Astra.AGNI] = 0;
        assertEquals(-1, w.loadedAstra(), "nothing to load");
    }

    // --- the wire --------------------------------------------------------------------------------

    @Test
    void aGuestsReleaseIsLoosedOnTheHubForThatGuestAndPaysThem() {
        World hub = arena(2);
        Enemy a = hub.enemies.get(0), b = hub.enemies.get(1);
        a.netId = 11;
        b.netId = 12;
        hub.astraFor(1, Astra.JALA, 1f, new int[] {11, 12});
        assertEquals(2, hub.astra.size());
        for (AstraCube c : hub.astra) {
            assertEquals(1, c.owner, "the kill is the guest's");
            assertEquals(Astra.JALA, c.element);
            assertEquals(Astra.DAMAGE[Astra.JALA] * Astra.damageMult(1f), c.damage, 1e-3f);
        }
        hub.astra.clear();
        hub.astraFor(1, 99, 1f, new int[] {11}); // a nonsense kind is dropped, never a crash
        assertTrue(hub.astra.isEmpty());
    }

    @Test
    void aGuestsAgniOrVayuIsALoosedFlowOnTheHubFromThatGuestsHull() {
        World hub = arena(0);
        hub.astraFor(1, Astra.AGNI, 1f, new int[0]);
        assertEquals(vajra.world.AstraFlow.AGNI, hub.flow.kind, "the fire pours out on the hub");
        assertEquals(1, hub.flow.seat, "and its kills are the guest's");
        hub.astraFor(1, Astra.VAYU, 1f, new int[0]);
        assertEquals(vajra.world.AstraFlow.VAYU, hub.flow.kind, "and the tornado turns there");
    }

    // --- helpers -----------------------------------------------------------------------------------

    private static int heldCubes(World w) {
        int n = 0;
        for (AstraCube c : w.astra) if (!c.armed) n++;
        return n;
    }

    private static float maxDamage(World w) {
        float m = 0;
        for (AstraCube c : w.astra) m = Math.max(m, c.damage);
        return m;
    }
}

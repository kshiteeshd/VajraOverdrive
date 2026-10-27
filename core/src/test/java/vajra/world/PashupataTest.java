package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.Hangar;

/** THE PASHUPATASTRA (Rasatala's, the third eye) and the grandeur of depth: fire and stone pay for it, its beam ignores armour and rock, the wind-up is slow, and
 *  the deeper the loka an astra lives in, the greater it is. */
class PashupataTest {
    private static final float DT = 1 / 60f;

    private static World arena() {
        World w = new World();
        w.hangar.owned[0][1] = true; // the 2nd variant: only it can forge an astra
        w.enemies.clear();
        w.ship.x = 2600;
        w.ship.y = 2700;
        w.ship.facing = (float) (Math.PI / 2);
        return w;
    }

    private static Enemy foe(float x, float y) {
        Enemy e = new Enemy(Enemy.Kind.MAYAVI, x, y);
        e.hp = e.maxHp = 400;
        e.active = true;
        return e;
    }

    @Test
    void fireAndStoneTogetherPayForIt() {
        Hangar h = new Hangar();
        h.owned[h.shipIndex][1] = true; // a 2nd variant: only it can forge an astra
        h.shards[Astra.AGNI] = 3;
        h.shards[Astra.PRITHVI] = 2;
        assertTrue(Astra.canLoose(h, Astra.PASHUPATA), "three of fire and two of stone are five");
        assertTrue(Astra.pay(h, Astra.PASHUPATA));
        assertEquals(0, h.shards[Astra.AGNI] + h.shards[Astra.PRITHVI], "all five are spent");
        h.shards[Astra.AKASHA] = 99;
        h.shards[Astra.JALA] = 99;
        assertFalse(Astra.canLoose(h, Astra.PASHUPATA), "no other element can pay");
    }

    @Test
    void theThirdEyeBurnsThroughArmourAndRock() {
        World w = arena();
        Enemy plated = foe(2600, 3100);
        plated.armoured = true;
        w.enemies.add(plated);
        Enemy inRock = foe(2600, 3400);
        inRock.inRock = true;
        w.enemies.add(inRock);
        w.hangar.shards[Astra.AGNI] = 5;
        w.astraSel = Astra.PASHUPATA;
        assertTrue(w.astraBegin(), "the invocation starts");
        for (float t = 0; t < Astra.WIND_FULL; t += DT) w.updateAstra(DT);
        assertTrue(w.astraLoose(), "and looses");
        assertTrue(w.gazeT > 0f, "the beam is burning");
        for (int i = 0; i < 90; i++) w.updateAstra(DT);
        assertTrue(plated.hp < 400f - 150f, "armour did not turn it aside: " + plated.hp);
        assertTrue(inRock.hp < 400f - 100f, "a body in rock is not safe from it: " + inRock.hp);
        for (int i = 0; i < 300; i++) w.updateAstra(DT);
        assertEquals(0f, w.gazeT, 0.001f, "the beam closes after " + Astra.GAZE_TIME + " s");
    }

    @Test
    void theBeamFollowsTheNose() {
        World w = arena();
        Enemy east = foe(3100, 2700);
        w.enemies.add(east);
        w.hangar.shards[Astra.AGNI] = 5;
        w.astraSel = Astra.PASHUPATA;
        w.astraBegin();
        for (float t = 0; t < Astra.WIND_FULL; t += DT) w.updateAstra(DT);
        w.astraLoose();
        for (int i = 0; i < 30; i++) w.updateAstra(DT);
        assertEquals(400f, east.hp, 0.01f, "nothing is hit while the nose points north");
        w.ship.facing = 0f;                                              // swing it east
        for (int i = 0; i < 60; i++) w.updateAstra(DT);
        assertTrue(east.hp < 400f, "the beam is steered by the nose onto what it now points at");
    }

    @Test
    void theWindUpIsSlowAndTheDeeperAstrasAreGreater() {
        assertTrue(Astra.WIND_FULL >= 3f, "the formation takes its time");
        assertTrue(Astra.WIND_MIN > 0.6f);
        assertEquals(7, Astra.KINDS);
        assertEquals(0, Astra.TIER[Astra.AKASHA], "Akasha is Atala's");
        assertEquals(1, Astra.TIER[Astra.AGNI], "Agni is Vitala's");
        assertEquals(5, Astra.TIER[Astra.PASHUPATA], "the third eye is Rasatala's");
        assertEquals(6, Astra.TIER[Astra.BRAHMA], "the last arrow is Patala's");
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int t : Astra.TIER) seen.add(t);
        assertEquals(7, seen.size(), "seven astras, seven lokas, no two alike");
        assertEquals(7, Astra.NAME.length);
        assertEquals(7, Astra.EPITHET.length);
        assertEquals(7, Astra.EFFECT.length);
    }
}

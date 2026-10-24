package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

/** Vitala's mechanics (docs/VITALA_PLAN.md): the Hataki's current, the fire vents' beat, the shrine that hushes both, the wraith that walks through
 *  rock, the names, the ash and the stages of the two rulers. */
class VitalaMechanicsTest {
    private static ShipControls steer(float tx, float ty, World w) {
        return new ShipControls() {
            public float thrustY() { return Math.max(-1f, Math.min(1f, (ty - w.ship.y) / 80f)); }
            public float thrustX() { return Math.max(-1f, Math.min(1f, (tx - w.ship.x) / 60f)); }
            public float rotate() { return 0; }
            public boolean fire() { return false; }
            public boolean boost() { return false; }
            public boolean shield() { return false; }
            public boolean mouseAim() { return false; }
            public float aimX() { return 0; }
            public float aimY() { return 0; }
        };
    }

    static ShipControls idle() {
        return new ShipControls() {
            public float thrustY() { return 0; }
            public float thrustX() { return 0; }
            public float rotate() { return 0; }
            public boolean fire() { return false; }
            public boolean boost() { return false; }
            public boolean shield() { return false; }
            public boolean mouseAim() { return false; }
            public float aimX() { return 0; }
            public float aimY() { return 0; }
        };
    }

    private static World vitala() {
        World w = World.vitala(0);
        w.enemies.clear();
        return w;
    }

    @Test
    void theVentsBreatheOnAThreeSecondBeat() {
        Vents v = new Vents();
        v.at = new float[][] {{100f, 100f, 0f, 0f}};
        assertEquals(Vents.WARNING, v.state(0, 0.5f, false));
        assertEquals(Vents.FLAMING, v.state(0, 1.5f, false));
        assertEquals(Vents.COOL, v.state(0, 2.5f, false));
        assertEquals(Vents.WARNING, v.state(0, 3.1f, false), "the beat repeats");
        assertEquals(Vents.COOL, v.state(0, 1.5f, true), "a lit shrine hushes the band");
        assertTrue(v.burns(0, 250f, 100f, 10f, 1.5f, false), "inside the jet while it flames");
        assertFalse(v.burns(0, 250f, 100f, 10f, 0.5f, false), "not while it only warns");
        assertFalse(v.burns(0, 250f, 160f, 10f, 1.5f, false), "not beside the jet");
        assertFalse(v.burns(0, 460f, 100f, 10f, 1.5f, false), "not beyond its reach");
        assertTrue(v.burns(0, 250f, 100f, 10f, 4.5f, false), "the same, a beat later");
    }

    @Test
    void vitalaHasVentsInTheSmeltersAndTheHearthOnly() {
        World w = World.vitala(0);
        assertTrue(w.vents.count() >= 12, "vents: " + w.vents.count());
        int rim = 0, smelters = 0, hearth = 0;
        for (float[] a : w.vents.at) {
            if (a.length > 4) { rim++; continue; }                  // (the Altar's rim vents are the boss's)
            int band = Hataki.band(a[1]);
            assertTrue(band == 2 || band == 4, "vent in band " + band);
            if (band == 2) smelters++; else hearth++;
            for (Checkpoint c : w.checkpoints) assertTrue(Math.hypot(c.x - a[0], c.y - a[1]) >= 280f, "a vent beside a shrine");
            assertTrue(w.blockedPx(a[0] + (float) Math.cos(a[2]) * -10f, a[1]), "the vent sits in the wall face");
            assertFalse(w.blockedPx(a[0] + (float) Math.cos(a[2]) * 40f, a[1]), "and breathes into open air");
        }
        assertTrue(rim >= 4, "the Altar's rim vents: " + rim);
        assertTrue(smelters >= 5 && hearth >= 6, smelters + " / " + hearth);
        for (float[] a : w.vents.at) if (a.length == 4 && Hataki.band(a[1]) == 2) assertEquals(0f, a[3], 1e-4f);
        boolean zero = false, half = false;
        for (float[] a : w.vents.at) if (a.length == 4 && Hataki.band(a[1]) == 4) { zero |= a[3] == 0f; half |= a[3] == 1.5f; }
        assertTrue(zero && half, "two clocks in the Hearth Deep");
    }

    @Test
    void theRapidsCarryAnIdleHullUpTheRiver() {
        World w = vitala();
        float y0 = Hataki.START[4] + 1500f;
        w.ship.x = Hataki.centre(y0);
        w.ship.y = y0;
        for (int i = 0; i < 60; i++) w.update(1 / 60f, idle());
        assertTrue(w.ship.y - y0 > 60f, "a second in the rapids carried the hull up: " + (w.ship.y - y0));
        w.ship.x = Hataki.centre(y0) + 500f;
        w.ship.y = y0;
        float before = w.ship.y;
        for (int i = 0; i < 30; i++) w.update(1 / 60f, idle());
        assertTrue(Math.abs(w.ship.y - before) < 12f, "off the river nothing pulls: " + (w.ship.y - before));
    }

    @Test
    void thePoolsOnlyDrift() {
        World w = vitala();
        float py = Hataki.START[1] + 800f, ry = Hataki.START[4] + 1500f;
        float[] f = w.currentAt(Hataki.centre(py), py);
        assertTrue(f[1] > 0f && f[1] < 60f, "a gentle drift in the Mouth: " + f[1]);
        float[] r = w.currentAt(Hataki.centre(ry), ry);
        assertTrue(r[1] > 90f, "a hard pull in the rapids: " + r[1]);
        assertTrue(Hataki.forced(ry) && !Hataki.forced(py));
    }

    @Test
    void aLitShrineHushesTheCurrentAndTheVentsForSixSeconds() {
        World w = vitala();
        Checkpoint cp = w.checkpoints[5];
        w.ship.x = cp.x;
        w.ship.y = cp.y;
        w.update(1 / 60f, idle());
        assertTrue(cp.collected, "the shrine lit");
        assertTrue(w.quietT > 5.5f, "hushed: " + w.quietT);
        float[] f = w.currentAt(Hataki.centre(cp.y), cp.y);
        assertEquals(0f, f[0] + f[1], 1e-4f, "no current while hushed");
        for (int i = 0; i < 60 * 7; i++) w.update(1 / 60f, idle());
        assertEquals(0f, w.quietT, 1e-4f, "the hush ends after six seconds");
        World a = new World(0, World.CAMPAIGN);
        a.enemies.clear();
        a.ship.x = a.checkpoints[0].x;
        a.ship.y = a.checkpoints[0].y;
        a.update(1 / 60f, idle());
        assertEquals(0f, a.quietT, 1e-4f, "an Atala lamp hushes nothing");
    }

    @Test
    void aVentBurnsAHullInItsJetWhileItFlames() {
        World w = vitala();
        float[] a = w.vents.at[0];
        assertEquals(4, a.length, "the first vent is a river one");
        w.clock = Vents.WARN + Vents.FLAME / 2f - a[3] - 0.0166f;
        w.ship.x = a[0] + (float) Math.cos(a[2]) * 150f;
        w.ship.y = a[1];
        float hp = w.ship.hp;
        w.update(1 / 60f, idle());
        assertTrue(w.ship.hp < hp, "the jet scalded the hull: " + hp + " -> " + w.ship.hp);
    }

    /** A pilot who only follows the river centre flies from Vitala's floor to the Altar's gate: nothing in the map shuts the road. */
    @Test
    void followingTheRiverReachesTheAltar() {
        World w = vitala();
        w.vents.at = new float[0][];             // (the vents' damage is a different test)
        float t = 0f;
        while (w.ship.y < Hataki.START[5] + 200f && t < 900f) {
            float ty = w.ship.y + 220f;
            w.update(1 / 60f, steer(Hataki.centre(ty), ty, w));
            t += 1 / 60f;
        }
        assertTrue(w.ship.y >= Hataki.START[5] + 200f, "reached the Altar's gate at y=" + w.ship.y + " after " + t + " s");
    }

    @Test
    void theBhutaPhasesThroughRockAndHasNoBodyInsideIt() {
        World w = vitala();
        float y = Hataki.START[2] + 1000f, x = Hataki.centre(y);
        while (!w.blockedPx(x, y)) x += 4f;
        float x2 = x;
        while (w.blockedPx(x2, y)) x2 += 4f;
        assertTrue(x2 - x >= 24f, "a wall at least a tile thick: " + (x2 - x));
        x = (x + x2) / 2f;
        Enemy g = new Enemy(Enemy.Kind.HATAKA, Hataki.centre(y), y);
        w.add(g);                                                   // (add carves the rock round a post: so it is placed in the open, then moved in)
        g.x = x;
        assertTrue(w.blockedPx(g.x, g.y), "now in solid rock");
        assertTrue(g.phasing && g.vitala, "Vitala's HATAKA is Rekha the wraith: it phases");
        g.active = true;
        w.ship.x = Hataki.centre(y);
        w.ship.y = y;
        w.update(1 / 60f, idle());
        assertTrue(g.inRock, "it floats inside the wall");
        float hp = g.hp;
        assertFalse(g.hurt(50f, w), "a body in rock cannot be hit");
        assertEquals(hp, g.hp, 1e-4f);
        assertTrue(g.submerged(), "no body in the fight while it is in the rock");
        Enemy lancer = new Enemy(Enemy.Kind.LANCE, Hataki.centre(y), y + 200f);
        w.add(lancer);
        assertFalse(lancer.phasing, "only Rekha phases");
        World atala = new World();
        Enemy h = new Enemy(Enemy.Kind.HATAKA, 2600, 3000);
        atala.add(h);
        assertFalse(h.phasing || h.vitala, "Atala's hataka are saucers, not ghosts");
    }

    @Test
    void vitalaRosterWearsTheOriginalBestiaryNames() {
        String[] want = {"DHUMA", "REKHA", "KILA", "AGNEYA", "TAMRA", "JVALA", "BHAIRAVA"};
        Enemy.Kind[] slots = {Enemy.Kind.AURIS, Enemy.Kind.HATAKA, Enemy.Kind.LANCE, Enemy.Kind.VAKRA, Enemy.Kind.BHAVI, Enemy.Kind.MAYAVI, Enemy.Kind.BALA};
        for (int i = 0; i < want.length; i++) assertEquals(want[i], Enemy.nameIn(slots[i], 1));
        assertEquals("MAYAVI", Enemy.nameIn(Enemy.Kind.MAYAVI, 0), "Atala keeps its own names");
        assertEquals("Bhairava", LokaTable.at(1).boss);
    }

    private static Enemy summonBhairava(World w) {
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        assertTrue(w.boss != null, "the quota fills and Bhairava stands in the Altar");
        return w.boss;
    }

    @Test
    void bhairavaKeepsTheAltarAndHisThreePhasesTurnTheHallInto() {
        World w = vitala();
        Enemy b = summonBhairava(w);
        assertEquals(Hataki.ALTAR_X, b.x, 1f);
        assertEquals(Hataki.ALTAR_Y, b.y, 1f);
        assertTrue(b.vitala && b.boss);
        assertTrue(w.bossWakeY > Hataki.START[5] && w.bossWakeY < b.y, "he wakes as the hull comes into the Altar");
        w.ship.x = Hataki.ALTAR_X;
        w.ship.y = Hataki.ALTAR_Y - 380f;
        int rim = -1;
        for (int i = 0; i < w.vents.count(); i++) if (w.vents.at[i].length > 4) { rim = i; break; }
        assertTrue(rim >= 0);
        w.update(1 / 60f, idle());
        assertEquals(1, b.bossPhase, "stoking: full hull");
        assertEquals(Vents.COOL, w.vents.state(rim, 1.5f, false), "phase 1: the rim holds");
        b.hp = b.maxHp * 0.5f;
        w.update(1 / 60f, idle());
        assertEquals(2, b.bossPhase, "kindling at two thirds");
        w.update(1 / 60f, idle());
        assertEquals(Vents.FLAMING, w.vents.state(rim, 1.5f - w.vents.at[rim][3], false), "phase 2: the rim vents breathe");
        b.hp = b.maxHp * 0.2f;
        w.ship.x = Hataki.ALTAR_X - 200f;
        w.ship.y = Hataki.ALTAR_Y - 250f;
        float x0 = w.ship.x;
        for (int i = 0; i < 61; i++) w.update(1 / 60f, idle());
        assertEquals(3, b.bossPhase);
        assertTrue(w.flood, "phase 3 is the Flood");
        assertTrue(w.ship.x - x0 > 30f, "the flood carries the hull east: " + (w.ship.x - x0));
        // the ash ring closes as he falls
        assertTrue(w.hazards.ring);
        assertTrue(w.hazards.ringR < vajra.enemy.BhairavaBrain.safeRadius(0.5f), "the safe arena has shrunk: " + w.hazards.ringR);
        assertEquals(vajra.enemy.BhairavaBrain.SAFE_MAX, vajra.enemy.BhairavaBrain.safeRadius(1f), 1e-3f);
        assertEquals(vajra.enemy.BhairavaBrain.SAFE_MIN, vajra.enemy.BhairavaBrain.safeRadius(0f), 1e-3f);
    }

    @Test
    void bhairavaFallingLiftsTheSealAndOpensTheWayToSutalasHall() {
        World w = vitala();
        Enemy b = summonBhairava(w);
        int c = (w.sealC0 + w.sealC1) / 2;
        assertTrue(w.sealMap.get(c, w.sealRow) != TileMap.EMPTY, "the north door is sealed in gold");
        w.victory(b);
        assertEquals(TileMap.EMPTY, w.sealMap.get(c, w.sealRow), "the kill lifts it");
        assertFalse(w.won, "the campaign does not end here");
        assertFalse(w.hazards.ring, "the ash ring goes out with its herald");
        w.victoryT = World.VICTORY_LAP + 1f;
        assertFalse(w.runEnded(), "no results card: Sutala's hall lies beyond the door");
        assertFalse(w.blockedPx(Hataki.ALTAR_X, Hataki.HEIGHT + 300f), "the passage runs on past the door");
        assertFalse(w.blockedPx(Hataki.HALL_X, Hataki.HALL_Y), "into the hall");
        assertTrue(w.blockedPx(Hataki.HALL_X, Hataki.TOP - 20f), "which is closed at the top");
        assertTrue(w.blockedPx(Hataki.HALL_X + 1300f, Hataki.HALL_Y), "and at the sides");
    }

    @Test
    void balaFallingOpensTheRoadUpIntoVitalaAndTheSecondRulerBecomesTheQuota() {
        World a = new World(0, World.CAMPAIGN);
        assertEquals(0, a.stage);
        Enemy bala = summonBala(a);
        int c = (a.sealC0 + a.sealC1) / 2;
        assertTrue(a.sealMap.get(c, a.sealRow) != TileMap.EMPTY, "Bala's door is sealed");
        TileMap door = a.sealMap;
        int row = a.sealRow;
        a.victory(bala);
        assertEquals(TileMap.EMPTY, door.get(c, row), "his fall opens the gold door at the top of Atala");
        assertEquals(1, a.stage, "and Bhairava is the stage now");
        assertTrue(a.boss == null && !a.won && !a.runEnded(), "the run goes on, up into Vitala");
        assertFalse(a.quotaMet(), "Vitala's own quota, not Atala's");
        assertEquals(Hataki.ALTAR_Y, a.pitY, 1f);
        a.victoryT = World.VICTORY_LAP + 1f;
        assertFalse(a.runEnded());
    }

    private static Enemy summonBala(World w) {
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        assertTrue(w.boss != null && w.boss.kind == Enemy.Kind.BALA && !w.boss.vitala, "Bala holds the first arena");
        return w.boss;
    }

    @Test
    void theForgeLandsHeatTheHullUnlessAShrineHoldsItsBreath() {
        World w = vitala();
        w.ship.x = Hataki.centre(Hataki.START[2] + 500f);
        w.ship.y = Hataki.START[2] + 500f;
        w.update(1 / 60f, idle());
        assertTrue(w.ship.heated, "heated in the Smelters");
        assertEquals(Ship_COOLDOWN(w, true), w.ship.cooldownMult() , 1e-4f);
        w.quietT = 3f;
        w.update(1 / 60f, idle());
        assertFalse(w.ship.heated, "a hushed band is not hot");
        w.quietT = 0f;
        w.ship.x = Hataki.CX;
        w.ship.y = 2000f;                                           // back down in Atala
        w.update(1 / 60f, idle());
        assertFalse(w.ship.heated, "Atala is cold");
        World a = new World();
        a.update(1 / 60f, idle());
        assertFalse(a.ship.heated);
    }

    private static float Ship_COOLDOWN(World w, boolean heated) {
        return w.hangar.cooldownMult(w.ship.def) * (heated ? vajra.ship.Ship.HEAT_COOLDOWN : 1f);
    }
}

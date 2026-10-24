package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

/** Bodies have room: nobody starts the run on top of anyone, nobody lives inside rock, two units
 *  that end up stacked part, and a unit that finds itself in rock is lifted out. */
class SpaceTest {
    // These tests are about the rock the world was built with; the default world has none (World.walls).
    @org.junit.jupiter.api.BeforeAll
    static void rockOn() { World.walls = true; }

    @org.junit.jupiter.api.AfterAll
    static void rockOff() { World.walls = true; }

    @Test
    void theRosterStartsWithoutOverlaps() {
        World w = new World();
        int overlaps = 0, pairs = 0;
        for (int i = 0; i < w.enemies.size(); i++)
            for (int j = i + 1; j < w.enemies.size(); j++) {
                Enemy a = w.enemies.get(i), b = w.enemies.get(j);
                float need = (a.w + b.w) * 0.5f * 0.85f;
                float dx = a.x - b.x, dy = a.y - b.y;
                pairs++;
                if (dx * dx + dy * dy < need * need) overlaps++;
            }
        assertTrue(pairs > 1000);
        assertEquals(0, overlaps, "no two bodies are placed on top of each other");
    }

    @Test
    void nobodyIsPlacedInsideRock() {
        World w = new World();
        for (Enemy e : w.enemies)
            assertFalse(w.blockedPx(e.x, e.y), e.kind + " at " + e.x + "," + e.y + " stands in rock");
    }

    @Test
    void aLeaderIsBiggerThanItsCourtAndAnEliteBiggerAgain() {
        Enemy plain = new Enemy(Enemy.Kind.VAKRA, 0, 0), lead = new Enemy(Enemy.Kind.VAKRA, 0, 0),
                elite = new Enemy(Enemy.Kind.VAKRA, 0, 0);
        lead.rank = 1;
        elite.rank = 1;
        elite.elite = true;
        assertTrue(lead.drawSize() > plain.drawSize());
        assertTrue(elite.drawSize() > lead.drawSize());
        assertTrue(plain.drawSize() <= 64, "no body is drawn bigger than its 64 px cell");
        assertTrue(Enemy.baseDraw(Enemy.Kind.AURIS) < Enemy.baseDraw(Enemy.Kind.DVARAPALA), "a scout is smaller than a door-ward");
    }

    @Test
    void twoStackedUnitsPart() {
        World w = new World();
        w.enemies.clear();
        w.ship.x = 2600;
        w.ship.y = 700; // far from anyone: the test is about the pair
        Enemy a = new Enemy(Enemy.Kind.VAKRA, 2600, 5200), b = new Enemy(Enemy.Kind.VAKRA, 2604, 5200);
        a.parley = true; // nobody wakes or steers: only the separation law is under test
        b.parley = false;
        a.parley = false;
        a.active = false;
        b.active = false;
        w.enemies.add(a);
        w.enemies.add(b);
        for (int i = 0; i < 120; i++) w.update(1 / 60f, ShipControls.NONE);
        float d = (float) Math.hypot(a.x - b.x, a.y - b.y);
        assertTrue(d >= (a.w + b.w) * 0.5f * 0.9f, "the pair parted to " + d);
    }

    @Test
    void aUnitInRockIsLiftedOut() {
        World w = new World();
        // find a rock pixel in band 1 and put a unit in it
        float rx = -1, ry = -1;
        outer:
        for (float y = 2200; y < 4000; y += 8)
            for (float x = 300; x < 4900; x += 8)
                if (w.blockedPx(x, y)) {
                    rx = x;
                    ry = y;
                    break outer;
                }
        assertTrue(rx > 0, "the band has rock");
        w.enemies.clear();
        w.ship.x = 2600;
        w.ship.y = 700;
        Enemy e = new Enemy(Enemy.Kind.AURIS, rx, ry);
        e.active = true;
        e.homeX = 2600; // home is open air on the centre line, so a unit with nowhere near to go can still go home
        e.homeY = 2400;
        assertFalse(w.blockedPx(e.homeX, e.homeY));
        w.enemies.add(e);
        for (int i = 0; i < 30; i++) w.update(1 / 60f, ShipControls.NONE);
        assertFalse(w.blockedPx(e.x, e.y), "the unit is lifted into open air");
    }
}

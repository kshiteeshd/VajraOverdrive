package vajra.world;

import vajra.ship.ShipControls;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The test arena: every scenario builds a world and survives two seconds of flight, in every category. */
class ArenaTest {
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

    @Test
    void everyCategoryHasScenariosAndTheCatalogueIsBig() {
        assertTrue(Arena.count() >= 140, "the arena holds a scenario for every part of the game: " + Arena.count());
        for (String c : Arena.CATEGORIES) assertFalse(Arena.in(c).isEmpty(), c + " is empty");
        int total = 0;
        for (String c : Arena.CATEGORIES) total += Arena.in(c).size();
        assertEquals(Arena.count(), total, "every scenario sits in a listed category");
        assertEquals(36, Arena.in(Arena.WEAPONS).stream().filter(s -> Character.isDigit(s.name.charAt(0))).count(), "one scenario per weapon");
        assertEquals(7, Arena.in(Arena.SHIPS).stream().filter(s -> Character.isDigit(s.name.charAt(0))).count(), "one per hull");
    }

    @Test
    void everyScenarioBuildsAndFliesWithoutError() {
        for (String c : Arena.CATEGORIES)
            for (Arena.Scenario s : Arena.in(c)) {
                World w;
                try {
                    w = Arena.build(s);
                    for (int i = 0; i < 120; i++) w.update(0.016f, IDLE);
                } catch (RuntimeException e) {
                    throw new AssertionError("scenario failed: " + c + " / " + s.name, e);
                }
                assertNotNull(w.ship, s.name);
                assertFalse(Float.isNaN(w.ship.x) || Float.isNaN(w.ship.y), s.name + " put the hull somewhere impossible");
                assertTrue(w.ship.x > 0 && w.ship.x < w.width && w.ship.y > 0 && w.ship.y < w.height, s.name + " put the hull outside the world");
            }
    }

    @Test
    void theArmouryIsFullAndTheNewPilotIsNot() {
        World w = Arena.build(Arena.in(Arena.SANDBOX).get(0));
        assertTrue(w.hangar.weaponOwned[35] && w.hangar.canForge() && w.hangar.owned[6][2]);
        World n = Arena.build(Arena.in(Arena.SANDBOX).get(1));
        assertFalse(n.hangar.weaponOwned[1]);
        assertFalse(n.hangar.canForge());
    }

    @Test
    void aDummyTakesHitsAndNeverFallsOrMoves() {
        World w = Arena.build(Arena.in(Arena.SANDBOX).get(3));
        vajra.enemy.Enemy d = null;
        for (vajra.enemy.Enemy e : w.enemies) if (e.dummy) { d = e; break; }
        assertNotNull(d);
        float x = d.x, y = d.y;
        d.hurt(900_000f, w);
        for (int i = 0; i < 60; i++) w.update(0.016f, IDLE);
        assertFalse(d.dead);
        assertTrue(d.hp >= d.maxHp * 0.5f);
        assertEquals(x, d.x, 0.01f);
        assertEquals(y, d.y, 0.01f);
    }

    @Test
    void theRulerScenariosSetTheRulerUpAwake() {
        for (Arena.Scenario s : Arena.in(Arena.BOSSES)) {
            if (!s.name.contains("FULL FIGHT")) continue;
            World w = Arena.build(s);
            assertNotNull(w.boss, s.name);
            for (int i = 0; i < 90; i++) w.update(0.016f, IDLE);
            assertTrue(w.boss.active, s.name + ": the ruler woke as the hull came in");
        }
    }
}

package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The rock tiles are the play layer by default; with `World.walls = false` (-Dvajra.walls=false) they are swept away after
 *  generation: nothing solid anywhere, nothing rooted in them, and the boulder belt alone marks the boundary. */
class WallsOffTest {
    private static World open() {
        World.walls = false;
        try {
            return new World();
        } finally {
            World.walls = true;
        }
    }

    @Test
    void withTheSwitchOffTheWorldHasNoRockAtAll() {
        World w = open();
        assertFalse(w.walled);
        for (TileMap m : w.tileMaps())
            for (int y = 0; y < m.rows; y += 3)
                for (int x = 0; x < m.cols; x += 3) assertEquals(TileMap.EMPTY, m.get(x, y), "tile " + x + "," + y);
        assertEquals(0, w.plants.length);
        assertEquals(0, w.torches.length);
        for (float y = 100; y < w.height; y += 700)
            for (float x = 100; x < w.width; x += 500)
                if (Math.abs(y - 1600) > 300) assertFalse(w.blockedPx(x, y), "open at " + x + "," + y); // (the boulder belt is not a wall)
        // the hull flies through where the gate wall stood
        w.ship.x = w.width / 2;
        w.ship.y = 1600;
        float[] c = w.collide(w.ship.x - 12, w.ship.y - 12, 24, 24);
        assertEquals(w.ship.x - 12, c[0], 1e-3f);
        assertEquals(w.ship.y - 12, c[1], 1e-3f);
    }

    @Test
    void byDefaultTheRockIsThere() {
        World w = new World();
        assertTrue(w.walled);
        int solid = 0;
        for (TileMap m : w.tileMaps())
            for (int y = 0; y < m.rows; y++) for (int x = 0; x < m.cols; x++) if (m.get(x, y) != TileMap.EMPTY) solid++;
        assertTrue(solid > 1000, "the old rock stands: " + solid);
    }

    @Test
    void theBoulderBeltIsSolidExceptAtThePass() {
        World w = open();
        int solid = 0, open = 0;
        for (float x = 20; x < w.width; x += 20) {
            boolean blocked = false;
            for (float y = 1490; y < 1710; y += 10) blocked |= w.blockedPx(x, y);
            if (Math.abs(x - 2600) < Boundary.PASS_HALF - 60) {
                assertFalse(blocked, "the pass is open at x=" + x);
                open++;
            } else if (blocked) solid++;
        }
        assertTrue(open > 10, "there is a pass");
        assertTrue(solid > 100, "and the rest of the line is rock: " + solid);
        // a hull pushed into a boulder comes out of it
        float[] b = w.boulders[0];
        float[] c = w.collide(b[0] - 12, b[1] - 12, 24, 24);
        assertTrue(Math.hypot(c[0] + 12 - b[0], c[1] + 12 - b[1]) >= b[2] * 0.85f - 1f, "pushed out of the rock");
    }
}

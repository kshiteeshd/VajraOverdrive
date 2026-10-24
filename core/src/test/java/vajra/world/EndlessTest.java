package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** There is no game over: the last life spent, the hull still wakes at the last lit lamp. */
class EndlessTest {
    @Test
    void theLastLifeNoLongerEndsTheRun() {
        World w = new World();
        w.lives = 1;
        w.ship.x = 2600;
        w.ship.y = 900;
        w.ship.invuln = 0;
        w.ship.damage(9999f, w);
        assertFalse(w.runEnded(), "no game over");
        assertEquals(0, w.lives, "the counter still falls to zero");
        assertTrue(w.ship.hp > 0, "and the hull is back on its feet");
        w.ship.invuln = 0;
        w.ship.damage(9999f, w);                       // and again, and again
        assertFalse(w.runEnded());
        assertTrue(w.ship.hp > 0);
    }
}

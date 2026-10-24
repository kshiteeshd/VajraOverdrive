package vajra.world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.ship.ShipControls;

/** The pilot must always be able to fly forward from the spawn, through the boulder belt's pass and on. */
class FlightTest {
    private static ShipControls up(float x) {
        return new ShipControls() {
            public float thrustY() { return 1; }
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

    @Test
    void straightUpFromTheSpawnClearsTheBoulderBelt() {
        World.walls = false;                     // (a straight line through the real caverns would hit rock: this is the open world)
        World w;
        try {
            w = new World();
        } finally {
            World.walls = true;
        }
        ShipControls c = up(0);
        float t446 = -1;
        for (int i = 0; i < 60 * 400; i++) {
            w.update(1 / 60f, c);
            if (w.ship.y > 446 && t446 < 0) t446 = w.clock;
            if (w.ship.y > 20300) break;
        }
        System.out.println("Flight: reached y=446 at clock " + t446);
        System.out.println("Flight: straight up reached y=" + w.ship.y + " x=" + w.ship.x);
        assertTrue(w.ship.y > 20000, "the hull flew on past the belt, reached y=" + w.ship.y + " x=" + w.ship.x);
    }
}

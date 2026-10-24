package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** How high the hull flies, place by place: far over the meadow and the dream-circle, near in the mountains, the mirror-wood
 *  and the mansion, and never a jump between. */
class AltitudeTest {
    @Test
    void theHullFliesFarInSomePlacesAndNearInOthers() {
        assertTrue(Altitude.fly(400) > 350, "high over the meadow: " + Altitude.fly(400));
        assertTrue(Altitude.fly(1700) < 140, "close to the rock in the pass: " + Altitude.fly(1700));
        assertTrue(Altitude.fly(6000) < 100, "low among the trees of the mirror-wood: " + Altitude.fly(6000));
        assertTrue(Altitude.fly(10000) > 350, "high over the dream-circle: " + Altitude.fly(10000));
        assertTrue(Altitude.fly(19000) < 110, "low in the mansion: " + Altitude.fly(19000));
        assertTrue(Altitude.ground(Altitude.fly(400)) < Altitude.ground(Altitude.fly(6000)),
                "the ground is smaller (further) from high up");
    }

    @Test
    void theAltitudeNeverJumps() {
        float last = Altitude.fly(0);
        for (float y = 0; y <= 20600; y += 10) {
            float f = Altitude.fly(y);
            assertTrue(Math.abs(f - last) < 12f, "a smooth climb and fall at y=" + y + ": " + last + " -> " + f);
            last = f;
        }
    }

    @Test
    void aPointProjectsAboveTheGroundAndLevelWithThePlane() {
        float fly = 130f;
        assertEquals(1f, Altitude.scale(fly, fly), 1e-6f, "the flight plane is the reference");
        assertTrue(Altitude.scale(0, fly) < 1f, "the ground is smaller");
        assertTrue(Altitude.scale(200, fly) > 1f, "a tall thing is nearer the camera than the plane");
    }
}

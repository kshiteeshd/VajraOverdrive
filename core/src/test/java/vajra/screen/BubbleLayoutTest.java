package vajra.screen;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.world.Missions;
import vajra.world.Speech;
import vajra.world.World;

/** No dialogue box may sit on anything: not on a body, not on a HUD panel, not on another box. The
 *  placement is pure, so it is pinned here without a screen. */
class BubbleLayoutTest {
    private static final float BW = 300, BH = 70;

    private static boolean overlaps(float[] box, float[] r, float margin) {
        return box[0] < r[2] + margin && box[0] + BW > r[0] - margin && box[1] < r[3] + margin && box[1] + BH > r[1] - margin;
    }

    @Test
    void withNothingInTheWayABubbleSitsOverItsSpeaker() {
        float[] p = Bubbles.place(BW, BH, 640, 300, 40, false, 20, 700, new float[0][]);
        assertEquals(640 - BW / 2, p[0], 0.01f);
        assertEquals(340, p[1], 0.01f);
        assertEquals(0f, p[2], "above: the flag says not under");
    }

    @Test
    void aBodyOverTheSpotMovesTheBubbleOff() {
        float[][] avoid = {{480, 330, 800, 420}}; // a hull standing right where the box would go
        float[] p = Bubbles.place(BW, BH, 640, 300, 40, false, 20, 700, avoid);
        assertFalse(overlaps(p, avoid[0], 6), "the box keeps clear of the body");
        assertTrue(p[0] >= 20 && p[0] + BW <= 1260 && p[1] >= 20 && p[1] + BH <= 700, "and stays on the screen");
    }

    @Test
    void aBubbleStaysNearItsSpeakerEvenWhenBodiesAreInTheWay() {
        float[][] bodies = {{480, 330, 800, 420}, {480, 200, 800, 280}, {330, 250, 470, 400}}; // all round the speaker
        float[] p = Bubbles.place(BW, BH, 640, 300, 40, false, 20, 700, new float[0][], bodies);
        float cx = p[0] + BW / 2, cy = p[1] + BH / 2;
        double dist = Math.hypot(cx - 640, cy - 300);
        assertTrue(dist < 230, "the box stays beside its speaker (" + Math.round(dist) + " px away), it does not roam");
    }

    @Test
    void aBubbleIsAlwaysWithinReachOfItsSpeakerWhenNothingBlocksIt() {
        for (float ax = 100; ax < 1200; ax += 220)
            for (float ay = 150; ay < 600; ay += 150) {
                float[] p = Bubbles.place(BW, BH, ax, ay, 40, false, 20, 700, new float[0][], new float[0][]);
                double d = Math.hypot(p[0] + BW / 2 - ax, p[1] + BH / 2 - ay);
                assertTrue(d < 260, "at " + ax + "," + ay + " the box is " + Math.round(d) + " px from its speaker");
            }
    }

    @Test
    void aHudPanelIsKeptClearOfToo() {
        World w = new World();
        w.missions.salvage = Missions.OPEN;
        w.toast("A", "B");
        float[] jobs = HudJobs.rect(w);
        assertNotNull(jobs, "an open job has a panel");
        float[][] avoid = HudZones.of(w).toArray(new float[0][]);
        // a speaker standing right under the job tracker: the box must not cover it
        float[] p = Bubbles.place(BW, BH, 140, jobs[1] - 30, 40, false, 20, 700, avoid);
        for (float[] z : avoid) assertFalse(overlaps(p, z, 6), "the box sits on a HUD zone");
    }

    @Test
    void twoNeighboursGetTwoBoxesThatNeverTouch() {
        float[][] first = {};
        float[] a = Bubbles.place(BW, BH, 600, 300, 40, false, 20, 700, first);
        float[][] avoid = {{a[0], a[1], a[0] + BW, a[1] + BH}};
        float[] b = Bubbles.place(BW, BH, 640, 300, 40, false, 20, 700, avoid);
        assertFalse(overlaps(b, avoid[0], 6), "the second box keeps off the first");
    }

    @Test
    void aCrowdAtTheEdgeStillFindsRoomOnTheScreen() {
        float[][] avoid = {{0, 200, 400, 340}, {0, 340, 400, 420}};
        float[] p = Bubbles.place(BW, BH, 30, 250, 40, false, 20, 700, avoid);
        assertTrue(p[0] >= 20 && p[0] + BW <= 1260, "never off the sides");
        assertTrue(p[1] >= 20 && p[1] + BH <= 700, "never off the top or bottom");
        for (float[] z : avoid) assertFalse(overlaps(p, z, 6));
    }

    @Test
    void theBubbleStaysInsideTheBandItIsGiven() {
        float[] p = Bubbles.place(BW, BH, 640, 600, 40, false, 100, 640, new float[0][]); // no room above: it goes under
        assertTrue(p[1] >= 100 && p[1] + BH <= 640);
        assertEquals(1f, p[2], "and the flag says it is now under its speaker");
    }

    @Test
    void aPersonSpeaksOneBoxAtATime() {
        World w = new World();
        Enemy maya = w.voices.get(1), scout = w.voices.get(0);
        w.say(maya, "ONE.");
        w.say(maya, "TWO.");
        w.say(scout, "THREE.");
        List<Speech> heads = w.heads();
        assertEquals(2, heads.size(), "Maya and the Scout each have a box, not three");
        assertEquals("ONE.", heads.get(0).full());
        for (int i = 0; i < 400; i++) w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertTrue(w.speech.isEmpty(), "and the queue plays out in turn");
    }

    @Test
    void theZonesNameWhereTheHudLives() {
        World w = new World();
        List<float[]> zones = HudZones.of(w);
        assertTrue(zones.size() >= 4, "bars, score, compass, tape");
        for (float[] z : zones) assertTrue(z[0] < z[2] && z[1] < z[3], "each zone is a real rectangle");
        w.toast("T", "L");
        assertEquals(zones.size() + 1, HudZones.of(w).size(), "a toast adds its own");
    }
}

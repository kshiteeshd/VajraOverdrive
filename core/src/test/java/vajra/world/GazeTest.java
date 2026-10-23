package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;

/** The ruler's gaze: it reads the quota, it speaks in three stages, and it never repeats. */
class GazeTest {
    private static void fill(World w, float frac) {
        for (int i = 0; i < w.quota.length; i++) w.kills[i] = Math.round(w.quota[i] * frac);
    }

    private static java.util.List<Cue> drain(World w) {
        java.util.List<Cue> out = new java.util.ArrayList<>(w.cues);
        w.cues.clear();
        return out;
    }

    @Test
    void aFreshRunIsUnwatched() {
        World w = new World();
        assertEquals(0f, w.gaze(), 1e-6);
        assertEquals(0, w.gazeStage);
    }

    @Test
    void theGazeIsTheShareOfTheQuotaAlreadyPaid() {
        World w = new World();
        fill(w, 0.5f);
        assertTrue(w.gaze() > 0.4f && w.gaze() < 0.6f);
        for (int i = 0; i < w.kills.length; i++) w.kills[i] += 50; // a flood of one easy kind adds nothing past its cap
        assertEquals(1f, w.gaze(), 1e-6, "kills past the quota are capped per kind");
    }

    @Test
    void theThreeStagesFireOnceAndInOrder() {
        World w = new World();
        drain(w);
        fill(w, 0.3f);
        w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertEquals(java.util.List.of(Cue.GAZE_NOTICED), drain(w));
        w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertTrue(drain(w).isEmpty(), "a stage never repeats");
        fill(w, 0.7f);
        w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertEquals(java.util.List.of(Cue.GAZE_WATCHING), drain(w));
        fill(w, 1f);
        w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertNotNull(w.boss, "the quota lifts the seal");
        assertTrue(drain(w).contains(Cue.GAZE_ROUSED));
        assertEquals(3, w.gazeStage);
    }

    @Test
    void aBurstOfKillsPlaysOnlyTheStageItLandedOn() {
        World w = new World();
        drain(w);
        fill(w, 0.7f);
        w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertEquals(java.util.List.of(Cue.GAZE_WATCHING), drain(w), "the notice is skipped, not queued behind");
    }

    @Test
    void theKeepersDoNotCountAgainstTheRuler() {
        World w = new World();
        for (Enemy.Kind k : Enemy.Kind.values())
            if (k.keeper()) assertEquals(0, w.quota[k.ordinal()], k + " holds a post, it is not in the quota");
    }

    @Test
    void theGateGuardsAreFourAndWaitForTheTalk() {
        World w = new World();
        int held = 0;
        for (Enemy e : w.enemies) if (e.parley) held++;
        assertEquals(4, held);
    }
}

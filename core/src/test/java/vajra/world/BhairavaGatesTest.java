package vajra.world;

import vajra.enemy.BhairavaBrain;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Bhairava's four gated phases: no blow skips one, none can kill him outright, the last ends in a yield. */
class BhairavaGatesTest {
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

    private static World fight() {
        for (Arena.Scenario s : Arena.in(Arena.BOSSES))
            if (s.name.equals("BHAIRAVA: THE FULL FIGHT")) {
                World w = Arena.build(s);
                w.ship.invuln = 1e6f;
                return w;
            }
        throw new AssertionError("no scenario");
    }

    private static void run(World w, float s) {
        for (int i = 0; i < (int) (s / 0.016f); i++) w.update(0.016f, IDLE);
    }

    @Test
    void aBlowCanTakeOnlyASixthOfATenthAndNeverSkipsAGate() {
        World w = fight();
        run(w, 1f);
        Enemy b = w.boss;
        assertNotNull(b);
        assertEquals(1, b.bossPhase);
        float before = b.hp;
        b.hurt(1_000_000f, w);
        assertEquals(before - b.maxHp * BhairavaBrain.HIT_CAP, b.hp, 1f, "a single blow is capped");
        for (int i = 0; i < 3000 && !b.bossInvuln; i++) { b.hurt(1_000_000f, w); w.update(0.016f, IDLE); }
        assertFalse(b.dead);
        assertTrue(b.hp >= b.maxHp * 0.75f - 1f, "the hull stops at the first gate: " + b.hp / b.maxHp);
        assertTrue(b.bossInvuln, "and the phase turns while he cannot be touched");
    }

    @Test
    void thePhasesTurnInOrderAndTheLastEndsInAYield() {
        World w = fight();
        int last = 1;
        float t = 0f;
        for (int i = 0; i < 20000 && w.yielder == null && !w.boss.yielding; i++) {
            if (!w.boss.bossInvuln) w.boss.hurt(1_000_000f, w);
            w.update(0.016f, IDLE);
            t += 0.016f;
            int ph = w.boss.bossPhase;
            assertTrue(ph == last || ph == last + 1, "no phase is skipped: " + last + " -> " + ph);
            last = ph;
        }
        assertEquals(4, last, "all four phases were fought");
        assertTrue(w.boss.yielding || w.yielder != null, "the fight ended in a yield, not a death");
        assertTrue(t > 12f, "and a burst cannot empty him: " + t + " s");
    }

    @Test
    void aFightThatBeginsMidWayTakesItsPhaseFromTheHull() {
        for (Arena.Scenario s : Arena.in(Arena.BOSSES)) {
            if (!s.name.startsWith("BHAIRAVA: PHASE")) continue;
            World w = Arena.build(s);
            run(w, 0.5f);
            int want = s.name.contains("TWO") ? 2 : s.name.contains("THREE") ? 3 : 4;
            assertEquals(want, w.boss.bossPhase, s.name);
        }
    }
}

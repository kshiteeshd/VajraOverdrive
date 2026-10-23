package vajra.coach;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.profile.Profile;
import vajra.world.World;

/** The first flight and the primer sheet, headless. */
class CoachTest {
    @Test
    void aNewPilotCannotFireUntilTheyHaveFlown() {
        Coach c = new Coach(false, false);
        assertEquals(Coach.Stage.THRUST, c.stage());
        assertFalse(c.canFire());
        c.update(1, 2600, 90, true); // mashing fire at the start proves nothing
        assertEquals(Coach.Stage.THRUST, c.stage());
        assertEquals(Lessons.THRUST, c.update(0.1f, 2600, 90 + Coach.FLY_FAR + 1, false));
        assertEquals(Coach.Stage.FIRE, c.stage());
        assertFalse(c.canFire(), "flown, but the trigger is still handed over only by pressing it");
    }

    @Test
    void holdingFireFreesTheControls() {
        Coach c = new Coach(true, false);
        assertEquals(Coach.Stage.FIRE, c.stage());
        c.update(Coach.FIRE_HOLD / 2, 0, 0, true);
        assertEquals(-1, c.update(0.01f, 0, 0, false), "released early, still waiting");
        assertEquals(Lessons.FIRE, c.update(Coach.FIRE_HOLD, 0, 0, true));
        assertTrue(c.canFire());
        assertEquals(-1, c.lesson());
    }

    @Test
    void aTaughtPilotIsNeverHeld() {
        Coach c = new Coach(true, true);
        assertTrue(c.canFire());
        assertEquals(-1, c.lesson());
    }

    @Test
    void theSheetIsWholeAndEveryGroupHasLessons() {
        for (int i = 0; i < Lessons.COUNT; i++) {
            Lesson l = Lessons.ALL[i];
            assertNotNull(l, "lesson " + i);
            assertEquals(i, l.id);
            assertFalse(l.title.isBlank());
            assertTrue(l.body.length >= 1 && l.body.length <= 4, l.title);
            assertFalse(l.when.isBlank());
        }
        for (String g : Lessons.GROUPS) assertFalse(Lessons.inGroup(g).isEmpty(), g);
    }

    @Test
    void lessonsLearnedSurviveTheSave() {
        Profile p = new Profile();
        p.learn(Lessons.FIRE);
        Profile q = new Profile();
        Profile.deserialize(q, Profile.serialize(p));
        assertTrue(q.learned(Lessons.FIRE));
        assertFalse(q.learned(Lessons.THRUST));
    }

    @Test
    void aFreshRunWakesLowAndStraightAndQuiet() {
        World w = new World();
        assertEquals(World.SPAWN_Y, w.ship.y, 0.01f);
        assertEquals(Math.PI / 2, w.ship.facing, 1e-5);
        assertEquals(0, w.ship.vx, 0);
        assertEquals(0, w.popups.activeCount(), "no codex note over the first frame");
        assertTrue(w.codex[vajra.world.Codex.ATALA], "the pages are still lifted");
    }
}

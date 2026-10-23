package vajra.scene;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import vajra.world.Cue;
import vajra.world.World;

/** The cutscene clock and its scripts, headless: the camera lands on the subject, the line
 *  types, a press completes it and then moves on, and the way out hands the camera back. */
class CutsceneTest {
    private static CutscenePlayer two() {
        Shot a = new Shot(100, 200, 0.5f, "PILOT", true, "ABCDEFGHIJ", "SECOND");
        Shot b = new Shot(900, 800, 0.7f, "GATE", false, "OPEN");
        return new CutscenePlayer(List.of(a, b), 0, 0, 1f);
    }

    private static void run(CutscenePlayer p, float secs) {
        for (float t = 0; t < secs; t += 1 / 60f) p.advance(1 / 60f, 0, 0, 1f);
    }

    @Test
    void cameraLandsOnTheSubjectBeforeAnyTextAppears() {
        CutscenePlayer p = two();
        run(p, CutscenePlayer.MOVE / 2);
        assertEquals("", p.text());
        assertTrue(p.camZoom < 1f && p.camZoom > 0.5f, "mid-move zoom is between the two");
        run(p, CutscenePlayer.MOVE);
        assertEquals(100, p.camX, 0.5f);
        assertEquals(200, p.camY, 0.5f);
        assertEquals(0.5f, p.camZoom, 0.01f);
    }

    @Test
    void pressCompletesTheLineThenAdvancesThenLeaves() {
        CutscenePlayer p = two();
        run(p, CutscenePlayer.MOVE + 0.1f);
        assertFalse(p.lineComplete());
        p.press();
        assertEquals("ABCDEFGHIJ", p.text());
        assertTrue(p.lineComplete());
        p.press();
        assertEquals("", p.text(), "the next line starts empty");
        run(p, 1);
        assertEquals("SECOND", p.text());
        p.press();
        p.press(); // into shot two
        run(p, CutscenePlayer.MOVE + 1);
        assertEquals(900, p.camX, 0.5f);
        assertFalse(p.shot().player, "anyone else speaks from the top");
        p.press();
        p.press();
        run(p, CutscenePlayer.OUTRO + 0.1f);
        assertTrue(p.done());
        assertEquals(1f, p.camZoom, 0.001f, "the camera is handed back to play");
    }

    @Test
    void escapeSkipsStraightToTheWayBack() {
        CutscenePlayer p = two();
        run(p, 1);
        p.skip();
        run(p, CutscenePlayer.OUTRO + 0.1f);
        assertTrue(p.done());
    }

    @Test
    void pressDuringTheCameraMoveDoesNothing() {
        CutscenePlayer p = two();
        run(p, 0.2f);
        p.press();
        run(p, CutscenePlayer.MOVE);
        assertTrue(p.text().length() < 10, "the line still types from the start");
    }

    @Test
    void theOpeningIsTheNarratorThenIndrasThought() {
        World w = new World();
        assertEquals(Cue.OPENING, w.cues.poll(), "a fresh run raises the opening");
        List<Shot> s = Scenes.build(Cue.OPENING, w);
        assertEquals(3, s.size());
        assertTrue(s.get(0).narrator() && s.get(1).narrator(), "the narrator opens, in the band");
        assertTrue(s.get(2).player && "INDRA".equals(s.get(2).speaker), "then Indra thinks aloud");
        assertTrue(s.get(2).y > w.ship.y, "the camera sits above the pilot so the bubble has sky");
    }

    @Test
    void everyCueHasAScript() {
        World w = new World();
        for (Cue c : Cue.values())
            if (c != Cue.BALA_WAKES && c != Cue.BALA_FALLS)
                assertFalse(Scenes.build(c, w).isEmpty(), c.name());
    }

    @Test
    void theGateGuardsHoldTheirFireUntilTheTalkingIsOver() {
        World w = new World();
        w.cues.clear();
        java.util.List<vajra.enemy.Enemy> guards = new java.util.ArrayList<>();
        for (vajra.enemy.Enemy e : w.enemies) if (e.parley) guards.add(e);
        assertEquals(4, guards.size(), "two lances and two hatakas hold the grand gate");
        w.ship.x = 2600;
        w.ship.y = 1700; // right under their guns
        for (int i = 0; i < 300; i++) for (vajra.enemy.Enemy e : guards) e.update(1 / 60f, w, i / 60f);
        assertEquals(0, w.bullets.size(), "not a round while they are parleying");
        assertFalse(guards.get(0).active);

        Scenes.finish(Cue.ATALA_GATE, w);
        for (vajra.enemy.Enemy e : guards) {
            assertFalse(e.parley);
            assertTrue(e.active, "after the talk they fight");
        }
    }

    @Test
    void theParleyOpensWithTheNarratorThenGuardsAndPilotTakeTurns() {
        World w = new World();
        w.ship.y = 1700;
        List<Shot> s = Scenes.build(Cue.ATALA_GATE, w);
        assertTrue(s.get(0).narrator(), "the narrator needs no box");
        assertEquals(4, s.get(0).gather.length, "all four guards close on the pilot");
        boolean guard = false, pilot = false;
        for (Shot x : s.subList(1, s.size())) {
            assertFalse(x.narrator());
            if (x.player) pilot = true; else guard = x.who != null;
        }
        assertTrue(guard && pilot);
    }

    @Test
    void aShotInTheSameFramingDoesNotWaitOnACameraMove() {
        Shot a = new Shot(500, 500, 0.8f, "A", false, "ONE");
        Shot b = new Shot(500, 500, 0.8f, "B", false, "TWO");
        CutscenePlayer p = new CutscenePlayer(List.of(a, b), 500, 500, 0.8f);
        run(p, 0.01f);
        run(p, 1);
        p.press();
        p.press();
        p.advance(0.2f, 0, 0, 1f);
        assertFalse(p.text().isEmpty(), "the second line starts at once");
    }
}

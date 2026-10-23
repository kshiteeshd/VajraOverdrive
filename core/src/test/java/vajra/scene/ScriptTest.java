package vajra.scene;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.world.Chatter;
import vajra.world.Codex;
import vajra.world.Cue;
import vajra.world.Speech;
import vajra.world.Toast;
import vajra.world.World;

/** The script, the people and the jobs: lines stay short and in the story's words, a first meeting is
 *  a scene and everything after it is a bubble or a toast, and nothing floats over the hull any more. */
class ScriptTest {
    private static World met(String name) {
        World w = new World();
        w.cues.clear();
        Enemy v = Chatter.person(w, name);
        w.ship.x = v.x + 40;
        w.ship.y = v.y;
        return w;
    }

    private static List<String> lines(World w, Cue c) {
        List<String> out = new ArrayList<>();
        for (Shot s : Scenes.build(c, w)) for (String l : s.lines) out.add(l);
        return out;
    }

    @Test
    void everyLineIsShortUppercaseAndHasSomethingToSay() {
        World w = new World();
        w.cues.clear();
        w.cue(Cue.BALA_WAKES);
        for (Cue c : Cue.values()) {
            World x = new World();
            x.cues.clear();
            x.ship.x = 2600;
            x.ship.y = 1700;
            if (c == Cue.BALA_WAKES || c == Cue.BALA_FALLS || c == Cue.GAZE_ROUSED) {
                x.enemies.add(new Enemy(Enemy.Kind.BALA, World.PIT_X, World.PIT_Y));
                x.boss = x.enemies.get(x.enemies.size() - 1);
            }
            for (String l : lines(x, c)) {
                assertTrue(l.length() <= 96, c + ": too long for a breath (" + l.length() + "): " + l);
                assertEquals(l.toUpperCase(), l, c + ": the page is set in capitals");
                assertFalse(l.isBlank());
            }
        }
    }

    @Test
    void theOpeningSpeaksOfIndraSwargaAndTheBolt() {
        String all = String.join(" ", lines(new World(), Cue.OPENING));
        for (String word : new String[] {"SWARGA", "VAJRAYUDHA", "TRIVIKRAMA", "DADHICHI", "INDRA"})
            assertTrue(all.contains(word), "the opening names " + word);
    }

    @Test
    void theGateIsAConversationInTheStoryNotAGenericChallenge() {
        World w = new World();
        w.cues.clear();
        w.ship.x = 2600;
        w.ship.y = 1700;
        String all = String.join(" ", lines(w, Cue.ATALA_GATE));
        assertTrue(all.contains("BALA") && all.contains("INDRA") && all.contains("SWARGA"));
        assertTrue(all.contains("WEAPONS FREE"), "and it ends the way the guards' job ends");
    }

    @Test
    void eachPersonGetsAFirstMeetingSceneInTheirOwnWords() {
        String scout = String.join(" ", lines(met("SCOUT"), Cue.VOICE_SCOUT));
        assertTrue(scout.contains("UPARICHARA") && scout.contains("BEACON"));
        String maya = String.join(" ", lines(met("MAYA"), Cue.VOICE_MAYA));
        assertTrue(maya.contains("MAYA") && maya.contains("HAMMER") && maya.contains("BOLT"));
        String kaliya = String.join(" ", lines(met("KALIYA"), Cue.VOICE_KALIYA));
        assertTrue(kaliya.contains("KALIYA") && kaliya.contains("TAKSHAKA") && kaliya.contains("GARUDA"));
    }

    @Test
    void aPersonIsMetOnceAsASceneAndHoldsTheirJob() {
        World w = met("MAYA");
        Chatter.update(w);
        assertEquals(Cue.VOICE_MAYA, w.cues.poll(), "the first sight raises the meeting scene");
        assertTrue(Chatter.person(w, "MAYA").voiceMet);
        assertEquals(vajra.world.Missions.OPEN, w.missions.salvage, "and her job opens");
        assertTrue(w.codex[Codex.MAYA]);
        Chatter.update(w);
        assertTrue(w.cues.isEmpty(), "only once");
        assertEquals(0, w.popups.activeCount(), "nothing floats over the hull");
    }

    @Test
    void theScoutsBeaconSpeaksThroughABubbleAndAToast() {
        World w = met("SCOUT");
        Chatter.update(w);
        Chatter.beaconRaised(w);
        assertFalse(w.speech.isEmpty(), "he answers over his hull, while the world moves");
        assertFalse(w.toasts.isEmpty(), "and the HUD says what it pays");
        assertEquals(vajra.world.Missions.OPEN, w.missions.bounty, "the brokers' card goes up");
    }

    @Test
    void anUnmetPersonPaysNothing() {
        World w = new World();
        w.cues.clear();
        Chatter.beaconRaised(w);
        assertTrue(w.speech.isEmpty() && w.toasts.isEmpty(), "every other cache stays silent");
        assertEquals(vajra.world.Missions.NONE, w.missions.bounty);
    }

    @Test
    void aSpeechTypesHoldsAndEndsLineByLine() {
        World w = new World();
        Enemy v = w.voices.get(0);
        Speech s = new Speech(v, "ONE.", "TWO.");
        assertEquals("", s.typed());
        s.update(0.05f);
        assertTrue(s.typed().length() >= 1 && !s.done());
        s.update(s.lineSeconds());
        assertEquals("TWO.", s.full(), "the second line follows the first");
        s.update(s.lineSeconds() + 0.1f);
        assertTrue(s.done());
        assertEquals(0f, s.alpha());
    }

    @Test
    void aToastSlidesInHoldsAndGoes() {
        Toast t = new Toast("A", "B");
        assertEquals(0f, t.k(), 1e-6);
        t.age = Toast.LIFE / 2;
        assertEquals(1f, t.k(), 1e-6);
        t.age = Toast.LIFE;
        assertTrue(t.done());
    }

    @Test
    void wordsAreTickedByTheWorld() {
        World w = new World();
        w.cues.clear();
        w.say(w.voices.get(0), "HELLO.");
        w.toast("T", "L");
        for (int i = 0; i < 60 * 8; i++) w.update(1 / 60f, vajra.ship.ShipControls.NONE);
        assertTrue(w.speech.isEmpty() && w.toasts.isEmpty(), "both end on their own clocks");
    }

    @Test
    void theLetterIsReadOnlyWhenMayaWasMet() {
        World w = new World();
        w.cues.clear();
        w.enemies.add(new Enemy(Enemy.Kind.BALA, World.PIT_X, World.PIT_Y));
        w.boss = w.enemies.get(w.enemies.size() - 1);
        assertFalse(String.join(" ", lines(w, Cue.BALA_FALLS)).contains("LETTER"), "unmet: no letter");
        Chatter.person(w, "MAYA").voiceMet = true;
        assertTrue(String.join(" ", lines(w, Cue.BALA_FALLS)).contains("LETTER"), "met: the letter comes up from her woods");
    }
}

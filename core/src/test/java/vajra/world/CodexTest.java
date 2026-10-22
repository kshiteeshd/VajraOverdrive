package vajra.world;

import org.junit.jupiter.api.Test;
import vajra.profile.Profile;
import static org.junit.jupiter.api.Assertions.*;

/** THE CODEX: the run opens its pages, the world opens the rest by event, and the fold
 *  keeps what any run gave — a page unlocked is a page unlocked for the pilot. */
class CodexTest {

    @Test
    void theSheetAndTheRunAgreeOnTwelvePages() {
        assertEquals(Codex.COUNT, Codex.TITLE.length, "every page has a title");
        assertEquals(Codex.COUNT, Codex.BODY.length, "and a body");
        World w = new World();
        assertTrue(w.codex[Codex.ATALA], "the pilot's ground is the first page");
        assertTrue(w.codex[Codex.VAJRA], "and what he carries");
        for (int i = 2; i < Codex.COUNT; i++)
            assertFalse(w.codex[i], "everything else waits for the world to speak");
    }

    @Test
    void aLiftSaidTwiceIsOnePageAndTheFoldNeverErases() {
        World w = new World();
        Codex.unlock(w, Codex.ROGUE);
        Codex.unlock(w, Codex.ROGUE);
        assertTrue(w.codex[Codex.ROGUE], "one page, however often it's said");
        assertTrue(w.codex[Codex.VAJRA], "the run's own pages never wobble");
        Profile p = new Profile();
        assertNull(p.codex, "a fresh pilot has no book yet");
        p.recordCodex(w.codex);
        assertTrue(p.codex[Codex.ROGUE], "taken pages fold in");
        boolean[] later = new boolean[Codex.COUNT];
        later[Codex.SCOUT] = true;
        p.recordCodex(later); // a later run adds its page — and erases nothing
        assertTrue(p.codex[Codex.SCOUT], "the later run's page rides in");
        assertTrue(p.codex[Codex.VAJRA], "and the first run's staying was kept");
    }

    @Test
    void theBookRidesTheSave() {
        Profile p = new Profile();
        p.recordCodex(new boolean[] {true, false, false, true});
        Profile back = new Profile();
        Profile.deserialize(back, Profile.serialize(p));
        assertArrayEquals(new boolean[] {true, false, false, true}, back.codex,
                "pure serialization roundtrips the book");
    }
}

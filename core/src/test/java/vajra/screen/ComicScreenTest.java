package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

class ComicScreenTest {
    @Test
    void aTallPanelIsLetterboxedNotStretched() {
        float[] r = ComicScreen.fit(1000, 1500, 1280, 720);
        assertEquals(1000f / 1500f, r[2] / r[3], 1e-4f, "aspect kept");
        assertEquals(720f, r[3], 1e-3f, "height-bound");
        assertEquals((1280f - r[2]) / 2f, r[0], 1e-3f, "centred");
    }

    @Test
    void aWidePanelFitsTheWidth() {
        float[] r = ComicScreen.fit(3000, 1000, 1280, 720);
        assertEquals(1280f, r[2], 1e-3f);
        assertTrue(r[3] < 720f);
    }

    @Test
    void aPanelFadesInThenHoldsAndPushesIn() {
        assertEquals(0f, ComicScreen.alpha(0f));
        assertEquals(1f, ComicScreen.alpha(ComicScreen.FADE));
        assertEquals(0.94f, ComicScreen.scale(0f), 1e-4f);
        assertEquals(1f, ComicScreen.scale(ComicScreen.PUSH * 2), 1e-4f);
    }

    @Test
    void theNarratorTypesSlowlyAndFinishes() {
        String s = "THE THIRD STEP FELL ON SWARGA.";
        assertEquals("", ComicScreen.typed(s, 0f));
        assertTrue(ComicScreen.typed(s, 1.0f).length() > 0 && ComicScreen.typed(s, 1.0f).length() < s.length());
        assertEquals(s, ComicScreen.typed(s, ComicScreen.typeSeconds(s) + 0.01f));
    }

    @Test
    void everyPanelHasArtAndAShortCapitalLine() {
        File dir = new File("../assets"); // the core module runs from core/
        assertTrue(StoryScript.PANELS.length >= 18);
        for (StoryScript.Panel p : StoryScript.PANELS) {
            assertTrue(new File(dir, p.file).exists(), "art missing: " + p.file);
            assertEquals(p.text.toUpperCase(), p.text, "lines are set in capitals: " + p.text);
            assertTrue(p.text.length() <= 100, "a narrator line stays short: " + p.text);
        }
        assertEquals(3, StoryScript.pages());
    }
}

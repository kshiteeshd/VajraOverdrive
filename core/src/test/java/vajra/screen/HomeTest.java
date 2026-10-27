package vajra.screen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The home screen is a plain menu: the only logic worth pinning is the wrapping cursor and
 *  that the four rows stay in the order the test expects.
 *
 *  <p>Plus its vertical budget. The logo is drawn 2x and everything below it was squeezed to
 *  make room; the first attempt put QUIT straight through the footer hint and nothing but a
 *  rendered frame catches that. These are the numbers, asserted against each other. */
class HomeTest {
    private static final float FRAME_TOP = Ui.UI_H - 28;  // the hairline frame's top edge
    private static final float HINT_TOP = UiTheme.FOOT_Y + 14;

    @Test
    void theMenuCursorWrapsBothWays() {
        assertEquals(1, HomeScreen.move(0, 1));
        assertEquals(3, HomeScreen.move(0, -1), "up from the top wraps to EXIT");
        assertEquals(0, HomeScreen.move(3, 1), "down from the bottom wraps to the top");
    }

    @Test
    void withNoSaveTheRowsAreNewGameMultiplayerAndTheRest() {
        assertArrayEquals(
                new String[] {"NEW GAME", "MULTIPLAYER", "SETTINGS", "EXIT"},
                HomeScreen.rows(false));
    }

    @Test
    void withASavedRunContinueLeadsAndNewGameFollows() {
        String[] r = HomeScreen.rows(true);
        assertEquals("CONTINUE", r[0]);
        assertEquals("NEW GAME", r[1]);
        assertEquals(HomeScreen.MENU.length + 1, r.length);
    }

    /** The logo is 2x: the 500x300 asset drawn at 800x480. It carries heavy transparent padding
     *  (ink is only 330x149 of the 500x300), so the drawn box is far taller than the visible
     *  text - which is the point: the ink lands at 526x238, exactly twice the old 263x119. */
    @Test
    void theLogoIsDrawnAt2x() {
        assertEquals(800, HomeScreen.LOGO_W, "the asset's 500 wide, doubled");
        assertEquals(480, HomeScreen.LOGO_H, "its 300 tall, doubled");
    }

    /** The bottom row must clear the footer hint. This is the collision that actually happened. */
    @Test
    void theMenuClearsTheFooterHint() {
        float bottom = HomeScreen.menuY() - 4 * HomeScreen.pitch(); // the bottom of five rows (with CONTINUE)
        assertTrue(bottom - 14 > HINT_TOP + 10,
                "QUIT at y" + bottom + " runs into the hint at y" + HINT_TOP);
    }

    /** And the menu's top row clears the divider under the logo (the new row squeezed the pitch). */
    @Test
    void theMenuClearsTheDivider() {
        assertTrue(HomeScreen.menuY() + 22 < HomeScreen.dividerY(),
                "the first row's caps run into the divider at y" + HomeScreen.dividerY());
    }

    /** A row's click zone must reach the next row's: no dead strips between rows at pitch 40. */
    @Test
    void theRowsAbutWithNoDeadZone() {
        assertEquals(HomeScreen.pitch(), 2 * HomeScreen.halfH(),
                "double the hit half-height must cover the pitch exactly");
    }
}
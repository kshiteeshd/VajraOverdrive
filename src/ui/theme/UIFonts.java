package ui.theme;

import java.awt.*;

/**
 * Central font registry for the entire game.
 *
 * Press Start 2P is a bitmap pixel font — it only renders crisply
 * at multiples of its base grid (8px). All sizes here are strictly
 * multiples of 8 to avoid blurry fractional scaling.
 *
 *   TITLE     32px — screen headings, level names
 *   MENU      16px — menu items, primary HUD values
 *   BODY      16px — story / intro text (same size, different usage)
 *   SMALL      8px — HUD labels, footers, sub-labels
 *   HUD        8px — alias for SMALL, used in HUD panels
 *   HUD_LARGE 24px — large accent values (enemy count, boss phase)
 */
public class UIFonts {

    public static Font TITLE;      // 32px
    public static Font MENU;       // 16px
    public static Font BODY;       // 16px
    public static Font SMALL;      //  8px
    public static Font HUD;        //  8px  (alias for SMALL)
    public static Font HUD_LARGE;  // 24px  (enemy count, large accents)

    public static void load() {
        TITLE     = FontLoader.loadFont("fonts/press_start.ttf", 32f);
        MENU      = FontLoader.loadFont("fonts/press_start.ttf", 16f);
        BODY      = FontLoader.loadFont("fonts/press_start.ttf", 16f);
        SMALL     = FontLoader.loadFont("fonts/press_start.ttf",  8f);
        HUD       = SMALL;
        HUD_LARGE = FontLoader.loadFont("fonts/press_start.ttf", 24f);
    }
}
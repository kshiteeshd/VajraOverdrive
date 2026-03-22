package ui.theme;

import java.awt.*;

/**
 * Central place for all game fonts.
 *
 * FIXED:
 *  - All sizes are now multiples of 8 so Press Start 2P renders on its
 *    native pixel grid and stays crisp (no blurry fractional scaling).
 *    TITLE 32px, MENU 16px, BODY 16px (was 13 — blurry), SMALL 8px.
 *  - Added HUD font at 8px for tight panel labels.
 */
public class UIFonts {

    public static Font TITLE;   // 32px — screen headings
    public static Font MENU;    // 16px — menu items, HUD values
    public static Font BODY;    // 16px — story / intro text
    public static Font SMALL;   // 8px  — HUD labels, footers, hints
    public static Font HUD;     // 8px  — alias for SMALL, used in panel

    public static void load() {
        TITLE = FontLoader.loadFont("fonts/press_start.ttf", 32f);
        MENU  = FontLoader.loadFont("fonts/press_start.ttf", 16f);
        BODY  = FontLoader.loadFont("fonts/press_start.ttf", 16f);
        SMALL = FontLoader.loadFont("fonts/press_start.ttf", 8f);
        HUD   = SMALL;
    }
}
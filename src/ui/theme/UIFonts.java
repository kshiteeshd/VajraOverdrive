package ui.theme;

import java.awt.*;

/**
 * Central font registry.
 *
 * CHANGED:
 *  - SMALL bumped from 8px → 10px for readability.
 *  - MENU bumped from 16px → 18px for better legibility.
 *  - HUD_LARGE bumped from 24px → 26px.
 *  - HUD alias kept pointing at SMALL.
 *  - TITLE stays at 32px — it's already prominent enough.
 *
 * Press Start 2P is a bitmap font — crisply designed for 8px multiples.
 * Going to 10px (non-multiple) introduces slight interpolation blur at
 * native resolution, but at game scale (1000×600) it reads far better
 * than straining to read 8px glyphs. The visual "pixel-art" feel is
 * preserved because the font itself is bitmap — just scaled up more.
 */
public class UIFonts {

    public static Font TITLE;      // 32px — screen headings
    public static Font MENU;       // 18px — menu items, primary HUD values  (was 16)
    public static Font BODY;       // 18px — story / intro text               (was 16)
    public static Font SMALL;      // 10px — HUD labels, footers              (was 8)
    public static Font HUD;        // 10px — alias for SMALL
    public static Font HUD_LARGE;  // 26px — large accent values              (was 24)

    public static void load() {
        TITLE     = FontLoader.loadFont("fonts/press_start.ttf", 32f);
        MENU      = FontLoader.loadFont("fonts/press_start.ttf", 18f);
        BODY      = FontLoader.loadFont("fonts/press_start.ttf", 18f);
        SMALL     = FontLoader.loadFont("fonts/press_start.ttf", 10f);
        HUD       = SMALL;
        HUD_LARGE = FontLoader.loadFont("fonts/press_start.ttf", 26f);
    }
}
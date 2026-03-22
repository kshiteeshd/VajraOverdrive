package config;

/**
 * BATCH 4 CHANGE:
 * Combat area is now the full virtual canvas — no side panel.
 * INFO_PANEL_WIDTH set to 0. GAME_AREA_WIDTH = full 1000px.
 * GAME_AREA_X = 0 (game renders from left edge).
 *
 * The old 200px left panel is replaced by the floating NebulaHUD
 * which renders as an overlay on top of the game area.
 */
public class LayoutConfig {

    // Fixed virtual canvas — never changes
    public static final int VIRTUAL_WIDTH  = 1000;
    public static final int VIRTUAL_HEIGHT = 600;

    // CHANGED: full width combat, no panel
    public static final int INFO_PANEL_WIDTH = 0;
    public static final int GAME_AREA_WIDTH  = VIRTUAL_WIDTH;   // 1000
    public static final int GAME_AREA_HEIGHT = VIRTUAL_HEIGHT;  // 600
    public static final int GAME_AREA_X      = 0;

    public static final int DEFAULT_WIDTH  = 1000;
    public static final int DEFAULT_HEIGHT = 600;

    private static int windowWidth  = DEFAULT_WIDTH;
    private static int windowHeight = DEFAULT_HEIGHT;

    public static int WINDOW_WIDTH  = DEFAULT_WIDTH;
    public static int WINDOW_HEIGHT = DEFAULT_HEIGHT;

    public static int getWindowWidth()  { return windowWidth;  }
    public static int getWindowHeight() { return windowHeight; }

    public static void setResolution(int w, int h) {
        windowWidth   = w;
        windowHeight  = h;
        WINDOW_WIDTH  = w;
        WINDOW_HEIGHT = h;
    }
}
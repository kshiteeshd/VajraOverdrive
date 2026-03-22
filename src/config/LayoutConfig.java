package config;

/**
 * Layout constants for window and UI panel.
 *
 * CHANGED:
 *  - WINDOW_WIDTH / WINDOW_HEIGHT are now mutable via setResolution().
 *  - DEFAULT_WIDTH / DEFAULT_HEIGHT hold the original 1000x600 values.
 *  - VIRTUAL_WIDTH / VIRTUAL_HEIGHT stay fixed at 1000x600 always —
 *    the game renders to a fixed virtual canvas and GameCanvas scales it
 *    to fit whatever the real window size is. This means resolution
 *    settings just change the window size, not the game logic.
 *  - GAME_AREA_WIDTH / GAME_AREA_HEIGHT derived from virtual canvas.
 */
public class LayoutConfig {

    // ── Fixed virtual canvas — never changes ──────────────────────────
    public static final int VIRTUAL_WIDTH  = 1000;
    public static final int VIRTUAL_HEIGHT = 600;

    // ── Panel layout — fixed relative to virtual canvas ───────────────
    public static final int INFO_PANEL_WIDTH = 200;
    public static final int GAME_AREA_WIDTH  = VIRTUAL_WIDTH - INFO_PANEL_WIDTH;  // 800
    public static final int GAME_AREA_HEIGHT = VIRTUAL_HEIGHT;                    // 600
    public static final int GAME_AREA_X      = INFO_PANEL_WIDTH;

    // ── Default window size ───────────────────────────────────────────
    public static final int DEFAULT_WIDTH  = 1000;
    public static final int DEFAULT_HEIGHT = 600;

    // ── Actual window size — mutable ──────────────────────────────────
    private static int windowWidth  = DEFAULT_WIDTH;
    private static int windowHeight = DEFAULT_HEIGHT;

    public static int getWindowWidth()  { return windowWidth;  }
    public static int getWindowHeight() { return windowHeight; }

    /**
     * Called by GameWindow at startup based on saved settings.
     * Does NOT affect VIRTUAL_WIDTH/HEIGHT — the game still renders
     * at 1000x600 internally and gets scaled by GameCanvas.
     */
    public static void setResolution(int w, int h) {
        windowWidth  = w;
        windowHeight = h;
    }

    // Legacy static fields kept for backward compatibility
    // (screens that reference LayoutConfig.WINDOW_WIDTH directly
    //  will still compile — they get the default value)
    public static int WINDOW_WIDTH  = DEFAULT_WIDTH;
    public static int WINDOW_HEIGHT = DEFAULT_HEIGHT;
}
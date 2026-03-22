package core;

/**
 * All possible game states.
 *
 * CHANGED:
 *  - Added DEBUG_MENU for the developer test level selector.
 */
public enum GameState {

    // ── Menus ─────────────────────────────────────────────────────────
    MAIN_MENU,
    NEW_GAME_MENU,
    NAME_ENTRY,
    LOAD_GAME_MENU,
    SETTINGS_MENU,
    CONTROL_MENU,
    DEBUG_MENU,         // developer test level + boss selector

    // ── Game flow ─────────────────────────────────────────────────────
    CAMPAIGN_INTRO,
    REGION_INTRO,
    LEVEL_LOAD,
    PLAYING,
    LEVEL_TRANSITION,
    GAME_OVER,
    CAMPAIGN_COMPLETE
}
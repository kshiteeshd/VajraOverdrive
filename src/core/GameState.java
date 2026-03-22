package core;

/**
 * All possible game states.
 *
 * CHANGED:
 *  - Removed: PROFILE_SELECT, MODE_SELECT, NEW_GAME_CONFIRM
 *    (old overlapping screens gone)
 *  - Added: NEW_GAME_MENU, LOAD_GAME_MENU, SETTINGS_MENU,
 *           MODE_SELECT (kept — now used inside New Game only),
 *           NAME_ENTRY (kept)
 *
 * New menu flow:
 *   MAIN_MENU
 *     -> NEW_GAME_MENU  -> MODE_SELECT -> NAME_ENTRY -> CAMPAIGN_INTRO / ENDLESS_INTRO
 *     -> LOAD_GAME_MENU -> (pick slot)  -> LEVEL_LOAD  (or ENDLESS_LOAD future)
 *     -> SETTINGS_MENU
 *     -> EXIT
 */
public enum GameState {

    // ── Menus ─────────────────────────────────────────────────────────
    MAIN_MENU,
    NEW_GAME_MENU,      // Campaign / Endless choice
    NAME_ENTRY,         // Pilot name entry (new game)
    LOAD_GAME_MENU,     // Pick a save slot to continue
    SETTINGS_MENU,
    CONTROL_MENU ,// Settings screen

    // ── Game flow ─────────────────────────────────────────────────────
    CAMPAIGN_INTRO,     // Story typewriter before level 1
    REGION_INTRO,       // Sector intro between regions
    LEVEL_LOAD,         // 3-2-1 countdown (continue path)
    PLAYING,
    LEVEL_TRANSITION,   // "LEVEL CLEARED" card between waves
    GAME_OVER,
    CAMPAIGN_COMPLETE
}
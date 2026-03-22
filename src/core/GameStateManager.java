package core;

/**
 * FIX: Initial state is MAIN_MENU.
 *      Was CAMPAIGN_INTRO which caused a double-set conflict with GameCanvas constructor.
 */
public class GameStateManager {

    private static GameState state = GameState.MAIN_MENU;

    public static GameState getState()               { return state; }
    public static void setState(GameState newState)  { state = newState; }
}

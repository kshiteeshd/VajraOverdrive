package ui.menu;

import save.GameMode;

/**
 * Data carrier: set by menu screens, read by GameCanvas when
 * it rebuilds a game session.
 *
 * CHANGED:
 *  - Added gameMode (CAMPAIGN / ENDLESS).
 *  - Added saveSlot so GameCanvas knows which slot to auto-save to.
 *  - isNewGame kept for routing (new = show intro, continue = level load).
 */
public class PendingGameStart {

    /** Level to start from. 1 for new game, profile.level for continue. */
    public static int      level     = 1;

    /** True = new game (show intro). False = continue (jump to level). */
    public static boolean  isNewGame = true;

    /** CAMPAIGN or ENDLESS. */
    public static GameMode gameMode  = GameMode.CAMPAIGN;

    /** Save slot number (1-based) for auto-save. */
    public static int      saveSlot  = 1;
}
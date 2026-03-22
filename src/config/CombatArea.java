package config;

/**
 * BATCH 4 CHANGE:
 * LEFT_BOUND is now 0 — player and enemies use the full width.
 * Margins are enforced by the player ship clamp, not the panel.
 */
public class CombatArea {

    public static final int LEFT_BOUND   = 0;
    public static final int RIGHT_BOUND  = LayoutConfig.GAME_AREA_WIDTH;   // 1000
    public static final int TOP_BOUND    = 0;
    public static final int BOTTOM_BOUND = LayoutConfig.GAME_AREA_HEIGHT;  // 600

    public static final int WIDTH  = RIGHT_BOUND  - LEFT_BOUND;
    public static final int HEIGHT = BOTTOM_BOUND - TOP_BOUND;
}
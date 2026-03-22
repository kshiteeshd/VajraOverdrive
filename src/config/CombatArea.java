package config;

/**
 * Defines the playable combat region where entities can move.
 */
public class CombatArea {

    public static final int LEFT_BOUND = LayoutConfig.INFO_PANEL_WIDTH;
    public static final int RIGHT_BOUND = LEFT_BOUND + LayoutConfig.GAME_AREA_WIDTH;

    public static final int TOP_BOUND = 0;
    public static final int BOTTOM_BOUND = LayoutConfig.GAME_AREA_HEIGHT;

    public static final int WIDTH = RIGHT_BOUND - LEFT_BOUND;
    public static final int HEIGHT = BOTTOM_BOUND - TOP_BOUND;

}
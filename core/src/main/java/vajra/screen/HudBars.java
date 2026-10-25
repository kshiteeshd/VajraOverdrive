package vajra.screen;

/** The HUD's shared colours and margins (the hull, shield and boost fills, the gold, the crew's anchor). */
public final class HudBars {
    // fills sourced from the shared theme
    public static final float[] HULL_C = { UiTheme.HULL_R, UiTheme.HULL_G, UiTheme.HULL_B };
    public static final float[] SHIELD_C = { UiTheme.SHIELD_R, UiTheme.SHIELD_G, UiTheme.SHIELD_B };
    public static final float[] BOOST_C = { UiTheme.BOOST_R, UiTheme.BOOST_G, UiTheme.BOOST_B };
    /** The boost tube in Vitala's heat: it glows hotter while the forge-lands feed it (Ship.heated). */
    public static final float[] BOOST_HOT = { 1f, 0.5f, 0.12f };
    public static final float[] GOLD = { UiTheme.GOLD_R, UiTheme.GOLD_G, UiTheme.GOLD_B };

    // block geometry (public: the screenshot test samples it)
    public static final float X = 14, TOP = Ui.UI_H - 14, W = 220;
    public static final float HULL_H = 8, SHIELD_H = 3, BOOST_H = 6;
    public static final float GAP = 2; // bars huddled close
    public static final float SHIELD_TOP = TOP - HULL_H - GAP;
    public static final float BOOST_TOP = SHIELD_TOP - SHIELD_H - GAP;
    public static final float BOT = BOOST_TOP - BOOST_H; // block bottom edge

    public static final float LIVES_Y = BOT - 5, LIVES_R = 3, LIVES_PITCH = 9;
}

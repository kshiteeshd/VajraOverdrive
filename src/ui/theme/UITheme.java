package ui.theme;

import java.awt.*;

/**
 * Global UI colour theme.
 *
 * Region nebula colours (faint background tints):
 *   RED    — deep crimson nebula, dust and heat
 *   YELLOW — amber/gold ion cloud
 *   BLUE   — cold deep-space ice field
 *   GREEN  — toxic gas giant haze
 *   WHITE  — bright star cluster, near-blinding
 */
public class UITheme {

    // ── Core palette ────────────────────────────────────────────────
    public static final Color BACKGROUND   = Color.BLACK;
    public static final Color PRIMARY      = new Color(0,   255, 255);   // cyan
    public static final Color ACCENT       = new Color(255, 180,   0);   // orange
    public static final Color DANGER       = new Color(255,  50,  50);   // red
    public static final Color HIGHLIGHT    = new Color(255, 255,   0);   // yellow
    public static final Color SECONDARY    = new Color(160, 120, 255);   // purple
    public static final Color TEXT_DIM     = new Color(120, 120, 140);   // muted label
    public static final Color TEXT_FAINT   = new Color( 70,  70,  85);   // very muted

    // ── HUD panel ───────────────────────────────────────────────────
    public static final Color PANEL_BG     = new Color( 10,  10,  18);
    public static final Color PANEL_BORDER = new Color(  0, 180, 180,  80);
    public static final Color PANEL_SEP    = new Color( 40,  40,  60);

    // ── Region accent colours (text / glow) ─────────────────────────
    public static final Color REGION_RED    = new Color(255,  70,  70);
    public static final Color REGION_YELLOW = new Color(255, 210,  60);
    public static final Color REGION_BLUE   = new Color( 80, 160, 255);
    public static final Color REGION_GREEN  = new Color( 60, 220, 100);
    public static final Color REGION_WHITE  = new Color(220, 220, 255);

    // ── Region nebula background tint colours ───────────────────────
    // Used by SpaceBackground to tint the star field
    public static final Color NEBULA_RED    = new Color( 80,   8,   8);
    public static final Color NEBULA_YELLOW = new Color( 70,  50,   0);
    public static final Color NEBULA_BLUE   = new Color(  0,  15,  70);
    public static final Color NEBULA_GREEN  = new Color(  0,  40,  15);
    public static final Color NEBULA_WHITE  = new Color( 30,  30,  55);

    /** Returns the bright accent colour for a region name string. */
    public static Color getRegionColor(String region) {
        return switch (region) {
            case "RED"    -> REGION_RED;
            case "YELLOW" -> REGION_YELLOW;
            case "BLUE"   -> REGION_BLUE;
            case "GREEN"  -> REGION_GREEN;
            case "WHITE"  -> REGION_WHITE;
            default       -> PRIMARY;
        };
    }

    /** Returns the dark nebula tint used as the space background. */
    public static Color getNebulaColor(String region) {
        return switch (region) {
            case "RED"    -> NEBULA_RED;
            case "YELLOW" -> NEBULA_YELLOW;
            case "BLUE"   -> NEBULA_BLUE;
            case "GREEN"  -> NEBULA_GREEN;
            case "WHITE"  -> NEBULA_WHITE;
            default       -> new Color(5, 5, 18);
        };
    }
}

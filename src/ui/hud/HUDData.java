package ui.hud;

import java.awt.Color;
import ui.theme.UITheme;

/**
 * Snapshot of all data the NebulaHUD needs to render one frame.
 * GameCanvas populates this each frame from WaveManager, ScoreManager
 * etc. and passes it into NebulaHUD.render().
 *
 * This breaks the direct static coupling between the HUD renderer
 * and the session state managers — fixing the architecture concern
 * identified in the Batch 1 analysis.
 */
public class HUDData {

    // ── Tactical (top-left cloud) ─────────────────────────────────────
    public int    score        = 0;
    public int    wave         = 0;
    public int    comboMult    = 1;      // score multiplier for combo display
    public String formation    = "";     // 3-char code: LNE, V-A, SWM, etc.

    // ── Navigation (top-right cloud) ─────────────────────────────────
    public String region       = "RED";
    public int    level        = 1;
    public int    totalLevels  = 25;
    public String gameMode     = "CAMPAIGN";

    // ── Survival (bottom-left cloud) ──────────────────────────────────
    public int   lives         = 3;
    public int   maxLives      = 3;
    public float shieldFrac    = 1.0f;  // 0.0 – 1.0
    public float armorFrac     = 1.0f;  // 0.0 – 1.0 (future use)
    public boolean shieldCrit  = false; // true when shield < 0.2

    // ── Threat (bottom-right cloud) ───────────────────────────────────
    public int    enemyCount   = 0;
    public String formationDots = "";   // pattern string for dot renderer

    // ── Boss (center-top bar, visible only during boss waves) ─────────
    public boolean bossActive  = false;
    public String  bossName    = "";
    public float   bossHpFrac  = 1.0f;
    public int     bossPhase   = 1;
    public int     bossTotalPhases = 1;

    // ── Meta ──────────────────────────────────────────────────────────
    public int fps = 60;

    // ── Helpers ───────────────────────────────────────────────────────
    /** Region accent color — drives all cloud tints. */
    public Color regionColor() {
        return UITheme.getRegionColor(region);
    }

    /**
     * 3-character formation code shown in top-left cloud.
     * Full name → compact code so it fits in the small cloud.
     */
    public static String formationCode(String name) {
        if (name == null || name.isEmpty()) return "---";
        return switch (name) {
            case "LINE"   -> "LNE";
            case "V"      -> "V-A";
            case "GRID"   -> "GRD";
            case "CIRCLE" -> "CIR";
            case "STAR"   -> "STR";
            case "DIAMOND"-> "DMD";
            default       -> name.length() >= 3
                    ? name.substring(0, 3).toUpperCase()
                    : name.toUpperCase();
        };
    }
}
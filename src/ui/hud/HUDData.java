package ui.hud;

import java.awt.Color;
import ui.theme.UITheme;

public class HUDData {

    // ── Tactical ──────────────────────────────────────────────────────
    public int    score       = 0;
    public int    wave        = 0;
    public int    comboMult   = 1;
    public String formation   = "";

    // ── Navigation ───────────────────────────────────────────────────
    public String region      = "RED";
    public int    level       = 1;
    public int    totalLevels = 25;
    public String gameMode    = "CAMPAIGN";

    // ── Survival ─────────────────────────────────────────────────────
    public int     lives      = 3;
    public int     maxLives   = 3;
    public float   shieldFrac = 1.0f;
    public float   armorFrac  = 1.0f;
    public boolean shieldCrit = false;

    // ── Threat ───────────────────────────────────────────────────────
    public int    enemyCount  = 0;

    // ── Boss ─────────────────────────────────────────────────────────
    public boolean bossActive      = false;
    public String  bossName        = "";
    public float   bossHpFrac      = 1.0f;
    public int     bossPhase       = 1;
    public int     bossTotalPhases = 1;

    // ── Meta ─────────────────────────────────────────────────────────
    public int fps = 60;

    // ── Helpers ───────────────────────────────────────────────────────
    public Color regionColor() {
        return UITheme.getRegionColor(region);
    }

    public static String formationCode(String name) {
        if (name == null || name.isEmpty()) return "---";
        return switch (name) {
            case "LINE"    -> "LNE";
            case "V"       -> "V-A";
            case "GRID"    -> "GRD";
            case "CIRCLE"  -> "CIR";
            case "STAR"    -> "STR";
            case "DIAMOND" -> "DMD";
            case "BOSS"    -> "BSS";
            default        -> name.length() >= 3
                    ? name.substring(0, 3).toUpperCase()
                    : name.toUpperCase();
        };
    }
}
package ui;

import config.LayoutConfig;
import score.ScoreManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;
import wave.WaveManager;

import java.awt.*;

public class GameUILayout {

    // ── Layout constants ──────────────────────────────────────────────
    private static final int PX  = 10;    // left padding
    private static final int PW  = 180;   // usable width
    private static final int PH  = LayoutConfig.GAME_AREA_HEIGHT;  // 600
    private static final int CX  = LayoutConfig.INFO_PANEL_WIDTH / 2;  // 100

    // ── Section Y positions — fixed, calculated once ──────────────────
    // Total usable height = 600 - 16 (region tag) - 20 (top pad) = 564
    // 6 sections: title, score, wave, formation, enemies, lives
    // Each section gets ~78px. Separators sit between them.
    private static final int Y_TITLE      = 20;
    private static final int Y_SEP_1      = 52;
    private static final int Y_SCORE      = 68;
    private static final int Y_SEP_2      = 106;
    private static final int Y_WAVE       = 122;
    private static final int Y_SEP_3      = 160;
    private static final int Y_FORMATION  = 176;
    private static final int Y_SEP_4      = 214;
    private static final int Y_ENEMIES    = 230;
    private static final int Y_SEP_5      = 268;
    private static final int Y_LIVES      = 284;
    private static final int Y_REGION     = PH - 18;

    // ── State ─────────────────────────────────────────────────────────
    private String currentRegion = "RED";

    // Score animation
    private int  displayedScore  = 0;   // what is shown — ticks toward real score
    private long lastScoreTick   = 0;

    // Wave slide-in
    private int  lastSeenWave    = -1;
    private long waveSlideStart  = 0;
    private static final long WAVE_SLIDE_MS = 400;

    // Lives flash
    private int  lastSeenLives   = 3;
    private long livesFlashStart = 0;
    private static final long LIVES_FLASH_MS = 600;

    // Formation fade-in
    private String lastFormation = "";
    private long   formationFadeStart = 0;
    private static final long FORMATION_FADE_MS = 300;

    // ── Public API ────────────────────────────────────────────────────
    public void setRegion(String region) { this.currentRegion = region; }

    public void render(Graphics2D g) {
        long now = System.currentTimeMillis();
        updateAnimations(now);

        drawPanel(g);

        drawTitle(g);
        drawSeparator(g, Y_SEP_1);
        drawScore(g, now);
        drawSeparator(g, Y_SEP_2);
        drawWave(g, now);
        drawSeparator(g, Y_SEP_3);
        drawFormation(g, now);
        drawSeparator(g, Y_SEP_4);
        drawEnemies(g);
        drawSeparator(g, Y_SEP_5);
        drawLives(g, now);
        drawRegion(g);
    }

    // ── Animation tick ────────────────────────────────────────────────
    private void updateAnimations(long now) {
        // Score counter — ticks up toward real score
        int realScore = ScoreManager.get().getScore();
        if (displayedScore < realScore) {
            if (now - lastScoreTick > 16) {   // ~60fps tick
                int delta = Math.max(1, (realScore - displayedScore) / 8);
                displayedScore += delta;
                if (displayedScore > realScore) displayedScore = realScore;
                lastScoreTick = now;
            }
        } else {
            displayedScore = realScore;
        }

        // Wave slide-in trigger
        int currentWave = WaveManager.getCurrentWave();
        if (currentWave != lastSeenWave) {
            lastSeenWave   = currentWave;
            waveSlideStart = now;
        }

        // Lives flash trigger
        int currentLives = WaveManager.getPlayerLives();
        if (currentLives < lastSeenLives) {
            lastSeenLives   = currentLives;
            livesFlashStart = now;
        } else {
            lastSeenLives = currentLives;
        }

        // Formation fade trigger
        String currentFormation = WaveManager.getCurrentFormation();
        if (currentFormation != null
                && !currentFormation.equals(lastFormation)) {
            lastFormation       = currentFormation;
            formationFadeStart  = now;
        }
    }

    // ── Panel background ──────────────────────────────────────────────
    private void drawPanel(Graphics2D g) {
        // Base
        g.setColor(UITheme.PANEL_BG);
        g.fillRect(0, 0, LayoutConfig.INFO_PANEL_WIDTH, PH);

        // Right border
        g.setColor(UITheme.PANEL_BORDER);
        g.fillRect(LayoutConfig.INFO_PANEL_WIDTH - 1, 0, 1, PH);

        // Left accent line
        g.setColor(new Color(0, 255, 255, 12));
        g.fillRect(0, 0, 2, PH);
    }

    // ── Separator ─────────────────────────────────────────────────────
    private void drawSeparator(Graphics2D g, int y) {
        g.setColor(UITheme.PANEL_SEP);
        g.fillRect(PX, y, PW, 1);
    }

    // ── Title ─────────────────────────────────────────────────────────
    private void drawTitle(Graphics2D g) {
        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.PRIMARY);
        g.drawString("VAJRA", PX, Y_TITLE);
        g.drawString("OVERDRIVE", PX, Y_TITLE + 14);
    }

    // ── Score (animated counter) ──────────────────────────────────────
    private void drawScore(Graphics2D g, long now) {
        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("SCORE", PX, Y_SCORE);

        // Colour pulses briefly when score just changed
        boolean justChanged = (ScoreManager.get().getScore() != displayedScore
                || now - lastScoreTick < 120);
        g.setFont(UIFonts.MENU);
        g.setColor(justChanged ? UITheme.HIGHLIGHT : UITheme.ACCENT);
        g.drawString(String.format("%08d", displayedScore), PX, Y_SCORE + 22);
    }

    // ── Wave (slide-in on new wave) ───────────────────────────────────
    private void drawWave(Graphics2D g, long now) {
        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("WAVE", PX, Y_WAVE);

        float slideT = Math.min((now - waveSlideStart) / (float) WAVE_SLIDE_MS, 1f);
        float ease   = easeOut(slideT);
        int   offX   = (int)((1f - ease) * (-30));   // slides in from left

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ease));
        g.setFont(UIFonts.MENU);
        g.setColor(UITheme.PRIMARY);
        g.drawString(String.format("%02d", WaveManager.getCurrentWave()),
                PX + offX, Y_WAVE + 22);
        g.setComposite(old);
    }

    // ── Formation (fade-in on new formation) ──────────────────────────
    private void drawFormation(Graphics2D g, long now) {
        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("FORMATION", PX, Y_FORMATION);

        String f = WaveManager.getCurrentFormation();
        if (f == null || f.isEmpty()) {
            g.setFont(UIFonts.HUD);
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString("---", PX, Y_FORMATION + 18);
            return;
        }

        float fadeT = Math.min((now - formationFadeStart)
                / (float) FORMATION_FADE_MS, 1f);

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, easeOut(fadeT)));

        String icon = formationIcon(f);
        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.SECONDARY);
        g.drawString(icon + "  " + f, PX, Y_FORMATION + 18);

        g.setComposite(old);
    }

    private String formationIcon(String f) {
        if (f == null) return "-";
        return switch (f) {
            case "LINE"   -> "---";
            case "V"      -> "/\\";
            case "GRID"   -> "##";
            case "CIRCLE" -> "()";
            case "STAR"   -> " *";
            default       -> "-";
        };
    }

    // ── Enemies ───────────────────────────────────────────────────────
    private void drawEnemies(Graphics2D g) {
        int enemies = WaveManager.getRemainingEnemies();

        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("ENEMIES", PX, Y_ENEMIES);

        Color col = (enemies > 0 && enemies <= 3)
                ? UITheme.DANGER : UITheme.PRIMARY;

        // Pulse red when <= 3
        if (enemies > 0 && enemies <= 3) {
            boolean pulse = (System.currentTimeMillis() / 300) % 2 == 0;
            col = pulse ? UITheme.DANGER : new Color(200, 40, 40);
        }

        g.setFont(UIFonts.MENU);
        g.setColor(col);
        g.drawString(String.format("%02d", enemies), PX, Y_ENEMIES + 22);
    }

    // ── Lives (flash red on lose) ─────────────────────────────────────
    private void drawLives(Graphics2D g, long now) {
        int lives    = WaveManager.getPlayerLives();
        int maxLives = WaveManager.getMaxLives();

        g.setFont(UIFonts.HUD);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("LIVES", PX, Y_LIVES);

        long sinceFlash = now - livesFlashStart;
        boolean flashing = sinceFlash < LIVES_FLASH_MS;

        int iconY = Y_LIVES + 14;

        for (int i = 0; i < maxLives; i++) {
            if (i < lives) {
                // Active life icon
                // Flash the last-lost icon position in red briefly
                boolean isLostSlot = flashing && (i == lives);
                Color col = isLostSlot
                        ? (sinceFlash % 150 < 75 ? UITheme.DANGER
                        : UITheme.PRIMARY)
                        : UITheme.PRIMARY;
                drawShipIcon(g, PX + i * 22, iconY, col);
            } else {
                // Empty slot — dim
                boolean isLostSlot = flashing && (i == lives);
                if (isLostSlot) {
                    // Show the just-lost icon flashing red then fading
                    float alpha = 1f - (sinceFlash / (float) LIVES_FLASH_MS);
                    Composite old = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, alpha));
                    drawShipIcon(g, PX + i * 22, iconY, UITheme.DANGER);
                    g.setComposite(old);
                } else {
                    drawShipIcon(g, PX + i * 22, iconY, UITheme.TEXT_FAINT);
                }
            }
        }
    }

    private void drawShipIcon(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        int[] px = { x + 6, x,       x + 12 };
        int[] py = { y,     y + 12,  y + 12  };
        g.fillPolygon(px, py, 3);
        g.setColor(color.darker());
        g.fillRect(x + 4, y + 11, 4, 2);
    }

    // ── Region tag — pinned to bottom ─────────────────────────────────
    private void drawRegion(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(currentRegion);

        // Separator above region tag
        g.setColor(UITheme.PANEL_SEP);
        g.fillRect(PX, Y_REGION - 14, PW, 1);

        // Color dot
        g.setColor(regionColor);
        g.fillRect(PX, Y_REGION - 7, 6, 6);

        // Label
        g.setFont(UIFonts.HUD);
        g.setColor(regionColor);
        g.drawString(currentRegion + " SECTOR", PX + 10, Y_REGION);
    }

    // ── Easing ────────────────────────────────────────────────────────
    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
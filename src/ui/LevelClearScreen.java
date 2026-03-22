package ui;

import config.LayoutConfig;
import score.ScoreManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

public class LevelClearScreen {

    private static final int TOTAL_LEVELS = 25;

    private static final int GX = LayoutConfig.GAME_AREA_X;
    private static final int GW = LayoutConfig.GAME_AREA_WIDTH;
    private static final int GH = LayoutConfig.GAME_AREA_HEIGHT;
    private static final int CX = GX + GW / 2;

    private int    level         = 1;
    private String region        = "RED";
    private long   enterTime     = 0;
    private int    scoreSnapshot = 0;

    public void enter(int level, String region) {
        this.level         = level;
        this.region        = region;
        this.enterTime     = System.currentTimeMillis();
        this.scoreSnapshot = ScoreManager.get().getScore();
    }

    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        long  elapsed     = System.currentTimeMillis() - enterTime;

        // Dark overlay
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(GX, 0, GW, GH);

        // Card slides in — separate from bar
        float cardSlide = easeOut(Math.min(elapsed / 350f, 1f));
        int   cardOffY  = (int)((1f - cardSlide) * (-GH));

        // ── Centre card ───────────────────────────────────────────────
        int bw = 400, bh = 160;
        int bx = CX - bw / 2;
        int by = GH / 2 - bh / 2 + cardOffY;

        // Border glow
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 80));
        g.fillRect(bx - 2, by - 2, bw + 4, bh + 4);

        // Card fill
        g.setColor(new Color(8, 8, 16));
        g.fillRect(bx, by, bw, bh);

        // Top accent stripe
        g.setColor(regionColor);
        g.fillRect(bx, by, bw, 3);

        // LEVEL XX
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String levelStr = String.format("LEVEL  %02d", level);
        g.setColor(regionColor);
        g.drawString(levelStr, CX - fmT.stringWidth(levelStr) / 2, by + 50);

        // CLEARED
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String cleared = "C L E A R E D";
        g.setColor(UITheme.PRIMARY);
        g.drawString(cleared, CX - fmM.stringWidth(cleared) / 2, by + 84);

        // Score
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String scoreStr = "SCORE  " + String.format("%08d", scoreSnapshot);
        g.setColor(UITheme.ACCENT);
        g.drawString(scoreStr, CX - fmS.stringWidth(scoreStr) / 2, by + 116);

        // ── Progress bar — slides in separately, always stays in view ─
        float barSlide = easeOut(Math.min(elapsed / 500f, 1f));
        int   barOffY  = (int)((1f - barSlide) * 60);  // slides UP from below

        int barY = GH - 72 + barOffY;   // FIXED: no longer uses cardOffY
        int barX = GX + 60;
        int barW = GW - 120;

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, barSlide));

        // Track
        g.setColor(new Color(30, 30, 40));
        g.fillRect(barX, barY, barW, 8);

        // Fill
        int filledW = (int)(barW * ((float) level / TOTAL_LEVELS));
        g.setColor(regionColor);
        g.fillRect(barX, barY, filledW, 8);

        // Region milestone dots at every 5 levels
        for (int i = 5; i <= TOTAL_LEVELS; i += 5) {
            int nx = barX + (int)(barW * (i / (float) TOTAL_LEVELS));
            g.setColor(i <= level ? regionColor : UITheme.TEXT_FAINT);
            g.fillRect(nx - 2, barY - 3, 5, 14);
        }

        // Label
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String prog = "LEVEL " + level + " / " + TOTAL_LEVELS;
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(prog, CX - fmS.stringWidth(prog) / 2, barY + 24);

        g.setComposite(old);
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
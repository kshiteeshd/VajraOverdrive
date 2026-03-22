package ui;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import score.ScoreManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Level clear overlay — shown during LEVEL_TRANSITION state.
 *
 * CHANGED (completing Batch 5 placeholder):
 *  - update() now handles ENTER to skip the transition timer early.
 *    Sets state to PLAYING directly — CampaignManager will call
 *    nextLevel() on the next tick because isLevelFinished() is true
 *    and the transition delay check sees elapsed > 0.
 *    We set a skipRequested flag and let CampaignManager drive the
 *    actual state change so the level load sequence stays intact.
 *  - All fonts UIFonts (Press Start 2P) — same as Batch 5 render().
 */
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

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter(int level, String region) {
        this.level         = level;
        this.region        = region;
        this.enterTime     = System.currentTimeMillis();
        this.scoreSnapshot = ScoreManager.get().getScore();
    }

    // ── Update ────────────────────────────────────────────────────────
    /**
     * ENTER pressed during level clear skips the remaining wait time
     * by winding enterTime back so CampaignManager's elapsed check
     * fires on the very next update tick.
     */
    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
            // Wind the clock back so CampaignManager sees
            // elapsed > LEVEL_TRANSITION_DELAY immediately.
            // We subtract 10 seconds — more than any transition delay.
            enterTime -= 10_000;
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        long  elapsed     = System.currentTimeMillis() - enterTime;

        // ── Dark overlay over the game ────────────────────────────────
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(GX, 0, GW, GH);

        // ── Card slide in from top ────────────────────────────────────
        float cardSlide = easeOut(Math.min(elapsed / 300f, 1f));
        int   cardOffY  = (int)((1f - cardSlide) * (-GH / 2));

        int bw = 440;
        int bh = 148;
        int bx = CX - bw / 2;
        int by = GH / 2 - bh / 2 - 20 + cardOffY;

        // ── Outer glow ────────────────────────────────────────────────
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 50));
        g.fillRect(bx - 3, by - 3, bw + 6, bh + 6);

        // ── Card body ─────────────────────────────────────────────────
        g.setColor(new Color(6, 6, 14));
        g.fillRect(bx, by, bw, bh);

        // Top accent stripe
        g.setColor(regionColor);
        g.fillRect(bx, by, bw, 3);

        // Left accent stripe
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 120));
        g.fillRect(bx, by + 3, 3, bh - 3);

        // Bottom border
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 40));
        g.fillRect(bx, by + bh - 1, bw, 1);

        // ── Region tag ────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String regionTag = region + "  SECTOR";
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 180));
        g.drawString(regionTag,
                CX - fmS.stringWidth(regionTag) / 2,
                by + 22);

        // ── Level number ──────────────────────────────────────────────
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String levelStr = String.format("LEVEL  %02d", level);
        g.setColor(regionColor);
        g.drawString(levelStr,
                CX - fmM.stringWidth(levelStr) / 2,
                by + 54);

        // ── CLEARED — fades in after card appears ─────────────────────
        float clearedAlpha = easeOut(Math.min(
                Math.max(elapsed - 160f, 0f) / 260f, 1f));
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, clearedAlpha));

        g.setFont(UIFonts.MENU);
        fmM = g.getFontMetrics();
        String cleared = "C L E A R E D";
        g.setColor(UITheme.PRIMARY);
        g.drawString(cleared,
                CX - fmM.stringWidth(cleared) / 2,
                by + 84);

        g.setComposite(old);

        // ── Score row ─────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String scoreLabel = "SCORE";
        String scoreVal   = String.format("%08d", scoreSnapshot);

        int scoreRowY = by + 118;
        int labelX    = bx + 20;
        int valueX    = bx + bw - 20 - fmS.stringWidth(scoreVal);

        // Divider above score row
        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(bx + 16, scoreRowY - 14, bw - 32, 1);

        g.setColor(UITheme.TEXT_DIM);
        g.drawString(scoreLabel, labelX, scoreRowY);

        g.setColor(UITheme.ACCENT);
        g.drawString(scoreVal, valueX, scoreRowY);

        // ── Progress bar ──────────────────────────────────────────────
        float barSlide = easeOut(Math.min(
                Math.max(elapsed - 100f, 0f) / 400f, 1f));
        int   barOffY  = (int)((1f - barSlide) * 50);

        int barY = GH - 60 + barOffY;
        int barX = GX + 60;
        int barW = GW - 120;
        int barH = 6;

        Composite barOld = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, barSlide * 0.95f));

        // Track
        g.setColor(new Color(20, 20, 30));
        g.fillRect(barX, barY, barW, barH);

        // Fill
        int filledW = (int)(barW * ((float) level / TOTAL_LEVELS));
        g.setColor(regionColor);
        g.fillRect(barX, barY, filledW, barH);

        // Leading edge glow dot
        if (filledW > 0) {
            g.setColor(Color.WHITE);
            g.fillRect(barX + filledW - 2, barY - 1, 3, barH + 2);
        }

        // Region milestone ticks every 5 levels
        for (int i = 5; i <= TOTAL_LEVELS; i += 5) {
            int nx = barX + (int)(barW * (i / (float) TOTAL_LEVELS));
            g.setColor(i <= level ? regionColor : new Color(40, 40, 55));
            g.fillRect(nx - 1, barY - 3, 2, barH + 6);
        }

        // Progress label
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String prog = "LVL " + level + " / " + TOTAL_LEVELS;
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(prog,
                CX - fmS.stringWidth(prog) / 2,
                barY + barH + 16);

        // ENTER hint — blinks at bottom
        boolean blinkOn = (System.currentTimeMillis() / 600) % 2 == 0;
        if (blinkOn) {
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.5f));
            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            String skip = "ENTER  continue";
            g.setColor(UITheme.TEXT_DIM);
            g.drawString(skip,
                    CX - fmS.stringWidth(skip) / 2,
                    GH - 16);
        }

        g.setComposite(barOld);
    }

    // ── Easing ────────────────────────────────────────────────────────
    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
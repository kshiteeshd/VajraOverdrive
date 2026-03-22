package ui;

import config.LayoutConfig;
import score.ScoreManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;
import wave.WaveManager;

import java.awt.*;

/**
 * Game over screen.
 *
 * CHANGED:
 *  - All fonts now UIFonts (Press Start 2P) — no inline Font objects.
 *  - Scanline sweep uses a single pass with a smooth falloff instead
 *    of per-row alpha allocation — same visual, far fewer compositing
 *    state changes.
 *  - Stat rows vertically centered properly for Press Start 2P's
 *    ascent metrics.
 *  - Blink prompt pinned to bottom — not affected by slide offset.
 *  - Title glow uses region-safe dark red — not hardcoded pink.
 */
public class GameOverScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private long    enterTime = 0;
    private int     scoreSnap = 0;
    private int     waveSnap  = 0;
    private boolean blinkOn   = true;
    private long    blinkTimer = 0;
    private float   scanY     = 0f;

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter() {
        enterTime  = System.currentTimeMillis();
        scoreSnap  = ScoreManager.get().getScore();
        waveSnap   = WaveManager.getCurrentWave();
        blinkOn    = true;
        blinkTimer = enterTime;
        scanY      = 0f;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        long now = System.currentTimeMillis();

        // Scanline sweeps down the screen
        scanY += 1.2f;
        if (scanY > H) scanY = 0f;

        if (now - blinkTimer > 550) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        long  elapsed = System.currentTimeMillis() - enterTime;
        float slide   = easeOut(Math.min(elapsed / 500f, 1f));
        int   offY    = (int)((1f - slide) * (-H / 2));

        // ── Background ────────────────────────────────────────────────
        g.setColor(new Color(4, 0, 0));
        g.fillRect(0, 0, W, H);

        // ── Scanline sweep — single gradient band ─────────────────────
        // One composite set, one fill rect, done.
        // The "glow" comes from the band being brightest at scanY
        // and fading above and below — drawn as three overlapping rects
        // with decreasing alpha instead of per-row allocation.
        Composite orig = g.getComposite();
        g.setColor(new Color(180, 0, 0));

        int scanBandH = 60;
        for (int band = 0; band < 3; band++) {
            float bandAlpha = 0.06f - band * 0.018f;
            int   bandY     = (int) scanY - band * 20;
            int   bandH     = scanBandH - band * 16;
            if (bandH <= 0) continue;
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, bandAlpha));
            g.fillRect(0, bandY, W, bandH);
        }
        g.setComposite(orig);

        // ── Title block — slides in ───────────────────────────────────
        int titleY = 190 + offY;

        // Glow layers behind title text
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "GAME  OVER";
        int    tx    = CX - fmT.stringWidth(title) / 2;

        for (int pass = 3; pass >= 1; pass--) {
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.07f * pass));
            g.setColor(UITheme.DANGER);
            g.drawString(title, tx - pass, titleY + pass);
            g.drawString(title, tx + pass, titleY + pass);
        }
        g.setComposite(orig);

        g.setFont(UIFonts.TITLE);
        g.setColor(UITheme.DANGER);
        g.drawString(title, tx, titleY);

        // Divider under title
        int divY = titleY + 20;
        g.setColor(new Color(180, 30, 30, 55));
        g.fillRect(CX - 210, divY, 420, 1);

        // ── Stat rows ─────────────────────────────────────────────────
        // Anchored relative to divY so they move with the slide
        int rowH   = 44;
        int stat1Y = divY + rowH;
        int stat2Y = divY + rowH * 2;

        drawStatRow(g, "SCORE",
                String.format("%08d", scoreSnap),
                CX, stat1Y, UITheme.ACCENT);

        drawStatRow(g, "WAVE",
                String.format("%02d", waveSnap),
                CX, stat2Y, UITheme.PRIMARY);

        // Lower divider
        int lowerDivY = stat2Y + 28;
        g.setColor(new Color(120, 0, 0, 55));
        g.fillRect(CX - 210, lowerDivY, 420, 1);

        // ── Blink prompt — always at bottom, never slides ─────────────
        if (blinkOn) {
            g.setFont(UIFonts.SMALL);
            FontMetrics fmS = g.getFontMetrics();
            String prompt = "ENTER  RETURN TO MENU";
            g.setColor(UITheme.HIGHLIGHT);
            g.drawString(prompt,
                    CX - fmS.stringWidth(prompt) / 2,
                    H - 40);
        }
    }

    // ── Stat row ──────────────────────────────────────────────────────
    private void drawStatRow(Graphics2D g,
                             String label, String value,
                             int cx, int y, Color valColor) {
        int rowW   = 360;
        int labelX = cx - rowW / 2;
        int valueX = cx + rowW / 2;

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(label, labelX, y);

        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        g.setColor(valColor);
        g.drawString(value,
                valueX - fmM.stringWidth(value), y);
    }

    // ── Easing ────────────────────────────────────────────────────────
    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
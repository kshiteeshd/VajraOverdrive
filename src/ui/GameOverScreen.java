package ui;

import config.LayoutConfig;
import score.ScoreManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;
import wave.WaveManager;

import java.awt.*;

public class GameOverScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private long    enterTime  = 0;
    private int     scoreSnap  = 0;
    private int     waveSnap   = 0;
    private boolean blinkOn    = true;
    private long    blinkTimer = 0;
    private float   scanY      = 0;

    public void enter() {
        enterTime  = System.currentTimeMillis();
        scoreSnap  = ScoreManager.get().getScore();
        waveSnap   = WaveManager.getCurrentWave();
        blinkOn    = true;
        blinkTimer = enterTime;
        scanY      = 0;
    }

    public void update() {
        long now = System.currentTimeMillis();
        scanY += 0.8f;
        if (scanY > H) scanY = 0;
        if (now - blinkTimer > 600) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }
    }

    public void render(Graphics2D g) {
        long  elapsed = System.currentTimeMillis() - enterTime;
        float slide   = easeOut(Math.min(elapsed / 500f, 1f));
        int   offY    = (int)((1f - slide) * (-H));

        // Base
        g.setColor(new Color(4, 0, 0));
        g.fillRect(0, 0, W, H);

        // Scanline sweep
        Composite old = g.getComposite();
        for (int i = 0; i < H; i += 8) {
            float dist  = Math.abs(i - scanY) / (float) H;
            float alpha = Math.max(0, 0.07f - dist * 0.12f);
            if (alpha > 0) {
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, alpha));
                g.setColor(new Color(200, 0, 0));
                g.fillRect(0, i, W, 4);
            }
        }
        g.setComposite(old);

        // ── Title ─────────────────────────────────────────────────────
        int titleY = 200 + offY;
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "GAME OVER";
        int tx = CX - fmT.stringWidth(title) / 2;

        // Glow
        for (int pass = 3; pass >= 1; pass--) {
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.08f * pass));
            g.setColor(UITheme.DANGER);
            g.drawString(title, tx - pass, titleY + pass);
            g.drawString(title, tx + pass, titleY + pass);
        }
        g.setComposite(old);
        g.setColor(UITheme.DANGER);
        g.drawString(title, tx, titleY);

        // Divider
        int divY = titleY + 24;
        g.setColor(new Color(200, 50, 50, 60));
        g.fillRect(CX - 200, divY, 400, 1);

        // ── Stats — all anchored to divY + offY ───────────────────────
        int rowH   = 40;
        int statY1 = divY + 44;
        int statY2 = divY + 44 + rowH;

        drawStatRow(g, "SCORE",        String.format("%08d", scoreSnap),
                CX, statY1, UITheme.ACCENT);
        drawStatRow(g, "WAVE REACHED", String.format("%02d", waveSnap),
                CX, statY2, UITheme.PRIMARY);

        // Bottom rule
        int ruleY = statY2 + rowH + 10;
        g.setColor(new Color(120, 0, 0, 80));
        g.fillRect(CX - 240, ruleY, 480, 1);

        // ── Blink prompt — fixed at bottom, not sliding ───────────────
        if (blinkOn) {
            g.setFont(UIFonts.SMALL);
            FontMetrics fmS = g.getFontMetrics();
            String prompt = "[ ENTER ]  RETURN TO MENU";
            g.setColor(UITheme.HIGHLIGHT);
            g.drawString(prompt,
                    CX - fmS.stringWidth(prompt) / 2, H - 50);
        }
    }

    private void drawStatRow(Graphics2D g, String label, String value,
                             int cx, int y, Color valColor) {
        int rowW   = 340;
        int labelX = cx - rowW / 2;
        int valueX = cx + rowW / 2;

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(label, labelX, y);

        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        g.setColor(valColor);
        g.drawString(value, valueX - fmM.stringWidth(value), y);
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
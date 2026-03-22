package ui;

import config.LayoutConfig;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

public class CampaignCompleteScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private long    enterTime  = 0;
    private int     finalScore = 0;
    private boolean blinkOn    = true;
    private long    blinkTimer = 0;

    public void enter(int score) {
        this.enterTime  = System.currentTimeMillis();
        this.finalScore = score;
        this.blinkOn    = true;
        this.blinkTimer = enterTime;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - blinkTimer > 600) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }
    }

    public void render(Graphics2D g) {
        long  elapsed = System.currentTimeMillis() - enterTime;
        float slide   = easeOut(Math.min(elapsed / 600f, 1f));
        int   offY    = (int)((1f - slide) * (-H));

        // Background
        g.setColor(new Color(0, 4, 8));
        g.fillRect(0, 0, W, H);

        // Subtle cyan tint
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
        g.setColor(UITheme.PRIMARY);
        g.fillRect(0, 0, W, H);
        g.setComposite(old);

        // Top rule
        g.setColor(new Color(0, 180, 180, 60));
        g.fillRect(CX - 240, 100 + offY, 480, 1);

        // Title
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String line1 = "MISSION";
        String line2 = "COMPLETE";
        g.setColor(UITheme.PRIMARY);
        g.drawString(line1, CX - fmT.stringWidth(line1) / 2, 180 + offY);
        g.setColor(UITheme.ACCENT);
        g.drawString(line2, CX - fmT.stringWidth(line2) / 2, 222 + offY);

        // Divider
        g.setColor(new Color(0, 200, 200, 50));
        g.fillRect(CX - 200, 244 + offY, 400, 1);

        // Flavour text
        g.setFont(UIFonts.BODY);
        FontMetrics fmB = g.getFontMetrics();
        String msg1 = "The armada has been destroyed.";
        String msg2 = "Vajra returns home.";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(msg1, CX - fmB.stringWidth(msg1) / 2, 292 + offY);
        g.drawString(msg2, CX - fmB.stringWidth(msg2) / 2, 322 + offY);

        // Score label
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String scoreLabel = "FINAL SCORE";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(scoreLabel, CX - fmS.stringWidth(scoreLabel) / 2, 376 + offY);

        // Score value
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String scoreVal = String.format("%08d", finalScore);
        g.setColor(UITheme.ACCENT);
        g.drawString(scoreVal, CX - fmM.stringWidth(scoreVal) / 2, 404 + offY);

        // Bottom rule
        g.setColor(new Color(0, 180, 180, 60));
        g.fillRect(CX - 240, 428 + offY, 480, 1);

        // Blink prompt — FIXED: pinned to bottom, never slides off
        if (blinkOn) {
            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            String prompt = "[ ENTER ]  RETURN TO MENU";
            g.setColor(UITheme.HIGHLIGHT);
            g.drawString(prompt, CX - fmS.stringWidth(prompt) / 2, H - 50);
        }
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
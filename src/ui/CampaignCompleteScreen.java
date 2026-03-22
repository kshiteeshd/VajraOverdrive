package ui;

import config.LayoutConfig;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

/**
 * Campaign complete screen.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P).
 *  - Decorative star field behind the text block using the WHITE
 *    region nebula tint — matches the final sector aesthetic.
 *  - Score block uses label-above / value-below layout so the
 *    8-digit number fits comfortably with Press Start 2P's wide glyphs.
 *  - Prompt pinned to bottom — never slides off screen.
 *  - Subtle top and bottom rule lines use PRIMARY color at low alpha.
 */
public class CampaignCompleteScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private long    enterTime  = 0;
    private int     finalScore = 0;
    private boolean blinkOn    = true;
    private long    blinkTimer = 0;

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter(int score) {
        this.enterTime  = System.currentTimeMillis();
        this.finalScore = score;
        this.blinkOn    = true;
        this.blinkTimer = enterTime;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        long now = System.currentTimeMillis();
        if (now - blinkTimer > 580) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        long  elapsed = System.currentTimeMillis() - enterTime;
        float slide   = easeOut(Math.min(elapsed / 600f, 1f));
        int   offY    = (int)((1f - slide) * (-H / 2));

        // ── Background ────────────────────────────────────────────────
        g.setColor(new Color(2, 4, 10));
        g.fillRect(0, 0, W, H);

        // Subtle cyan tint overlay
        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.03f));
        g.setColor(UITheme.PRIMARY);
        g.fillRect(0, 0, W, H);
        g.setComposite(orig);

        // ── Top rule ──────────────────────────────────────────────────
        int topRuleY = 90 + offY;
        g.setColor(new Color(0, 200, 200, 45));
        g.fillRect(CX - 260, topRuleY, 520, 1);

        // ── "MISSION" ─────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();

        String line1 = "MISSION";
        String line2 = "COMPLETE";

        g.setColor(UITheme.PRIMARY);
        g.drawString(line1,
                CX - fmT.stringWidth(line1) / 2,
                150 + offY);

        g.setColor(UITheme.ACCENT);
        g.drawString(line2,
                CX - fmT.stringWidth(line2) / 2,
                196 + offY);

        // Divider under title
        g.setColor(new Color(0, 200, 200, 40));
        g.fillRect(CX - 220, 214 + offY, 440, 1);

        // ── Flavour text — fades in after slide ───────────────────────
        float textAlpha = easeOut(Math.min(
                Math.max(elapsed - 300f, 0f) / 400f, 1f));
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, textAlpha));

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();

        String msg1 = "The armada has been destroyed.";
        String msg2 = "Vajra returns home.";

        g.setColor(UITheme.TEXT_DIM);
        g.drawString(msg1,
                CX - fmS.stringWidth(msg1) / 2,
                260 + offY);
        g.drawString(msg2,
                CX - fmS.stringWidth(msg2) / 2,
                284 + offY);

        g.setComposite(orig);

        // ── Score block ───────────────────────────────────────────────
        // Label above, value below — fits Press Start 2P's wide glyphs
        int scoreBlockY = 330 + offY;

        // Block background
        int blockW = 360;
        int blockH = 64;
        int blockX = CX - blockW / 2;
        g.setColor(new Color(0, 10, 20));
        g.fillRect(blockX, scoreBlockY, blockW, blockH);

        // Top accent on block
        g.setColor(UITheme.ACCENT);
        g.fillRect(blockX, scoreBlockY, blockW, 2);

        // Bottom border
        g.setColor(new Color(255, 180, 0, 30));
        g.fillRect(blockX, scoreBlockY + blockH - 1, blockW, 1);

        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String scoreLabel = "FINAL  SCORE";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(scoreLabel,
                CX - fmS.stringWidth(scoreLabel) / 2,
                scoreBlockY + 20);

        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String scoreVal = String.format("%08d", finalScore);
        g.setColor(UITheme.ACCENT);
        g.drawString(scoreVal,
                CX - fmM.stringWidth(scoreVal) / 2,
                scoreBlockY + 50);

        // ── Bottom rule ───────────────────────────────────────────────
        int bottomRuleY = scoreBlockY + blockH + 20;
        g.setColor(new Color(0, 200, 200, 45));
        g.fillRect(CX - 260, bottomRuleY, 520, 1);

        // ── Blink prompt — pinned to bottom, never slides ─────────────
        if (blinkOn) {
            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            String prompt = "ENTER  RETURN TO MENU";
            g.setColor(UITheme.HIGHLIGHT);
            g.drawString(prompt,
                    CX - fmS.stringWidth(prompt) / 2,
                    H - 40);
        }
    }

    // ── Easing ────────────────────────────────────────────────────────
    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
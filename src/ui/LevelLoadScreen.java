package ui;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

/**
 * Level load screen — 3-2-1-GO countdown before gameplay starts.
 *
 * CHANGED:
 *  - Countdown digits use UIFonts.TITLE (Press Start 2P 32px).
 *  - Each countdown number has its own accent color:
 *      3 → DANGER (red)   2 → ACCENT (orange)
 *      1 → HIGHLIGHT (yellow)   GO → PRIMARY (cyan)
 *  - Pulse alpha now eases in sharply and out slowly so each number
 *    feels like it "hits" rather than just fading uniformly.
 *  - Region sector label uses UIFonts.SMALL consistently.
 *  - Progress bar has a leading-edge glow dot matching the original
 *    LevelClearScreen style for visual consistency.
 *  - Bar segment ticks at 25 / 50 / 75 percent kept.
 */
public class LevelLoadScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    // Total time before PLAYING state is entered
    private static final long TOTAL_MS  = 3600;

    // Each number shows for 900ms
    private static final long SLOT_MS   = 900;

    private int     level    = 1;
    private String  region   = "RED";
    private long    enterTime;
    private boolean advanced = false;

    private final SpaceBackground bg = new SpaceBackground();

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter(int level, String region) {
        this.level     = level;
        this.region    = region;
        this.enterTime = System.currentTimeMillis();
        this.advanced  = false;
        bg.setRegion(region);
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        bg.update();
        long elapsed = System.currentTimeMillis() - enterTime;
        if (!advanced && elapsed >= TOTAL_MS) {
            advanced = true;
            GameStateManager.setState(GameState.PLAYING);
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        long  elapsed     = System.currentTimeMillis() - enterTime;

        bg.render(g, 0, 0, W, H);

        // Dark overlay so text pops over the background
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, W, H);

        // ── Sector tag ────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String sectorStr = region + "  SECTOR";
        g.setColor(regionColor);
        g.drawString(sectorStr,
                CX - fmS.stringWidth(sectorStr) / 2,
                H / 2 - 100);

        // ── Level label ───────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String lvlStr = String.format("LEVEL  %02d", level);
        g.setColor(UITheme.PRIMARY);
        g.drawString(lvlStr,
                CX - fmT.stringWidth(lvlStr) / 2,
                H / 2 - 32);

        // Accent line under level label
        g.setColor(regionColor);
        g.fillRect(CX - 160, H / 2 - 14, 320, 2);

        // ── Countdown number ──────────────────────────────────────────
        // Determine which slot we are in
        String countStr;
        Color  countColor;

        if      (elapsed < SLOT_MS)         { countStr = "3";  countColor = UITheme.DANGER;    }
        else if (elapsed < SLOT_MS * 2)     { countStr = "2";  countColor = UITheme.ACCENT;    }
        else if (elapsed < SLOT_MS * 3)     { countStr = "1";  countColor = UITheme.HIGHLIGHT; }
        else                                { countStr = "GO"; countColor = UITheme.PRIMARY;   }

        // Pulse: sharp attack, slow decay within each 900ms slot
        long withinSlot  = elapsed % SLOT_MS;
        float attackT    = Math.min(withinSlot / 80f,  1f);   // 0→1 in 80ms
        float decayT     = Math.max((withinSlot - 80f) / (SLOT_MS - 80f), 0f);
        float pulseAlpha = attackT * (1f - decayT * 0.4f);    // 1.0 → 0.6

        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                Math.max(0.05f, Math.min(pulseAlpha, 1f))));

        g.setFont(UIFonts.TITLE);
        fmT = g.getFontMetrics();
        g.setColor(countColor);
        g.drawString(countStr,
                CX - fmT.stringWidth(countStr) / 2,
                H / 2 + 90);

        g.setComposite(orig);

        // ── Progress bar ──────────────────────────────────────────────
        int   barW   = 320;
        int   barH   = 4;
        int   barX   = CX - barW / 2;
        int   barY   = H - 54;
        float prog   = Math.min(elapsed / (float) TOTAL_MS, 1f);
        int   fillW  = (int)(barW * prog);

        // Track
        g.setColor(new Color(20, 20, 30));
        g.fillRect(barX, barY, barW, barH);

        // Fill
        g.setColor(regionColor);
        g.fillRect(barX, barY, fillW, barH);

        // Leading edge glow dot
        if (fillW > 2) {
            g.setColor(Color.WHITE);
            g.fillRect(barX + fillW - 2, barY - 1, 3, barH + 2);
        }

        // Segment ticks at 25 / 50 / 75 percent
        for (int tick = 1; tick <= 3; tick++) {
            int tx = barX + (int)(barW * tick * 0.25f);
            g.setColor(new Color(50, 50, 65));
            g.fillRect(tx - 1, barY - 2, 2, barH + 4);
        }
    }
}
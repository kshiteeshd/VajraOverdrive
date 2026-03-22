package ui;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

public class LevelLoadScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final long TOTAL_MS = 3600;  // 3.6 seconds total

    private int     level    = 1;
    private String  region   = "RED";
    private long    enterTime;
    private boolean advanced = false;

    private final SpaceBackground bg = new SpaceBackground();

    public void enter(int level, String region) {
        this.level     = level;
        this.region    = region;
        this.enterTime = System.currentTimeMillis();
        this.advanced  = false;
        bg.setRegion(region);
    }

    public void update() {
        bg.update();
        long elapsed = System.currentTimeMillis() - enterTime;
        if (!advanced && elapsed >= TOTAL_MS) {
            advanced = true;
            GameStateManager.setState(GameState.PLAYING);
        }
    }

    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        long  elapsed     = System.currentTimeMillis() - enterTime;

        bg.render(g, 0, 0, W, H);

        // Dark overlay
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(0, 0, W, H);

        // ── Sector tag ────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String sectorStr = region + " SECTOR";
        g.setColor(regionColor);
        g.drawString(sectorStr,
                CX - fmS.stringWidth(sectorStr) / 2, H / 2 - 88);

        // ── Level label ───────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String lvlStr = "LEVEL  " + String.format("%02d", level);
        g.setColor(UITheme.PRIMARY);
        g.drawString(lvlStr,
                CX - fmT.stringWidth(lvlStr) / 2, H / 2 - 24);

        // Accent line under level label
        g.setColor(regionColor);
        g.fillRect(CX - 150, H / 2 - 6, 300, 2);

        // ── Countdown — 3  2  1  GO ───────────────────────────────────
        // Which number to show
        String countStr;
        Color  countColor;

        if      (elapsed < 900)  { countStr = "3"; countColor = UITheme.DANGER;    }
        else if (elapsed < 1800) { countStr = "2"; countColor = UITheme.ACCENT;    }
        else if (elapsed < 2700) { countStr = "1"; countColor = UITheme.HIGHLIGHT; }
        else                     { countStr = "GO"; countColor = UITheme.PRIMARY;  }

        // Pulse alpha: peaks at start of each second, fades toward end
        long withinSlot = elapsed % 900;
        float alpha = 1.0f - (withinSlot / 900f) * 0.35f;   // 1.0 → 0.65

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        g.setFont(UIFonts.TITLE);
        fmT = g.getFontMetrics();
        g.setColor(countColor);
        g.drawString(countStr,
                CX - fmT.stringWidth(countStr) / 2, H / 2 + 80);

        g.setComposite(old);

        // ── Progress bar ──────────────────────────────────────────────
        int barW   = 320;
        int barX   = CX - barW / 2;
        int barY   = H - 56;
        float prog = Math.min(elapsed / (float) TOTAL_MS, 1f);

        g.setColor(new Color(30, 30, 50));
        g.fillRect(barX, barY, barW, 4);

        g.setColor(regionColor);
        g.fillRect(barX, barY, (int)(barW * prog), 4);

        // Glow dot at fill edge
        int dotX = barX + (int)(barW * prog) - 2;
        if (dotX > barX) {
            g.setColor(Color.WHITE);
            g.fillRect(dotX, barY - 1, 4, 6);
        }

        // Segment ticks at 25% 50% 75%
        for (int t = 1; t <= 3; t++) {
            int tx = barX + (int)(barW * t * 0.25f);
            g.setColor(new Color(60, 60, 80));
            g.fillRect(tx - 1, barY - 2, 2, 8);
        }
    }
}
package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Main menu screen — redesigned.
 *
 * CHANGED:
 *  - Logo moved higher with much more vertical breathing room.
 *  - Title uses two-size treatment: "VAJRA" at TITLE (32px),
 *    "OVERDRIVE" at a derived 22px — creates clear word hierarchy
 *    without needing a color difference.
 *  - Cyan accent glow under the logo feels more dramatic.
 *  - Menu items spaced 60px apart (was 52px) — easier to scan.
 *  - Selected item gets a wider highlight pill with a bright left bar.
 *  - Tagline added between logo and menu for personality.
 *  - Version tag right-aligned bottom-right, not center-bottom.
 *  - Footer hint at absolute bottom edge with better key formatting.
 *  - DEBUG MENU item added (last in list) to reach the test level selector.
 */
public class MainMenuScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final String[] LABELS = {
            "NEW GAME",
            "LOAD GAME",
            "SETTINGS",
            "DEBUG MODE",
            "EXIT"
    };

    private int  selected  = 0;
    private long enterTime = 0;

    public void enter() {
        selected  = 0;
        enterTime = System.currentTimeMillis();
    }

    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_UP))   selected--;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN)) selected++;
        if (selected < 0)              selected = LABELS.length - 1;
        if (selected >= LABELS.length) selected = 0;

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
            switch (selected) {
                case 0 -> GameStateManager.setState(GameState.NEW_GAME_MENU);
                case 1 -> GameStateManager.setState(GameState.LOAD_GAME_MENU);
                case 2 -> GameStateManager.setState(GameState.SETTINGS_MENU);
                case 3 -> GameStateManager.setState(GameState.DEBUG_MENU);
                case 4 -> System.exit(0);
            }
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        long  elapsed = System.currentTimeMillis() - enterTime;
        float slide   = easeOut(Math.min(elapsed / 600f, 1f));
        int   offY    = (int)((1f - slide) * 50);

        // ── Logo block ────────────────────────────────────────────────
        // Sits in upper ~35% of screen
        int logoBlockTop = H / 6 + offY;

        // Glow backdrop — wide cyan bar behind the title
        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
        g.setColor(UITheme.PRIMARY);
        g.fillRect(CX - 200, logoBlockTop - 10, 400, 90);
        g.setComposite(orig);

        // "VAJRA" — primary title word, full 32px
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String word1 = "VAJRA";

        // Shadow
        g.setColor(new Color(0, 120, 120, 25));
        g.drawString(word1, CX - fmT.stringWidth(word1) / 2 + 2, logoBlockTop + 36 + 2);
        // Main
        g.setColor(UITheme.PRIMARY);
        g.drawString(word1, CX - fmT.stringWidth(word1) / 2, logoBlockTop + 36);

        // "OVERDRIVE" — secondary word, derived 22px — slightly smaller
        Font overdriveFont = UIFonts.TITLE.deriveFont(22f);
        g.setFont(overdriveFont);
        FontMetrics fmO = g.getFontMetrics();
        String word2 = "OVERDRIVE";

        g.setColor(new Color(0, 200, 200, 25));
        g.drawString(word2, CX - fmO.stringWidth(word2) / 2 + 2, logoBlockTop + 68 + 2);
        g.setColor(new Color(0, 200, 200));
        g.drawString(word2, CX - fmO.stringWidth(word2) / 2, logoBlockTop + 68);

        // Horizontal accent line under logo
        g.setColor(new Color(0, 255, 255, 55));
        g.fillRect(CX - 130, logoBlockTop + 82, 260, 1);
        // Thin secondary line 3px below
        g.setColor(new Color(0, 255, 255, 20));
        g.fillRect(CX - 80, logoBlockTop + 86, 160, 1);

        // Tagline — adds personality
        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fmS9 = g.getFontMetrics();
        String tagline = "BREACH THE ARMADA. RETURN HOME.";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(tagline, CX - fmS9.stringWidth(tagline) / 2, logoBlockTop + 102);

        // ── Menu items ────────────────────────────────────────────────
        // Start at 54% down the screen for generous breathing room
        int menuTop   = (int)(H * 0.52f) + offY;
        int lineH     = 60;  // generous item spacing
        int pillPadX  = 20;
        int pillPadY  = 6;

        for (int i = 0; i < LABELS.length; i++) {
            boolean sel  = (i == selected);
            String label = LABELS[i];
            // Debug mode gets a distinct color treatment
            boolean isDebug = (i == 3);

            g.setFont(UIFonts.MENU);
            FontMetrics fmM = g.getFontMetrics();

            String display = (sel ? ">  " : "   ") + label;
            int drawX = CX - fmM.stringWidth(display) / 2;
            int drawY = menuTop + i * lineH;

            if (sel) {
                // Wide pill highlight
                int pillW = fmM.stringWidth(display) + pillPadX * 2;
                int pillX = drawX - pillPadX;
                int pillY = drawY - fmM.getAscent() - pillPadY;
                int pillH = fmM.getHeight() + pillPadY * 2;

                // Pill fill
                g.setColor(new Color(0, 255, 255, 14));
                g.fillRect(pillX, pillY, pillW, pillH);

                // Left accent bar — thicker at 4px
                g.setColor(isDebug ? UITheme.SECONDARY : UITheme.ACCENT);
                g.fillRect(pillX, pillY, 4, pillH);

                // Bottom edge of pill
                g.setColor(new Color(0, 255, 255, 40));
                g.fillRect(pillX + 4, pillY + pillH - 1, pillW - 4, 1);
            }

            // Item text
            Color textCol;
            if (sel) {
                textCol = isDebug ? UITheme.SECONDARY : UITheme.ACCENT;
            } else {
                textCol = isDebug ? new Color(140, 100, 220) : UITheme.PRIMARY;
            }
            g.setColor(textCol);
            g.drawString(display, drawX, drawY);

            // DEBUG label tag — small badge next to the debug item
            if (isDebug && !sel) {
                g.setFont(UIFonts.SMALL.deriveFont(8f));
                FontMetrics fmTag = g.getFontMetrics();
                String tag = "DEV";
                int tagX = drawX + fmM.stringWidth(display) + 8;
                int tagY = drawY - fmM.getAscent() + 2;
                g.setColor(new Color(120, 80, 200, 160));
                g.fillRect(tagX - 2, tagY, fmTag.stringWidth(tag) + 4, fmTag.getHeight() - 2);
                g.setColor(new Color(200, 160, 255, 200));
                g.drawString(tag, tagX, drawY - 2);
            }
        }

        // ── Version tag — bottom right ────────────────────────────────
        g.setFont(UIFonts.SMALL.deriveFont(8f));
        FontMetrics fmVer = g.getFontMetrics();
        String ver = "v0.1  ALPHA";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(ver, W - PAD(fmVer.stringWidth(ver)), H - 8);

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fmF = g.getFontMetrics();
        String footer = "UP/DOWN  select    ENTER  confirm    F  fullscreen";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmF.stringWidth(footer) / 2, H - 20);
    }

    private int PAD(int textW) { return textW + 14; }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }
}
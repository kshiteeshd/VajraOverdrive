package ui.menu;

import config.LayoutConfig;
import ui.SpaceBackground;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;

/**
 * Shared menu rendering utilities.
 *
 * CHANGED:
 *  - render() uses UIFonts.MENU (Press Start 2P) explicitly instead
 *    of inheriting whatever font was last set by the caller.
 *  - drawSectionLabel() uses UIFonts.SMALL explicitly.
 *  - Pill background and accent bar geometry unchanged — they work
 *    correctly with Press Start 2P metrics.
 */
public class MenuButton {

    public String  text;
    public int     x, y, width, height;
    public boolean selected;

    public MenuButton(String text, int x, int y, int w, int h) {
        this.text   = text;
        this.x      = x;
        this.y      = y;
        this.width  = w;
        this.height = h;
    }

    // ── Shared background ─────────────────────────────────────────────
    /**
     * Renders and updates the shared menu space background.
     * Call at the start of every menu screen's render() method.
     */
    public static void renderBackground(Graphics2D g) {
        SpaceBackground bg = SpaceBackground.getMenuBackground();
        bg.update();
        bg.render(g, 0, 0,
                LayoutConfig.WINDOW_WIDTH,
                LayoutConfig.WINDOW_HEIGHT);
    }

    // ── Single button ─────────────────────────────────────────────────
    public void render(Graphics2D g) {
        g.setFont(UIFonts.MENU);
        FontMetrics fm = g.getFontMetrics();

        String display = (selected ? ">  " : "   ") + text;
        int drawX = (LayoutConfig.WINDOW_WIDTH
                - fm.stringWidth(display)) / 2;

        if (selected) {
            // Pill background
            int pillW = fm.stringWidth(display) + 28;
            int pillX = drawX - 14;
            int pillY = y - fm.getAscent() - 4;
            int pillH = fm.getHeight() + 8;

            g.setColor(new Color(0, 255, 255, 18));
            g.fillRect(pillX, pillY, pillW, pillH);

            // Left accent bar
            g.setColor(UITheme.ACCENT);
            g.fillRect(pillX, pillY, 3, pillH);

            // Bottom edge line
            g.setColor(new Color(0, 255, 255, 45));
            g.fillRect(pillX + 3, pillY + pillH - 1,
                    pillW - 3, 1);
        }

        g.setColor(selected ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(display, drawX, y);
    }

    // ── Section header label ──────────────────────────────────────────
    public static void drawSectionLabel(Graphics2D g,
                                        String text,
                                        int cx, int y) {
        g.setFont(UIFonts.SMALL);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int lx = cx - tw / 2;

        // Side lines
        g.setColor(new Color(0, 255, 255, 35));
        g.fillRect(lx - 32, y - 4, 26, 1);
        g.fillRect(lx + tw + 6, y - 4, 26, 1);

        g.setColor(UITheme.TEXT_DIM);
        g.drawString(text, lx, y);
    }
}
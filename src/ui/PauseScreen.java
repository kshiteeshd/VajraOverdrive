package ui;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * In-game pause overlay.
 *
 * Drawn on top of the frozen game world. Semi-transparent black panel,
 * PAUSED title, and two options: RESUME and QUIT TO MENU.
 *
 * Controls:
 *   ESC or ENTER (on RESUME) — resume
 *   UP / DOWN               — navigate
 *   ENTER                   — confirm selection
 */
public class PauseScreen {

    private static final int VW = LayoutConfig.VIRTUAL_WIDTH;
    private static final int VH = LayoutConfig.VIRTUAL_HEIGHT;

    // 0 = RESUME, 1 = QUIT TO MENU
    private int selected = 0;

    public void enter() {
        selected = 0;
    }

    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            GameStateManager.setState(GameState.PLAYING);
            return;
        }

        if (InputManager.isKeyPressed(KeyEvent.VK_UP))
            selected = (selected + 1) % 2;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN))
            selected = (selected + 1) % 2;

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
            if (selected == 0) {
                GameStateManager.setState(GameState.PLAYING);
            } else {
                GameStateManager.setState(GameState.MAIN_MENU);
            }
        }
    }

    public void render(Graphics2D g) {
        // Darken the frozen game frame behind
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, VW, VH);

        // Panel
        int pw = 280, ph = 160;
        int px = VW / 2 - pw / 2;
        int py = VH / 2 - ph / 2;

        g.setColor(new Color(6, 6, 14, 230));
        g.fillRect(px, py, pw, ph);

        g.setColor(UITheme.PRIMARY);
        g.fillRect(px,      py,      pw, 2);
        g.fillRect(px,      py+ph-1, pw, 1);
        g.fillRect(px,      py,      1,  ph);
        g.fillRect(px+pw-1, py,      1,  ph);

        // Title
        g.setFont(UIFonts.MENU);
        FontMetrics fm = g.getFontMetrics();
        String title = "PAUSED";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, VW / 2 - fm.stringWidth(title) / 2, py + 38);

        // Divider
        g.setColor(new Color(0, 255, 255, 25));
        g.fillRect(px + 20, py + 50, pw - 40, 1);

        // Menu items
        String[] items = { "RESUME", "QUIT TO MENU" };
        int itemY = py + 80;
        for (int i = 0; i < items.length; i++) {
            boolean sel = (i == selected);
            g.setFont(UIFonts.SMALL.deriveFont(10f));
            fm = g.getFontMetrics();
            String label = (sel ? ">  " : "   ") + items[i];
            g.setColor(sel ? UITheme.PRIMARY : UITheme.TEXT_DIM);
            g.drawString(label, VW / 2 - fm.stringWidth(label) / 2, itemY + i * 28);
        }

        // Hint
        g.setFont(UIFonts.SMALL.deriveFont(8f));
        fm = g.getFontMetrics();
        String hint = "ESC  resume   UP/DOWN  navigate   ENTER  confirm";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(hint, VW / 2 - fm.stringWidth(hint) / 2, py + ph - 10);
    }
}

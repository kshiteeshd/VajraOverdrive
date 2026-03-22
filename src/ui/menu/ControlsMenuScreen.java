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
 * Standalone controls reference screen.
 * Reachable via GameState.CONTROL_MENU.
 * The Settings screen also has an inline controls panel —
 * this screen is kept for any direct navigation that needs it.
 */
public class ControlsMenuScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final String[][] BINDINGS = {
            { "ARROW KEYS",  "MOVE SHIP"        },
            { "SPACE",       "FIRE"              },
            { "Z",           "FIRE  (alt)"       },
            { "F",           "TOGGLE FULLSCREEN" },
            { "ENTER",       "CONFIRM"           },
            { "ESC",         "BACK / PAUSE"      },
    };

    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER) ||
                InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.MAIN_MENU);
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "CONTROLS";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, H / 5);

        g.setColor(new Color(0, 255, 255, 40));
        g.fillRect(CX - 110, H / 5 + 14, 220, 1);

        // ── Binding table ─────────────────────────────────────────────
        int tableW  = 480;
        int tableX  = CX - tableW / 2;
        int rowH    = 44;
        int colMid  = tableX + tableW / 2;
        int startY  = H / 2 - (BINDINGS.length * rowH) / 2;

        for (int i = 0; i < BINDINGS.length; i++) {
            int     ry     = startY + i * rowH;
            String  key    = BINDINGS[i][0];
            String  action = BINDINGS[i][1];
            boolean even   = (i % 2 == 0);

            // Row tint
            if (!even) {
                g.setColor(new Color(255, 255, 255, 5));
                g.fillRect(tableX, ry - 18, tableW, rowH - 2);
            }

            // Key — right-aligned in left column
            g.setFont(UIFonts.BODY);
            FontMetrics fmB = g.getFontMetrics();
            g.setColor(UITheme.ACCENT);
            g.drawString(key, colMid - fmB.stringWidth(key) - 18, ry);

            // Divider
            g.setFont(UIFonts.SMALL);
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString("|", colMid - 3, ry);

            // Action — left-aligned in right column
            g.setFont(UIFonts.BODY);
            g.setColor(UITheme.PRIMARY);
            g.drawString(action, colMid + 14, ry);
        }

        // ── Separator ─────────────────────────────────────────────────
        int sepY = startY + BINDINGS.length * rowH + 16;
        g.setColor(UITheme.PANEL_SEP);
        g.fillRect(tableX, sepY, tableW, 1);

        // ── Back prompt ───────────────────────────────────────────────
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String back = "> BACK";
        g.setColor(UITheme.ACCENT);
        g.drawString(back, CX - fmM.stringWidth(back) / 2, sepY + 40);

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String footer = "ENTER or ESC  to return";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmS.stringWidth(footer) / 2, H - 16);
    }
}
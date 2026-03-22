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
 * Controls reference screen.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P).
 *  - Binding table uses SMALL (8px) for both key and action columns
 *    so nothing overflows the virtual width.
 *  - Row height increased to 32px for 8px Press Start 2P line spacing.
 *  - Back navigation goes to SETTINGS_MENU (this screen is reached
 *    from Settings — returning to MAIN_MENU skipped a step).
 *  - Divider and separator lines use UITheme colors consistently.
 *  - Footer double-space between key and action.
 */
public class ControlsMenuScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final String[][] BINDINGS = {
            { "ARROW  KEYS",  "MOVE  SHIP"         },
            { "SPACE",        "FIRE"                },
            { "Z",            "FIRE  (alt)"         },
            { "F",            "TOGGLE  FULLSCREEN"  },
            { "ENTER",        "CONFIRM"             },
            { "ESC",          "BACK / PAUSE"        },
    };

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)
                || InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.SETTINGS_MENU);
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "CONTROLS";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title,
                CX - fmT.stringWidth(title) / 2,
                H / 5);

        g.setColor(new Color(0, 255, 255, 35));
        g.fillRect(CX - 120, H / 5 + 14, 240, 1);

        // ── Binding table ─────────────────────────────────────────────
        int tableW   = 500;
        int tableX   = CX - tableW / 2;
        int rowH     = 32;
        int colMid   = tableX + tableW / 2;
        int startY   = H / 2 - (BINDINGS.length * rowH) / 2;

        for (int i = 0; i < BINDINGS.length; i++) {
            int     ry     = startY + i * rowH;
            String  key    = BINDINGS[i][0];
            String  action = BINDINGS[i][1];
            boolean even   = (i % 2 == 0);

            // Alternating row tint
            if (!even) {
                g.setColor(new Color(255, 255, 255, 4));
                g.fillRect(tableX, ry - 20, tableW, rowH - 2);
            }

            // Key — right-aligned in left column
            g.setFont(UIFonts.SMALL);
            FontMetrics fmS = g.getFontMetrics();
            g.setColor(UITheme.ACCENT);
            g.drawString(key,
                    colMid - fmS.stringWidth(key) - 20,
                    ry);

            // Divider pipe
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString("|", colMid - 4, ry);

            // Action — left-aligned in right column
            g.setColor(UITheme.PRIMARY);
            g.drawString(action, colMid + 16, ry);
        }

        // ── Separator ─────────────────────────────────────────────────
        int sepY = startY + BINDINGS.length * rowH + 18;
        g.setColor(UITheme.PANEL_SEP);
        g.fillRect(tableX, sepY, tableW, 1);

        // ── Back row ──────────────────────────────────────────────────
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String back = ">  BACK";
        g.setColor(UITheme.ACCENT);
        g.drawString(back,
                CX - fmM.stringWidth(back) / 2,
                sepY + 40);

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String footer = "ENTER  or  ESC  to return";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer,
                CX - fmS.stringWidth(footer) / 2,
                H - 16);
    }
}
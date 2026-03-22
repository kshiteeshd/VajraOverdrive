package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import save.GameMode;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

public class NewGameMenuScreen {

    private static final int W  = LayoutConfig.WINDOW_WIDTH;
    private static final int H  = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX = W / 2;

    // 0 = CAMPAIGN, 1 = ENDLESS, 2 = BACK
    private int selected = 0;

    private static final String[] LABELS = { "CAMPAIGN", "ENDLESS", "BACK" };
    private static final String[] SUBS   = {
            "25 levels  |  story  |  auto-save",
            "infinite waves  |  score attack  |  auto-save",
            ""
    };

    public void enter() { selected = 0; }

    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_UP))   selected--;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN)) selected++;
        if (selected < 0)              selected = LABELS.length - 1;
        if (selected >= LABELS.length) selected = 0;

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.MAIN_MENU);

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
            switch (selected) {
                case 0 -> {
                    PendingGameStart.gameMode  = GameMode.CAMPAIGN;
                    PendingGameStart.isNewGame = true;
                    GameStateManager.setState(GameState.NAME_ENTRY);
                }
                case 1 -> {
                    PendingGameStart.gameMode  = GameMode.ENDLESS;
                    PendingGameStart.isNewGame = true;
                    GameStateManager.setState(GameState.NAME_ENTRY);
                }
                case 2 -> GameStateManager.setState(GameState.MAIN_MENU);
            }
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "NEW GAME";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, H / 4);

        g.setColor(new Color(0, 255, 255, 40));
        g.fillRect(CX - 100, H / 4 + 14, 200, 1);

        // ── Mode cards ────────────────────────────────────────────────
        int cardW = 420;
        int cardH = 80;
        int cardX = CX - cardW / 2;
        int startY = H / 2 - cardH - 16;
        int gap    = 20;

        // Only 2 game mode cards + back
        for (int i = 0; i < LABELS.length; i++) {
            boolean sel = (i == selected);
            int cy = startY + i * (cardH + gap);

            if (i < 2) {
                // Mode card
                g.setColor(sel ? new Color(0, 40, 40) : new Color(8, 8, 16));
                g.fillRect(cardX, cy, cardW, cardH);

                // Border
                g.setColor(sel ? UITheme.ACCENT : new Color(0, 255, 255, 30));
                g.fillRect(cardX,              cy,          cardW, 1);
                g.fillRect(cardX,              cy + cardH - 1, cardW, 1);
                g.fillRect(cardX,              cy,          1, cardH);
                g.fillRect(cardX + cardW - 1,  cy,          1, cardH);

                // Left accent
                g.setColor(sel ? UITheme.ACCENT : UITheme.TEXT_FAINT);
                g.fillRect(cardX, cy, 4, cardH);

                // Label
                g.setFont(UIFonts.MENU);
                FontMetrics fmM = g.getFontMetrics();
                g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
                g.drawString(LABELS[i], cardX + 20, cy + 30);

                // Sub-label
                g.setFont(UIFonts.SMALL);
                FontMetrics fmS = g.getFontMetrics();
                g.setColor(sel ? UITheme.TEXT_DIM : UITheme.TEXT_FAINT);
                g.drawString(SUBS[i], cardX + 20, cy + 56);

                // Arrow indicator
                if (sel) {
                    g.setFont(UIFonts.MENU);
                    g.setColor(UITheme.ACCENT);
                    g.drawString(">", cardX + cardW - 28, cy + 30);
                }

            } else {
                // Back option
                g.setFont(UIFonts.MENU);
                FontMetrics fmM = g.getFontMetrics();
                String back = (sel ? "> " : "  ") + "BACK";
                g.setColor(sel ? UITheme.ACCENT : UITheme.TEXT_DIM);
                g.drawString(back, CX - fmM.stringWidth(back) / 2,
                        cy + cardH / 2);
            }
        }

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String footer = "UP / DOWN   navigate      ENTER   select      ESC   back";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmS.stringWidth(footer) / 2, H - 16);
    }
}
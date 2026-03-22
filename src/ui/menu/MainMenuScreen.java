package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class MainMenuScreen {

    private static final int W  = LayoutConfig.WINDOW_WIDTH;
    private static final int H  = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX = W / 2;

    private static final String[] LABELS = {
            "NEW GAME",
            "LOAD GAME",
            "SETTINGS",
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
                case 3 -> System.exit(0);
            }
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        long elapsed = System.currentTimeMillis() - enterTime;
        float slide  = easeOut(Math.min(elapsed / 500f, 1f));
        int   offY   = (int)((1f - slide) * 40);   // items slide up on enter

        // ── Logo ──────────────────────────────────────────────────────
        int logoY = H / 5 + offY;

        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();

        // Shadow layer
        g.setColor(new Color(0, 180, 180, 35));
        g.drawString("VAJRA",     CX - fmT.stringWidth("VAJRA")     / 2 + 2, logoY + 2);
        g.drawString("OVERDRIVE", CX - fmT.stringWidth("OVERDRIVE") / 2 + 2, logoY + 42 + 2);

        // Main text
        g.setColor(UITheme.PRIMARY);
        g.drawString("VAJRA", CX - fmT.stringWidth("VAJRA") / 2, logoY);
        g.setColor(new Color(0, 210, 210));
        g.drawString("OVERDRIVE", CX - fmT.stringWidth("OVERDRIVE") / 2, logoY + 42);

        // Underline
        g.setColor(new Color(0, 255, 255, 45));
        g.fillRect(CX - 130, logoY + 56, 260, 1);

        // Version tag
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String ver = "v0.1  ALPHA";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(ver, CX - fmS.stringWidth(ver) / 2, logoY + 70);

        // ── Menu items ────────────────────────────────────────────────
        int startY  = H / 2 + 30 + offY;
        int lineH   = 52;

        for (int i = 0; i < LABELS.length; i++) {
            boolean sel = (i == selected);
            String  display = (sel ? "> " : "  ") + LABELS[i];

            g.setFont(UIFonts.MENU);
            FontMetrics fmM = g.getFontMetrics();
            int drawX = CX - fmM.stringWidth(display) / 2;
            int drawY = startY + i * lineH;

            if (sel) {
                // Pill
                int pillW = fmM.stringWidth(display) + 28;
                int pillX = drawX - 14;
                int pillY = drawY - fmM.getAscent() - 4;
                int pillH = fmM.getHeight() + 8;
                g.setColor(new Color(0, 255, 255, 18));
                g.fillRect(pillX, pillY, pillW, pillH);
                g.setColor(UITheme.ACCENT);
                g.fillRect(pillX, pillY, 3, pillH);
                g.setColor(new Color(0, 255, 255, 45));
                g.fillRect(pillX + 3, pillY + pillH - 1, pillW - 3, 1);
            }

            g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
            g.drawString(display, drawX, drawY);
        }

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String footer = "UP / DOWN   navigate      ENTER   select      F   fullscreen";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmS.stringWidth(footer) / 2, H - 16);
    }

    private float easeOut(float t) { return 1f - (1f - t) * (1f - t); }
}
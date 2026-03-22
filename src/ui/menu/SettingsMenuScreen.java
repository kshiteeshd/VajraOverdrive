package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import save.SettingsManager;
import save.SettingsProfile;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

public class SettingsMenuScreen {

    private static final int W  = LayoutConfig.WINDOW_WIDTH;
    private static final int H  = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX = W / 2;

    // Row indices
    private static final int ROW_CONTROLS   = 0;
    private static final int ROW_SOUND      = 1;
    private static final int ROW_MUSIC      = 2;
    private static final int ROW_RESOLUTION = 3;
    private static final int ROW_BRIGHTNESS = 4;
    private static final int ROW_BACK       = 5;
    private static final int ROW_COUNT      = 6;

    private int selected = 0;

    public void enter() { selected = 0; }

    public void update() {
        SettingsProfile s = SettingsManager.get();

        if (InputManager.isKeyPressed(KeyEvent.VK_UP))   selected--;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN)) selected++;
        if (selected < 0)          selected = ROW_COUNT - 1;
        if (selected >= ROW_COUNT) selected = 0;

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE) ||
                (selected == ROW_BACK &&
                        InputManager.isKeyPressed(KeyEvent.VK_ENTER))) {
            SettingsManager.save();
            GameStateManager.setState(GameState.MAIN_MENU);
            return;
        }

        // LEFT / RIGHT adjust values
        boolean left  = InputManager.isKeyPressed(KeyEvent.VK_LEFT);
        boolean right = InputManager.isKeyPressed(KeyEvent.VK_RIGHT);
        boolean enter = InputManager.isKeyPressed(KeyEvent.VK_ENTER);

        switch (selected) {
            case ROW_CONTROLS -> {
                if (enter) GameStateManager.setState(GameState.SETTINGS_MENU); // shows controls inline
                // Handled below in render — controls are drawn in-screen
            }
            case ROW_SOUND -> {
                if (enter || left || right) {
                    s.soundEnabled = !s.soundEnabled;
                }
                if (left  && s.soundVolume > 0)   s.soundVolume -= 10;
                if (right && s.soundVolume < 100)  s.soundVolume += 10;
            }
            case ROW_MUSIC -> {
                if (enter || left || right) {
                    if (enter) s.musicEnabled = !s.musicEnabled;
                }
                if (left  && s.musicVolume > 0)   s.musicVolume -= 10;
                if (right && s.musicVolume < 100)  s.musicVolume += 10;
            }
            case ROW_RESOLUTION -> {
                if (right || enter)
                    s.resolutionIdx = (s.resolutionIdx + 1)
                            % SettingsProfile.RESOLUTION_LABELS.length;
                if (left)
                    s.resolutionIdx = (s.resolutionIdx - 1
                            + SettingsProfile.RESOLUTION_LABELS.length)
                            % SettingsProfile.RESOLUTION_LABELS.length;
            }
            case ROW_BRIGHTNESS -> {
                if (left  && s.brightness > 0.5f) s.brightness = Math.round((s.brightness - 0.1f) * 10) / 10f;
                if (right && s.brightness < 1.5f) s.brightness = Math.round((s.brightness + 0.1f) * 10) / 10f;
            }
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);
        SettingsProfile s = SettingsManager.get();

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "SETTINGS";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, 60);
        g.setColor(new Color(0, 255, 255, 40));
        g.fillRect(CX - 90, 74, 180, 1);

        // ── Rows ──────────────────────────────────────────────────────
        int rowH   = 62;
        int startY = 110;
        int tableW = 640;
        int tableX = CX - tableW / 2;

        // Controls row
        drawRow(g, ROW_CONTROLS, tableX, startY, tableW, rowH,
                "VIEW CONTROLS",
                "ENTER to view key bindings",
                selected == ROW_CONTROLS);

        // Sound row
        drawToggleRow(g, ROW_SOUND, tableX, startY + rowH, tableW, rowH,
                "SOUND EFFECTS",
                s.soundEnabled,
                s.soundVolume,
                "volume",
                selected == ROW_SOUND);

        // Music row
        drawToggleRow(g, ROW_MUSIC, tableX, startY + rowH * 2, tableW, rowH,
                "MUSIC",
                s.musicEnabled,
                s.musicVolume,
                "volume  (coming soon)",
                selected == ROW_MUSIC);

        // Resolution row
        drawCycleRow(g, ROW_RESOLUTION,
                tableX, startY + rowH * 3, tableW, rowH,
                "RESOLUTION",
                SettingsProfile.RESOLUTION_LABELS[s.resolutionIdx],
                selected == ROW_RESOLUTION);

        // Brightness row
        drawSliderRow(g, ROW_BRIGHTNESS,
                tableX, startY + rowH * 4, tableW, rowH,
                "BRIGHTNESS",
                s.brightness, 0.5f, 1.5f,
                selected == ROW_BRIGHTNESS);

        // Back row
        int backY = startY + rowH * 5 + 8;
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String back = (selected == ROW_BACK ? "> " : "  ") + "BACK";
        g.setColor(selected == ROW_BACK ? UITheme.ACCENT : UITheme.TEXT_DIM);
        g.drawString(back, CX - fmM.stringWidth(back) / 2, backY + 20);

        // ── Footer ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String footer = "UP/DOWN  navigate    LEFT/RIGHT  adjust    ESC  back";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmS.stringWidth(footer) / 2, H - 16);

        // ── Controls sub-panel (inline, shown when ROW_CONTROLS selected) ──
        if (selected == ROW_CONTROLS) drawControlsPanel(g);
    }

    // ── Row helpers ───────────────────────────────────────────────────

    private void drawRow(Graphics2D g, int rowIdx, int x, int y,
                         int w, int h, String label, String sub, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);
        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 16, y + 28);
        g.setFont(UIFonts.SMALL);
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(sub, x + 16, y + 50);
    }

    private void drawToggleRow(Graphics2D g, int rowIdx, int x, int y,
                               int w, int h, String label,
                               boolean enabled, int volume,
                               String sub, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);
        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 16, y + 28);

        // ON/OFF badge
        String toggle = enabled ? "ON" : "OFF";
        Color  togCol = enabled ? UITheme.HIGHLIGHT : UITheme.DANGER;
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        g.setColor(togCol);
        g.drawString(toggle, x + w - fmS.stringWidth(toggle) - 16, y + 22);

        // Volume bar
        int barW  = 120;
        int barX  = x + w - barW - 16;
        int barY  = y + 36;
        int fillW = (int)(barW * (volume / 100f));
        g.setColor(new Color(30, 30, 50));
        g.fillRect(barX, barY, barW, 6);
        g.setColor(enabled ? UITheme.PRIMARY : UITheme.TEXT_FAINT);
        g.fillRect(barX, barY, fillW, 6);

        g.setFont(UIFonts.SMALL);
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(sub, x + 16, y + 50);
    }

    private void drawCycleRow(Graphics2D g, int rowIdx, int x, int y,
                              int w, int h, String label,
                              String value, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);
        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 16, y + 28);

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String display = "< " + value + " >";
        g.setColor(sel ? UITheme.HIGHLIGHT : UITheme.TEXT_DIM);
        g.drawString(display, x + w - fmS.stringWidth(display) - 16, y + 28);

        g.setColor(UITheme.TEXT_FAINT);
        g.drawString("LEFT / RIGHT  cycle", x + 16, y + 50);
    }

    private void drawSliderRow(Graphics2D g, int rowIdx, int x, int y,
                               int w, int h, String label,
                               float value, float min, float max, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);
        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 16, y + 28);

        // Slider bar
        int barW  = 160;
        int barX  = x + w - barW - 16;
        int barY  = y + 20;
        float t   = (value - min) / (max - min);
        int fillW = (int)(barW * t);

        g.setColor(new Color(30, 30, 50));
        g.fillRect(barX, barY, barW, 6);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.fillRect(barX, barY, fillW, 6);

        // Thumb
        g.setColor(UITheme.ACCENT);
        g.fillRect(barX + fillW - 2, barY - 3, 4, 12);

        // Value label
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String valStr = String.format("%.1f", value);
        g.setColor(sel ? UITheme.HIGHLIGHT : UITheme.TEXT_DIM);
        g.drawString(valStr, barX + barW + 8, barY + 8);

        g.setColor(UITheme.TEXT_FAINT);
        g.drawString("LEFT / RIGHT  adjust", x + 16, y + 50);
    }

    private void drawRowBg(Graphics2D g, int x, int y,
                           int w, int h, boolean sel) {
        g.setColor(sel ? new Color(0, 30, 30) : new Color(8, 8, 14));
        g.fillRect(x, y, w, h);
        g.setColor(sel ? UITheme.ACCENT : new Color(0, 255, 255, 20));
        g.fillRect(x, y, w, 1);
        g.fillRect(x, y + h - 1, w, 1);
        // Left accent
        g.setColor(sel ? UITheme.ACCENT : UITheme.TEXT_FAINT);
        g.fillRect(x, y, 4, h);
    }

    // ── Inline controls reference panel ──────────────────────────────
    private void drawControlsPanel(Graphics2D g) {
        int pw = 500, ph = 260;
        int px = CX - pw / 2, py = H / 2 - ph / 2;

        // Overlay
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, W, H);

        // Panel
        g.setColor(new Color(6, 12, 18));
        g.fillRect(px, py, pw, ph);
        g.setColor(UITheme.PRIMARY);
        g.fillRect(px, py, pw, 2);
        g.fillRect(px, py + ph - 1, pw, 1);
        g.fillRect(px, py, 1, ph);
        g.fillRect(px + pw - 1, py, 1, ph);

        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String head = "CONTROLS";
        g.setColor(UITheme.PRIMARY);
        g.drawString(head, CX - fmM.stringWidth(head) / 2, py + 28);

        g.setColor(new Color(0, 255, 255, 35));
        g.fillRect(px + 20, py + 36, pw - 40, 1);

        String[][] binds = {
                { "ARROWS",  "MOVE SHIP"     },
                { "SPACE",   "FIRE"           },
                { "Z",       "FIRE (alt)"     },
                { "F",       "FULLSCREEN"     },
                { "ESC",     "BACK / PAUSE"   },
                { "ENTER",   "CONFIRM"        },
        };

        int bx    = px + 30;
        int right = px + pw - 30;
        int by    = py + 60;
        int blineH = 28;

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();

        for (String[] bind : binds) {
            g.setColor(UITheme.ACCENT);
            g.drawString(bind[0], bx, by);
            g.setColor(UITheme.TEXT_DIM);
            g.drawString("|", CX - 4, by);
            g.setColor(UITheme.PRIMARY);
            g.drawString(bind[1], CX + 12, by);
            by += blineH;
        }

        g.setColor(UITheme.TEXT_FAINT);
        String close = "UP / DOWN to close this panel";
        g.drawString(close, CX - fmS.stringWidth(close) / 2, py + ph - 14);
    }
}
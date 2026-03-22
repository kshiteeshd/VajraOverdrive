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

/**
 * Settings menu.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P) — no inline Font objects.
 *  - Row height increased to 66px for Press Start 2P metrics.
 *  - Toggle ON/OFF label uses SMALL (8px) so it fits beside the bar.
 *  - Slider value label uses SMALL consistently.
 *  - Footer double-space between key and action.
 *  - Controls row subtitle updated to match ControlsMenuScreen.
 */
public class SettingsMenuScreen {

    private static final int W  = LayoutConfig.WINDOW_WIDTH;
    private static final int H  = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX = W / 2;

    private static final int ROW_CONTROLS   = 0;
    private static final int ROW_SOUND      = 1;
    private static final int ROW_MUSIC      = 2;
    private static final int ROW_RESOLUTION = 3;
    private static final int ROW_BRIGHTNESS = 4;
    private static final int ROW_BACK       = 5;
    private static final int ROW_COUNT      = 6;

    private int selected = 0;

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter() { selected = 0; }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        SettingsProfile s = SettingsManager.get();

        if (InputManager.isKeyPressed(KeyEvent.VK_UP))   selected--;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN)) selected++;
        if (selected < 0)          selected = ROW_COUNT - 1;
        if (selected >= ROW_COUNT) selected = 0;

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE)
                || (selected == ROW_BACK
                && InputManager.isKeyPressed(KeyEvent.VK_ENTER))) {
            SettingsManager.save();
            GameStateManager.setState(GameState.MAIN_MENU);
            return;
        }

        boolean left  = InputManager.isKeyPressed(KeyEvent.VK_LEFT);
        boolean right = InputManager.isKeyPressed(KeyEvent.VK_RIGHT);
        boolean enter = InputManager.isKeyPressed(KeyEvent.VK_ENTER);

        switch (selected) {
            case ROW_CONTROLS -> {
                if (enter) {
                    SettingsManager.save();
                    GameStateManager.setState(GameState.CONTROL_MENU);
                }
            }
            case ROW_SOUND -> {
                if (enter) s.soundEnabled = !s.soundEnabled;
                if (left  && s.soundVolume > 0)   s.soundVolume -= 10;
                if (right && s.soundVolume < 100)  s.soundVolume += 10;
            }
            case ROW_MUSIC -> {
                if (enter) s.musicEnabled = !s.musicEnabled;
                if (left  && s.musicVolume > 0)   s.musicVolume -= 10;
                if (right && s.musicVolume < 100)  s.musicVolume += 10;
            }
            case ROW_RESOLUTION -> {
                if (right || enter)
                    s.resolutionIdx = (s.resolutionIdx + 1)
                            % SettingsProfile.RESOLUTION_LABELS.length;
                if (left)
                    s.resolutionIdx =
                            (s.resolutionIdx - 1
                                    + SettingsProfile.RESOLUTION_LABELS.length)
                                    % SettingsProfile.RESOLUTION_LABELS.length;
            }
            case ROW_BRIGHTNESS -> {
                if (left && s.brightness > 0.5f)
                    s.brightness = Math.round(
                            (s.brightness - 0.1f) * 10) / 10f;
                if (right && s.brightness < 1.5f)
                    s.brightness = Math.round(
                            (s.brightness + 0.1f) * 10) / 10f;
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);
        SettingsProfile s = SettingsManager.get();

        // Title
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "SETTINGS";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title,
                CX - fmT.stringWidth(title) / 2, 56);

        g.setColor(new Color(0, 255, 255, 35));
        g.fillRect(CX - 100, 70, 200, 1);

        int rowH   = 66;
        int startY = 88;
        int tableW = 660;
        int tableX = CX - tableW / 2;

        drawRow(g, tableX, startY,
                tableW, rowH,
                "VIEW  CONTROLS",
                "ENTER  open controls",
                selected == ROW_CONTROLS);

        drawToggleRow(g, tableX, startY + rowH,
                tableW, rowH,
                "SOUND  FX",
                s.soundEnabled, s.soundVolume,
                "LEFT/RIGHT  volume",
                selected == ROW_SOUND);

        drawToggleRow(g, tableX, startY + rowH * 2,
                tableW, rowH,
                "MUSIC",
                s.musicEnabled, s.musicVolume,
                "coming soon",
                selected == ROW_MUSIC);

        drawCycleRow(g, tableX, startY + rowH * 3,
                tableW, rowH,
                "RESOLUTION",
                SettingsProfile.RESOLUTION_LABELS[s.resolutionIdx],
                selected == ROW_RESOLUTION);

        drawSliderRow(g, tableX, startY + rowH * 4,
                tableW, rowH,
                "BRIGHTNESS",
                s.brightness, 0.5f, 1.5f,
                selected == ROW_BRIGHTNESS);

        // Back row
        int backY = startY + rowH * 5 + 10;
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String back = (selected == ROW_BACK ? ">  " : "   ") + "BACK";
        g.setColor(selected == ROW_BACK
                ? UITheme.ACCENT : UITheme.TEXT_DIM);
        g.drawString(back,
                CX - fmM.stringWidth(back) / 2,
                backY + 22);

        // Footer
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String footer =
                "UP/DOWN  navigate    LEFT/RIGHT  adjust    ESC  back";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer,
                CX - fmS.stringWidth(footer) / 2,
                H - 16);
    }

    // ── Row helpers ───────────────────────────────────────────────────

    private void drawRow(Graphics2D g,
                         int x, int y, int w, int h,
                         String label, String sub, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);

        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 14, y + 28);

        g.setFont(UIFonts.SMALL);
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(sub, x + 14, y + 52);
    }

    private void drawToggleRow(Graphics2D g,
                               int x, int y, int w, int h,
                               String label,
                               boolean enabled, int volume,
                               String sub, boolean sel) {
        drawRowBg(g, x, y, w, h, sel);

        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 14, y + 28);

        // ON/OFF badge — right side top
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String toggle = enabled ? "ON" : "OFF";
        Color  togCol = enabled ? UITheme.HIGHLIGHT : UITheme.DANGER;
        g.setColor(togCol);
        g.drawString(toggle,
                x + w - fmS.stringWidth(toggle) - 14,
                y + 20);

        // Volume bar
        int barW  = 130;
        int barX  = x + w - barW - 14;
        int barY  = y + 30;
        int fillW = (int)(barW * (volume / 100f));

        g.setColor(new Color(25, 25, 38));
        g.fillRect(barX, barY, barW, 5);
        g.setColor(enabled ? UITheme.PRIMARY : UITheme.TEXT_FAINT);
        if (fillW > 0)
            g.fillRect(barX, barY, fillW, 5);

        g.setFont(UIFonts.SMALL);
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(sub, x + 14, y + 52);
    }

    private void drawCycleRow(Graphics2D g,
                              int x, int y, int w, int h,
                              String label, String value,
                              boolean sel) {
        drawRowBg(g, x, y, w, h, sel);

        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 14, y + 28);

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String display = "<  " + value + "  >";
        g.setColor(sel ? UITheme.HIGHLIGHT : UITheme.TEXT_DIM);
        g.drawString(display,
                x + w - fmS.stringWidth(display) - 14,
                y + 28);

        g.setColor(UITheme.TEXT_FAINT);
        g.drawString("LEFT/RIGHT  cycle", x + 14, y + 52);
    }

    private void drawSliderRow(Graphics2D g,
                               int x, int y, int w, int h,
                               String label,
                               float value, float min, float max,
                               boolean sel) {
        drawRowBg(g, x, y, w, h, sel);

        g.setFont(UIFonts.MENU);
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        g.drawString(label, x + 14, y + 28);

        int   barW  = 170;
        int   barX  = x + w - barW - 14;
        int   barY  = y + 22;
        float t     = (value - min) / (max - min);
        int   fillW = (int)(barW * t);

        // Track
        g.setColor(new Color(25, 25, 38));
        g.fillRect(barX, barY, barW, 5);

        // Fill
        g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
        if (fillW > 0)
            g.fillRect(barX, barY, fillW, 5);

        // Handle pip
        g.setColor(UITheme.ACCENT);
        g.fillRect(barX + fillW - 2, barY - 3, 4, 11);

        // Value label
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String valStr = String.format("%.1f", value);
        g.setColor(sel ? UITheme.HIGHLIGHT : UITheme.TEXT_DIM);
        g.drawString(valStr, barX + barW + 8, barY + 8);

        g.setColor(UITheme.TEXT_FAINT);
        g.drawString("LEFT/RIGHT  adjust", x + 14, y + 52);
    }

    private void drawRowBg(Graphics2D g,
                           int x, int y, int w, int h,
                           boolean sel) {
        g.setColor(sel
                ? new Color(0, 28, 28)
                : new Color(7, 7, 13));
        g.fillRect(x, y, w, h);

        // Top border
        g.setColor(sel
                ? UITheme.ACCENT
                : new Color(0, 255, 255, 18));
        g.fillRect(x, y, w, 1);

        // Bottom border
        g.fillRect(x, y + h - 1, w, 1);

        // Left accent stripe
        g.setColor(sel ? UITheme.ACCENT : UITheme.TEXT_FAINT);
        g.fillRect(x, y, 4, h);
    }
}
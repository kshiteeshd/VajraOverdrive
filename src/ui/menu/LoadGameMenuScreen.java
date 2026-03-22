package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import save.GameMode;
import save.PlayerProfile;
import save.ProfileManager;
import save.SaveManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

public class LoadGameMenuScreen {

    private static final int W          = LayoutConfig.WINDOW_WIDTH;
    private static final int H          = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX         = W / 2;
    private static final int SLOT_COUNT = SaveManager.MAX_SLOTS;
    private static final int ROWS       = SLOT_COUNT + 1; // slots + BACK

    private int     selected         = 0;
    private boolean confirmingDelete = false;
    private int     deleteSlot       = -1;

    // Cached once on enter() — no per-frame disk reads
    private final PlayerProfile[] cache = new PlayerProfile[SLOT_COUNT + 1];

    public void enter() {
        selected         = 0;
        confirmingDelete = false;
        deleteSlot       = -1;
        refreshCache();
    }

    private void refreshCache() {
        for (int i = 1; i <= SLOT_COUNT; i++)
            cache[i] = SaveManager.loadProfile(i);
    }

    public void update() {
        // ── Delete confirmation ───────────────────────────────────────
        if (confirmingDelete) {
            if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
                ProfileManager.deleteProfile(deleteSlot);
                confirmingDelete = false;
                deleteSlot       = -1;
                refreshCache();
            }
            if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE)) {
                confirmingDelete = false;
                deleteSlot       = -1;
            }
            return;
        }

        // ── Navigation ───────────────────────────────────────────────
        if (InputManager.isKeyPressed(KeyEvent.VK_UP))   selected--;
        if (InputManager.isKeyPressed(KeyEvent.VK_DOWN)) selected++;
        if (selected < 0)      selected = ROWS - 1;
        if (selected >= ROWS)  selected = 0;

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.MAIN_MENU);

        // DELETE key on an occupied slot
        if (InputManager.isKeyPressed(KeyEvent.VK_DELETE)
                && selected < SLOT_COUNT
                && cache[selected + 1] != null) {
            confirmingDelete = true;
            deleteSlot       = selected + 1;
            return;
        }

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)) {
            if (selected == SLOT_COUNT) {
                // BACK row
                GameStateManager.setState(GameState.MAIN_MENU);
                return;
            }
            int slot = selected + 1;
            if (cache[slot] != null) {
                // Load and continue
                ProfileManager.loadSlot(slot);
                PlayerProfile p = ProfileManager.getProfile();
                PendingGameStart.saveSlot  = slot;
                PendingGameStart.isNewGame = false;
                PendingGameStart.level     = p.level;
                PendingGameStart.gameMode  = p.gameMode != null
                        ? p.gameMode : GameMode.CAMPAIGN;
                GameStateManager.setState(GameState.LEVEL_LOAD);
            }
            // Empty slot — no action (greyed out)
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "LOAD GAME";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, 60);

        g.setColor(new Color(0, 255, 255, 40));
        g.fillRect(CX - 120, 74, 240, 1);

        // ── Slot cards ────────────────────────────────────────────────
        int cardW  = 680;
        int cardH  = 72;
        int cardX  = CX - cardW / 2;
        int gap    = 12;
        int startY = 100;

        for (int i = 0; i < ROWS; i++) {
            int  cy  = startY + i * (cardH + gap);
            boolean sel = (i == selected);

            if (i < SLOT_COUNT) {
                drawSlotCard(g, i + 1, cardX, cy, cardW, cardH, sel);
            } else {
                // BACK row
                g.setFont(UIFonts.MENU);
                FontMetrics fmM = g.getFontMetrics();
                String back = (sel ? "> " : "  ") + "BACK";
                g.setColor(sel ? UITheme.ACCENT : UITheme.TEXT_DIM);
                g.drawString(back, CX - fmM.stringWidth(back) / 2,
                        cy + cardH / 2 + 6);
            }
        }

        // ── Delete confirm overlay ────────────────────────────────────
        if (confirmingDelete) drawDeleteConfirm(g);

        // ── Footer ────────────────────────────────────────────────────
        if (!confirmingDelete) {
            g.setFont(UIFonts.SMALL);
            FontMetrics fmS = g.getFontMetrics();
            String footer = "ENTER  load      DELETE  remove slot      ESC  back";
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString(footer, CX - fmS.stringWidth(footer) / 2, H - 16);
        }
    }

    // ── Individual slot card ──────────────────────────────────────────
    private void drawSlotCard(Graphics2D g, int slot, int x, int y,
                              int w, int h, boolean sel) {
        PlayerProfile p      = cache[slot];
        boolean       active = (ProfileManager.getCurrentSlot() == slot);
        boolean       empty  = (p == null);

        // Background
        g.setColor(empty ? new Color(6, 6, 10)
                : sel   ? new Color(0, 28, 28)
                :          new Color(8, 8, 16));
        g.fillRect(x, y, w, h);

        // Border
        Color borderCol = sel   ? UITheme.ACCENT
                : empty ? new Color(40, 40, 60)
                :          new Color(0, 255, 255, 28);
        g.setColor(borderCol);
        g.fillRect(x,         y,         w, 1);
        g.fillRect(x,         y + h - 1, w, 1);
        g.fillRect(x,         y,         1, h);
        g.fillRect(x + w - 1, y,         1, h);

        // Left accent stripe
        Color stripe = active ? UITheme.SECONDARY
                : sel    ? UITheme.ACCENT
                : empty  ? new Color(40, 40, 60)
                :           new Color(0, 180, 180, 80);
        g.setColor(stripe);
        g.fillRect(x, y, 4, h);

        // Slot number
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("SLOT " + slot, x + 14, y + 16);

        // ACTIVE badge
        if (active) {
            g.setColor(UITheme.SECONDARY);
            String badge = "ACTIVE";
            g.drawString(badge, x + w - fmS.stringWidth(badge) - 14, y + 16);
        }

        if (empty) {
            // Empty slot
            g.setFont(UIFonts.MENU);
            FontMetrics fmM = g.getFontMetrics();
            String emptyStr = "-- EMPTY --";
            g.setColor(new Color(60, 60, 80));
            g.drawString(emptyStr, x + 14, y + 48);
        } else {
            // Pilot name
            g.setFont(UIFonts.MENU);
            FontMetrics fmM = g.getFontMetrics();
            g.setColor(sel ? UITheme.ACCENT : UITheme.PRIMARY);
            String name = p.name != null ? p.name : "???";
            g.drawString(name, x + 14, y + 50);

            // Mode tag badge
            String modeStr = p.gameMode == GameMode.ENDLESS ? "ENDLESS" : "CAMPAIGN";
            Color  modeCol = p.gameMode == GameMode.ENDLESS
                    ? UITheme.HIGHLIGHT : UITheme.SECONDARY;
            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            int modeX = x + 14 + fmM.stringWidth(name) + 14;
            g.setColor(modeCol);
            g.drawString(modeStr, modeX, y + 50);

            // Right side: level | region | score
            String detail = String.format("LVL %02d  |  %s  |  %08d",
                    p.level,
                    p.region != null ? p.region : "RED",
                    p.score);
            g.setColor(sel ? UITheme.TEXT_DIM : new Color(80, 80, 100));
            int detailX = x + w - fmS.stringWidth(detail) - 14;
            g.drawString(detail, detailX, y + 34);

            // Last played date (bottom right)
            String date = p.lastPlayedString();
            g.setColor(new Color(55, 55, 70));
            g.drawString(date, x + w - fmS.stringWidth(date) - 14, y + 54);
        }

        // Selection arrow
        if (sel && !empty) {
            g.setFont(UIFonts.MENU);
            g.setColor(UITheme.ACCENT);
            g.drawString(">", x - 20, y + h / 2 + 6);
        }
    }

    // ── Delete confirm overlay ────────────────────────────────────────
    private void drawDeleteConfirm(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, W, H);

        int bw = 460, bh = 130;
        int bx = CX - bw / 2, by = H / 2 - bh / 2;

        g.setColor(new Color(20, 6, 6));
        g.fillRect(bx, by, bw, bh);
        g.setColor(UITheme.DANGER);
        g.fillRect(bx,          by,          bw, 2);
        g.fillRect(bx,          by + bh - 1, bw, 1);
        g.fillRect(bx,          by,          1,  bh);
        g.fillRect(bx + bw - 1, by,          1,  bh);

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();

        String l1 = "DELETE SLOT " + deleteSlot + "?";
        String l2 = "THIS CANNOT BE UNDONE";
        String l3 = "ENTER = YES      ESC = NO";

        g.setColor(UITheme.DANGER);
        g.drawString(l1, CX - fmS.stringWidth(l1) / 2, by + 32);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(l2, CX - fmS.stringWidth(l2) / 2, by + 60);
        g.setColor(UITheme.ACCENT);
        g.drawString(l3, CX - fmS.stringWidth(l3) / 2, by + 100);
    }
}
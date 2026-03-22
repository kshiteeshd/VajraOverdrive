package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import save.GameMode;
import save.ProfileManager;
import save.SaveManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Pilot name entry screen.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P).
 *  - Cursor blink stateless — System.currentTimeMillis() modulo.
 *  - Card height increased to 80px for Press Start 2P metrics.
 *  - Character counter right-aligned inside card bottom-right.
 *  - Hint labels use SMALL (8px) — consistent with rest of UI.
 *  - Error state (no slots) uses SMALL for body text so it doesn't
 *    overflow the virtual width.
 */
public class NameEntryScreen {

    private static final int W       = LayoutConfig.WINDOW_WIDTH;
    private static final int H       = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX      = W / 2;
    private static final int MAX_LEN = 10;

    private String  name    = "";
    private int     slot    = -1;
    private boolean noSlots = false;

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter() {
        name    = "";
        noSlots = false;
        slot    = SaveManager.nextEmptySlot();
        if (slot == -1) noSlots = true;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (noSlots) {
            if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE)
                    || InputManager.isKeyPressed(KeyEvent.VK_ENTER))
                GameStateManager.setState(GameState.LOAD_GAME_MENU);
            return;
        }

        // A–Z
        for (char c = 'A'; c <= 'Z'; c++) {
            if (InputManager.isKeyPressed(c) && name.length() < MAX_LEN)
                name += c;
        }
        // 0–9
        for (int i = 0; i <= 9; i++) {
            if (InputManager.isKeyPressed(KeyEvent.VK_0 + i)
                    && name.length() < MAX_LEN)
                name += (char)('0' + i);
        }

        if (InputManager.isKeyPressed(KeyEvent.VK_BACK_SPACE)
                && !name.isEmpty())
            name = name.substring(0, name.length() - 1);

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.NEW_GAME_MENU);

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER)
                && !name.isEmpty()) {
            GameMode mode = PendingGameStart.gameMode;
            ProfileManager.createProfile(slot, name, mode);

            PendingGameStart.saveSlot  = slot;
            PendingGameStart.level     = 1;
            PendingGameStart.isNewGame = true;

            GameStateManager.setState(GameState.CAMPAIGN_INTRO);
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "NEW  PILOT";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title,
                CX - fmT.stringWidth(title) / 2,
                H / 4);

        // Mode + slot tag
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String modeTag = PendingGameStart.gameMode == GameMode.ENDLESS
                ? "ENDLESS" : "CAMPAIGN";
        String slotTag = noSlots
                ? "ALL  SLOTS  FULL"
                : "SLOT " + slot + "  |  " + modeTag;
        g.setColor(noSlots ? UITheme.DANGER : UITheme.SECONDARY);
        g.drawString(slotTag,
                CX - fmS.stringWidth(slotTag) / 2,
                H / 4 + 26);

        g.setColor(new Color(0, 255, 255, 30));
        g.fillRect(CX - 110, H / 4 + 38, 220, 1);

        // ── No slots error state ──────────────────────────────────────
        if (noSlots) {
            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            String err1 = "All save slots are full.";
            String err2 = "Delete a slot in LOAD GAME first.";
            g.setColor(UITheme.DANGER);
            g.drawString(err1,
                    CX - fmS.stringWidth(err1) / 2,
                    H / 2);
            g.setColor(UITheme.TEXT_DIM);
            g.drawString(err2,
                    CX - fmS.stringWidth(err2) / 2,
                    H / 2 + 28);

            boolean blinkOn = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blinkOn) {
                String back = "ENTER / ESC  back";
                g.setColor(UITheme.ACCENT);
                g.drawString(back,
                        CX - fmS.stringWidth(back) / 2,
                        H - 40);
            }
            return;
        }

        // ── Input card ────────────────────────────────────────────────
        int cardW = 400;
        int cardH = 80;
        int cardX = CX - cardW / 2;
        int cardY = H / 2 - cardH / 2 - 10;

        // Card body
        g.setColor(new Color(8, 8, 18));
        g.fillRect(cardX, cardY, cardW, cardH);

        // Top accent bar
        g.setColor(UITheme.ACCENT);
        g.fillRect(cardX, cardY, cardW, 2);

        // Side borders
        g.setColor(new Color(255, 180, 0, 35));
        g.fillRect(cardX,              cardY + 2, 1, cardH - 2);
        g.fillRect(cardX + cardW - 1,  cardY + 2, 1, cardH - 2);

        // Bottom border
        g.fillRect(cardX, cardY + cardH - 1, cardW, 1);

        // Hint above card
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String hint = "ENTER  PILOT  NAME";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(hint,
                CX - fmS.stringWidth(hint) / 2,
                cardY - 14);

        // Name text with blinking cursor
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        boolean cursorOn = (System.currentTimeMillis() / 500) % 2 == 0;
        String  display  = name + (cursorOn ? "_" : " ");

        // Center the displayed name in the card
        int textX = CX - fmM.stringWidth(name + "_") / 2;
        g.setColor(UITheme.ACCENT);
        g.drawString(display, textX, cardY + 50);

        // Character counter — bottom right of card
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String counter = name.length() + "/" + MAX_LEN;
        g.setColor(name.length() >= MAX_LEN
                ? UITheme.DANGER
                : UITheme.TEXT_FAINT);
        g.drawString(counter,
                cardX + cardW - fmS.stringWidth(counter) - 10,
                cardY + cardH - 8);

        // ── Bottom hints ──────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String h1 = "A-Z  0-9  type    BKSP  delete    ENTER  confirm";
        String h2 = "ESC  cancel";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(h1, CX - fmS.stringWidth(h1) / 2, H - 36);
        g.drawString(h2, CX - fmS.stringWidth(h2) / 2, H - 18);
    }
}
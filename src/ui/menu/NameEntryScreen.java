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

public class NameEntryScreen {

    private static final int W  = LayoutConfig.WINDOW_WIDTH;
    private static final int H  = LayoutConfig.WINDOW_HEIGHT;
    private static final int CX = W / 2;
    private static final int MAX_LEN = 10;

    private String   name     = "";
    private int      slot     = -1;     // auto-assigned
    private boolean  noSlots  = false;  // all slots full

    public void enter() {
        name    = "";
        noSlots = false;
        slot    = SaveManager.nextEmptySlot();
        if (slot == -1) noSlots = true;
    }

    public void update() {
        if (noSlots) {
            // All slots full — only option is to go back
            if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE) ||
                    InputManager.isKeyPressed(KeyEvent.VK_ENTER))
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

        if (InputManager.isKeyPressed(KeyEvent.VK_BACK_SPACE) && !name.isEmpty())
            name = name.substring(0, name.length() - 1);

        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE))
            GameStateManager.setState(GameState.NEW_GAME_MENU);

        if (InputManager.isKeyPressed(KeyEvent.VK_ENTER) && !name.isEmpty()) {
            GameMode mode = PendingGameStart.gameMode;
            ProfileManager.createProfile(slot, name, mode);

            PendingGameStart.saveSlot  = slot;
            PendingGameStart.level     = 1;
            PendingGameStart.isNewGame = true;

            if (mode == GameMode.CAMPAIGN)
                GameStateManager.setState(GameState.CAMPAIGN_INTRO);
            else
                GameStateManager.setState(GameState.CAMPAIGN_INTRO); // ENDLESS_INTRO later
        }
    }

    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "NEW PILOT";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, H / 4);

        // Mode + slot tag
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String modeTag = PendingGameStart.gameMode == GameMode.ENDLESS
                ? "ENDLESS" : "CAMPAIGN";
        String slotTag = noSlots ? "ALL SLOTS FULL"
                : "SLOT " + slot + "  |  " + modeTag;
        g.setColor(noSlots ? UITheme.DANGER : UITheme.SECONDARY);
        g.drawString(slotTag, CX - fmS.stringWidth(slotTag) / 2, H / 4 + 28);

        g.setColor(new Color(0, 255, 255, 35));
        g.fillRect(CX - 100, H / 4 + 40, 200, 1);

        if (noSlots) {
            // Error state
            g.setFont(UIFonts.BODY);
            FontMetrics fmB = g.getFontMetrics();
            String err1 = "All save slots are full.";
            String err2 = "Delete a slot in LOAD GAME first.";
            g.setColor(UITheme.DANGER);
            g.drawString(err1, CX - fmB.stringWidth(err1) / 2, H / 2);
            g.setColor(UITheme.TEXT_DIM);
            g.drawString(err2, CX - fmB.stringWidth(err2) / 2, H / 2 + 32);

            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            String back = "[ ENTER / ESC ]  BACK";
            g.setColor(UITheme.ACCENT);
            g.drawString(back, CX - fmS.stringWidth(back) / 2, H - 40);
            return;
        }

        // ── Name input card ───────────────────────────────────────────
        int cardW = 380;
        int cardH = 72;
        int cardX = CX - cardW / 2;
        int cardY = H / 2 - cardH / 2 - 10;

        // Card background
        g.setColor(new Color(10, 10, 20));
        g.fillRect(cardX, cardY, cardW, cardH);
        g.setColor(UITheme.ACCENT);
        g.fillRect(cardX,              cardY,          cardW, 2);       // top bar
        g.setColor(new Color(255, 180, 0, 40));
        g.fillRect(cardX,              cardY,          1, cardH);       // left
        g.fillRect(cardX + cardW - 1,  cardY,          1, cardH);       // right
        g.fillRect(cardX,              cardY + cardH - 1, cardW, 1);   // bottom

        // Name with blinking cursor
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        boolean cursorOn = (System.currentTimeMillis() / 500) % 2 == 0;
        String display   = name + (cursorOn ? "_" : " ");
        // Centre the display string
        int textX = CX - fmM.stringWidth(name + "_") / 2;
        g.setColor(UITheme.ACCENT);
        g.drawString(display, textX, cardY + 44);

        // Character counter
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String counter = name.length() + " / " + MAX_LEN;
        g.setColor(name.length() >= MAX_LEN ? UITheme.DANGER : UITheme.TEXT_FAINT);
        g.drawString(counter, cardX + cardW - fmS.stringWidth(counter) - 10,
                cardY + cardH - 6);

        // ── Hint label ────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        String hint = "ENTER PILOT NAME";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(hint, CX - fmS.stringWidth(hint) / 2, cardY - 14);

        // ── Confirm / cancel hints ────────────────────────────────────
        String h1 = "A-Z  0-9  type      BKSP  delete      ENTER  confirm";
        String h2 = "ESC  cancel";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(h1, CX - fmS.stringWidth(h1) / 2, H - 38);
        g.drawString(h2, CX - fmS.stringWidth(h2) / 2, H - 20);
    }
}
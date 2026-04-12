package ui.menu;

import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import save.GameMode;
import save.ProfileManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Debug / test level selector screen.
 *
 * FIX: Tab key replaced with Q for panel switching.
 * Swing intercepts VK_TAB for focus traversal before it reaches
 * the KeyListener — Tab events never arrive at InputManager.
 * Q cycles focus identically to the old Tab behaviour.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────┐
 *   │  DEBUG MODE                                         │
 *   │  ─────────────────────────────────────────────────  │
 *   │  LEVEL SELECT          BOSS SELECT                  │
 *   │  [<  01  >]            RED    YELLOW  BLUE          │
 *   │  Region: RED           GREEN  WHITE                 │
 *   │  Difficulty info                                    │
 *   │  ─────────────────────────────────────────────────  │
 *   │  [ LAUNCH LEVEL ]   [ LAUNCH BOSS ]                 │
 *   │                                                     │
 *   │  BACK                                               │
 *   └─────────────────────────────────────────────────────┘
 *
 * Controls:
 *   Q switches panels (was Tab — Tab is eaten by Swing focus traversal).
 *   LEFT/RIGHT changes selection within a panel.
 *   UP/DOWN navigates buttons/back row.
 *   ENTER launches.
 *   ESC → main menu.
 */
public class DebugMenuScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    // Focus: 0 = level panel, 1 = boss panel, 2 = launch level, 3 = launch boss, 4 = back
    private int focus       = 0;
    private int selectedLevel = 1;
    private int selectedBoss  = 0;  // index into BOSS_NAMES

    private static final String[] BOSS_NAMES   = { "RED", "YELLOW", "BLUE", "GREEN", "WHITE" };
    private static final String[] BOSS_LABELS  = { "VANGUARD", "SWARMMASTER", "IRONCLAD", "PHANTOM", "APEX" };
    private static final int[]    BOSS_LEVELS  = { 5, 10, 15, 20, 25 };

    private static final String[] REGION_NAMES = { "RED", "YELLOW", "BLUE", "GREEN", "WHITE" };
    private static final int[][]  REGION_RANGE = { {1,5}, {6,10}, {11,15}, {16,20}, {21,25} };

    public void enter() {
        focus         = 0;
        selectedLevel = 1;
        selectedBoss  = 0;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (InputManager.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            GameStateManager.setState(GameState.MAIN_MENU);
            return;
        }

        // FIX: Q cycles focus instead of Tab (Swing eats VK_TAB before KeyListener sees it)
        if (InputManager.isKeyPressed(KeyEvent.VK_Q)) {
            focus = (focus + 1) % 5;
            return;
        }

        boolean up    = InputManager.isKeyPressed(KeyEvent.VK_UP);
        boolean down  = InputManager.isKeyPressed(KeyEvent.VK_DOWN);
        boolean left  = InputManager.isKeyPressed(KeyEvent.VK_LEFT);
        boolean right = InputManager.isKeyPressed(KeyEvent.VK_RIGHT);
        boolean enter = InputManager.isKeyPressed(KeyEvent.VK_ENTER);

        switch (focus) {
            case 0 -> { // Level panel
                if (right || down) selectedLevel = Math.min(selectedLevel + 1, 25);
                if (left  || up)   selectedLevel = Math.max(selectedLevel - 1, 1);
                if (enter)         launchLevel(selectedLevel);
            }
            case 1 -> { // Boss panel
                if (right || down) selectedBoss = (selectedBoss + 1) % BOSS_NAMES.length;
                if (left  || up)   selectedBoss = (selectedBoss - 1 + BOSS_NAMES.length) % BOSS_NAMES.length;
                if (enter)         launchBoss(selectedBoss);
            }
            case 2 -> { // Launch level button
                if (up)    focus = 0;
                if (down)  focus = 4;
                if (enter) launchLevel(selectedLevel);
            }
            case 3 -> { // Launch boss button
                if (up)    focus = 1;
                if (down)  focus = 4;
                if (enter) launchBoss(selectedBoss);
            }
            case 4 -> { // Back
                if (up)    focus = 2;
                if (enter) GameStateManager.setState(GameState.MAIN_MENU);
            }
        }
    }

    // ── Launch helpers ────────────────────────────────────────────────
    private void launchLevel(int level) {
        ProfileManager.createProfile(5, "DEBUG", GameMode.CAMPAIGN);
        PendingGameStart.level     = level;
        PendingGameStart.isNewGame = false;
        PendingGameStart.gameMode  = GameMode.CAMPAIGN;
        PendingGameStart.saveSlot  = 5;
        GameStateManager.setState(GameState.LEVEL_LOAD);
    }

    private void launchBoss(int bossIndex) {
        int bossLevel = BOSS_LEVELS[bossIndex];
        ProfileManager.createProfile(5, "DEBUG", GameMode.CAMPAIGN);
        PendingGameStart.level            = bossLevel;
        PendingGameStart.isNewGame        = false;
        PendingGameStart.gameMode         = GameMode.CAMPAIGN;
        PendingGameStart.saveSlot         = 5;
        // Flag tells WaveManager to skip fleet waves and spawn boss directly
        PendingGameStart.directBossRegion = BOSS_NAMES[bossIndex];
        GameStateManager.setState(GameState.LEVEL_LOAD);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private String regionForLevel(int level) {
        for (int i = 0; i < REGION_RANGE.length; i++) {
            if (level >= REGION_RANGE[i][0] && level <= REGION_RANGE[i][1])
                return REGION_NAMES[i];
        }
        return "?";
    }

    private boolean isBossLevel(int level) {
        for (int bl : BOSS_LEVELS) if (bl == level) return true;
        return false;
    }

    private String difficultyTag(int level) {
        if (level <= 5)  return "BEGINNER   T1 basic only";
        if (level <= 10) return "EASY       T1 + fast enemies";
        if (level <= 15) return "MEDIUM     T1 + tanks + snipers";
        if (level <= 20) return "HARD       T2 enemies introduced";
        return              "EXTREME    All T2 elite formations";
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        MenuButton.renderBackground(g);

        // ── Title ─────────────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        FontMetrics fmT = g.getFontMetrics();
        String title = "DEBUG MODE";
        g.setColor(UITheme.PRIMARY);
        g.drawString(title, CX - fmT.stringWidth(title) / 2, 52);

        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fmS9 = g.getFontMetrics();
        String sub = "TEST LEVEL DIFFICULTY  |  SPAWN ANY BOSS  |  SLOT 5 OVERWRITTEN";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(sub, CX - fmS9.stringWidth(sub) / 2, 72);

        g.setColor(new Color(0, 255, 255, 40));
        g.fillRect(CX - 180, 82, 360, 1);

        // ── Two-column panels ─────────────────────────────────────────
        int panelY  = 100;
        int panelH  = 200;
        int panelW  = 320;
        int leftX   = CX - panelW - 16;
        int rightX  = CX + 16;

        // Level panel
        drawPanel(g, leftX, panelY, panelW, panelH,
                "LEVEL SELECT", focus == 0);
        renderLevelPanel(g, leftX + 16, panelY + 28, panelW - 32);

        // Boss panel
        drawPanel(g, rightX, panelY, panelW, panelH,
                "BOSS SELECT", focus == 1);
        renderBossPanel(g, rightX + 16, panelY + 28, panelW - 32);

        // ── Launch buttons ────────────────────────────────────────────
        int btnY = panelY + panelH + 24;
        int btnW = 200;
        int btnH = 38;

        drawButton(g, leftX + (panelW - btnW) / 2, btnY, btnW, btnH,
                "LAUNCH LEVEL " + String.format("%02d", selectedLevel),
                focus == 2,
                UITheme.ACCENT);

        String bossLabel = "LAUNCH " + BOSS_LABELS[selectedBoss];
        drawButton(g, rightX + (panelW - btnW) / 2, btnY, btnW, btnH,
                bossLabel,
                focus == 3,
                UITheme.DANGER);

        // ── Back ──────────────────────────────────────────────────────
        int backY = btnY + btnH + 32;
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String back = (focus == 4 ? ">  " : "   ") + "BACK";
        g.setColor(focus == 4 ? UITheme.ACCENT : UITheme.TEXT_DIM);
        g.drawString(back, CX - fmM.stringWidth(back) / 2, backY);

        // ── Footer — updated key hint ─────────────────────────────────
        g.setFont(UIFonts.SMALL.deriveFont(9f));
        fmS9 = g.getFontMetrics();
        // FIX: changed TAB to Q in the hint text
        String footer = "Q  switch panel    LEFT/RIGHT  change    ENTER  launch    ESC  back";
        g.setColor(UITheme.TEXT_FAINT);
        g.drawString(footer, CX - fmS9.stringWidth(footer) / 2, H - 16);
    }

    // ── Level panel contents ──────────────────────────────────────────
    private void renderLevelPanel(Graphics2D g, int x, int y, int w) {
        String region = regionForLevel(selectedLevel);
        Color  rc     = UITheme.getRegionColor(region);
        boolean boss  = isBossLevel(selectedLevel);

        g.setFont(UIFonts.MENU);
        FontMetrics fm = g.getFontMetrics();
        String levelStr = String.format("%02d", selectedLevel);
        String arrowL   = "<";
        String arrowR   = ">";

        int cx = x + w / 2;
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(arrowL, cx - fm.stringWidth(levelStr) / 2 - 24, y + 24);
        g.setColor(UITheme.PRIMARY);
        g.drawString(levelStr, cx - fm.stringWidth(levelStr) / 2, y + 24);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(arrowR, cx + fm.stringWidth(levelStr) / 2 + 10, y + 24);

        g.setFont(UIFonts.SMALL.deriveFont(10f));
        fm = g.getFontMetrics();
        String regStr = "REGION:  " + region;
        g.setColor(rc);
        g.drawString(regStr, x, y + 50);

        if (boss) {
            String bossTag = "\u25B6 BOSS LEVEL";
            g.setColor(new Color(255, 80, 80));
            g.drawString(bossTag, x, y + 68);
        }

        g.setFont(UIFonts.SMALL.deriveFont(9f));
        fm = g.getFontMetrics();
        String diff = difficultyTag(selectedLevel);
        String[] parts = diff.split("   ");
        if (parts.length >= 2) {
            g.setColor(UITheme.ACCENT);
            g.drawString(parts[0].trim(), x, y + 90);
            g.setColor(UITheme.TEXT_DIM);
            g.drawString(parts[1].trim(), x, y + 106);
        }

        int barW   = w;
        int barH   = 4;
        int barY   = y + 130;
        float prog = (selectedLevel - 1) / 24f;
        int fillW  = (int)(barW * prog);

        g.setColor(new Color(30, 30, 45));
        g.fillRect(x, barY, barW, barH);
        g.setColor(rc);
        if (fillW > 0) g.fillRect(x, barY, fillW, barH);

        for (int r = 0; r < REGION_RANGE.length; r++) {
            int tickLevel = REGION_RANGE[r][0];
            int tickX     = x + (int)(barW * (tickLevel - 1) / 24f);
            g.setColor(new Color(80, 80, 100));
            g.fillRect(tickX, barY - 2, 1, barH + 4);
        }

        g.setFont(UIFonts.SMALL.deriveFont(8f));
        fm = g.getFontMetrics();
        for (int tick : new int[]{1, 5, 10, 15, 20, 25}) {
            int tickX = x + (int)(barW * (tick - 1) / 24f);
            g.setColor(tick == selectedLevel ? UITheme.ACCENT : UITheme.TEXT_FAINT);
            String ts = String.valueOf(tick);
            g.drawString(ts, tickX - fm.stringWidth(ts) / 2, barY + 16);
        }
    }

    // ── Boss panel contents ───────────────────────────────────────────
    private void renderBossPanel(Graphics2D g, int x, int y, int w) {
        int cols     = 3;
        int cellW    = (w - (cols - 1) * 8) / cols;
        int cellH    = 48;
        int rowGap   = 10;

        for (int i = 0; i < BOSS_NAMES.length; i++) {
            int col  = i % cols;
            int row  = i / cols;
            int bx   = x + col * (cellW + 8);
            int by   = y + row * (cellH + rowGap);
            boolean  sel = (i == selectedBoss);
            Color    bc  = UITheme.getRegionColor(BOSS_NAMES[i]);

            g.setColor(sel
                    ? new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 35)
                    : new Color(10, 10, 18));
            g.fillRect(bx, by, cellW, cellH);

            g.setColor(sel ? bc : new Color(40, 40, 60));
            g.fillRect(bx, by,         cellW, 1);
            g.fillRect(bx, by + cellH - 1, cellW, 1);
            g.fillRect(bx, by,         1, cellH);
            g.fillRect(bx + cellW - 1, by, 1, cellH);

            if (sel) {
                g.setColor(bc);
                g.fillRect(bx, by, cellW, 2);
            }

            g.setFont(UIFonts.SMALL.deriveFont(9f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(sel ? bc : UITheme.TEXT_DIM);
            g.drawString(BOSS_NAMES[i], bx + cellW / 2 - fm.stringWidth(BOSS_NAMES[i]) / 2, by + 18);

            g.setFont(UIFonts.SMALL.deriveFont(8f));
            fm = g.getFontMetrics();
            g.setColor(sel ? UITheme.PRIMARY : new Color(70, 70, 90));
            g.drawString(BOSS_LABELS[i], bx + cellW / 2 - fm.stringWidth(BOSS_LABELS[i]) / 2, by + 34);
        }

        Color selC = UITheme.getRegionColor(BOSS_NAMES[selectedBoss]);
        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fmD = g.getFontMetrics();
        String detail = "LVL " + BOSS_LEVELS[selectedBoss] + "  |  BOSS WAVE";
        g.setColor(selC);
        g.drawString(detail, x, y + 2 * (cellH + rowGap) + 18);

        g.setColor(UITheme.TEXT_DIM);
        g.setFont(UIFonts.SMALL.deriveFont(8f));
        String hint = "Spawns the boss directly (last wave only)";
        fmD = g.getFontMetrics();
        g.drawString(hint, x, y + 2 * (cellH + rowGap) + 34);
    }

    // ── Reusable panel frame ──────────────────────────────────────────
    private void drawPanel(Graphics2D g, int x, int y, int w, int h,
                           String title, boolean focused) {
        g.setColor(new Color(6, 6, 14));
        g.fillRect(x, y, w, h);

        Color border = focused ? UITheme.PRIMARY : new Color(40, 40, 60);
        g.setColor(border);
        g.fillRect(x,     y,     w, 1);
        g.fillRect(x,     y+h-1, w, 1);
        g.fillRect(x,     y,     1, h);
        g.fillRect(x+w-1, y,     1, h);

        g.setColor(focused ? UITheme.PRIMARY : new Color(60, 60, 80));
        g.fillRect(x, y, w, 2);

        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(focused ? UITheme.PRIMARY : UITheme.TEXT_DIM);
        g.drawString(title, x + 10, y + 16);

        if (focused) {
            g.setColor(UITheme.PRIMARY);
            g.fillOval(x + w - 16, y + 8, 6, 6);
        }
    }

    // ── Reusable button ───────────────────────────────────────────────
    private void drawButton(Graphics2D g, int x, int y, int w, int h,
                            String label, boolean focused, Color accentCol) {
        g.setColor(focused
                ? new Color(accentCol.getRed(), accentCol.getGreen(), accentCol.getBlue(), 25)
                : new Color(8, 8, 18));
        g.fillRect(x, y, w, h);

        g.setColor(focused ? accentCol : new Color(40, 40, 60));
        g.fillRect(x,     y,     w, 1);
        g.fillRect(x,     y+h-1, w, 1);
        g.fillRect(x,     y,     1, h);
        g.fillRect(x+w-1, y,     1, h);

        if (focused) {
            g.setColor(accentCol);
            g.fillRect(x, y, w, 2);
        }

        g.setFont(UIFonts.SMALL.deriveFont(9f));
        FontMetrics fm = g.getFontMetrics();
        String disp = label;
        while (fm.stringWidth(disp) > w - 16 && disp.length() > 4) {
            disp = disp.substring(0, disp.length() - 1);
        }
        g.setColor(focused ? accentCol : UITheme.TEXT_DIM);
        g.drawString(disp, x + w / 2 - fm.stringWidth(disp) / 2, y + h / 2 + 4);
    }
}
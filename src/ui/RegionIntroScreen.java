package ui;

import campaign.RegionStoryRegistry;
import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

public class RegionIntroScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final long MS_PER_CHAR     = 40;
    private static final long MS_BETWEEN_LINE = 360;
    private static final long AUTO_ADVANCE_MS = 5000;

    private String   region     = "RED";
    private String[] storyLines = {};

    // Typewriter state machine
    private enum TwState { TYPING, LINE_PAUSE, DONE }
    private TwState twState       = TwState.TYPING;
    private int     currentLine   = 0;
    private int     charsShown    = 0;
    private long    stateStart    = 0;
    private long    enterTime     = 0;

    // Blink
    private boolean blinkOn    = true;
    private long    blinkTimer = 0;

    private final SpaceBackground bg = new SpaceBackground();

    // ── Public ────────────────────────────────────────────────────────
    public void enter(String regionName) {
        this.region     = regionName;
        this.storyLines = RegionStoryRegistry.getStory(regionName);
        if (this.storyLines == null) this.storyLines = new String[]{};

        twState       = TwState.TYPING;
        currentLine   = 0;
        charsShown    = 0;
        enterTime     = System.currentTimeMillis();
        stateStart    = enterTime;
        blinkOn       = true;
        blinkTimer    = enterTime;

        bg.setRegion(regionName);
    }

    public void update() {
        long now = System.currentTimeMillis();
        bg.update();

        // Blink
        if (now - blinkTimer > 480) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }

        // Auto-advance after timeout
        if (now - enterTime >= AUTO_ADVANCE_MS) {
            advance();
            return;
        }

        // SPACE — skip typewriter first press, advance second press
        if (InputManager.isKeyPressed(KeyEvent.VK_SPACE)) {
            if (twState != TwState.DONE) {
                // Reveal everything instantly
                twState     = TwState.DONE;
                currentLine = storyLines.length;
            } else {
                advance();
            }
            return;
        }

        if (twState == TwState.DONE) return;
        if (currentLine >= storyLines.length) {
            twState = TwState.DONE;
            return;
        }

        String line = storyLines[currentLine];

        switch (twState) {
            case TYPING -> {
                int target = (int)((now - stateStart) / MS_PER_CHAR);
                charsShown = Math.min(target, line.length());

                if (charsShown >= line.length()) {
                    twState    = TwState.LINE_PAUSE;
                    stateStart = now;
                }
            }
            case LINE_PAUSE -> {
                if (now - stateStart >= MS_BETWEEN_LINE) {
                    currentLine++;
                    charsShown = 0;
                    twState    = TwState.TYPING;
                    stateStart = now;
                }
            }
        }
    }

    private void advance() {
        GameStateManager.setState(GameState.PLAYING);
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        bg.render(g, 0, 0, W, H);

        // ── Left-aligned content block ────────────────────────────────
        int blockX = 110;
        int blockY = 155;

        // Vertical accent bar
        g.setColor(regionColor);
        g.fillRect(blockX - 20, blockY - 38, 4, 56);

        // Region name (big)
        g.setFont(UIFonts.TITLE);
        g.setColor(regionColor);
        g.drawString(region, blockX, blockY);

        // SECTOR sub-label
        g.setFont(UIFonts.BODY);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("SECTOR", blockX, blockY + 26);

        // Horizontal rule
        int ruleAlpha = 55;
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(),
                ruleAlpha));
        g.fillRect(blockX - 20, blockY + 42, W - blockX - 60, 1);

        // ── Story lines (skip index 0 — already shown as region name) ─
        boolean isDone  = (twState == TwState.DONE);
        int     lineStartY = blockY + 84;
        int     lineH      = 40;

        for (int i = 1; i < storyLines.length; i++) {
            String line = storyLines[i];
            String toShow;
            Color  col = UITheme.PRIMARY;

            if (isDone || i < currentLine) {
                toShow = line;
            } else if (i == currentLine) {
                toShow = line.substring(0, Math.min(charsShown, line.length()));

                // Cursor
                g.setFont(UIFonts.BODY);
                FontMetrics fm = g.getFontMetrics();
                int curX = blockX + fm.stringWidth(toShow);
                if ((System.currentTimeMillis() / 380) % 2 == 0) {
                    g.setColor(regionColor);
                    g.fillRect(curX, lineStartY + (i - 1) * lineH - 13, 2, 15);
                }
            } else {
                continue;
            }

            // Fade in completed lines slightly
            g.setFont(UIFonts.BODY);
            g.setColor(col);
            g.drawString(toShow, blockX, lineStartY + (i - 1) * lineH);
        }

        // ── Progress bar — time until auto-advance ────────────────────
        long  elapsed  = System.currentTimeMillis() - enterTime;
        float progress = Math.min(elapsed / (float) AUTO_ADVANCE_MS, 1f);
        int   barW     = W - blockX * 2;
        int   barY     = H - 54;

        g.setColor(new Color(40, 40, 55));
        g.fillRect(blockX, barY, barW, 2);
        g.setColor(regionColor);
        g.fillRect(blockX, barY, (int)(barW * progress), 2);

        // ── Skip / advance prompt ─────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();

        if (twState == TwState.DONE) {
            if (blinkOn) {
                String prompt = "[ SPACE ]  ENTER SECTOR";
                g.setColor(regionColor);
                g.drawString(prompt,
                        W - blockX - fmS.stringWidth(prompt), H - 38);
            }
        } else {
            String skip = "[ SPACE ]  skip";
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString(skip,
                    W - blockX - fmS.stringWidth(skip), H - 38);
        }
    }
}
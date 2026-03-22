package ui;

import campaign.CampaignIntroStory;
import config.LayoutConfig;
import core.GameState;
import core.GameStateManager;
import input.InputManager;
import ui.theme.UIFonts;
import ui.theme.UITheme;

import java.awt.*;
import java.awt.event.KeyEvent;

public class CampaignIntroScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    // Typewriter timing
    private static final long MS_PER_CHAR     = 36;
    private static final long MS_BLANK_LINE   = 220;
    private static final long MS_BETWEEN_LINE = 280;

    private final String[] lines = CampaignIntroStory.INTRO;

    // Typewriter state
    private enum TwState { TYPING, LINE_PAUSE, BLANK_PAUSE, DONE }
    private TwState twState       = TwState.TYPING;
    private int     currentLine   = 0;
    private int     charsRevealed = 0;
    private long    stateStart    = 0;

    // Blink prompt
    private boolean blinkOn    = true;
    private long    blinkTimer = 0;

    private final SpaceBackground bg = SpaceBackground.getMenuBackground();

    // ── Public ────────────────────────────────────────────────────────
    public void reset() {
        twState       = TwState.TYPING;
        currentLine   = 0;
        charsRevealed = 0;
        stateStart    = System.currentTimeMillis();
        blinkOn       = true;
        blinkTimer    = stateStart;
    }

    public void update() {
        bg.update();
        long now = System.currentTimeMillis();

        // Blink
        if (now - blinkTimer > 500) {
            blinkOn    = !blinkOn;
            blinkTimer = now;
        }

        // SPACE — skip to end or advance to REGION_INTRO
        if (InputManager.isKeyPressed(KeyEvent.VK_SPACE)) {
            if (twState == TwState.DONE) {
                GameStateManager.setState(GameState.REGION_INTRO);
            } else {
                // Reveal all lines instantly
                twState       = TwState.DONE;
                currentLine   = lines.length;
                charsRevealed = 0;
            }
            return;
        }

        if (twState == TwState.DONE) return;

        // Finished all lines
        if (currentLine >= lines.length) {
            twState = TwState.DONE;
            return;
        }

        String line = lines[currentLine];

        switch (twState) {

            case TYPING -> {
                if (line.isEmpty()) {
                    // Blank spacer — go straight to pause
                    twState    = TwState.BLANK_PAUSE;
                    stateStart = now;
                    return;
                }
                long msPerChar = (now - stateStart);
                int  target    = (int)(msPerChar / MS_PER_CHAR);
                charsRevealed  = Math.min(target, line.length());

                if (charsRevealed >= line.length()) {
                    twState    = TwState.LINE_PAUSE;
                    stateStart = now;
                }
            }

            case LINE_PAUSE -> {
                if (now - stateStart >= MS_BETWEEN_LINE) {
                    advanceLine(now);
                }
            }

            case BLANK_PAUSE -> {
                if (now - stateStart >= MS_BLANK_LINE) {
                    advanceLine(now);
                }
            }
        }
    }

    private void advanceLine(long now) {
        currentLine++;
        charsRevealed = 0;
        twState       = TwState.TYPING;
        stateStart    = now;
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        bg.render(g, 0, 0, W, H);

        // Top decoration
        g.setColor(UITheme.PANEL_BORDER);
        g.fillRect(CX - 180, 58, 360, 1);

        // Header label
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String header = "- MISSION LOG -";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(header, CX - fmS.stringWidth(header) / 2, 52);

        // Mission ID
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String id = "VJ-01";
        g.setColor(UITheme.ACCENT);
        g.drawString(id, CX - fmM.stringWidth(id) / 2, 98);

        // Decoration line below ID
        g.setColor(UITheme.PANEL_BORDER);
        g.fillRect(CX - 180, 110, 360, 1);

        // ── Story lines ───────────────────────────────────────────────
        int startY = 152;
        int lineH  = 32;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;

            String toShow;
            Color  col;

            boolean isDone = (twState == TwState.DONE);

            if (isDone || i < currentLine) {
                toShow = line;
                col    = (i == 0) ? UITheme.ACCENT : UITheme.PRIMARY;
            } else if (i == currentLine) {
                toShow = line.substring(0, Math.min(charsRevealed, line.length()));
                col    = (i == 0) ? UITheme.ACCENT : UITheme.PRIMARY;

                // Blinking cursor at end of current line
                g.setFont(UIFonts.BODY);
                FontMetrics fmB = g.getFontMetrics();
                int lineStartX = CX - fmB.stringWidth(line) / 2;
                int cursorX    = lineStartX + fmB.stringWidth(toShow);
                if ((now() / 380) % 2 == 0) {
                    g.setColor(UITheme.PRIMARY);
                    g.fillRect(cursorX, startY + i * lineH - 13, 2, 15);
                }
            } else {
                continue;   // not yet reached
            }

            g.setFont(UIFonts.BODY);
            FontMetrics fmB = g.getFontMetrics();
            int tx = CX - fmB.stringWidth(line) / 2;
            g.setColor(col);
            g.drawString(toShow, tx, startY + i * lineH);
        }

        // Bottom decoration
        g.setColor(UITheme.PANEL_BORDER);
        g.fillRect(CX - 180, H - 82, 360, 1);

        // Prompt
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();
        if (twState == TwState.DONE) {
            if (blinkOn) {
                String prompt = "[ SPACE ]  BEGIN MISSION";
                g.setColor(UITheme.HIGHLIGHT);
                g.drawString(prompt, CX - fmS.stringWidth(prompt) / 2, H - 52);
            }
        } else {
            String skip = "[ SPACE ]  skip";
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString(skip, CX - fmS.stringWidth(skip) / 2, H - 52);
        }
    }

    private long now() { return System.currentTimeMillis(); }
}
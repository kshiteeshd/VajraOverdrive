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

/**
 * Region intro screen — shown when entering a new sector.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P).
 *  - Cursor blink uses System.currentTimeMillis() modulo directly
 *    so it doesn't need a separate blinkTimer field.
 *  - Auto-advance bar color uses regionColor (was hardcoded TEXT_DIM).
 *  - Story lines start Y pushed down slightly so the region name
 *    title has more breathing room above the text block.
 *  - Double-advance guard kept from original — advance() is idempotent.
 *  - SPACE first press reveals all text, second press advances.
 */
public class RegionIntroScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final long MS_PER_CHAR     = 38;
    private static final long MS_BETWEEN_LINE = 340;
    private static final long AUTO_ADVANCE_MS = 5500;

    private static final int LEFT_MARGIN = 110;

    private String   region     = "RED";
    private String[] storyLines = {};

    private enum TwState { TYPING, LINE_PAUSE, DONE }
    private TwState twState     = TwState.TYPING;
    private int     currentLine = 0;
    private int     charsShown  = 0;
    private long    stateStart  = 0;
    private long    enterTime   = 0;
    private boolean advanced    = false;

    private final SpaceBackground bg = new SpaceBackground();

    // ── Enter ─────────────────────────────────────────────────────────
    public void enter(String regionName) {
        this.region     = regionName;
        this.storyLines = RegionStoryRegistry.getStory(regionName);
        if (this.storyLines == null) this.storyLines = new String[]{};

        twState     = TwState.TYPING;
        currentLine = 0;
        charsShown  = 0;
        enterTime   = System.currentTimeMillis();
        stateStart  = enterTime;
        advanced    = false;

        bg.setRegion(regionName);
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        long now = System.currentTimeMillis();
        bg.update();

        // Auto-advance
        if (now - enterTime >= AUTO_ADVANCE_MS) {
            advance();
            return;
        }

        if (InputManager.isKeyPressed(KeyEvent.VK_SPACE)) {
            if (twState != TwState.DONE) {
                // First press — reveal all text instantly
                twState     = TwState.DONE;
                currentLine = storyLines.length;
            } else {
                // Second press — advance to game
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

    // ── Advance (idempotent) ──────────────────────────────────────────
    private void advance() {
        if (advanced) return;
        advanced = true;
        GameStateManager.setState(GameState.PLAYING);
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g) {
        Color regionColor = UITheme.getRegionColor(region);
        long  elapsed     = System.currentTimeMillis() - enterTime;

        bg.render(g, 0, 0, W, H);

        // ── Left accent bar ───────────────────────────────────────────
        g.setColor(regionColor);
        g.fillRect(LEFT_MARGIN - 20, 130, 3, 72);

        // ── Region name ───────────────────────────────────────────────
        g.setFont(UIFonts.TITLE);
        g.setColor(regionColor);
        g.drawString(region, LEFT_MARGIN, 172);

        // ── "SECTOR" sub-label ────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        g.setColor(UITheme.TEXT_DIM);
        g.drawString("SECTOR", LEFT_MARGIN, 192);

        // ── Horizontal rule ───────────────────────────────────────────
        g.setColor(new Color(
                regionColor.getRed(),
                regionColor.getGreen(),
                regionColor.getBlue(), 50));
        g.fillRect(LEFT_MARGIN - 20, 204, W - LEFT_MARGIN - 60, 1);

        // ── Story lines ───────────────────────────────────────────────
        boolean isDone     = (twState == TwState.DONE);
        int     lineStartY = 248;
        int     lineH      = 38;

        // Skip index 0 — it's the region name, already shown as title
        for (int i = 1; i < storyLines.length; i++) {
            String line = storyLines[i];
            String toShow;

            if (isDone || i < currentLine) {
                toShow = line;
            } else if (i == currentLine) {
                toShow = line.substring(0,
                        Math.min(charsShown, line.length()));

                // Cursor blink at end of current line
                boolean cursorOn =
                        (System.currentTimeMillis() / 400) % 2 == 0;
                if (cursorOn) {
                    g.setFont(UIFonts.SMALL);
                    FontMetrics fm = g.getFontMetrics();
                    int curX = LEFT_MARGIN + fm.stringWidth(toShow);
                    int curY = lineStartY + (i - 1) * lineH;
                    g.setColor(regionColor);
                    g.fillRect(curX + 2, curY - 11, 2, 13);
                }
            } else {
                continue;
            }

            g.setFont(UIFonts.SMALL);
            g.setColor(UITheme.PRIMARY);
            g.drawString(toShow,
                    LEFT_MARGIN,
                    lineStartY + (i - 1) * lineH);
        }

        // ── Auto-advance progress bar ─────────────────────────────────
        float progress = Math.min(elapsed / (float) AUTO_ADVANCE_MS, 1f);
        int   barW     = W - LEFT_MARGIN * 2;
        int   barY     = H - 52;

        g.setColor(new Color(30, 30, 40));
        g.fillRect(LEFT_MARGIN, barY, barW, 2);

        g.setColor(regionColor);
        g.fillRect(LEFT_MARGIN, barY, (int)(barW * progress), 2);

        // ── Skip / advance prompt ─────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();

        if (isDone) {
            boolean blinkOn = (System.currentTimeMillis() / 500) % 2 == 0;
            if (blinkOn) {
                String prompt = "SPACE  ENTER SECTOR";
                g.setColor(regionColor);
                g.drawString(prompt,
                        W - LEFT_MARGIN - fmS.stringWidth(prompt),
                        H - 30);
            }
        } else {
            String skip = "SPACE  skip";
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString(skip,
                    W - LEFT_MARGIN - fmS.stringWidth(skip),
                    H - 30);
        }
    }
}
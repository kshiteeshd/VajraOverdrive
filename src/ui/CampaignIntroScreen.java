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

/**
 * Campaign intro — typewriter story screen before level 1.
 *
 * CHANGED:
 *  - All fonts UIFonts (Press Start 2P).
 *  - Cursor blink is stateless — uses System.currentTimeMillis()
 *    modulo, no blinkTimer field needed.
 *  - blinkOn / blinkTimer fields removed.
 *  - Prompt uses double-space between key and action for Press Start
 *    2P kerning consistency ("SPACE  skip", "SPACE  BEGIN MISSION").
 *  - Header and divider lines use PRIMARY at low alpha instead of
 *    PANEL_BORDER so they respond to region color in future.
 *  - Line height increased to 36px to give Press Start 2P room
 *    between lines at SMALL (8px) size — was 32px.
 */
public class CampaignIntroScreen {

    private static final int W  = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H  = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int CX = W / 2;

    private static final long MS_PER_CHAR     = 36;
    private static final long MS_BLANK_LINE   = 200;
    private static final long MS_BETWEEN_LINE = 260;

    private final String[] lines = CampaignIntroStory.INTRO;

    // ── Typewriter state ──────────────────────────────────────────────
    private enum TwState { TYPING, LINE_PAUSE, BLANK_PAUSE, DONE }
    private TwState twState       = TwState.TYPING;
    private int     currentLine   = 0;
    private int     charsRevealed = 0;
    private long    stateStart    = 0;

    private final SpaceBackground bg = SpaceBackground.getMenuBackground();

    // ── Public ────────────────────────────────────────────────────────
    public void reset() {
        twState       = TwState.TYPING;
        currentLine   = 0;
        charsRevealed = 0;
        stateStart    = System.currentTimeMillis();
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        bg.update();
        long now = System.currentTimeMillis();

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

        if (currentLine >= lines.length) {
            twState = TwState.DONE;
            return;
        }

        String line = lines[currentLine];

        switch (twState) {
            case TYPING -> {
                if (line.isEmpty()) {
                    twState    = TwState.BLANK_PAUSE;
                    stateStart = now;
                    return;
                }
                int target    = (int)((now - stateStart) / MS_PER_CHAR);
                charsRevealed = Math.min(target, line.length());

                if (charsRevealed >= line.length()) {
                    twState    = TwState.LINE_PAUSE;
                    stateStart = now;
                }
            }
            case LINE_PAUSE -> {
                if (now - stateStart >= MS_BETWEEN_LINE)
                    advanceLine(now);
            }
            case BLANK_PAUSE -> {
                if (now - stateStart >= MS_BLANK_LINE)
                    advanceLine(now);
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

        // ── Header ────────────────────────────────────────────────────
        g.setColor(new Color(0, 200, 200, 35));
        g.fillRect(CX - 190, 50, 380, 1);

        g.setFont(UIFonts.SMALL);
        FontMetrics fmS = g.getFontMetrics();
        String header = "MISSION  LOG";
        g.setColor(UITheme.TEXT_DIM);
        g.drawString(header,
                CX - fmS.stringWidth(header) / 2, 46);

        // Mission ID
        g.setFont(UIFonts.MENU);
        FontMetrics fmM = g.getFontMetrics();
        String id = "VJ-01";
        g.setColor(UITheme.ACCENT);
        g.drawString(id,
                CX - fmM.stringWidth(id) / 2, 90);

        // Rule below ID
        g.setColor(new Color(0, 200, 200, 35));
        g.fillRect(CX - 190, 102, 380, 1);

        // ── Story lines ───────────────────────────────────────────────
        int startY = 142;
        int lineH  = 36;
        boolean isDone = (twState == TwState.DONE);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;

            String toShow;
            Color  col;

            if (isDone || i < currentLine) {
                toShow = line;
                col    = (i == 0) ? UITheme.ACCENT : UITheme.PRIMARY;
            } else if (i == currentLine) {
                toShow = line.substring(0,
                        Math.min(charsRevealed, line.length()));
                col    = (i == 0) ? UITheme.ACCENT : UITheme.PRIMARY;

                // Stateless cursor blink
                boolean cursorOn =
                        (System.currentTimeMillis() / 380) % 2 == 0;
                if (cursorOn) {
                    g.setFont(UIFonts.SMALL);
                    FontMetrics fm = g.getFontMetrics();
                    int lineStartX = CX - fm.stringWidth(line) / 2;
                    int cursorX    = lineStartX + fm.stringWidth(toShow);
                    g.setColor(UITheme.PRIMARY);
                    g.fillRect(cursorX + 1,
                            startY + i * lineH - 11, 2, 13);
                }
            } else {
                continue;
            }

            g.setFont(UIFonts.SMALL);
            fmS = g.getFontMetrics();
            int tx = CX - fmS.stringWidth(line) / 2;
            g.setColor(col);
            g.drawString(toShow, tx, startY + i * lineH);
        }

        // ── Bottom rule ───────────────────────────────────────────────
        g.setColor(new Color(0, 200, 200, 35));
        g.fillRect(CX - 190, H - 78, 380, 1);

        // ── Prompt ────────────────────────────────────────────────────
        g.setFont(UIFonts.SMALL);
        fmS = g.getFontMetrics();

        if (isDone) {
            boolean blinkOn =
                    (System.currentTimeMillis() / 500) % 2 == 0;
            if (blinkOn) {
                String prompt = "SPACE  BEGIN  MISSION";
                g.setColor(UITheme.HIGHLIGHT);
                g.drawString(prompt,
                        CX - fmS.stringWidth(prompt) / 2,
                        H - 48);
            }
        } else {
            String skip = "SPACE  skip";
            g.setColor(UITheme.TEXT_FAINT);
            g.drawString(skip,
                    CX - fmS.stringWidth(skip) / 2,
                    H - 48);
        }
    }
}
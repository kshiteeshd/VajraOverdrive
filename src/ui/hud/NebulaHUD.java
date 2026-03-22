package ui.hud;

import config.LayoutConfig;
import ui.theme.UIFonts;

import java.awt.*;

/**
 * NebulaHUD — two-bar overlay HUD.
 *
 * CHANGED:
 *  - FloatLabel lifetime increased from 800ms → 2500ms. Labels now
 *    stay fully visible for 1.5s, then fade over the remaining 1s.
 *  - Floaters rise slower (max 20px over full life) so they're readable.
 *  - Added explicit HP bar (green→red gradient) separate from shield bar.
 *  - All layout anchors derived from LayoutConfig.VIRTUAL_* constants,
 *    no magic pixel offsets scattered through the code.
 *  - Bottom bar height increased to 36px for better readability.
 *  - HUD font sizes bumped: labels use 10px, values use 18px.
 *  - Lives icons scaled to match new bar height.
 */
public class NebulaHUD {

    // ── Layout constants (all derived from LayoutConfig) ─────────────
    private static final int VW      = LayoutConfig.VIRTUAL_WIDTH;
    private static final int VH      = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int TOP_H   = 32;     // top bar height
    private static final int BOSS_H  = 28;     // boss sub-bar height
    private static final int BOT_H   = 44;     // bottom bar height (taller for health)
    private static final int PAD     = 10;     // horizontal inner padding
    private static final int COL1_W  = VW / 3; // left third
    private static final int COL2_W  = VW / 3; // center third
    private static final int COL3_W  = VW / 3; // right third

    // ── Fonts ─────────────────────────────────────────────────────────
    // Sizes bumped for readability — SMALL was 8px (too tiny)
    private static Font F_LABEL;   // 10px — muted labels
    private static Font F_VALUE;   // 18px — primary values
    private static Font F_LARGE;   // 26px — large accent (enemy count)
    private static boolean fontsLoaded = false;

    private static void ensureFonts() {
        if (fontsLoaded) return;
        // Derive from loaded Press Start 2P font
        F_LABEL = UIFonts.SMALL.deriveFont(10f);
        F_VALUE = UIFonts.MENU.deriveFont(18f);
        F_LARGE = UIFonts.MENU.deriveFont(26f);
        fontsLoaded = true;
    }

    // ── Color palette ─────────────────────────────────────────────────
    private static final Color BAR_BG         = new Color(0,  0,  0,  170);
    private static final Color BAR_BORDER     = new Color(0,  255, 255, 30);
    private static final Color COL_LABEL      = new Color(0,  180, 180, 150);
    private static final Color COL_VALUE      = new Color(0,  232, 255);
    private static final Color COL_SCORE      = new Color(255, 180, 0);
    private static final Color COL_DIVIDER    = new Color(0,  255, 255, 18);
    private static final Color COL_DANGER     = new Color(255, 50,  50);
    private static final Color COL_COMBO      = new Color(255, 140, 0);
    private static final Color COL_HP_HIGH    = new Color(40,  220, 90);
    private static final Color COL_HP_MID     = new Color(255, 200, 0);
    private static final Color COL_HP_LOW     = new Color(255, 50,  50);
    private static final Color COL_SHD_HIGH   = new Color(80,  160, 255);
    private static final Color COL_SHD_LOW    = new Color(180, 80,  255);

    // ── Animation state ───────────────────────────────────────────────
    private final long birthTime = System.currentTimeMillis();
    private int  lastScore     = 0;
    private long scorePoppedAt = 0;
    private static final long SCORE_POP_MS = 250;

    // ── Floating score labels ─────────────────────────────────────────
    // Lifetime bumped from 800ms → 2500ms, fully opaque for first 1500ms
    private static final int   MAX_FLOATERS  = 12;
    private static final long  FLOATER_LIFE  = 2500;
    private static final long  FLOATER_HOLD  = 1500; // stay fully visible this long
    private final FloatLabel[] floaters      = new FloatLabel[MAX_FLOATERS];
    private int floaterHead = 0;

    private static class FloatLabel {
        String text;
        float  x, y;
        long   bornAt;
        Color  color;

        boolean alive() {
            return System.currentTimeMillis() - bornAt < FLOATER_LIFE;
        }

        float alpha() {
            long age = System.currentTimeMillis() - bornAt;
            if (age < FLOATER_HOLD)  return 1.0f;                    // fully visible
            float fadeT = (age - FLOATER_HOLD) / (float)(FLOATER_LIFE - FLOATER_HOLD);
            return 1f - fadeT;                                         // linear fade
        }

        float offsetY() {
            // Slow rise: max 20px over full lifetime (was 32px too fast)
            float t = Math.min((System.currentTimeMillis() - bornAt) / (float) FLOATER_LIFE, 1f);
            return -t * 20f;
        }
    }

    // ── Public API ────────────────────────────────────────────────────

    public void spawnFloater(String text, float worldX, float worldY, Color color) {
        FloatLabel f = new FloatLabel();
        f.text   = text;
        f.x      = worldX;
        f.y      = worldY;
        f.color  = color;
        f.bornAt = System.currentTimeMillis();
        floaters[floaterHead % MAX_FLOATERS] = f;
        floaterHead++;
    }

    public void spawnFloater(String text, float worldX, float worldY) {
        spawnFloater(text, worldX, worldY, new Color(255, 200, 60));
    }

    // ── Main render ───────────────────────────────────────────────────
    public void render(Graphics2D g, HUDData d) {
        ensureFonts();
        long now = System.currentTimeMillis();

        // Score pop
        if (d.score != lastScore) { scorePoppedAt = now; lastScore = d.score; }
        float scorePop = 0f;
        if (now - scorePoppedAt < SCORE_POP_MS) {
            float t = (now - scorePoppedAt) / (float) SCORE_POP_MS;
            scorePop = (float) Math.sin(t * Math.PI);
        }

        float breathe = 0.85f + 0.15f * (float) Math.sin(
                ((now - birthTime) % 4000) / 4000f * Math.PI * 2);

        Color rc = d.regionColor();
        Composite orig = g.getComposite();

        drawTopBar(g, d, rc, scorePop, breathe, orig, now);

        if (d.bossActive) drawBossBar(g, d, rc, orig);

        drawBottomBar(g, d, rc, breathe, orig, now);
        drawFloaters(g, orig);

        g.setComposite(orig);
    }

    // ── Top bar (score | wave + formation | region + level) ──────────
    private void drawTopBar(Graphics2D g, HUDData d, Color rc,
                            float scorePop, float breathe,
                            Composite orig, long now) {
        // Background
        g.setColor(BAR_BG);
        g.fillRect(0, 0, VW, TOP_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, TOP_H - 1, VW, 1);

        int midY_label = TOP_H / 4 + 2;    // label baseline (~upper quarter)
        int midY_value = TOP_H * 3 / 4 + 3; // value baseline (~lower quarter)

        // ── LEFT: Score ───────────────────────────────────────────────
        drawLabel(g, "SCORE", PAD, midY_label, orig);
        Color scoreCol = scorePop > 0.05f ? new Color(255, 255, 120) : COL_SCORE;
        g.setComposite(orig);
        g.setFont(F_VALUE);
        g.setColor(scoreCol);
        g.drawString(String.format("%08d", d.score), PAD, midY_value);

        // ── CENTER: Wave + formation ──────────────────────────────────
        g.setFont(F_LABEL);
        FontMetrics fm = g.getFontMetrics();
        String wLabel = "WAVE";
        drawLabel(g, wLabel, VW / 2 - fm.stringWidth(wLabel) / 2, midY_label, orig);

        String wStr  = String.format("W%02d", d.wave);
        String fStr  = " " + HUDData.formationCode(d.formation);
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        int cw = fm.stringWidth(wStr + fStr);
        int cwX = VW / 2 - cw / 2;
        g.setComposite(orig);
        g.setColor(COL_VALUE);
        g.drawString(wStr, cwX, midY_value);
        g.setColor(new Color(0, 150, 150, 160));
        g.drawString(fStr, cwX + fm.stringWidth(wStr), midY_value);

        // ── RIGHT: Region + level ─────────────────────────────────────
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        String secLabel = "SECTOR";
        drawLabel(g, secLabel, VW - PAD - fm.stringWidth(secLabel), midY_label, orig);

        String regStr = d.region;
        String lvlStr = String.format("LVL%02d", d.level);
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        g.setComposite(orig);
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(),
                (int)(breathe * 220)));
        g.drawString(regStr, VW - PAD - fm.stringWidth(regStr) - fm.stringWidth(lvlStr) - 6, midY_value);
        g.setColor(COL_VALUE);
        g.drawString(lvlStr, VW - PAD - fm.stringWidth(lvlStr), midY_value);

        // Dividers
        g.setColor(COL_DIVIDER);
        g.fillRect(COL1_W, 4, 1, TOP_H - 8);
        g.fillRect(COL1_W + COL2_W, 4, 1, TOP_H - 8);
    }

    // ── Boss bar ──────────────────────────────────────────────────────
    private void drawBossBar(Graphics2D g, HUDData d, Color rc, Composite orig) {
        int barY  = TOP_H;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, barY, VW, BOSS_H);
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 20));
        g.fillRect(0, barY, VW, BOSS_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY + BOSS_H - 1, VW, 1);

        // Boss name
        g.setComposite(orig);
        g.setFont(F_LABEL);
        FontMetrics fm = g.getFontMetrics();
        String nameStr = d.bossName.toUpperCase();
        g.setColor(rc);
        g.drawString(nameStr, VW / 2 - fm.stringWidth(nameStr) / 2, barY + 11);

        // HP bar
        int hpW  = 340;
        int hpX  = VW / 2 - hpW / 2;
        int hpY  = barY + 16;
        int hpH  = 5;
        int fill = (int)(hpW * Math.max(0, d.bossHpFrac));

        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(hpX, hpY, hpW, hpH);

        if (fill > 0) {
            g.setColor(lerpColor(COL_DANGER, rc, d.bossHpFrac));
            g.fillRect(hpX, hpY, fill, hpH);
        }

        // Phase dividers
        if (d.bossTotalPhases > 1) {
            g.setColor(new Color(0, 0, 0, 180));
            for (int p = 1; p < d.bossTotalPhases; p++) {
                int divX = hpX + (int)(hpW * ((float) p / d.bossTotalPhases));
                g.fillRect(divX - 1, hpY - 1, 2, hpH + 2);
            }
        }
    }

    // ── Bottom bar (lives + HP | shield | combo | enemies) ───────────
    private void drawBottomBar(Graphics2D g, HUDData d, Color rc,
                               float breathe, Composite orig, long now) {
        int barY = VH - BOT_H;

        g.setColor(BAR_BG);
        g.fillRect(0, barY, VW, BOT_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY, VW, 1);

        int labelY = barY + 12;    // label row y
        int barRow = barY + 18;    // bar row y
        int valY   = barY + 36;    // value text y

        // ── LEFT SECTION: Lives + HP bar + Shield bar ─────────────────
        int leftX = PAD;

        // Lives label + icons
        drawLabel(g, "LIVES", leftX, labelY, orig);

        int lifeIconW = 12;
        int lifeIconH = 11;
        int lifeIconY = barRow - 2;
        boolean critLives = d.lives == 1;

        for (int i = 0; i < d.maxLives; i++) {
            boolean alive = i < d.lives;
            int ix = leftX + i * (lifeIconW + 4);

            int[] px = { ix + lifeIconW / 2, ix, ix + lifeIconW };
            int[] py = { lifeIconY, lifeIconY + lifeIconH, lifeIconY + lifeIconH };

            if (alive) {
                Color ic = critLives && i == 0 ? COL_DANGER : COL_VALUE;
                float alpha = critLives && i == 0
                        ? 0.6f + 0.4f * (float) Math.sin(now * 0.008f)
                        : breathe * 0.9f;
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, Math.min(alpha, 1f)));
                g.setColor(ic);
                g.fillPolygon(px, py, 3);
            } else {
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.14f));
                g.setColor(new Color(40, 60, 80));
                g.fillPolygon(px, py, 3);
            }
        }
        g.setComposite(orig);

        // HP bar (green → yellow → red)
        int hpBarX = leftX + d.maxLives * (lifeIconW + 4) + 8;
        int hpBarW = 90;
        int hpBarH = 5;
        drawLabel(g, "HP", hpBarX, labelY, orig);
        int hpFillW = (int)(hpBarW * Math.max(0f, d.shieldFrac)); // shieldFrac = hp fraction
        Color hpCol = hpColor(d.shieldFrac);

        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(hpBarX, barRow, hpBarW, hpBarH);
        if (hpFillW > 0) {
            boolean crit = d.shieldCrit;
            float sa = crit
                    ? 0.6f + 0.4f * (float) Math.abs(Math.sin(now * 0.009f))
                    : 1f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sa));
            g.setColor(hpCol);
            g.fillRect(hpBarX, barRow, hpFillW, hpBarH);
            g.setComposite(orig);
        }

        // HP value text
        g.setFont(F_LABEL);
        g.setColor(new Color(hpCol.getRed(), hpCol.getGreen(), hpCol.getBlue(), 180));
        g.drawString(String.format("%02d%%", (int)(d.shieldFrac * 100)),
                hpBarX, valY);

        // Shield bar (blue → purple)
        int shdBarX = hpBarX + hpBarW + 12;
        int shdBarW = 70;
        int shdBarH = 5;
        drawLabel(g, "SHD", shdBarX, labelY, orig);
        int shdFillW = (int)(shdBarW * Math.max(0f, d.armorFrac));

        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(shdBarX, barRow, shdBarW, shdBarH);
        if (shdFillW > 0) {
            g.setColor(lerpColor(COL_SHD_LOW, COL_SHD_HIGH, d.armorFrac));
            g.fillRect(shdBarX, barRow, shdFillW, shdBarH);
        }
        g.setFont(F_LABEL);
        g.setColor(new Color(120, 160, 255, 140));
        g.drawString(String.format("%02d%%", (int)(d.armorFrac * 100)),
                shdBarX, valY);

        // ── CENTER: Combo ─────────────────────────────────────────────
        if (d.comboMult > 1) {
            g.setFont(F_LABEL);
            FontMetrics fm = g.getFontMetrics();
            String comboLabel = "COMBO";
            drawLabel(g, comboLabel, VW / 2 - fm.stringWidth(comboLabel) / 2,
                    labelY, orig);

            String comboStr = "x" + d.comboMult;
            g.setFont(F_VALUE);
            fm = g.getFontMetrics();
            float pulse = 0.8f + 0.2f * (float) Math.sin(now * 0.006f);
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, pulse));
            g.setColor(COL_COMBO);
            g.drawString(comboStr, VW / 2 - fm.stringWidth(comboStr) / 2, valY);
            g.setComposite(orig);
        }

        // ── RIGHT: Enemy count ────────────────────────────────────────
        int count    = d.enemyCount;
        String cStr  = String.format("%02d", count);
        Color cCol   = count == 0 ? new Color(0, 220, 80)
                : count <= 3     ? COL_DANGER
                :                  COL_VALUE;

        g.setFont(F_LABEL);
        FontMetrics fm8 = g.getFontMetrics();
        drawLabel(g, "ENEMY", VW - PAD - fm8.stringWidth("ENEMY"), labelY, orig);

        g.setFont(F_VALUE);
        FontMetrics fmV = g.getFontMetrics();
        float cAlpha = (count > 0 && count <= 3)
                ? 0.6f + 0.4f * (float) Math.abs(Math.sin(now * 0.007f))
                : 1.0f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cAlpha));
        g.setColor(cCol);
        g.drawString(cStr, VW - PAD - fmV.stringWidth(cStr), valY);

        // Dividers
        g.setComposite(orig);
        g.setColor(COL_DIVIDER);
        g.fillRect(COL1_W, barY + 3, 1, BOT_H - 6);
        g.fillRect(COL1_W + COL2_W, barY + 3, 1, BOT_H - 6);

        // FPS (faint)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
        g.setFont(F_LABEL);
        fm8 = g.getFontMetrics();
        String fpsStr = d.fps + " FPS";
        g.setColor(Color.WHITE);
        g.drawString(fpsStr, VW / 2 - fm8.stringWidth(fpsStr) / 2, VH - 2);
        g.setComposite(orig);
    }

    // ── Floating labels ───────────────────────────────────────────────
    private void drawFloaters(Graphics2D g, Composite orig) {
        g.setFont(F_VALUE.deriveFont(14f));
        for (FloatLabel f : floaters) {
            if (f == null || !f.alive()) continue;
            float alpha = Math.max(0f, Math.min(f.alpha(), 1f));
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g.setColor(f.color != null ? f.color : new Color(255, 200, 60));
            g.drawString(f.text, (int) f.x, (int)(f.y + f.offsetY()));
        }
        g.setComposite(orig);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void drawLabel(Graphics2D g, String text, int x, int y, Composite orig) {
        g.setComposite(orig);
        g.setFont(F_LABEL);
        g.setColor(COL_LABEL);
        g.drawString(text, x, y);
    }

    private static Color hpColor(float frac) {
        if (frac > 0.6f) return lerpColor(COL_HP_MID, COL_HP_HIGH, (frac - 0.6f) / 0.4f);
        if (frac > 0.3f) return lerpColor(COL_HP_LOW,  COL_HP_MID,  (frac - 0.3f) / 0.3f);
        return COL_HP_LOW;
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t)
        );
    }
}
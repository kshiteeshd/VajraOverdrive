package ui.hud;

import config.LayoutConfig;
import ui.theme.UIFonts;

import java.awt.*;

/**
 * NebulaHUD — two-bar overlay HUD.
 *
 * LAYOUT REVISION:
 *  - Top bar height 32 → 42px. Label and value rows now have proper
 *    vertical breathing room instead of being 6px apart.
 *  - Bottom bar height 44 → 56px. Elements are vertically centred with
 *    consistent gaps between label, bar, and value text rows.
 *  - Bottom-left section shows LIVES + HP bar only (SHD moved to centre).
 *  - Bottom-centre shows SHD bar on the left + COMBO always visible on
 *    the right (dimmed at x1, bright when active).
 *  - PAD increased 10 → 16 so content breathes from the column edges.
 */
public class NebulaHUD {

    // ── Layout constants ──────────────────────────────────────────────
    private static final int VW     = LayoutConfig.VIRTUAL_WIDTH;
    private static final int VH     = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int TOP_H  = 42;   // was 32
    private static final int BOSS_H = 30;   // was 28
    private static final int BOT_H  = 56;   // was 44
    private static final int PAD    = 16;   // was 10

    private static final int COL1_W = VW / 3;
    private static final int COL2_W = VW / 3;

    // ── Fonts ─────────────────────────────────────────────────────────
    private static Font    F_LABEL;
    private static Font    F_VALUE;
    private static boolean fontsLoaded = false;

    private static void ensureFonts() {
        if (fontsLoaded) return;
        F_LABEL = UIFonts.SMALL.deriveFont(10f);
        F_VALUE = UIFonts.MENU.deriveFont(18f);
        fontsLoaded = true;
    }

    // ── Colours ───────────────────────────────────────────────────────
    private static final Color BAR_BG       = new Color(0,   0,   0,   175);
    private static final Color BAR_BORDER   = new Color(0,   255, 255,  30);
    private static final Color COL_LABEL    = new Color(0,   180, 180, 150);
    private static final Color COL_VALUE    = new Color(0,   232, 255);
    private static final Color COL_SCORE    = new Color(255, 180,   0);
    private static final Color COL_DIVIDER  = new Color(0,   255, 255,  18);
    private static final Color COL_DANGER   = new Color(255,  50,  50);
    private static final Color COL_COMBO    = new Color(255, 140,   0);
    private static final Color COL_HP_HIGH  = new Color( 40, 220,  90);
    private static final Color COL_HP_MID   = new Color(255, 200,   0);
    private static final Color COL_HP_LOW   = new Color(255,  50,  50);
    private static final Color COL_SHD_HIGH = new Color( 80, 160, 255);
    private static final Color COL_SHD_LOW  = new Color(180,  80, 255);

    // ── Animation state ───────────────────────────────────────────────
    private final long birthTime  = System.currentTimeMillis();
    private int  lastScore        = 0;
    private long scorePoppedAt    = 0;
    private static final long SCORE_POP_MS = 250;

    // ── Floating score labels ─────────────────────────────────────────
    private static final int  MAX_FLOATERS = 12;
    private static final long FLOATER_LIFE = 2500;
    private static final long FLOATER_HOLD = 1500;
    private final FloatLabel[] floaters    = new FloatLabel[MAX_FLOATERS];
    private int floaterHead = 0;

    private static class FloatLabel {
        String text; float x, y; long bornAt; Color color;

        boolean alive() { return System.currentTimeMillis() - bornAt < FLOATER_LIFE; }

        float alpha() {
            long age = System.currentTimeMillis() - bornAt;
            if (age < FLOATER_HOLD) return 1.0f;
            float t = (age - FLOATER_HOLD) / (float)(FLOATER_LIFE - FLOATER_HOLD);
            return 1f - t;
        }

        float offsetY() {
            float t = Math.min((System.currentTimeMillis() - bornAt) / (float) FLOATER_LIFE, 1f);
            return -t * 20f;
        }
    }

    // ── Public API ────────────────────────────────────────────────────

    public void spawnFloater(String text, float worldX, float worldY, Color color) {
        FloatLabel f = new FloatLabel();
        f.text = text; f.x = worldX; f.y = worldY;
        f.color = color; f.bornAt = System.currentTimeMillis();
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

        if (d.score != lastScore) { scorePoppedAt = now; lastScore = d.score; }
        float scorePop = 0f;
        if (now - scorePoppedAt < SCORE_POP_MS) {
            float t = (now - scorePoppedAt) / (float) SCORE_POP_MS;
            scorePop = (float) Math.sin(t * Math.PI);
        }

        float breathe = 0.85f + 0.15f * (float) Math.sin(
                ((now - birthTime) % 4000) / 4000f * Math.PI * 2);

        Color     rc   = d.regionColor();
        Composite orig = g.getComposite();

        drawTopBar(g, d, rc, scorePop, breathe, orig, now);
        if (d.bossActive) drawBossBar(g, d, rc, orig);
        drawBottomBar(g, d, rc, breathe, orig, now);
        drawFloaters(g, orig);

        g.setComposite(orig);
    }

    // ── Top bar ───────────────────────────────────────────────────────
    private void drawTopBar(Graphics2D g, HUDData d, Color rc,
                            float scorePop, float breathe,
                            Composite orig, long now) {
        g.setColor(BAR_BG);
        g.fillRect(0, 0, VW, TOP_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, TOP_H - 1, VW, 1);

        // Two text rows inside 42px bar: label ~30%, value ~80%
        int labelY = 14;
        int valueY = 35;

        // ── LEFT: Score ───────────────────────────────────────────────
        drawLabel(g, "SCORE", PAD, labelY, orig);
        g.setComposite(orig);
        g.setFont(F_VALUE);
        g.setColor(scorePop > 0.05f ? new Color(255, 255, 120) : COL_SCORE);
        g.drawString(String.format("%08d", d.score), PAD, valueY);

        // ── CENTRE: Wave + formation ──────────────────────────────────
        g.setFont(F_LABEL);
        FontMetrics fm = g.getFontMetrics();
        String wLabel = "WAVE";
        drawLabel(g, wLabel, VW / 2 - fm.stringWidth(wLabel) / 2, labelY, orig);

        String wStr = String.format("W%02d", d.wave);
        String fStr = "  " + HUDData.formationCode(d.formation);
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        int cw = fm.stringWidth(wStr + fStr);
        int cwX = VW / 2 - cw / 2;
        g.setComposite(orig);
        g.setColor(COL_VALUE);
        g.drawString(wStr, cwX, valueY);
        g.setColor(new Color(0, 150, 150, 160));
        g.drawString(fStr, cwX + fm.stringWidth(wStr), valueY);

        // ── RIGHT: Region + level ─────────────────────────────────────
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        String secLabel = "SECTOR";
        drawLabel(g, secLabel, VW - PAD - fm.stringWidth(secLabel), labelY, orig);

        String regStr = d.region;
        String lvlStr = String.format("LVL%02d", d.level);
        g.setFont(F_LABEL);
        fm = g.getFontMetrics();
        int lvlW = fm.stringWidth(lvlStr);
        int regW = fm.stringWidth(regStr);
        g.setComposite(orig);
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), (int)(breathe * 220)));
        g.drawString(regStr, VW - PAD - lvlW - 10 - regW, valueY);
        g.setColor(COL_VALUE);
        g.drawString(lvlStr, VW - PAD - lvlW, valueY);

        // Dividers
        g.setColor(COL_DIVIDER);
        g.fillRect(COL1_W,          6, 1, TOP_H - 12);
        g.fillRect(COL1_W + COL2_W, 6, 1, TOP_H - 12);
    }

    // ── Boss bar ──────────────────────────────────────────────────────
    private void drawBossBar(Graphics2D g, HUDData d, Color rc, Composite orig) {
        int barY = TOP_H;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, barY, VW, BOSS_H);
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 20));
        g.fillRect(0, barY, VW, BOSS_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY + BOSS_H - 1, VW, 1);

        g.setComposite(orig);
        g.setFont(F_LABEL);
        FontMetrics fm = g.getFontMetrics();
        String nameStr = d.bossName.toUpperCase();
        g.setColor(rc);
        g.drawString(nameStr, VW / 2 - fm.stringWidth(nameStr) / 2, barY + 13);

        int hpW  = 360;
        int hpX  = VW / 2 - hpW / 2;
        int hpY  = barY + 20;
        int hpH  = 5;
        int fill = (int)(hpW * Math.max(0, d.bossHpFrac));

        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(hpX, hpY, hpW, hpH);
        if (fill > 0) {
            g.setColor(lerpColor(COL_DANGER, rc, d.bossHpFrac));
            g.fillRect(hpX, hpY, fill, hpH);
        }

        if (d.bossTotalPhases > 1) {
            g.setColor(new Color(0, 0, 0, 180));
            for (int p = 1; p < d.bossTotalPhases; p++) {
                int divX = hpX + (int)(hpW * ((float) p / d.bossTotalPhases));
                g.fillRect(divX - 1, hpY - 1, 2, hpH + 2);
            }
        }
    }

    // ── Bottom bar ────────────────────────────────────────────────────
    // Left third:   LIVES icons + HP bar
    // Centre third: SHD bar (left half) + COMBO (right half, always shown)
    // Right third:  ENEMY count
    private void drawBottomBar(Graphics2D g, HUDData d, Color rc,
                               float breathe, Composite orig, long now) {
        int barY = VH - BOT_H;

        g.setColor(BAR_BG);
        g.fillRect(0, barY, VW, BOT_H);
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY, VW, 1);

        // Row positions inside 56px bar
        int labelY = barY + 13;   // label text baseline
        int barRow = barY + 26;   // progress-bar top
        int barH   = 5;
        int valY   = barY + 47;   // value text baseline

        // ── LEFT: Lives + HP ─────────────────────────────────────────
        int leftX = PAD;

        drawLabel(g, "LIVES", leftX, labelY, orig);

        int iconW = 12, iconH = 11, iconY = barRow - 3;
        boolean critLives = (d.lives == 1);

        for (int i = 0; i < d.maxLives; i++) {
            boolean alive = i < d.lives;
            int ix = leftX + i * (iconW + 5);
            int[] px = { ix + iconW / 2, ix, ix + iconW };
            int[] py = { iconY, iconY + iconH, iconY + iconH };

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
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f));
                g.setColor(new Color(40, 60, 80));
                g.fillPolygon(px, py, 3);
            }
        }
        g.setComposite(orig);

        // HP bar — right of life icons
        int hpBarX = leftX + d.maxLives * (iconW + 5) + 14;
        int hpBarW = COL1_W - hpBarX - PAD;
        drawLabel(g, "HP", hpBarX, labelY, orig);

        Color hpCol   = hpColor(d.shieldFrac);
        int   hpFillW = (int)(hpBarW * Math.max(0f, d.shieldFrac));
        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(hpBarX, barRow, hpBarW, barH);
        if (hpFillW > 0) {
            float sa = d.shieldCrit
                    ? 0.6f + 0.4f * (float) Math.abs(Math.sin(now * 0.009f)) : 1f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sa));
            g.setColor(hpCol);
            g.fillRect(hpBarX, barRow, hpFillW, barH);
            g.setComposite(orig);
        }
        g.setFont(F_LABEL);
        g.setColor(new Color(hpCol.getRed(), hpCol.getGreen(), hpCol.getBlue(), 180));
        g.drawString(String.format("%02d%%", (int)(d.shieldFrac * 100)), hpBarX, valY);

        // ── CENTRE: SHD + COMBO ───────────────────────────────────────
        int centreX  = COL1_W + PAD;
        int centreEnd = COL1_W + COL2_W - PAD;
        int centreW  = centreEnd - centreX;
        int halfW    = centreW / 2 - 8;

        // SHD bar (left half of centre)
        drawLabel(g, "SHD", centreX, labelY, orig);
        int shdFillW = (int)(halfW * Math.max(0f, d.armorFrac));
        g.setColor(new Color(255, 255, 255, 18));
        g.fillRect(centreX, barRow, halfW, barH);
        if (shdFillW > 0) {
            g.setColor(lerpColor(COL_SHD_LOW, COL_SHD_HIGH, d.armorFrac));
            g.fillRect(centreX, barRow, shdFillW, barH);
        }
        g.setFont(F_LABEL);
        g.setColor(new Color(120, 160, 255, 140));
        g.drawString(String.format("%02d%%", (int)(d.armorFrac * 100)), centreX, valY);

        // COMBO (right half of centre) — always rendered, dimmed when x1
        int comboX   = centreX + halfW + 16;
        boolean hasCombo = d.comboMult > 1;
        drawLabel(g, "COMBO", comboX, labelY, orig);
        String comboStr = "x" + d.comboMult;
        g.setFont(F_VALUE);
        FontMetrics fmV = g.getFontMetrics();
        float comboAlpha = hasCombo
                ? 0.8f + 0.2f * (float) Math.sin(now * 0.006f) : 0.22f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, comboAlpha));
        g.setColor(hasCombo ? COL_COMBO : COL_VALUE);
        g.drawString(comboStr, comboX, valY);
        g.setComposite(orig);

        // ── RIGHT: Enemy count ────────────────────────────────────────
        int count = d.enemyCount;
        String cStr = String.format("%02d", count);
        Color cCol  = count == 0 ? new Color(0, 220, 80)
                    : count <= 3 ? COL_DANGER : COL_VALUE;

        g.setFont(F_LABEL);
        FontMetrics fmL = g.getFontMetrics();
        String enemyLabel = "ENEMY";
        drawLabel(g, enemyLabel, VW - PAD - fmL.stringWidth(enemyLabel), labelY, orig);

        g.setFont(F_VALUE);
        fmV = g.getFontMetrics();
        float cAlpha = (count > 0 && count <= 3)
                ? 0.6f + 0.4f * (float) Math.abs(Math.sin(now * 0.007f)) : 1.0f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cAlpha));
        g.setColor(cCol);
        g.drawString(cStr, VW - PAD - fmV.stringWidth(cStr), valY);
        g.setComposite(orig);

        // Column dividers
        g.setColor(COL_DIVIDER);
        g.fillRect(COL1_W,          barY + 4, 1, BOT_H - 8);
        g.fillRect(COL1_W + COL2_W, barY + 4, 1, BOT_H - 8);

        // FPS — very faint, bottom-centre
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.14f));
        g.setFont(F_LABEL);
        FontMetrics fmFps = g.getFontMetrics();
        String fpsStr = d.fps + " FPS";
        g.setColor(Color.WHITE);
        g.drawString(fpsStr, VW / 2 - fmFps.stringWidth(fpsStr) / 2, VH - 2);
        g.setComposite(orig);
    }

    // ── Floating labels ───────────────────────────────────────────────
    private void drawFloaters(Graphics2D g, Composite orig) {
        g.setFont(F_VALUE.deriveFont(14f));
        for (FloatLabel f : floaters) {
            if (f == null || !f.alive()) continue;
            float alpha = Math.max(0f, Math.min(f.alpha(), 1f));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
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

package ui.hud;

import config.LayoutConfig;
import ui.theme.UIFonts;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * NebulaHUD — two-bar overlay HUD.
 *
 * Layout:
 *   TOP BAR  (y=0..28)  : score | wave+formation | region+level
 *   BOSS BAR (y=28..52) : visible only during boss waves
 *   BOTTOM BAR (y=H-28..H) : lives | shield | combo | enemies
 *
 * Font: Press Start 2P (UIFonts) throughout — no Courier New.
 * Fonts are cached as static finals — no per-frame allocation.
 */
public class NebulaHUD {

    // ── Cached fonts (allocated once) ────────────────────────────────
    // We derive from UIFonts.SMALL (8px) and UIFonts.MENU (16px).
    // Press Start 2P is a bitmap font — only use multiples of 8.
    private static Font F8;     // 8px  — labels, hints
    private static Font F16;    // 16px — values
    private static Font F24;    // 24px — large enemy count
    private static boolean fontsLoaded = false;

    private static void ensureFonts() {
        if (fontsLoaded) return;
        // UIFonts.SMALL is already 8px Press Start 2P
        // Derive larger sizes from the same font object
        F8  = UIFonts.SMALL;
        F16 = UIFonts.MENU;
        F24 = UIFonts.MENU.deriveFont(24f);
        fontsLoaded = true;
    }

    // ── Layout constants ─────────────────────────────────────────────
    private static final int W       = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H       = LayoutConfig.VIRTUAL_HEIGHT;
    private static final int BAR_H   = 28;   // top and bottom bar height
    private static final int BOSS_H  = 26;   // boss bar height below top bar
    private static final int PAD     = 8;    // inner horizontal padding

    // ── Bar colors ────────────────────────────────────────────────────
    private static final Color BAR_BG        = new Color(0, 0, 0, 160);
    private static final Color BAR_BORDER     = new Color(0, 255, 255, 30);
    private static final Color COL_LABEL      = new Color(0, 180, 180, 160);
    private static final Color COL_VALUE      = new Color(0, 232, 255);
    private static final Color COL_SCORE      = new Color(255, 180, 0);
    private static final Color COL_DIVIDER    = new Color(0, 255, 255, 18);
    private static final Color COL_DANGER     = new Color(255, 50, 50);
    private static final Color COL_COMBO      = new Color(255, 140, 0);
    private static final Color COL_SHIELD_OK  = new Color(0, 220, 80);
    private static final Color COL_SHIELD_MID = new Color(255, 200, 0);
    private static final Color COL_SHIELD_LOW = new Color(255, 50, 50);

    // ── Animation state ───────────────────────────────────────────────
    private long  birthTime     = System.currentTimeMillis();
    private int   lastScore     = 0;
    private long  scorePoppedAt = 0;
    private static final long SCORE_POP_MS = 220;

    // ── Floating score labels ─────────────────────────────────────────
    private static final int MAX_FLOATERS = 8;
    private final FloatLabel[] floaters   = new FloatLabel[MAX_FLOATERS];
    private int floaterHead = 0;

    private static class FloatLabel {
        String text;
        float  x, y;
        long   bornAt;
        Color  color;
        static final long LIFE = 800;

        boolean alive() {
            return System.currentTimeMillis() - bornAt < LIFE;
        }
        float alpha() {
            float t = (System.currentTimeMillis() - bornAt) / (float) LIFE;
            if (t < 0.12f) return t / 0.12f;
            if (t < 0.60f) return 1f;
            return 1f - (t - 0.60f) / 0.40f;
        }
        float offsetY() {
            float t = (System.currentTimeMillis() - bornAt) / (float) LIFE;
            return -t * 32f;
        }
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Spawn a floating label at a world position.
     * Call from ScoreManager or CollisionSystem after kills.
     */
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

        // Score pop animation
        if (d.score != lastScore) {
            scorePoppedAt = now;
            lastScore     = d.score;
        }
        float scorePop = 0f;
        if (now - scorePoppedAt < SCORE_POP_MS) {
            float t = (now - scorePoppedAt) / (float) SCORE_POP_MS;
            scorePop = (float) Math.sin(t * Math.PI);
        }

        // Breathe pulse (slow, subtle)
        float breathe = 0.85f + 0.15f * (float) Math.sin(
                ((now - birthTime) % 4000) / 4000f * Math.PI * 2);

        Color rc = d.regionColor();
        Composite orig = g.getComposite();

        drawTopBar(g, d, rc, scorePop, breathe, orig);

        if (d.bossActive) {
            drawBossBar(g, d, rc, orig);
        }

        drawBottomBar(g, d, rc, breathe, orig);
        drawFloaters(g, orig);

        g.setComposite(orig);
    }

    // ── Top bar ───────────────────────────────────────────────────────
    private void drawTopBar(Graphics2D g, HUDData d, Color rc,
                            float scorePop, float breathe, Composite orig) {

        // Bar background
        g.setColor(BAR_BG);
        g.fillRect(0, 0, W, BAR_H);

        // Bottom edge line
        g.setColor(BAR_BORDER);
        g.fillRect(0, BAR_H - 1, W, 1);

        // ── LEFT: Score ───────────────────────────────────────────────
        drawLabel(g, "SCORE", PAD, 9, orig);

        // Score value — pops yellow on change
        Color scoreCol = scorePop > 0.05f
                ? new Color(255, 255, 100)
                : COL_SCORE;
        g.setComposite(orig);
        g.setFont(F16);
        g.setColor(scoreCol);
        g.drawString(String.format("%08d", d.score), PAD, 24);

        // ── CENTER: Wave + formation ──────────────────────────────────
        g.setFont(F8);
        FontMetrics fm8 = g.getFontMetrics();

        String waveStr = String.format("W%02d", d.wave);
        String fmtStr  = " " + HUDData.formationCode(d.formation);
        int    cwTotal = fm8.stringWidth(waveStr + fmtStr);
        int    cwX     = W / 2 - cwTotal / 2;

        drawLabel(g, "WAVE", W / 2 - fm8.stringWidth("WAVE") / 2, 9, orig);

        g.setComposite(orig);
        g.setFont(F8);
        g.setColor(COL_VALUE);
        g.drawString(waveStr, cwX, 24);
        g.setColor(new Color(0, 160, 160, 180));
        g.drawString(fmtStr, cwX + fm8.stringWidth(waveStr), 24);

        // ── RIGHT: Region + level ─────────────────────────────────────
        g.setFont(F8);
        fm8 = g.getFontMetrics();

        String regionStr = d.region;
        String levelStr  = String.format("LVL%02d", d.level);
        int    rx1       = W - PAD - fm8.stringWidth(regionStr);
        int    rx2       = W - PAD - fm8.stringWidth(levelStr);

        drawLabel(g, "SECTOR", W - PAD - fm8.stringWidth("SECTOR"), 9, orig);

        g.setComposite(orig);
        g.setFont(F8);

        // Region name in region accent color
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(),
                (int)(breathe * 220)));
        g.drawString(regionStr, rx1, 19);

        g.setColor(COL_VALUE);
        g.drawString(levelStr, rx2, 24);

        // ── Vertical dividers ─────────────────────────────────────────
        g.setColor(COL_DIVIDER);
        g.fillRect(W / 3, 4, 1, BAR_H - 8);
        g.fillRect(W * 2 / 3, 4, 1, BAR_H - 8);
    }

    // ── Boss bar ──────────────────────────────────────────────────────
    private void drawBossBar(Graphics2D g, HUDData d, Color rc, Composite orig) {
        int barY  = BAR_H;
        int barBg = 24; // height of the boss bar region

        // Dark backing
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, barY, W, barBg);
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 20));
        g.fillRect(0, barY, W, barBg);

        // Bottom edge
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY + barBg - 1, W, 1);

        // Boss name — centered
        g.setComposite(orig);
        g.setFont(F8);
        FontMetrics fm = g.getFontMetrics();
        String nameStr = d.bossName.toUpperCase();
        int nx = W / 2 - fm.stringWidth(nameStr) / 2;
        g.setColor(rc);
        g.drawString(nameStr, nx, barY + 10);

        // HP bar
        int hpW   = 320;
        int hpX   = W / 2 - hpW / 2;
        int hpY   = barY + 14;
        int hpH   = 4;

        // Track
        g.setColor(new Color(255, 255, 255, 20));
        g.fillRect(hpX, hpY, hpW, hpH);

        // Fill — shifts from region color to red as HP drops
        int fillW = (int)(hpW * Math.max(0, d.bossHpFrac));
        if (fillW > 0) {
            Color fillCol = lerpColor(COL_DANGER, rc, d.bossHpFrac);
            g.setColor(fillCol);
            g.fillRect(hpX, hpY, fillW, hpH);
        }

        // Phase divider lines
        if (d.bossTotalPhases > 1) {
            g.setColor(new Color(0, 0, 0, 160));
            for (int p = 1; p < d.bossTotalPhases; p++) {
                int divX = hpX + (int)(hpW * ((float) p / d.bossTotalPhases));
                g.fillRect(divX - 1, hpY - 1, 2, hpH + 2);
            }
        }

        // Phase dots
        int dotSpacing = 10;
        int dotsX = W / 2 - (d.bossTotalPhases * dotSpacing) / 2;
        for (int p = 0; p < d.bossTotalPhases; p++) {
            int dotX = dotsX + p * dotSpacing + 4;
            int dotY = hpY + hpH + 4;
            if (p < d.bossPhase - 1) {
                g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 80));
            } else if (p == d.bossPhase - 1) {
                g.setColor(rc);
            } else {
                g.setColor(new Color(50, 50, 70));
            }
            g.fillOval(dotX, dotY, 4, 4);
        }
    }

    // ── Bottom bar ────────────────────────────────────────────────────
    private void drawBottomBar(Graphics2D g, HUDData d, Color rc,
                               float breathe, Composite orig) {
        int barY = H - BAR_H;

        // Background
        g.setColor(BAR_BG);
        g.fillRect(0, barY, W, BAR_H);

        // Top edge line
        g.setColor(BAR_BORDER);
        g.fillRect(0, barY, W, 1);

        // ── LEFT: Lives ───────────────────────────────────────────────
        drawLabel(g, "LIVES", PAD, barY + 9, orig);

        boolean critLives = d.lives == 1;
        for (int i = 0; i < d.maxLives; i++) {
            boolean alive = i < d.lives;
            int ix = PAD + i * 14;
            int iy = barY + 13;

            int[] px = { ix + 5, ix,      ix + 10 };
            int[] py = { iy,     iy + 10,  iy + 10 };

            if (alive) {
                Color ic = critLives && i == 0
                        ? COL_DANGER
                        : COL_VALUE;
                float alpha = critLives && i == 0
                        ? 0.6f + 0.4f * (float) Math.sin(
                        System.currentTimeMillis() * 0.008f)
                        : breathe * 0.9f;
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, Math.min(alpha, 1f)));
                g.setColor(ic);
                g.fillPolygon(px, py, 3);
            } else {
                g.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.15f));
                g.setColor(new Color(40, 60, 80));
                g.fillPolygon(px, py, 3);
            }
        }

        // ── LEFT-CENTER: Shield bar ───────────────────────────────────
        int shieldX = PAD + d.maxLives * 14 + 10;
        drawLabel(g, "SHD", shieldX, barY + 9, orig);

        int   shieldBarW = 80;
        int   shieldBarH = 4;
        int   shieldBarY = barY + 13;
        int   shieldFillW = (int)(shieldBarW * Math.max(0f, d.shieldFrac));
        Color shieldCol   = shieldColor(d.shieldFrac);

        g.setComposite(orig);
        g.setColor(new Color(255, 255, 255, 20));
        g.fillRect(shieldX, shieldBarY, shieldBarW, shieldBarH);

        if (shieldFillW > 0) {
            // Pulsing when critical
            float sa = d.shieldCrit
                    ? 0.6f + 0.4f * (float) Math.abs(
                    Math.sin(System.currentTimeMillis() * 0.009f))
                    : 0.9f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sa));
            g.setColor(shieldCol);
            g.fillRect(shieldX, shieldBarY, shieldFillW, shieldBarH);
        }

        // Shield percentage text
        g.setComposite(orig);
        g.setFont(F8);
        g.setColor(new Color(shieldCol.getRed(), shieldCol.getGreen(),
                shieldCol.getBlue(), 160));
        g.drawString(String.format("%02d%%",
                (int)(d.shieldFrac * 100)), shieldX, barY + 24);

        // ── CENTER: Combo multiplier ──────────────────────────────────
        if (d.comboMult > 1) {
            g.setFont(F8);
            FontMetrics fm8 = g.getFontMetrics();
            String comboStr = "x" + d.comboMult;
            int cx = W / 2 - fm8.stringWidth(comboStr) / 2;

            drawLabel(g, "COMBO", W / 2 - fm8.stringWidth("COMBO") / 2,
                    barY + 9, orig);

            // Pulse combo value
            float pulse = 0.8f + 0.2f * (float) Math.sin(
                    System.currentTimeMillis() * 0.006f);
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, pulse));
            g.setFont(F8);
            g.setColor(COL_COMBO);
            g.drawString(comboStr, cx, barY + 24);
        }

        // ── RIGHT: Enemy count ────────────────────────────────────────
        drawLabel(g, "ENEMY", W - PAD - enemyLabelWidth(g, d), barY + 9, orig);

        int    count    = d.enemyCount;
        String countStr = String.format("%02d", count);

        // Color: white when many, shifts to red as count drops
        Color countCol;
        if (count == 0) {
            countCol = new Color(0, 220, 80);       // green — cleared
        } else if (count <= 3) {
            countCol = COL_DANGER;
        } else {
            countCol = COL_VALUE;
        }

        // Blink when ≤3 and alive
        float countAlpha = (count > 0 && count <= 3)
                ? 0.6f + 0.4f * (float) Math.abs(
                Math.sin(System.currentTimeMillis() * 0.007f))
                : 1.0f;

        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, countAlpha));
        g.setFont(F16);
        FontMetrics fm16 = g.getFontMetrics();
        g.setColor(countCol);
        g.drawString(countStr, W - PAD - fm16.stringWidth(countStr), barY + 25);

        // ── RIGHT divider ─────────────────────────────────────────────
        g.setComposite(orig);
        g.setColor(COL_DIVIDER);
        g.fillRect(W / 3,     barY + 2, 1, BAR_H - 4);
        g.fillRect(W * 2 / 3, barY + 2, 1, BAR_H - 4);

        // ── FPS (very faint, center bottom edge) ──────────────────────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setFont(F8);
        FontMetrics fmFps = g.getFontMetrics();
        String fpsStr = d.fps + " FPS";
        g.setColor(Color.WHITE);
        g.drawString(fpsStr, W / 2 - fmFps.stringWidth(fpsStr) / 2, H - 1);

        g.setComposite(orig);
    }

    // ── Floating labels ───────────────────────────────────────────────
    private void drawFloaters(Graphics2D g, Composite orig) {
        g.setFont(F8);
        for (FloatLabel f : floaters) {
            if (f == null || !f.alive()) continue;
            float alpha = Math.max(0f, f.alpha());
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g.setColor(f.color != null ? f.color : new Color(255, 200, 60));
            g.drawString(f.text, (int) f.x, (int)(f.y + f.offsetY()));
        }
        g.setComposite(orig);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Draw a small muted label at the given position. */
    private void drawLabel(Graphics2D g, String text, int x, int y, Composite orig) {
        g.setComposite(orig);
        g.setFont(F8);
        g.setColor(COL_LABEL);
        g.drawString(text, x, y);
    }

    private int enemyLabelWidth(Graphics2D g, HUDData d) {
        g.setFont(F16);
        FontMetrics fm = g.getFontMetrics();
        return fm.stringWidth(String.format("%02d", d.enemyCount));
    }

    private static Color shieldColor(float frac) {
        if (frac > 0.5f) {
            return lerpColor(COL_SHIELD_MID, COL_SHIELD_OK, (frac - 0.5f) * 2f);
        } else {
            return lerpColor(COL_SHIELD_LOW, COL_SHIELD_MID, frac * 2f);
        }
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
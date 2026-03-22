package ui.hud;

import config.LayoutConfig;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * NebulaHUD — full-screen floating overlay HUD.
 *
 * FIXED:
 *  - File was truncated — drawFormationDots(), drawAura(), shieldColor(),
 *    lerpColor(), monoFont() methods were missing. Added them all.
 *  - Class renamed from NebulHUD (typo in GameCanvas import) to NebulaHUD.
 */
public class NebulaHUD {

    private static final int CLOUD_W  = 220;
    private static final int CLOUD_H  = 180;

    private static final int W = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H = LayoutConfig.VIRTUAL_HEIGHT;

    private long   birthTime      = System.currentTimeMillis();
    private float  breathePhase   = 0f;

    private int    lastScore      = 0;
    private long   scorePoppedAt  = 0;
    private static final long SCORE_POP_MS = 280;

    private long   shieldPulseAt  = 0;

    private static final int MAX_FLOATERS = 5;
    private final FloatLabel[] floaters = new FloatLabel[MAX_FLOATERS];
    private int floaterHead = 0;

    private static class FloatLabel {
        String text;
        float  x, y;
        long   bornAt;
        static final long LIFE = 900;
        boolean alive() {
            return System.currentTimeMillis() - bornAt < LIFE;
        }
        float alpha() {
            float t = (System.currentTimeMillis() - bornAt) / (float) LIFE;
            if (t < 0.15f) return t / 0.15f;
            if (t < 0.65f) return 1f;
            return 1f - (t - 0.65f) / 0.35f;
        }
        float offsetY() {
            float t = (System.currentTimeMillis() - bornAt) / (float) LIFE;
            return -t * 28f;
        }
    }

    // ── Public API ────────────────────────────────────────────────────

    public void spawnFloater(String text, float worldX, float worldY) {
        FloatLabel f = new FloatLabel();
        f.text   = text;
        f.x      = worldX;
        f.y      = worldY;
        f.bornAt = System.currentTimeMillis();
        floaters[floaterHead % MAX_FLOATERS] = f;
        floaterHead++;
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g, HUDData d) {
        long now = System.currentTimeMillis();

        breathePhase = ((now - birthTime) % 4000) / 4000f;
        float breathe = 0.85f + 0.15f * (float) Math.sin(breathePhase * Math.PI * 2);

        if (d.score != lastScore) {
            scorePoppedAt = now;
            lastScore     = d.score;
        }
        float scorePop = 0f;
        if (now - scorePoppedAt < SCORE_POP_MS) {
            float t = (now - scorePoppedAt) / (float) SCORE_POP_MS;
            scorePop = (float) Math.sin(t * Math.PI);
        }

        if (d.shieldCrit) shieldPulseAt = now;

        Color rc = d.regionColor();
        Composite orig = g.getComposite();

        drawCloudTL(g, d, rc, breathe, scorePop, orig);
        drawCloudTR(g, d, rc, breathe, orig);
        drawCloudBL(g, d, rc, breathe, orig);
        drawCloudBR(g, d, rc, breathe, orig);

        if (d.bossActive) drawBossBar(g, d, rc, orig);

        drawFloaters(g, orig);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setFont(new Font("Courier New", Font.PLAIN, 9));
        g.setColor(Color.WHITE);
        String fpsStr = d.fps + " fps";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(fpsStr, W / 2 - fm.stringWidth(fpsStr) / 2, H - 4);
        g.setComposite(orig);
    }

    // ── Cloud TL ──────────────────────────────────────────────────────
    private void drawCloudTL(Graphics2D g, HUDData d, Color rc,
                             float breathe, float scorePop, Composite orig) {
        drawAura(g, -CLOUD_W / 2, -CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2, rc, breathe * 0.82f, orig);

        float scoreAlpha = 0.90f + scorePop * 0.10f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreAlpha));

        float scoreSz = 22f + scorePop * 3f;
        g.setFont(monoFont((int) scoreSz));
        g.setColor(scorePop > 0.1f
                ? new Color(255, 220, 80)
                : new Color(255, 170, 0));
        String scoreStr = String.format("%08d", d.score);
        g.drawString(scoreStr, 10, 30);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.75f));
        g.setFont(monoFont(10));
        g.setColor(new Color(0, 200, 220));
        g.drawString(String.format("W%02d", d.wave), 10, 46);

        g.setColor(new Color(0, 160, 160, 180));
        g.drawString(HUDData.formationCode(d.formation), 48, 46);

        if (d.comboMult > 1) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() * 0.006f)));
            g.setFont(monoFont(11));
            g.setColor(new Color(255, 120, 0));
            g.drawString("x" + d.comboMult, 10, 62);
        }

        g.setComposite(orig);
    }

    // ── Cloud TR ──────────────────────────────────────────────────────
    private void drawCloudTR(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {
        drawAura(g, W - CLOUD_W - CLOUD_W / 2, -CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2, rc, breathe * 0.78f, orig);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.92f));
        g.setFont(monoFont(20));
        g.setColor(rc);
        FontMetrics fm = g.getFontMetrics();
        String sectorStr = d.region;
        g.drawString(sectorStr, W - fm.stringWidth(sectorStr) - 10, 28);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.55f));
        g.setFont(monoFont(8));
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 160));
        String modeStr = d.gameMode;
        fm = g.getFontMetrics();
        g.drawString(modeStr, W - fm.stringWidth(modeStr) - 10, 42);

        int   arcCX   = W - 18;
        int   arcCY   = 18;
        int   arcR    = 28;
        float arcFrac = (float) d.level / (float) d.totalLevels;
        float arcDeg  = arcFrac * 270f;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue()));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                arcR * 2, arcR * 2, 135f, 270f, Arc2D.OPEN));

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.85f));
        g.setColor(rc);
        if (arcDeg > 0) {
            g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                    arcR * 2, arcR * 2, 135f, arcDeg, Arc2D.OPEN));
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g.setFont(monoFont(8));
        fm = g.getFontMetrics();
        String lvlStr = String.format("%02d", d.level);
        g.setColor(rc);
        g.drawString(lvlStr, arcCX - fm.stringWidth(lvlStr) / 2,
                arcCY + fm.getAscent() / 2);

        g.setStroke(new BasicStroke(1f));
        g.setComposite(orig);
    }

    // ── Cloud BL ──────────────────────────────────────────────────────
    private void drawCloudBL(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {
        boolean crit   = d.shieldFrac < 0.20f;
        float   pulse  = crit
                ? 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() * 0.008f)
                : breathe;
        Color   auraCol = crit ? new Color(180, 20, 20) : rc;

        drawAura(g, -CLOUD_W / 2, H - CLOUD_H - CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2, auraCol, pulse * 0.80f, orig);

        int baseX = 10;
        int baseY = H - 14;

        int iconW  = 12;
        int iconH  = 14;
        int iconGap = 5;

        for (int i = 0; i < d.maxLives; i++) {
            boolean alive = i < d.lives;
            float   alpha = alive ? (pulse * 0.9f) : 0.10f;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int ix = baseX + i * (iconW + iconGap);
            int iy = baseY - 36;

            Color iconCol = alive
                    ? (crit && i == d.lives - 1
                    ? new Color(255, 80, 80)
                    : new Color(0, 200, 255))
                    : new Color(40, 60, 80);

            g.setColor(iconCol);
            int[] px = { ix + iconW / 2, ix,           ix + iconW };
            int[] py = { iy,             iy + iconH,    iy + iconH };
            g.fillPolygon(px, py, 3);

            if (alive) {
                g.setColor(iconCol.darker());
                g.fillRect(ix + iconW / 2 - 2, iy + iconH - 3, 4, 3);
            }
        }

        int arcCX  = baseX + 34;
        int arcCY  = baseY - 10;
        int arcR   = 20;

        Color shieldCol = shieldColor(d.shieldFrac);
        float shieldDeg = d.shieldFrac * 180f;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                arcR * 2, arcR * 2, 0f, 180f, Arc2D.OPEN));

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                pulse * 0.90f));
        g.setColor(shieldCol);
        if (shieldDeg > 0) {
            g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                    arcR * 2, arcR * 2, 0f, shieldDeg, Arc2D.OPEN));
        }

        if (d.armorFrac < 1.0f || d.armorFrac > 0f) {
            int   arcR2  = arcR - 6;
            float armDeg = d.armorFrac * 180f;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            g.setColor(new Color(160, 80, 255));
            g.draw(new Arc2D.Float(arcCX - arcR2, arcCY - arcR2,
                    arcR2 * 2, arcR2 * 2, 0f, 180f, Arc2D.OPEN));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    breathe * 0.75f));
            g.setColor(new Color(180, 100, 255));
            if (armDeg > 0) {
                g.draw(new Arc2D.Float(arcCX - arcR2, arcCY - arcR2,
                        arcR2 * 2, arcR2 * 2, 0f, armDeg, Arc2D.OPEN));
            }
        }

        g.setStroke(new BasicStroke(1f));
        g.setComposite(orig);
    }

    // ── Cloud BR ──────────────────────────────────────────────────────
    private void drawCloudBR(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {
        drawAura(g, W - CLOUD_W - CLOUD_W / 2, H - CLOUD_H - CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2, rc, breathe * 0.78f, orig);

        int   count     = d.enemyCount;
        float intensity = count == 0 ? 0f
                : Math.max(0.55f, 1.0f - (count / 20f));
        int   red       = (int)(180 + 75 * intensity);
        Color countCol  = new Color(Math.min(red, 255), 30, 30);

        float countAlpha = count <= 3 && count > 0
                ? 0.7f + 0.3f * (float) Math.abs(Math.sin(
                System.currentTimeMillis() * 0.007f))
                : breathe * 0.92f;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, countAlpha));
        g.setFont(monoFont(28));
        g.setColor(countCol);
        FontMetrics fm = g.getFontMetrics();
        String countStr = String.format("%02d", count);
        int countX = W - fm.stringWidth(countStr) - 10;
        g.drawString(countStr, countX, H - 36);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.60f));
        drawFormationDots(g, d.formation, W - 60, H - 22, rc);

        g.setComposite(orig);
    }

    // ── Boss bar ──────────────────────────────────────────────────────
    private void drawBossBar(Graphics2D g, HUDData d, Color rc, Composite orig) {
        int barW  = 400;
        int barH  = 6;
        int barX  = W / 2 - barW / 2;
        int barY  = 18;
        int nameY = barY - 4;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(new Color(10, 10, 20));
        g.fillRect(barX - 2, nameY - 12, barW + 4, barH + 18);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
        g.setFont(monoFont(8));
        g.setColor(rc);
        FontMetrics fm = g.getFontMetrics();
        String nameStr = d.bossName.toUpperCase();
        g.drawString(nameStr, W / 2 - fm.stringWidth(nameStr) / 2, nameY);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g.setColor(Color.BLACK);
        for (int p = 1; p < d.bossTotalPhases; p++) {
            int divX = barX + (int)(barW * ((float) p / d.bossTotalPhases));
            g.fillRect(divX - 1, barY, 2, barH);
        }

        float fillW  = d.bossHpFrac * barW;
        Color fillCol = lerpColor(new Color(220, 30, 30), rc, d.bossHpFrac);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
        g.setColor(fillCol);
        if (fillW > 0) g.fillRect(barX, barY, (int) fillW, barH);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
        g.setColor(rc);
        g.drawRect(barX, barY, barW, barH);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.70f));
        int dotSpacing = 10;
        int dotsStartX = W / 2 - (d.bossTotalPhases * dotSpacing) / 2;
        for (int p = 0; p < d.bossTotalPhases; p++) {
            int dotX = dotsStartX + p * dotSpacing;
            int dotY = barY + barH + 4;
            if (p < d.bossPhase - 1) {
                g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 80));
            } else if (p == d.bossPhase - 1) {
                g.setColor(rc);
            } else {
                g.setColor(new Color(40, 40, 60));
            }
            g.fillOval(dotX, dotY, 4, 4);
        }

        g.setComposite(orig);
    }

    // ── Floating labels ───────────────────────────────────────────────
    private void drawFloaters(Graphics2D g, Composite orig) {
        g.setFont(monoFont(11));
        for (FloatLabel f : floaters) {
            if (f == null || !f.alive()) continue;
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, Math.max(0f, f.alpha())));
            g.setColor(new Color(255, 200, 60));
            g.drawString(f.text, (int) f.x, (int)(f.y + f.offsetY()));
        }
        g.setComposite(orig);
    }

    // ── Formation dots ────────────────────────────────────────────────
    /**
     * Draws a miniature dot pattern representing the current formation.
     * Player learns to recognise the shape over time.
     */
    private void drawFormationDots(Graphics2D g, String formation,
                                   int cx, int cy, Color rc) {
        if (formation == null || formation.isEmpty() || formation.equals("---")) return;

        g.setColor(rc);
        int ds = 4; // dot size
        int sp = 7; // spacing

        switch (formation) {
            case "LINE" -> {
                for (int i = -2; i <= 2; i++)
                    g.fillOval(cx + i * sp - ds/2, cy - ds/2, ds, ds);
            }
            case "V" -> {
                int[] ox = {-2,-1,0,1,2};
                int[] oy = {-2,-1,0,-1,-2};
                for (int i = 0; i < 5; i++)
                    g.fillOval(cx + ox[i]*sp - ds/2, cy + oy[i]*sp - ds/2, ds, ds);
            }
            case "GRID" -> {
                for (int r = -1; r <= 1; r++)
                    for (int c = -1; c <= 1; c++)
                        g.fillOval(cx + c*sp - ds/2, cy + r*sp - ds/2, ds, ds);
            }
            case "CIRCLE" -> {
                int count = 6;
                int r = 10;
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    int dx = (int)(Math.cos(angle) * r);
                    int dy = (int)(Math.sin(angle) * r);
                    g.fillOval(cx + dx - ds/2, cy + dy - ds/2, ds, ds);
                }
            }
            case "STAR" -> {
                int[] ox = {0, -2, 2, -1, 1};
                int[] oy = {-2, 0, 0, 2, 2};
                for (int i = 0; i < 5; i++)
                    g.fillOval(cx + ox[i]*sp - ds/2, cy + oy[i]*sp - ds/2, ds, ds);
            }
            case "DIAMOND" -> {
                int[][] pts = {{0,-2},{-2,0},{0,0},{2,0},{0,2}};
                for (int[] pt : pts)
                    g.fillOval(cx + pt[0]*sp - ds/2, cy + pt[1]*sp - ds/2, ds, ds);
            }
            case "BOSS" -> {
                // Single large dot for boss wave
                g.fillOval(cx - 5, cy - 5, 10, 10);
            }
        }
    }

    // ── Aura pool ─────────────────────────────────────────────────────
    private void drawAura(Graphics2D g, int x, int y, int w, int h,
                          Color rc, float alpha, Composite orig) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.max(0f, Math.min(alpha * 0.22f, 1f))));
        g.setColor(new Color(
                Math.max(0, Math.min(rc.getRed()   / 4, 255)),
                Math.max(0, Math.min(rc.getGreen() / 4, 255)),
                Math.max(0, Math.min(rc.getBlue()  / 4, 255))
        ));
        g.fillOval(x, y, w, h);
        g.setComposite(orig);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private static Color shieldColor(float frac) {
        if (frac > 0.5f) {
            float t = (frac - 0.5f) * 2f;
            return lerpColor(new Color(255, 200, 0), new Color(60, 220, 80), t);
        } else {
            float t = frac * 2f;
            return lerpColor(new Color(220, 40, 40), new Color(255, 200, 0), t);
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

    private static Font monoFont(int size) {
        return new Font("Courier New", Font.BOLD, size);
    }
}
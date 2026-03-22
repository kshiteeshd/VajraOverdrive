package ui.hud;

import config.LayoutConfig;

import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * NebulaHUD — full-screen floating overlay HUD.
 *
 * Four organic dark "clouds" pool in the corners. Data floats
 * inside them. No hard borders. No labels on arcs or icons —
 * intentional learning curve. The clouds breathe (slow alpha pulse)
 * and tint to match the current region color.
 *
 * Layout:
 *   TOP-LEFT     — tactical: score, wave, combo multiplier, formation code
 *   TOP-RIGHT    — navigation: sector name, radial level arc
 *   BOTTOM-LEFT  — survival: ship-icon lives, shield arc, armor arc
 *   BOTTOM-RIGHT — threat: enemy count, formation dot-pattern
 *   CENTER-TOP   — boss health bar (visible only during boss waves)
 *
 * All rendering is done in virtual canvas coordinates (1000 × 600).
 */
public class NebulaHUD {

    // ── Cloud geometry — corner anchor points ─────────────────────────
    // Each cloud is an ellipse radiating from its corner.
    // These define how far the cloud extends inward.
    private static final int CLOUD_W  = 220;  // horizontal extent
    private static final int CLOUD_H  = 180;  // vertical extent

    private static final int W = LayoutConfig.VIRTUAL_WIDTH;
    private static final int H = LayoutConfig.VIRTUAL_HEIGHT;

    // ── Animation state ───────────────────────────────────────────────
    private long   birthTime      = System.currentTimeMillis();
    private float  breathePhase   = 0f;

    // Score pop — brief brightness burst when score increases
    private int    lastScore      = 0;
    private long   scorePoppedAt  = 0;
    private static final long SCORE_POP_MS = 280;

    // Shield critical pulse
    private long   shieldPulseAt  = 0;

    // Floating combo labels (kill streak popups)
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
            // fade in fast, hold, fade out
            if (t < 0.15f) return t / 0.15f;
            if (t < 0.65f) return 1f;
            return 1f - (t - 0.65f) / 0.35f;
        }
        float offsetY() {
            float t = (System.currentTimeMillis() - bornAt) / (float) LIFE;
            return -t * 28f;  // floats upward
        }
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Spawn a floating kill-streak label (called by ScoreManager hook).
     * Shows "+x1200" style labels that drift upward and fade.
     */
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

        // Animate breathe — slow sine, period ~4s
        breathePhase = ((now - birthTime) % 4000) / 4000f;
        float breathe = 0.85f + 0.15f * (float) Math.sin(breathePhase * Math.PI * 2);

        // Detect score pop
        if (d.score != lastScore) {
            scorePoppedAt = now;
            lastScore     = d.score;
        }
        float scorePop = 0f;
        if (now - scorePoppedAt < SCORE_POP_MS) {
            float t = (now - scorePoppedAt) / (float) SCORE_POP_MS;
            scorePop = (float) Math.sin(t * Math.PI);  // 0→1→0
        }

        // Detect shield critical
        if (d.shieldCrit) {
            shieldPulseAt = now;
        }

        Color rc = d.regionColor();

        // Save composite
        Composite orig = g.getComposite();

        // ── Draw the four clouds ──────────────────────────────────────
        drawCloudTL(g, d, rc, breathe, scorePop, orig);
        drawCloudTR(g, d, rc, breathe, orig);
        drawCloudBL(g, d, rc, breathe, orig);
        drawCloudBR(g, d, rc, breathe, orig);

        // ── Boss bar (center top) ─────────────────────────────────────
        if (d.bossActive) drawBossBar(g, d, rc, orig);

        // ── Floating labels ───────────────────────────────────────────
        drawFloaters(g, orig);

        // ── FPS (tiny, bottom center, always visible) ─────────────────
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setFont(new Font("Courier New", Font.PLAIN, 9));
        g.setColor(Color.WHITE);
        String fpsStr = d.fps + " fps";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(fpsStr, W / 2 - fm.stringWidth(fpsStr) / 2, H - 4);
        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // CLOUD — TOP LEFT — Tactical
    // Score (large amber), wave number (small cyan), formation code,
    // combo multiplier when active.
    // ══════════════════════════════════════════════════════════════════
    private void drawCloudTL(Graphics2D g, HUDData d, Color rc,
                             float breathe, float scorePop, Composite orig) {

        // Aura pool
        drawAura(g, -CLOUD_W / 2, -CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2,
                rc, breathe * 0.82f, orig);

        // Score — large, amber, pops brighter on change
        float scoreAlpha = 0.90f + scorePop * 0.10f;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreAlpha));

        float scoreSzBase = 22f;
        float scoreSz     = scoreSzBase + scorePop * 3f;
        g.setFont(monoFont((int) scoreSz));
        g.setColor(scorePop > 0.1f
                ? new Color(255, 220, 80)   // bright pop
                : new Color(255, 170, 0));  // normal amber
        String scoreStr = String.format("%08d", d.score);
        g.drawString(scoreStr, 10, 30);

        // Wave number — small, cyan, below score
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.75f));
        g.setFont(monoFont(10));
        g.setColor(new Color(0, 200, 220));
        g.drawString(String.format("W%02d", d.wave), 10, 46);

        // Formation code — next to wave, dim
        g.setColor(new Color(0, 160, 160, 180));
        g.drawString(HUDData.formationCode(d.formation), 48, 46);

        // Combo multiplier — only when > 1, orange pulse
        if (d.comboMult > 1) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() * 0.006f)));
            g.setFont(monoFont(11));
            g.setColor(new Color(255, 120, 0));
            g.drawString("x" + d.comboMult, 10, 62);
        }

        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // CLOUD — TOP RIGHT — Navigation
    // Sector name (large, region-colored), radial level progress arc.
    // No text label on the arc — player learns to read it.
    // ══════════════════════════════════════════════════════════════════
    private void drawCloudTR(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {

        drawAura(g, W - CLOUD_W - CLOUD_W / 2, -CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2,
                rc, breathe * 0.78f, orig);

        // Sector name — large, right-aligned, region color
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.92f));
        g.setFont(monoFont(20));
        g.setColor(rc);
        FontMetrics fm = g.getFontMetrics();
        String sectorStr = d.region;
        g.drawString(sectorStr, W - fm.stringWidth(sectorStr) - 10, 28);

        // Tiny mode tag below sector
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.55f));
        g.setFont(monoFont(8));
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 160));
        String modeStr = d.gameMode;
        fm = g.getFontMetrics();
        g.drawString(modeStr, W - fm.stringWidth(modeStr) - 10, 42);

        // Radial level progress arc — top-right corner
        // Arc drawn as a partial circle. No label.
        // Full circle = level 25. Current fill = level / 25.
        int    arcCX   = W - 18;
        int    arcCY   = 18;
        int    arcR    = 28;
        float  arcFrac = (float) d.level / (float) d.totalLevels;
        float  arcDeg  = arcFrac * 270f;  // 270 degree sweep (3/4 circle)

        // Track (dim background arc)
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue()));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                arcR * 2, arcR * 2,
                135f, 270f, Arc2D.OPEN));

        // Fill arc — region color, bright
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.85f));
        g.setColor(rc);
        if (arcDeg > 0) {
            g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                    arcR * 2, arcR * 2,
                    135f, arcDeg, Arc2D.OPEN));
        }

        // Level number inside arc — tiny
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g.setFont(monoFont(8));
        fm = g.getFontMetrics();
        String lvlStr = String.format("%02d", d.level);
        g.setColor(rc);
        g.drawString(lvlStr,
                arcCX - fm.stringWidth(lvlStr) / 2,
                arcCY + fm.getAscent() / 2);

        g.setStroke(new BasicStroke(1f));
        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // CLOUD — BOTTOM LEFT — Survival
    // Ship-silhouette lives (no label), shield arc (no label),
    // armor arc below it (no label). Cloud pulses red when critical.
    // ══════════════════════════════════════════════════════════════════
    private void drawCloudBL(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {

        // Shield critical — pulse the aura red
        boolean crit   = d.shieldFrac < 0.20f;
        float   pulse  = crit
                ? 0.7f + 0.3f * (float) Math.sin(System.currentTimeMillis() * 0.008f)
                : breathe;
        Color   auraCol = crit ? new Color(180, 20, 20) : rc;

        drawAura(g, -CLOUD_W / 2, H - CLOUD_H - CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2,
                auraCol, pulse * 0.80f, orig);

        int baseX = 10;
        int baseY = H - 14;

        // ── Ship icons for lives ───────────────────────────────────────
        // Each icon is a small triangle pointing upward.
        // Dead slots are very dim — no other visual indicator.
        int iconW  = 12;
        int iconH  = 14;
        int iconGap = 5;

        for (int i = 0; i < d.maxLives; i++) {
            boolean alive = i < d.lives;
            float   alpha = alive ? (pulse * 0.9f) : 0.10f;

            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));

            int ix = baseX + i * (iconW + iconGap);
            int iy = baseY - 36;

            Color iconCol = alive
                    ? (crit && i == d.lives - 1
                    ? new Color(255, 80, 80)  // last life flashes red when crit
                    : new Color(0, 200, 255))
                    : new Color(40, 60, 80);

            g.setColor(iconCol);
            int[] px = { ix + iconW / 2, ix,           ix + iconW };
            int[] py = { iy,             iy + iconH,    iy + iconH };
            g.fillPolygon(px, py, 3);

            // Engine nub at base of living ships
            if (alive) {
                g.setColor(iconCol.darker());
                g.fillRect(ix + iconW / 2 - 2, iy + iconH - 3, 4, 3);
            }
        }

        // ── Shield arc ────────────────────────────────────────────────
        // Rendered as a partial arc below the ship icons.
        // Full = green. Draining = transitions toward red.
        int arcCX  = baseX + 34;
        int arcCY  = baseY - 10;
        int arcR   = 20;

        // Interpolate color: green → yellow → red as shield drains
        Color shieldCol = shieldColor(d.shieldFrac);
        float shieldDeg = d.shieldFrac * 180f;  // half-circle sweep

        // Track
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                arcR * 2, arcR * 2,
                0f, 180f, Arc2D.OPEN));

        // Fill
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                pulse * 0.90f));
        g.setColor(shieldCol);
        if (shieldDeg > 0) {
            g.draw(new Arc2D.Float(arcCX - arcR, arcCY - arcR,
                    arcR * 2, arcR * 2,
                    0f, shieldDeg, Arc2D.OPEN));
        }

        // Armor arc — concentric, smaller, purple (future stat)
        if (d.armorFrac < 1.0f || d.armorFrac > 0f) {
            int arcR2    = arcR - 6;
            float armDeg = d.armorFrac * 180f;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            g.setColor(new Color(160, 80, 255));
            g.draw(new Arc2D.Float(arcCX - arcR2, arcCY - arcR2,
                    arcR2 * 2, arcR2 * 2,
                    0f, 180f, Arc2D.OPEN));

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    breathe * 0.75f));
            g.setColor(new Color(180, 100, 255));
            if (armDeg > 0) {
                g.draw(new Arc2D.Float(arcCX - arcR2, arcCY - arcR2,
                        arcR2 * 2, arcR2 * 2,
                        0f, armDeg, Arc2D.OPEN));
            }
        }

        g.setStroke(new BasicStroke(1f));
        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // CLOUD — BOTTOM RIGHT — Threat
    // Enemy count as large number — gets brighter as count falls.
    // Formation dot-pattern below it.
    // ══════════════════════════════════════════════════════════════════
    private void drawCloudBR(Graphics2D g, HUDData d, Color rc,
                             float breathe, Composite orig) {

        drawAura(g, W - CLOUD_W - CLOUD_W / 2, H - CLOUD_H - CLOUD_H / 2,
                CLOUD_W * 2, CLOUD_H * 2,
                rc, breathe * 0.78f, orig);

        // Enemy count — inversely bright: fewer = more intense red
        int   count     = d.enemyCount;
        float intensity = count == 0 ? 0f
                : Math.max(0.55f, 1.0f - (count / 20f));
        int   red       = (int)(180 + 75 * intensity);
        Color countCol  = new Color(Math.min(red, 255), 30, 30);

        // Pulse when count is low
        float countAlpha = count <= 3 && count > 0
                ? 0.7f + 0.3f * (float) Math.abs(Math.sin(
                System.currentTimeMillis() * 0.007f))
                : breathe * 0.92f;

        g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, countAlpha));
        g.setFont(monoFont(28));
        g.setColor(countCol);
        FontMetrics fm = g.getFontMetrics();
        String countStr = String.format("%02d", count);
        int countX = W - fm.stringWidth(countStr) - 10;
        g.drawString(countStr, countX, H - 36);

        // Formation dot pattern — tiny dots arranged in formation shape
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                breathe * 0.60f));
        drawFormationDots(g, d.formation, W - 60, H - 22, rc);

        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // BOSS BAR — center top, slides down on boss entry
    // ══════════════════════════════════════════════════════════════════
    private void drawBossBar(Graphics2D g, HUDData d, Color rc, Composite orig) {
        int barW  = 400;
        int barH  = 6;
        int barX  = W / 2 - barW / 2;
        int barY  = 18;
        int nameY = barY - 4;

        // Background track
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(new Color(10, 10, 20));
        g.fillRect(barX - 2, nameY - 12, barW + 4, barH + 18);

        // Boss name — tiny monospace, centered above bar
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
        g.setFont(monoFont(8));
        g.setColor(rc);
        FontMetrics fm = g.getFontMetrics();
        String nameStr = d.bossName.toUpperCase();
        g.drawString(nameStr, W / 2 - fm.stringWidth(nameStr) / 2, nameY);

        // Phase segment dividers
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g.setColor(Color.BLACK);
        for (int p = 1; p < d.bossTotalPhases; p++) {
            int divX = barX + (int)(barW * ((float) p / d.bossTotalPhases));
            g.fillRect(divX - 1, barY, 2, barH);
        }

        // Health fill — region colored, drains left to right
        float fillW = d.bossHpFrac * barW;
        // Color shifts: full hp = region color, low hp = red
        Color fillCol = lerpColor(new Color(220, 30, 30), rc, d.bossHpFrac);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
        g.setColor(fillCol);
        if (fillW > 0) g.fillRect(barX, barY, (int) fillW, barH);

        // Track outline
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
        g.setColor(rc);
        g.drawRect(barX, barY, barW, barH);

        // Phase indicator dots below bar
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.70f));
        int dotSpacing = 10;
        int dotsStartX = W / 2 - (d.bossTotalPhases * dotSpacing) / 2;
        for (int p = 0; p < d.bossTotalPhases; p++) {
            int dotX = dotsStartX + p * dotSpacing;
            int dotY = barY + barH + 4;
            if (p < d.bossPhase - 1) {
                // completed phase — dim dot
                g.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 80));
            } else if (p == d.bossPhase - 1) {
                // current phase — bright
                g.setColor(rc);
            } else {
                // future phase — very dim
                g.setColor(new Color(40, 40, 60));
            }
            g.fillOval(dotX, dotY, 4, 4);
        }

        g.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════════
    // FLOATING KILL LABELS
    // ══════════════════════════════════════════════════════════════════
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

// ══════════════════════════════════════════════════════════════════
// FORMATION DOT PATTERN
// Tiny dots arranged in the shape of the current formation.
// Player learns to recognise the pattern.
// ══════════════════════════════════════════════════════════════════
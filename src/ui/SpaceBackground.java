package ui;

import config.LayoutConfig;
import ui.theme.UITheme;

import java.awt.*;
import java.util.Random;

/**
 * Region-aware parallax space background.
 *
 * FIXED:
 *  - renderDust() clamps all colour channels to 255.
 *  - RED region streak: streakX is now a stored field updated only
 *    once per streak interval (every ~90 frames). Previously called
 *    rand.nextInt(w) every frame, producing a different X each tick
 *    and making the streak appear to flash/flicker randomly rather
 *    than sweep across the screen smoothly.
 */
public class SpaceBackground {

    private static final int W = LayoutConfig.WINDOW_WIDTH;
    private static final int H = LayoutConfig.WINDOW_HEIGHT;

    private static final int   LAYER_COUNT     = 3;
    private static final int[] STARS_PER_LAYER = { 80, 50, 25 };
    private static final float[] LAYER_SPEED   = { 0.3f, 0.7f, 1.4f };
    private static final int[] STAR_SIZE       = { 1, 1, 2 };
    private static final int[] BASE_BRIGHT     = { 90, 140, 200 };
    private static final int   DUST_COUNT      = 6;

    private final float[][] sx, sy;
    private final int[][]   bright;
    private final float[]   dustX, dustY, dustR, dustAlpha;

    private String region      = "RED";
    private Color  nebulaTint;
    private Color  regionAccent;

    private long   frame = 0;
    private final Random rand = new Random(42);

    // ── RED streak state ──────────────────────────────────────────────
    // Stored so it doesn't change every frame.
    private int   redStreakX        = 200;
    private long  redStreakLastSwap = 0;
    private static final long RED_STREAK_INTERVAL = 1500; // ms between streaks

    private static SpaceBackground menuInstance;

    // ── Constructor ───────────────────────────────────────────────────
    public SpaceBackground() {
        sx     = new float[LAYER_COUNT][];
        sy     = new float[LAYER_COUNT][];
        bright = new int[LAYER_COUNT][];

        for (int l = 0; l < LAYER_COUNT; l++) {
            int n  = STARS_PER_LAYER[l];
            sx[l]     = new float[n];
            sy[l]     = new float[n];
            bright[l] = new int[n];
            for (int i = 0; i < n; i++) {
                sx[l][i]     = rand.nextInt(W);
                sy[l][i]     = rand.nextInt(H);
                bright[l][i] = BASE_BRIGHT[l] + rand.nextInt(55);
            }
        }

        dustX     = new float[DUST_COUNT];
        dustY     = new float[DUST_COUNT];
        dustR     = new float[DUST_COUNT];
        dustAlpha = new float[DUST_COUNT];
        for (int i = 0; i < DUST_COUNT; i++) {
            dustX[i]     = rand.nextInt(W);
            dustY[i]     = rand.nextInt(H);
            dustR[i]     = 80 + rand.nextInt(140);
            dustAlpha[i] = 0.04f + rand.nextFloat() * 0.07f;
        }

        setRegion("RED");
    }

    // ── Region ────────────────────────────────────────────────────────
    public void setRegion(String region) {
        this.region       = region;
        this.nebulaTint   = UITheme.getNebulaColor(region);
        this.regionAccent = UITheme.getRegionColor(region);
    }

    public String getRegion() { return region; }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        frame++;

        // Scroll stars
        for (int l = 0; l < LAYER_COUNT; l++) {
            int n = STARS_PER_LAYER[l];
            for (int i = 0; i < n; i++) {
                sy[l][i] += LAYER_SPEED[l];
                if (sy[l][i] > H) {
                    sy[l][i] = -2;
                    sx[l][i] = rand.nextInt(W);
                }
                // Occasional twinkle
                if (rand.nextInt(60) == 0) {
                    bright[l][i] = BASE_BRIGHT[l] + rand.nextInt(55);
                }
            }
        }

        // Update RED streak position on a timer — not every frame
        if (region.equals("RED")) {
            long now = System.currentTimeMillis();
            if (now - redStreakLastSwap > RED_STREAK_INTERVAL) {
                redStreakX        = rand.nextInt(Math.max(1, W));
                redStreakLastSwap = now;
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics2D g, int x, int y, int w, int h) {
        // Deep space base
        g.setColor(new Color(
                Math.min(nebulaTint.getRed()   + 2, 20),
                Math.min(nebulaTint.getGreen() + 2, 20),
                Math.min(nebulaTint.getBlue()  + 5, 25)
        ));
        g.fillRect(x, y, w, h);

        renderDust(g, x, y);
        renderAtmosphere(g, x, y, w, h);

        // Stars
        for (int l = 0; l < LAYER_COUNT; l++) {
            int n  = STARS_PER_LAYER[l];
            int sz = STAR_SIZE[l];
            for (int i = 0; i < n; i++) {
                int px = (int) sx[l][i];
                int py = (int) sy[l][i];
                if (px < x || px > x + w || py < y || py > y + h) continue;
                int b = bright[l][i];
                Color starColor;
                if (l == 2) {
                    starColor = blendColor(
                            new Color(b, b, b), regionAccent, 0.18f, b);
                } else {
                    starColor = new Color(b, b, b);
                }
                g.setColor(starColor);
                g.fillRect(px, py, sz, sz);
            }
        }
    }

    // ── Dust ──────────────────────────────────────────────────────────
    private void renderDust(Graphics2D g2, int ox, int oy) {
        Composite old = g2.getComposite();
        for (int i = 0; i < DUST_COUNT; i++) {
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, dustAlpha[i]));
            int r  = (int) dustR[i];
            int cx = (int) dustX[i] + ox;
            int cy = (int) dustY[i] + oy;

            g2.setColor(new Color(
                    Math.min(nebulaTint.getRed()   + 30, 255),
                    Math.min(nebulaTint.getGreen() + 20, 255),
                    Math.min(nebulaTint.getBlue()  + 40, 255)
            ));
            g2.fillOval(cx - r, cy - r / 2, r * 2, r);

            g2.setColor(new Color(
                    Math.min(nebulaTint.getRed()   + 50, 255),
                    Math.min(nebulaTint.getGreen() + 35, 255),
                    Math.min(nebulaTint.getBlue()  + 60, 255)
            ));
            g2.fillOval(cx - r / 2, cy - r / 3, r, r / 2);
        }
        g2.setComposite(old);
    }

    // ── Atmosphere ────────────────────────────────────────────────────
    private void renderAtmosphere(Graphics2D g2,
                                  int ox, int oy, int w, int h) {
        Composite old = g2.getComposite();
        long t = frame;

        switch (region) {
            case "RED" -> {
                float pulse = 0.018f
                        + 0.008f * (float) Math.sin(t * 0.02);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, pulse));
                g2.setColor(new Color(120, 10, 10));
                g2.fillRect(ox, oy + h * 2 / 3, w, h / 3);

                // Streak — fires at stored X, not random per frame
                long now = System.currentTimeMillis();
                long sinceSwap = now - redStreakLastSwap;
                if (sinceSwap < 120) {
                    // Only visible for 120ms after position update
                    float streakAlpha = 0.12f
                            * (1f - sinceSwap / 120f);
                    g2.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, streakAlpha));
                    g2.setColor(new Color(200, 40, 40));
                    g2.fillRect(redStreakX + ox, oy, 1, h);
                }
            }
            case "YELLOW" -> {
                float pulse = 0.015f
                        + 0.006f * (float) Math.sin(t * 0.015);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, pulse));
                g2.setColor(new Color(100, 70, 0));
                g2.fillRect(ox, oy, w, h / 2);

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.06f));
                g2.setColor(new Color(220, 160, 0));
                for (int i = 0; i < 8; i++) {
                    int px = ox + (int)((sx[0][i % sx[0].length]
                            + t * 0.5) % w);
                    int py = oy + (int)(sy[0][i % sy[0].length]);
                    g2.fillOval(px, py, 3, 3);
                }
            }
            case "BLUE" -> {
                float pulse = 0.02f
                        + 0.01f * (float) Math.sin(t * 0.025);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, pulse));
                g2.setColor(new Color(0, 20, 80));
                g2.fillRect(ox, oy, w, h);

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.35f));
                g2.setColor(new Color(100, 180, 255));
                for (int i = 0; i < DUST_COUNT; i++) {
                    if ((t / 15 + i) % 4 == 0) {
                        g2.fillRect(
                                (int) dustX[i] + ox,
                                (int) dustY[i] + oy,
                                2, 2);
                    }
                }
            }
            case "GREEN" -> {
                float pulse = 0.022f
                        + 0.009f * (float) Math.sin(t * 0.018);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, pulse));
                g2.setColor(new Color(5, 60, 15));
                g2.fillRect(ox, oy, w, h);

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.04f));
                g2.setColor(new Color(30, 120, 50));
                for (int i = 0; i < 4; i++) {
                    int fogY = oy
                            + (int)((t * 0.4 + i * (h / 4)) % h);
                    g2.fillRect(ox, fogY, w, 40);
                }
            }
            case "WHITE" -> {
                float pulse = 0.025f
                        + 0.012f * (float) Math.sin(t * 0.03);
                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, pulse));
                g2.setColor(new Color(50, 50, 80));
                g2.fillRect(ox, oy, w, h);

                g2.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.06f));
                g2.setColor(new Color(180, 180, 255));
                int glowR = 200
                        + (int)(30 * Math.sin(t * 0.02));
                g2.fillOval(
                        ox + w / 2 - glowR,
                        oy + h / 2 - glowR,
                        glowR * 2,
                        glowR * 2);
            }
        }
        g2.setComposite(old);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private static Color blendColor(Color base, Color accent,
                                    float ratio, int alpha) {
        int r = (int)(base.getRed()   * (1 - ratio)
                + accent.getRed()   * ratio);
        int g = (int)(base.getGreen() * (1 - ratio)
                + accent.getGreen() * ratio);
        int b = (int)(base.getBlue()  * (1 - ratio)
                + accent.getBlue()  * ratio);
        return new Color(
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255),
                Math.min(alpha, 255)
        );
    }

    public static SpaceBackground getMenuBackground() {
        if (menuInstance == null) {
            menuInstance = new SpaceBackground();
            menuInstance.setRegion("WHITE");
        }
        return menuInstance;
    }
}
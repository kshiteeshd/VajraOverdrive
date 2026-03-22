package gfx;

import entity.EntityManager;

import java.awt.*;
import java.util.Random;

/**
 * Fire-and-forget visual effects.
 *
 * ADDED:
 *  - particles()  — burst of colored dots (debris, sparks)
 *  - deathBurst() — combined explosion + particle burst
 *  - Screen shake triggered automatically on large explosions
 *
 * Effect names for spawn():
 *   "explosion_small"  — enemy death (32x32)
 *   "explosion_large"  — big explosion (48x48)
 *   "hit_flash"        — bullet impact (28x28)
 */
public class FxLayer {

    private static final FxLayer instance = new FxLayer();
    public  static FxLayer get() { return instance; }

    private EntityManager entityManager;
    private static final Random RNG = new Random();

    private FxLayer() {}

    public void init(EntityManager em) {
        this.entityManager = em;
    }

    // ── Sprite-based effects ──────────────────────────────────────────

    public void spawn(String effectName, double worldX, double worldY) {
        if (entityManager == null) return;
        int w, h;
        switch (effectName) {
            case "explosion_large" -> { w = 48; h = 48; }
            case "hit_flash"       -> { w = 28; h = 28; }
            default                -> { w = 32; h = 32; }
        }
        entityManager.add(new FxEntity(worldX, worldY, effectName, w, h));
    }

    public void explode(double ex, double ey, double ew, double eh) {
        double cx = ex + ew / 2;
        double cy = ey + eh / 2;
        spawn("explosion_small", cx, cy);
        // Subtle shake on every enemy death
        ScreenShake.small();
    }

    public void hitFlash(double ex, double ey, double ew, double eh) {
        spawn("hit_flash", ex + ew / 2, ey + eh / 2);
    }

    // ── Particle bursts ───────────────────────────────────────────────

    /**
     * Spawn a burst of colored particles at world position.
     *
     * @param cx      center X
     * @param cy      center Y
     * @param count   number of particles
     * @param color   base color (alpha ignored — controlled by particle)
     * @param speed   max initial velocity in pixels per frame
     * @param lifetime particle lifetime in ms
     */
    public void particles(double cx, double cy,
                          int count, Color color,
                          float speed, int lifetime) {
        if (entityManager == null) return;
        for (int i = 0; i < count; i++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double vel   = (0.3 + RNG.nextDouble() * 0.7) * speed;
            double vx    = Math.cos(angle) * vel;
            double vy    = Math.sin(angle) * vel;

            // Slightly vary the color brightness
            int dr = Math.min(255, color.getRed()   + RNG.nextInt(40) - 20);
            int dg = Math.min(255, color.getGreen() + RNG.nextInt(40) - 20);
            int db = Math.min(255, color.getBlue()  + RNG.nextInt(40) - 20);
            Color c = new Color(
                    Math.max(0, dr),
                    Math.max(0, dg),
                    Math.max(0, db));

            entityManager.add(new ParticleEntity(
                    cx + RNG.nextDouble() * 8 - 4,
                    cy + RNG.nextDouble() * 8 - 4,
                    vx, vy, c, lifetime,
                    RNG.nextBoolean()
            ));
        }
    }

    /**
     * Full death burst — explosion sprite + particles + shake.
     * Call this for dramatic enemy deaths (boss, tank).
     */
    public void deathBurst(double cx, double cy, Color color) {
        if (entityManager == null) return;
        spawn("explosion_large", cx, cy);
        particles(cx, cy, 18, color, 3.5f, 500);
        ScreenShake.medium();
    }

    /**
     * Small death — for basic/fast enemies.
     * Explosion sprite + small particle burst.
     */
    public void smallDeath(double cx, double cy, Color color) {
        if (entityManager == null) return;
        spawn("explosion_small", cx, cy);
        particles(cx, cy, 8, color, 2.5f, 300);
        ScreenShake.small();
    }

    /**
     * Player hit — red sparks, medium shake.
     */
    public void playerHit(double cx, double cy) {
        if (entityManager == null) return;
        spawn("hit_flash", cx, cy);
        particles(cx, cy, 12, new Color(255, 80, 80), 3.0f, 350);
        ScreenShake.medium();
    }
}
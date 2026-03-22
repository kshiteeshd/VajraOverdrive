package gfx;

import entity.Entity;

import java.awt.*;

/**
 * Single particle — spawned in bursts by FxLayer.
 * Drifts outward, fades alpha, then removes itself.
 *
 * No sprite — always drawn as a small colored square or oval.
 * Extremely lightweight. Dozens can run simultaneously.
 */
public class ParticleEntity extends Entity {

    private float   alpha;
    private float   alphaDecay;
    private Color   color;
    private boolean square;   // true = 2x2 square, false = oval

    public ParticleEntity(double x, double y,
                          double vx, double vy,
                          Color color,
                          int lifetimeMs,
                          boolean square) {
        super(x, y, 3, 3);
        this.velocityX  = vx;
        this.velocityY  = vy;
        this.color      = color;
        this.alpha      = 1.0f;
        this.alphaDecay = 1.0f / (lifetimeMs / 16.0f);  // ~60fps
        this.square     = square;
    }

    @Override
    public void update() {
        x     += velocityX;
        y     += velocityY;
        alpha -= alphaDecay;
        // Slow down over time
        velocityX *= 0.94;
        velocityY *= 0.94;
        if (alpha <= 0) {
            alpha    = 0;
            removable = true;
        }
    }

    @Override
    public void render(Graphics g) {
        if (alpha <= 0) return;
        Graphics2D g2  = (Graphics2D) g;
        Composite  old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.min(alpha, 1f)));
        g2.setColor(color);
        if (square)
            g2.fillRect((int) x, (int) y, 2, 2);
        else
            g2.fillOval((int) x, (int) y, 3, 3);
        g2.setComposite(old);
    }
}
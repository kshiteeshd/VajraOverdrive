package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * High health, slow, fires heavy single shots.
 * Rendered larger than BasicEnemy (40x40).
 * Fallback: dark red hexagon shape.
 */
public class TankEnemy extends EnemyEntity {

    public TankEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_tank");
    }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback: dark chunky hexagon
        Graphics2D g2  = (Graphics2D) g;
        int        cx  = (int) x + width  / 2;
        int        cy  = (int) y + height / 2;
        int        r   = width / 2 - 2;

        // Hex body
        int[] hx = new int[6];
        int[] hy = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            hx[i] = (int)(cx + r * Math.cos(angle));
            hy[i] = (int)(cy + r * Math.sin(angle));
        }
        g2.setColor(new Color(160, 20, 20));
        g2.fillPolygon(hx, hy, 6);

        // Inner plate
        g2.setColor(new Color(200, 50, 50));
        int r2 = r - 6;
        int[] ix = new int[6];
        int[] iy = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            ix[i] = (int)(cx + r2 * Math.cos(angle));
            iy[i] = (int)(cy + r2 * Math.sin(angle));
        }
        g2.fillPolygon(ix, iy, 6);

        // Centre dot
        g2.setColor(new Color(255, 80, 80));
        g2.fillOval(cx - 5, cy - 5, 10, 10);

        // Outline
        g2.setColor(new Color(100, 10, 10));
        g2.drawPolygon(hx, hy, 6);
    }
}
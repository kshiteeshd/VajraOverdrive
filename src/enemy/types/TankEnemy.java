package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Tier-1: dark red hexagon.
 * Tier-2: deep purple hexagon with double-ring armour plate —
 *         heavier, more menacing silhouette.
 */
public class TankEnemy extends EnemyEntity {

    public TankEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_tank");
    }

    @Override
    public EnemyClass getEnemyClass() { return EnemyClass.TANK; }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        Graphics2D g2  = (Graphics2D) g;
        int        cx  = (int) x + width  / 2;
        int        cy  = (int) y + height / 2;
        int        r   = width / 2 - 2;

        if (stats.tier == 2) {
            // ── Tier-2: purple armoured hexagon with outer spike ring
            // Outer glow
            g2.setColor(new Color(160, 40, 255, 40));
            g2.fillOval(cx - r - 6, cy - r - 6,
                    (r + 6) * 2, (r + 6) * 2);

            // Outer hex — deep purple
            int[] hx = new int[6];
            int[] hy = new int[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                hx[i] = (int)(cx + r * Math.cos(angle));
                hy[i] = (int)(cy + r * Math.sin(angle));
            }
            g2.setColor(new Color(100, 20, 180));
            g2.fillPolygon(hx, hy, 6);

            // Middle armour plate
            int r2 = r - 5;
            int[] mx = new int[6];
            int[] my = new int[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                mx[i] = (int)(cx + r2 * Math.cos(angle));
                my[i] = (int)(cy + r2 * Math.sin(angle));
            }
            g2.setColor(new Color(160, 60, 255));
            g2.fillPolygon(mx, my, 6);

            // Inner core plate
            int r3 = r - 11;
            int[] ix2 = new int[6];
            int[] iy2 = new int[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                ix2[i] = (int)(cx + r3 * Math.cos(angle));
                iy2[i] = (int)(cy + r3 * Math.sin(angle));
            }
            g2.setColor(new Color(80, 0, 140));
            g2.fillPolygon(ix2, iy2, 6);

            // Core dot — bright
            g2.setColor(new Color(220, 160, 255));
            g2.fillOval(cx - 4, cy - 4, 8, 8);

            // Outlines
            g2.setColor(new Color(200, 100, 255, 180));
            g2.drawPolygon(hx, hy, 6);
            g2.setColor(new Color(200, 100, 255, 80));
            g2.drawPolygon(mx, my, 6);

        } else {
            // ── Tier-1: original dark red hexagon
            int[] hx = new int[6];
            int[] hy = new int[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                hx[i] = (int)(cx + r * Math.cos(angle));
                hy[i] = (int)(cy + r * Math.sin(angle));
            }
            g2.setColor(new Color(160, 20, 20));
            g2.fillPolygon(hx, hy, 6);

            int r2 = r - 6;
            int[] ix = new int[6];
            int[] iy = new int[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i - 30);
                ix[i] = (int)(cx + r2 * Math.cos(angle));
                iy[i] = (int)(cy + r2 * Math.sin(angle));
            }
            g2.setColor(new Color(200, 50, 50));
            g2.fillPolygon(ix, iy, 6);

            g2.setColor(new Color(255, 80, 80));
            g2.fillOval(cx - 5, cy - 5, 10, 10);

            g2.setColor(new Color(100, 10, 10));
            g2.drawPolygon(hx, hy, 6);
        }
    }
}
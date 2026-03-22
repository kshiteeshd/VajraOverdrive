package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Tier-1: standard red diamond fallback.
 * Tier-2: brighter core, double outline ring, magenta accent dot —
 *         visually distinct even without sprites loaded.
 */
public class BasicEnemy extends EnemyEntity {

    public BasicEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_basic");
    }

    @Override
    public EnemyClass getEnemyClass() { return EnemyClass.BASIC; }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;

        if (stats.tier == 2) {
            // ── Tier-2 fallback: bright magenta-red diamond + outer ring
            // Outer glow ring
            g2.setColor(new Color(255, 60, 180, 60));
            g2.fillOval(cx - width / 2 - 3, cy - height / 2 - 3,
                    width + 6, height + 6);

            // Main diamond — hot pink-red
            g2.setColor(new Color(255, 40, 100));
            int[] dx = { cx,      (int)x,       cx,            (int)x + width };
            int[] dy = { (int)y,  cy,            (int)y+height, cy             };
            g2.fillPolygon(dx, dy, 4);

            // Inner bright core
            g2.setColor(new Color(255, 160, 200));
            int[] ix = { cx,          cx - width/4,  cx,          cx + width/4 };
            int[] iy = { (int)y + 4,  cy,            (int)y+height-4, cy       };
            g2.fillPolygon(ix, iy, 4);

            // Centre accent dot
            g2.setColor(new Color(255, 255, 200));
            g2.fillOval(cx - 3, cy - 3, 6, 6);

            // Outline
            g2.setColor(new Color(200, 20, 80));
            g2.drawPolygon(dx, dy, 4);

        } else {
            // ── Tier-1 fallback: original red diamond
            g2.setColor(new Color(220, 40, 40));
            int[] dx = { cx,      (int)x,       cx,            (int)x + width };
            int[] dy = { (int)y,  cy,            (int)y+height, cy             };
            g2.fillPolygon(dx, dy, 4);

            g2.setColor(new Color(255, 100, 100));
            g2.fillOval(cx - 4, cy - 4, 8, 8);

            g2.setColor(new Color(140, 20, 20));
            g2.drawPolygon(dx, dy, 4);
        }
    }
}
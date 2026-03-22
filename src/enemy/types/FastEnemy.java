package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Tier-1: orange arrowhead.
 * Tier-2: electric cyan arrowhead — thinner, faster-looking,
 *         with a bright streak line down the center.
 */
public class FastEnemy extends EnemyEntity {

    public FastEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_fast");
    }

    @Override
    public EnemyClass getEnemyClass() { return EnemyClass.FAST; }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        Graphics2D g2  = (Graphics2D) g;
        int cx  = (int) x + width  / 2;
        int top = (int) y;
        int bot = (int) y + height;
        int mid = (int) y + height / 2;

        if (stats.tier == 2) {
            // ── Tier-2: cyan electric dart
            // Outer glow
            g2.setColor(new Color(0, 220, 255, 45));
            g2.fillOval(cx - width / 2 - 4, top - 2, width + 8, height + 4);

            // Body — electric blue-cyan arrow
            g2.setColor(new Color(0, 200, 255));
            int[] ax = { cx,          (int)x,      (int)x + 5,
                    (int)x + 5,  cx,           (int)x + width - 5,
                    (int)x + width - 5, (int)x + width };
            int[] ay = { bot,         mid,          mid,
                    top,         top,          top,
                    mid,         mid };
            g2.fillPolygon(ax, ay, 8);

            // Center streak — bright white line
            g2.setColor(new Color(180, 255, 255));
            g2.fillRect(cx - 1, top + 2, 2, height - 4);

            // Nose highlight
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 2, bot - 5, 4, 4);

            // Outline
            g2.setColor(new Color(0, 120, 180));
            g2.drawPolygon(ax, ay, 8);

        } else {
            // ── Tier-1: original orange arrowhead
            g2.setColor(new Color(255, 140, 0));
            int[] ax = { cx,          (int)x,      (int)x + 6,
                    (int)x + 6,  cx,           (int)x + width - 6,
                    (int)x + width - 6, (int)x + width };
            int[] ay = { bot,         mid,          mid,
                    top,         top,          top,
                    mid,         mid };
            g2.fillPolygon(ax, ay, 8);

            g2.setColor(new Color(255, 210, 80));
            g2.fillOval(cx - 3, top + 4, 6, 6);

            g2.setColor(new Color(180, 80, 0));
            g2.drawPolygon(ax, ay, 8);
        }
    }
}
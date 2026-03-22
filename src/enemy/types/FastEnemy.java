package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Low health, fast, zigzag movement pattern.
 * Small hitbox (20x20). Fires rapidly.
 * Fallback: bright orange arrow shape.
 */
public class FastEnemy extends EnemyEntity {

    public FastEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_fast");
    }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback: bright orange arrowhead pointing down
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + width  / 2;
        int top = (int) y;
        int bot = (int) y + height;
        int mid = (int) y + height / 2;

        // Body arrow
        g2.setColor(new Color(255, 140, 0));
        int[] ax = { cx,          (int)x,      (int)x + 6,
                (int)x + 6,  cx,           (int)x + width - 6,
                (int)x + width - 6, (int)x + width };
        int[] ay = { bot,         mid,          mid,
                top,          top,          top,
                mid,          mid };
        g2.fillPolygon(ax, ay, 8);

        // Highlight
        g2.setColor(new Color(255, 210, 80));
        g2.fillOval(cx - 3, top + 4, 6, 6);

        // Outline
        g2.setColor(new Color(180, 80, 0));
        g2.drawPolygon(ax, ay, 8);
    }
}
package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;
import entity.ProjectileEntity;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Fires a single high-damage slow bullet aimed directly at the
 * player's last known position. Stays static in formation.
 * Fallback: purple crosshair shape.
 */
public class SniperEnemy extends EnemyEntity {

    // Last known player X — updated by FleetController
    private double targetX = 400;

    public SniperEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_sniper");
    }

    public void setTargetX(double tx) { this.targetX = tx; }

    // Replace fire() in SniperEnemy with this:

    @Override
    public void fire() {
        if (entityManager == null) return;

        double dx  = targetX - (x + width / 2.0);
        double dy  = 300.0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) len = 1;
        double vx  = (dx / len) * 5.5;
        double vy  = (dy / len) * 5.5;

        entityManager.add(new entity.ProjectileEntity(
                x + width / 2.0 - 2,
                y + height,
                vx, vy,
                4, 18,
                stats.bulletDamage,
                false,
                entity.ProjectileEntity.BulletType.SNIPER
        ));
    }


    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback: purple crosshair
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + width  / 2;
        int cy = (int) y + height / 2;
        int r  = width / 2 - 2;

        // Outer ring
        g2.setColor(new Color(160, 60, 220));
        g2.drawOval((int)x + 2, (int)y + 2, width - 4, height - 4);
        g2.drawOval((int)x + 4, (int)y + 4, width - 8, height - 8);

        // Cross lines
        g2.setColor(new Color(200, 100, 255));
        g2.fillRect(cx - 1, (int)y + 2, 2, height - 4);   // vertical
        g2.fillRect((int)x + 2, cy - 1, width - 4, 2);    // horizontal

        // Centre dot
        g2.setColor(new Color(240, 160, 255));
        g2.fillOval(cx - 3, cy - 3, 6, 6);

        // Body fill (faint)
        g2.setColor(new Color(80, 20, 120, 120));
        g2.fillOval((int)x + 2, (int)y + 2, width - 4, height - 4);
    }
}
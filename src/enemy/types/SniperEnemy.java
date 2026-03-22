package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;
import entity.ProjectileEntity;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Tier-1: purple crosshair — single aimed shot.
 * Tier-2: gold crosshair with outer triangle indicators —
 *         fires a 3-shot aimed burst spread.
 */
public class SniperEnemy extends EnemyEntity {

    private double targetX = 400;

    public SniperEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_sniper");
    }

    @Override
    public EnemyClass getEnemyClass() { return EnemyClass.SNIPER; }

    public void setTargetX(double tx) { this.targetX = tx; }

    @Override
    public void fire() {
        if (entityManager == null) return;

        double bx = x + width / 2.0 - 2;
        double by = y + height;

        if (stats.tier == 2) {
            // ── Tier-2: 3-shot burst spread aimed at player
            // Centre shot aimed directly, two flanking shots ±10 degrees
            double[] spreadAngles = { -10.0, 0.0, 10.0 };
            for (double spreadDeg : spreadAngles) {
                double dx  = targetX - (x + width / 2.0);
                double dy  = 300.0;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len == 0) len = 1;

                // Rotate the aim vector by spreadDeg
                double rad = Math.toRadians(spreadDeg);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);
                double ndx = dx * cos - dy * sin;
                double ndy = dx * sin + dy * cos;
                double nlen = Math.sqrt(ndx * ndx + ndy * ndy);
                if (nlen == 0) nlen = 1;

                double vx = (ndx / nlen) * 5.5;
                double vy = (ndy / nlen) * 5.5;

                entityManager.add(new ProjectileEntity(
                        bx, by,
                        vx, vy,
                        4, 18,
                        stats.bulletDamage,
                        false,
                        ProjectileEntity.BulletType.SNIPER
                ));
            }
        } else {
            // ── Tier-1: single aimed shot
            double dx  = targetX - (x + width / 2.0);
            double dy  = 300.0;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len == 0) len = 1;
            double vx  = (dx / len) * 5.5;
            double vy  = (dy / len) * 5.5;

            entityManager.add(new ProjectileEntity(
                    bx, by,
                    vx, vy,
                    4, 18,
                    stats.bulletDamage,
                    false,
                    ProjectileEntity.BulletType.SNIPER
            ));
        }
    }

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
            // ── Tier-2: gold targeting crosshair with outer brackets
            // Outer glow
            g2.setColor(new Color(255, 200, 0, 40));
            g2.fillOval((int)x - 3, (int)y - 3, width + 6, height + 6);

            // Outer ring — gold
            g2.setColor(new Color(255, 180, 0));
            g2.drawOval((int)x + 1, (int)y + 1, width - 2, height - 2);

            // Inner ring
            g2.setColor(new Color(255, 220, 60));
            g2.drawOval((int)x + 4, (int)y + 4, width - 8, height - 8);

            // Cross lines — gold
            g2.setColor(new Color(255, 200, 40));
            g2.fillRect(cx - 1, (int)y + 2, 2, height - 4);
            g2.fillRect((int)x + 2, cy - 1, width - 4, 2);

            // Corner bracket marks (targeting indicator)
            int bSize = 4;
            g2.setColor(new Color(255, 240, 100));
            // top-left
            g2.fillRect((int)x + 2, (int)y + 2, bSize, 1);
            g2.fillRect((int)x + 2, (int)y + 2, 1, bSize);
            // top-right
            g2.fillRect((int)x + width - 2 - bSize, (int)y + 2, bSize, 1);
            g2.fillRect((int)x + width - 3, (int)y + 2, 1, bSize);
            // bottom-left
            g2.fillRect((int)x + 2, (int)y + height - 3, bSize, 1);
            g2.fillRect((int)x + 2, (int)y + height - 2 - bSize, 1, bSize);
            // bottom-right
            g2.fillRect((int)x + width - 2 - bSize, (int)y + height - 3, bSize, 1);
            g2.fillRect((int)x + width - 3, (int)y + height - 2 - bSize, 1, bSize);

            // Centre dot — bright gold
            g2.setColor(new Color(255, 255, 150));
            g2.fillOval(cx - 3, cy - 3, 6, 6);

            // Body fill — dark gold tint
            g2.setColor(new Color(60, 40, 0, 100));
            g2.fillOval((int)x + 4, (int)y + 4, width - 8, height - 8);

        } else {
            // ── Tier-1: original purple crosshair
            g2.setColor(new Color(160, 60, 220));
            g2.drawOval((int)x + 2, (int)y + 2, width - 4, height - 4);
            g2.drawOval((int)x + 4, (int)y + 4, width - 8, height - 8);

            g2.setColor(new Color(200, 100, 255));
            g2.fillRect(cx - 1, (int)y + 2, 2, height - 4);
            g2.fillRect((int)x + 2, cy - 1, width - 4, 2);

            g2.setColor(new Color(240, 160, 255));
            g2.fillOval(cx - 3, cy - 3, 6, 6);

            g2.setColor(new Color(80, 20, 120, 120));
            g2.fillOval((int)x + 2, (int)y + 2, width - 4, height - 4);
        }
    }
}
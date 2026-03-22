package enemy;

import enemy.data.EnemyStats;
import entity.Entity;
import entity.EntityManager;
import entity.ProjectileEntity;
import gfx.FxLayer;
import gfx.ImageSequenceSet;
import gfx.ShipRegistry;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EnemyEntity extends Entity {

    protected EnemyStats    stats;
    protected EntityManager entityManager;
    protected ImageSequenceSet anim;

    // ── Movement state ────────────────────────────────────────────────
    // Formation position — set by FleetController after entering
    protected double formationX;
    protected double formationY;

    // Strafe / zigzag
    private double  moveOffsetX  = 0;
    private double  moveOffsetY  = 0;
    private double  moveVelX     = 0.6;
    private double  moveVelY     = 0.4;
    private boolean moveDirFlipX = false;
    private long    diveStart    = 0;
    private boolean diving       = false;
    private static final long DIVE_INTERVAL = 4000;  // ms between dives
    private long lastDiveTime = 0;

    // ── Constructor ───────────────────────────────────────────────────
    public EnemyEntity(double x, double y, EnemyStats stats,
                       EntityManager em, String animKey) {
        super(x, y, stats.width, stats.height);
        this.stats         = stats;
        this.entityManager = em;
        this.health        = stats.health;
        this.velocityY     = stats.speed;
        this.anim          = ShipRegistry.buildEnemySet(animKey);
    }

    // Backward-compat — uses "enemy_basic"
    public EnemyEntity(double x, double y, EnemyStats stats, EntityManager em) {
        this(x, y, stats, em, "enemy_basic");
    }

    // ── Formation position (set by FleetController) ───────────────────
    public void setFormationPos(double fx, double fy) {
        this.formationX = fx;
        this.formationY = fy;
    }

    // ── Damage ────────────────────────────────────────────────────────
    public void takeDamage(int dmg) {
        health -= dmg;
        if (health <= 0) {
            // Pick death effect based on enemy type
            double cx = x + width  / 2.0;
            double cy = y + height / 2.0;

            String cls = getClass().getSimpleName();
            switch (cls) {
                case "TankEnemy" ->
                        FxLayer.get().deathBurst(cx, cy, new Color(200, 40, 40));
                case "SniperEnemy" ->
                        FxLayer.get().deathBurst(cx, cy, new Color(160, 60, 220));
                case "FastEnemy" ->
                        FxLayer.get().smallDeath(cx, cy, new Color(255, 160, 20));
                default ->
                        FxLayer.get().smallDeath(cx, cy, new Color(220, 80, 80));
            }

            removable = true;
        } else {
            FxLayer.get().hitFlash(x, y, width, height);
            if (anim.has("hit")) anim.play("hit", "idle");
        }
    }

    public int getScoreValue() { return stats.scoreValue; }

    // ── Update ────────────────────────────────────────────────────────
    @Override
    public void update() {
        anim.update();
        applyMovePattern();
    }

    /**
     * Applies movement offset on top of formation position.
     * FleetController sets x/y to formation position each frame
     * during ENTERING. Once FORMED, this method drives sub-movement.
     */
    protected void applyMovePattern() {
        long now = System.currentTimeMillis();

        switch (stats.movePattern) {

            case STATIC -> { /* no movement — position held by FleetController */ }

            case STRAFE -> {
                moveOffsetX += moveVelX;
                if (Math.abs(moveOffsetX) > 28) moveVelX = -moveVelX;
                x = formationX + moveOffsetX;
            }

            case ZIGZAG -> {
                moveOffsetX += moveVelX;
                moveOffsetY += moveVelY;
                if (Math.abs(moveOffsetX) > 36) moveVelX = -moveVelX;
                if (Math.abs(moveOffsetY) > 18) moveVelY = -moveVelY;
                x = formationX + moveOffsetX;
                y = formationY + moveOffsetY;
            }

            case DIVE -> {
                if (!diving && now - lastDiveTime > DIVE_INTERVAL) {
                    diving     = true;
                    diveStart  = now;
                    lastDiveTime = now;
                }
                if (diving) {
                    long elapsed = now - diveStart;
                    if (elapsed < 600) {
                        // Dive down
                        y = formationY + (elapsed / 600.0) * 120;
                    } else if (elapsed < 1200) {
                        // Return up
                        y = formationY + ((1200 - elapsed) / 600.0) * 120;
                    } else {
                        // Reset
                        y      = formationY;
                        diving = false;
                    }
                } else {
                    y = formationY;
                }
            }
        }
    }

    // ── Fire ──────────────────────────────────────────────────────────
    // Replace the existing fire() method in EnemyEntity with this:

    public void fire() {
        if (entityManager == null) return;

        // Determine bullet type from this enemy's class name
        entity.ProjectileEntity.BulletType bt = resolveBulletType();

        // Bullet size per type
        int bw, bh;
        double bvy;
        switch (bt) {
            case TANK   -> { bw = 10; bh = 10; bvy = 3.0; }
            case SNIPER -> { bw = 4;  bh = 16; bvy = 6.0; }
            case FAST   -> { bw = 5;  bh = 10; bvy = 5.5; }
            default     -> { bw = 6;  bh = 12; bvy = 4.0; }
        }

        if (stats.scatterShot && stats.bulletCount > 1) {
            double totalSpread = stats.bulletSpread * (stats.bulletCount - 1);
            double startAngle  = -totalSpread / 2.0;
            for (int i = 0; i < stats.bulletCount; i++) {
                double angle = Math.toRadians(startAngle + i * stats.bulletSpread);
                double vx    = Math.sin(angle) * 4.0;
                double vy    = Math.cos(angle) * 4.0;
                entityManager.add(new entity.ProjectileEntity(
                        x + width / 2.0 - bw / 2.0,
                        y + height,
                        vx, vy, bw, bh,
                        stats.bulletDamage, false, bt
                ));
            }
        } else {
            entityManager.add(new entity.ProjectileEntity(
                    x + width / 2.0 - bw / 2.0,
                    y + height,
                    0, bvy, bw, bh,
                    stats.bulletDamage, false, bt
            ));
        }
    }

    private entity.ProjectileEntity.BulletType resolveBulletType() {
        String cls = getClass().getSimpleName();
        return switch (cls) {
            case "TankEnemy"   -> entity.ProjectileEntity.BulletType.TANK;
            case "FastEnemy"   -> entity.ProjectileEntity.BulletType.FAST;
            case "SniperEnemy" -> entity.ProjectileEntity.BulletType.SNIPER;
            default            -> entity.ProjectileEntity.BulletType.BASIC;
        };
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback: red diamond
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + width / 2;
        int cy = (int) y + height / 2;

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
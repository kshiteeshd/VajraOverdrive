package enemy;

import enemy.data.EnemyStats;
import entity.Entity;
import entity.EntityManager;
import gfx.FxLayer;
import gfx.ImageSequenceSet;
import gfx.ShipRegistry;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public abstract class EnemyEntity extends Entity {

    /**
     * FIX: replaces getClass().getSimpleName() usage everywhere.
     * Each subclass returns its canonical class identity via this enum.
     * Safe under obfuscation, safe under subclassing, O(1) switch.
     */
    public enum EnemyClass { BASIC, FAST, TANK, SNIPER }

    /** Subclasses must declare their type. */
    public abstract EnemyClass getEnemyClass();

    protected EnemyStats    stats;
    protected EntityManager entityManager;
    protected ImageSequenceSet anim;

    // Formation position — set by FleetController after entering
    protected double formationX;
    protected double formationY;

    // Strafe / zigzag
    private double  moveOffsetX;
    private double  moveOffsetY;
    private double  moveVelX = 0.6;
    private double  moveVelY = 0.4;
    private long    diveStart    = 0;
    private boolean diving       = false;
    private static final long DIVE_INTERVAL = 4000;
    private long lastDiveTime;

    // FIX: shared RNG for phase offsets
    private static final Random RNG = new Random();

    // ── Constructor ───────────────────────────────────────────────────
    public EnemyEntity(double x, double y, EnemyStats stats,
                       EntityManager em, String animKey) {
        super(x, y, stats.width, stats.height);
        this.stats         = stats;
        this.entityManager = em;
        this.health        = stats.health;
        this.velocityY     = stats.speed;
        this.anim          = ShipRegistry.buildEnemySet(animKey);

        // FIX: randomize initial phase so enemies in a fleet
        // don't all zigzag/dive in lockstep
        moveOffsetX  = (RNG.nextDouble() * 2 - 1) * 28.0;
        moveOffsetY  = (RNG.nextDouble() * 2 - 1) * 18.0;
        // FIX: stagger dive timers so fleet doesn't all dive simultaneously
        lastDiveTime = System.currentTimeMillis()
                - (long)(RNG.nextDouble() * DIVE_INTERVAL);
    }

    public EnemyEntity(double x, double y, EnemyStats stats, EntityManager em) {
        this(x, y, stats, em, "enemy_basic");
    }

    // ── Formation position ────────────────────────────────────────────
    public void setFormationPos(double fx, double fy) {
        this.formationX = fx;
        this.formationY = fy;
    }

    /**
     * Scale this enemy's movement velocities by a difficulty multiplier.
     * Called by FleetController after construction so DifficultyController
     * speed values actually reach individual enemies.
     */
    public void applyDifficultyScale(double speedMult) {
        moveVelX  *= speedMult;
        moveVelY  *= speedMult;
        velocityY *= speedMult;
    }

    // ── Damage ────────────────────────────────────────────────────────
    public void takeDamage(int dmg) {
        health -= dmg;
        if (health <= 0) {
            double cx = x + width  / 2.0;
            double cy = y + height / 2.0;

            // FIX: use getEnemyClass() instead of getSimpleName()
            switch (getEnemyClass()) {
                case TANK   -> FxLayer.get().deathBurst(cx, cy, new Color(200, 40, 40));
                case SNIPER -> FxLayer.get().deathBurst(cx, cy, new Color(160, 60, 220));
                case FAST   -> FxLayer.get().smallDeath(cx, cy, new Color(255, 160, 20));
                default     -> FxLayer.get().smallDeath(cx, cy, new Color(220, 80, 80));
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

    protected void applyMovePattern() {
        long now = System.currentTimeMillis();

        switch (stats.movePattern) {

            case STATIC -> {
                // position held by FleetController
            }

            case STRAFE -> {
                moveOffsetX += moveVelX;
                if (Math.abs(moveOffsetX) > 28) moveVelX = -moveVelX;
                x = formationX + moveOffsetX;
                // FIX: strafe never reset Y — tank drifted off row
                y = formationY;
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
                    diving       = true;
                    diveStart    = now;
                    lastDiveTime = now;
                }
                if (diving) {
                    long elapsed = now - diveStart;
                    // FIX: clamp elapsed to prevent lag-spike teleport
                    elapsed = Math.min(elapsed, 1200);
                    if (elapsed < 600) {
                        y = formationY + (elapsed / 600.0) * 120;
                    } else if (elapsed < 1200) {
                        y = formationY + ((1200 - elapsed) / 600.0) * 120;
                    } else {
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
    public void fire() {
        if (entityManager == null) return;

        // FIX: use getEnemyClass() instead of getSimpleName()
        entity.ProjectileEntity.BulletType bt = resolveBulletType();

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
        // FIX: enum switch, not string comparison
        return switch (getEnemyClass()) {
            case TANK   -> entity.ProjectileEntity.BulletType.TANK;
            case FAST   -> entity.ProjectileEntity.BulletType.FAST;
            case SNIPER -> entity.ProjectileEntity.BulletType.SNIPER;
            default     -> entity.ProjectileEntity.BulletType.BASIC;
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

        // Fallback shape — red diamond
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
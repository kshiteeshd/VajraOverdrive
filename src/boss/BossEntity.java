package boss;

import config.CombatArea;
import entity.Entity;
import entity.EntityManager;
import entity.ProjectileEntity;
import gfx.FxLayer;
import gfx.ScreenShake;
import player.PlayerShip;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Base class for all bosses.
 *
 * Bosses are NOT EnemyEntity subclasses — they have phase mechanics,
 * a health bar, minion spawning hooks, and complex movement patterns
 * that don't fit the fleet/formation system.
 *
 * Each region boss extends this and overrides:
 *   renderBody()    — the actual visual for this boss
 *   onPhaseChange() — triggered when a threshold is crossed
 *   firePattern()   — per-phase attack logic
 */
public abstract class BossEntity extends Entity {

    protected final BossStats     stats;
    protected final EntityManager entityManager;
    protected       PlayerShip    player;

    // ── Phase state ───────────────────────────────────────────────────
    protected int  phase        = 0;
    protected int  maxPhases;
    private   boolean phaseJustChanged = false;

    // ── Health ────────────────────────────────────────────────────────
    protected int  currentHealth;
    private   int  hitFlashFrames = 0;  // frames remaining for hit flash

    // ── Movement ──────────────────────────────────────────────────────
    // Bosses sweep horizontally across the top of the play field.
    // sweepDir: +1 = moving right, -1 = moving left
    private double sweepDir      = 1.0;
    private double currentSpeed;
    protected double centerX;           // horizontal center anchor
    protected double formationY;        // target Y to hold after entering

    // Entry animation
    private boolean entering     = true;
    private static final double ENTRY_SPEED = 2.0;

    // ── Fire timing ───────────────────────────────────────────────────
    private long lastFireTime    = 0;
    protected long fireRateMs;

    // ── Constructor ───────────────────────────────────────────────────
    public BossEntity(double spawnX, double spawnY,
                      BossStats stats,
                      EntityManager em,
                      PlayerShip player) {
        super(spawnX, spawnY, stats.width, stats.height);
        this.stats         = stats;
        this.entityManager = em;
        this.player        = player;
        this.currentHealth = stats.maxHealth;
        this.maxPhases     = stats.phaseThresholds.length + 1;
        this.fireRateMs    = stats.fireRateMs;
        this.currentSpeed  = stats.moveSpeed;
        this.centerX       = CombatArea.LEFT_BOUND
                + CombatArea.WIDTH / 2.0
                - stats.width / 2.0;
        this.formationY    = spawnY;  // set by WaveManager before first update
    }

    // ── Public API ────────────────────────────────────────────────────
    public int   getCurrentHealth() { return currentHealth; }
    public int   getMaxHealth()     { return stats.maxHealth; }
    public int   getPhase()         { return phase; }
    public int   getMaxPhases()     { return maxPhases; }
    public String getBossName()     { return stats.name; }
    public Color  getBossColor()    { return stats.color; }
    public int   getScoreValue()    { return stats.scoreValue; }
    public boolean isEntering()     { return entering; }

    public void setFormationY(double y) { this.formationY = y; }

    public void takeDamage(int dmg) {
        if (entering) return;   // invulnerable during entry
        currentHealth -= dmg;
        hitFlashFrames = 4;

        // Check phase transitions
        for (int i = 0; i < stats.phaseThresholds.length; i++) {
            // Math.round prevents the int-cast truncation that could skip a
            // threshold when maxHealth * fraction is e.g. 299.99 → 299 not 300
            int threshold = Math.round(stats.maxHealth * stats.phaseThresholds[i]);
            // transition to phase i+1 when health drops below threshold
            if (currentHealth <= threshold && phase == i) {
                phase++;
                phaseJustChanged = true;
                onPhaseChange(phase);
                ScreenShake.large();
                // Speed up movement each phase — capped so boss can't overshoot
                currentSpeed = Math.min(
                        stats.moveSpeed * (1.0 + phase * 0.3), 8.0);
                // Fire rate increases each phase
                fireRateMs = (long)(stats.fireRateMs / (1.0 + phase * 0.4));
                break;
            }
        }

        if (currentHealth <= 0) {
            currentHealth = 0;
            onDeath();
            removable = true;
        }
    }

    // ── Update ────────────────────────────────────────────────────────
    @Override
    public void update() {
        // Entry animation — move down to formationY
        if (entering) {
            y += ENTRY_SPEED;
            if (y >= formationY) {
                y       = formationY;
                entering = false;
                lastFireTime = System.currentTimeMillis();
            }
            return;
        }

        // Horizontal sweep
        x += currentSpeed * sweepDir;

        double leftBound  = CombatArea.LEFT_BOUND + 20;
        double rightBound = CombatArea.RIGHT_BOUND - width - 20;

        if (x <= leftBound) {
            x       = leftBound;
            sweepDir = 1.0;
            onEdgeReached();
        } else if (x >= rightBound) {
            x       = rightBound;
            sweepDir = -1.0;
            onEdgeReached();
        }

        // Hit flash decay
        if (hitFlashFrames > 0) hitFlashFrames--;

        // Fire
        long now = System.currentTimeMillis();
        if (now - lastFireTime >= fireRateMs) {
            firePattern(phase);
            lastFireTime = now;
        }

        updatePhase(phase);
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        // Hit flash — white overlay on damage
        if (hitFlashFrames > 0) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    hitFlashFrames * 0.18f));
            g2.setColor(Color.WHITE);
            g2.fillRect((int)x, (int)y, width, height);
            g2.setComposite(old);
        }

        renderBody(g2);
    }

    // ── Hooks for subclasses ──────────────────────────────────────────
    /** Draw the boss visual. Called every frame after hit-flash. */
    protected abstract void renderBody(Graphics2D g);

    /** Called when health crosses a phase threshold. Override for phase FX. */
    protected void onPhaseChange(int newPhase) {}

    /** Called once per update tick for subclass per-phase logic. */
    protected void updatePhase(int currentPhase) {}

    /** Called when the boss reaches a horizontal edge. Override for dive etc. */
    protected void onEdgeReached() {}

    /** Called when health hits zero. Triggers explosion FX by default. */
    protected void onDeath() {
        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().deathBurst(cx, cy, stats.color);
        // Extra explosions for a boss-scale death
        FxLayer.get().deathBurst(cx - 30, cy + 10, stats.color);
        FxLayer.get().deathBurst(cx + 30, cy - 10, stats.color);
        FxLayer.get().deathBurst(cx, cy + 20, Color.WHITE);
        ScreenShake.large();
    }

    // ── Fire helpers for subclasses ───────────────────────────────────
    /** Fire straight down from center. */
    protected void fireStraight(int damage, int bw, int bh, double speed) {
        entityManager.add(new ProjectileEntity(
                x + width / 2.0 - bw / 2.0, y + height,
                0, speed, bw, bh,
                damage, false, ProjectileEntity.BulletType.TANK
        ));
    }

    /** Fire aimed at current player position. */
    protected void fireAimed(int damage, double speed) {
        if (player == null) return;
        double px  = player.x + player.width  / 2.0;
        double py  = player.y + player.height / 2.0;
        double dx  = px - (x + width  / 2.0);
        double dy  = py - (y + height / 2.0);
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) len = 1;
        entityManager.add(new ProjectileEntity(
                x + width / 2.0 - 3, y + height,
                (dx / len) * speed, (dy / len) * speed,
                5, 18,
                damage, false, ProjectileEntity.BulletType.SNIPER
        ));
    }

    /** Fire N bullets in a spread fan downward. */
    protected void fireSpread(int count, double spreadDeg,
                              int damage, double speed) {
        double totalSpread = spreadDeg * (count - 1);
        double startAngle  = -totalSpread / 2.0;
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(startAngle + i * spreadDeg);
            double vx    = Math.sin(angle) * speed;
            double vy    = Math.cos(angle) * speed;
            entityManager.add(new ProjectileEntity(
                    x + width / 2.0 - 4, y + height,
                    vx, vy, 8, 8,
                    damage, false, ProjectileEntity.BulletType.BASIC
            ));
        }
    }

    /** Fire abstract pattern — must be implemented by each boss. */
    protected abstract void firePattern(int currentPhase);
}
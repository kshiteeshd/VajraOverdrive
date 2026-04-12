package player;

import entity.EntityManager;
import entity.ProjectileEntity;
import entity.ProjectilePool;

import java.awt.*;

/**
 * Tempo-based weapon system for the player ship.
 *
 * ── Tempo model ──────────────────────────────────────────────────────
 * Tempo replaces the old heat/overheat model.
 *
 * Tempo (0.0 → 1.0) represents how aggressively the player is firing:
 *   - Holding fire continuously builds tempo toward 1.0.
 *   - Releasing fire decays tempo back to 0.0.
 *   - There is NO hard lockout. The weapon always fires.
 *
 * As tempo rises:
 *   - Fire rate slows   (BASE_RATE_MS → MAX_RATE_MS)
 *   - Bullet speed drops (BASE_SPEED → MIN_SPEED)
 *   - Bullet damage drops (BASE_DAMAGE → MIN_DAMAGE)
 *   - SPREAD mode: cone widens  (15° → 38°)
 *   - PIERCING mode: bullet gets longer and brighter
 *
 * The skill ceiling is rhythm. Calm, rhythmic bursts deal full damage.
 * Panic spraying still fires, just weaker and slower — not punishing,
 * but not optimal either. Players learn this naturally.
 *
 * ── Weapon modes ─────────────────────────────────────────────────────
 *   SINGLE   — one bolt, center, straight up. Default.
 *   SPREAD   — three bolts in a cone. Cone widens with tempo.
 *   PIERCING — one long bolt that passes through enemies.
 *              CollisionSystem must check getModifier() == PIERCING
 *              and skip marking that bullet removable on first hit.
 *
 * Modes are set via setMode() — wired to the upgrade card system.
 *
 * ── Upgrade hooks ────────────────────────────────────────────────────
 *   upgradeFireRate()   — reduces BASE_RATE_MS by RATE_UPGRADE_STEP
 *   upgradeDamage()     — increases BASE_DAMAGE by DAMAGE_UPGRADE_STEP
 *   upgradeTempoDecay() — increases TEMPO_DECAY (cools down faster)
 *
 * ── External API (unchanged from old WeaponSystem) ───────────────────
 *   canFire(long now)
 *   fire(double shipX, double shipY, int shipWidth, EntityManager em)
 *   update(boolean fireHeld)
 *   renderOverlay(Graphics2D g, double shipX, double shipY, int w, int h)
 *   getTempo()       — replaces getHeat()
 *   isHot()          — replaces isOverheated(); true when tempo > 0.75
 *
 * Note: getHeat() and isOverheated() are kept as aliases so any existing
 * HUD wiring that references them still compiles without change.
 */
public class WeaponSystem {

    // ── Weapon modes ──────────────────────────────────────────────────
    public enum WeaponMode { SINGLE, SPREAD, PIERCING }

    // ── Tuning — base values (before upgrades) ────────────────────────
    private static final long   BASE_RATE_MS        = 130;   // ms between shots at calm
    private static final long   MAX_RATE_MS         = 280;   // ms between shots at max tempo
    private static final int    BASE_DAMAGE         = 8;
    private static final int    MIN_DAMAGE          = 3;     // at max tempo
    private static final double BASE_SPEED          = 13.0;
    private static final double MIN_SPEED           = 8.5;

    private static final float  TEMPO_BUILD         = 0.013f; // per frame while fire held (~60fps)
    private static final float  TEMPO_DECAY         = 0.032f; // per frame while fire released

    // Spread mode angle range
    private static final float  SPREAD_ANGLE_MIN    = 15f;   // degrees at tempo=0
    private static final float  SPREAD_ANGLE_MAX    = 38f;   // degrees at tempo=1

    // Upgrade step sizes
    private static final long   RATE_UPGRADE_STEP   = 12;    // ms reduction per upgrade
    private static final int    DAMAGE_UPGRADE_STEP = 2;     // flat damage per upgrade
    private static final float  DECAY_UPGRADE_STEP  = 0.005f;

    // ── Mutable tuning — modified by upgrades ─────────────────────────
    private long  fireRateBase  = BASE_RATE_MS;
    private int   damageBase    = BASE_DAMAGE;
    private float tempoDecay    = TEMPO_DECAY;

    // ── State ─────────────────────────────────────────────────────────
    private WeaponMode mode          = WeaponMode.SINGLE;
    private float      tempo         = 0f;
    private boolean    fireHeld      = false;
    private long       lastFireTime  = 0;

    // ── Upgrade state ─────────────────────────────────────────────────
    private int  fireRateLevel  = 0;
    private int  damageLevel    = 0;
    private int  decayLevel     = 0;

    // ── Computed this frame (for overlay rendering) ───────────────────
    private int    lastDamage  = BASE_DAMAGE;
    private double lastSpeed   = BASE_SPEED;

    // ── Mode ──────────────────────────────────────────────────────────
    public void setMode(WeaponMode m) { this.mode = m; }
    public WeaponMode getMode()       { return mode;   }

    // ── Gate — PlayerShip calls this before each fire attempt ─────────
    public boolean canFire(long now) {
        long currentRate = interpolateRate();
        return now - lastFireTime >= currentRate;
    }

    // ── Per-frame update — pass whether fire key is held ──────────────
    public void update(boolean held) {
        this.fireHeld = held;
        if (held) {
            tempo = Math.min(1f, tempo + TEMPO_BUILD);
        } else {
            tempo = Math.max(0f, tempo - tempoDecay);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────
    /** 0.0 = fully calm, 1.0 = fully spammed. */
    public float   getTempo()      { return tempo;         }
    /** True when tempo is high enough that output is noticeably degraded. */
    public boolean isHot()         { return tempo > 0.75f; }
    /** Alias for HUD compatibility — same as getTempo(). */
    public float   getHeat()       { return tempo;         }
    /** Alias for HUD compatibility — same as isHot(). */
    public boolean isOverheated()  { return isHot();       }
    public int     getDamageBase() { return damageBase;    }

    // ── Upgrade hooks (called from UpgradeCardScreen) ─────────────────
    public void upgradeFireRate() {
        fireRateLevel++;
        fireRateBase = Math.max(70, BASE_RATE_MS - (long)(fireRateLevel * RATE_UPGRADE_STEP));
    }

    public void upgradeDamage() {
        damageLevel++;
        damageBase = BASE_DAMAGE + damageLevel * DAMAGE_UPGRADE_STEP;
    }

    public void upgradeTempoDecay() {
        decayLevel++;
        tempoDecay = TEMPO_DECAY + decayLevel * DECAY_UPGRADE_STEP;
    }

    // ── Fire — spawns bullet(s) based on current mode and tempo ───────
    public void fire(double shipX, double shipY, int shipWidth, EntityManager em) {
        // Interpolate bullet properties based on tempo
        int    dmg   = interpolateDamage();
        double speed = interpolateSpeed();

        lastDamage = dmg;
        lastSpeed  = speed;

        double cx      = shipX + shipWidth / 2.0 - 2;
        double muzzleY = shipY - 2;

        switch (mode) {
            case SINGLE   -> fireSingle(cx, muzzleY, speed, dmg, em);
            case SPREAD   -> fireSpread(cx, muzzleY, speed, dmg, em);
            case PIERCING -> firePiercing(cx, muzzleY, speed, dmg, em);
        }

        // TODO: SoundManager.get().play("sfx_fire");

        lastFireTime = System.currentTimeMillis();
    }

    // ── Fire patterns ─────────────────────────────────────────────────

    private void fireSingle(double cx, double muzzleY,
                            double speed, int dmg, EntityManager em) {
        em.add(pool(cx, muzzleY, 0, -speed, 5, 13, dmg,
                ProjectileEntity.BulletType.PLAYER,
                ProjectileEntity.ProjectileModifier.NORMAL));
    }

    private void fireSpread(double cx, double muzzleY,
                            double speed, int dmg, EntityManager em) {
        // Cone angle widens with tempo — calm = tight, spammed = wide
        float angle = SPREAD_ANGLE_MIN + (SPREAD_ANGLE_MAX - SPREAD_ANGLE_MIN) * tempo;
        double rad  = Math.toRadians(angle);

        // Center
        em.add(pool(cx, muzzleY, 0, -speed, 5, 13, dmg,
                ProjectileEntity.BulletType.PLAYER,
                ProjectileEntity.ProjectileModifier.NORMAL));

        // Left flank
        double lx = -Math.sin(rad) * speed;
        double ly = -Math.cos(rad) * speed;
        em.add(pool(cx - 4, muzzleY, lx, ly, 5, 11,
                Math.max(1, dmg - 1),
                ProjectileEntity.BulletType.PLAYER,
                ProjectileEntity.ProjectileModifier.NORMAL));

        // Right flank
        em.add(pool(cx + 4, muzzleY, -lx, ly, 5, 11,
                Math.max(1, dmg - 1),
                ProjectileEntity.BulletType.PLAYER,
                ProjectileEntity.ProjectileModifier.NORMAL));
    }

    private void firePiercing(double cx, double muzzleY,
                              double speed, int dmg, EntityManager em) {
        // Piercing bolt: taller, full damage, passes through enemies.
        // Width and height scale slightly with calm (thinner when calm,
        // thicker/slower when hot — visual feedback for tempo state).
        int bw = 4 + (int)(tempo * 3);  // 4px calm → 7px hot
        int bh = 18 - (int)(tempo * 5); // 18px calm → 13px hot

        em.add(pool(cx, muzzleY, 0, -speed, bw, bh, dmg,
                ProjectileEntity.BulletType.PLAYER,
                ProjectileEntity.ProjectileModifier.PIERCING));
    }

    // ── Interpolation helpers ─────────────────────────────────────────

    private long interpolateRate() {
        return fireRateBase + (long)((MAX_RATE_MS - fireRateBase) * tempo);
    }

    private int interpolateDamage() {
        int minDmg = Math.max(1, MIN_DAMAGE + (damageLevel));
        return (int)(damageBase - (damageBase - minDmg) * tempo);
    }

    private double interpolateSpeed() {
        return BASE_SPEED - (BASE_SPEED - MIN_SPEED) * tempo;
    }

    // ── Pool helper — keeps fire methods readable ─────────────────────
    private ProjectileEntity pool(double x, double y,
                                  double vx, double vy,
                                  int w, int h, int dmg,
                                  ProjectileEntity.BulletType type,
                                  ProjectileEntity.ProjectileModifier mod) {
        ProjectileEntity p = ProjectilePool.get(x, y, vx, vy, w, h, dmg, true, type);
        p.setModifier(mod);
        return p;
    }

    // ── Overlay — muzzle tempo indicator ─────────────────────────────
    /**
     * Draws a small tempo ring just above the ship muzzle.
     * Invisible at zero tempo. Grows and shifts warm as tempo rises.
     * No text. No labels. Purely visual.
     *
     * At max tempo (isHot), ring pulses to signal degraded output.
     */
    public void renderOverlay(Graphics2D g, double shipX, double shipY,
                              int shipWidth, int shipHeight) {
        if (tempo < 0.06f) return;

        int cx  = (int)(shipX + shipWidth / 2.0);
        int top = (int)(shipY - 6);

        // Ring radius grows with tempo: 3px → 7px
        int r = 3 + (int)(tempo * 4f);

        // Color: cyan at calm → orange at hot → red-orange at max
        Color ringColor;
        if (tempo < 0.5f) {
            float t = tempo / 0.5f;
            ringColor = new Color(
                    (int)(0   + t * 255),
                    (int)(200 + t * (-60)),
                    (int)(255 - t * 255),
                    180
            );
        } else {
            float t = (tempo - 0.5f) / 0.5f;
            ringColor = new Color(
                    255,
                    (int)(140 - t * 100),
                    0,
                    180
            );
        }

        // Pulse ring when hot: alpha oscillates
        float alpha = 0.7f;
        if (isHot()) {
            long now = System.currentTimeMillis();
            alpha = 0.5f + 0.45f * (float) Math.abs(Math.sin(now * 0.015));
        }

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Outer ring
        g.setColor(ringColor);
        g.drawOval(cx - r, top - r, r * 2, r * 2);

        // Inner dot — shows fire mode visually
        int dotR = mode == WeaponMode.PIERCING ? 2 : 1;
        g.fillOval(cx - dotR, top - dotR, dotR * 2, dotR * 2);

        // SPREAD: draw two small wing dots flanking center
        if (mode == WeaponMode.SPREAD && tempo > 0.1f) {
            float angle = SPREAD_ANGLE_MIN + (SPREAD_ANGLE_MAX - SPREAD_ANGLE_MIN) * tempo;
            int wingX   = (int)(Math.sin(Math.toRadians(angle)) * (r + 3));
            g.fillOval(cx - wingX - 1, top - 1, 3, 3);
            g.fillOval(cx + wingX - 1, top - 1, 3, 3);
        }

        g.setComposite(old);
    }
}
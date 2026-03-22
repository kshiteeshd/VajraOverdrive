package enemy.data;

public class EnemyStats {

    public final int    health;
    public final double speed;
    public final double fireRate;

    public final int    bulletDamage;
    public final int    bulletCount;
    public final double bulletSpread;

    public final boolean laserShot;
    public final boolean scatterShot;
    public final boolean homingShot;

    // ── New fields ────────────────────────────────────────────────────
    /** Points awarded when this enemy is killed. */
    public final int scoreValue;

    /** Pixel width and height of this enemy's hitbox / sprite. */
    public final int width;
    public final int height;

    /**
     * Movement pattern applied by FleetController during FORMED state.
     *   STATIC   — stays in formation position (original behaviour)
     *   STRAFE   — slow horizontal drift left/right
     *   ZIGZAG   — fast diagonal bounce
     *   DIVE     — periodically dives toward player then returns
     */
    public enum MovePattern { STATIC, STRAFE, ZIGZAG, DIVE }
    public final MovePattern movePattern;

    // ── Full constructor ──────────────────────────────────────────────
    public EnemyStats(
            int    health,
            double speed,
            double fireRate,
            int    bulletDamage,
            int    bulletCount,
            double bulletSpread,
            boolean laserShot,
            boolean scatterShot,
            boolean homingShot,
            int    scoreValue,
            int    width,
            int    height,
            MovePattern movePattern
    ) {
        this.health       = health;
        this.speed        = speed;
        this.fireRate     = fireRate;
        this.bulletDamage = bulletDamage;
        this.bulletCount  = bulletCount;
        this.bulletSpread = bulletSpread;
        this.laserShot    = laserShot;
        this.scatterShot  = scatterShot;
        this.homingShot   = homingShot;
        this.scoreValue   = scoreValue;
        this.width        = width;
        this.height       = height;
        this.movePattern  = movePattern;
    }

    // ── Backward-compat constructor (old callers) ─────────────────────
    // Defaults: scoreValue=100, width=28, height=28, STATIC movement
    public EnemyStats(
            int health, double speed, double fireRate,
            int bulletDamage, int bulletCount, double bulletSpread,
            boolean laserShot, boolean scatterShot, boolean homingShot
    ) {
        this(health, speed, fireRate,
                bulletDamage, bulletCount, bulletSpread,
                laserShot, scatterShot, homingShot,
                100, 28, 28, MovePattern.STATIC);
    }
}
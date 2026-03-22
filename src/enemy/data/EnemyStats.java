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

    public final int scoreValue;
    public final int width;
    public final int height;

    /**
     * tier 1 = standard enemy (RED / YELLOW / BLUE regions)
     * tier 2 = elite enemy   (GREEN / WHITE regions)
     * Visual distinction and score bonus are driven by this field.
     */
    public final int tier;

    public enum MovePattern { STATIC, STRAFE, ZIGZAG, DIVE }
    public final MovePattern movePattern;

    // ── Full constructor ──────────────────────────────────────────────
    public EnemyStats(
            int     health,
            double  speed,
            double  fireRate,
            int     bulletDamage,
            int     bulletCount,
            double  bulletSpread,
            boolean laserShot,
            boolean scatterShot,
            boolean homingShot,
            int     scoreValue,
            int     width,
            int     height,
            MovePattern movePattern,
            int     tier
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
        this.tier         = tier;
    }

    // ── Tier-1 convenience constructor (omit tier — defaults to 1) ────
    public EnemyStats(
            int     health,
            double  speed,
            double  fireRate,
            int     bulletDamage,
            int     bulletCount,
            double  bulletSpread,
            boolean laserShot,
            boolean scatterShot,
            boolean homingShot,
            int     scoreValue,
            int     width,
            int     height,
            MovePattern movePattern
    ) {
        this(health, speed, fireRate,
                bulletDamage, bulletCount, bulletSpread,
                laserShot, scatterShot, homingShot,
                scoreValue, width, height,
                movePattern, 1);
    }

    // ── Legacy backward-compat constructor ────────────────────────────
    // Keeps old callers that predate scoreValue / width / height compiling.
    // Defaults: scoreValue=100, width=28, height=28, STATIC, tier=1
    public EnemyStats(
            int health, double speed, double fireRate,
            int bulletDamage, int bulletCount, double bulletSpread,
            boolean laserShot, boolean scatterShot, boolean homingShot
    ) {
        this(health, speed, fireRate,
                bulletDamage, bulletCount, bulletSpread,
                laserShot, scatterShot, homingShot,
                100, 28, 28, MovePattern.STATIC, 1);
    }
}
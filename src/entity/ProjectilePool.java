package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Object pool for ProjectileEntity instances.
 *
 * Bullets are returned here by EntityManager.update() when they become
 * removable (off-screen or after collision). ProjectilePool.get() then
 * recycles them via reset() instead of allocating new objects.
 *
 * FIX: added clear() — must be called on session reset (new game,
 *      return to menu) to prevent stale bullets from a previous run
 *      leaking into the next session. CampaignManager or GameCanvas
 *      should call ProjectilePool.clear() alongside EntityManager.clear().
 */
public class ProjectilePool {

    // Pre-allocated pool — capped so it doesn't grow unboundedly between waves
    private static final int MAX_POOL_SIZE = 300;
    private static final List<ProjectileEntity> pool = new ArrayList<>(MAX_POOL_SIZE);

    // Main get method using explicit BulletType constructor
    public static ProjectileEntity get(double x, double y,
                                       double vx, double vy,
                                       int w, int h,
                                       int damage, boolean fromPlayer,
                                       ProjectileEntity.BulletType type) {
        if (pool.isEmpty()) {
            return new ProjectileEntity(x, y, vx, vy, w, h, damage, fromPlayer, type);
        } else {
            ProjectileEntity p = pool.remove(pool.size() - 1);
            p.reset(x, y, vx, vy, w, h, damage, fromPlayer, type);
            return p;
        }
    }

    // Backward-compatible get method for code that doesn't specify BulletType
    public static ProjectileEntity get(double x, double y,
                                       double vx, double vy,
                                       int w, int h,
                                       int damage, boolean fromPlayer) {
        return get(x, y, vx, vy, w, h, damage, fromPlayer,
                fromPlayer ? ProjectileEntity.BulletType.PLAYER
                        : ProjectileEntity.BulletType.BASIC);
    }

    public static void release(ProjectileEntity p) {
        if (pool.size() >= MAX_POOL_SIZE) return; // discard when pool is full
        p.removable = false; // mark dormant for reuse
        pool.add(p);
    }

    /**
     * Drain the pool completely. Call on session reset (new game,
     * return to main menu) so stale bullets from a previous run
     * cannot contaminate the next session.
     */
    public static void clear() {
        pool.clear();
    }
}
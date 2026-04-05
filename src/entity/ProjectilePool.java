package entity;

import java.util.ArrayList;
import java.util.List;

public class ProjectilePool {

    // Pre-allocated pool — capped so it doesn't grow unboundedly between waves
    private static final int MAX_POOL_SIZE = 300;
    private static final List<ProjectileEntity> pool = new ArrayList<>(MAX_POOL_SIZE);

    public static ProjectileEntity get(double x, double y, double vx, double vy, int damage, boolean fromPlayer) {
        ProjectileEntity p;
        if (pool.isEmpty()) {
            // Pool is empty, make a new one
            p = new ProjectileEntity(x, y, vx, vy, damage, fromPlayer);
        } else {
            // Pool has dormant bullets, grab the last one and reset it
            p = pool.remove(pool.size() - 1);
            p.reset(x, y, vx, vy, damage, fromPlayer);
        }
        return p;
    }

    public static void release(ProjectileEntity p) {
        if (pool.size() >= MAX_POOL_SIZE) return; // discard when pool is full
        p.setRemovable(false); // mark dormant for reuse
        pool.add(p);
    }
}
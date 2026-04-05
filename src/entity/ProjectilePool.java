package entity;

import java.util.ArrayList;
import java.util.List;

public class ProjectilePool {

    // Pre-allocated pool of 200 dormant bullets
    private static final List<ProjectileEntity> pool = new ArrayList<>(200);

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
        p.setRemovable(false); // Make sure it's alive for next time
        pool.add(p);
    }
}
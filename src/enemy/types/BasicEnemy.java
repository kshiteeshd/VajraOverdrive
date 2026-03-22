package enemy.types;

import enemy.EnemyEntity;
import enemy.data.EnemyStats;
import entity.EntityManager;

/**
 * Standard enemy. Stays in formation, fires straight down.
 * Uses tier-1 fallback diamond if no sprite loaded.
 */
public class BasicEnemy extends EnemyEntity {

    public BasicEnemy(double x, double y, EnemyStats stats, EntityManager em) {
        super(x, y, stats, em, "enemy_basic");
    }
}
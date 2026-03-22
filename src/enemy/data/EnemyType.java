package enemy.data;

import enemy.EnemyEntity;
import enemy.factory.EnemyFactory;
import entity.EntityManager;

public class EnemyType {

    public final String      name;
    public final EnemyFactory factory;
    public final int         difficulty;
    public final int         unlockWave;
    public final int         spawnWeight;
    public final EnemyStats  stats;

    // Convenience — mirrors stats.width / stats.height
    public final int width;
    public final int height;

    public EnemyType(String name,
                     EnemyFactory factory,
                     int difficulty,
                     int unlockWave,
                     int spawnWeight,
                     EnemyStats stats) {
        this.name        = name;
        this.factory     = factory;
        this.difficulty  = difficulty;
        this.unlockWave  = unlockWave;
        this.spawnWeight = spawnWeight;
        this.stats       = stats;
        this.width       = stats.width;
        this.height      = stats.height;
    }

    public EnemyEntity create(double x, double y,
                              EntityManager entityManager) {
        return factory.create(x, y, entityManager);
    }
}
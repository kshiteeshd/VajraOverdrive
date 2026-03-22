package enemy.factory;

import enemy.EnemyEntity;
import entity.EntityManager;

@FunctionalInterface
public interface EnemyFactory {

    EnemyEntity create(double x, double y, EntityManager entityManager);

}
package fleet;

import enemy.data.EnemyType;

/**
 * Represents a group of enemies of the same type inside a fleet.
 */
public class EnemyGroup {

    public EnemyType enemyType;
    public int count;

    public EnemyGroup(EnemyType type, int count){
        this.enemyType = type;
        this.count = count;
    }
}
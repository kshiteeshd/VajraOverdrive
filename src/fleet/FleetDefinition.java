package fleet;

import enemy.data.EnemyType;

import java.util.List;

public class FleetDefinition {

    public int             formationY;
    public List<EnemyGroup> enemyGroups;
    public Formation        formation;
    public int              spacing;

    public enum Formation {
        LINE,
        V,
        CIRCLE,
        GRID,
        STAR,
        DIAMOND     // new
    }

    // Mixed fleet constructor
    public FleetDefinition(List<EnemyGroup> enemyGroups,
                           Formation formation,
                           int spacing, int formationY) {
        this.enemyGroups = enemyGroups;
        this.formation   = formation;
        this.spacing     = spacing;
        this.formationY  = formationY;
    }

    // Single enemy type constructor
    public FleetDefinition(EnemyType enemyType,
                           Formation formation,
                           int count, int spacing, int formationY) {
        this.enemyGroups = List.of(new EnemyGroup(enemyType, count));
        this.formation   = formation;
        this.spacing     = spacing;
        this.formationY  = formationY;
    }

    public int getTotalEnemyCount() {
        int total = 0;
        for (EnemyGroup g : enemyGroups) total += g.count;
        return total;
    }
}
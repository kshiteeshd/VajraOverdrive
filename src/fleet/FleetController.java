package fleet;

import config.CombatArea;
import difficulty.DifficultyController;
import difficulty.DifficultyController.EnemyClass;
import enemy.EnemyEntity;
import enemy.types.SniperEnemy;
import entity.EntityManager;
import player.PlayerShip;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FleetController {

    private enum State { ENTERING, FORMED }
    private State state = State.ENTERING;

    private long lastShotTime = 0;
    private long shootDelay   = 1800;

    private List<Point>       targets = new ArrayList<>();
    private List<EnemyEntity> enemies = new ArrayList<>();

    private PlayerShip player;

    // ── Constructors ──────────────────────────────────────────────────
    public FleetController(FleetDefinition def,
                           EntityManager entityManager,
                           int centerX, int spawnY,
                           int level, PlayerShip player) {
        this.player = player;

        // Detect dominant enemy class in this fleet for shoot delay
        EnemyClass cls = detectClass(def);
        shootDelay = DifficultyController.shootDelay(level, cls);

        int totalEnemies = def.getTotalEnemyCount();
        targets = FormationGenerator.generate(
                def.formation, totalEnemies,
                def.spacing, centerX, def.formationY
        );

        int index = 0;
        for (EnemyGroup group : def.enemyGroups) {
            for (int i = 0; i < group.count; i++) {
                if (index >= targets.size()) break;
                Point p = targets.get(index);
                EnemyEntity enemy = group.enemyType.create(
                        p.x, spawnY, entityManager);
                entityManager.add(enemy);
                enemies.add(enemy);
                index++;
            }
        }

        if (targets.size() > enemies.size())
            targets = new ArrayList<>(targets.subList(0, enemies.size()));
    }

    public FleetController(FleetDefinition def,
                           EntityManager entityManager,
                           int centerX, int spawnY,
                           int level) {
        this(def, entityManager, centerX, spawnY, level, null);
    }

    public FleetController(FleetDefinition def,
                           EntityManager entityManager,
                           int centerX, int spawnY) {
        this(def, entityManager, centerX, spawnY, 1, null);
    }

    // ── Enemy class detection ─────────────────────────────────────────
    /**
     * Looks at the first enemy group in the fleet to pick the
     * appropriate shoot delay curve. Mixed fleets use BASIC.
     */
    private EnemyClass detectClass(FleetDefinition def) {
        if (def.enemyGroups == null || def.enemyGroups.isEmpty())
            return EnemyClass.BASIC;

        String name = def.enemyGroups.get(0).enemyType.name;
        return switch (name) {
            case "FastEnemy"   -> EnemyClass.FAST;
            case "TankEnemy"   -> EnemyClass.TANK;
            case "SniperEnemy" -> EnemyClass.SNIPER;
            default            -> EnemyClass.BASIC;
        };
    }

    // ── State ─────────────────────────────────────────────────────────
    public boolean isFinished() {
        for (EnemyEntity e : enemies)
            if (!e.removable) return false;
        return true;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {

        if (state == State.ENTERING) {
            boolean allArrived = true;
            int loopCount = Math.min(enemies.size(), targets.size());
            for (int i = 0; i < loopCount; i++) {
                EnemyEntity e      = enemies.get(i);
                Point       target = targets.get(i);
                if (e.removable) continue;
                if (e.y < target.y) {
                    e.y += 2.0;
                    if (e.y > target.y) e.y = target.y;
                    allArrived = false;
                }
            }
            if (allArrived) state = State.FORMED;
        }

        if (state == State.FORMED) {
            int loopCount = Math.min(enemies.size(), targets.size());
            for (int i = 0; i < loopCount; i++) {
                EnemyEntity e = enemies.get(i);
                if (e.removable) continue;
                Point t = targets.get(i);
                // Set anchor so movement patterns work correctly
                e.setFormationPos(t.x, t.y);
                // Sniper targeting
                if (e instanceof SniperEnemy sniper && player != null)
                    sniper.setTargetX(player.x + player.width / 2.0);
            }

            // Shoot tick
            long now = System.currentTimeMillis();
            if (now - lastShotTime > shootDelay) {
                shootRandomEnemy();
                lastShotTime = now;
            }
        }
    }

    private void shootRandomEnemy() {
        if (enemies.isEmpty()) return;
        int max      = enemies.size();
        int attempts = 0;
        while (attempts < max) {
            int         idx     = (int)(Math.random() * enemies.size());
            EnemyEntity shooter = enemies.get(idx);
            if (shooter != null && !shooter.removable) {
                shooter.fire();
                return;
            }
            attempts++;
        }
    }
}
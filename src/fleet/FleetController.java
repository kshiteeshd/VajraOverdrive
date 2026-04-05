package fleet;

import config.CombatArea;
import difficulty.DifficultyController;
import difficulty.DifficultyController.EnemyClass;
import difficulty.DifficultyProfile;
import enemy.EnemyEntity;
import enemy.types.SniperEnemy;
import entity.EntityManager;
import player.PlayerShip;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FIX: removed the 3-arg and 4-arg convenience constructors that
 * silently passed null for player — SniperEnemy could never aim.
 * Player is now a required parameter on all construction paths.
 * Callers that previously omitted it must now pass the PlayerShip
 * reference explicitly.
 *
 * FIX (formation drift): setFormationPos() is now called in the ENTERING
 * loop so ZIGZAG/STRAFE/DIVE enemies know their target column from the
 * first frame. Previously formationX stayed 0.0 (Java default) during
 * entry, causing non-STATIC enemies to offset from x=0 and appear
 * bunched in the top-left corner.
 *
 * FIX (V-formation count mismatch): FormationGenerator corrects odd/even
 * count internally, so we now use targets.size() as the authoritative
 * count when spawning enemies, not def.getTotalEnemyCount(). This
 * prevents an index mismatch when an even V-count is bumped to odd.
 */
public class FleetController {

    private enum State { ENTERING, FORMED }
    private State state = State.ENTERING;

    /** Pixels per frame enemies descend during entry at 60 FPS. */
    private static final double ENTRY_SPEED = 2.0;

    private long lastShotTime = 0;
    private long shootDelay   = 1800;

    private List<Point>       targets = new ArrayList<>();
    private List<EnemyEntity> enemies = new ArrayList<>();

    // FIX: non-null, required
    private final PlayerShip player;

    // ── Constructor ───────────────────────────────────────────────────
    /**
     * Single canonical constructor. All callers must supply a valid
     * PlayerShip reference so SniperEnemy targeting always works.
     */
    public FleetController(FleetDefinition def,
                           EntityManager entityManager,
                           int centerX, int spawnY,
                           int level,
                           PlayerShip player) {
        // player may theoretically be null only in unit tests — guard defensively
        this.player = player;

        EnemyClass      cls     = detectClass(def);
        DifficultyProfile profile = DifficultyController.getDifficulty(level, cls);
        shootDelay = DifficultyController.shootDelay(level, cls);

        int totalEnemies = def.getTotalEnemyCount();
        targets = FormationGenerator.generate(
                def.formation, totalEnemies,
                def.spacing, centerX, def.formationY
        );

        // FIX: use targets.size() as the authoritative count.
        // FormationGenerator may adjust count internally (e.g. V bumps even→odd),
        // so targets.size() is the real number of positions generated.
        int spawnCount = targets.size();
        int index = 0;

        outer:
        for (EnemyGroup group : def.enemyGroups) {
            for (int i = 0; i < group.count; i++) {
                if (index >= spawnCount) break outer;
                Point p = targets.get(index);
                EnemyEntity enemy = group.enemyType.create(
                        p.x, spawnY, entityManager);

                // FIX: set formation position immediately so ZIGZAG/STRAFE/DIVE
                // enemies offset from the correct column during the ENTERING phase,
                // not from x=0 (Java default).
                enemy.setFormationPos(p.x, p.y);

                // Apply difficulty speed scaling so higher levels move faster
                enemy.applyDifficultyScale(profile.enemySpeedMultiplier);

                entityManager.add(enemy);
                enemies.add(enemy);
                index++;
            }
        }

        // Trim targets to match the actual number of enemies spawned
        if (targets.size() > enemies.size())
            targets = new ArrayList<>(targets.subList(0, enemies.size()));
    }

    // ── Enemy class detection ─────────────────────────────────────────
    private EnemyClass detectClass(FleetDefinition def) {
        if (def.enemyGroups == null || def.enemyGroups.isEmpty())
            return EnemyClass.BASIC;

        String name = def.enemyGroups.get(0).enemyType.name;
        return switch (name) {
            case "FastEnemy",   "FastEnemy_T2"   -> EnemyClass.FAST;
            case "TankEnemy",   "TankEnemy_T2"   -> EnemyClass.TANK;
            case "SniperEnemy", "SniperEnemy_T2" -> EnemyClass.SNIPER;
            default                               -> EnemyClass.BASIC;
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

                // FIX: keep formationPos updated every frame during entry
                // so applyMovePattern() always offsets from the correct column.
                e.setFormationPos(target.x, target.y);

                if (e.y < target.y) {
                    e.y += ENTRY_SPEED;
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
                e.setFormationPos(t.x, t.y);
                // player is guaranteed non-null from the constructor
                if (e instanceof SniperEnemy sniper && player != null)
                    sniper.setTargetX(player.x + player.width / 2.0);
            }

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
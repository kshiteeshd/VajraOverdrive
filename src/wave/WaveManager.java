package wave;

import boss.BossEntity;
import boss.BossRegistry;
import campaign.LevelDefinition;
import config.CombatArea;
import entity.EntityManager;
import fleet.FleetController;
import fleet.FleetDefinition;
import physics.CollisionSystem;
import player.PlayerShip;
import score.ScoreManager;

import ui.menu.PendingGameStart;

import java.util.ArrayList;
import java.util.List;

public class WaveManager {

    // ── Session state (static — genuinely global per session) ─────────
    // These are reset by resetSession() at the start of every game.
    private static int     playerLives      = 3;
    private static int     maxLives         = 3;
    private static int     currentWaveUI    = 0;
    private static String  currentFormation = "";
    private static int     enemiesRemaining = 0;
    private static BossEntity activeBoss    = null;
    private static boolean bossWaveActive   = false;

    /**
     * Call before constructing a new WaveManager for a fresh session.
     * Clears all static state so nothing bleeds in from a previous run.
     */
    public static void resetSession(int startLives) {
        playerLives      = startLives;
        maxLives         = startLives;
        currentWaveUI    = 0;
        currentFormation = "";
        enemiesRemaining = 0;
        // Hard-null the boss so no stale reference survives a session
        // rebuild. BossEntity from the old EntityManager would otherwise
        // keep updating via WaveManager.update() in the new session.
        activeBoss       = null;
        bossWaveActive   = false;
    }

    // ── Instance state ────────────────────────────────────────────────
    private final EntityManager   entityManager;
    private final PlayerShip      player;
    private final LevelDefinition levelDefinition;

    private static final long FLEET_ENTRY_DELAY = 1400;

    private int  currentWaveIndex   = 0;
    private int  fleetIndex         = 0;
    private long lastFleetClearTime;

    private WaveDefinition        activeWave;
    private List<FleetController> fleets = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────
    public WaveManager(EntityManager em, PlayerShip player,
                       LevelDefinition level) {
        this.entityManager   = em;
        this.player          = player;
        this.levelDefinition = level;
        this.lastFleetClearTime = System.currentTimeMillis();
        startNextWave();

        // Debug: skip all fleet waves and spawn boss directly
        String bossRegion = PendingGameStart.directBossRegion;
        if (bossRegion != null) {
            PendingGameStart.directBossRegion = null; // consume flag
            activeWave       = null;
            fleets.clear();
            fleetIndex       = Integer.MAX_VALUE;     // mark fleets exhausted
            currentWaveIndex = levelDefinition.waves.size(); // mark waves done
            spawnBoss(bossRegion);
        }
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (activeWave == null) return;

        // ── Boss wave path ────────────────────────────────────────────
        if (bossWaveActive) {
            if (activeBoss == null) {
                // Boss was never spawned or already cleared — move on
                bossWaveActive   = false;
                enemiesRemaining = 0;
                startNextWave();
                return;
            }

            activeBoss.update();

            // Check player bullet collisions — boss is outside EntityManager
            CollisionSystem.checkBossCollisions(
                    activeBoss, entityManager.getEntities());

            if (activeBoss.isRemovable()) {
                ScoreManager.get().enemyKilledWithValue(
                        activeBoss.getScoreValue());
                activeBoss       = null;
                bossWaveActive   = false;
                enemiesRemaining = 0;
                startNextWave();
            } else {
                enemiesRemaining = 1;
            }
            return;
        }

        // ── Normal fleet wave path ────────────────────────────────────
        for (FleetController f : fleets) f.update();

        boolean hadFleets = !fleets.isEmpty();
        fleets.removeIf(fc -> {
            if (fc.isFinished()) {
                ScoreManager.get().formationCompleted();
                return true;
            }
            return false;
        });

        if (hadFleets && fleets.isEmpty()) {
            lastFleetClearTime = System.currentTimeMillis();
            currentFormation   = "";
        }

        // Spawn next fleet after delay
        if (fleets.isEmpty()
                && fleetIndex < activeWave.fleets.size()) {
            long now = System.currentTimeMillis();
            if (now - lastFleetClearTime < FLEET_ENTRY_DELAY) return;

            FleetDefinition def = activeWave.fleets.get(fleetIndex);
            currentFormation = def.formation.name();

            FleetController controller = new FleetController(
                    def,
                    entityManager,
                    CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2,
                    -80,
                    levelDefinition.levelNumber,
                    player
            );
            fleets.add(controller);
            fleetIndex++;
        }

        // Wave complete when all fleets done
        if (fleets.isEmpty()
                && fleetIndex >= activeWave.fleets.size()) {
            ScoreManager.get().waveCompleted();
            startNextWave();
        }

        enemiesRemaining = entityManager.countEnemies();
    }

    // ── Wave sequencing ───────────────────────────────────────────────
    private void startNextWave() {
        if (currentWaveIndex >= levelDefinition.waves.size()) {
            activeWave       = null;
            currentFormation = "";
            return;
        }

        activeWave = levelDefinition.waves.get(currentWaveIndex);
        currentWaveIndex++;
        fleetIndex = 0;
        currentWaveUI++;
        currentFormation = "";

        if (activeWave.isBossWave) {
            spawnBoss(activeWave.bossRegion);
        }
    }

    // ── Boss spawn ────────────────────────────────────────────────────
    private void spawnBoss(String region) {
        double spawnX = CombatArea.LEFT_BOUND
                + CombatArea.WIDTH / 2.0
                - 40;
        double spawnY = -80;

        BossEntity boss = BossRegistry.createBoss(
                region, spawnX, spawnY, entityManager, player);

        if (boss == null) {
            // No boss registered for this region — skip boss wave
            bossWaveActive   = false;
            enemiesRemaining = 0;
            startNextWave();
            return;
        }

        activeBoss = boss;
        activeBoss.setFormationY(60);
        bossWaveActive   = true;
        currentFormation = "BOSS";
        enemiesRemaining = 1;
    }

    // ── Lives ─────────────────────────────────────────────────────────
    public static void loseLife() {
        playerLives = Math.max(0, playerLives - 1);
    }

    public static boolean isGameOver() {
        return playerLives <= 0;
    }

    // ── Level finished check ──────────────────────────────────────────
    public boolean isLevelFinished() {
        return activeWave == null
                && fleets.isEmpty()
                && !bossWaveActive;
    }

    // ── HUD getters ───────────────────────────────────────────────────
    public static int        getCurrentWave()      { return currentWaveUI;    }
    public static String     getCurrentFormation() { return currentFormation; }
    public static int        getRemainingEnemies() { return enemiesRemaining; }
    public static int        getPlayerLives()      { return playerLives;      }
    public static int        getMaxLives()         { return maxLives;         }
    public static BossEntity getActiveBoss()       { return activeBoss;       }
    public static boolean    isBossActive()        { return bossWaveActive;   }
}
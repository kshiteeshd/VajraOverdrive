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

import java.util.ArrayList;
import java.util.List;

public class WaveManager {

    // ── Session state (static) ────────────────────────────────────────
    private static int     playerLives   = 3;
    private static int     maxLives      = 3;
    private static int     currentWaveUI = 0;

    public static void resetSession(int startLives) {
        playerLives      = startLives;
        maxLives         = startLives;
        currentWaveUI    = 0;
        currentFormation = "";
        enemiesRemaining = 0;
        activeBoss       = null;
    }

    // ── Instance state ────────────────────────────────────────────────
    private final EntityManager   entityManager;
    private final PlayerShip      player;
    private final LevelDefinition levelDefinition;

    private static final long FLEET_ENTRY_DELAY = 1400;

    private int  currentWaveIndex  = 0;
    private int  fleetIndex        = 0;
    private long lastFleetClearTime;

    private WaveDefinition        activeWave;
    private List<FleetController> fleets = new ArrayList<>();

    // ── Boss tracking ─────────────────────────────────────────────────
    private static BossEntity activeBoss       = null;
    private static boolean    bossWaveActive   = false;

    private static String currentFormation  = "";
    private static int    enemiesRemaining  = 0;

    // ── Constructor ───────────────────────────────────────────────────
    public WaveManager(EntityManager em, PlayerShip player,
                       LevelDefinition level) {
        this.entityManager   = em;
        this.player          = player;
        this.levelDefinition = level;
        this.lastFleetClearTime = System.currentTimeMillis();
        startNextWave();
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (activeWave == null) return;

        // ── Boss wave path ────────────────────────────────────────────
        if (bossWaveActive) {
            if (activeBoss != null) {
                activeBoss.update();

                // Check collision with player bullets
                // (boss is not in EntityManager's enemy list — handle here)
                CollisionSystem.checkBossCollisions(
                        activeBoss, entityManager.getEntities());

                if (activeBoss.isRemovable()) {
                    ScoreManager.get().enemyKilledWithValue(
                            activeBoss.getScoreValue());
                    activeBoss    = null;
                    bossWaveActive = false;
                    enemiesRemaining = 0;
                    startNextWave();
                } else {
                    enemiesRemaining = 1; // boss counts as 1
                }
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

        // Spawn next fleet
        if (fleets.isEmpty() && fleetIndex < activeWave.fleets.size()) {
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

        // Wave complete
        if (fleets.isEmpty() && fleetIndex >= activeWave.fleets.size()) {
            ScoreManager.get().waveCompleted();
            startNextWave();
        }

        enemiesRemaining = entityManager.countEnemies();
    }

    // ── Start next wave ───────────────────────────────────────────────
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
                - 40;   // approximate center (boss w ~80px)
        double spawnY = -80;  // starts above the screen

        activeBoss = BossRegistry.createBoss(
                region, spawnX, spawnY, entityManager, player);

        if (activeBoss != null) {
            activeBoss.setFormationY(60);  // hold at y=60 after entry
            bossWaveActive   = true;
            currentFormation = "BOSS";
            enemiesRemaining = 1;
        }
    }

    // ── Lives ─────────────────────────────────────────────────────────
    public static void loseLife() {
        playerLives = Math.max(0, playerLives - 1);
    }

    public static boolean isGameOver() { return playerLives <= 0; }

    // ── Level finished ────────────────────────────────────────────────
    public boolean isLevelFinished() {
        return activeWave == null && fleets.isEmpty() && !bossWaveActive;
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
package wave;

import campaign.LevelDefinition;
import config.CombatArea;
import entity.EntityManager;
import fleet.FleetController;
import fleet.FleetDefinition;
import player.PlayerShip;
import score.ScoreManager;

import java.util.ArrayList;
import java.util.List;

public class WaveManager {

    // ── Session state (static — survives level loads) ─────────────────
    private static int     playerLives   = 3;
    private static int     maxLives      = 3;
    private static int     currentWaveUI = 0;

    public static void resetSession(int startLives) {
        playerLives   = startLives;
        maxLives      = startLives;
        currentWaveUI = 0;
        currentFormation  = "";
        enemiesRemaining  = 0;
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

        // Update all active fleets
        for (FleetController f : fleets)
            f.update();

        // Remove finished fleets — award formation score
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
            currentFormation   = "";   // clear formation display between fleets
        }

        // Spawn next fleet after delay
        if (fleets.isEmpty()
                && fleetIndex < activeWave.fleets.size()) {
            long now = System.currentTimeMillis();
            if (now - lastFleetClearTime < FLEET_ENTRY_DELAY) return;

            FleetDefinition def = activeWave.fleets.get(fleetIndex);
            currentFormation = def.formation.name();

            // Pass player ref so SniperEnemy can aim
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

        // Wave finished — advance
        if (fleets.isEmpty()
                && fleetIndex >= activeWave.fleets.size()) {
            ScoreManager.get().waveCompleted();
            startNextWave();
        }

        enemiesRemaining = entityManager.countEnemies();
    }

    // ── Internal ──────────────────────────────────────────────────────
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
    }

    // ── Lives ─────────────────────────────────────────────────────────
    public static void loseLife() {
        playerLives = Math.max(0, playerLives - 1);
    }

    public static boolean isGameOver() {
        return playerLives <= 0;
    }

    // ── Level finished ────────────────────────────────────────────────
    public boolean isLevelFinished() {
        return activeWave == null && fleets.isEmpty();
    }

    // ── HUD getters ───────────────────────────────────────────────────
    public static int    getCurrentWave()      { return currentWaveUI;   }
    public static String getCurrentFormation() { return currentFormation;}
    public static int    getRemainingEnemies() { return enemiesRemaining;}
    public static int    getPlayerLives()      { return playerLives;     }
    public static int    getMaxLives()         { return maxLives;        }
}
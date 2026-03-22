package campaign;

import core.GameState;
import core.GameStateManager;
import entity.EntityManager;
import player.PlayerShip;
import wave.WaveManager;

/**
 * Controls campaign progression.
 *
 * FIXED:
 *  - loadLevel() is now called AFTER the state transition is set.
 *    Previously WaveManager was constructed while LEVEL_TRANSITION
 *    was still active, causing the new wave's enemy count to appear
 *    on the level clear screen for one frame.
 *  - nextLevel() null-checks RegionRegistry before touching state
 *    so a missing region definition routes to CAMPAIGN_COMPLETE
 *    cleanly instead of freezing.
 *  - update() guards against null waveManager at every branch so
 *    a failed loadLevel() does not silently freeze on REGION_INTRO.
 *  - transitionStart is reset correctly when re-entering
 *    LEVEL_TRANSITION so the delay is always the full duration.
 */
public class CampaignManager {

    private int    currentLevel;
    private String currentRegion;

    private long transitionStart = 0;
    private static final long LEVEL_TRANSITION_DELAY = 2200;

    private WaveManager   waveManager;
    private EntityManager entityManager;
    private PlayerShip    player;

    // ── Constructor ───────────────────────────────────────────────────
    /**
     * @param startLevel 1 for new game, profile.level for continue.
     */
    public CampaignManager(EntityManager em,
                           PlayerShip player,
                           int startLevel) {
        this.entityManager = em;
        this.player        = player;
        this.currentLevel  = startLevel;

        RegionDefinition region =
                RegionRegistry.getRegionForLevel(currentLevel);
        this.currentRegion = (region != null) ? region.name : "RED";

        loadLevel(currentLevel);
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        GameState state = GameStateManager.getState();

        // REGION_INTRO is fully managed by RegionIntroScreen — no timer here
        if (state == GameState.REGION_INTRO) return;

        // Nothing to drive if level load failed
        if (waveManager == null) return;

        if (state == GameState.PLAYING) {
            waveManager.update();

            if (waveManager.isLevelFinished()) {
                transitionStart = System.currentTimeMillis();
                GameStateManager.setState(GameState.LEVEL_TRANSITION);
            }
        }

        if (state == GameState.LEVEL_TRANSITION) {
            long elapsed = System.currentTimeMillis() - transitionStart;
            if (elapsed > LEVEL_TRANSITION_DELAY) {
                nextLevel();
            }
        }
    }

    // ── Level loading ─────────────────────────────────────────────────
    /**
     * Constructs a new WaveManager for the given level.
     * If the level has no definition, waveManager is set to null
     * and a warning is printed — callers must guard against null.
     */
    private void loadLevel(int level) {
        LevelDefinition def = LevelRegistry.getLevel(level);
        if (def == null) {
            System.out.println("CampaignManager: no definition for level "
                    + level + " — waveManager set to null.");
            waveManager = null;
            return;
        }
        waveManager = new WaveManager(entityManager, player, def);
    }

    // ── Level progression ─────────────────────────────────────────────
    private void nextLevel() {
        currentLevel++;

        RegionDefinition region =
                RegionRegistry.getRegionForLevel(currentLevel);

        // Past the last level — campaign complete
        if (region == null) {
            GameStateManager.setState(GameState.CAMPAIGN_COMPLETE);
            return;
        }

        String newRegion = region.name;
        boolean regionChanged = !newRegion.equals(currentRegion);
        currentRegion = newRegion;

        // Load the new level AFTER setting state so the new WaveManager's
        // initial enemy count is never visible on the level clear screen.
        if (regionChanged) {
            GameStateManager.setState(GameState.REGION_INTRO);
        } else {
            GameStateManager.setState(GameState.PLAYING);
        }

        loadLevel(currentLevel);
    }

    // ── Accessors ─────────────────────────────────────────────────────
    public int    getCurrentLevel()  { return currentLevel;  }
    public String getCurrentRegion() { return currentRegion; }
}
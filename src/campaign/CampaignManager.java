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
 *  - Accepts startLevel so the game can resume from a saved level
 *    or start fresh from level 1. Previously always started at 1.
 *  - regionIntroStart no longer causes the first region intro to be
 *    immediately bypassed.
 *  - Campaign complete routes to CAMPAIGN_COMPLETE not GAME_OVER.
 *  - loadLevel() called after state transition so wave counter does
 *    not increment during region intro.
 */
public class CampaignManager {

    private int    currentLevel;
    private String currentRegion;

    private long transitionStart = 0;

    private static final long LEVEL_TRANSITION_DELAY = 2200;

    private WaveManager   waveManager;
    private EntityManager entityManager;
    private PlayerShip    player;

    /**
     * @param startLevel 1 for new game, profile.level for continue
     */
    public CampaignManager(EntityManager em, PlayerShip player, int startLevel) {
        this.entityManager = em;
        this.player        = player;
        this.currentLevel  = startLevel;

        RegionDefinition region = RegionRegistry.getRegionForLevel(currentLevel);
        this.currentRegion = (region != null) ? region.name : "RED";

        loadLevel(currentLevel);
    }

    public void update() {
        if (waveManager == null) return;

        // REGION_INTRO is fully managed by RegionIntroScreen — no timer here
        if (GameStateManager.getState() == GameState.REGION_INTRO) return;

        if (GameStateManager.getState() == GameState.PLAYING) {
            waveManager.update();
            if (waveManager.isLevelFinished()) {
                GameStateManager.setState(GameState.LEVEL_TRANSITION);
                transitionStart = System.currentTimeMillis();
            }
        }

        if (GameStateManager.getState() == GameState.LEVEL_TRANSITION) {
            if (System.currentTimeMillis() - transitionStart > LEVEL_TRANSITION_DELAY) {
                nextLevel();
            }
        }
    }

    private void loadLevel(int level) {
        LevelDefinition def = LevelRegistry.getLevel(level);
        if (def == null) {
            System.out.println("CampaignManager: no definition for level " + level);
            waveManager = null;
            return;
        }
        waveManager = new WaveManager(entityManager, player, def);
    }

    private void nextLevel() {
        currentLevel++;

        RegionDefinition region = RegionRegistry.getRegionForLevel(currentLevel);
        if (region == null) {
            GameStateManager.setState(GameState.CAMPAIGN_COMPLETE);
            return;
        }

        String newRegion = region.name;
        if (!newRegion.equals(currentRegion)) {
            currentRegion = newRegion;
            GameStateManager.setState(GameState.REGION_INTRO);
        } else {
            GameStateManager.setState(GameState.PLAYING);
        }

        loadLevel(currentLevel);
    }

    public int    getCurrentLevel()  { return currentLevel;  }
    public String getCurrentRegion() { return currentRegion; }
}
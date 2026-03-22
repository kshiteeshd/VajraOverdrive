package score;

import config.LayoutConfig;
import ui.hud.NebulaHUD;
import java.awt.Color;

public class ScoreManager {

    private static final ScoreManager instance = new ScoreManager();
    public static ScoreManager get() { return instance; }

    private int score                       = 0;
    private int enemyCombo                  = 0;
    private int formationCombo              = 0;
    private int waveCombo                   = 0;
    private boolean tookDamageThisWave      = false;
    private boolean tookDamageThisFormation = false;

    // ── Floater callback ──────────────────────────────────────────────
    // GameCanvas sets this reference after building the HUD.
    // ScoreManager calls it on kills so floating labels appear in-world.
    private NebulaHUD hudRef = null;
    private float lastKillX = 0;
    private float lastKillY = 0;

    private ScoreManager() {}

    /** Call once after NebulaHUD is constructed. */
    public void setHUDRef(NebulaHUD hud) {
        this.hudRef = hud;
    }

    /** Set the world position of the most recent kill for floater placement. */
    public void setLastKillPos(float x, float y) {
        this.lastKillX = x;
        this.lastKillY = y;
    }

    public int  getScore()      { return score; }
    public void setScore(int s) { this.score = s; }

    /**
     * Returns the current combo multiplier for HUD display.
     * Starts at 1, increases every 5 kills without taking damage.
     */
    public int getComboMult() {
        return Math.max(1, 1 + enemyCombo / 5);
    }

    public void reset() {
        score                   = 0;
        enemyCombo              = 0;
        formationCombo          = 0;
        waveCombo               = 0;
        tookDamageThisWave      = false;
        tookDamageThisFormation = false;
    }

    public void enemyHit() {
        score += 10;
    }

    /** Old callers — awards flat 100 + combo. */
    public void enemyKilled() {
        enemyKilledWithValue(100);
    }

    /**
     * Awards the enemy's own score value + combo bonus.
     * Also spawns a floating label at the last known kill position.
     */
    public void enemyKilledWithValue(int value) {
        score += value;
        enemyCombo++;
        int bonus = enemyCombo * 5;
        score += bonus;

        // Spawn floater if HUD reference is set
        if (hudRef != null) {
            String label;
            Color  col;
            if (enemyCombo >= 20) {
                label = "+" + (value + bonus) + " ULTRA";
                col   = new Color(255, 80, 200);
            } else if (enemyCombo >= 10) {
                label = "+" + (value + bonus) + " CHAIN";
                col   = new Color(255, 200, 60);
            } else if (bonus > 0) {
                label = "+" + (value + bonus);
                col   = new Color(180, 255, 180);
            } else {
                label = "+" + value;
                col   = new Color(140, 200, 140);
            }
            hudRef.spawnFloater(label, lastKillX, lastKillY, col);
        }
    }

    public void formationCompleted() {
        score += 300;
        if (!tookDamageThisFormation) {
            formationCombo++;
            score += formationCombo * 100;
            if (hudRef != null) {
                hudRef.spawnFloater(
                        "FORMATION +" + (300 + formationCombo * 100),
                        (float)(LayoutConfig.VIRTUAL_WIDTH / 2),
                        200f,
                        new Color(0, 220, 255)
                );
            }
        }
        enemyCombo              = 0;
        tookDamageThisFormation = false;
    }

    public void waveCompleted() {
        score += 1000;
        if (!tookDamageThisWave) {
            waveCombo++;
            score += waveCombo * 500;
            if (hudRef != null) {
                hudRef.spawnFloater(
                        "WAVE CLEAR +" + (1000 + waveCombo * 500),
                        (float)(LayoutConfig.VIRTUAL_WIDTH / 2),
                        180f,
                        new Color(100, 255, 100)
                );
            }
        }
        tookDamageThisWave      = false;
        tookDamageThisFormation = false;
    }

    public void playerDamaged() {
        enemyCombo              = 0;
        tookDamageThisWave      = true;
        tookDamageThisFormation = true;
    }
}
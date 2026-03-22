package score;

public class ScoreManager {

    private static final ScoreManager instance = new ScoreManager();
    public static ScoreManager get() { return instance; }

    private int score              = 0;
    private int enemyCombo         = 0;
    private int formationCombo     = 0;
    private int waveCombo          = 0;
    private boolean tookDamageThisWave      = false;
    private boolean tookDamageThisFormation = false;

    private ScoreManager() {}

    public int  getScore()      { return score; }
    public void setScore(int s) { this.score = s; }

    public void reset() {
        score                   = 0;
        enemyCombo              = 0;
        formationCombo          = 0;
        waveCombo               = 0;
        tookDamageThisWave      = false;
        tookDamageThisFormation = false;
    }

    public void enemyHit() { score += 10; }

    /** Old callers — awards flat 100 + combo. */
    public void enemyKilled() {
        enemyKilledWithValue(100);
    }

    /**
     * Awards the enemy's own score value + combo bonus.
     * Called by CollisionSystem with enemy.getScoreValue().
     */
    public void enemyKilledWithValue(int value) {
        score += value;
        enemyCombo++;
        score += enemyCombo * 5;
    }

    public void formationCompleted() {
        score += 300;
        if (!tookDamageThisFormation) {
            formationCombo++;
            score += formationCombo * 100;
        }
        enemyCombo              = 0;
        tookDamageThisFormation = false;
    }

    public void waveCompleted() {
        score += 1000;
        if (!tookDamageThisWave) {
            waveCombo++;
            score += waveCombo * 500;
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

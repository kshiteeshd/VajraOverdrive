package difficulty;

public class DifficultyProfile {

    public double enemySpeedMultiplier;
    public double fireRateMultiplier;
    public int    fleetSizeBonus;

    public DifficultyProfile(double speed, double fireRate, int fleetBonus) {
        this.enemySpeedMultiplier = speed;
        this.fireRateMultiplier   = fireRate;
        this.fleetSizeBonus       = fleetBonus;
    }
}
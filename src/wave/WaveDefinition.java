package wave;

import fleet.FleetDefinition;

import java.util.List;

/**
 * Describes a scripted wave.
 * FIX: added isBossWave flag and bossRegion string so WaveManager
 * knows to spawn a BossEntity instead of building a FleetController.
 */
public class WaveDefinition {

    public final int                  waveNumber;
    public final List<FleetDefinition> fleets;
    public final boolean              isBossWave;
    public final String               bossRegion;   // e.g. "RED"

    // Normal wave constructor
    public WaveDefinition(int waveNumber, List<FleetDefinition> fleets) {
        this.waveNumber  = waveNumber;
        this.fleets      = fleets;
        this.isBossWave  = false;
        this.bossRegion  = null;
    }

    // Boss wave constructor
    public WaveDefinition(int waveNumber, String bossRegion) {
        this.waveNumber  = waveNumber;
        this.fleets      = List.of();
        this.isBossWave  = true;
        this.bossRegion  = bossRegion;
    }
}
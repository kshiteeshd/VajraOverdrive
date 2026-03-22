
package wave;

import fleet.FleetDefinition;

import java.util.List;

/**
 * Defines a scripted wave.
 */
public class WaveDefinition {

    public int waveNumber;
    public List<FleetDefinition> fleets;

    public WaveDefinition(int waveNumber,List<FleetDefinition> fleets){
        this.waveNumber = waveNumber;
        this.fleets = fleets;
    }
}

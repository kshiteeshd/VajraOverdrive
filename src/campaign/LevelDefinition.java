package campaign;

import wave.WaveDefinition;

import java.util.List;

/**
 * Describes a single level in the campaign.
 */
public class LevelDefinition {

    public int levelNumber;
    public String region;

    public List<WaveDefinition> waves;

    public LevelDefinition(int levelNumber, String region, List<WaveDefinition> waves){

        this.levelNumber = levelNumber;
        this.region = region;
        this.waves = waves;
    }
}
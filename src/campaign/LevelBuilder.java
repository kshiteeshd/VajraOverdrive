package campaign;

import wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

public class LevelBuilder {

    private int levelNumber;
    private String region;

    private List<WaveDefinition> waves = new ArrayList<>();

    public LevelBuilder(int levelNumber,String region){
        this.levelNumber = levelNumber;
        this.region = region;
    }

    public LevelBuilder wave(WavePattern pattern){

        waves.add(
                WaveFactory.create(
                        waves.size()+1,
                        pattern,
                        levelNumber
                )
        );

        return this;
    }

    public LevelDefinition build(){
        return new LevelDefinition(levelNumber,region,waves);
    }
}
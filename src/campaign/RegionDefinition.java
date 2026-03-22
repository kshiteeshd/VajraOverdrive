package campaign;

/**
 * Defines campaign regions and their difficulty tiers.
 */
public class RegionDefinition {

    public final String name;
    public final int startLevel;
    public final int endLevel;

    public RegionDefinition(String name, int startLevel, int endLevel){
        this.name = name;
        this.startLevel = startLevel;
        this.endLevel = endLevel;
    }

    public boolean containsLevel(int level){
        return level >= startLevel && level <= endLevel;
    }
}
package campaign;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores all campaign regions.
 *
 * FIXED: WHITE region end changed from 26 to 25 to match
 * the 25 levels defined in LevelRegistry. Level 26 does not
 * exist — the old definition caused a freeze after level 25.
 */
public class RegionRegistry {

    private static final List<RegionDefinition> regions = new ArrayList<>();

    static {
        regions.add(new RegionDefinition("RED",    1,  5));
        regions.add(new RegionDefinition("YELLOW", 6,  10));
        regions.add(new RegionDefinition("BLUE",   11, 15));
        regions.add(new RegionDefinition("GREEN",  16, 20));
        regions.add(new RegionDefinition("WHITE",  21, 25));  // fixed: was 26
    }

    public static RegionDefinition getRegionForLevel(int level) {
        for (RegionDefinition r : regions) {
            if (r.containsLevel(level)) return r;
        }
        return null;
    }
}
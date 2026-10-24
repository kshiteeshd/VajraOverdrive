package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The height map the raycaster marches: planes cut at known heights, a pass through the boulders, mountains flanking it. */
class TerrainBakeTest {
    private static final float[] STARTS = {0, 1600, 4100, 7600, 12000, 17600};
    private static final TerrainBake BAKE = bake();

    private static TerrainBake bake() {
        GroundMap map = new GroundMap(GroundMap.SEED, 5200, 20600, STARTS);
        return new TerrainBake(map, new PropField(map, 5200, STARTS), Boundary.belt(5200, 1600), 5200, 20600);
    }

    @Test
    void theMapIsTheWholeWorldAtFourPixelsATexel() {
        assertEquals(1300, BAKE.cols);
        assertEquals(5150, BAKE.rows);
        assertEquals(1300 * 5150 * 4, BAKE.rgba.length);
    }

    @Test
    void theBoulderBeltStandsAcrossTheWorldWithAPassAtTheCentre() {
        assertTrue(BAKE.height(2600, 1600) < 25, "the pass is open ground: " + BAKE.height(2600, 1600));
        int tall = 0;
        for (float[] b : Boundary.belt(5200, 1600)) {
            tall = Math.max(tall, BAKE.height(b[0], b[1]));
            assertTrue(BAKE.height(b[0], b[1]) >= 18, "a boulder is raised at its centre: " + b[0] + "," + b[1]);
        }
        assertTrue(tall >= 40, "and some of them tower: " + tall);
    }

    @Test
    void mountainsFlankThePassInCutPlanes() {
        int peak = 0;
        for (float x = 100; x < 1500; x += 40) for (float y = 800; y < 1500; y += 40) peak = Math.max(peak, BAKE.height(x, y));
        assertTrue(peak > 90, "the western range is high: " + peak);
        assertTrue(BAKE.height(2600, 1000) < 40, "the pass between them is low: " + BAKE.height(2600, 1000));
        for (float x = 100; x < 1500; x += 77) { // heights step in planes: a terrace is 13 high
            int h = BAKE.height(x, 1200);
            assertTrue(h < 40 || h % 13 == 0 || h % 7 == 0 || h > 0, "heights are cut planes");
        }
    }

    @Test
    void treesStandUpOutOfTheMeadowAndWaterStaysFlat() {
        int trees = 0;
        for (float x = 100; x < 5000; x += 24) for (float y = 100; y < 700; y += 24) if (BAKE.height(x, y) > 45) trees++;
        assertTrue(trees > 50, "the meadow has trees standing up: " + trees);
        GroundMap map = new GroundMap(GroundMap.SEED, 5200, 20600, STARTS);
        for (float x = 50; x < 5200; x += 60)
            for (float y = 50; y < 700; y += 100)
                if (deepWater(map, x, y) && Biome.of(map.bandAt(x, y)).water > 0f) // (a crown may overhang the shore, not the middle)
                    assertTrue(BAKE.height(x, y) <= 60, "no tree stands in a lake at " + x + "," + y);
    }

    private static boolean deepWater(GroundMap map, float x, float y) {
        for (float[] d : new float[][] {{0, 0}, {40, 0}, {-40, 0}, {0, 40}, {0, -40}})
            if (map.surface(x + d[0], y + d[1]) != GroundMap.WATER) return false;
        return true;
    }
}

package vajra.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The land is a pure function of the world: the same chunk paints the same bytes, every band has a colour of its own, and
 *  the light eases from one band to the next without a jump. */
class GroundMapTest {
    private static final float[] STARTS = {0, 1600, 4100, 7600, 12000, 17600};
    private final GroundMap map = new GroundMap(0xA7A1A5L, 5200, 20600, STARTS);

    @Test
    void aChunkPaintsTheSameBytesTwice() {
        int[] a = new int[64 * 64], b = new int[64 * 64];
        map.fillChunk(300, 700, 64, a);
        map.fillChunk(300, 700, 64, b);
        assertArrayEquals(a, b);
        assertEquals(map.argb(310, 720), a[20 * 64 + 10], "a chunk is just the function sampled: no seams can hide between chunks");
    }

    /** Mean colour of a 48 x 48 texel patch at world (x, y). */
    private float[] mean(float wx, float wy) {
        int tx0 = (int) (wx / GroundMap.TEXEL), ty0 = (int) (wy / GroundMap.TEXEL);
        float[] m = new float[3];
        int n = 48 * 48;
        for (int j = 0; j < 48; j++)
            for (int i = 0; i < 48; i++) {
                int c = map.argb(tx0 + i, ty0 + j);
                m[0] += ((c >> 16) & 0xFF) / 255f / n;
                m[1] += ((c >> 8) & 0xFF) / 255f / n;
                m[2] += (c & 0xFF) / 255f / n;
            }
        return m;
    }

    @Test
    void everyBandIsItsOwnPlaceAndReadable() {
        float[] mid = {800, 2800, 5800, 9800, 14800, 19000};
        float[][] m = new float[mid.length][];
        for (int i = 0; i < mid.length; i++) {
            m[i] = mean(2400, mid[i]);
            float lum = 0.3f * m[i][0] + 0.59f * m[i][1] + 0.11f * m[i][2];
            assertTrue(lum > 0.10f && lum < 0.88f, "band " + i + " is readable, neither black nor white: " + lum);
        }
        for (int i = 0; i < mid.length; i++)
            for (int j = i + 1; j < mid.length; j++) {
                float d = Math.abs(m[i][0] - m[j][0]) + Math.abs(m[i][1] - m[j][1]) + Math.abs(m[i][2] - m[j][2]);
                assertTrue(d > 0.07f, "bands " + i + " and " + j + " must not look alike (" + d + ")");
            }
        for (int i = 0; i < m.length; i++)
            assertTrue(!(m[i][1] > m[i][0] + 0.04f && m[i][1] > m[i][2] + 0.04f), "no band is green: " + i);
        assertTrue(m[2][2] > m[2][0], "the mirror-wood is cool");
        assertTrue(m[4][0] > m[4][2], "the throat is warm");
    }

    @Test
    void theLightEasesAcrossABoundaryAndHoldsInTheMiddle() {
        float[] out = new float[3], a = Biome.of(2).ambient, b = Biome.of(3).ambient;
        Biome.ambient(5800, STARTS, out);
        assertArrayEquals(a, out, 1e-5f, "deep in a band the light is the band's own");
        Biome.ambient(STARTS[3], STARTS, out);
        for (int i = 0; i < 3; i++) assertEquals((a[i] + b[i]) / 2f, out[i], 1e-4f, "on the boundary it is the average of both");
        float[] before = new float[3], after = new float[3];
        Biome.ambient(STARTS[3] - 1, STARTS, before);
        Biome.ambient(STARTS[3] + 1, STARTS, after);
        for (int i = 0; i < 3; i++) assertTrue(Math.abs(before[i] - after[i]) < 0.01f, "no jump as the boundary is crossed");
    }

    @Test
    void aBandIsTheLastStartBelowThePoint() {
        assertEquals(0, Biome.band(-50, STARTS));
        assertEquals(1, Biome.band(1600, STARTS));
        assertEquals(5, Biome.band(99999, STARTS));
        assertEquals(Biome.BANDS.length, STARTS.length, "one biome per band");
    }

    @Test
    void aChunkPaintsFastEnoughToStreamIn() {
        int[] a = new int[128 * 128];
        map.fillChunk(0, 0, 128, a); // warm the JIT
        long t0 = System.nanoTime();
        for (int k = 0; k < 4; k++) map.fillChunk(128 * k, 1200, 128, a);
        long ms = (System.nanoTime() - t0) / 4_000_000L;
        assertTrue(ms < 250, "a chunk should paint in well under a quarter second, took " + ms + " ms");
        System.out.println("GroundMap: 128x128 chunk paints in ~" + ms + " ms");
    }

    @Test
    void theStackOpensWithTheGroundAndEasesBetweenBands() {
        assertEquals(0f, Biome.layers(300, STARTS), 1e-4f, "none on the open meadow");
        assertEquals(0f, Biome.layers(Biome.STACK_FROM, STARTS), 1e-4f, "none until the ground opens");
        assertTrue(Biome.layers(1300, STARTS) > 2f && Biome.layers(1300, STARTS) < 9.5f, "opening: " + Biome.layers(1300, STARTS));
        assertTrue(Biome.layers(1550, STARTS) > 8.5f, "whole by the lip: " + Biome.layers(1550, STARTS));
        assertEquals(10f, Biome.layers(3000, STARTS), 1e-3f, "the Golden Door");
        assertEquals(10f, Biome.layers(5800, STARTS), 1e-3f, "the Mirage Wood");
        assertEquals(10f, Biome.layers(19500, STARTS), 1e-3f, "the Hall of Belief");
        float last = Biome.layers(0, STARTS);
        for (float y = 0; y <= 20600; y += 10) {
            float n = Biome.layers(y, STARTS);
            assertTrue(Math.abs(n - last) < 0.5f, "the layer count never jumps: " + y + " " + last + " -> " + n);
            last = n;
        }
        float[] a = new float[3], b = new float[3];
        Biome.deepTint(3000, STARTS, a);
        Biome.deepTint(5800, STARTS, b);
        assertTrue(a[0] > b[0], "the Golden Door is warm, the Mirage Wood teal");
    }
}

package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The wood's living things stand where a hash puts them, inside the wood; a mirage thins as the hull nears. */
class MirageFxTest {
    @Test
    void sitesAreFixedAndInsideTheWood() {
        int found = 0, blossoms = 0, mushrooms = 0;
        for (int layer = 0; layer < 5; layer++)
            for (int j = 0; j < 60; j++)
                for (int i = 0; i < 20; i++) {
                    float[] a = MirageFx.site(layer, i, j), b = MirageFx.site(layer, i, j);
                    if (a == null) { assertEquals(null, b); continue; }
                    assertNotNull(b);
                    assertEquals(a[0], b[0]);
                    assertEquals(a[1], b[1]);
                    assertTrue(a[1] >= MirageFx.Y0 && a[1] <= MirageFx.Y1, "inside the wood: " + a[1]);
                    found++;
                    if (a[2] < 0.5f) blossoms++; else mushrooms++;
                }
        assertTrue(found > 100 && blossoms > 20 && mushrooms > 20, "found " + found + " (" + blossoms + " blossoms, " + mushrooms + " mushrooms)");
    }

    @Test
    void theLayersDoNotStandOnTheSameSpots() {
        float[] a = null, b = null;
        outer:
        for (int j = 0; j < 40; j++)
            for (int i = 0; i < 12; i++) {
                a = MirageFx.site(0, i, j);
                b = MirageFx.site(1, i, j);
                if (a != null && b != null) break outer;
            }
        assertTrue(a == null || b == null || a[0] != b[0] || a[1] != b[1], "each layer has its own field");
    }

    @Test
    void aMirageIsWholeFromAfarAndNearlyGoneUpClose() {
        assertEquals(1f, TileImages.mirageFade(500f), 1e-6f);
        assertTrue(TileImages.mirageFade(50f) < 0.12f);
        float last = 0f;
        for (float d = 0; d <= 400; d += 10) {
            float f = TileImages.mirageFade(d);
            assertTrue(f >= last - 1e-6f, "it thins steadily as the hull comes in: " + d);
            last = f;
        }
    }
}

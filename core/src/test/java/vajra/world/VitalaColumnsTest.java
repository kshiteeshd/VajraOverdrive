package vajra.world;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The river hall has columns on its banks, and the river itself stays clear to fly. */
class VitalaColumnsTest {
    @Test
    void columnsStandOnTheBanksAndTheRiverIsClear() {
        java.util.List<float[]> cols = MapGen.pillars(MapGen.pockets());
        assertTrue(cols.size() >= 15, "the hall has its columns: " + cols.size());
        World w = World.vitala(0);
        int solid = 0;
        for (float[] c : cols) {
            if (!w.openFor(c[0], c[1], 4f)) solid++;
            assertTrue(Math.abs(c[0] - Hataki.centre(c[1])) > Hataki.half(c[1]) + 60f, "and it stands outside the river");
        }
        assertTrue(solid >= cols.size() - 2, "the columns stand (a few are cleared for a post): " + solid + " of " + cols.size());
        for (float y = Hataki.START[1] + 100f; y < Hataki.START[5] - 300f; y += 40f)
            assertTrue(w.openFor(Hataki.centre(y), y, 20f), "the river's line is clear at " + y);
    }
}

package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Flowers, grass and flames grow from DRAWN wall: none hangs in the air, none is rooted in a boulder, and what grew from the seal is gone when the seal opens. */
class RootedTest {
    @Test
    void everyPlantAndTorchIsRootedInDrawnWallInBothWorlds() {
        for (World w : new World[] {new World(), new World(0, World.CAMPAIGN)}) {
            assertTrue(w.plants.length > 100, "the land is still planted: " + w.plants.length);
            for (float[] p : w.plants) assertTrue(w.wallDrawnPx(p[6], p[7]), "a plant at " + p[0] + "," + p[1] + " hangs in the air");
            for (float[] t : w.torches) assertTrue(w.wallDrawnPx(t[3], t[4]), "a flame at " + t[0] + "," + t[1] + " hangs in the air");
        }
    }

    @Test
    void whatGrewFromTheSealGoesWhenTheSealOpens() {
        World w = new World();
        int row = w.sealRow, mid = (w.sealC0 + w.sealC1) / 2;
        float rx = w.sealMap.ox + (mid + 0.5f) * TileMap.TILE, ry = w.sealMap.oy + (row + 0.5f) * TileMap.TILE;
        float[][] more = java.util.Arrays.copyOf(w.plants, w.plants.length + 1);
        more[more.length - 1] = new float[] {rx, ry + 20f, 3, 0f, -90f, 1f, rx, ry};       // a tuft that has taken root in the seal itself
        w.plants = more;
        w.pruneRooted();
        assertEquals(more.length, w.plants.length, "while the seal stands it holds");
        for (int c = w.sealC0; c <= w.sealC1; c++) w.sealMap.set(c, row, TileMap.EMPTY);
        w.pruneRooted();
        assertTrue(w.plants.length <= more.length - 1, "and the tuft goes with it (and any real plant that grew from the seal)");
        for (float[] q : w.plants) assertFalse(q[6] == rx && q[7] == ry, "the tuft is gone");
    }
}

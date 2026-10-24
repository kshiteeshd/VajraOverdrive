package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Mirage Wood: some wall is an illusion - drawn, and nothing to a hull, a body or a round. */
class MirageTest {
    private static final float[] STARTS = {0, 1600, 4100, 7600, 12000, 17600};

    @Test
    void aMirageIsDrawnButEmptyToTheWorld() {
        World w = new World();
        int ghosts = 0, real = 0;
        for (TileMap m : w.tileMaps())
            for (int ty = 0; ty < m.rows; ty++)
                for (int tx = 0; tx < m.cols; tx++) {
                    if (m.raw(tx, ty) == TileMap.EMPTY) continue;
                    if (m.isGhost(tx, ty)) {
                        ghosts++;
                        assertEquals(TileMap.EMPTY, m.get(tx, ty), "the world sees nothing there");
                        assertFalse(m.solidPx(m.ox + (tx + 0.5f) * TileMap.TILE, m.oy + (ty + 0.5f) * TileMap.TILE), "and nothing stops a hull");
                        float y = m.oy + ty * TileMap.TILE;
                        assertTrue(y > STARTS[2] + 250f && y < STARTS[3] - 250f, "only inside the Mirage Wood: y=" + y);
                    } else real++;
                }
        assertTrue(ghosts > 200, "there are mirages: " + ghosts);
        assertTrue(real > ghosts, "most of the rock is real");
    }

    @Test
    void theGatesTheShopsAndTheCheckpointsKeepRealWalls() {
        World w = new World();
        for (TileMap m : w.tileMaps())
            for (int ty = 0; ty < m.rows; ty++)
                for (int tx = 0; tx < m.cols; tx++) {
                    if (!m.isGhost(tx, ty)) continue;
                    float x = m.ox + tx * TileMap.TILE, y = m.oy + ty * TileMap.TILE;
                    for (float[] s : w.shops) assertFalse(Math.abs(s[0] - x) < 380 && Math.abs(s[1] - y) < 380, "a shop's walls are real");
                    for (Checkpoint c : w.checkpoints) assertFalse(Math.abs(c.x - x) < 380 && Math.abs(c.y - y) < 380, "so are a checkpoint's");
                }
    }

    @Test
    void aGhostBlockCanBeFlownThrough() {
        World w = new World();
        for (TileMap m : w.tileMaps())
            for (int ty = 2; ty < m.rows - 2; ty++)
                for (int tx = 2; tx < m.cols - 2; tx++)
                    if (m.isGhost(tx, ty) && m.isGhost(tx + 1, ty) && m.isGhost(tx, ty + 1) && m.isGhost(tx - 1, ty) && m.isGhost(tx, ty - 1)) {
                        float cx = m.ox + (tx + 0.5f) * TileMap.TILE, cy = m.oy + (ty + 0.5f) * TileMap.TILE;
                        assertFalse(w.blockedPx(cx, cy), "a mirage does not stop a round");
                        float[] c = w.collide(cx - 12, cy - 12, 24, 24);
                        assertEquals(cx - 12, c[0], 1e-3f, "or push a hull");
                        return;
                    }
        throw new AssertionError("no interior mirage found to fly through");
    }

    @Test
    void theWoodHoldsInsideItsBandAndEasesAtTheEdges() {
        assertEquals(1f, Biome.bandWeight(5800, STARTS, 2), 1e-4f);
        assertEquals(0f, Biome.bandWeight(2500, STARTS, 2), 1e-4f);
        assertEquals(0f, Biome.bandWeight(9000, STARTS, 2), 1e-4f);
        assertEquals(0.5f, Biome.bandWeight(STARTS[2], STARTS, 2), 1e-3f, "half-strength on the boundary");
        float last = -1;
        for (float y = 3500; y <= 4700; y += 20) {
            float v = Biome.bandWeight(y, STARTS, 2);
            assertTrue(v >= last - 1e-5f, "it grows steadily into the band at y=" + y);
            last = v;
        }
    }
}

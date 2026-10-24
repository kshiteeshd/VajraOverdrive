package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;

/** The campaign world (docs/VITALA_PLAN.md): Atala, and Vitala on its roof - one continuous world. The cavern is one connected road from the
 *  Atala spawn through Bala's door to the Altar's sealed door, every landmark is on it, and the river is where the map says it is. */
class VitalaTest {
    private static World campaign() {
        return World.vitala(0);
    }

    @Test
    void theCampaignWorldIsAtalaWithVitalaOnItsRoof() {
        World w = campaign();
        assertTrue(w.campaign);
        assertEquals(5200f, w.width, 0.01f);
        assertEquals(Hataki.TOP, w.height, 0.01f);
        assertEquals(12, w.regionStart.length, "Bhu Loka and Atala's five, then Vitala's six");
        assertEquals(Hataki.Y0, w.regionStart[6], 0.01f);
        assertEquals(10, w.checkpoints.length);
        assertEquals(8, w.shops.length);
        assertEquals(10, w.caches.length);
        for (Checkpoint c : w.checkpoints) assertTrue(c != null, "every shrine was placed");
        assertEquals(Hataki.CX, w.ship.x, 1f);
        assertTrue(w.ship.y > Hataki.Y0 && w.ship.y < Hataki.Y0 + 200f, "this run starts in Vitala's Plunge");
        assertEquals(1, w.stage, "the second ruler is the quota's business");
        assertEquals(1, w.lokaIndex);
        assertTrue(w.cues.isEmpty(), "no opening scene replays when a run starts in Vitala");
        // the classic world is still Atala alone
        World a = new World();
        assertFalse(a.campaign);
        assertEquals(20600f, a.height, 0.01f);
        assertEquals(6, a.regionStart.length);
        // a campaign run begins at the Atala foot, with the opening to play once
        World c = new World(0, World.CAMPAIGN);
        assertEquals(0, c.stage);
        assertTrue(c.ship.y < 200f);
        assertTrue(c.cues.contains(Cue.OPENING));
    }

    @Test
    void vitalaIsTenThousandYojanasLongAndItsPocketsAreAllUsed() {
        assertEquals(10000, Hataki.yojana(Hataki.HEIGHT));
        assertEquals(0, Hataki.yojana(Hataki.Y0));
        World w = campaign();
        assertTrue(w.pockets.length >= 24, "pockets: " + w.pockets.length);
        int vault = 0, well = 0, den = 0;
        for (float[] p : w.pockets) {
            int t = (int) p[5];
            if (t == Chest.VAULT) vault++; else if (t == Chest.WELL) well++; else den++;
        }
        assertTrue(vault >= 8 && well >= 8 && den >= 8, vault + " vaults, " + well + " wells, " + den + " dens");
        int vitalaChests = 0, atalaChests = 0;
        for (Chest c : w.chests) if (c.loka == 1) vitalaChests++; else atalaChests++;
        assertEquals(vault + den, vitalaChests, "every vault and den holds a chest");
        assertTrue(atalaChests >= 6, "Atala hides its own: " + atalaChests);
        int guarded = 0;
        for (Chest c : w.chests) if (c.guards > 0) guarded++;
        assertEquals(den, guarded, "a den's chest is guarded");
    }

    /** Flood-fill the open air in 16 px steps from the very foot of Atala: Bala's door, every checkpoint, shop and cache of BOTH lokas, the chests
     *  and the foot of Bhairava's sealed door must be reached. */
    @Test
    void everyLandmarkOfBothLokasIsReachableFromTheAtalaSpawn() {
        World w = campaign();
        int step = 16, cols = (int) (w.width / step), rows = (int) (w.height / step);
        boolean[] seen = new boolean[cols * rows];
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        int sx = (int) (w.width / 2 / step), sy = (int) (90f / step);
        q.add(new int[] {sx, sy});
        seen[sy * cols + sx] = true;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] e : d) {
                int nx = c[0] + e[0], ny = c[1] + e[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows || seen[ny * cols + nx]) continue;
                if (!w.openFor(nx * step + step / 2f, ny * step + step / 2f, 13f)) continue;   // (a hull-sized box: the necks of Atala are narrow)
                seen[ny * cols + nx] = true;
                q.add(new int[] {nx, ny});
            }
        }
        dump(w, seen, cols, rows, step);
        for (int i = 0; i < w.checkpoints.length; i++)
            assertTrue(reached(seen, cols, step, w.checkpoints[i].x, w.checkpoints[i].y), "checkpoint " + i);
        for (int i = 0; i < w.shops.length; i++)
            assertTrue(reached(seen, cols, step, w.shops[i][0], w.shops[i][1] - 96f), "the bay under shop " + i);
        for (int i = 0; i < w.caches.length; i++)
            assertTrue(reached(seen, cols, step, w.caches[i][0], w.caches[i][1]), "cache " + i);
        for (int i = 0; i < w.chests.size(); i++)
            assertTrue(reached(seen, cols, step, w.chests.get(i).x, w.chests.get(i).y), "chest " + i + " of loka " + w.chests.get(i).loka);
        assertTrue(reached(seen, cols, step, Hataki.ALTAR_X, Hataki.ALTAR_Y), "the Altar's heart");
        assertTrue(reached(seen, cols, step, Hataki.ALTAR_X, Hataki.HEIGHT - 400f), "the foot of the sealed door");
    }

    private static boolean reached(boolean[] seen, int cols, int step, float x, float y) {
        int cx = (int) (x / step), cy = (int) (y / step);
        for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) if (seen[(cy + dy) * cols + cx + dx]) return true;
        return false;
    }

    /** A picture of the whole world (rock grey, air black, the river gold, reached air blue) under build/ for a look. */
    private static void dump(World w, boolean[] seen, int cols, int rows, int step) {
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++) {
                float wx = x * step + step / 2f, wy = y * step + step / 2f;
                int rgb = w.blockedPx(wx, wy) ? 0x666666 : Hataki.on(wx, wy) ? 0xE0A020 : seen[y * cols + x] ? 0x203060 : 0x000000;
                img.setRGB(x, rows - 1 - y, rgb);
            }
        try {
            File f = new File("build/campaign_map.png");
            f.getParentFile().mkdirs();
            javax.imageio.ImageIO.write(img, "png", f);
        } catch (Exception ignored) {
        }
    }

    @Test
    void theRiverIsGoldWhereTheMapSaysAndPinnedToTheAxisAtEveryGate() {
        for (int i = 1; i < Hataki.START.length; i++)
            assertEquals(Hataki.CX, Hataki.centre(Hataki.START[i]), 1f, "gate " + i);
        float y = Hataki.START[2] + 2000f;
        assertTrue(Hataki.on(Hataki.centre(y), y));
        assertTrue(!Hataki.on(Hataki.centre(y) + 500f, y));
        assertTrue(!Hataki.on(Hataki.CX, 800f), "Atala has no river");
        assertTrue(!Hataki.on(Hataki.CX, Hataki.Y0 + 800f), "the Plunge is a shaft: no river");
        float[] f = new float[2];
        float ry = Hataki.START[4] + 2000f;
        Hataki.flow(Hataki.centre(ry), ry, f);
        assertTrue(f[1] > 100f, "the rapids run up the map: " + f[1]);
        float py = Hataki.START[1] + 800f;
        Hataki.flow(Hataki.centre(py) + 1200f, py, f);
        assertEquals(0f, f[1], 1e-4f);
        assertEquals(-1, Hataki.band(5000f), "below Vitala is Atala");
        assertEquals(0, Hataki.band(Hataki.Y0 + 10f));
        assertEquals(5, Hataki.band(Hataki.HEIGHT - 10f));
    }
}

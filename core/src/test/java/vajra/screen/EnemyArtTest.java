package vajra.screen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;

/** The loader probes idle_0, idle_1, ... until a file is missing, so the frames on disk have to be
 *  contiguous — and the animation has to reach every one of them. */
class EnemyArtTest {
    @Test
    void everyEnemyShipsContiguousIdleFrames() {
        File root = enemyRoot();
        assertNotNull(root, "no assets/atala/enemies from " + new File(".").getAbsolutePath());
        for (Enemy.Kind k : Enemy.Kind.values()) {
            if (sheetOf(k) != null) continue; // a kind with a 64x64 sheet needs no idle_N folder
            File dir = new File(root, k.name().toLowerCase());
            assertTrue(dir.isDirectory(), "no folder for " + k);
            File[] idle = dir.listFiles((d, n) -> n.startsWith("idle_") && n.endsWith(".png"));
            assertNotNull(idle, "cannot list " + dir);
            int contiguous = 0;
            while (new File(dir, "idle_" + contiguous + ".png").isFile()) contiguous++;
            assertTrue(contiguous >= 1, k + " has no idle_0.png");
            assertEquals(idle.length, contiguous,
                    k + " has a gap in idle_N — the loader would stop early and drop frames");
        }
    }

    @Test
    void animationReachesEveryFrame() {
        for (int n = 1; n <= 6; n++) {
            Set<Integer> seen = new HashSet<>();
            for (int t = 0; t < 400; t++) {
                int f = EnemyImages.frame(t / 20f, 0f, n);
                assertTrue(f >= 0 && f < n, "frame out of range for " + n + " frames");
                seen.add(f);
                seen.add(EnemyImages.frame(t / 20f, 2.5f, n));
            }
            assertEquals(n, seen.size(), "animation must touch all " + n + " frames");
        }
    }

    /** The keepers and the six soldiers wear 64x64 sheets: idle, rage, hit, death, ranked, then three action clips each for a plain unit, a leader and an elite. */
    @Test
    void everySheetIsSixteenByFourCellsOfSixtyFour() throws Exception {
        int sheets = 0;
        for (Enemy.Kind k : Enemy.Kind.values()) {
            File f = sheetOf(k);
            if (f == null) continue;
            sheets++;
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(f);
            assertEquals(1024, im.getWidth(), k + " sheet width");
            assertEquals(832, im.getHeight(), k + " sheet height (the usual four rows and the nine action rows)");
            for (int col = 0; col < 16; col++) // every cell the game reads must hold a drawn body
                for (int row = 0; row < 4; row++) {
                    if (row == 1 && col >= 7) continue; // hit x2 + death x6 fill the row's first eight; the last may be nearly empty
                    int px = 0;
                    for (int y = 0; y < 64; y++)
                        for (int x = 0; x < 64; x++)
                            if ((im.getRGB(col * 64 + x, row * 64 + y) >>> 24) > 0) px++;
                    assertTrue(px > 0, k + " cell " + col + "," + row + " is empty");
                }
        }
        assertTrue(sheets >= 9, "the nine Atala archetypes ship sheets (found " + sheets + ")");
    }

    /** The idle is eight distinct frames, not a body shifting a pixel: no two neighbours may match. */
    @Test
    void theIdleLoopMovesOnEveryFrame() throws Exception {
        for (Enemy.Kind k : Enemy.Kind.values()) {
            File f = sheetOf(k);
            if (f == null) continue;
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(f);
            for (int row : new int[] {0, 2, 3})
                for (int half = 0; half < 2; half++)
                    for (int t = 0; t < EnemyImages.IDLE_N; t++) {
                        int a = half * EnemyImages.IDLE_N + t, b = half * EnemyImages.IDLE_N + (t + 1) % EnemyImages.IDLE_N, diff = 0;
                        for (int y = 0; y < 64; y++)
                            for (int x = 0; x < 64; x++)
                                if (im.getRGB(a * 64 + x, row * 64 + y) != im.getRGB(b * 64 + x, row * 64 + y)) diff++;
                        assertTrue(diff >= 6, k + " row " + row + " frames " + a + "->" + b + " barely change (" + diff + " px)");
                    }
        }
    }

    @Test
    void theKeepersAreEnemiesWithAJob() {
        assertTrue(Enemy.Kind.DVARAPALA.keeper() && Enemy.Kind.DIPAKA.keeper() && Enemy.Kind.VANIJA.keeper());
        assertFalse(Enemy.Kind.LANCE.keeper() || Enemy.Kind.BALA.keeper());
        assertTrue(Enemy.Kind.BALA.ordinal() < Enemy.Kind.DVARAPALA.ordinal(), "older ordinals never move");
    }

    @Test
    void theSheetCellFollowsTheUnitsState() {
        Enemy e = new Enemy(Enemy.Kind.LANCE, 0, 0);
        int[] idle = EnemyImages.cell(e, 0);
        assertEquals(0, idle[0]);
        assertTrue(idle[1] < EnemyImages.IDLE_N, "plain idle sits in the left half");
        e.rank = 1;
        assertEquals(2, EnemyImages.cell(e, 0)[0], "a leader wears the crest row");
        e.elite = true;
        assertEquals(3, EnemyImages.cell(e, 0)[0], "an elite wears the vents row");
        e.hp = 1; // nearly dead: rage
        assertTrue(EnemyImages.cell(e, 0)[1] >= EnemyImages.IDLE_N, "rage plays the right half");
        e.hitFlash = 0.1f;
        assertEquals(1, EnemyImages.cell(e, 0)[0], "a fresh hit shows the hit row");
        assertEquals(0, EnemyImages.cell(e, 0)[1]);
        assertEquals(5, EnemyImages.deathFrame(0.59f, 0.6f));
        assertEquals(0, EnemyImages.deathFrame(0f, 0.6f));
    }

    private static File sheetOf(Enemy.Kind k) {
        for (String p : new String[] { "assets/enemies/atala/", "../assets/enemies/atala/" }) {
            File f = new File(p + k.name().toLowerCase() + ".png");
            if (f.isFile() && k != Enemy.Kind.BALA) return f;                  // (the ruler wears his own 128 px sheet, not a 64 px unit sheet)
        }
        return null;
    }

    private static File enemyRoot() {
        for (String p : new String[] { "assets/atala/enemies", "../assets/atala/enemies" }) {
            File d = new File(p);
            if (d.isDirectory()) return d;
        }
        return null;
    }

    /** Bala's own sheet: 8 x 8 cells of 128, every cell of the idle, tell, release, cast and arrival rows drawn. */
    @Test
    void balaWearsA128SheetWithAClipForEachScene() throws Exception {
        File f = new File("assets/enemies/atala/bala.png");
        if (!f.isFile()) f = new File("../assets/enemies/atala/bala.png");
        java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(f);
        assertEquals(1024, im.getWidth());
        assertEquals(1024, im.getHeight());
        for (int row : new int[] {0, 1, 2, 4, 5, 6, 7})
            for (int col = 0; col < 8; col++) {
                int px = 0;
                for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) if ((im.getRGB(col * 128 + x, row * 128 + y) >>> 24) > 40) px++;
                assertTrue(px > 800, "row " + row + " col " + col + " holds a body: " + px);
            }
    }
}

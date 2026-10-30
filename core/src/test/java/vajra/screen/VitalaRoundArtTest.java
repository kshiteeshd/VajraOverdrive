package vajra.screen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;

/** Vitala's creatures are round and alive: nine sheets of thirteen rows (idle, rage, hit and death, leader, elite, and three action clips each of the three), and each unit
 *  does its own actions at its own moments. */
class VitalaRoundArtTest {
    private static final String[] KINDS = {"vakra", "mayavi", "bhavi", "lance", "auris", "hataka", "dvarapala", "dipaka", "vanija"};

    private static File sheet(String kind) { return new File("../assets/enemies/vitala/" + kind + ".png"); }

    @Test
    void everyCreatureHasThirteenRowsAndEveryCellItReadsIsDrawn() throws Exception {
        for (String k : KINDS) {
            java.awt.image.BufferedImage im = ImageIO.read(sheet(k));
            assertEquals(1024, im.getWidth(), k);
            assertEquals(13 * 64, im.getHeight(), k);
            for (int row = 0; row < 13; row++)
                for (int col = 0; col < 16; col++) {
                    boolean idleRow = row <= 3;
                    if (idleRow && row == 1 && col >= 7) continue;
                    if (row >= 4 && false) continue;
                    int px = 0;
                    for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if ((im.getRGB(col * 64 + x, row * 64 + y) >>> 24) > 0) px++;
                    assertTrue(px > 20, k + " cell " + col + "," + row + " is empty (" + px + " px)");
                }
        }
    }

    @Test
    void theActionClipsReallyMove() throws Exception {
        for (String k : KINDS) {
            java.awt.image.BufferedImage im = ImageIO.read(sheet(k));
            for (int row = 4; row < 7; row++) {
                int changed = 0;
                for (int col = 1; col < 16; col++) {
                    int diff = 0;
                    for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if (im.getRGB(col * 64 + x, row * 64 + y) != im.getRGB((col - 1) * 64 + x, row * 64 + y)) diff++;
                    if (diff > 8) changed++;
                }
                assertTrue(changed >= 12, k + " action clip " + (row - 4) + " is a still (" + changed + " of 15 frames differ)");
            }
        }
    }

    @Test
    void eachUnitDoesItsOwnActionsAtItsOwnMoments() {
        Set<Integer> clips = new HashSet<>();
        int acting = 0, total = 0;
        Enemy a = new Enemy(Enemy.Kind.VAKRA, 100, 100), b = new Enemy(Enemy.Kind.VAKRA, 900, 400);
        a.phase = 1.3f;
        b.phase = 4.1f;
        int together = 0;
        for (float t = 0f; t < 240f; t += 0.05f) {
            float[] x = EnemyImages.action(a, t), y = EnemyImages.action(b, t);
            total++;
            if (x[0] >= 0f) { acting++; clips.add((int) x[0]); assertTrue(x[1] >= 0f && x[1] <= 1f); }
            if (x[0] >= 0f && y[0] >= 0f) together++;
        }
        assertEquals(3, clips.size(), "all three clips are done in time");
        assertTrue(acting > total * 0.05 && acting < total * 0.35, "a unit mostly idles and acts now and then: " + acting + " of " + total);
        assertTrue(together < acting * 0.5, "two units are not in step: " + together + " together of " + acting);
    }

    @Test
    void aCalmUnitActsButARagingOrHitOneDoesNot() {
        Enemy e = new Enemy(Enemy.Kind.AURIS, 300, 300);
        e.vitala = true;
        e.phase = 2.2f;
        boolean saw = false;
        for (float t = 0f; t < 60f && !saw; t += 0.05f) {
            int[] c = EnemyImages.cellV(e, t);
            if (c[0] >= 4) { saw = true; assertTrue(c[0] <= 6 && c[1] >= 0 && c[1] < 16, "a plain unit's clip rows are 4-6"); }
        }
        assertTrue(saw, "a calm unit does an action");
        e.hitFlash = 0.1f;
        for (float t = 0f; t < 60f; t += 0.05f) assertTrue(EnemyImages.cellV(e, t)[0] <= 1, "a hit unit shows the hit frames");
        e.hitFlash = 0f;
        e.hp = e.maxHp * 0.1f;
        for (float t = 0f; t < 60f; t += 0.05f) assertTrue(EnemyImages.cellV(e, t)[0] < 4, "a wounded, raging unit does not stop to idle");
        e.hp = e.maxHp;
        e.rank = 1;
        for (float t = 0f; t < 60f; t += 0.05f) { int[] c = EnemyImages.cellV(e, t); if (c[0] >= 4) assertTrue(c[0] >= 7 && c[0] <= 9, "a leader's clips are rows 7-9"); }
        e.rank = 0;
        e.elite = true;
        for (float t = 0f; t < 60f; t += 0.05f) { int[] c = EnemyImages.cellV(e, t); if (c[0] >= 4) assertTrue(c[0] >= 10 && c[0] <= 12, "an elite's clips are rows 10-12"); }
    }
}

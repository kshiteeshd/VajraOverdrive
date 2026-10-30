package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

/** The story flow end to end, headless, and the seams between the newer systems: the whole campaign as it stands (Bala, the door, Vitala, Bhairava), the doors of Bala's third astra, a save at the worst
 *  moment, a kill in the middle of his astra, a mantra half-said when the hull dies, and a long random flight that must not break anything. */
class StoryFlowTest {
    private static void run(World w, float secs) {
        for (int i = 0; i < (int) (secs * 60); i++) w.update(1 / 60f, ShipControls.NONE);
    }

    private static Enemy summon(World w) {
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        assertNotNull(w.boss);
        return w.boss;
    }

    @Test
    void theWholeCampaignAsItStandsFromBalaToBhairavaAndTheHall() {
        World w = new World(0, World.CAMPAIGN);
        assertEquals(java.util.List.of(Cue.OPENING), new java.util.ArrayList<>(w.cues));
        w.cues.clear();
        Enemy bala = summon(w);
        bala.hurt(99999, w);
        assertTrue(bala.yielding && w.cues.contains(Cue.BALA_FALLS));
        w.bossReleases();
        w.ship.x = World.PIT_X;
        w.ship.y = World.PIT_Y + 300;
        run(w, 6);
        assertTrue(w.hangar.pieces[0], "the first piece is held");
        assertEquals(1, w.stage);
        assertEquals(1f, w.veilClear[0], 1e-3f, "the cloud over Vitala's road has parted");
        assertEquals(0f, w.veilClear[1], 1e-3f, "and the one over the way beyond Bhairava has not");
        w.ship.x = Hataki.ALTAR_X;
        w.ship.y = Hataki.ALTAR_Y - 500f;
        run(w, 1);
        assertEquals(1, w.lokaIndex);
        Enemy bhairava = summon(w);
        for (int i = 0; i < 12000 && !bhairava.yielding; i++) {              // he is brought down through his four gated phases, never in one blow
            if (!bhairava.bossInvuln) bhairava.hurt(99999, w);
            w.ship.invuln = 1e6f;
            w.ship.x = Hataki.ALTAR_X;
            w.ship.y = Hataki.ALTAR_Y - 500f;
            w.update(1 / 60f, ShipControls.NONE);
        }
        assertTrue(bhairava.yielding, "the fight ended in a yield");
        assertTrue(w.cues.contains(Cue.BHAIRAVA_FALLS));
        w.bossReleases();
        run(w, 8);
        assertTrue(w.hangar.pieces[0] && w.hangar.pieces[1] && !w.hangar.pieces[2], "two pieces of seven");
        assertFalse(w.won || w.runEnded() || w.vajraDone, "the campaign goes on into Sutala's hall: there is no ending yet");
        String json = RunSave.capture(w);
        World back = new World(0, World.CAMPAIGN);
        assertTrue(RunSave.restore(back, json));
        run(back, 1);
        assertTrue(back.hangar.pieces[0] && back.hangar.pieces[1]);
        assertEquals(1f, back.veilClear[0], 1e-3f);
        assertEquals(1f, back.veilClear[1], 1e-3f);
    }

    @Test
    void everyDoorOfBalasThirdAstraIsOpenGroundConnectedToTheStartSoTheWayBackIsAlwaysThere() {
        for (int seed = 0; seed < 3; seed++) {
            World w = new World(seed, World.CAMPAIGN);
            Enemy bala = new Enemy(Enemy.Kind.BALA, World.PIT_X, World.PIT_Y);
            bala.boss = true;
            bala.bossCasts = 2;
            w.enemies.add(bala);
            w.startMaya(bala, Maya.DOORS);
            int step = 32, cols = (int) (w.width / step) + 1, rows = (int) (w.height / step) + 1;
            boolean[][] seen = new boolean[cols][rows];
            ArrayDeque<int[]> q = new ArrayDeque<>();
            seen[(int) (w.ship.x / step)][(int) (w.ship.y / step)] = true;
            q.add(new int[] {(int) (w.ship.x / step), (int) (w.ship.y / step)});
            int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int k = 0; k < 4; k++) {
                    int nc = c[0] + dx[k], nr = c[1] + dy[k];
                    if (nc < 0 || nr < 0 || nc >= cols || nr >= rows || seen[nc][nr] || !w.openFor(nc * step + 16f, nr * step + 16f, 14f)) continue;
                    seen[nc][nr] = true;
                    q.add(new int[] {nc, nr});
                }
            }
            assertTrue(seen[(int) (World.PIT_X / step)][(int) (World.PIT_Y / step)], "Bala's arena is reachable from the start (seed " + seed + ")");
            for (int i = 0; i < Maya.DOOR_COUNT; i++)
                assertTrue(seen[(int) (w.maya.doorX[i] / step)][(int) (w.maya.doorY[i] / step)], "door " + i + " (seed " + seed + ") leads somewhere the hull can fly back from");
        }
    }

    @Test
    void killingBalaInTheMiddleOfHisAstraEndsTheAstraAndNoDoorCarriesTheHullAway() {
        World w = new World(0, World.CAMPAIGN);
        w.cues.clear();
        Enemy b = summon(w);
        w.ship.x = World.PIT_X;
        w.ship.y = World.PIT_Y - 400f;
        b.hp = b.maxHp * 0.2f;
        run(w, 0.5f);
        assertTrue(w.maya.active, "his astra is running");
        run(w, 3f);
        b.bossInvuln = false;
        float y0 = w.ship.y;
        b.hurt(99999, w);
        assertFalse(w.maya.active, "it ends with his yielding");
        w.bossReleases();
        run(w, 25f);
        assertEquals(y0, w.ship.y, 200f, "nothing carried the hull away");
    }

    @Test
    void aMantraHalfSaidDiesWithTheHullAndCannotBeLoosedFromTheWreck() {
        World w = new World(0, World.CAMPAIGN);
        w.hangar.owned[0][1] = true;
        w.hangar.shards[Astra.AGNI] = 5;
        w.astraSel = Astra.AGNI;
        assertTrue(w.astraBegin());
        w.ship.hp = 1;
        w.ship.invuln = 0;
        w.ship.damage(999, w);
        assertFalse(w.astraInvoking, "the invocation ended with the hull");
        run(w, 1.5f);
        assertFalse(w.astraLoose(), "and there is nothing to loose");
        assertEquals(AstraFlow.NONE, w.flow.kind);
    }

    @Test
    void aSaveInTheSecondThePieceFliesDoesNotLoseThePiece() {
        World w = new World(0, World.CAMPAIGN);
        summon(w).hurt(99999, w);
        w.bossReleases();
        assertFalse(w.hangar.pieces[0], "not yet taken");
        World back = new World(0, World.CAMPAIGN);
        assertTrue(RunSave.restore(back, RunSave.capture(w)));
        assertEquals(1, back.stage, "Bala's door is open in the saved run");
        assertTrue(back.hangar.pieces[0], "so the piece is the pilot's: it was on its way");
    }

    @Test
    void aLongRandomFlightThroughEveryBandBreaksNothingAndStaysCheap() {
        World w = new World(0, World.CAMPAIGN);
        w.cues.clear();
        java.util.Random r = new java.util.Random(5);
        float[] ys = {300, 5000, 10000, 15000, 19000, Hataki.Y0 + 800, Hataki.Y0 + 12000, Hataki.Y0 + 20000};
        int maxFx = 0, maxBullets = 0;
        long t0 = System.nanoTime();
        int frames = 0;
        for (float y : ys) {
            w.ship.x = 2600;
            w.ship.y = y;
            for (int i = 0; i < 60 * 15; i++) {
                final float tx = r.nextFloat() * 2 - 1, ty = r.nextFloat() * 2 - 1;
                final boolean fire = r.nextInt(3) > 0;
                w.update(1 / 60f, new ShipControls() {
                    public float thrustY() { return ty; }
                    public float thrustX() { return tx; }
                    public float rotate() { return 0.3f; }
                    public boolean fire() { return fire; }
                    public boolean boost() { return false; }
                    public boolean shield() { return false; }
                    public boolean mouseAim() { return false; }
                    public float aimX() { return 0; }
                    public float aimY() { return 0; }
                });
                w.cues.clear();
                frames++;
                maxFx = Math.max(maxFx, w.effects.size());
                maxBullets = Math.max(maxBullets, w.bullets.size());
                assertFalse(Float.isNaN(w.ship.x) || Float.isNaN(w.ship.y) || Float.isNaN(w.ship.hp), "the hull stays a number");
            }
        }
        double ms = (System.nanoTime() - t0) / 1e6 / frames;
        assertTrue(maxFx < 3000 && maxBullets < 1500, "nothing leaks: " + maxFx + " effects, " + maxBullets + " bullets");
        assertTrue(ms < 8.0, "the simulation is cheap: " + ms + " ms a frame");
    }
}

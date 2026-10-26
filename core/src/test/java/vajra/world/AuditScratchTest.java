package vajra.world;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

/** SCRATCH: an audit probe that prints what it finds. */
class AuditScratchTest {
    static boolean[][] reach(World w, float sx, float sy, int step) {
        int cols = (int) (w.width / step) + 1, rows = (int) (w.height / step) + 1;
        boolean[][] seen = new boolean[cols][rows];
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        int c0 = (int) (sx / step), r0 = (int) (sy / step);
        seen[c0][r0] = true;
        q.add(new int[] {c0, r0});
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int k = 0; k < 4; k++) {
                int nc = c[0] + dx[k], nr = c[1] + dy[k];
                if (nc < 0 || nr < 0 || nc >= cols || nr >= rows || seen[nc][nr]) continue;
                if (!w.openFor(nc * step + step / 2f, nr * step + step / 2f, 14f)) continue;
                seen[nc][nr] = true;
                q.add(new int[] {nc, nr});
            }
        }
        return seen;
    }

    @Test
    void doors() {
        int bad = 0, total = 0, farBad = 0;
        for (long seed = 0; seed < 4; seed++) {
            World w = new World((int) seed, World.CAMPAIGN);
            Enemy fake = new Enemy(Enemy.Kind.BALA, World.PIT_X, World.PIT_Y);
            fake.boss = true;
            w.enemies.add(fake);
            fake.bossCasts = 2;
            w.startMaya(fake, Maya.DOORS);
            boolean[][] r = reach(w, w.ship.x, w.ship.y, 32);
            int pitC = (int) (World.PIT_X / 32), pitR = (int) (World.PIT_Y / 32);
            System.out.println("AUDIT seed " + seed + " spawn reaches pit=" + r[pitC][pitR] + " shipStart=" + w.ship.x + "," + w.ship.y);
            for (int i = 0; i < Maya.DOOR_COUNT; i++) {
                total++;
                int c = (int) (w.maya.doorX[i] / 32), rr = (int) (w.maya.doorY[i] / 32);
                if (!r[c][rr]) { bad++; System.out.println("AUDIT   door " + i + " at " + w.maya.doorX[i] + "," + w.maya.doorY[i] + " is NOT connected to the spawn"); }
            }
        }
        System.out.println("AUDIT doors unreachable: " + bad + " of " + total);
    }

    @Test
    void releaseLokaAndPieceVariant() {
        World w = new World(0, World.CAMPAIGN);
        System.out.println("AUDIT campaign lokaIndex at start=" + w.lokaIndex + " stage=" + w.stage + " cues=" + w.cues);
        // Atala: kill Bala
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        Enemy bala = w.boss;
        System.out.println("AUDIT bala spawned=" + (bala != null) + " cues after spawn=" + w.cues);
        w.cues.clear();
        bala.hurt(99999, w);
        System.out.println("AUDIT after yield cues=" + w.cues + " maya.active=" + w.maya.active);
        w.bossReleases();
        System.out.println("AUDIT after release stage=" + w.stage + " lokaIndex=" + w.lokaIndex + " pickups=" + w.pickups.size());
        for (Pickup p : w.pickups) if (p.type == Pickup.PIECE) System.out.println("AUDIT piece variant=" + p.variant);
        // fly the ship up into Vitala and let the world notice
        w.ship.x = World.PIT_X;
        w.ship.y = Hataki.Y0 + 600;
        for (int i = 0; i < 120; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT in Vitala lokaIndex=" + w.lokaIndex + " stage=" + w.stage + " hangar.loka=" + w.hangar.loka + " pieces0=" + w.hangar.pieces[0] + " cues=" + w.cues);
    }

    @Test
    void yieldDuringMaya() {
        World w = new World(0, World.CAMPAIGN);
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        Enemy b = w.boss;
        w.ship.x = World.PIT_X;
        w.ship.y = World.PIT_Y - 400;
        w.cues.clear();
        b.hp = b.maxHp * 0.2f;
        for (int i = 0; i < 30; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT bala at 20%: maya.active=" + w.maya.active + " mode=" + w.maya.mode + " casts=" + b.bossCasts + " phase=" + b.bossPhase);
        // the pilot kills him in the middle of the astra
        for (int i = 0; i < 60 * 3; i++) w.update(1 / 60f, ShipControls.NONE);
        b.bossInvuln = false;
        b.hurt(99999, w);
        System.out.println("AUDIT killed during maya: maya.active=" + w.maya.active + " yielding=" + b.yielding);
        w.bossReleases();
        for (int i = 0; i < 60 * 25; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT 25 s later: maya.active=" + w.maya.active + " ship at " + w.ship.x + "," + w.ship.y);
    }

    @Test
    void astraWhileDying() {
        World w = new World(0, World.CAMPAIGN);
        w.hangar.shards[Astra.AGNI] = 5;
        w.astraSel = Astra.AGNI;
        w.astraBegin();
        System.out.println("AUDIT invoking=" + w.astraInvoking);
        w.ship.hp = 1;
        w.ship.invuln = 0;
        w.ship.damage(999, w);
        for (int i = 0; i < 90; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT while dying: invoking still=" + w.astraInvoking + " charge=" + w.astraCharge + " hidden=" + w.death.hidden());
        boolean loosed = w.astraLoose();
        System.out.println("AUDIT loose from a dead hull -> " + loosed + " flow.kind=" + w.flow.kind);
    }

    @Test
    void saveLosesPiece() {
        World w = new World(0, World.CAMPAIGN);
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        w.boss.hurt(99999, w);
        w.bossReleases();
        String json = RunSave.capture(w);              // an autosave in the second the piece is flying
        World w2 = new World(0, World.CAMPAIGN);
        RunSave.restore(w2, json);
        System.out.println("AUDIT saved while the piece flew: stage=" + w2.stage + " pieces0=" + w2.hangar.pieces[0] + " pickups=" + w2.pickups.size());
    }
}

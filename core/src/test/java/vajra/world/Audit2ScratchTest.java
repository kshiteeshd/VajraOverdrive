package vajra.world;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

class Audit2ScratchTest {
    @Test
    void wholeCampaign() {
        World w = new World(0, World.CAMPAIGN);
        w.cues.clear();
        // Bala
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        w.boss.hurt(99999, w);
        w.bossReleases();
        w.ship.x = World.PIT_X; w.ship.y = World.PIT_Y + 300;
        for (int i = 0; i < 60 * 6; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT after Bala: pieces=" + java.util.Arrays.toString(w.hangar.pieces) + " veil=" + w.veilClear[0] + "," + w.veilClear[1] + " stage=" + w.stage + " cues=" + w.cues);
        w.cues.clear();
        // Vitala: Bhairava
        w.ship.x = Hataki.ALTAR_X; w.ship.y = Hataki.ALTAR_Y - 500;
        for (int i = 0; i < 60; i++) w.update(1 / 60f, ShipControls.NONE);
        for (int k = 0; k < w.quota.length; k++) w.kills[k] = w.quota[k];
        w.trySpawnBoss();
        Enemy b = w.boss;
        System.out.println("AUDIT bhairava=" + (b != null) + " lokaIndex=" + w.lokaIndex + " stage=" + w.stage);
        b.hurt(99999, w);
        System.out.println("AUDIT bhairava yielded cues=" + w.cues + " won=" + w.won + " victoryT=" + w.victoryT);
        w.bossReleases();
        for (int i = 0; i < 60 * 8; i++) w.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT after Bhairava: pieces=" + java.util.Arrays.toString(w.hangar.pieces) + " veil1=" + w.veilClear[1] + " won=" + w.won + " runEnded=" + w.runEnded() + " vajraDone=" + w.vajraDone);
        // save and restore after both
        String json = RunSave.capture(w);
        World w2 = new World(0, World.CAMPAIGN);
        boolean ok = RunSave.restore(w2, json);
        System.out.println("AUDIT restore ok=" + ok + " stage=" + w2.stage + " pieces=" + java.util.Arrays.toString(w2.hangar.pieces) + " medkits=" + w2.medkits + " veil=" + w2.veilClear[0] + "," + w2.veilClear[1]);
        w2.update(1 / 60f, ShipControls.NONE);
        System.out.println("AUDIT after one step veil=" + w2.veilClear[0] + "," + w2.veilClear[1] + " bossDead=" + w2.bossDead);
    }
}

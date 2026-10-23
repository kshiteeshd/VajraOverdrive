package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.profile.Profile;

/** CONTINUE's record: a run is captured, written as text, and poured over a freshly built world. */
class RunSaveTest {
    private static World played() {
        World w = new World();
        // fly a bit: hull moved and hurt, a purse, a bought hull, a rank, score, kills
        w.ship.x = 1234;
        w.ship.y = 5678;
        w.ship.facing = 1.1f;
        w.ship.hp = w.ship.maxHp() * 0.5f;
        w.lives = 2;
        w.hangar.coins = 777;
        w.hangar.owned[1][0] = true;
        w.hangar.weaponOwned[1] = true;
        w.hangar.weaponRank[1] = 1;
        w.hangar.shards[2] = 4;
        w.hangar.pieces[0] = true;
        w.score.points = 54_321;
        w.kills[0] = 7;
        w.checkpoints[0].collected = true;
        w.respawnCp = w.checkpoints[0];
        w.wreckTaken[2] = true;
        w.regionScored()[2] = true;
        // a few foes fall
        for (int i = 0; i < 5; i++) w.enemies.get(i).dead = true;
        w.cues.clear();
        return w;
    }

    @Test
    void aRunSurvivesTheRoundTrip() {
        World a = played();
        String text = RunSave.capture(a);

        World b = new World();
        assertTrue(RunSave.restore(b, text));

        assertEquals(1234, b.ship.x, 0.01f);
        assertEquals(5678, b.ship.y, 0.01f);
        assertEquals(1.1f, b.ship.facing, 1e-4f);
        assertEquals(a.ship.maxHp() * 0.5f, b.ship.hp, 0.01f);
        assertEquals(2, b.lives);
        assertEquals(777, b.hangar.coins);
        assertTrue(b.hangar.owned[1][0]);
        assertTrue(b.hangar.weaponOwned[1]);
        assertEquals(1, b.hangar.weaponRank[1]);
        assertEquals(4, b.hangar.shards[2]);
        assertTrue(b.hangar.pieces[0]);
        assertEquals(54_321, b.score.points);
        assertEquals(7, b.kills[0]);
        assertTrue(b.checkpoints[0].collected);
        assertEquals(b.checkpoints[0], b.respawnCp);
        assertTrue(b.wreckTaken[2]);
        assertTrue(b.regionScored()[2]);
    }

    @Test
    void theFallenStayFallenAndTheLivingStayPut() {
        World a = played();
        int before = a.enemies.size();
        int fallen = 0;
        for (Enemy e : a.enemies) if (e.dead && e.netId < a.garrison) fallen++;
        assertEquals(5, fallen);

        World b = new World();
        int built = b.enemies.size();
        assertEquals(before, built, "the world is built the same way every time");
        RunSave.restore(b, RunSave.capture(a));
        assertEquals(built - 5, b.enemies.size(), "five struck off the roster");
        for (Enemy e : b.enemies) assertFalse(e.dead);
    }

    @Test
    void theOpeningAndTheMeetingsDoNotReplay() {
        World a = new World(); // a fresh world queues the opening
        assertFalse(a.cues.isEmpty());
        for (Enemy v : a.voices) v.voiceMet = true;
        World b = new World();
        RunSave.restore(b, RunSave.capture(a));
        assertTrue(b.cues.isEmpty(), "no opening cutscene on continue");
        for (Enemy v : b.voices) assertTrue(v.voiceMet, "a person already met is not introduced again");
    }

    @Test
    void anOpenBountyIsRaisedAgain() {
        World a = new World();
        a.missions.openBounty();
        assertEquals(Missions.OPEN, a.missions.bounty);
        World b = new World();
        RunSave.restore(b, RunSave.capture(a));
        assertEquals(Missions.OPEN, b.missions.bounty);
    }

    @Test
    void garbageIsRefusedAndFliesNothingBroken() {
        World w = new World();
        float x = w.ship.x;
        assertFalse(RunSave.restore(w, "not json at all {{{"));
        assertFalse(RunSave.restore(w, "{version:99}"));
        assertEquals(x, w.ship.x, 0.001f);
    }

    @Test
    void aPilotCarriesTheSaveThroughTheirProfileFile() {
        Profile p = new Profile();
        assertFalse(p.hasRun());
        p.run = RunSave.capture(played());
        String json = Profile.serialize(p);

        Profile q = new Profile();
        Profile.deserialize(q, json);
        assertTrue(q.hasRun());
        assertNotNull(q.run);
        World w = new World();
        assertTrue(RunSave.restore(w, q.run));
        assertEquals(777, w.hangar.coins);

        q.run = null;
        assertFalse(q.hasRun());
        assertNull(q.run);
    }
}

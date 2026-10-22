package vajra.world;

import vajra.enemy.Enemy;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** THE BOUNTY — the first mission on the board. The card goes up only when the Scout's beacon
 *  answers and only if he was met; the rogue is a real roster member once placed; the purse
 *  pays once, the board closes, and reopening never seats a second rogue. */
class MissionsTest {

    @Test
    void theCardGoesUpOnlyWhenTheBeaconWasMet() {
        World w = new World();
        assertEquals(Missions.NONE, w.missions.bounty, "no card before the beacon");
        Chatter.beaconRaised(w); // the scout was never met: the report is a rumour to nobody
        assertEquals(Missions.NONE, w.missions.bounty, "an unmet voice opens nothing");
        assertEquals(227, w.enemies.size(), "and no rogue rides the roster yet");
        w.voices.get(0).voiceMet = true; // the Scout is a person now
        Chatter.beaconRaised(w);
        assertEquals(Missions.OPEN, w.missions.bounty, "the met report opens the board");
        assertEquals(228, w.enemies.size(), "the rogue joins the roster once");
        Enemy rogue = w.enemies.get(227);
        assertEquals(Enemy.Kind.BHAVI, rogue.kind, "a heavy-shell close-range duel at the card");
        assertTrue(rogue.elite && rogue.hunt, "elite, and straight to hunting");
        Chatter.beaconRaised(w);
        assertEquals(228, w.enemies.size(), "the card never opens twice");
    }

    @Test
    void thePursePaysOnceAndTheBoardCloses() {
        World w = new World();
        w.voices.get(0).voiceMet = true;
        Chatter.beaconRaised(w); // OPEN
        Enemy rogue = w.enemies.get(227);
        int coins = w.hangar.coins;
        int lives = w.lives;
        rogue.hurt(9999, w); // DamBha is spent
        assertTrue(rogue.dead, "the kill lands");
        w.missions.update(); // the payment frame
        assertEquals(Missions.DONE, w.missions.bounty, "the row closes on his fall");
        assertEquals(coins + 250, w.hangar.coins, "the purse pays the card");
        assertEquals(lives + 1, w.lives, "and a life sails for it");
        w.missions.update();
        w.missions.update();
        assertEquals(coins + 250, w.hangar.coins, "sold once is paid once");
    }

    @Test
    void aClosedBoardNeverReopensInARun() {
        World w = new World();
        w.voices.get(0).voiceMet = true;
        Chatter.beaconRaised(w);
        w.enemies.get(227).hurt(9999, w);
        w.missions.update();
        assertEquals(Missions.DONE, w.missions.bounty);
        Chatter.beaconRaised(w); // the beacon answers again — midrun nothing reopens
        assertEquals(Missions.DONE, w.missions.bounty, "one card per run");
    }

    @Test
    void theWrecksComeHomeAndTheHammerPays() {
        World w = new World();
        assertEquals(Missions.NONE, w.missions.salvage, "no ask before Maya");
        w.missions.openSalvage(); // Maya's sight does the asking in play; the row is what it opens
        assertEquals(Missions.OPEN, w.missions.salvage);
        w.missions.openSalvage();
        assertEquals(Missions.OPEN, w.missions.salvage, "the ask seats once: no reset of the count");
        int coins = w.hangar.coins;
        for (int i = 0; i < 6; i++) { // the hull carried home, one wreck per stop
            w.ship.x = w.wrecks[i][0];
            w.ship.y = w.wrecks[i][1];
            w.missions.update();
            assertEquals(i + 1, w.missions.salvageTaken, "stop " + i + " landed");
            assertEquals(i == 5 ? Missions.DONE : Missions.OPEN, w.missions.salvage);
            assertTrue(w.wreckTaken[i], "gone is gone");
        }
        w.missions.update();
        w.missions.update();
        assertEquals(coins + 200, w.hangar.coins, "the retempering pays once");
        assertEquals(1, w.hangar.shards[0], "and the forge's first Akasha shard");
        assertTrue(w.codex[Codex.RETEMPER], "the book takes the page");
    }
}

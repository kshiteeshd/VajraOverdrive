package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.ship.Hangar;
import vajra.ship.ShipDef;
import vajra.weapon.WeaponDef;

/** How a run begins: with the base ship and nothing else, and with the side story held back. */
class StartTest {
    @Test
    void aNewRunOwnsOnlyTheBaseShipAndTheSingleBolt() {
        Hangar h = new Hangar();
        assertEquals(0, h.coins, "a new run is broke");
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            assertEquals(i == 0, h.ownsShip(i), ShipDef.ALL[i].name + (i == 0 ? " comes with the run" : " is locked"));
            for (int t = 1; t < 3; t++) assertFalse(h.owned[i][t], ShipDef.ALL[i].name + " tier " + t + " is locked");
        }
        int weapons = 0;
        for (boolean o : h.weaponOwned) if (o) weapons++;
        assertEquals(1, weapons, "only the Single Bolt");
        assertTrue(h.weaponOwned[WeaponDef.SINGLE.idx]);
        assertEquals(0, h.shipIndex, "the base ship is the one fitted");
        World w = new World(0, World.CAMPAIGN);
        assertEquals(ShipDef.ALL[0], w.ship.def, "and the one flown");
        assertFalse(w.hangar.ownsShip(1), "the second hull is not for free");
    }

    @Test
    void withTheSideStoryHeldBackNobodyIsPlacedAndNoJobOpens() {
        boolean was = World.sideStory;
        World.sideStory = false;
        try {
            World w = new World(0, World.CAMPAIGN);
            assertTrue(w.voices.isEmpty(), "no side characters in either loka");
            assertEquals(Missions.NONE, w.missions.salvage, "no salvage job");
            assertEquals(Missions.NONE, w.missions.bounty, "no bounty");
            for (int i = 0; i < 600; i++) w.update(1 / 60f, new vajra.ship.ShipControls() {
                public float thrustY() { return 0; }
                public float thrustX() { return 0; }
                public float rotate() { return 0; }
                public boolean fire() { return false; }
                public boolean boost() { return false; }
                public boolean shield() { return false; }
                public boolean mouseAim() { return false; }
                public float aimX() { return 0; }
                public float aimY() { return 0; }
            });
            for (Cue c : w.cues) assertFalse(c.name().startsWith("VOICE_"), "no voice scene is queued: " + c);
        } finally {
            World.sideStory = was;
        }
    }

    @Test
    void aRunParkedByTheOldTestBuildIsNotOfferedBack() {
        var p = new vajra.profile.Profile();
        p.run = "{coins:999999,shipIndex:0}";
        assertFalse(p.hasRun(), "the everything-unlocked purse is recognised");
        p.run = "{coins:412,shipIndex:0}";
        assertTrue(p.hasRun(), "a real run is kept");
    }
}

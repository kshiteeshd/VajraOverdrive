package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.ship.ShipDef;
import vajra.world.World;

/** With everything unlocked (the test build), every hull in the strip can be fitted from the bay, by the keys and by the mouse. */
class BayUnlockedTest {
    @Test
    void everyHullCanBeFittedFromTheBay() {
        World w = new World();
        w.hangar.unlockAll();
        BayController c = new BayController(w, new BayUi(null));
        c.open();
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            c.ui().setRail(BayUi.HULLS);
            c.ui().setHull(i);
            c.ui().setTierFocus(-1);
            assertTrue(c.act(), "ENTER acts");
            assertEquals(i, w.hangar.shipIndex, "hull " + i + " is fitted");
            assertEquals(ShipDef.ALL[i], w.ship.def, "and the hull on the pad is that one");
        }
    }

    @Test
    void theNumberKeysAndTheStripKeysFocusEachHullAndEnterFitsIt() {
        World w = new World();
        w.hangar.unlockAll();
        BayController c = new BayController(w, new BayUi(null));
        c.open();
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            c.keyDown(com.badlogic.gdx.Input.Keys.NUM_1 + i);
            assertEquals(i, c.ui().hull(), "the number key focuses hull " + i);
            c.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
            assertEquals(i, w.hangar.shipIndex, "ENTER fits it");
        }
    }

    @Test
    void anOwnedHullIsFittedByItsNumberKeyAtOnce() {
        World w = new World();
        w.hangar.unlockAll();
        BayController c = new BayController(w, new BayUi(null));
        c.open();
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            c.keyDown(com.badlogic.gdx.Input.Keys.NUM_1 + i);
            assertEquals(i, w.hangar.shipIndex, "no ENTER needed for a hull you own: " + i);
        }
    }

    @Test
    void anOwnedTierCardWearsThatColourway() {
        World w = new World();
        w.hangar.unlockAll();
        BayController c = new BayController(w, new BayUi(null));
        c.open();
        c.ui().setRail(BayUi.HULLS);
        c.ui().setHull(2);
        for (int t = 0; t < 3; t++) {
            c.ui().setTierFocus(t);
            c.act();
            assertEquals(t + 1, ShipRenderer.variation(w.ship), "tier card " + t + " puts on look " + (t + 1));
        }
    }

    @Test
    void withoutTheUnlockTheLookFollowsTheHighestTierBought() {
        vajra.ship.Hangar h = new vajra.ship.Hangar();
        assertEquals(1, h.variation(0));
        h.owned[0][1] = true;
        assertEquals(2, h.variation(0));
        h.owned[0][2] = true;
        assertEquals(3, h.variation(0));
        h.look[0] = 0;
        assertEquals(1, h.variation(0), "a chosen look wins while it is owned");
    }
}

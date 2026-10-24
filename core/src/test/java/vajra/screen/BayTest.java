package vajra.screen;

import static org.junit.jupiter.api.Assertions.*;

import com.badlogic.gdx.Input;
import org.junit.jupiter.api.Test;
import vajra.ship.Hangar;
import vajra.ship.ShipDef;
import vajra.weapon.WeaponDef;
import vajra.world.World;

/** The bay, headless: focus moves where the keys say, ENTER buys or fits the right thing, the
 *  labels tell the truth, and the layout fits the screen without two cards sitting on each other. */
class BayTest {
    private static BayController bay(World w) {
        BayController c = new BayController(w, new BayUi(null));
        c.open();
        return c;
    }

    @Test
    void theStripWalksAndStopsAtTheEnds() {
        World w = new World();
        BayController c = bay(w);
        c.move(-1, 0);
        assertEquals(0, c.ui().hull(), "no wrapping off the left end");
        for (int i = 0; i < 12; i++) c.move(1, 0);
        assertEquals(ShipDef.ALL.length - 1, c.ui().hull(), "or the right end");
    }

    @Test
    void downReachesTheTierCardsAndUpReturns() {
        BayController c = bay(new World());
        c.move(0, 1);
        assertEquals(0, c.ui().tierFocus());
        c.move(1, 0);
        c.move(1, 0);
        c.move(1, 0);
        assertEquals(2, c.ui().tierFocus(), "three tier cards, no more");
        c.move(0, -1);
        assertEquals(-1, c.ui().tierFocus());
    }

    @Test
    void enterBuysAndFitsAHullYouCanAfford() {
        World w = new World();
        BayController c = bay(w);
        w.hangar.coins = ShipDef.ALL[1].cost;
        c.ui().setHull(1);
        c.act();
        assertTrue(w.hangar.ownsShip(1), "the Kite is bought");
        assertEquals(1, w.hangar.shipIndex, "and fitted at once");
        assertEquals(0, w.hangar.coins);
    }

    @Test
    void enterCannotBuyWhatTheLokaHasNotOpened() {
        World w = new World();
        BayController c = bay(w);
        w.hangar.coins = 9999;
        c.ui().setHull(5); // Mortar Mk-6 opens at Sutala
        c.act();
        assertFalse(w.hangar.ownsShip(5));
        assertEquals(9999, w.hangar.coins, "and no coin is taken");
    }

    @Test
    void aTierIsBoughtFromItsCardAndNeedsTheHull() {
        World w = new World();
        BayController c = bay(w);
        w.hangar.coins = ShipDef.ALL[0].upgrades[1].cost;
        c.move(0, 1);
        c.move(1, 0); // tier 1.2
        c.act();
        assertTrue(w.hangar.owned[0][1], "Rapid Cycler bought off its card");
        c.ui().setHull(1);
        c.ui().setTierFocus(1);
        w.hangar.coins = 9999;
        c.act();
        assertFalse(w.hangar.owned[1][1], "a hull you do not own cannot take a tier");
    }

    @Test
    void theWheelBuysThenFitsAPattern() {
        World w = new World();
        BayController c = bay(w);
        c.ui().setRail(BayUi.ARMS);
        c.ui().setArm(1);
        w.hangar.coins = WeaponDef.ALL[1].cost - 1;
        c.act();
        assertFalse(w.hangar.weaponOwned[1], "one coin short buys nothing");
        w.hangar.coins = WeaponDef.ALL[1].cost;
        c.act();
        assertTrue(w.hangar.weaponOwned[1]);
        assertEquals(1, w.hangar.weaponIndex, "a bought pattern is fitted");
        c.move(1, 0);
        assertEquals(2, c.ui().arm(), "A/D walk the ring");
        c.ui().setArm(0);
        c.move(-1, 0);
        assertEquals(WeaponDef.ALL.length - 1, c.ui().arm(), "and the ring wraps, unlike the strip");
    }

    @Test
    void anAstraCardMakesItTheOneFInvokes() {
        World w = new World();
        BayController c = bay(w);
        c.ui().setRail(BayUi.ASTRA);
        c.ui().setAstra(2);
        assertEquals(0, w.astraSel, "the Akasha astra is the default");
        c.act();
        assertEquals(2, w.astraSel, "ENTER on a card chooses it");
        c.ui().setAstra(vajra.world.Astra.BRAHMA);
        c.act();
        assertEquals(vajra.world.Astra.BRAHMA, w.astraSel, "the Brahmastra card too");
    }

    @Test
    void theAstraChosenIsTheOneFInvokesAndAnUnpayableChoiceFallsBackToOneThatCanFly() {
        World w = new World();
        w.hangar.shards[vajra.world.Astra.AKASHA] = 5;
        w.hangar.shards[vajra.world.Astra.AGNI] = 5;
        w.astraSel = vajra.world.Astra.AKASHA;
        assertEquals(vajra.world.Astra.AKASHA, w.loadedAstra());
        assertTrue(w.cycleAstra(), "X steps to the next astra you can pay");
        assertEquals(vajra.world.Astra.AGNI, w.astraSel);
        w.hangar.shards[vajra.world.Astra.AGNI] = 0;
        assertEquals(vajra.world.Astra.AKASHA, w.loadedAstra(), "the dry choice falls to what can still fly");
    }

    @Test
    void xSellsEveryShardAndNumberKeysJumpToAHull() {
        World w = new World();
        BayController c = bay(w);
        w.hangar.shards[0] = 3;
        c.keyDown(Input.Keys.X);
        assertEquals(3 * Hangar.SHARD_SELL, w.hangar.coins);
        c.keyDown(Input.Keys.NUM_4);
        assertEquals(3, c.ui().hull());
        assertEquals(BayUi.HULLS, c.ui().rail());
    }

    @Test
    void tabCyclesThreeRails() {
        BayController c = bay(new World());
        c.keyDown(Input.Keys.TAB);
        c.keyDown(Input.Keys.TAB);
        assertEquals(BayUi.ASTRA, c.ui().rail());
        c.keyDown(Input.Keys.TAB);
        assertEquals(BayUi.HULLS, c.ui().rail());
    }

    @Test
    void closingParksTheBay() {
        World w = new World();
        BayController c = bay(w);
        assertTrue(c.isOpen());
        c.close();
        assertFalse(c.isOpen());
    }

    // --- labels ---------------------------------------------------------------------------

    @Test
    void labelsSayWhatIsOwnedAffordableOrShort() {
        Hangar h = new Hangar();
        ShipDef kite = ShipDef.ALL[1];
        assertEquals("OWNED", BayUi.tierState(h, ShipDef.ALL[0], 0, 0));
        assertTrue(BayUi.tierState(h, kite, 0, 0).contains("SHORT"));
        h.coins = kite.cost;
        assertTrue(BayUi.tierState(h, kite, 0, 0).contains("ENTER BUYS"));
        assertEquals("OWN THE HULL FIRST", BayUi.tierState(h, kite, 1, 0));
        assertEquals("BUY 1.2 FIRST", BayUi.tierState(h, ShipDef.ALL[0], 2, 0), "x.3 waits for x.2");
        h.owned[0][1] = true;
        assertTrue(BayUi.tierState(h, ShipDef.ALL[0], 2, 0).contains("shards"), "x.3 asks for shards");
        assertEquals("MK-3", BayUi.markName(ShipDef.ALL[2]), "an unbought hull shows only its mark");
        assertTrue(BayUi.hullState(h, ShipDef.ALL[1], 0).startsWith("LOCKED"), "an unbought hull reads LOCKED");
        assertTrue(BayUi.hullState(h, ShipDef.ALL[5], 0).startsWith("OPENS"));
        assertEquals("FITTED", BayUi.weaponState(h, WeaponDef.ALL[0]));
        assertEquals("0 / 5 SHARDS", BayUi.astraState(h, 3));
        h.shards[0] = 5;
        assertEquals("1 RELEASE READY", BayUi.astraState(h, 0));
        h.shards[0] = 10;
        assertEquals("2 RELEASES READY", BayUi.astraState(h, 0));
        assertEquals("NEEDS 5 OF EACH", BayUi.astraState(h, vajra.world.Astra.BRAHMA));
    }

    @Test
    void everyBarIsAFractionOfTheBest() {
        float top = 0;
        for (WeaponDef d : WeaponDef.ALL) {
            float[] b = BayUi.weaponBars(d);
            assertTrue(b[0] > 0 && b[0] <= 1.0001f && b[1] > 0 && b[1] <= 1.0001f, d.name);
            top = Math.max(top, Math.max(b[0], b[1]));
        }
        assertEquals(1f, top, 0.0001f, "the best pattern fills a bar");
    }

    @Test
    void eachTierNamesWhatItChanges() {
        for (ShipDef d : ShipDef.ALL)
            for (int t = 1; t <= 2; t++)
                assertNotEquals("THE HULL ITSELF", BayUi.tierEffect(d, t), d.name + " tier " + t);
        assertEquals("THE HULL ITSELF", BayUi.tierEffect(ShipDef.ALL[0], 0));
    }

    // --- geometry -------------------------------------------------------------------------

    @Test
    void theCardsFitTheScreenAndNeverSitOnEachOther() {
        assertTrue(BayUi.stripX(0) >= 40 && BayUi.stripX(6) + BayUi.STRIP_W <= Ui.UI_W - 40, "strip fits");
        assertTrue(BayUi.tierX(0) >= 40 && BayUi.tierX(2) + BayUi.TIER_W <= Ui.UI_W - 40, "tiers fit");
        assertTrue(BayUi.astraX(0) >= 40 && BayUi.astraX(vajra.world.Astra.KINDS - 1) + BayUi.AST_W <= Ui.UI_W - 40, "astra cards fit");
        assertTrue(BayUi.STRIP_Y > BayUi.TIER_Y + BayUi.TIER_H + 16, "the strip clears the tier row");
        assertTrue(BayUi.STRIP_Y + BayUi.STRIP_H + 8 < BayUi.RULE_Y, "even the lifted card clears the header rule");
        assertTrue(BayUi.TIER_Y > BayUi.STATUS_Y + 24, "the tier row clears the status line");
    }

    @Test
    void theWheelNodesAreSpacedAndInsideTheScreen() {
        int n = WeaponDef.ALL.length;
        for (int i = 0; i < n; i++) {
            float[] a = BayUi.nodeAt(i), b = BayUi.nodeAt((i + 1) % n);
            assertTrue(Math.hypot(a[0] - b[0], a[1] - b[1]) > BayUi.NODE + 10, "nodes " + i + " and next crowd");
            assertTrue(a[1] - BayUi.NODE > BayUi.STATUS_Y + 10 && a[1] + BayUi.NODE < BayUi.RULE_Y, "node " + i + " inside");
            boolean clearX = Math.abs(a[0] - BayUi.WHEEL_CX) > BayUi.HUB_W / 2 + BayUi.NODE / 2;
            boolean clearY = Math.abs(a[1] - BayUi.WHEEL_CY) > BayUi.HUB_H / 2 + BayUi.NODE / 2;
            assertTrue(clearX || clearY, "node " + i + " overlaps the hub");
        }
    }
}

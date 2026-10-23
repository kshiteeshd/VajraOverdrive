package vajra.screen;

import vajra.ship.Ship;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.ship.ShipDef;

/** Ship sprites silently fall back to the polygon hull, so a missing folder or a renamed
 *  variation would never surface on screen. This is the check that fails instead. */
class ShipArtTest {
    @Test
    void everyShipHasThree32x32Variations() throws Exception {
        File assets = assetsDir();
        assertNotNull(assets, "no assets folder found from " + new File(".").getAbsolutePath());
        for (ShipDef d : ShipDef.ALL) {
            for (int v = 1; v <= 3; v++) {
                File f = new File(assets, ShipImages.artPath(d.id, v));
                assertTrue(f.isFile(), "no sprite for " + d.name + " variation " + v);
                BufferedImage img = ImageIO.read(f);
                assertEquals(32, img.getWidth(), d.name + " v" + v + " width");
                assertEquals(32, img.getHeight(), d.name + " v" + v + " height");
            }
        }
    }

    /** Every SHEETED MARK flies its forged sheet (ship_forge/ShipForge.java): 64x64 cells,
     *  8 idle frames per tier row (a, b, c), the hit and bank clips parked after the idle
     *  block. Marks without a sheet fall back to their static PNGs (the test above). */
    @Test
    void theForgedSheetsAreTheFlightArt() throws Exception {
        for (int mk = 1; mk <= 3; mk++) {
            File f = new File(assetsDir(), "vajra_ships/mk_" + mk + "/mark" + mk + "_sheet.png");
            assertTrue(f.isFile(),
                    "mark " + mk + "'s sheet is missing: run 'java ship_forge/ShipForge.java --mark " + mk
                            + " tools/forge_out' and copy mark" + mk + "_sheet.png to assets/vajra_ships/mk_" + mk + "/");
            BufferedImage img = ImageIO.read(f);
            assertEquals(18 * 64, img.getWidth(), "18 frames wide: idle 8, hit 4, bank 3 + 3");
            assertEquals(3 * 64, img.getHeight(), "one 64px tier row per variation: a, b, c");
        }
    }

    /** The clip pickers, headless: every hit re-punches the hit beat from its flash frame —
     *  a stream reads as continuous flash — and past the beat the art falls back to the bank
     *  or the idle. The bank picks its side by sign and its strength by thirds of the hull's
     *  lean; near-level leans idle (a shallow frame adds nothing over the idle flutter). */
    @Test
    void theClipPickerReadsTheSheet() {
        assertEquals(ShipImages.HIT_0, ShipImages.hitFrame(0f), "fresh hit: straight to the flash");
        assertEquals(ShipImages.HIT_0 + 1, ShipImages.hitFrame(0.13f), "second frame");
        assertEquals(ShipImages.HIT_0 + 3, ShipImages.hitFrame(0.3f), "last frame");
        assertEquals(-1, ShipImages.hitFrame(0.4f), "past the beat: idle again");
        assertEquals(-1, ShipImages.bankFrame(0f), "wings level idles");
        assertEquals(ShipImages.BANKL_0 + 1, ShipImages.bankFrame(-0.4f), "a left glide leans left");
        assertEquals(ShipImages.BANKL_0 + 2, ShipImages.bankFrame(-1f), "full lean reaches the hard bank");
        assertEquals(ShipImages.BANKR_0 + 2, ShipImages.bankFrame(1f), "and the right side is its mirror");
        assertEquals(ShipImages.BANKR_0 + 1, ShipImages.bankFrame(0.4f));
    }

    /** Variation tracks the highest bought tier, so the ladder must be 1 -> 2 -> 3. */
    @Test
    void variationFollowsTheBoughtTiers() {        vajra.world.World w = new vajra.world.World();
        w.hangar.coins = 9999; // test purse: the yard ships with 0
        vajra.ship.Ship s = w.ship;
        assertEquals(1, ShipRenderer.variation(s), "base tier ships with the hull");
        w.hangar.buy(s.def, 1);
        assertEquals(2, ShipRenderer.variation(s));
        w.hangar.shards[0] = vajra.ship.Hangar.SHARD_COST; // x.3 wants shards on top of coins
        w.hangar.buy(s.def, 2);
        assertEquals(3, ShipRenderer.variation(s));
    }

    /** A hold has to climb through all four pack levels: tap, small, large, max. Boost runs the
     *  same ramp — it changes hue and length instead of pinning the level. */
    @Test
    void plumeLevelsRampWithHoldTime() {
        vajra.world.World w = new vajra.world.World();
        vajra.ship.Ship s = w.ship;
        assertEquals(0, ShipRenderer.level(s), "fresh tap idles at level 0");
        s.thrustHold = 0.15f;
        assertEquals(1, ShipRenderer.level(s), "held a moment = level 1");
        s.thrustHold = 0.4f;
        assertEquals(2, ShipRenderer.level(s), "held longer = level 2");
        s.thrustHold = 0.9f;
        assertEquals(3, ShipRenderer.level(s), "held long = max thrust");
        s.boosting = true;
        assertEquals(3, ShipRenderer.level(s), "boost at full hold keeps the max level");
        s.thrustHold = 0f;
        assertEquals(0, ShipRenderer.level(s), "boost uses the same ramp, no pin to max");
        assertEquals(2f, ShipRenderer.stretch(s), "and the plume doubles in length");
        s.boosting = false;
        assertEquals(1f, ShipRenderer.stretch(s), "stowed when not boosting");
    }

    private static File assetsDir() {
        for (String p : new String[] { "assets", "../assets" }) {
            File d = new File(p);
            if (d.isDirectory()) return d;
        }
        return null;
    }
}

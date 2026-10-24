package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.ship.Hangar;

/** The wheel's pure parts: which patterns sit on it, where, and which one the pointer is on. */
class WeaponWheelTest {
    @Test
    void theWheelHoldsExactlyWhatYouOwnInTheHangarsOrder() {
        Hangar h = new Hangar();
        assertArrayEquals(new int[] {0}, WeaponWheel.owned(h), "a new pilot owns the Single Bolt alone");
        h.weaponOwned[3] = true;
        h.weaponOwned[1] = true;
        assertArrayEquals(new int[] {0, 1, 3}, WeaponWheel.owned(h));
    }

    @Test
    void theFirstChipIsAtTheTopAndTheRestRunClockwise() {
        assertEquals(90f, WeaponWheel.angle(0, 4), 1e-4f);
        assertEquals(0f, WeaponWheel.angle(1, 4), 1e-4f, "then east");
        assertEquals(-90f, WeaponWheel.angle(2, 4), 1e-4f, "then south");
    }

    @Test
    void thePointerPicksTheNearestChipAndTheCentreKeepsWhatYouHave() {
        int n = 4;
        assertEquals(-1, WeaponWheel.pick(n, 5, 5), "the dead centre picks nothing");
        assertEquals(0, WeaponWheel.pick(n, 0, 120), "straight up is the first chip");
        assertEquals(1, WeaponWheel.pick(n, 120, 0), "east is the second");
        assertEquals(2, WeaponWheel.pick(n, 0, -120), "south the third");
        assertEquals(3, WeaponWheel.pick(n, -120, 0), "west the fourth");
        assertEquals(1, WeaponWheel.pick(n, 90, 40), "between up and east, nearer east");
        assertEquals(-1, WeaponWheel.pick(0, 100, 100), "an empty wheel picks nothing");
    }

    @Test
    void aTapIsShorterThanTheHoldAndTheWheelSlowsTheWorldWithoutStoppingIt() {
        assertTrue(WeaponWheel.OPEN_AFTER > 0.1f && WeaponWheel.OPEN_AFTER < 0.5f);
        assertTrue(WeaponWheel.SLOW > 0f && WeaponWheel.SLOW < 0.5f, "a crawl, never a free pause");
    }
}

package vajra.ship;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import vajra.world.World;

/** Kite Mk-2's third variant (2.3) is the toughest hull of the ladder and nearly proof against fire. */
class KiteBurnProofTest {
    private static World world(boolean owned) {
        World w = World.vitala(0);
        w.hangar.owned[1][0] = true;
        w.hangar.owned[1][1] = owned;
        w.hangar.owned[1][2] = owned;
        w.ship.setDef(ShipDef.ALL[1]);
        w.ship.invuln = 0f;
        return w;
    }

    @Test
    void twinLinkIsTheBestHullAndTurnsFireAside() {
        World w = world(true);
        float max = w.ship.maxHp();
        for (ShipDef d : ShipDef.ALL) assertTrue(max >= d.maxHp + w.hangar.hullBonus(d) || d == ShipDef.ALL[1], "no hull beats it");
        assertEquals(310f, max, 0.01f);
        float before = w.ship.hp;
        w.ship.burn(20f, w);
        float fire = before - w.ship.hp;
        w.ship.invuln = 0f;
        before = w.ship.hp;
        w.ship.damage(20f, w);
        float plain = before - w.ship.hp;
        assertEquals(20f * 0.75f * 0.15f, fire, 0.01f, "fire: a quarter ignored, then 85% of the rest");
        assertEquals(20f * 0.75f, plain, 0.01f, "a plain hit: a quarter ignored");
    }

    @Test
    void theBaseKiteIsUnchanged() {
        World w = world(false);
        assertEquals(90f, w.ship.maxHp(), 0.01f);
        float before = w.ship.hp;
        w.ship.burn(20f, w);
        assertEquals(20f, before - w.ship.hp, 0.01f);
    }
}

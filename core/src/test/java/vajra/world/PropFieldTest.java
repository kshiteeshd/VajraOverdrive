package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The props are a pure, seeded function of the world: same chunk, same props; none on water or road; each band has its own. */
class PropFieldTest {
    private static final float[] STARTS = {0, 1600, 4100, 7600, 12000, 17600};
    private final GroundMap map = new GroundMap(GroundMap.SEED, 5200, 20600, STARTS);
    private final PropField field = new PropField(map, 5200, STARTS);

    private List<PropField.Prop> chunk(float x, float y) {
        List<PropField.Prop> out = new ArrayList<>();
        field.chunk(x, y, 512, out);
        return out;
    }

    @Test
    void aChunkHoldsTheSamePropsTwice() {
        List<PropField.Prop> a = chunk(2048, 512), b = chunk(2048, 512);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x);
            assertEquals(a.get(i).kind, b.get(i).kind);
        }
    }

    @Test
    void everyPropStandsInItsChunkOnLandAndNoneOnWaterOrRoad() {
        int total = 0;
        for (float y = 0; y < 20000; y += 1536)
            for (float x = 0; x < 5000; x += 1024) {
                for (PropField.Prop p : chunk(x, y)) {
                    total++;
                    assertTrue(p.x >= x && p.x < x + 512 && p.y >= y && p.y < y + 512, "in its own chunk");
                    if (p.kind == PropField.TREE || p.kind == PropField.MUSHROOM)
                        assertEquals(GroundMap.LAND, map.surface(p.x, p.y), "a tree stands on land: " + p.x + "," + p.y);
                }
            }
        assertTrue(total > 100, "the land is not bare: " + total);
    }

    @Test
    void theBandsHaveTheirOwnThingsAndTheDepthOrderHolds() {
        boolean tree = false, mushroom = false, lamp = false, pillar = false;
        for (float y = 0; y < 12000; y += 400)
            for (float x = 0; x < 5200; x += 512)
                for (PropField.Prop p : chunk(x, y)) {
                    tree |= p.kind == PropField.TREE && p.band == 0;
                    mushroom |= p.kind == PropField.MUSHROOM && p.band == 2;
                    lamp |= p.kind == PropField.LAMP && p.band == 1;
                    pillar |= p.kind == PropField.PILLAR && p.band == 3;
                }
        assertTrue(tree && mushroom && lamp && pillar, "tree " + tree + " mushroom " + mushroom + " lamp " + lamp + " pillar " + pillar);
        PropField.Prop p = new PropField.Prop(PropField.PILLAR, 3, 0, 0, 0.5f, 0);
        assertTrue(p.high(), "a pillar rises above the flight plane");
        assertFalse(new PropField.Prop(PropField.TREE, 0, 0, 0, 1f, 0).high(), "a tree stays below it");
    }
}

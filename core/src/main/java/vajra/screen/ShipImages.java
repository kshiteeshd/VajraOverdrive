package vajra.screen;

import vajra.ship.Ship;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.ship.ShipDef;

/** Player ship sprites. Mark 1 rides THE FORGED SHEET: ship_forge/ShipForge.java's 64x64
 *  render — 8 idle frames per tier row (a/b/c), flutter primaries, mirage ghosts and the
 *  exhaust glow baked in — sliced on its own grid, no atlas needed. Every other ship keeps
 *  vajra_ships/mk_<n>/ship_<v>.png (tools/gen_ships.py), one static variation per tier. */
public class ShipImages {
    private static final int VARIATIONS = 3;
    private static final int FORGE_FRAME = 64; // the sheet's cell
    private static final int FORGE_FRAMES = 8; // the idle row (hit and tilt clips unused here)
    private static final float FORGE_FPS = 10; // the 0.8 s loop the shimmer and flutter build

    private Texture forgeSheet;                                      // mk1's forged sheet
    private final TextureRegion[][] forge = new TextureRegion[VARIATIONS][FORGE_FRAMES];
    private final TextureRegion[][] reg = new TextureRegion[ShipDef.ALL.length][VARIATIONS];

    public ShipImages() {
        forgeSheet = ImageKit.texture("vajra_ships/mk_1/mark1_sheet.png", true);
        if (forgeSheet != null) {
            for (int t = 0; t < VARIATIONS; t++)
                for (int f = 0; f < FORGE_FRAMES; f++)
                    forge[t][f] = new TextureRegion(forgeSheet, f * FORGE_FRAME, t * FORGE_FRAME,
                            FORGE_FRAME, FORGE_FRAME);
        }
        for (ShipDef d : ShipDef.ALL) {
            for (int v = 0; v < VARIATIONS; v++) {
                if (d.id == 0 && forgeSheet != null) continue; // the forged sheet serves mark 1
                reg[d.id][v] = load(artPath(d.id, v + 1));
            }
        }
    }

    /** Ship ids are 0-based, folders are 1-based: mk_1 .. mk_7. */
    public static String artPath(int shipId, int variation) {
        return "vajra_ships/mk_" + (shipId + 1) + "/ship_" + variation + ".png";
    }

    private TextureRegion load(String path) {
        Texture t = ImageKit.texture(path, true);
        return t == null ? null : new TextureRegion(t); // caller falls back to the polygon hull
    }

    /** The tier's bare hull, frame 0 — the hangar preview and the hasArt check. Tier is
     *  1..3. Null when the art is missing. */
    public TextureRegion get(int shipId, int variation) {
        if (shipId < 0 || shipId >= reg.length || variation < 1 || variation > VARIATIONS) return null;
        if (shipId == 0 && forgeSheet != null) return forge[variation - 1][0];
        return reg[shipId][variation - 1];
    }

    /** The flight hull frame: mark 1 rides the forged idle flutter (the mirage sweeps its
     *  band and the primaries flap across the loop); every other ship keeps its one static
     *  PNG. Tier is 1..3. Null when the art is missing. */
    public TextureRegion frame(int shipId, int tier1to3, float time) {
        if (shipId == 0 && forgeSheet != null) {
            TextureRegion[] row = forge[tier1to3 - 1];
            return row[(int) (time * FORGE_FPS) % row.length];
        }
        return get(shipId, tier1to3);
    }

    public void dispose() {
        if (forgeSheet != null) {
            forgeSheet.dispose();
            forgeSheet = null;
        }
        for (TextureRegion[] row : reg) {
            for (TextureRegion r : row) {
                if (r != null) r.getTexture().dispose();
            }
        }
    }
}

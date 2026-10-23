package vajra.screen;

import vajra.ship.Ship;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.ship.ShipDef;

/** Player ship sprites. Any mark rides ITS FORGED SHEET when one exists: ship_forge/ShipForge.java,
 *  vajra_ships/mk_<n>/mark<n>_sheet.png — 64x64 cells, 18 frames per tier row (a/b/c) in the
 *  sheet layout — idle 0..7, hit 8..11 (flashes and sparks), left bank 12..14, right bank
 *  15..17 — sliced on its own grid, no atlas needed. Every mark without a sheet keeps
 *  vajra_ships/mk_<n>/ship_<v>.png (one static variation per tier). */
public class ShipImages {
    private static final int VARIATIONS = 3;
    private static final int FORGE_FRAME = 64; // the sheet's cell
    private static final float FORGE_FPS = 10; // the 0.8 s idle loop the shimmer builds
    // THE SHEET LAYOUT (ship_forge/ShipForge.java): cell order is the frame order.
    static final int IDLE_N = 8, HIT_N = 4, BANK_N = 3;
    static final int HIT_0 = IDLE_N, BANKL_0 = IDLE_N + HIT_N, BANKR_0 = BANKL_0 + BANK_N;
    static final int FRAME_N = BANKR_0 + BANK_N; // 18
    static final float HIT_FPS = 12;            // the hit beat: 4 frames, a third of a second

    private final Texture[] sheets = new Texture[ShipDef.ALL.length];  // one sheet per mark, or null
    private final TextureRegion[][][] forge = new TextureRegion[ShipDef.ALL.length][VARIATIONS][FRAME_N];
    private final TextureRegion[][] reg = new TextureRegion[ShipDef.ALL.length][VARIATIONS];

    public ShipImages() {
        for (ShipDef d : ShipDef.ALL) {
            sheets[d.id] = ImageKit.texture(sheetPath(d.id), true);
            if (sheets[d.id] == null) continue; // no forge sheet: the static PNGs serve this mark
            for (int t = 0; t < VARIATIONS; t++)
                for (int f = 0; f < FRAME_N; f++)
                    forge[d.id][t][f] = new TextureRegion(sheets[d.id], f * FORGE_FRAME,
                            t * FORGE_FRAME, FORGE_FRAME, FORGE_FRAME);
        }
        for (ShipDef d : ShipDef.ALL) {
            if (sheets[d.id] != null) continue; // the forged sheet serves this mark
            for (int v = 0; v < VARIATIONS; v++) {
                reg[d.id][v] = load(artPath(d.id, v + 1));
            }
        }
    }

    /** Ship ids are 0-based, folders are 1-based: mk_1 .. mk_7. */
    public static String artPath(int shipId, int variation) {
        return "vajra_ships/mk_" + (shipId + 1) + "/ship_" + variation + ".png";
    }

    /** A mark's forged sheet, next to its fallback PNGs: mk_<n>/mark<n>_sheet.png. */
    public static String sheetPath(int shipId) {
        return "vajra_ships/mk_" + (shipId + 1) + "/mark" + (shipId + 1) + "_sheet.png";
    }

    private TextureRegion load(String path) {
        Texture t = ImageKit.texture(path, true);
        return t == null ? null : new TextureRegion(t); // caller falls back to the polygon hull
    }

    /** The tier's bare hull, the sheet's idle frame 0 — the hangar preview and art checks.
     *  Tier is 1..3. Null when the art is missing. */
    public TextureRegion get(int shipId, int variation) {
        if (shipId < 0 || shipId >= reg.length || variation < 1 || variation > VARIATIONS) return null;
        if (sheets[shipId] != null) return forge[shipId][variation - 1][0];
        return reg[shipId][variation - 1];
    }

    /** THE FLIGHT FRAME for a sheeted mark: hits re-punch the forged hit beat from its first
     *  frame (a stream reads as continuous flash), the bank clip leans with the hull's strafe,
     *  and everything else idles (the mirage sweeps, the primaries flutter). A mark without
     *  a sheet keeps its one static PNG. Null when the art is missing. */
    public TextureRegion frame(Ship s, float time) {
        int v = ShipRenderer.variation(s) - 1;
        if (sheets[s.def.id] == null) return get(s.def.id, v + 1);
        int hit = hitFrame(s.hitT);
        if (hit >= 0) return forge[s.def.id][v][hit];
        int bank = bankFrame(s.lean);
        if (bank >= 0) return forge[s.def.id][v][bank];
        return forge[s.def.id][v][(int) (time * FORGE_FPS) % IDLE_N];
    }

    /** True while the hit beat is live: the renderer lets it override the blink cull —
     *  the flash IS the feedback. */
    public boolean hitClipLive(Ship s) {
        return sheets[s.def.id] != null && hitFrame(s.hitT) >= 0;
    }

    // --- pure clip pickers: cell index into the tier row, or -1 for "not this clip" ---

    static int hitFrame(float hitT) {
        if (hitT < 0 || hitT >= HIT_N / HIT_FPS) return -1;
        return HIT_0 + Math.min(HIT_N - 1, (int) (hitT * HIT_FPS));
    }

    static int bankFrame(float lean) {
        int idx = Math.round(Math.abs(lean) * (BANK_N - 1f)); // thirds: wings / lean / hard bank
        if (idx == 0) return -1; // near-level idles instead: a shallow frame adds nothing
        return (lean < 0 ? BANKL_0 : BANKR_0) + idx;
    }

    public void dispose() {
        for (Texture t : sheets) if (t != null) t.dispose();
        for (TextureRegion[] row : reg) {
            for (TextureRegion r : row) {
                if (r != null) r.getTexture().dispose();
            }
        }
    }
}

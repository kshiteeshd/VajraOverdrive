package vajra.screen;

import vajra.ship.Ship;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.ship.ShipDef;

/** Player ship sprites. Any mark rides ITS FORGED SHEET when one exists: ship_forge/ShipForge_8.java,
 *  vajra_ships/mk_<n>/mark<n>_sheet.png - 128x128 cells, 34 frames per tier row (a/b/c): idle 0..11, hit 12..17
 *  (flashes and sparks), boost 18..25 (two frames closing in, six flying in the boost form: wings folded, petals
 *  shut, heads tucked, blades swept back...), left bank 26..29, right bank 30..33 - sliced on its own grid, no
 *  atlas needed. The sheets are filtered with mipmaps: the ship flies at about a third of its texel size. Every mark
 *  without a sheet keeps vajra_ships/mk_<n>/ship_<v>.png (one static variation per tier). */
public class ShipImages {
    private static final int VARIATIONS = 3;
    private static final int FORGE_FRAME = 128; // the sheet's cell
    private static final float FORGE_FPS = 13;  // the ~0.9 s idle loop the shimmer builds
    // THE SHEET LAYOUT (ship_forge/ShipForge_8.java): cell order is the frame order.
    static final int IDLE_N = 12, HIT_N = 6, BOOST_N = 8, BANK_N = 4;
    static final int HIT_0 = IDLE_N, BOOST_0 = IDLE_N + HIT_N, BANKL_0 = BOOST_0 + BOOST_N, BANKR_0 = BANKL_0 + BANK_N;
    static final int FRAME_N = BANKR_0 + BANK_N; // 34
    static final float HIT_FPS = 18;            // the hit beat: 6 frames, a third of a second
    static final float BOOST_FPS = 16;          // the boost clip: 2 frames closing in, then 6 flying, looped
    static final int BOOST_IN = 2;

    private final Texture[] sheets = new Texture[ShipDef.ALL.length];  // one sheet per mark, or null
    private final TextureRegion[][][] forge = new TextureRegion[ShipDef.ALL.length][VARIATIONS][FRAME_N];
    private final TextureRegion[][] reg = new TextureRegion[ShipDef.ALL.length][VARIATIONS];

    public ShipImages() {
        for (ShipDef d : ShipDef.ALL) {
            sheets[d.id] = sheet(sheetPath(d.id));
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

    /** A forged sheet, filtered smooth with mipmaps: it is drawn about a third of its size, and Nearest would shimmer. */
    private static Texture sheet(String path) {
        try {
            if (!com.badlogic.gdx.Gdx.files.internal(path).exists()) return null;
            Texture t = new Texture(com.badlogic.gdx.Gdx.files.internal(path), true);
            t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            return t;
        } catch (Throwable ignored) {
            return null;
        }
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
     *  frame (a stream reads as continuous flash), the BOOST clip plays while the boost is held (the mark's own
     *  boost form), the bank clip leans with the hull's strafe, and everything else idles (the mirage sweeps, the
     *  primaries flutter). A mark without
     *  a sheet keeps its one static PNG. Null when the art is missing. */
    public TextureRegion frame(Ship s, float time) {
        int v = ShipRenderer.variation(s) - 1;
        if (sheets[s.def.id] == null) return get(s.def.id, v + 1);
        int hit = hitFrame(s.hitT);
        if (hit >= 0) return forge[s.def.id][v][hit];
        if (s.boosting) return forge[s.def.id][v][boostFrame(s.boostT)];
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

    /** The boost clip: the first BOOST_IN frames close the ship into its boost form as the boost begins, then the
     *  remaining frames loop for as long as it is held. */
    static int boostFrame(float boostT) {
        int step = (int) (Math.max(0f, boostT) * BOOST_FPS);
        if (step < BOOST_IN) return BOOST_0 + step;
        return BOOST_0 + BOOST_IN + (step - BOOST_IN) % (BOOST_N - BOOST_IN);
    }

    static int bankFrame(float lean) {
        int idx = Math.round(Math.abs(lean) * (BANK_N - 1f)); // quarters: wings / lean / harder / hard bank
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

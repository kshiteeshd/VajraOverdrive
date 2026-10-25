package vajra.screen;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import vajra.world.Checkpoint;
import vajra.world.Chest;
import vajra.world.World;

/** The compass, the bottom-left corner. NORTH-UP: a world bearing lands straight on the ring
 *  and never moves while you fight — in a twin-stick ship the nose is wherever the cursor
 *  points, so a ring that turned with facing only swam. "N" pins the climb, the one
 *  direction that never changes. The centre is the SHIP; the faint hand is the nose.
 *
 *  <p>Every kind shows its NEXT subject rather than every subject: the nearest gate, shop and
 *  UNCOLLECTED checkpoint above the ship, kind-coloured — gold diamond gate, cyan square
 *  shop, pink cross checkpoint. Drawing all of them
 *  collapsed instead: the whole world shares the centre column, so from mid-column every
 *  gate landed on one ring point and the rose showed one diamond where five gates stand.
 *  The whole rose is the MINIMAP: solid rock around the ship in the tiles' own off-white,
 *  and the points of interest as kind-coloured dots at their true offsets from you,
 *  clamped to the window's edge when a POI is beyond it, so "next, which way" reads off
 *  the edge dot and "what, exactly where" reads off the map. On arrival a dot vanishes;
 *  the respawn anchor always draws, even behind the ship, dimmed with a filled centre,
 *  so "where I come back" survives the run.
 *
 *  <p>Split from {@link Hud} so the HUD file only orchestrates. Two passes, like everything
 *  else: {@link #shapes} before the batch opens, {@link #label} inside it. */
final class HudCompass {
    static final float CX = 72, CY = 72, R = 56, W = 2f; // corner: ring clears the 14px margin

    /** The x of every gate. Match World if it ever publishes one. */
    private static final float GATE_X = World.WORLD_CX; // the spine every gate hangs on

    /** Closer than this (squared, px) the ship is on top of a subject: no mark. */
    private static final float NEAR2 = 400;

    /** The minimap: solid rock and the points of interest around the ship, inside the ring.
     *  A one-and-a-half-screen half-window (768 px) at one-tile steps renders terrain
     *  near-contiguous, snapped to the world grid so rock stays put as you climb past it.
     *  POIs sit at their true offset, clamped to MAP_LIM when beyond the window. */
    private static final float MAP_R = 55;   // rock fills the dial to the ring line
    private static final float MAP_LIM = 52; // edge-clamped POI dots rest just inside it
    private static final float MAP_STEP = 32, MAP_SCALE = MAP_R / 768;
    private static final int MAP_N = 24;
    private static final float[] ROCKC = { 0.91f, 0.90f, 0.88f }; // the tiles' off-white

    private static final int GATE = 0, SHOP = 1, CHECK = 2, CHEST = 3;
    private static final float[] NORTH = { 0.5f, 0.5f, 1f };
    private static final float[] GOLD = HudBars.GOLD;           // the gate arches' gold
    private static final float[] SHOPC = World.TATVA_RGB[3];    // the readout cyan (Jala)
    private static final float[] CHECKC = { 1f, 0.45f, 0.85f }; // the collector's pink
    private static final float[] CHESTC = { 1f, 0.78f, 0.25f };  // a coffer's gold

    private static final int RING_N = 32;
    private static final float[] RING_COS = new float[RING_N + 1], RING_SIN = new float[RING_N + 1];

    static {
        for (int i = 0; i <= RING_N; i++) { // the ring never changes: do the trig once
            float a = i * 360f / RING_N;
            RING_COS[i] = MathUtils.cosDeg(a);
            RING_SIN[i] = MathUtils.sinDeg(a);
        }
    }

    private final Ui ui;
    private final GlyphLayout layout = new GlyphLayout();
    private final float nW; // the "N" width, measured once
    private final float[] spot = new float[2]; // nearest-above scratch: x first, y second

    HudCompass(Ui ui) {
        this.ui = ui;
        this.nW = measure("N");
    }

    private float measure(String s) {
        layout.setText(ui.caption, s);
        return layout.width;
    }

    /** World bearing of a direction, degrees on the ring: 0 = world up, +90 = east. */
    static float bearingDeg(float dx, float dy) {
        return MathUtils.atan2(dx, dy) * MathUtils.radiansToDegrees;
    }

    /** The ship's nose as a ring bearing. It can spin with the cursor; the ring stays put. */
    static float noseBearingDeg(float facingRad) {
        return bearingDeg(MathUtils.cos(facingRad), MathUtils.sin(facingRad));
    }

    /** The nearest gate above: the grand gate and every band boundary, all on the centre
     *  column. Writes it into out (x first, y second). */
    static boolean nearestGate(float[] out, World w) {
        float best = Float.MAX_VALUE;
        boolean found = false;
        for (int i = 1; i < w.regionStart.length; i++) { // regionStart[0] = 0 is no gate
            float gy = w.regionStart[i];
            if (gy <= w.ship.y) continue;
            if (gy < best) {
                best = gy;
                out[0] = w.axisX;
                out[1] = gy;
                found = true;
            }
        }
        return found;
    }

    /** The nearest shop above the ship (shops are bands 1-4, all below the boss gate). */
    static boolean nearestShop(float[] out, World w) {
        float best = Float.MAX_VALUE;
        boolean found = false;
        for (float[] p : w.shops) {
            if (p[1] <= w.ship.y) continue;
            if (p[1] < best) {
                best = p[1];
                out[0] = p[0];
                out[1] = p[1];
                found = true;
            }
        }
        return found;
    }

    /** The nearest UNCOLLECTED checkpoint above the ship. Fully-collected copies are the
     *  anchor's business, not the next-aim's. */
    static boolean nearestCp(float[] out, World w) {
        float best = Float.MAX_VALUE;
        boolean found = false;
        for (Checkpoint c : w.checkpoints) {
            if (c == null || c.collected || c.y <= w.ship.y) continue;
            if (c.y < best) {
                best = c.y;
                out[0] = c.x;
                out[1] = c.y;
                found = true;
            }
        }
        return found;
    }

    /** Has the pilot met the one who knows where this loka's treasure lies. */
    static boolean senses(World w, Chest c) {
        vajra.enemy.Enemy who = vajra.world.Chatter.person(w, c.loka == 0 ? "SCOUT" : "SELLER");
        return who != null && who.voiceMet;
    }

}

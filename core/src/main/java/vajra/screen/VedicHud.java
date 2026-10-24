package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.util.Locale;
import vajra.ship.Hangar;
import vajra.ship.Ship;
import vajra.weapon.WeaponDef;
import vajra.world.Astra;
import vajra.world.Checkpoint;
import vajra.world.LokaTable;
import vajra.world.World;

/** THE VEDIC HUD: the flight instruments drawn again from scratch, as an alternative to the classic set
 *  (Hud, HudBars, HudCompass, HudGaze, ShardGauge's own drawing, the yojana tape). Settings.hudStyle picks one;
 *  the classic code is untouched, so flipping back is one key (F9) or the HUD row in SETTINGS.
 *
 *  <p>The picture it follows (the pilot's own sketch): top-left, a round WEAPON MEDALLION with its key under it, and
 *  running out of it a long glass HEALTH TUBE - a capsule on its side, the hull a flowing liquid in it, ringed in
 *  gold bands - with the LIVES as small rings under the tube. The SHIELD and the BOOST are the two halves of a ring
 *  round the MINIMAP (shield the left half, boost the right, each filling from the bottom up). Bottom-right, ONE
 *  small ASTRA GEM in a ring of five cells (one per shard; nothing else - no counts, no labels). Top-centre, Bala's eye. The world meter is the classic one (the depth tape and the loka's name),
 *  drawn by Hud. There is no dash.
 *
 *  <p>The language: muted gold hairlines (a quiet gold, never the loud one), the diamond as the one ornament, round
 *  dials, segmented bars in the manner of an old cockpit. Everything hangs from ONE safe margin ({@link #M}) on four
 *  corners. Nothing sits on a tinted plate or container: the instruments are drawn straight onto the game.
 *
 *  <p>It replaces only the instruments. The score, the combo, the hull-and-purse line, the crew, the jobs and the
 *  toasts stay the classic HUD's (the hull-and-purse line and the crew drop below the medallion in this set).
 *  Two passes, like everything else here: {@link #shapes} inside the open shape pass, {@link #labels} inside the batch. */
final class VedicHud {
    /** The one safe margin: every corner and edge instrument keys off it. */
    static final float M = 18;
    /** The quiet gold: the HUD's own gold, softer than the classic set's loud yellow. */
    static final float[] GOLD = {0.82f, 0.70f, 0.42f};
    private static final float[] JADE = {0.22f, 0.80f, 0.46f}, AMBER = {0.95f, 0.72f, 0.22f}, VERMILION = {0.95f, 0.28f, 0.2f};
    private static final float[] SKY = {0.42f, 0.80f, 0.98f}, SAFFRON = {0.98f, 0.58f, 0.2f};
    private static final float[] CHECKC = {1f, 0.45f, 0.85f}, SHOPC = World.TATVA_RGB[3];

    // --- top-left: the weapon medallion, the health tube out of it, the lives under the tube -----------
    static final float MED_R = 32, MED_CX = M + MED_R, MED_CY = Ui.UI_H - M - MED_R;
    static final float TUBE_H = 14, TUBE_L = 250, TUBE_X0 = MED_CX + 20, TUBE_CY = Ui.UI_H - M - 14.5f;
    static final float LIFE_R = 7.5f, LIFE_X0 = MED_CX + 53, LIFE_PITCH = 24.7f, LIFE_Y = MED_CY - 10;

    // --- top-centre: the eye ---------------------------------------------------------------------------
    static final float EYE_X = Ui.UI_W / 2f, EYE_Y = Ui.UI_H - 92, EYE_A = 30, EYE_H = 13;

    // --- bottom-right: the astra gem in its ring ------------------------------------------------------
    static final float RIGHT = Ui.UI_W - M;
    static final float GEM_R = 13, RING_R = 24, GEM_CX = Ui.UI_W - M - RING_R - 2, GEM_CY = M + RING_R + 2;

    // --- bottom-left: the minimap dial, and the shield and boost arcs round it -------------------------
    static final float MAP_R = 52, ARC_R = MAP_R + 8, MAP_CX = M + ARC_R + 3, MAP_CY = M + ARC_R + 3;

    /** The weapon key printed under the medallion: V alone, B while a session owns V for push-to-talk. */
    static String weaponKey() { return vajra.net.Net.active() ? "B" : "V"; }

    /** Where the classic hull-and-purse line and the crew block sit in this set: under the medallion. */
    static final float VITALS_Y = MED_CY - MED_R - 12, CREW_Y = VITALS_Y - 28;

    private final Ui ui;
    private final ShardGauge shards;
    private final WeaponCards cards;
    private final float[] spot = new float[2], land = new float[3];

    private float gazeShown;                       // eased toward the true gaze
    private float eyeA;                            // the eye's own fade: shut away while he is unaware of you
    private int weaponShown = -1;
    private String weaponStr = "";

    VedicHud(Ui ui, ShardGauge shards, WeaponCards cards) {
        this.ui = ui;
        this.shards = shards;
        this.cards = cards;
    }

    // =================================================================================
    // SHAPES
    // =================================================================================

    void shapes(World w, float dt) {
        tube(w);
        medallion(w);
        lives(w);
        eyeA += ((w.currentRegionIndex() >= 1 && w.gazeStage >= 1 ? 1f : 0f) - eyeA) * Math.min(1f, dt * 3f);
        if (eyeA > 0.02f) eye(w, dt);
        minimap(w);
        shieldAndBoost(w);
        astraDial(w);
    }

    // ---- the health tube: a glass capsule on its side, the hull a liquid that flows in it ----------

    private void tube(World w) {
        Ship sh = w.ship;
        float hp = frac(sh.hp / sh.maxHp());
        float h = TUBE_H / 2f, cy = TUBE_CY, x0 = TUBE_X0, x1 = TUBE_X0 + TUBE_L;
        float t = w.clock;
        float[] c = hp >= 0.5f ? JADE : hp >= 0.3f ? AMBER : VERMILION;
        float cry = hp < 0.3f ? 0.65f + 0.35f * (float) Math.sin(t * 9f) : 1f;      // the low-hull cry

        // the glass: a dark capsule, a little larger than the liquid so a rim of glass shows
        s().setColor(0.03f, 0.05f, 0.08f, 0.82f);
        capsule(x0, x1, cy, h);
        if (hp > 0) {
            float liquidH = h - 1.8f;
            float xe = x0 + 2 * liquidH + (TUBE_L - 2 * liquidH) * hp;              // the liquid's front
            s().setColor(c[0] * 0.62f, c[1] * 0.62f, c[2] * 0.62f, cry);
            capsule(x0 + 1.5f, xe, cy, liquidH);                                      // body, darker
            s().setColor(c[0], c[1], c[2], cry);
            s().rect(x0 + liquidH + 1.5f, cy - 0.5f, Math.max(0, xe - x0 - 2 * liquidH - 1.5f), liquidH - 0.2f); // brighter upper half
            // the flow: soft light bands drifting along the liquid, left to right
            for (float xs = x0 + liquidH + 1.5f; xs < xe - liquidH; xs += 3f) {
                float a = 0.10f * (1f + (float) Math.sin((xs - t * 52f) * 0.115f));
                s().setColor(1, 1, 1, a * cry);
                s().rect(xs, cy - liquidH + 1.5f, 3f, 2 * liquidH - 3f);
            }
            s().setColor(1, 1, 1, 0.5f * cry);                                       // the meniscus at the front
            s().rect(xe - 3.5f, cy - liquidH + 2.2f, 1.2f, 2 * liquidH - 4.4f);
        }
        // the glass catching light along its top
        s().setColor(1, 1, 1, 0.24f);
        s().rect(x0 + h, cy + h - 3.2f, TUBE_L - 2 * h, 1f);

        // gold bands, slanted a little, as on the sketch's tube
        gold(0.92f);
        int bands = 6;
        for (int k = 1; k <= bands; k++) {
            float bx = x0 + 2 * h + k * (TUBE_L - 2 * h) / (bands + 0.5f);
            s().rectLine(bx, cy - h - 1.2f, bx + 2.6f, cy + h + 1.2f, 2.3f);
        }
        // the tube's outline: gold, a rounded end at the far side, a collar where it meets the medallion
        gold(0.55f);
        s().rectLine(x0 + h, cy + h, x1 - h, cy + h, 1.2f);
        s().rectLine(x0 + h, cy - h, x1 - h, cy - h, 1.2f);
        gold(0.95f);
        arc(x1 - h, cy, h, -90, 90, 8, 1.7f);
        s().rect(x0 + 2, cy - h - 2.5f, 3.5f, TUBE_H + 5);
    }

    /** A filled capsule lying along x from x0 to x1, half-height h. */
    private void capsule(float x0, float x1, float cy, float h) {
        if (x1 - x0 < 2 * h) {
            s().circle((x0 + x1) / 2f, cy, h, 14);
            return;
        }
        s().rect(x0 + h, cy - h, x1 - x0 - 2 * h, 2 * h);
        s().circle(x0 + h, cy, h, 14);
        s().circle(x1 - h, cy, h, 14);
    }

    // ---- the weapon medallion ---------------------------------------------------------------

    private void medallion(World w) {
        float cx = MED_CX, cy = MED_CY, r = MED_R;
        s().setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.92f);
        s().circle(cx, cy, r, 40);                                  // covers where the tube runs in
        gold(0.9f);
        for (int q = 0; q < 4; q++) arc(cx, cy, r, q * 90 + 7, q * 90 + 83, 12, 1.7f); // the ring, broken at N, E, S, W
        s().setColor(1, 1, 1, 0.12f);
        arc(cx, cy, r - 4.5f, 0, 360, 36, 1f);
        gold(1f);
        dia(cx, cy + r, 2.6f);                                      // the jewel in the northern gap
    }

    // ---- lives: small rings, whole while they stand, dashed once spent -------------------------

    private void lives(World w) {
        int slots = MathUtils.clamp(Math.max(3, w.lives), 3, World.MAX_LIVES);
        for (int i = 0; i < slots; i++) {
            float x = LIFE_X0 + i * LIFE_PITCH;
            if (i < w.lives) {
                gold(0.95f);
                arc(x, LIFE_Y, LIFE_R, 0, 360, 22, 1.6f);
                s().circle(x, LIFE_Y, LIFE_R - 3.4f, 14);
            } else {
                gold(0.4f);
                for (int q = 0; q < 8; q++) arc(x, LIFE_Y, LIFE_R, q * 45 + 6, q * 45 + 30, 3, 1.5f); // a dashed ring
            }
        }
    }

    // ---- Bala's gaze: the third eye ----------------------------------------------------

    private void eye(World w, float dt) {
        gazeShown += (w.gaze() - gazeShown) * Math.min(1f, dt * 6f);
        float f = frac(gazeShown), ea = eyeA;
        float open = 0.45f + 0.55f * f;                       // the lid lifts as he notices
        float[] t = tint(w.gazeStage);
        float beat = w.gazeStage >= 2 ? 0.75f + 0.25f * (float) Math.sin(w.clock * 4f) : 1f;

        // the almond: two lids, each a curve y = open * H * (1 - u^2)
        gold(0.9f * ea);
        int n = 16;
        for (int i = 0; i < n; i++) {
            float u0 = -1 + 2f * i / n, u1 = -1 + 2f * (i + 1) / n;
            float y0 = open * EYE_H * (1 - u0 * u0), y1 = open * EYE_H * (1 - u1 * u1);
            s().rectLine(EYE_X + EYE_A * u0, EYE_Y + y0, EYE_X + EYE_A * u1, EYE_Y + y1, 1.4f);
            s().rectLine(EYE_X + EYE_A * u0, EYE_Y - y0, EYE_X + EYE_A * u1, EYE_Y - y1, 1.4f);
        }
        float r = Math.min(6.2f, open * EYE_H * 0.8f) * (w.gazeStage >= 3 ? 0.9f + 0.15f * (float) Math.sin(w.clock * 7f) : 1f);
        s().setColor(t[0], t[1], t[2], beat * ea);
        s().circle(EYE_X, EYE_Y, r, 20);
        s().setColor(0, 0, 0, 0.85f * ea);
        s().circle(EYE_X, EYE_Y, r * 0.42f, 12);
        // the gaze spreads out from the eye in six ticks a side; the taller ones are the two stages
        for (int i = 0; i < 6; i++) {
            boolean lit = f * 6f > i + 0.001f;
            boolean notch = i == 1 || i == 3;
            float h = notch ? 12 : 7;
            if (lit) s().setColor(t[0], t[1], t[2], beat * ea);
            else s().setColor(1, 1, 1, (notch ? 0.3f : 0.16f) * ea);
            float dx = EYE_A + 8 + i * 9;
            s().rect(EYE_X + dx, EYE_Y - h / 2f, 2, h);
            s().rect(EYE_X - dx - 2, EYE_Y - h / 2f, 2, h);
        }
    }

    // ---- the ASTRA GEM ---------------------------------------------------------------------------
    // The least thing that says it all. One gem, the loaded astra's own element, in a ring of five cells: a cell for
    // each shard of the five a release costs, lit in the element's colour as they arrive (the Brahmastra's five are the
    // five elements, each lit once it holds its five). A ring that is whole and glowing is a release in hand. While the
    // astra recovers a thin gold line round it drains; while you HOLD F the ring fills from the top in the astra's
    // colour. No numbers and no labels: X and the toast name it, the hangar explains it.

    private int loadedKind(World w) {
        return w.winding() ? w.astraKind : (w.loadedAstra() >= 0 ? w.loadedAstra() : Math.max(0, w.astraSel));
    }

    private void astraDial(World w) {
        Hangar h = w.hangar;
        int kind = loadedKind(w);
        float[] c = World.astraRgb(kind);
        boolean can = Astra.canLoose(h, kind);
        float cell = 360f / Astra.COST, gap = 14f;
        for (int k = 0; k < Astra.COST; k++) {
            float from = 90 - k * cell - gap / 2f, to = from - (cell - gap);
            float[] e = kind == Astra.BRAHMA ? World.TATVA_RGB[k] : c;
            boolean lit = kind == Astra.BRAHMA ? h.shards[k] >= Astra.COST : k < Math.min(Astra.COST, h.shards[kind]);
            if (lit) s().setColor(e[0], e[1], e[2], can ? 1f : 0.85f); else s().setColor(1, 1, 1, 0.14f);
            arc(GEM_CX, GEM_CY, RING_R, from, to, 5, lit ? 2.4f : 1.4f);
        }
        if (w.astraCd > 0) { // recovering: a thin gold line draining clockwise
            gold(0.7f);
            float frac = Math.min(1f, w.astraCd / Astra.COOLDOWN);
            arc(GEM_CX, GEM_CY, RING_R + 5, 90, 90 - 360 * frac, Math.max(2, (int) (36 * frac)), 1f);
        }
        if (w.winding()) { // invoking: the ring fills from the top in the astra's colour, beating once it is full
            float k = Astra.k(w.astraCharge);
            float beat = k >= 1f ? 0.7f + 0.3f * (float) Math.sin(w.clock * 12f) : 1f;
            s().setColor(c[0], c[1], c[2], beat);
            arc(GEM_CX, GEM_CY, RING_R + 5, 90, 90 - 360 * k, Math.max(2, (int) (36 * k)), 2.2f);
            float ta = (float) Math.toRadians(90 - 360 * (Astra.WIND_MIN / Astra.WIND_FULL));
            gold(1f); // where the mantra starts to count
            s().rectLine(GEM_CX + (RING_R + 2) * (float) Math.cos(ta), GEM_CY + (RING_R + 2) * (float) Math.sin(ta),
                    GEM_CX + (RING_R + 8) * (float) Math.cos(ta), GEM_CY + (RING_R + 8) * (float) Math.sin(ta), 1.4f);
        }
        if (kind == Astra.BRAHMA) { // the Brahmastra has no single gem: a gold diamond
            gold(can ? 1f : 0.4f);
            dia(GEM_CX, GEM_CY, 8);
        }
    }

    // ---- the minimap dial --------------------------------------------------------------------

    private static final float MAP_LIM = MAP_R - 6, MAP_STEP = 32, MAP_SCALE = (MAP_R - 3) / 768f, NEAR2 = 400;
    private static final int MAP_N = 24;
    private static final float[] ROCKC = {0.91f, 0.90f, 0.88f};

    private void minimap(World w) {
        float cx = MAP_CX, cy = MAP_CY;
        float sx = MathUtils.round(w.ship.x / MAP_STEP) * MAP_STEP, sy = MathUtils.round(w.ship.y / MAP_STEP) * MAP_STEP;
        float pw = MAP_STEP * MAP_SCALE, r2 = (MAP_R - 3) * (MAP_R - 3);
        // THE MAP IS THE WORLD, and opaque: an ink disc, then the real land (the baked thumbnail) cell by cell
        s().setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1f);
        s().circle(cx, cy, MAP_R - 1.5f, 48);
        WorldThumb thumb = WorldThumb.of(w);
        for (int i = -MAP_N; i <= MAP_N; i++)
            for (int j = -MAP_N; j <= MAP_N; j++) {
                float mx = i * MAP_STEP * MAP_SCALE, my = j * MAP_STEP * MAP_SCALE;
                if (mx * mx + my * my > r2) continue;
                thumb.sample(sx + i * MAP_STEP, sy + j * MAP_STEP, land);
                s().setColor(land[0] * 0.9f, land[1] * 0.9f, land[2] * 0.9f, 1f);
                s().rect(cx + mx - pw / 2 - 0.3f, cy + my - pw / 2 - 0.3f, pw + 0.6f, pw + 0.6f);
                if (thumb.rock(sx + i * MAP_STEP, sy + j * MAP_STEP)) { // the wall tiles stand on the land (baked)
                    s().setColor(ROCKC[0], ROCKC[1], ROCKC[2], 0.92f);
                    s().rect(cx + mx - pw / 2 - 0.3f, cy + my - pw / 2 - 0.3f, pw + 0.6f, pw + 0.6f);
                }
            }
        gold(0.75f);
        arc(cx, cy, MAP_R, 0, 360, 40, 1.6f);
        s().setColor(1, 1, 1, 0.12f);
        arc(cx, cy, MAP_R - 5, 0, 360, 40, 1f);
        for (int i = 0; i < 8; i++) {
            float a = i * 45f, c = MathUtils.cosDeg(a), sn = MathUtils.sinDeg(a);
            float len = i % 2 == 0 ? 7 : 4;
            if (i % 2 == 0) gold(0.85f); else s().setColor(1, 1, 1, 0.35f);
            s().rectLine(cx + c * (MAP_R - len), cy + sn * (MAP_R - len), cx + c * MAP_R, cy + sn * MAP_R, 1.4f);
        }
        contacts(w);
        if (HudCompass.nearestGate(spot, w)) poi(w, 0, GOLD, 1f, spot[0], spot[1]);
        if (HudCompass.nearestShop(spot, w)) poi(w, 1, SHOPC, 1f, spot[0], spot[1]);
        Checkpoint anchor = w.respawnCp;
        boolean drawn = false;
        if (HudCompass.nearestCp(spot, w)) {
            drawn = anchor != null && anchor.x == spot[0] && anchor.y == spot[1];
            poi(w, 2, CHECKC, 1f, spot[0], spot[1]);
        }
        if (anchor != null && !drawn) poi(w, 2, CHECKC, 0.5f, anchor.x, anchor.y);

        float nb = HudCompass.noseBearingDeg(w.ship.facing);
        float ux = MathUtils.sinDeg(nb), uy = MathUtils.cosDeg(nb);
        gold(1f);
        s().triangle(cx + ux * 6, cy + uy * 6, cx - ux * 3 - uy * 3.5f, cy - uy * 3 + ux * 3.5f,
                cx - ux * 3 + uy * 3.5f, cy - uy * 3 - ux * 3.5f);
    }

    private static final float MAP_RANGE2 = (MAP_R - 5) * (MAP_R - 5);

    /** Everything on the dial that helps a decision: the radar sweep, who is awake and who sleeps (leaders, elites, the
     *  keepers and the commander read differently), loot worth the detour, rounds that are coming, the other hulls, the
     *  wrecks Maya asks for. Off the dial, only the commander and a bounty keep an edge marker. */
    private void contacts(World w) {
        float t = w.clock, sweep = (t * 130f) % 360f;
        float sx = w.ship.x, sy = w.ship.y;
        // the sweep: a bright arm and a fading wake behind it
        for (int i = 0; i < 8; i++) {
            float a = sweep - i * 4f;
            s().setColor(GOLD[0], GOLD[1], GOLD[2], 0.22f * (1f - i / 8f));
            s().rectLine(MAP_CX, MAP_CY, MAP_CX + MathUtils.cosDeg(a) * (MAP_R - 5), MAP_CY + MathUtils.sinDeg(a) * (MAP_R - 5), 1.3f);
        }
        // loot: coins as specks, health as a green plus, shards in their tatva, the Vajrayudha piece as a pulsing star
        for (int i = 0; i < w.pickups.size(); i++) {
            vajra.world.Pickup p = w.pickups.get(i);
            float mx = (p.x - sx) * MAP_SCALE, my = (p.y - sy) * MAP_SCALE;
            if (mx * mx + my * my > MAP_RANGE2) continue;
            float x = MAP_CX + mx, y = MAP_CY + my;
            switch (p.type) {
                case vajra.world.Pickup.HEALTH:
                    s().setColor(JADE[0], JADE[1], JADE[2], 0.9f);
                    s().rect(x - 1.6f, y - 0.5f, 3.2f, 1f);
                    s().rect(x - 0.5f, y - 1.6f, 1f, 3.2f);
                    break;
                case vajra.world.Pickup.SHARD: {
                    float[] c = World.TATVA_RGB[MathUtils.clamp(p.variant, 0, World.TATVA_RGB.length - 1)];
                    s().setColor(c[0], c[1], c[2], 0.95f);
                    dia(x, y, 2.3f);
                    break;
                }
                case vajra.world.Pickup.PIECE:
                    s().setColor(1f, 0.92f, 0.5f, 0.7f + 0.3f * MathUtils.sin(t * 6f));
                    dia(x, y, 3.4f + MathUtils.sin(t * 6f));
                    break;
                default:
                    s().setColor(GOLD[0], GOLD[1], GOLD[2], 0.7f);
                    s().rect(x - 0.7f, y - 0.7f, 1.4f, 1.4f);
            }
        }
        // rounds coming at the hull
        int shown = 0;
        for (int i = 0; i < w.bullets.size() && shown < 90; i++) {
            vajra.weapon.Bullet bl = w.bullets.get(i);
            if (!bl.hostile || bl.dead) continue;
            float mx = (bl.x - sx) * MAP_SCALE, my = (bl.y - sy) * MAP_SCALE;
            if (mx * mx + my * my > MAP_RANGE2) continue;
            shown++;
            s().setColor(VERMILION[0], VERMILION[1], VERMILION[2], 0.55f);
            s().rect(MAP_CX + mx - 0.6f, MAP_CY + my - 0.6f, 1.2f, 1.2f);
        }
        // the cast: every living body, as the sweep goes over it
        for (int i = 0; i < w.enemies.size(); i++) {
            vajra.enemy.Enemy e = w.enemies.get(i);
            if (e.dead || e.dismissed || e.ghost || e.bossHidden) continue;
            float dx = e.x - sx, dy = e.y - sy, mx = dx * MAP_SCALE, my = dy * MAP_SCALE, m2 = mx * mx + my * my;
            float ang = MathUtils.atan2(my, mx) * MathUtils.radiansToDegrees;
            float behind = ((sweep - ang) % 360f + 360f) % 360f;           // degrees since the sweep passed it
            float ping = Math.max(0f, 1f - behind / 110f);
            if (e.boss) { // the commander: always on the dial, an edge chevron when beyond it
                float k = m2 > MAP_LIM * MAP_LIM ? MAP_LIM / (float) Math.sqrt(m2) : 1f;
                float x = MAP_CX + mx * k, y = MAP_CY + my * k, pulse = 0.7f + 0.3f * MathUtils.sin(t * 7f);
                s().setColor(VERMILION[0], VERMILION[1], VERMILION[2], pulse);
                dia(x, y, 4.6f);
                arc(x, y, 6.4f, 0, 360, 14, 1f);
                continue;
            }
            if (m2 > MAP_RANGE2) continue;
            float x = MAP_CX + mx, y = MAP_CY + my;
            if (e.voice) { // people are blue: friends, not targets
                s().setColor(SKY[0], SKY[1], SKY[2], 0.95f);
                s().circle(x, y, 2.1f, 8);
                continue;
            }
            float a = (e.active ? 0.62f : 0.32f) + 0.38f * ping;
            if (e.kind.keeper()) { // a post's keeper: amber, square
                s().setColor(AMBER[0], AMBER[1], AMBER[2], a);
                s().rect(x - 2f, y - 2f, 4f, 4f);
            } else {
                float r = e.elite ? 2.8f : e.rank >= 1 ? 2.4f : 1.8f;
                if (e.active) s().setColor(VERMILION[0], VERMILION[1], VERMILION[2], a);
                else s().setColor(0.7f, 0.72f, 0.78f, a);
                s().circle(x, y, r + 0.6f * ping, 8);
                if (e.elite) { // an elite wears a gold ring; a leader a pale one
                    gold(0.95f);
                    arc(x, y, r + 2.2f, 0, 360, 10, 0.9f);
                } else if (e.rank >= 1) {
                    s().setColor(1f, 1f, 1f, 0.55f);
                    arc(x, y, r + 2f, 0, 360, 10, 0.8f);
                }
            }
        }
        // the other hulls of a shared flight
        if (w.partner != null) seatMark(w, w.partner, w.partnerSeat);
        for (World.SeatHull gh : w.extras) if (gh.ship != null) seatMark(w, gh.ship, gh.seat);
        // Maya's wrecks, while she is asking for them
        if (w.missions.salvage == vajra.world.Missions.OPEN) {
            for (int i = 0; i < w.wrecks.length; i++) {
                if (w.wreckTaken[i]) continue;
                float mx = (w.wrecks[i][0] - sx) * MAP_SCALE, my = (w.wrecks[i][1] - sy) * MAP_SCALE;
                if (mx * mx + my * my > MAP_RANGE2) continue;
                s().setColor(SAFFRON[0], SAFFRON[1], SAFFRON[2], 0.95f);
                diaLine(MAP_CX + mx, MAP_CY + my, 3f, 1f);
            }
        }
    }

    /** A remote hull: a small triangle in its seat's colour, held on the dial's edge when it is far. */
    private void seatMark(World w, Ship h, int seat) {
        float mx = (h.x - w.ship.x) * MAP_SCALE, my = (h.y - w.ship.y) * MAP_SCALE, m2 = mx * mx + my * my;
        if (m2 > MAP_LIM * MAP_LIM) {
            float k = MAP_LIM / (float) Math.sqrt(m2);
            mx *= k;
            my *= k;
        }
        com.badlogic.gdx.graphics.Color c = SeatColors.of(seat);
        s().setColor(c.r, c.g, c.b, 1f);
        float x = MAP_CX + mx, y = MAP_CY + my;
        s().triangle(x, y + 3.6f, x - 3f, y - 2.4f, x + 3f, y - 2.4f);
    }

    private void poi(World w, int kind, float[] c, float a, float tx, float ty) {
        float dx = tx - w.ship.x, dy = ty - w.ship.y;
        if (dx * dx + dy * dy < NEAR2) return;
        float mx = dx * MAP_SCALE, my = dy * MAP_SCALE, m2 = mx * mx + my * my;
        if (m2 > MAP_LIM * MAP_LIM) {
            float k = MAP_LIM / (float) Math.sqrt(m2);
            mx *= k;
            my *= k;
        }
        float x = MAP_CX + mx, y = MAP_CY + my;
        s().setColor(c[0], c[1], c[2], a);
        if (kind == 0) dia(x, y, 3.6f);
        else if (kind == 1) arc(x, y, 3.2f, 0, 360, 10, 1.4f);
        else {
            s().rectLine(x - 3, y, x + 3, y, 1.4f);
            s().rectLine(x, y - 3, x, y + 3, 1.4f);
        }
    }

    // ---- shield (left half) and boost (right half), a ring round the minimap ------------------------

    private static final float HALF_SWEEP = 168f; // 6 degrees of gap at the top and the bottom of the ring

    private void shieldAndBoost(World w) {
        Ship sh = w.ship;
        float sp = frac(sh.shieldEnergy / sh.shieldCap());
        float bo = frac(sh.boostEnergy / 100f);
        float t = w.clock;
        // the shield (left) churns while it is up; the boost (right) surges while it burns
        halfTube(270 - 6, -1, sp, SKY, sh.shieldOn ? 1f : 0.8f, t, sh.shieldOn ? 14f : 5f, false);
        halfTube(-90 + 6, 1, bo, SAFFRON, sh.boosting ? 1f : 0.8f, t, sh.boosting ? 26f : 5f, sh.boosting);
        if (sp <= 0) { // a spent shield: a faint red edge, so "down" is not just "empty"
            s().setColor(VERMILION[0], VERMILION[1], VERMILION[2], 0.4f + 0.2f * (float) Math.sin(w.clock * 6f));
            arc(MAP_CX, MAP_CY, ARC_R + 5.6f, 270 - 6, 270 - 6 - HALF_SWEEP, 16, 1f);
        }
        gold(0.9f);
        dia(MAP_CX, MAP_CY + ARC_R, 3.2f);               // the join at the top
        dia(MAP_CX, MAP_CY - ARC_R, 3.2f);               // and at the bottom
    }

    private static final int TUBE_STEPS = 42;
    private static final float TUBE_W = 8.4f;

    /** One half of the ring as a curved GLASS TUBE: a dark glass bore with gold rails, the liquid filling it from `from`
     *  in direction `dir` (+1 = counter-clockwise) to `fill` of the half. The liquid has a darker body and a brighter
     *  top, light bands drift along it at `flow` (faster = more agitated), bubbles rise, and the front is a meniscus
     *  that wobbles. `surge` adds streaks of speed trailing the front. */
    private void halfTube(float from, int dir, float fill, float[] c, float a, float t, float flow, boolean surge) {
        float step = dir * HALF_SWEEP / TUBE_STEPS, r = ARC_R;
        // the bore of the glass
        s().setColor(0.03f, 0.05f, 0.08f, 0.84f);
        arc(MAP_CX, MAP_CY, r, from, from + dir * HALF_SWEEP, TUBE_STEPS, TUBE_W);
        float front = fill * TUBE_STEPS;
        for (int i = 0; i < TUBE_STEPS; i++) {
            float k = MathUtils.clamp(front - i, 0f, 1f);
            if (k <= 0) break;
            float a0 = from + i * step, a1 = a0 + step * k;
            s().setColor(c[0] * 0.58f, c[1] * 0.58f, c[2] * 0.58f, a);             // the body of the liquid
            arcSeg(r - 0.2f, a0, a1, 5.2f);
            s().setColor(c[0], c[1], c[2], a);                                     // its brighter upper half
            arcSeg(r + 0.9f, a0, a1, 2.6f);
            float band = 0.13f * (1f + MathUtils.sin(i * 0.62f - t * flow * dir));  // light bands drifting along it
            s().setColor(1, 1, 1, band * a);
            arcSeg(r, a0, a1, 4.6f);
        }
        if (fill > 0.001f) {
            float af = from + step * front;
            float wob = 0.5f * MathUtils.sin(t * 9f + (surge ? 3f : 0f));          // the meniscus sloshes
            float ca = MathUtils.cosDeg(af), sa = MathUtils.sinDeg(af);
            s().setColor(1, 1, 1, 0.55f * a);
            s().rectLine(MAP_CX + ca * (r - 2.5f), MAP_CY + sa * (r - 2.5f), MAP_CX + ca * (r + 2.5f + wob), MAP_CY + sa * (r + 2.5f + wob), 1.1f);
            // bubbles rise through the liquid
            for (int k = 0; k < 3; k++) {
                float u = ((t * (surge ? 0.7f : 0.22f) + k / 3f) % 1f) * fill;
                float ab = from + dir * HALF_SWEEP * u;
                float off = 1.2f * MathUtils.sin(t * 3f + k * 2f);
                s().setColor(1, 1, 1, 0.45f * a);
                s().circle(MAP_CX + MathUtils.cosDeg(ab) * (r + off), MAP_CY + MathUtils.sinDeg(ab) * (r + off), 0.9f + 0.3f * k, 6);
            }
            if (surge) { // speed streaks behind the front
                for (int k = 1; k <= 3; k++) {
                    s().setColor(1f, 0.95f, 0.8f, 0.5f * (1f - k / 4f));
                    arcSeg(r + 0.9f, af - step * (0.8f * k + 0.5f), af - step * 0.8f * k, 1.2f);
                }
            }
        }
        // the glass catching light along the outside, and the gold rails
        s().setColor(1, 1, 1, 0.22f);
        arc(MAP_CX, MAP_CY, r + 2.1f, from + step * 0.5f, from + dir * HALF_SWEEP - step * 0.5f, TUBE_STEPS / 2, 0.9f);
        gold(0.6f);
        arc(MAP_CX, MAP_CY, r + TUBE_W / 2f + 0.4f, from, from + dir * HALF_SWEEP, TUBE_STEPS / 2, 1f);
        arc(MAP_CX, MAP_CY, r - TUBE_W / 2f - 0.4f, from, from + dir * HALF_SWEEP, TUBE_STEPS / 2, 1f);
        gold(0.95f);
        for (int e = 0; e < 2; e++) { // the end collars
            float ae = e == 0 ? from : from + dir * HALF_SWEEP, ce = MathUtils.cosDeg(ae), se = MathUtils.sinDeg(ae);
            s().rectLine(MAP_CX + ce * (r - TUBE_W / 2f - 1.6f), MAP_CY + se * (r - TUBE_W / 2f - 1.6f),
                    MAP_CX + ce * (r + TUBE_W / 2f + 1.6f), MAP_CY + se * (r + TUBE_W / 2f + 1.6f), 2f);
        }
    }

    /** A short stretch of the ring between two angles, at radius r. */
    private void arcSeg(float r, float a0, float a1, float width) {
        int n = Math.max(1, (int) (Math.abs(a1 - a0) / 3f));
        arc(MAP_CX, MAP_CY, r, a0, a1, n, width);
    }

    // =================================================================================
    // LABELS and ART
    // =================================================================================

    void labels(SpriteBatch batch, World w) {
        refreshWords(w);

        // the weapon: its card in the medallion, its key under it, its name beside the lives
        batch.draw(cards.get(w.hangar.weaponIndex), MED_CX - 17, MED_CY - 13, 34, 34);
        ui.caption.setColor(GOLD[0], GOLD[1], GOLD[2], 0.95f);
        ui.caption.draw(batch, weaponKey(), MED_CX - ui.measure(ui.caption, weaponKey()) / 2f, MED_CY - 17);
        ui.hud.setColor(1, 1, 1, 0.9f);
        int slots = MathUtils.clamp(Math.max(3, w.lives), 3, World.MAX_LIVES);
        ui.hud.draw(batch, weaponStr, LIFE_X0 + (slots - 1) * LIFE_PITCH + LIFE_R + 14, LIFE_Y + 6);
        ui.hud.setColor(1, 1, 1, 1);

        // the gaze's name and word, just above the eye
        if (eyeA > 0.3f) {
            ui.caption.setColor(1, 1, 1, 0.6f * eyeA);
            ui.centered(batch, ui.caption, LokaTable.at(w.lokaIndex).boss.toUpperCase(Locale.ROOT) + "  -  "
                    + STAGE[Math.min(3, w.gazeStage)], EYE_Y + 24);
            ui.caption.setColor(1, 1, 1, 1);
        }

        // the astra's gem, the one thing in the corner
        int kind = loadedKind(w);
        if (kind < Astra.ELEMENTS) {
            batch.setColor(1, 1, 1, Astra.canLoose(w.hangar, kind) && w.astraCd <= 0 ? 1f : 0.5f);
            batch.draw(shards.gem(kind), GEM_CX - GEM_R, GEM_CY - GEM_R, GEM_R * 2, GEM_R * 2);
            batch.setColor(1, 1, 1, 1);
        }
        int rel = Astra.releases(w.hangar, kind);
        if (rel > 1) { // only when there is more than one to loose
            ui.caption.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
            ui.caption.draw(batch, "" + rel, GEM_CX + RING_R - 2, GEM_CY + RING_R + 8);
            ui.caption.setColor(1, 1, 1, 1);
        }

        // where the pilot is: the band's name beside the dial, over the land it names
        String band = vajra.world.Biome.of(vajra.world.Biome.band(w.ship.y, w.regionStart)).name.toUpperCase(Locale.ROOT);
        ui.caption.setColor(GOLD[0], GOLD[1], GOLD[2], 0.85f);
        ui.caption.draw(batch, band, MAP_CX + ARC_R + 12, MAP_CY - ARC_R + 16);
        ui.caption.setColor(1, 1, 1, 1);
        // north, over the dial
        ui.caption.setColor(GOLD[0], GOLD[1], GOLD[2], 0.95f);
        ui.caption.draw(batch, "N", MAP_CX - ui.measure(ui.caption, "N") / 2f, MAP_CY + ARC_R + 17);
        ui.caption.setColor(1, 1, 1, 1);
    }

    private static final String[] STAGE = {"UNAWARE", "NOTICED", "WATCHING", "WAITING"};

    private void refreshWords(World w) {
        if (w.hangar.weaponIndex != weaponShown) {
            weaponShown = w.hangar.weaponIndex;
            weaponStr = WeaponDef.ALL[weaponShown].name.toUpperCase(Locale.ROOT);
        }
    }

    // =================================================================================
    // PRIMITIVES
    // =================================================================================

    private ShapeRenderer s() { return ui.shapes; }

    private void gold(float a) { ui.shapes.setColor(GOLD[0], GOLD[1], GOLD[2], a); }

    private static float frac(float f) { return Math.max(0, Math.min(1, f)); }

    /** A filled diamond. */
    private void dia(float x, float y, float r) {
        s().triangle(x - r, y, x, y + r, x + r, y);
        s().triangle(x - r, y, x, y - r, x + r, y);
    }

    /** A diamond outline. */
    private void diaLine(float x, float y, float r, float w) {
        s().rectLine(x - r, y, x, y + r, w);
        s().rectLine(x, y + r, x + r, y, w);
        s().rectLine(x + r, y, x, y - r, w);
        s().rectLine(x, y - r, x - r, y, w);
    }

    /** A polyline arc from `from` to `to` degrees (0 = east, counter-clockwise), `steps` segments. */
    private void arc(float cx, float cy, float r, float from, float to, int steps, float w) {
        float px = cx + r * MathUtils.cosDeg(from), py = cy + r * MathUtils.sinDeg(from);
        for (int i = 1; i <= steps; i++) {
            float a = from + (to - from) * i / steps;
            float x = cx + r * MathUtils.cosDeg(a), y = cy + r * MathUtils.sinDeg(a);
            s().rectLine(px, py, x, y, w);
            px = x;
            py = y;
        }
    }

    /** Cool cream while he sleeps, gold as he watches, ember once he waits (the classic gaze's own colours). */
    private static float[] tint(int stage) {
        switch (stage) {
            case 0: return new float[] {0.85f, 0.85f, 0.75f};
            case 1: return new float[] {0.95f, 0.78f, 0.34f};
            case 2: return new float[] {1f, 0.55f, 0.15f};
            default: return new float[] {1f, 0.3f, 0.2f};
        }
    }
}

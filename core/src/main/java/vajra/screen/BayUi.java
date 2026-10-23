package vajra.screen;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import vajra.ship.Hangar;
import vajra.ship.ShipDef;
import vajra.weapon.WeaponDef;
import vajra.world.LokaTable;
import vajra.world.World;

/** THE BAY - the hangar, drawn as temple panels. One screen, three rails (TAB or click):
 *
 *  <ul>
 *  <li><b>HULLS</b> - a strip of seven hull cards and, under it, the three tiers of the selected
 *      hull. A hull you do not own is a LOCKED silhouette - nobody sees what a ship looks like until
 *      it is bought - and so is every tier you have not bought.</li>
 *  <li><b>ARMS</b> - the weapon wheel: the patterns round a ring, the fitted one lit, a pattern the
 *      pilot's loka does not offer yet a lock. The hub holds the chosen pattern, its bars and its
 *      three ranks (III adds an ability).</li>
 *  <li><b>ASTRA</b> - one card per loka: the element, the forge's progress, what you carry. The one
 *      marked SELECTED is what F spends.</li>
 *  </ul>
 *
 *  Gold means yours, the accent means focus, dim means not yet. Every box is Ornament.panel - the
 *  same chamfered, double-hairlined temple panel the cutscene bubbles and the pages wear. Nothing
 *  here decides a purchase: BayController turns a key or click into a call on the hangar. Two passes
 *  like every screen: {@link #drawBackground} (shapes) and {@link #draw} (batch). */
public final class BayUi {
    // --- the rails ------------------------------------------------------------------------
    public static final int HULLS = 0, ARMS = 1, ASTRA = 2;
    public static final String[] RAIL_NAMES = {"HULLS", "ARMS", "ASTRA"};
    // --- what a click can land on ---------------------------------------------------------
    public static final int NONE = -1, RAIL = 0, HULL = 1, TIER = 2, ARM = 3, ASTRA_CARD = 4, SELL = 5;

    // --- layout (UI units, y up) ----------------------------------------------------------
    public static final float HEAD_Y = 672, RULE_Y = 640, FOOT_Y = 34, STATUS_Y = 64;
    static final float STRIP_W = 148, STRIP_H = 188, STRIP_GAP = 12;
    static final float STRIP_X0 = (Ui.UI_W - (7 * STRIP_W + 6 * STRIP_GAP)) / 2f;
    static final float STRIP_Y = 432;
    static final float TIER_W = 336, TIER_H = 250, TIER_GAP = 24;
    static final float TIER_X0 = (Ui.UI_W - (3 * TIER_W + 2 * TIER_GAP)) / 2f;
    static final float TIER_Y = 130;
    static final float WHEEL_CX = Ui.UI_W / 2f, WHEEL_CY = 378, WHEEL_R = 200, NODE = 48;
    static final float HUB_W = 256, HUB_H = 210;
    static final float AST_W = 150, AST_H = 330, AST_GAP = 12;
    static final float AST_X0 = (Ui.UI_W - (LokaTable.COUNT * AST_W + (LokaTable.COUNT - 1) * AST_GAP)) / 2f;
    static final float AST_Y = 196;
    static final float SELL_X = 840;
    private static final float CUT = 6, SEL_LIFT = 8;

    private final Ui ui;
    private final GlyphLayout layout = new GlyphLayout();
    private int rail, hull, tierFocus = -1, arm, astra;
    private int hover = NONE, hoverIdx = -1, hitRegion = NONE, hitIdx = -1;
    private final float[] tabX = new float[RAIL_NAMES.length], tabW = new float[RAIL_NAMES.length];

    /** `ui` may be null in a headless test: the layout then uses a fixed tab width and nothing draws. */
    public BayUi(Ui ui) {
        this.ui = ui;
        float total = -56;
        for (int i = 0; i < tabW.length; i++) {
            tabW[i] = ui == null ? 60f : ui.measure(ui.hud, RAIL_NAMES[i]);
            total += tabW[i] + 56;
        }
        float x = Ui.UI_W / 2f - total / 2f;
        for (int i = 0; i < tabW.length; i++) {
            tabX[i] = x;
            x += tabW[i] + 56;
        }
    }

    // --- state ----------------------------------------------------------------------------
    public int rail() { return rail; }
    public int hull() { return hull; }
    public int tierFocus() { return tierFocus; }
    public int arm() { return arm; }
    public int astra() { return astra; }
    public int hitRegion() { return hitRegion; }
    public int hitIndex() { return hitIdx; }

    public void setRail(int r) { rail = Math.floorMod(r, RAIL_NAMES.length); }
    public void setHull(int i) { if (i >= 0 && i < ShipDef.ALL.length) hull = i; }
    public void setTierFocus(int t) { tierFocus = Math.max(-1, Math.min(2, t)); }
    public void setArm(int i) { arm = Math.floorMod(i, WeaponDef.ALL.length); }
    public void setAstra(int i) { astra = Math.floorMod(i, LokaTable.COUNT); }

    /** Open on what the run is actually flying and carrying. */
    public void syncTo(World w) {
        hull = w.hangar.shipIndex;
        arm = w.hangar.weaponIndex;
        astra = w.astraLoka;
        tierFocus = -1;
    }

    // --- geometry (pure, so the headless test can pin it) ---------------------------------
    public static float stripX(int i) { return STRIP_X0 + i * (STRIP_W + STRIP_GAP); }
    public static float tierX(int t) { return TIER_X0 + t * (TIER_W + TIER_GAP); }
    public static float astraX(int i) { return AST_X0 + i * (AST_W + AST_GAP); }

    /** The centre of weapon node i on the wheel: the first sits at the top, then clockwise. */
    public static float[] nodeAt(int i) {
        double a = Math.PI / 2 - i * 2 * Math.PI / WeaponDef.ALL.length;
        return new float[] {WHEEL_CX + (float) Math.cos(a) * WHEEL_R, WHEEL_CY + (float) Math.sin(a) * WHEEL_R};
    }

    // --- hit testing ----------------------------------------------------------------------
    public boolean hover(Vector2 m) {
        probe(m);
        hover = hitRegion;
        hoverIdx = hitIdx;
        return hitRegion != NONE;
    }

    public boolean click(Vector2 m) {
        probe(m);
        return hitRegion != NONE;
    }

    private void probe(Vector2 m) {
        hitRegion = NONE;
        hitIdx = -1;
        for (int i = 0; i < tabX.length; i++)
            if (Template.box(m, tabX[i] - 28, HEAD_Y - 22, tabX[i] + tabW[i] + 28, HEAD_Y + 22)) {
                hitRegion = RAIL;
                hitIdx = i;
                return;
            }
        if (Template.box(m, SELL_X, HEAD_Y - 22, SELL_X + 180, HEAD_Y + 22)) {
            hitRegion = SELL;
            return;
        }
        if (rail == HULLS) {
            for (int i = 0; i < ShipDef.ALL.length; i++)
                if (Template.box(m, stripX(i), STRIP_Y, stripX(i) + STRIP_W, STRIP_Y + STRIP_H)) {
                    hitRegion = HULL;
                    hitIdx = i;
                    return;
                }
            for (int t = 0; t < 3; t++)
                if (Template.box(m, tierX(t), TIER_Y, tierX(t) + TIER_W, TIER_Y + TIER_H)) {
                    hitRegion = TIER;
                    hitIdx = t;
                    return;
                }
        } else if (rail == ARMS) {
            for (int i = 0; i < WeaponDef.ALL.length; i++) {
                float[] n = nodeAt(i);
                if (Template.box(m, n[0] - NODE / 2 - 4, n[1] - NODE / 2 - 4, n[0] + NODE / 2 + 4, n[1] + NODE / 2 + 4)) {
                    hitRegion = ARM;
                    hitIdx = i;
                    return;
                }
            }
        } else {
            for (int i = 0; i < LokaTable.COUNT; i++)
                if (Template.box(m, astraX(i), AST_Y, astraX(i) + AST_W, AST_Y + AST_H)) {
                    hitRegion = ASTRA_CARD;
                    hitIdx = i;
                    return;
                }
        }
    }

    // =========================================================== labels (pure)
    /** The highest tier of a hull you own, as the art variation to show (1..3). */
    public static int artLevel(Hangar h, int ship) {
        for (int t = 2; t >= 0; t--) if (h.owned[ship][t]) return t + 1;
        return 1;
    }

    /** The short name a hull wears until it is bought: its mark only. */
    public static String markName(ShipDef d) {
        return "MK-" + (d.id + 1);
    }

    public static String hullState(Hangar h, ShipDef d, int loka) {
        if (h.shipIndex == d.id) return "FLYING";
        if (h.ownsShip(d.id)) return "OWNED";
        if (!LokaTable.shipUnlocked(loka, d.id)) return "OPENS " + LokaTable.at(d.minLoka).name.toUpperCase();
        return "LOCKED  " + d.cost + "c";
    }

    public static boolean hullOpen(Hangar h, ShipDef d, int loka) {
        return h.ownsShip(d.id) || LokaTable.shipUnlocked(loka, d.id);
    }

    /** What a tier changes, in the player's words. */
    public static String tierEffect(ShipDef d, int tier) {
        ShipDef.Upgrade u = d.upgrades[tier];
        StringBuilder s = new StringBuilder();
        if (u.damageMult != 1f) add(s, "DAMAGE x" + trim(u.damageMult));
        if (u.cooldownMult != 1f) add(s, "RELOAD x" + trim(u.cooldownMult));
        if (u.dashCdMult != 1f) add(s, "DASH x" + trim(u.dashCdMult));
        if (u.hullBonus != 0) add(s, "HULL +" + (int) u.hullBonus);
        if (u.shieldBonus != 0) add(s, "SHIELD +" + (int) u.shieldBonus);
        if (u.speedBonus != 0) add(s, "SPEED +" + (int) u.speedBonus);
        if (u.turnBonus != 0) add(s, "TURN +" + (int) u.turnBonus);
        return s.length() == 0 ? "THE HULL ITSELF" : s.toString();
    }

    public static boolean tierAffordable(Hangar h, ShipDef d, int t) {
        if (t == 0) return h.coins >= d.cost;
        return h.coins >= d.upgrades[t].cost && (t != 2 || h.shardCount() >= Hangar.tierShards(d));
    }

    /** True when a tier has been bought (tier 0 is the hull itself). */
    public static boolean tierOwned(Hangar h, ShipDef d, int t) {
        return t == 0 ? h.ownsShip(d.id) : h.owned[d.id][t];
    }

    /** True when this tier is the next thing that can be bought on this hull. */
    public static boolean tierNext(Hangar h, ShipDef d, int t) {
        return !tierOwned(h, d, t) && (t == 0 || (h.ownsShip(d.id) && tierOwned(h, d, t - 1)));
    }

    /** The line at the foot of a tier card: owned, or what it costs, or why it cannot be had. */
    public static String tierState(Hangar h, ShipDef d, int t, int loka) {
        if (t == 0) return h.ownsShip(d.id) ? "OWNED" : hullOpen(h, d, loka)
                ? d.cost + "c" + (tierAffordable(h, d, 0) ? "   ENTER BUYS" : "   SHORT")
                : "OPENS " + LokaTable.at(d.minLoka).name.toUpperCase();
        if (h.owned[d.id][t]) return "OWNED";
        if (!h.ownsShip(d.id)) return "OWN THE HULL FIRST";
        if (t == 2 && !h.owned[d.id][1]) return "BUY " + d.upgrades[1].label + " FIRST";
        String price = t == 2 ? d.upgrades[t].cost + "c + " + Hangar.tierShards(d) + " shards" : d.upgrades[t].cost + "c";
        return price + (tierAffordable(h, d, t) ? "   ENTER BUYS" : "   SHORT");
    }

    public static String weaponState(Hangar h, WeaponDef w) {
        if (h.weaponIndex == w.idx) return "FITTED";
        if (h.weaponOwned[w.idx]) return "OWNED";
        return w.cost + "c" + (h.coins >= w.cost ? "   ENTER BUYS" : "   SHORT");
    }

    /** The second line under a pattern: what ENTER will do next - fit it, buy it, or temper it. */
    public static String weaponHint(Hangar h, WeaponDef w) {
        if (!h.weaponOffered(w.idx)) return "OPENS " + LokaTable.at(w.minLoka).name.toUpperCase();
        if (!h.weaponOwned[w.idx]) return h.coins >= w.cost ? "ENTER BUYS" : "SHORT OF " + (w.cost - h.coins) + "c";
        int r = h.weaponRank[w.idx];
        if (h.weaponIndex != w.idx) return "ENTER FITS";
        if (r >= WeaponDef.RANKS - 1) return "FULLY TEMPERED";
        String price = w.rankCost(r) + "c" + (w.rankShards(r) > 0 ? " + " + w.rankShards(r) + " shards" : "");
        boolean can = h.coins >= w.rankCost(r) && h.shardCount() >= w.rankShards(r);
        return "ENTER TEMPERS TO RANK " + roman(r + 1) + "  " + price + (can ? "" : "  SHORT");
    }

    public static String roman(int rankIndex) {
        return rankIndex == 0 ? "I" : rankIndex == 1 ? "II" : "III";
    }

    /** A pattern's bars, 0..1 against the best of the fifteen: power per volley, rate of fire. */
    public static float[] weaponBars(WeaponDef w) {
        float maxPow = 0, maxRate = 0;
        for (WeaponDef o : WeaponDef.ALL) {
            maxPow = Math.max(maxPow, power(o));
            maxRate = Math.max(maxRate, rate(o));
        }
        return new float[] {Math.min(1f, power(w) / maxPow), Math.min(1f, rate(w) / maxRate)};
    }

    private static float power(WeaponDef w) { return w.damage * w.count * Math.max(1, w.burst); }
    private static float rate(WeaponDef w) { return 1f / Math.max(0.05f, w.cooldown + w.chargeTime); }

    /** The one-line traits of a pattern. */
    public static String traits(WeaponDef d) {
        StringBuilder t = new StringBuilder();
        if (d.homingTurn > 0) add(t, "HOMING");
        if (d.pierceEnemies) add(t, "PIERCE");
        if (d.pierceWalls) add(t, "WALLBORE");
        if (d.aimAssist) add(t, "AIM ASSIST");
        if (d.beam) add(t, "BEAM");
        if (d.aoeRadius > 0) add(t, "SPLASH");
        if (d.chargeTime > 0) add(t, "CHARGED");
        if (d.count > 1) add(t, "x" + d.count + " SPREAD");
        return t.length() == 0 ? "PLAIN BOLT" : t.toString();
    }

    /** An Astra card's state: sealed beyond the deepest loka, banking toward the forge, forged,
     *  or carrying charges. */
    public static String astraState(Hangar h, int loka, int deepest) {
        if (loka > deepest && !h.isForged(loka)) return "SEALED";
        if (h.forgeCharges[loka] > 0)
            return h.forgeCharges[loka] + (h.forgeCharges[loka] == 1 ? " CHARGE" : " CHARGES");
        if (h.isForged(loka)) return "FORGED";
        return h.forgeProgress[loka] + " / " + Hangar.FORGE_COST + " TO FORGE";
    }

    public static float astraFill(Hangar h, int loka) {
        return Math.max(0f, Math.min(1f, h.forgeProgress[loka] / (float) Math.max(1, h.forgeNeed(loka))));
    }

    private static void add(StringBuilder s, String part) {
        if (s.length() > 0) s.append("  ");
        s.append(part);
    }

    private static String trim(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    // =========================================================== drawing: shape pass
    private void bar(float x, float y, float w, float frac, float r, float g, float b, float a) {
        ui.shapes.setColor(1, 1, 1, 0.12f * a);
        ui.shapes.rect(x, y, w, 3);
        ui.shapes.setColor(r, g, b, a);
        ui.shapes.rect(x, y, w * Math.max(0f, Math.min(1f, frac)), 3);
    }

    /** Pips for a rank: three diamonds, filled up to the rank held (held < 0 = none yet). */
    private void pips(float x, float y, int held, float r, float g, float b) {
        for (int i = 0; i < WeaponDef.RANKS; i++) {
            ui.shapes.setColor(r, g, b, i <= held ? 1f : 0.28f);
            Ornament.diamond(ui, x + i * 16, y, i <= held ? 5f : 3.5f);
        }
    }

    public void drawBackground(World w) {
        Hangar h = w.hangar;
        float ar = Template.accentR, ag = Template.accentG, ab = Template.accentB;
        Template.dim(ui, 1f);
        Ornament.frame(ui);
        Ornament.divider(ui, Ui.UI_W / 2f, RULE_Y, 470, 0.5f);
        ui.shapes.setColor(ar, ag, ab, 1f);
        ui.shapes.rect(tabX[rail], HEAD_Y - 14, tabW[rail], 2);
        if (rail == HULLS) hullShapes(h, w.lokaIndex, ar, ag, ab);
        else if (rail == ARMS) armShapes(h, ar, ag, ab);
        else astraShapes(w, ar, ag, ab);
    }

    private void hullShapes(Hangar h, int loka, float ar, float ag, float ab) {
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        float[] mx = {0, 0, 0};
        for (ShipDef o : ShipDef.ALL) {
            mx[0] = Math.max(mx[0], o.maxHp);
            mx[1] = Math.max(mx[1], o.maxSpeed);
            mx[2] = Math.max(mx[2], o.turnRate);
        }
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            ShipDef d = ShipDef.ALL[i];
            boolean sel = i == hull, own = h.ownsShip(i), open = hullOpen(h, d, loka);
            float x = stripX(i), y = STRIP_Y + (sel ? SEL_LIFT : 0);
            boolean focus = sel && tierFocus < 0;
            if (own) Ornament.panel(ui, x, y, STRIP_W, STRIP_H, CUT, gr, gg, gb, focus ? 0.95f : 0.5f, focus);
            else Ornament.panel(ui, x, y, STRIP_W, STRIP_H, CUT, ar, ag, ab, focus ? 0.9f : open ? 0.3f : 0.14f, focus);
            if (own) {
                bar(x + 50, y + 42, 84, d.maxHp / mx[0], UiTheme.HULL_R, UiTheme.HULL_G, UiTheme.HULL_B, 1f);
                bar(x + 50, y + 31, 84, d.maxSpeed / mx[1], UiTheme.BOOST_R, UiTheme.BOOST_G, UiTheme.BOOST_B, 1f);
                bar(x + 50, y + 20, 84, d.turnRate / mx[2], UiTheme.SHIELD_R, UiTheme.SHIELD_G, UiTheme.SHIELD_B, 1f);
            } else { // a hull you do not own is a lock, not a picture
                ui.shapes.setColor(ar, ag, ab, open ? 0.75f : 0.3f);
                Ornament.lock(ui, x + STRIP_W / 2f, y + STRIP_H - 62, 2.2f);
            }
        }
        ShipDef d = ShipDef.ALL[hull];
        for (int t = 0; t < 3; t++) {
            boolean own = tierOwned(h, d, t), next = tierNext(h, d, t), focus = tierFocus == t;
            float x = tierX(t);
            if (own) Ornament.panel(ui, x, TIER_Y, TIER_W, TIER_H, CUT + 2, gr, gg, gb, 0.85f, focus);
            else if (next) Ornament.panel(ui, x, TIER_Y, TIER_W, TIER_H, CUT + 2, ar, ag, ab, 0.7f, focus);
            else Ornament.panel(ui, x, TIER_Y, TIER_W, TIER_H, CUT + 2, 1, 1, 1, 0.16f, focus);
            if (!own) {
                ui.shapes.setColor(ar, ag, ab, next ? 0.8f : 0.3f);
                Ornament.lock(ui, x + TIER_W / 2f, TIER_Y + TIER_H - 96, 3.4f);
            }
            if (t < 2) { // the chain between tiers: a gold link once the lower one is yours
                ui.shapes.setColor(own ? gr : 1, own ? gg : 1, own ? gb : 1, own ? 0.9f : 0.2f);
                ui.shapes.rect(x + TIER_W, TIER_Y + TIER_H / 2f, TIER_GAP, 2);
                Ornament.diamond(ui, x + TIER_W + TIER_GAP / 2f, TIER_Y + TIER_H / 2f + 1, 3f);
            }
        }
    }

    private void armShapes(Hangar h, float ar, float ag, float ab) {
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        ui.shapes.setColor(1, 1, 1, 0.14f); // the rim: a dotted ring through the nodes
        for (int k = 0; k < 120; k++) {
            double a = k * Math.PI * 2 / 120;
            ui.shapes.rect(WHEEL_CX + (float) Math.cos(a) * WHEEL_R - 1, WHEEL_CY + (float) Math.sin(a) * WHEEL_R - 1, 2, 2);
        }
        for (int i = 0; i < WeaponDef.ALL.length; i++) {
            float[] n = nodeAt(i);
            boolean sel = i == arm, own = h.weaponOwned[i], fit = h.weaponIndex == i, offered = h.weaponOffered(i);
            float s = NODE + (sel ? 8 : 0);
            float x = n[0] - s / 2, y = n[1] - s / 2;
            if (fit) Ornament.panel(ui, x, y, s, s, 5, gr, gg, gb, 1f, false);
            else if (own) Ornament.panel(ui, x, y, s, s, 5, gr, gg, gb, sel ? 0.9f : 0.4f, false);
            else Ornament.panel(ui, x, y, s, s, 5, ar, ag, ab, sel ? 0.9f : offered ? 0.28f : 0.14f, false);
            if (!offered) {
                ui.shapes.setColor(ar, ag, ab, sel ? 0.9f : 0.4f);
                Ornament.lock(ui, n[0], n[1] - 2, 1.0f);
            }
        }
        float hx = WHEEL_CX - HUB_W / 2f, hy = WHEEL_CY - HUB_H / 2f;
        Ornament.panel(ui, hx, hy, HUB_W, HUB_H, 9, ar, ag, ab, 0.6f, true);
        if (h.weaponOffered(arm)) {
            float[] bars = weaponBars(h.weapon(arm));
            bar(hx + 78, hy + 124, HUB_W - 100, bars[0], 1f, 0.45f, 0.15f, 1f);
            bar(hx + 78, hy + 110, HUB_W - 100, bars[1], UiTheme.SHIELD_R, UiTheme.SHIELD_G, UiTheme.SHIELD_B, 1f);
            pips(hx + 84, hy + 80, h.weaponOwned[arm] ? h.weaponRank[arm] : -1, gr, gg, gb);
        }
    }

    private void astraShapes(World w, float ar, float ag, float ab) {
        Hangar h = w.hangar;
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        for (int i = 0; i < LokaTable.COUNT; i++) {
            boolean sel = i == astra, sealed = i > w.lokaIndex && !h.isForged(i);
            float[] c = World.TATVA_RGB[LokaTable.at(i).shards[0]];
            float x = astraX(i), y = AST_Y + (sel ? SEL_LIFT : 0);
            boolean active = w.astraLoka == i && h.isForged(i);
            if (active) Ornament.panel(ui, x, y, AST_W, AST_H, CUT, gr, gg, gb, 0.95f, sel);
            else if (h.isForged(i)) Ornament.panel(ui, x, y, AST_W, AST_H, CUT, c[0], c[1], c[2], sel ? 0.9f : 0.5f, sel);
            else Ornament.panel(ui, x, y, AST_W, AST_H, CUT, 1, 1, 1, sel ? 0.5f : sealed ? 0.1f : 0.2f, sel);
            if (h.isForged(i)) {
                ui.shapes.setColor(c[0], c[1], c[2], 0.16f); // a glow behind the element once the weapon is forged
                ui.shapes.circle(x + AST_W / 2f, y + AST_H - 112, 40);
            }
            bar(x + 20, y + AST_H - 196, AST_W - 40, astraFill(h, i), c[0], c[1], c[2], sealed ? 0.3f : 1f);
        }
    }

    // =========================================================== drawing: batch pass
    private void centered(SpriteBatch b, BitmapFont f, String s, float cx, float y) {
        f.draw(b, s, cx - ui.measure(f, s) / 2f, y);
    }

    public void draw(SpriteBatch b, World w, ShipRenderer ships, WeaponCards cards) {
        Hangar h = w.hangar;
        float ar = Template.accentR, ag = Template.accentG, ab = Template.accentB;
        ui.title.setColor(ar, ag, ab, 1f);
        ui.title.draw(b, "HANGAR", 96, HEAD_Y + 12);
        ui.title.setColor(1, 1, 1, 1);
        for (int i = 0; i < RAIL_NAMES.length; i++) {
            boolean on = i == rail;
            ui.hud.setColor(on ? ar : 1, on ? ag : 1, on ? ab : 1, on ? 1f : hover == RAIL && hoverIdx == i ? 0.85f : 0.5f);
            ui.hud.draw(b, RAIL_NAMES[i], tabX[i], HEAD_Y);
        }
        ui.hud.setColor(1, 1, 1, 1);
        wallet(b, h, w);
        if (rail == HULLS) hullText(b, w, ships);
        else if (rail == ARMS) armText(b, w, cards);
        else astraText(b, w);
        ui.hud.setColor(1, 1, 1, 0.5f);
        centered(b, ui.hud, "FLYING " + ShipDef.ALL[h.shipIndex].name.toUpperCase() + "     FITTED "
                + WeaponDef.ALL[h.weaponIndex].name.toUpperCase() + " " + roman(h.weaponRank[h.weaponIndex]),
                Ui.UI_W / 2f, STATUS_Y);
        ui.hud.setColor(1, 1, 1, 1);
        Template.keys(ui, b, FOOT_Y, "TAB", "RAIL", "A/D", "MOVE", "W/S", "ROW", "ENTER", "BUY / FIT / TEMPER", "X", "SELL SHARDS", "ESC", "CLOSE");
    }

    private void wallet(SpriteBatch b, Hangar h, World w) {
        float x1 = Ui.UI_W - 100;
        String coins = Integer.toString(h.coins);
        float cw = ui.measure(ui.hud, coins);
        b.draw(ui.icons.coin(), x1 - cw - 24, HEAD_Y - 14, 18, 18);
        ui.hud.draw(b, coins, x1 - cw, HEAD_Y);
        int el = LokaTable.at(Math.max(0, w.lokaIndex)).shards[0];
        String sh = Integer.toString(h.shardCount());
        float sw = ui.measure(ui.hud, sh);
        float sx = x1 - cw - 24 - 28 - sw;
        b.draw(ui.icons.shard(el), sx - 24, HEAD_Y - 14, 18, 18);
        ui.hud.draw(b, sh, sx, HEAD_Y);
        if (h.shardCount() > 0) {
            ui.caption.setColor(1, 1, 1, hover == SELL ? 1f : 0.55f);
            ui.caption.draw(b, "SELL SHARDS  X  +" + h.shardCount() * Hangar.SHARD_SELL + "c", SELL_X, HEAD_Y - 2);
            ui.caption.setColor(1, 1, 1, 1);
        }
    }

    private void hullText(SpriteBatch b, World w, ShipRenderer ships) {
        Hangar h = w.hangar;
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            ShipDef d = ShipDef.ALL[i];
            boolean sel = i == hull, open = hullOpen(h, d, w.lokaIndex), own = h.ownsShip(i);
            float x = stripX(i), y = STRIP_Y + (sel ? SEL_LIFT : 0), cx = x + STRIP_W / 2f;
            TextureRegion r = ships.hull(i, artLevel(h, i));
            if (r != null && own) b.draw(r, cx - 32, y + STRIP_H - 94, 64, 64);
            ui.hud.setColor(1, 1, 1, open ? 1f : 0.5f);
            centered(b, ui.hud, own ? d.name.toUpperCase() : markName(d), cx, y + STRIP_H - 110);
            boolean fly = h.shipIndex == i;
            String st = hullState(h, d, w.lokaIndex);
            ui.caption.setColor(own || fly ? gr : 1, own || fly ? gg : 1, own || fly ? gb : 1, own || fly ? 1f : open ? 0.75f : 0.45f);
            centered(b, ui.caption, st, cx, y + STRIP_H - 128);
            if (own) {
                ui.caption.setColor(1, 1, 1, 0.55f);
                ui.caption.draw(b, "HP", x + 14, y + 48);
                ui.caption.draw(b, "SPD", x + 14, y + 37);
                ui.caption.draw(b, "TURN", x + 14, y + 26);
            }
            ui.caption.setColor(1, 1, 1, 1);
            ui.hud.setColor(1, 1, 1, 1);
        }
        // the tier cards: the art of a tier is shown only once the tier is yours
        ShipDef d = ShipDef.ALL[hull];
        for (int t = 0; t < 3; t++) {
            boolean own = tierOwned(h, d, t), next = tierNext(h, d, t);
            float x = tierX(t), cx = x + TIER_W / 2f, top = TIER_Y + TIER_H;
            ui.hud.setColor(own ? gr : 1, own ? gg : 1, own ? gb : 1, own ? 1f : next ? 1f : 0.55f);
            ui.hud.draw(b, d.upgrades[t].label + "   " + (own ? d.upgrades[t].name.toUpperCase() : "???"), x + 22, top - 28);
            TextureRegion r = ships.hull(d.id, t + 1);
            if (r != null && own) b.draw(r, cx - 48, top - 140, 96, 96);
            ui.hud.setColor(1, 1, 1, own || next ? 0.85f : 0.45f);
            centered(b, ui.hud, tierEffect(d, t), cx, top - 170);
            String st = tierState(h, d, t, w.lokaIndex);
            boolean afford = !own && next && tierAffordable(h, d, t);
            ui.hud.setColor(own ? gr : 1, own ? gg : 1, own ? gb : 1, own ? 1f : afford ? 1f : 0.5f);
            centered(b, ui.hud, st, cx, TIER_Y + 26);
            ui.hud.setColor(1, 1, 1, 1);
        }
    }

    private void armText(SpriteBatch b, World w, WeaponCards cards) {
        Hangar h = w.hangar;
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        for (int i = 0; i < WeaponDef.ALL.length; i++) {
            float[] n = nodeAt(i);
            boolean sel = i == arm, own = h.weaponOwned[i], offered = h.weaponOffered(i);
            float s = NODE - 14 + (sel ? 8 : 0);
            TextureRegion r = cards.get(i);
            if (r != null && offered) {
                b.setColor(1, 1, 1, own ? 1f : 0.4f);
                b.draw(r, n[0] - s / 2, n[1] - s / 2, s, s);
                b.setColor(1, 1, 1, 1);
            }
            if (offered && !own) {
                ui.caption.setColor(1, 1, 1, sel ? 0.9f : 0.45f);
                centered(b, ui.caption, WeaponDef.ALL[i].cost + "c", n[0], n[1] - NODE / 2 - 8);
                ui.caption.setColor(1, 1, 1, 1);
            }
        }
        WeaponDef d = WeaponDef.ALL[arm];
        float hx = WHEEL_CX - HUB_W / 2f, hy = WHEEL_CY - HUB_H / 2f;
        boolean offered = h.weaponOffered(arm), own = h.weaponOwned[arm];
        TextureRegion r = cards.get(arm);
        if (r != null && offered) b.draw(r, hx + 20, hy + HUB_H - 76, 56, 56);
        ui.menu.setColor(own ? gr : 1, own ? gg : 1, own ? gb : 1, 1f);
        ui.menu.draw(b, offered ? d.name.toUpperCase() : "LOCKED", hx + 86, hy + HUB_H - 32);
        ui.menu.setColor(1, 1, 1, 1);
        ui.caption.setColor(1, 1, 1, 0.55f);
        if (offered) {
            layout.setText(ui.caption, traits(h.weapon(arm)), ui.caption.getColor(), HUB_W - 110, Align.left, true);
            ui.caption.draw(b, layout, hx + 86, hy + HUB_H - 52);
            ui.caption.draw(b, "POWER", hx + 18, hy + 131);
            ui.caption.draw(b, "RATE", hx + 18, hy + 117);
            ui.caption.draw(b, "RANK", hx + 18, hy + 86);
            int rk = h.weaponRank[arm];
            boolean has3 = own && rk >= WeaponDef.RANKS - 1;
            ui.caption.setColor(has3 ? gr : 1, has3 ? gg : 1, has3 ? gb : 1, has3 ? 1f : 0.5f);
            ui.caption.draw(b, "III  " + d.ability, hx + 136, hy + 86);
        }
        ui.caption.setColor(1, 1, 1, 1);
        String st = offered ? weaponState(h, d) : "NOT YET OFFERED";
        boolean fit = h.weaponIndex == arm;
        ui.hud.setColor(own || fit ? gr : 1, own || fit ? gg : 1, own || fit ? gb : 1, own || fit ? 1f : h.coins >= d.cost ? 1f : 0.55f);
        centered(b, ui.hud, st, WHEEL_CX, hy + 52);
        ui.hud.setColor(1, 1, 1, 1);
        layout.setText(ui.caption, weaponHint(h, d), ui.caption.getColor(), HUB_W - 30, Align.center, true);
        ui.caption.setColor(1, 1, 1, 0.6f);
        ui.caption.draw(b, layout, hx + 15, hy + 34);
        ui.caption.setColor(1, 1, 1, 1);
    }

    private void astraText(SpriteBatch b, World w) {
        Hangar h = w.hangar;
        float gr = UiTheme.GOLD_R, gg = UiTheme.GOLD_G, gb = UiTheme.GOLD_B;
        for (int i = 0; i < LokaTable.COUNT; i++) {
            LokaTable.Loka l = LokaTable.at(i);
            boolean sel = i == astra, forged = h.isForged(i), sealed = i > w.lokaIndex && !forged;
            float x = astraX(i), y = AST_Y + (sel ? SEL_LIFT : 0), cx = x + AST_W / 2f, top = y + AST_H;
            float[] c = World.TATVA_RGB[l.shards[0]];
            ui.hud.setColor(1, 1, 1, sealed ? 0.4f : 1f);
            centered(b, ui.hud, l.name.toUpperCase(), cx, top - 26);
            b.setColor(c[0], c[1], c[2], forged ? 1f : sealed ? 0.18f : 0.5f);
            b.draw(ui.icons.shard(l.shards[0]), cx - 32, top - 144, 64, 64);
            b.setColor(1, 1, 1, 1);
            ui.caption.setColor(1, 1, 1, sealed ? 0.35f : 0.7f);
            centered(b, ui.caption, World.astraName(i), cx, top - 164);
            String st = astraState(h, i, w.lokaIndex);
            boolean active = w.astraLoka == i && forged;
            ui.hud.setColor(forged ? c[0] : 1, forged ? c[1] : 1, forged ? c[2] : 1, sealed ? 0.35f : 1f);
            layout.setText(ui.hud, st, ui.hud.getColor(), AST_W - 20, Align.center, true);
            ui.hud.draw(b, layout, x + 10, top - 218);
            if (active) {
                ui.caption.setColor(gr, gg, gb, 1f);
                centered(b, ui.caption, "SELECTED  F SPENDS IT", cx, y + 26);
            } else if (forged) {
                ui.caption.setColor(1, 1, 1, 0.6f);
                centered(b, ui.caption, "ENTER TO SELECT", cx, y + 26);
            }
            ui.caption.setColor(1, 1, 1, 1);
            ui.hud.setColor(1, 1, 1, 1);
        }
        ui.hud.setColor(1, 1, 1, 0.6f);
        centered(b, ui.hud, "EVERY ASTRA YOU FORGE COMES WITH YOU. F RELEASES THE SELECTED ONE.", Ui.UI_W / 2f, 150);
        ui.hud.setColor(1, 1, 1, 1);
    }
}

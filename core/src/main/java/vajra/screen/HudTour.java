package vajra.screen;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

/** THE TOUR OF THE INSTRUMENTS: the first time a pilot flies (and whenever they ask for the tutorial again), the world holds still after the opening and a SPOTLIGHT goes round the HUD, one
 *  instrument at a time. The screen is dimmed except where the light lies; the light glides from one instrument to the next (and breathes), and a card says what the instrument is and how it is
 *  used, with a thin line to it. ENTER or a click goes to the next, ESC ends the tour. Pure UI: GameScreen holds the sim still while it runs, and records that it was seen. */
final class HudTour {
    /** One stop: the instrument's box in UI space (x, y, w, h), its name and its words. */
    private static final class Stop {
        final float x, y, w, h;
        final String title;
        final String[] lines;

        Stop(float x, float y, float w, float h, String title, String... lines) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.title = title;
            this.lines = lines;
        }
    }

    private final Ui ui;
    private final GlyphLayout gl = new GlyphLayout();
    private final List<Stop> stops = new ArrayList<>();
    private boolean active;
    private int idx;
    private float t, cx, cy, cw, ch;

    HudTour(Ui ui) { this.ui = ui; }

    boolean active() { return active; }

    int count() { return stops.size(); }

    int index() { return idx; }

    /** The tutorial of every new game: the instruments of the HUD, then the controls, one small card at a time (no page of keys, no wall of text). `full` is kept for callers: both parts always play. */
    void begin(boolean full) { begin(full, Ui.UI_W / 2f, Ui.UI_H / 2f); }

    /** As {@link #begin(boolean)}, with the hull's place on the screen (UI units) so the controls are taught on the ship itself. */
    void begin(boolean full, float shipX, float shipY) {
        this.shipUx = shipX;
        this.shipUy = shipY;
        stops.clear();
        addInstrumentStops();
        addControlStops();
        idx = 0;
        t = 0f;
        Stop first = stops.get(0);
        cx = first.x + first.w / 2f;
        cy = first.y + first.h / 2f;
        cw = first.w;
        ch = first.h;
        active = true;
    }

    /** The controls, as this pilot has them set (a rebound key shows as rebound), each on the hull itself. */
    private void addControlStops() {
        vajra.Settings st = vajra.profile.Profiles.active().settings;
        float sx = shipUx - 70f, sy = shipUy - 70f;
        stops.add(new Stop(sx, sy, 140f, 140f, "FLY",
                vajra.Keymap.UP.name(st) + " " + vajra.Keymap.LEFT.name(st) + " " + vajra.Keymap.DOWN.name(st) + " " + vajra.Keymap.RIGHT.name(st) + " OR THE ARROWS MOVE THE HULL. UP IS ALWAYS UP THE SCREEN.",
                vajra.Keymap.TURN_LEFT.name(st) + " AND " + vajra.Keymap.TURN_RIGHT.name(st) + " TURN THE NOSE. HOLD THE LEFT MOUSE BUTTON AND IT FOLLOWS THE CURSOR."));
        stops.add(new Stop(sx, sy, 140f, 140f, "FIGHT",
                vajra.Keymap.FIRE.name(st) + " OR THE LEFT BUTTON FIRES ALONG THE NOSE.",
                vajra.Keymap.BOOST.name(st) + " BOOSTS WHILE YOU FLY. " + vajra.Keymap.SHIELD.name(st) + " OR THE RIGHT BUTTON HOLDS THE SHIELD."));
        stops.add(new Stop(sx, sy, 140f, 140f, "ARMS AND ASTRAS",
                vajra.Keymap.WEAPON_NEXT.name(st) + " AND " + vajra.Keymap.WEAPON_PREV.name(st) + " CHANGE WEAPON. HOLD THE FIRST FOR THE WHEEL.",
                "HOLD " + vajra.Keymap.ASTRA.name(st) + " TO FORM AN ASTRA AND LET GO TO LOOSE IT. " + vajra.Keymap.ASTRA_CYCLE.name(st) + " CHOOSES. (2ND AND 3RD HULLS ONLY)"));
        stops.add(new Stop(sx, sy, 140f, 140f, "REPAIR, HULLS, PAUSE",
                vajra.Keymap.REPAIR.name(st) + " SPENDS A STORED REPAIR. " + vajra.Keymap.HULL_NEXT.name(st) + " OR 2-7 CHANGES TO A HULL YOU OWN.",
                "ESC PAUSES. EVERY KEY CAN BE CHANGED IN SETTINGS, UNDER CONTROLS."));
    }

    private void addInstrumentStops() {
        float m = VedicHud.M;
        stops.add(new Stop(VedicHud.TUBE_X0 - 6, VedicHud.TUBE_CY - 18, VedicHud.TUBE_L + 12, 36, "THE HULL",
                "THE TUBE IS YOUR SHIP. IT DRAINS AS YOU ARE HIT, AND TURNS AMBER, THEN RED.",
                "WHEN IT EMPTIES, A LIFE IS SPENT AND YOU WAKE AT THE LAST LAMP YOU BOUND."));
        stops.add(new Stop(VedicHud.LIFE_X0 - 14, VedicHud.LIFE_Y - 14, 3 * VedicHud.LIFE_PITCH + 6, 28, "LIVES",
                "THREE TO BEGIN WITH, FIVE AT MOST. THERE IS NO GAME OVER:",
                "WITH NONE LEFT, YOU STILL WAKE AT A LAMP, AND FLY ON."));
        stops.add(new Stop(VedicHud.TUBE_X0 + VedicHud.TUBE_L + 4, VedicHud.TUBE_CY - 14, 5 * 15 + 36, 28, "STORED REPAIRS",
                "A REPAIR THAT FINDS YOUR HULL WHOLE IS KEPT, UP TO FIVE.",
                "PRESS 1 TO SPEND ONE: IT MENDS 40% OF THE HULL."));
        stops.add(new Stop(VedicHud.MED_CX - 40, VedicHud.MED_CY - 40, 80, 96, "THE WEAPON",
                "THE PATTERN YOU FIRE. V STEPS TO THE NEXT ONE YOU OWN, B TO THE LAST.",
                "HOLD V FOR THE WHEEL, AND POINT AT ONE. NEW PATTERNS ARE BOUGHT IN A BAY."));
        stops.add(new Stop(VedicHud.MAP_CX - VedicHud.ARC_R - 10, VedicHud.MAP_CY - VedicHud.ARC_R - 10, 2 * VedicHud.ARC_R + 20, 2 * VedicHud.ARC_R + 34, "THE DIAL",
                "THE SWEEP SHOWS FOES, LOOT, LAMPS, BAYS AND THE RULER.",
                "THE LEFT ARC IS THE SHIELD (HOLD G OR THE RIGHT BUTTON). THE RIGHT ARC IS THE BOOST (HOLD SHIFT WHILE FLYING)."));
        stops.add(new Stop(Ui.UI_W / 2f - 150, Ui.UI_H - 52, 300, 42, "THE SCORE",
                "CHAIN KILLS WITHOUT BEING HIT TO RAISE THE COMBO. A HIT BREAKS THE CHAIN."));
        stops.add(new Stop(VedicHud.GEM_CX - VedicHud.RING_R - 12, VedicHud.GEM_CY - VedicHud.RING_R - 12, 2 * VedicHud.RING_R + 24, 2 * VedicHud.RING_R + 24, "THE ASTRA",
                "FIVE SHARDS OF AN ELEMENT PAY FOR ONE RELEASE OF ITS ASTRA. THE RING FILLS AS YOU COLLECT THEM.",
                "HOLD F TO FORM THE MANDALA, AND LET GO TO LOOSE IT. X CHOOSES WHICH ASTRA. ONLY A HULL'S 2ND AND 3RD VARIANTS CAN FORGE ONE."));
        stops.add(new Stop(Ui.UI_W - 176, Ui.UI_H / 2f - 110, 170, 150, "THE LOKA",
                "THE LOKA YOU FLY IN, ITS RULER, AND THE DEPTH IN YOJANAS FROM THE GRAND GATE."));
        stops.add(new Stop(VajraPanel.PX - 6, VajraPanel.PY - 6, VajraPanel.PW + 12, VajraPanel.PH + 12, "THE VAJRA",
                "WHEN A RULER LETS GO OF HIS PIECE OF THE BOLT, IT IS COUNTED HERE: ONE OF SEVEN.",
                "IT IS EMPTY NOW. IT WILL NOT STAY SO."));
    }

    void next() {
        if (!active) return;
        if (++idx >= stops.size()) active = false;
        t = 0f;
    }

    void skip() { active = false; }

    void update(float dt) {
        if (!active) return;
        t += dt;
        Stop s = stops.get(idx);
        float k = Math.min(1f, dt * 7f);
        cx += (s.x + s.w / 2f - cx) * k;
        cy += (s.y + s.h / 2f - cy) * k;
        cw += (s.w - cw) * k;
        ch += (s.h - ch) * k;
    }

    void render(SpriteBatch batch) {
        if (!active) return;
        Stop s = stops.get(idx);
        float pad = 10f + 2.5f * MathUtils.sin(t * 5f), x0 = cx - cw / 2f - pad, y0 = cy - ch / 2f - pad, w = cw + 2 * pad, h = ch + 2 * pad;
        float a = Math.min(1f, t * 3f + 0.4f);

        // the card: where it lies depends on where the light is (it keeps to the far side)
        gl.setText(ui.hud, joined(s.lines), ui.hud.getColor(), 440f, com.badlogic.gdx.utils.Align.left, true);
        float cardW = 500f, cardH = 96f + gl.height, cardX = cx < Ui.UI_W / 2f ? Ui.UI_W / 2f + 40f : Ui.UI_W / 2f - 40f - cardW, cardY = cy > Ui.UI_H / 2f ? 250f : Ui.UI_H - 250f - cardH;

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        ui.shapes.setColor(0f, 0f, 0f, 0.74f * a);
        ui.shapes.rect(0, 0, Ui.UI_W, y0);                                       // the dimming, in four pieces round the light
        ui.shapes.rect(0, y0 + h, Ui.UI_W, Ui.UI_H - y0 - h);
        ui.shapes.rect(0, y0, x0, h);
        ui.shapes.rect(x0 + w, y0, Ui.UI_W - x0 - w, h);
        float pulse = 0.65f + 0.35f * MathUtils.sin(t * 6f);
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.22f * pulse);   // a glow just outside the frame
        for (int i = 1; i <= 3; i++) {
            float g = i * 4f;
            ui.shapes.rect(x0 - g, y0 - g, w + 2 * g, 2);
            ui.shapes.rect(x0 - g, y0 + h + g - 2, w + 2 * g, 2);
            ui.shapes.rect(x0 - g, y0 - g, 2, h + 2 * g);
            ui.shapes.rect(x0 + w + g - 2, y0 - g, 2, h + 2 * g);
        }
        Template.panelBorder(ui, x0, y0, w, h);
        float lx = cardX + (cx < Ui.UI_W / 2f ? 0f : cardW), ly = cardY + cardH / 2f;                 // the line from the card to the light
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.6f * a);
        ui.shapes.rectLine(lx, ly, cx, cy, 1.4f);
        Ornament.panel(ui, cardX, cardY, cardW, cardH, 8, Template.accentR, Template.accentG, Template.accentB, 0.9f, false);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        ui.menu.setColor(Template.accentR, Template.accentG, Template.accentB, 1f);
        ui.menu.draw(batch, s.title, cardX + 28f, cardY + cardH - 26f);
        ui.menu.setColor(1, 1, 1, 1);
        ui.hud.setColor(1f, 0.97f, 0.9f, 1f);
        ui.hud.draw(batch, joined(s.lines), cardX + 28f, cardY + cardH - 56f, 440f, com.badlogic.gdx.utils.Align.left, true);
        ui.hud.setColor(1, 1, 1, 1);
        ui.caption.setColor(0.8f, 0.76f, 0.62f, 0.9f);
        ui.caption.draw(batch, "ENTER  NEXT      ESC  SKIP THE TOUR      " + (idx + 1) + " / " + stops.size(), cardX + 28f, cardY + 22f);
        ui.caption.setColor(1, 1, 1, 1);
        batch.end();
    }

    private float shipUx = Ui.UI_W / 2f, shipUy = Ui.UI_H / 2f;

    private static String joined(String[] lines) { return String.join("\n", lines); }
}

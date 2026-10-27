package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import vajra.flow.Flow;
import vajra.flow.FlowState;

/** THE CONTROL SCREEN — the full key and mouse reference for the run.
 *
 *  <p>Pushed from the home menu (and from pause). Lists every flight, hangar and system
 *  binding so the player never has to alt-tab out to remember them. ESC or a click returns
 *  to the screen that pushed it. */
public class ControlScreen implements FlowState {
    private static final float SUB_Y = Template.TITLE_Y - 26; // the subtitle, right under the heading
    private static final float ROW_Y = 468;
    private static final float PITCH = 24;
    private static final float COL_X = 96;
    private static final float KEY_X = 330;

    private static final String[][] ROWS = {
            {"W A S D / arrows", "Thrust - screen relative. W is always up, D always right. Never turns the ship."},
            {"Q / E", "Turn the nose by hand - Q counter-clockwise, E clockwise."},
            {"SPACE / LMB", "Fire. SPACE fires along the nose; LMB also swings the nose onto the cursor."},
            {"SHIFT", "Boost while thrusting - drains the energy meter."},
            {"G / RMB", "Shield - drains while held. Cost scales with what actually lands."},
            {"V / B", "Next / previous weapon you own."},
            {"1", "Spend a stored repair: 40% of the hull. A repair drop that finds the hull whole is kept for later, up to five."},
            {"2 - 7", "Fit that hull - one you have not bought is a no-op."},
            {"Z", "Next hull you own."},
            {"F", "HOLD to invoke the astra you have chosen: a cage of cubes gathers and foes in front of the nose are locked. LET GO to loose it. Five shards a release, paid at the loosing."},
            {"X", "Choose which astra F invokes: Akasha, Vayu, Agni, Jala, Prithvi, the Brahmastra of all five, or the Pashupatastra."},
            {"ENTER", "In the hangar: buy or fit the selected card. TAB changes section."},
            {"ESC", "Back, close, or pause - always the way out."},
            {"R", "A new run, once the last one has ended."},
            {"F11 / F12", "Fullscreen / swap the window between 1280x720 and 1920x1080."},
    };

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;

    public ControlScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
    }

    @Override
    public void update(float dt) {
        // a still reference page
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            flow.pop();
            return true;
        }
        return false;
    }

    @Override
    public void render() {
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f); // opaque ink field
        Ornament.frame(ui);
        Ornament.titleMarkers(ui, "CONTROLS", Template.TITLE_Y);
        Ornament.panel(ui, 60, 100, 1160, 396, 9, Template.accentR, Template.accentG, Template.accentB, 0.55f, false);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        Template.title(ui, batch, "CONTROLS", Template.TITLE_Y, 1f);
        Template.sub(ui, batch, "Every binding the run uses", SUB_Y, 1f);
        for (int i = 0; i < ROWS.length; i++) {
            float y = ROW_Y - i * PITCH;
            ui.hud.setColor(Template.accentR, Template.accentG, Template.accentB, 1f);
            ui.hud.draw(batch, label(i), COL_X, y);
            ui.hud.setColor(1, 1, 1, 0.75f);
            ui.hud.draw(batch, ROWS[i][1], KEY_X, y);
        }
        ui.hud.setColor(1, 1, 1, 1);
        Template.keys(ui, batch, UiTheme.FOOT_Y, "ESC", "BACK");
        batch.end();
    }

    /** The key column as THIS pilot has set it (the rows that name a flight key follow the settings; the others are fixed). */
    private static String label(int i) {
        vajra.Settings st = vajra.profile.Profiles.active().settings;
        switch (i) {
            case 0: return vajra.Keymap.UP.name(st) + " " + vajra.Keymap.LEFT.name(st) + " " + vajra.Keymap.DOWN.name(st) + " " + vajra.Keymap.RIGHT.name(st) + " / arrows";
            case 1: return vajra.Keymap.TURN_LEFT.name(st) + " / " + vajra.Keymap.TURN_RIGHT.name(st);
            case 2: return vajra.Keymap.FIRE.name(st) + " / LMB";
            case 3: return vajra.Keymap.BOOST.name(st);
            case 4: return vajra.Keymap.SHIELD.name(st) + " / RMB";
            case 5: return vajra.Keymap.WEAPON_NEXT.name(st) + " / " + vajra.Keymap.WEAPON_PREV.name(st);
            case 6: return vajra.Keymap.REPAIR.name(st);
            case 8: return vajra.Keymap.HULL_NEXT.name(st);
            case 9: return vajra.Keymap.ASTRA.name(st);
            case 10: return vajra.Keymap.ASTRA_CYCLE.name(st);
            default: return ROWS[i][0];
        }
    }

    @Override
    public void dispose() {}
}

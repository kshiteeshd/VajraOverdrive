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
            {"1 - 7", "Fit that hull - one you have not bought is a no-op."},
            {"Z", "Next hull you own."},
            {"T", "Wear the next colourway (a, b, c) of the hull you fly."},
            {"F", "HOLD to invoke the astra you have chosen: a cage of cubes gathers and foes in front of the nose are locked. LET GO to loose it. Five shards a release, paid at the loosing."},
            {"X", "Choose which astra F invokes: Akasha, Vayu, Agni, Jala, Prithvi, or the Brahmastra of all five."},
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
            ui.hud.draw(batch, ROWS[i][0], COL_X, y);
            ui.hud.setColor(1, 1, 1, 0.75f);
            ui.hud.draw(batch, ROWS[i][1], KEY_X, y);
        }
        ui.hud.setColor(1, 1, 1, 1);
        Template.keys(ui, batch, UiTheme.FOOT_Y, "ESC", "BACK");
        batch.end();
    }

    @Override
    public void dispose() {}
}

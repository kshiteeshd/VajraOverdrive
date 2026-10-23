package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.update.Updater;

/** The title screen: an opaque ink field, the pixel logo, a temple divider, and its text rows:
 *  CONTINUE (only when the pilot left a run mid-flight) / NEW GAME / MULTIPLAYER / TUTORIALS /
 *  PROFILE / SETTINGS / UPDATE / QUIT.
 *
 *  <p>The look is a vector-display take on temple geometry, all in the theme's one gold on ink:
 *  a hairline frame with corner brackets, a line-diamond-line divider, and diamond markers
 *  flanking the selected row. Nothing is filled heavily; every stroke is a hairline at low
 *  alpha, so the logo and the menu stay the loudest things.
 *
 *  <p>ENTER or a click activates the selected row. CONTINUE wakes the saved run where it was parked
 *  (quit-to-title, a closed window and the quiet autosave all park it); NEW GAME starts fresh, and
 *  when a save exists it asks once before erasing it. UPDATE is the
 *  in-game swapper: it pulls the newest setup exe per vajra.update.Updater and lets the
 *  installer take over. */
public class HomeScreen implements FlowState {
    /** The rows with no save to continue: NEW GAME leads. */
    static final String[] MENU =
            {"NEW GAME", "MULTIPLAYER", "TUTORIALS", "PROFILE", "SETTINGS", "UPDATE", "QUIT"};
    static final String CONTINUE = "CONTINUE";
    static final String SURE = "ERASE SAVE?  ENTER AGAIN";
    private static final float MENU_Y = 284;  // first row baseline: up to eight rows, tighter pitch
    private static final float PITCH = 28;    // row pitch (rows abut at HALF_H 14, no dead zone)
    private static final float HALF_W = 200;  // row hit half-width
    private static final float HALF_H = 14;   // row hit half-height

    /** The rows for a pilot: CONTINUE on top when a run is parked. */
    static String[] rows(boolean hasRun) {
        if (!hasRun) return MENU.clone();
        String[] r = new String[MENU.length + 1];
        r[0] = CONTINUE;
        System.arraycopy(MENU, 0, r, 1, MENU.length);
        return r;
    }

    // The logo, at 2x the size it had: the 500x300 asset drawn at 800x480. The asset carries a
    // lot of transparent padding (its ink is only 330x149), so the drawn box is much taller
    // than the visible text - the ink lands on 526x238, exactly twice the old 263x119.
    static final float LOGO_W = 800, LOGO_H = 480, LOGO_BOTTOM = 232;

    // The divider sits clear of the logo ink above and the menu below. The frame, divider and
    // row markers live in Ornament - PauseScreen draws the same three.
    private static final float DIVIDER_Y = 316;

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final Texture logo =
            new Texture(Gdx.files.internal("brand/vajra-overdrive-logo.png"));
    private final UiMenu menu;
    private final Updater updater;
    private final boolean hasRun;   // a parked run exists: CONTINUE is the first row
    private final String[] labels;
    private boolean sure;           // NEW GAME was pressed once over a save

    public HomeScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        this.hasRun = vajra.profile.Profiles.active().hasRun();
        this.labels = rows(hasRun);
        this.menu = new UiMenu(labels, MENU_Y, PITCH, HALF_W, HALF_H);
        updater = new Updater();
        Template.setAccent(UiTheme.GOLD_R, UiTheme.GOLD_G, UiTheme.GOLD_B); // back to brand gold
        logo.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    }

    // --- the vertical budget, exposed so a test can check it collides with nothing -----------

    static float menuY() { return MENU_Y; }
    static float pitch() { return PITCH; }
    static float halfH() { return HALF_H; }
    static float dividerY() { return DIVIDER_Y; }

    /** Menu cursor, wrapping. Package-private: the headless test pins the wrap. */
    static int move(int sel, int dir) {
        return UiMenu.wrap(sel, dir, MENU.length);
    }

    /** The row's name in the no-save list: CONTINUE shifts every row down by one. */
    private int row() { return menu.selected() - (hasRun ? 1 : 0); }

    @Override
    public boolean keyDown(int keycode) {
        if (menu.keyDown(keycode)) return true;
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            activate();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        if (menu.click(ui.at(sx, sy)) >= 0) {
            activate();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int sx, int sy) {
        menu.hover(ui.at(sx, sy));
        return false;
    }

    private void activate() {
        if (hasRun && menu.selected() == 0) { // CONTINUE: the parked run, where it hung
            flow.set(GameScreen.resume(batch, flow, ui, vajra.profile.Profiles.active().run));
            return;
        }
        int sel = row();
        if (sel == 0) newGame();
        else if (sel == 1) flow.push(new NetScreen(flow, ui, batch));
        else if (sel == 2) flow.push(new TutorialScreen(flow, ui, batch));
        else if (sel == 3) flow.push(new ProfileScreen(flow, ui, batch));
        else if (sel == 4) flow.push(new SettingsScreen(flow, ui, batch));
        else if (sel == 5) updater.begin();
        else Gdx.app.exit();
    }

    /** NEW GAME. Over a parked run it asks once: the first press arms the row, the second erases. */
    private void newGame() {
        if (hasRun && !sure) {
            sure = true;
            labels[1] = SURE;
            return;
        }
        vajra.profile.Profiles.clearRun();
        flow.set(new GameScreen(batch, flow, ui));
    }

    @Override
    public void update(float dt) {
        if (sure && menu.selected() != 1) { // wandered off the armed row: it disarms
            sure = false;
            labels[1] = MENU[0];
        }
    }

    @Override
    public void render() {
        float cx = Ui.UI_W / 2f;

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f); // opaque: nothing behind this screen shows through
        Ornament.frame(ui);
        Ornament.divider(ui, cx, DIVIDER_Y, 200, 0.6f);
        int sel = menu.selected();
        if (sel >= 0 && sel < menu.count())
            Ornament.markers(ui, cx, menu.label(sel), menu.rowY(sel));
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        batch.draw(logo, Template.COL - LOGO_W / 2, LOGO_BOTTOM, LOGO_W, LOGO_H);
        for (int i = 0; i < menu.count(); i++)
            Template.menuRow(ui, batch, menu.label(i), menu.selected(i), menu.rowY(i));
        if (updater.message().isEmpty()) // the hint row answers to the updater while it works
            Template.keys(ui, batch, UiTheme.FOOT_Y, "W/S", "SELECT", "ENTER / CLICK", "CONFIRM");
        else
            Template.sub(ui, batch, updater.message(), UiTheme.FOOT_Y, 0.85f);
        batch.end();
    }

    // --- ornament (shape pass) ---------------------------------------------------------
    // The geometry lives in Ornament: PauseScreen draws the same frame, divider and markers, and
    // two hand-copied sets drift the moment one of them is tuned.

    @Override
    public void dispose() {
        logo.dispose();
    }
}
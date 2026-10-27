package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import vajra.Keymap;
import vajra.Settings;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.update.Updater;

/** SETTINGS: everything the title screen no longer holds, in six tabs. Q / E (or a click on a tab) change the tab, W / S choose a row, A / D (or a click on its left or right half) change its value,
 *  ENTER acts on it, ESC leaves. Every change is kept at once, and display changes take effect at once.
 *  <ul>
 *   <li><b>DISPLAY</b>: fullscreen, the window's size, vsync, the frame limit, the frame readout;</li>
 *   <li><b>AUDIO</b>: master, music, effects and voices (kept for the sound that is coming);</li>
 *   <li><b>GAMEPLAY</b>: screen shake, popup size, dialogue speed, the tutorial hints and a replay of the tutorial;</li>
 *   <li><b>CONTROLS</b>: every flight key, rebindable (ENTER, then the new key; a key another action holds swaps with it), and a reset;</li>
 *   <li><b>ACCESS</b>: reduced flashing, a gentler camera;</li>
 *   <li><b>MORE</b>: the tutorials, the key reference, the pilot's profile, the update check, a reset of every setting.</li>
 *  </ul> */
public class SettingsScreen implements FlowState {
    static final String[] TABS = {"DISPLAY", "AUDIO", "GAMEPLAY", "CONTROLS", "ACCESS", "MORE", "TEST"};
    private static final float ROW_TOP = 492, HALF_W = 330, TAB_Y = 520;

    /** One line of a tab: a label, its value as text, what A / D do to it, what ENTER does, and a hint under the list while it is chosen. */
    private static final class Row {
        final String label, hint;
        final Supplier<String> value;
        final IntConsumer adjust;
        final Runnable act;

        Row(String label, Supplier<String> value, IntConsumer adjust, Runnable act, String hint) {
            this.label = label;
            this.value = value;
            this.adjust = adjust;
            this.act = act;
            this.hint = hint;
        }
    }

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final Settings settings = vajra.profile.Profiles.active().settings;
    private final Updater updater = new Updater();
    private final List<List<Row>> tabs = new ArrayList<>();
    private int tab, sel;
    private Keymap rebinding;
    private boolean sure;
    private final com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    public SettingsScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        build();
    }

    // ---- the tabs -------------------------------------------------------------------------------------------------------------------

    private static String pct(float v) { return Math.round(v * 100) + "%"; }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private static float step(float v, int dir, float by, float lo, float hi) { return Math.max(lo, Math.min(hi, Math.round((v + by * dir) / by) * by)); }

    private static int cycle(int i, int dir, int n) { return Math.floorMod(i + dir, n); }

    private void keep() { vajra.profile.Profiles.save(); }

    private void apply() {
        applyDisplay(settings);
        keep();
    }

    private void build() {
        List<Row> d = new ArrayList<>();
        d.add(new Row("FULLSCREEN", () -> onOff(Gdx.graphics.isFullscreen()), dir -> toggleFullscreen(), SettingsScreen::toggleFullscreen, "F11 does the same."));
        d.add(new Row("WINDOW SIZE", () -> settings.windowW + " x " + settings.windowH, dir -> {
            int i = 0;
            for (int k = 0; k < Settings.WINDOWS.length; k++) if (Settings.WINDOWS[k][0] == settings.windowW) i = k;
            i = cycle(i, dir, Settings.WINDOWS.length);
            settings.windowW = Settings.WINDOWS[i][0];
            settings.windowH = Settings.WINDOWS[i][1];
            if (!Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(settings.windowW, settings.windowH);
            keep();
        }, null, "The size of the window when it is not fullscreen."));
        d.add(new Row("VSYNC", () -> onOff(settings.vsync), dir -> { settings.vsync = !settings.vsync; apply(); }, () -> { settings.vsync = !settings.vsync; apply(); }, "Locks the picture to the screen's refresh: no tearing."));
        d.add(new Row("FRAME LIMIT", () -> settings.fpsCap == 0 ? "UNLIMITED" : settings.fpsCap + " FPS", dir -> {
            int i = 1;
            for (int k = 0; k < Settings.FPS_CAPS.length; k++) if (Settings.FPS_CAPS[k] == settings.fpsCap) i = k;
            settings.fpsCap = Settings.FPS_CAPS[cycle(i, dir, Settings.FPS_CAPS.length)];
            apply();
        }, null, "Lower it to save power; unlimited on a fast machine."));
        d.add(new Row("SHOW FRAME RATE", () -> onOff(settings.showFps), dir -> { settings.showFps = !settings.showFps; apply(); }, () -> { settings.showFps = !settings.showFps; apply(); }, "A readout of the frame time and the frame rate (F3)."));
        tabs.add(d);

        List<Row> a = new ArrayList<>();
        a.add(vol("MASTER VOLUME", () -> settings.master, v -> settings.master = v, "Everything."));
        a.add(vol("MUSIC", () -> settings.music, v -> settings.music = v, "The score."));
        a.add(vol("EFFECTS", () -> settings.sfx, v -> settings.sfx = v, "Guns, engines, astras."));
        a.add(vol("VOICES", () -> settings.voice, v -> settings.voice = v, "Spoken lines and the voice channel of a shared flight."));
        tabs.add(a);

        List<Row> g = new ArrayList<>();
        g.add(vol("SCREEN SHAKE", () -> settings.shake, v -> settings.shake = v, "How hard the camera shakes."));
        g.add(new Row("POPUP SIZE", () -> pct(settings.popupScale), dir -> { settings.popupScale = step(settings.popupScale, dir, 0.25f, 0f, 2f); keep(); }, null, "The size of floating text."));
        g.add(new Row("DIALOGUE SPEED", () -> pct(settings.textSpeed), dir -> { settings.textSpeed = step(settings.textSpeed, dir, 0.25f, 0.5f, 2f); apply(); }, null, "How fast a spoken line types out."));
        g.add(new Row("TUTORIAL HINTS", () -> onOff(settings.hints), dir -> { settings.hints = !settings.hints; keep(); }, () -> { settings.hints = !settings.hints; keep(); },
                "The tour of the instruments, the first sight of a lamp or a bay, and the lessons that come with the flight."));
        g.add(new Row("REPLAY THE TUTORIAL", () -> "", null, () -> { vajra.profile.Profiles.active().beats = new boolean[8]; vajra.profile.Profiles.active().lessons = null; keep(); flow.pop(); },
                "The tour and the first sights play again in your next flight."));
        tabs.add(g);

        List<Row> c = new ArrayList<>();
        for (Keymap k : Keymap.values())
            c.add(new Row(k.label, () -> rebinding == k ? "PRESS A KEY..." : k.name(settings), null, () -> rebinding = k, "ENTER, then the new key. A key another action holds swaps with it. ESC cancels."));
        c.add(new Row("RESET ALL KEYS", () -> "", null, () -> { Keymap.reset(settings); keep(); }, "Every action back on the key it ships on."));
        tabs.add(c);

        List<Row> x = new ArrayList<>();
        x.add(new Row("REDUCE FLASHING", () -> onOff(settings.reduceFlash), dir -> { settings.reduceFlash = !settings.reduceFlash; keep(); }, () -> { settings.reduceFlash = !settings.reduceFlash; keep(); },
                "No white flashes and no flaring bands of light (the shattering glass, a door, a burst)."));
        x.add(new Row("GENTLE CAMERA", () -> onOff(settings.gentleCamera), dir -> { settings.gentleCamera = !settings.gentleCamera; keep(); }, () -> { settings.gentleCamera = !settings.gentleCamera; keep(); },
                "The camera stays with your hull: it does not go to a boss's astra or sweep to a gate."));
        x.add(new Row("SCREEN SHAKE", () -> pct(settings.shake), dir -> { settings.shake = step(settings.shake, dir, 0.25f, 0f, 1f); keep(); }, null, "The same setting as under GAMEPLAY."));
        x.add(new Row("DIALOGUE SPEED", () -> pct(settings.textSpeed), dir -> { settings.textSpeed = step(settings.textSpeed, dir, 0.25f, 0.5f, 2f); apply(); }, null, "Slower to read at ease; faster to skim."));
        tabs.add(x);

        List<Row> m = new ArrayList<>();
        m.add(new Row("TUTORIALS", () -> "", null, () -> flow.push(new TutorialScreen(flow, ui, batch)), "Every lesson of the flight, to read again."));
        m.add(new Row("KEY REFERENCE", () -> "", null, () -> flow.push(new ControlScreen(flow, ui, batch)), "What every key does, as you have set them."));
        m.add(new Row("PILOT PROFILE", () -> vajra.profile.Profiles.active().name, null, () -> flow.push(new ProfileScreen(flow, ui, batch)), "Your name and your book."));
        m.add(new Row("CHECK FOR UPDATE", () -> updater.message(), null, updater::begin, "Pulls the newest setup from the project's releases and installs it over this one (Windows)."));
        m.add(new Row("RESET ALL SETTINGS", () -> sure ? "ENTER AGAIN" : "", null, () -> {
            if (!sure) { sure = true; return; }
            Settings.reset(settings);
            applyDisplay(settings);
            keep();
            sure = false;
        }, "Every setting, the keys too, back to how the game ships."));
        tabs.add(m);

        List<Row> t = new ArrayList<>();
        t.add(new Row("OPEN THE TEST ARENA", () -> vajra.world.Arena.count() + " SCENARIOS", null, () -> flow.push(new ArenaScreen(flow, ui, batch)),
                "Story, foes, weapons, astras, rulers, the world, loot, the HUD, hulls, hazards, tutorials and stress: fly any of them with the full armoury. Nothing is saved."));
        tabs.add(t);
    }

    private Row vol(String label, Supplier<Float> get, java.util.function.Consumer<Float> set, String hint) {
        return new Row(label, () -> pct(get.get()), dir -> { set.accept(step(get.get(), dir, 0.1f, 0f, 1f)); apply(); }, null, hint);
    }

    /** Put the display settings to work: vsync, the frame limit, the readout, the dialogue speed. Called when a setting changes and once at start. */
    public static void applyDisplay(Settings s) {
        if (Gdx.graphics != null) {
            Gdx.graphics.setVSync(s.vsync);
            Gdx.graphics.setForegroundFPS(s.fpsCap);
        }
        Probe.showFrame = s.showFps;
        vajra.scene.CutscenePlayer.speed = s.textSpeed;
    }

    /** Same swap VajraGame's F11 bind performs, so the row and the key never disagree. */
    static void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(vajra.profile.Profiles.active().settings.windowW, vajra.profile.Profiles.active().settings.windowH);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    // ---- input ----------------------------------------------------------------------------------------------------------------------

    private List<Row> rows() { return tabs.get(tab); }

    private void goTab(int dir) {
        tab = cycle(tab, dir, TABS.length);
        sel = 0;
        rebinding = null;
        sure = false;
    }

    @Override
    public void update(float dt) {}

    @Override
    public boolean keyDown(int keycode) {
        if (rebinding != null) {                                     // the next key is the answer
            if (keycode != Input.Keys.ESCAPE) { Keymap.bind(settings, rebinding, keycode); keep(); }
            rebinding = null;
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) { flow.pop(); return true; }
        if (keycode == Input.Keys.Q || keycode == Input.Keys.TAB && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) { goTab(keycode == Input.Keys.Q ? -1 : 1); return true; }
        if (keycode == Input.Keys.E) { goTab(1); return true; }
        if (keycode == Input.Keys.TAB) { goTab(-1); return true; }
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP) { sel = cycle(sel, -1, rows().size()); sure = false; return true; }
        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) { sel = cycle(sel, 1, rows().size()); sure = false; return true; }
        Row r = rows().get(sel);
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) { if (r.adjust != null) r.adjust.accept(-1); return true; }
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) { if (r.adjust != null) r.adjust.accept(1); return true; }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
            if (r.act != null) r.act.run(); else if (r.adjust != null) r.adjust.accept(1);
            return true;
        }
        return false;
    }

    private float rowY(int i) { return ROW_TOP - i * pitch(); }

    private float pitch() { return rows().size() > 10 ? 25f : 38f; }

    private float tabX(int i) { return Template.COL + (i - (TABS.length - 1) / 2f) * 150f; }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        Vector2 m = ui.at(sx, sy);
        if (rebinding != null) { rebinding = null; return true; }
        for (int i = 0; i < TABS.length; i++)
            if (Math.abs(m.x - tabX(i)) < 70 && Math.abs(m.y - TAB_Y) < 18) { tab = i; sel = 0; sure = false; return true; }
        for (int i = 0; i < rows().size(); i++) {
            if (Math.abs(m.y - rowY(i)) > pitch() / 2f || Math.abs(m.x - Template.COL) > HALF_W + 30) continue;
            sel = i;
            Row r = rows().get(i);
            if (r.adjust != null && r.act == null) r.adjust.accept(m.x < Template.COL + 60 ? -1 : 1);
            else if (r.adjust != null && m.x > Template.COL + 140) r.adjust.accept(m.x > Template.COL + 230 ? 1 : -1);
            else if (r.act != null) r.act.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int sx, int sy) {
        Vector2 m = ui.at(sx, sy);
        for (int i = 0; i < rows().size(); i++)
            if (Math.abs(m.y - rowY(i)) <= pitch() / 2f && Math.abs(m.x - Template.COL) <= HALF_W + 30) { sel = i; break; }
        return false;
    }

    // ---- drawing --------------------------------------------------------------------------------------------------------------------

    @Override
    public void render() {
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f);
        Ornament.frame(ui);
        Ornament.titleMarkers(ui, "SETTINGS", Template.TITLE_Y);
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.9f);
        ui.shapes.rect(tabX(tab) - 62, TAB_Y - 17, 124, 2);                        // the chosen tab's rule
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.28f);
        ui.shapes.rect(Template.COL - HALF_W - 30, TAB_Y - 22, 2 * (HALF_W + 30), 1);
        float y = rowY(sel);
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.95f);
        Ornament.diamond(ui, Template.COL - HALF_W - 18, y - 6, 4.2f);
        Ornament.diamond(ui, Template.COL + HALF_W + 18, y - 6, 4.2f);
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.07f);
        ui.shapes.rect(Template.COL - HALF_W - 6, y - pitch() / 2f - 4, 2 * HALF_W + 12, pitch() - 2);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        Template.title(ui, batch, "SETTINGS", Template.TITLE_Y, 1);
        for (int i = 0; i < TABS.length; i++) {
            ui.menu.setColor(i == tab ? Template.accentR : 0.7f, i == tab ? Template.accentG : 0.7f, i == tab ? Template.accentB : 0.74f, i == tab ? 1f : 0.8f);
            gl.setText(ui.menu, TABS[i]);
            ui.menu.draw(batch, TABS[i], tabX(i) - gl.width / 2f, TAB_Y);
        }
        ui.menu.setColor(1, 1, 1, 1);
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            boolean on = i == sel;
            float ry = rowY(i);
            ui.hud.setColor(on ? Template.accentR : 0.86f, on ? Template.accentG : 0.86f, on ? Template.accentB : 0.9f, on ? 1f : 0.92f);
            ui.hud.draw(batch, r.label, Template.COL - HALF_W, ry);
            String v = r.value.get();
            if (!v.isEmpty()) {
                boolean tweak = r.adjust != null && on;
                String shown = tweak ? "<  " + v + "  >" : v;
                ui.hud.setColor(on ? 1f : 0.72f, on ? 0.95f : 0.82f, on ? 0.8f : 0.92f, 1f);
                ui.right(batch, ui.hud, shown, Template.COL + HALF_W, ry);
            }
        }
        ui.hud.setColor(1, 1, 1, 1);
        String hint = rows.get(sel).hint;
        if (hint != null) {
            ui.caption.setColor(0.75f, 0.72f, 0.62f, 0.95f);
            ui.centered(batch, ui.caption, hint, FOOT_HINT_Y);
            ui.caption.setColor(1, 1, 1, 1);
        }
        Template.keys(ui, batch, Template.FOOT_Y, "Q/E", "TAB", "W/S", "ROW", "A/D", "CHANGE", "ENTER", "ACT", "ESC", "BACK");
        batch.end();
    }

    private static final float FOOT_HINT_Y = 80;
}

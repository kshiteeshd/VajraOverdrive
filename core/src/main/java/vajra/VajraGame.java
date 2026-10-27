package vajra;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.flow.Flow;
import vajra.net.Net;
import vajra.screen.SplashScreen;
import vajra.screen.Ui;

/** Boots the Flow screen stack (the one Screen libGDX gets) and owns the three things
 *  every state borrows: the batch, the Ui kit and the Flow itself. States are pushed,
 *  popped and replaced on the Flow; nothing else reaches the input or the screen.
 *  The window settings ride on Flow's global binds, so F11/F12 work on every screen. */
public class VajraGame extends Game {
    private SpriteBatch batch;
    private Ui ui;
    private Flow flow;
    private int winW = 1920, winH = 1080; // the windowed pair F12 swaps between (HD is home)

    @Override
    public void create() {
        batch = new SpriteBatch();
        ui = new Ui();
        flow = new Flow();
        flow.ui(ui); // so it can keep the UI viewport current for states with none of their own
        flow.bind(Input.Keys.F3, () -> vajra.screen.Probe.showFrame = !vajra.screen.Probe.showFrame);   // a frame-time readout (average and worst of the last second) in the corner
        flow.bind(Input.Keys.F11, this::toggleFullscreen);
        flow.bind(Input.Keys.F12, this::cycleWindowed);
        flow.onTop(this::topChanged);
        flow.keySound(k -> {                                                  // the menus' own keys: a tap to move, a bell to choose, a soft bell to go back
            vajra.audio.AudioEngine a = vajra.audio.Audio.get();
            if (k == -1 || k == Input.Keys.ENTER || k == Input.Keys.SPACE) a.ui("ui_select");                 // a click or ENTER: on to the next
            else if (k == Input.Keys.ESCAPE) a.ui("ui_back");
            else if (k == Input.Keys.W || k == Input.Keys.S || k == Input.Keys.UP || k == Input.Keys.DOWN) a.ui("ui_move");   // along a list
            else if (k == Input.Keys.A || k == Input.Keys.D || k == Input.Keys.LEFT || k == Input.Keys.RIGHT) a.ui("ui_adjust");   // a value
            else if (k == Input.Keys.Q || k == Input.Keys.E || k == Input.Keys.TAB) a.ui("ui_tab");                       // a tab
        });
        Gdx.input.setInputProcessor(flow); // every key, mouse and wheel event lands on the stack
        setScreen(flow);
        // FULLSCREEN ALWAYS: the game opens on the display's own mode, not the 1920x1080 window.
        // Do it before the first render so the very first frame - the studio card - is already
        // the size the player will see the game at. F11 still drops back to windowed.
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        vajra.screen.SettingsScreen.applyDisplay(vajra.profile.Profiles.active().settings);   // the pilot's own vsync, frame limit and dialogue speed
        flow.set(new SplashScreen(flow, ui, batch)); // then the main menu
    }

    /** What the ears do when a different screen comes to the top: the title has its own music, the menus another, a pause lowers the flight's. */
    private void topChanged(vajra.flow.FlowState top) {
        vajra.audio.AudioEngine a = vajra.audio.Audio.get();
        if (top == null) return;
        String n = top.getClass().getSimpleName();
        if (n.equals("GameScreen")) { a.setPaused(false); return; }
        if (n.equals("SplashScreen")) return;
        a.screen(n.equals("PauseScreen") ? "pause" : "title");          // the title, the settings and every menu share one music; the pause has its own
        a.leaveGame();
    }

    @Override
    public void render() {
        super.render();
        vajra.audio.Audio.get().updateScreens(Gdx.graphics.getDeltaTime());
    }

    /** F11: fullscreen on the current display, or back to the windowed size. */
    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(winW, winH);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    /** F12: swap the window between 1280x720 and 1920x1080 (leaves fullscreen for either). */
    private void cycleWindowed() {
        winW = winW == 1280 ? 1920 : 1280;
        winH = winW == 1280 ? 720 : 1080;
        Gdx.graphics.setWindowedMode(winW, winH);
    }

    @Override
    public void dispose() {
        vajra.audio.Audio.dispose();
        Net.close(); // first: a quitting guest says BYE while its socket still lives
        if (flow != null) flow.dispose(); // pops (and disposes) every state on the stack
        if (ui != null) ui.dispose();
        if (batch != null) batch.dispose();
        super.dispose();
    }
}

package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.Astra;
import vajra.world.World;

/** The tutorial in the real game (build/tour-*.png): the spotlight going round the instruments after the opening, then the camera going slowly to the first lamp and the first bay. */
class TutorialShotTest {
    @Test
    void theTourAndTheFirstSightsPlay() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        boolean[] ok = new boolean[3];
        vajra.Settings.tutorialAllowed = true;
        new Lwjgl3Application(new ApplicationAdapter() {
            Flow flow;

            @Override
            public void create() {
                try {
                    vajra.profile.Profiles.active().beats = new boolean[8];
                    vajra.profile.Profiles.active().settings.hints = true;
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    flow = new Flow();
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER);
                    GameScreen gs = (GameScreen) flow.top();
                    World w = gs.world();
                    for (int i = 0; i < 120; i++) flow.render(1 / 60f);
                    for (int k = 0; k < 12; k++) { flow.keyDown(Input.Keys.ENTER); for (int i = 0; i < 6; i++) flow.render(1 / 60f); }   // read the opening through
                    for (int i = 0; i < 40; i++) flow.render(1 / 60f);
                    ok[0] = vajra.profile.Profiles.active().seen(vajra.profile.Profile.BEAT_TOUR);
                    float y0 = w.ship.y;
                    for (int step = 0; step < 10; step++) {
                        for (int i = 0; i < 30; i++) flow.render(1 / 60f);
                        if (step == 0 || step == 3 || step == 6 || step == 8 || step == 9) shot("build/tour-" + step + ".png");
                        flow.keyDown(Input.Keys.ENTER);
                    }
                    for (int i = 0; i < 20; i++) flow.render(1 / 60f);
                    ok[1] = Math.abs(w.ship.y - y0) < 5f;     // the world held still through the tour
                    w.cues.clear();
                    vajra.world.Checkpoint cp = w.checkpoints[0];
                    w.ship.x = cp.x + 120f;
                    w.ship.y = cp.y - 360f;
                    for (int i = 0; i < 100; i++) flow.render(1 / 60f);
                    shot("build/tour-lamp.png");
                    for (int k = 0; k < 8; k++) { flow.keyDown(Input.Keys.ENTER); for (int i = 0; i < 20; i++) flow.render(1 / 60f); }
                    float[] sp = w.shops[0];
                    w.ship.x = sp[0] + 100f;
                    w.ship.y = sp[1] - 360f;
                    for (int i = 0; i < 100; i++) flow.render(1 / 60f);
                    shot("build/tour-bay.png");
                    ok[2] = vajra.profile.Profiles.active().seen(vajra.profile.Profile.BEAT_LAMP);
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    vajra.Settings.tutorialAllowed = false;
                    Gdx.app.exit();
                }
            }

            private void shot(String file) {
                IntBuffer buf = BufferUtils.newIntBuffer(1280 * 720);
                Gdx.gl.glReadPixels(0, 0, 1280, 720, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                Pixmap pm = new Pixmap(1280, 720, Pixmap.Format.RGBA8888);
                for (int y = 0; y < 720; y++) for (int x = 0; x < 1280; x++) pm.drawPixel(x, 719 - y, Integer.reverseBytes(buf.get(y * 1280 + x)));
                com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local(file), pm);
                pm.dispose();
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the tutorial failed", boom.get());
        assertTrue(ok[0], "the tour began after the opening and is recorded as seen");
        assertTrue(ok[1], "the world held still through the tour");
        assertTrue(ok[2], "the first lamp was shown, once");
    }
}

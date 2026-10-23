package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.profile.Profiles;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;

/** The title's two faces on the real Flow: with no save the menu opens on NEW GAME; fly, SAVE AND QUIT
 *  from the pause menu, and the title opens on CONTINUE; CONTINUE wakes a run (not the opening); NEW GAME
 *  over a save asks before it erases. PNGs: build/continue-*.png. Saves go under build/testdata. */
class ContinueShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void titleOffersContinueAfterASaveAndNewGameBefore() {
        List<String> failures = new ArrayList<>();
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Ui ui;
            SpriteBatch batch;
            Flow flow;

            @Override
            public void create() {
                try {
                    ui = new Ui();
                    batch = new SpriteBatch();
                    flow = new Flow();
                    Profiles.clearRun();

                    // 1. no save: the title opens on NEW GAME
                    flow.set(new HomeScreen(flow, ui, batch));
                    writePng(frame(), "build/continue-1-nosave.png");
                    if (Profiles.active().hasRun()) failures.add("a cleared pilot still has a run");

                    // 2. NEW GAME starts a run; fly a moment; pause; SAVE AND QUIT parks it
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof GameScreen)) failures.add("NEW GAME did not start a run");
                    for (int i = 0; i < 30; i++) frame();
                    flow.keyDown(Input.Keys.ESCAPE);   // the opening scene holds the keys: ESC skips it...
                    for (int i = 0; i < 90; i++) frame();
                    flow.keyDown(Input.Keys.ESCAPE);   // ...and this one is the pause
                    frame();
                    for (int i = 0; i < 3; i++) flow.keyDown(Input.Keys.DOWN);
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof HomeScreen)) failures.add("SAVE AND QUIT did not return to the title");
                    if (!Profiles.active().hasRun()) failures.add("SAVE AND QUIT parked no run");

                    // 3. the title now leads with CONTINUE
                    flow.set(new HomeScreen(flow, ui, batch));
                    writePng(frame(), "build/continue-2-save.png");

                    // 4. CONTINUE wakes a run, and no cutscene holds it
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof GameScreen)) failures.add("CONTINUE did not start a run");
                    for (int i = 0; i < 20; i++) frame();
                    writePng(frame(), "build/continue-3-woken.png");
                    flow.keyDown(Input.Keys.ESCAPE); // straight to the pause: no opening scene to skip
                    frame();
                    for (int i = 0; i < 3; i++) flow.keyDown(Input.Keys.DOWN);
                    flow.keyDown(Input.Keys.ENTER);
                    if (!Profiles.active().hasRun()) failures.add("the woken run did not park again");

                    // 5. NEW GAME over a save asks first
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.DOWN); // CONTINUE -> NEW GAME
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof HomeScreen)) failures.add("NEW GAME erased a save without asking");
                    writePng(frame(), "build/continue-4-sure.png");
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof GameScreen)) failures.add("the second press did not start a new game");
                    if (Profiles.active().hasRun()) failures.add("a new game left the old save behind");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Profiles.clearRun();
                    Gdx.app.exit();
                }
            }

            int[] frame() {
                flow.render(1 / 60f);
                IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                int[] px = new int[W * H];
                buf.get(px);
                return px;
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("continue flow crashed", boom.get());
        assertEquals(List.of(), failures);
    }

    private static void writePng(int[] px, String path) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int p = px[(H - 1 - y) * W + x];
                    img.setRGB(x, y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
                }
            new File(path).getParentFile().mkdirs();
            ImageIO.write(img, "png", new File(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

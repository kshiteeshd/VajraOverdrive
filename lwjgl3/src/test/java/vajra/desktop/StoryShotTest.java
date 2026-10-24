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
import vajra.screen.ComicScreen;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.StoryScript;
import vajra.screen.Ui;

/** The story, walked on the real Flow: every panel must draw (art in the stage, narrator text in
 *  the band), SPACE must finish a line before it turns the panel, and the last turn must land on the
 *  menu. PNGs for eyeballing go to build/story-NN.png. */
class StoryShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void theStoryPlaysPanelByPanelAndEndsOnTheMenu() {
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
                    assertTrue(ComicScreen.available(), "the story art is on disk");
                    System.clearProperty("vajra.nostory"); // the test config turns the story off; this test wants it
                    Profiles.clearRun();

                    // the menu is NOT the story: the splash hands over to the title
                    flow.set(new HomeScreen(flow, ui, batch));
                    if (!(flow.top() instanceof HomeScreen)) failures.add("the title is not the first screen");
                    // NEW GAME is the story's moment
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof ComicScreen)) failures.add("NEW GAME did not open the story");
                    int n = StoryScript.PANELS.length;
                    for (int i = 0; i < n; i++) {
                        for (int k = 0; k < 60; k++) frame(); // 1 s: the panel is up, the line is typing
                        flow.keyDown(Input.Keys.SPACE);        // finishes the line, does not turn
                        int[] done = frame();
                        if (i % 3 == 0 || i == n - 1) writePng(done, String.format("build/story-%02d.png", i));
                        if (!(flow.top() instanceof ComicScreen)) failures.add("left the story early at panel " + i);
                        // art in the stage, text in the band
                        if (lit(done, 40, W - 40, 170, H - 30) < 20000) failures.add("panel " + i + " drew no art");
                        if (lit(done, 200, W - 200, 20, 110) < 150) failures.add("panel " + i + " drew no narration");
                        flow.keyDown(Input.Keys.SPACE);        // turns
                    }
                    if (!(flow.top() instanceof GameScreen)) failures.add("the last turn did not start the run");

                    // ESC skips the whole story straight into the run
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER);
                    if (!(flow.top() instanceof ComicScreen)) failures.add("a second NEW GAME did not open the story");
                    flow.keyDown(Input.Keys.ESCAPE);
                    if (!(flow.top() instanceof GameScreen)) failures.add("ESC did not skip the story into the run");

                    // CONTINUE never replays it
                    Profiles.saveRun(((GameScreen) flow.top()).world());
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER); // CONTINUE
                    if (!(flow.top() instanceof GameScreen)) failures.add("CONTINUE showed the story");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    System.setProperty("vajra.nostory", "true");
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
        if (boom.get() != null) throw new AssertionError("story render crashed", boom.get());
        assertEquals(List.of(), failures);
    }

    /** Pixels brighter than near-black in a box (bottom-up buffer). */
    private static int lit(int[] px, int x0, int x1, int y0, int y1) {
        int n = 0;
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int p = px[y * W + x];
                if ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff) > 150) n++;
            }
        return n;
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

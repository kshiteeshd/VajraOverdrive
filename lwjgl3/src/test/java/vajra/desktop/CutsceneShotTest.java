package vajra.desktop;

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
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.Cue;
import vajra.world.World;

/** The cutscenes in the real renderer: the opening (the pilot's thought), the grand gate (the
 *  narrator in the bottom band, then a guard's bubble) and the ruler waking. PNGs land in build/. */
class CutsceneShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void theScenesDrawBandsBubblesAndNarration() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        AtomicReference<String> fail = new AtomicReference<>("");
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Flow flow;

            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    flow = new Flow();
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER);
                    World w = ((GameScreen) flow.top()).world();
                    // the opening: wait for the camera, the bands and a few typed words
                    for (int i = 0; i < 150; i++) flow.render(1 / 60f);
                    int[] open = frame();
                    write(open, "build/scene-opening.png");
                    check(dark(open, 0, 60) > 0.97 && dark(open, H - 60, H) > 0.97,
                            "the black bands cover the top and bottom", fail);
                    // skip it, then stage the gate
                    flow.keyDown(Input.Keys.ESCAPE);
                    for (int i = 0; i < 60; i++) flow.render(1 / 60f);
                    w.ship.x = 2600;
                    w.ship.y = 1700;
                    w.cue(Cue.ATALA_GATE);
                    for (int i = 0; i < 200; i++) flow.render(1 / 60f);   // the guards walk in; the narrator speaks
                    write(frame(), "build/scene-gate-narrator.png");
                    flow.keyDown(Input.Keys.ENTER);                        // finish the line
                    flow.keyDown(Input.Keys.ENTER);                        // next shot: a guard
                    for (int i = 0; i < 120; i++) flow.render(1 / 60f);
                    write(frame(), "build/scene-gate-guard.png");
                    flow.keyDown(Input.Keys.ENTER);
                    flow.keyDown(Input.Keys.ENTER);                        // the pilot answers
                    for (int i = 0; i < 120; i++) flow.render(1 / 60f);
                    write(frame(), "build/scene-gate-pilot.png");
                    flow.dispose();
                } catch (Throwable t) {
                    boom.set(t);
                }
                Gdx.app.exit();
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
        if (boom.get() != null) throw new AssertionError("scene shots crashed", boom.get());
        assertTrue(fail.get().isEmpty(), fail.get());
    }

    private static void check(boolean ok, String what, AtomicReference<String> fail) {
        if (!ok) fail.set(fail.get() + what + "; ");
    }

    /** Share of near-black pixels in the rows [y0, y1) counted bottom-up. */
    private static double dark(int[] px, int y0, int y1) {
        int n = 0, tot = 0;
        for (int y = y0; y < y1; y++)
            for (int x = 0; x < W; x += 4) {
                int p = px[y * W + x];
                tot++;
                if ((p & 0xff) < 12 && ((p >> 8) & 0xff) < 12 && ((p >> 16) & 0xff) < 12) n++;
            }
        return n / (double) tot;
    }

    private static void write(int[] px, String path) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int p = px[y * W + x];
                    img.setRGB(x, H - 1 - y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
                }
            File out = new File(path);
            out.getParentFile().mkdirs();
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

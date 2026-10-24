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
import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.TestAreaScreen;
import vajra.screen.Ui;

/** The test area: build/test-area-*.png (the walls alone with the world behind, then fifteen layers, then tighter ones). The far world must fill the space
 *  behind the walls and fifteen layers must fly smoothly. */
class TestAreaShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void theLayeredWallsFillTheDepthBehindThePlayLayer() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        int[] lit = new int[3];
        float[] ms = new float[2];
        int[][] frames = new int[3][];
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                Input real = Gdx.input;
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    TestAreaScreen t = new TestAreaScreen(flow, ui, batch);
                    Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                            (proxy, m, args) -> m.getName().equals("isKeyPressed") && (Integer) args[0] == Input.Keys.W
                                    ? Boolean.TRUE : m.invoke(real, args));
                    t.finishLoading();
                    for (int i = 0; i < 240; i++) { // fly forward four seconds
                        t.update(1 / 60f);
                        t.render();
                    }
                    for (int k = 0; k < 30; k++) t.keyDown(Input.Keys.MINUS);       // no layers: the walls alone, the world behind
                    t.update(1 / 60f);
                    t.render();
                    lit[0] = shot("build/test-area-0-walls-only.png");
                    for (int k = 0; k < 15; k++) t.keyDown(Input.Keys.EQUALS);      // fifteen layers behind
                    t.update(1 / 60f);
                    t.render();
                    lit[1] = shot("build/test-area-1-fifteen-layers.png");
                    float worst = 0, sum = 0;                                    // smoothness: every frame of a flight, timed
                    for (int i = 0; i < 180; i++) {
                        long t0 = System.nanoTime();
                        t.update(1 / 60f);
                        t.render();
                        Gdx.gl.glFinish();
                        float f = (System.nanoTime() - t0) / 1_000_000f;
                        if (i >= 20) { sum += f; worst = Math.max(worst, f); }
                    }
                    ms[0] = sum / 160f;
                    ms[1] = worst;
                    for (int k = 0; k < 6; k++) t.keyDown(Input.Keys.LEFT_BRACKET); // shrinking faster with distance
                    t.keyDown(Input.Keys.PERIOD);
                    t.update(1 / 60f);
                    t.render();
                    lit[2] = shot("build/test-area-2-tighter.png");
                } catch (Throwable x) {
                    boom.set(x);
                } finally {
                    Gdx.input = real;
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the test area crashed", boom.get());
        System.out.println("TestArea: lit pixels walls-only " + lit[0] + ", thirty layers " + lit[1] + ", tighter " + lit[2] + "; frame avg " + ms[0] + " ms, worst " + ms[1] + " ms");
        assertTrue(lit[0] > 100000, "the far world fills the empty space behind the walls: " + lit[0]);
        assertTrue(ms[0] < 14f, "fifteen layers fly smoothly: " + ms[0] + " ms a frame on average");
    }

    /** Write the frame and count the pixels that are not the empty dark. */
    private static int shot(String path) throws Exception {
        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        int lit = 0;
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int p = buf.get((H - 1 - y) * W + x), r = p & 0xff, g = (p >> 8) & 0xff, b = (p >> 16) & 0xff;
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
                if (r + g + b > 60) lit++;
            }
        new File(path).getParentFile().mkdirs();
        ImageIO.write(img, "png", new File(path));
        return lit;
    }
}

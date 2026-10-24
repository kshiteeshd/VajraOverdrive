package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** The wheel's blur: a hard-edged red block at the top-left of the window comes back soft (its edge is no longer a step),
 *  is still at the top-left (not flipped), and the rest of the frame stays dark. */
class BlurShotTest {
    private static final int W = 640, H = 360;

    @Test
    void aBlockGoesSoftAndStaysWhereItWas() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        int[] edge = new int[1], topLeft = new int[1], bottomRight = new int[1];
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    SpriteBatch batch = new SpriteBatch();
                    ShapeRenderer sh = new ShapeRenderer();
                    Blur blur = new Blur();
                    Gdx.gl.glViewport(0, 0, W, H);
                    Gdx.gl.glClearColor(0, 0, 0, 1);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    sh.getProjectionMatrix().setToOrtho2D(0, 0, W, H);
                    sh.begin(ShapeRenderer.ShapeType.Filled);
                    sh.setColor(1, 0, 0, 1);
                    sh.rect(0, H - 120, 160, 120); // the top-left corner (GL's y is up)
                    sh.end();
                    blur.render(batch, 1.6f);
                    IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                    Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                    int[] px = new int[W * H];
                    buf.get(px);
                    int row = H - 60;                                         // through the block's middle
                    edge[0] = red(px[row * W + 160]);                         // right on the old hard edge: now half-way
                    topLeft[0] = red(px[row * W + 40]);
                    bottomRight[0] = red(px[40 * W + 560]);
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("blur crashed", boom.get());
        assertTrue(topLeft[0] > 100, "the block is still at the top-left (" + topLeft[0] + ")");
        assertTrue(bottomRight[0] < 20, "and nothing bled to the far corner (" + bottomRight[0] + ")");
        assertTrue(edge[0] > 25 && edge[0] < 230, "the old hard edge is soft now (" + edge[0] + ")");
    }

    private static int red(int rgba) { return rgba & 0xff; }
}

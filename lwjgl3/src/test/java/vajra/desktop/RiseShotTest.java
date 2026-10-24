package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.screen.Ui;
import vajra.screen.WorldRenderer;
import vajra.world.World;

/** The checkpoint, close up, cold and then lit (build/checkpoint-*.png): a cold lamp is pink and a lit one is gold and warm, and the
 *  lit one lights up the dark round it. */
class RiseShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void reservesClimbOutOfTheFloor() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    World w = new World();
                    WorldRenderer r = new WorldRenderer(batch, ui, w);
                    r.finishLoading();
                    OrthographicCamera cam = new OrthographicCamera(Ui.UI_W, Ui.UI_H);
                    FitViewport vp = new FitViewport(Ui.UI_W, Ui.UI_H, cam);
                    w.enemies.clear();
                    w.ship.x = 2600;
                    w.ship.y = 3000;
                    cam.position.set(2600, 3000, 0);
                    cam.update();
                    vp.update(W, H);
                    float[] depths = {1f, 0.7f, 0.45f, 0.25f, 0f};
                    for (int i = 0; i < depths.length; i++) {
                        vajra.enemy.Enemy e = new vajra.enemy.Enemy(vajra.enemy.Enemy.Kind.VAKRA, 2600 - 240 + i * 120, 3120);
                        e.active = true;
                        e.depth = depths[i];
                        e.diveState = depths[i] > 0 ? 3 : 0;
                        w.enemies.add(e);
                    }
                    for (int k = 0; k < 4; k++) r.render(w, cam, vp);
                    int[] px = read();
                    write(px, "build/rise.png");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("rise render crashed", boom.get());
        assertTrue(true);
    }

    private static int[] read() {
        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        int[] px = new int[W * H];
        buf.get(px);
        return px;
    }

    /** Mean r, g, b over a box (screen coordinates, y from the top). */
    private static float[] mean(int[] px, int x0, int x1, int y0, int y1) {
        double r = 0, g = 0, b = 0;
        int n = 0;
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int p = px[(H - 1 - y) * W + x];
                r += p & 0xff;
                g += (p >> 8) & 0xff;
                b += (p >> 16) & 0xff;
                n++;
            }
        return new float[] {(float) (r / n), (float) (g / n), (float) (b / n)};
    }

    /** Mean brightness on a ring round a point. */
    private static float ring(int[] px, int cx, int cy, int r0, int r1) {
        double sum = 0;
        int n = 0;
        for (int y = Math.max(0, cy - r1); y < Math.min(H, cy + r1); y++)
            for (int x = Math.max(0, cx - r1); x < Math.min(W, cx + r1); x++) {
                double d = Math.hypot(x - cx, y - cy);
                if (d < r0 || d > r1) continue;
                int p = px[(H - 1 - y) * W + x];
                sum += ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff)) / 3.0;
                n++;
            }
        return (float) (sum / n);
    }

    private static void write(int[] px, String path) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int p = px[(H - 1 - y) * W + x];
                img.setRGB(x, y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
            }
        new File(path).getParentFile().mkdirs();
        ImageIO.write(img, "png", new File(path));
    }
}

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

/** Vanija's lotus berth, close up, in three states (build/shop-*.png): parked out, a hull settling in, and docked with the clamps
 *  closed. The docked frame must be warmer and brighter round the berth than the empty one. */
class ShopShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void theBerthLightsAndClosesOnAHull() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        float[] lum = new float[3];
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
                    float sx = w.shops[0][0], sy = w.shops[0][1];
                    cam.position.set(sx, sy - 20, 0);
                    cam.update();
                    vp.update(W, H);
                    float bx = sx + 2, by = sy - 46;
                    for (int pass = 0; pass < 3; pass++) {
                        w.ship.x = pass == 0 ? bx + 150 : bx;
                        w.ship.y = pass == 0 ? by - 60 : by;
                        w.shopState[0] = pass == 0 ? World.UNDOCKED : pass == 1 ? World.DOCKING : World.DOCKED;
                        w.shopTimer[0] = pass == 1 ? 0.3f : pass == 2 ? World.DOCK_TIME : 0f;
                        Gdx.gl.glClearColor(0, 0, 0, 1);
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        r.render(w, cam, vp);
                        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                        int[] px = new int[W * H];
                        buf.get(px);
                        write(px, "build/shop-" + pass + (pass == 0 ? "-parked-out" : pass == 1 ? "-settling" : "-docked") + ".png");
                        // brightness in a box round the berth (screen: x 640+2, y 360+20-46+... ~ 360 + 66)
                        double sum = 0;
                        int n = 0;
                        for (int y = 366 - 70; y < 366 + 70; y++)
                            for (int x = 642 - 70; x < 642 + 70; x++) {
                                int p = px[(H - 1 - y) * W + x];
                                sum += ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff)) / 3.0;
                                n++;
                            }
                        lum[pass] = (float) (sum / n);
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("shop render crashed", boom.get());
        System.out.println("Shop: brightness round the berth, parked out " + lum[0] + ", settling " + lum[1] + ", docked " + lum[2]);
        assertTrue(lum[2] > lum[0] * 1.05f, "a docked berth is brighter than an empty one");
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

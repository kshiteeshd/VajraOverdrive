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

/** The world as the pilot sees it, band by band: build/world-<n>.png. Nothing is asserted beyond "it draws". */
class WorldShotTest {
    private static final int W = 1280, H = 720;
    private static volatile float frameMs;
    private static volatile boolean northIsPaler;
    private static final float[] SPOTS = {300, 1250, 1700, 3000, 5200, 9000, 14500, 19000, 1600};
    private static final float[] SPOT_X = {Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN};

    @Test
    void theWorldDrawsBandByBand() {
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
                    long t0 = System.nanoTime();
                    boolean raycast = r.finishLoading();
                    System.out.println("WorldShot: terrain bake + upload " + (System.nanoTime() - t0) / 1_000_000 + " ms, raycast " + raycast);
                    OrthographicCamera cam = new OrthographicCamera(Ui.UI_W, Ui.UI_H);
                    FitViewport vp = new FitViewport(Ui.UI_W, Ui.UI_H, cam);
                    for (int i = 0; i < SPOTS.length; i++) {
                        w.ship.x = Float.isNaN(SPOT_X[i]) ? w.width / 2 : SPOT_X[i] < 0
                                ? new vajra.world.GroundMap(vajra.world.GroundMap.SEED, w.width, w.height, w.regionStart).roadCentre(SPOTS[i]) : SPOT_X[i];
                        w.ship.y = SPOTS[i];
                        for (int k = 0; k < 14; k++) { // a fan of enemy rounds in the air round the hull
                            double ang = k * Math.PI * 2 / 14;
                            w.spawnEnemyBullet(w.ship.x + (float) Math.cos(ang) * 150, w.ship.y + (float) Math.sin(ang) * 110, (float) ang);
                        }
                        w.effects.clear(); // (the muzzle flashes of the rounds just spawned would hide the rounds themselves)
                        cam.position.set(w.ship.x, Math.max(360, w.ship.y), 0); // the game's own clamp at the floor
                        cam.update();
                        vp.update(W, H);
                        Gdx.gl.glClearColor(0, 0, 0, 1);
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        for (int k = 0; k < 4; k++) r.render(w, cam, vp);   // (the first frames bake the wall chunks)
                        write("build/world-" + i + ".png");
                        if (i == 8) northIsPaler = Math.abs(lum(0, H / 3) - lum(2 * H / 3, H)) > 0.02f;   // Atala lies NORTH of the meadow: the two halves of the frame differ
                    }
                    // how long a frame of the raycast world takes (the whole renderer, GPU included), mid-forest and over the meadow
                    for (float y : new float[] {800, 5200}) {
                        w.ship.x = w.width / 2;
                        w.ship.y = y;
                        cam.position.set(w.ship.x, w.ship.y, 0);
                        cam.update();
                        for (int k = 0; k < 10; k++) r.render(w, cam, vp);
                        Gdx.gl.glFinish();
                        long t1 = System.nanoTime();
                        for (int k = 0; k < 60; k++) r.render(w, cam, vp);
                        Gdx.gl.glFinish();
                        frameMs = (System.nanoTime() - t1) / 60_000_000f;
                        System.out.println("WorldShot: y=" + y + " frame " + frameMs + " ms");
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("world render crashed", boom.get());
        assertTrue(northIsPaler, "the north and south of the boundary frame differ (the land is not mirrored onto itself)");
        assertTrue(frameMs < 60f, "a frame of the raycast world stays playable: " + frameMs + " ms");
    }

    /** Mean brightness of the rows [y0, y1) of the frame just drawn (y counted from the top of the screen). */
    private static float lum(int y0, int y1) {
        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        double sum = 0;
        for (int y = y0; y < y1; y++)
            for (int x = 0; x < W; x += 4) {
                int p = buf.get((H - 1 - y) * W + x);
                sum += ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff)) / 765.0;
            }
        return (float) (sum / ((y1 - y0) * (W / 4)));
    }

    private static void write(String path) throws Exception {
        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        int[] px = new int[W * H];
        buf.get(px);
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

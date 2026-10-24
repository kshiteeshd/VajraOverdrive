package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.screen.FxImages;
import vajra.screen.ShipRenderer;
import vajra.ship.Hangar;
import vajra.ship.Ship;
import vajra.ship.ShipDef;

/** Every mark, every tier, drawn by the game's own ShipRenderer (the in-game path, not the sheet): each must put
 *  real pixels on screen. build/ship-sheets.png for eyeballing, drawn at the in-game size and again at 4x. */
class ShipSheetsShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void everyMarkAndTierDrawsInGame() {
        List<String> failures = new ArrayList<>();
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    SpriteBatch batch = new SpriteBatch();
                    ShipRenderer r = new ShipRenderer(new FxImages());
                    OrthographicCamera cam = new OrthographicCamera(W, H);
                    cam.position.set(W / 2f, H / 2f, 0);
                    cam.update();
                    Gdx.gl.glClearColor(0.03f, 0.04f, 0.09f, 1);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    batch.setProjectionMatrix(cam.combined);
                    batch.begin();
                    for (int mk = 0; mk < ShipDef.ALL.length; mk++) {
                        for (int tier = 0; tier < 3; tier++) {
                            Hangar h = new Hangar();
                            h.owned[mk][0] = true;
                            for (int t = 1; t <= tier; t++) h.owned[mk][t] = true;
                            Ship s = new Ship(0, 0, ShipDef.ALL[mk], h);
                            s.facing = (float) (Math.PI / 2);
                            // in-game size on the left, a bigger copy on the right (the draw is a fixed 40 px, so
                            // the big copy is the same art through a scaled batch matrix)
                            s.x = 70 + mk * 80;
                            s.y = 640 - tier * 70;
                            r.draw(batch, s, 0.3f);
                        }
                        // and the same hull boosting: its own boost form (wings folded, petals shut, heads tucked...)
                        Hangar bh = new Hangar();
                        bh.owned[mk][0] = true;
                        Ship bs = new Ship(0, 0, ShipDef.ALL[mk], bh);
                        bs.facing = (float) (Math.PI / 2);
                        bs.boosting = true;
                        bs.boostT = 1f;
                        bs.x = 70 + mk * 80;
                        bs.y = 640 - 3 * 70;
                        r.draw(batch, bs, 0.3f);
                    }
                    batch.end();
                    IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                    Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                    int[] px = new int[W * H];
                    buf.get(px);
                    for (int mk = 0; mk < ShipDef.ALL.length; mk++)
                        for (int tier = 0; tier < 3; tier++) {
                            int cx = 70 + mk * 80, cy = 640 - tier * 70, n = 0;
                            for (int y = cy - 24; y < cy + 24; y++)
                                for (int x = cx - 24; x < cx + 24; x++) {
                                    int p = px[y * W + x];
                                    if ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff) > 90) n++;
                                }
                            if (n < 60) failures.add("mark " + (mk + 1) + " tier " + (tier + 1) + " drew no hull (" + n + " px)");
                        }
                    write(px, "build/ship-sheets.png");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("ship render crashed", boom.get());
        assertEquals(List.of(), failures);
    }

    private static void write(int[] px, String path) {
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

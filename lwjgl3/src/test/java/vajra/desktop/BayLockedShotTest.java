package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.BayUi;
import vajra.screen.GameScreen;
import vajra.screen.Ui;
import vajra.world.World;

/** The hangar of a brand-new run (build/bay-locked.png): only the base hull is shown in colour; every other hull is a black silhouette with a
 *  faint gold rim and a padlock in the corner - its shape teased, its colours and stats not. */
class BayLockedShotTest {
    @Test
    void lockedHullsAreSilhouettes() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        int[] rimmed = {0};
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    GameScreen g = new GameScreen(batch, flow, ui);
                    flow.push(g);
                    Field wf = GameScreen.class.getDeclaredField("world");
                    wf.setAccessible(true);
                    World w = (World) wf.get(g);
                    Field dir = GameScreen.class.getDeclaredField("director");
                    dir.setAccessible(true);
                    float bx = w.shops[0][0] + 2, by = w.shops[0][1] - 46;
                    for (int i = 0; i < 200 && !open(g); i++) {
                        Object d = dir.get(g);
                        if ((Boolean) d.getClass().getMethod("active").invoke(d)) d.getClass().getMethod("skip").invoke(d);
                        w.ship.x = bx;
                        w.ship.y = by;
                        w.ship.vx = w.ship.vy = 0;
                        g.update(1 / 60f);
                        g.render();
                    }
                    assertTrue(open(g), "the hangar opened");
                    g.render();
                    g.render();
                    IntBuffer buf = BufferUtils.newIntBuffer(1280 * 720);
                    Gdx.gl.glReadPixels(0, 0, 1280, 720, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                    int[] px = new int[1280 * 720];
                    buf.get(px);
                    BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < 720; y++)
                        for (int x = 0; x < 1280; x++) {
                            int p = px[(719 - y) * 1280 + x];
                            img.setRGB(x, y, ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF));
                        }
                    new File("build").mkdirs();
                    ImageIO.write(img, "png", new File("build/bay-locked.png"));
                    // the second hull's card: gold rim pixels around its silhouette
                    float x0 = BayUi.stripX(1);
                    for (int y = 0; y < 720; y++)
                        for (int x = (int) x0; x < (int) x0 + 148; x++) {
                            int p = px[y * 1280 + x];
                            int r = p & 0xFF, gg = (p >> 8) & 0xFF, b = (p >> 16) & 0xFF;
                            if (r > 80 && gg > 60 && b < r - 10 && y > 380 && y < 600) rimmed[0]++;
                        }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }

            private boolean open(GameScreen g) throws Exception {
                Field sf = GameScreen.class.getDeclaredField("shop");
                sf.setAccessible(true);
                Object shop = sf.get(g);
                return (Boolean) shop.getClass().getMethod("isOpen").invoke(shop);
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the locked bay failed", boom.get());
        assertTrue(rimmed[0] > 20, "the locked hull's silhouette rim is drawn (" + rimmed[0] + " px)");
    }
}

package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.world.Astra;
import vajra.world.World;

/** The third eye burning (build/pashupata-beam.png): the Pashupatastra loosed north, the beam a hot core inside gold and vermilion with rings running down it, the
 *  eye open over the hull, and the held mandala of the invocation just before it (build/pashupata-invoke.png). */
class PashupataShotTest {
    @Test
    void theBeamAndTheEyeDraw() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(64, 64);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        int[] lit = {0};
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    World w = new World();
                    w.enemies.clear();
                    w.ship.x = 2600;
                    w.ship.y = 2700;
                    w.ship.facing = (float) (Math.PI / 2);
                    w.hangar.shards[Astra.AGNI] = 5;
                    w.astraSel = Astra.PASHUPATA;
                    w.astraBegin();
                    for (float t = 0; t < Astra.WIND_FULL * 0.9f; t += 1 / 60f) w.updateAstra(1 / 60f);
                    shot(w, "build/pashupata-invoke.png", null);
                    for (float t = 0; t < Astra.WIND_FULL; t += 1 / 60f) w.updateAstra(1 / 60f);
                    w.astraLoose();
                    for (int i = 0; i < 40; i++) w.updateAstra(1 / 60f);
                    shot(w, "build/pashupata-beam.png", lit);
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }

            private void shot(World w, String file, int[] count) {
                int W = 900, H = 1000;
                FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, W, H, false);
                ShapeRenderer sr = new ShapeRenderer(20000);
                OrthographicCamera cam = new OrthographicCamera(W, H);
                cam.position.set(w.ship.x, w.ship.y + 300, 0);
                cam.update();
                fbo.begin();
                ScreenUtils.clear(0.04f, 0.035f, 0.05f, 1f);
                sr.setProjectionMatrix(cam.combined);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                sr.begin(ShapeRenderer.ShapeType.Filled);
                AstraFx.draw(sr, w, w.ship.x - W / 2f, w.ship.y - 200, W, H);
                sr.end();
                Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, W, H);
                fbo.end();
                if (count != null)
                    for (int y = 0; y < H; y += 2)
                        for (int x = W / 2 - 20; x < W / 2 + 20; x += 2) {
                            int p = pm.getPixel(x, y);
                            if (((p >> 24) & 0xFF) + ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) > 300) count[0]++;
                        }
                com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local(file), pm);
                pm.dispose();
                sr.dispose();
                fbo.dispose();
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the beam failed to draw", boom.get());
        assertTrue(lit[0] > 200, "a bright beam runs up the middle of the frame (" + lit[0] + " px)");
    }
}

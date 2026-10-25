package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Every mandala at four charges (build/mandala-<style>.png): rows are the seven styles (Akasha, Vayu, Agni, Jala, Prithvi, Brahma, Pashupata), columns 25%,
 *  50%, 80% and a full pulsing charge. Checks that each one draws, and that a full charge is far richer than the first moments (its elements form one by one). */
class MandalaShotTest {
    private static final int CELL = 300, COLS = 4, ROWS = 7;
    private static final float[] KS = {0.25f, 0.5f, 0.8f, 1f};
    private static final int[] STYLES = {0, 1, 2, 3, 4, 5, 6};

    @Test
    void everyMandalaFormsAsTheChargeBuilds() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(64, 64);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        long[][] lit = new long[ROWS][COLS];
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    int w = CELL * COLS, h = CELL * ROWS;
                    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
                    ShapeRenderer sr = new ShapeRenderer(20000);
                    OrthographicCamera cam = new OrthographicCamera(w, h);
                    cam.position.set(w / 2f, h / 2f, 0);
                    cam.update();
                    fbo.begin();
                    ScreenUtils.clear(0.04f, 0.035f, 0.05f, 1f);
                    sr.setProjectionMatrix(cam.combined);
                    Gdx.gl.glEnable(GL20.GL_BLEND);
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    sr.begin(ShapeRenderer.ShapeType.Filled);
                    for (int r = 0; r < ROWS; r++)
                        for (int c = 0; c < COLS; c++)
                            Mandala.draw(sr, c * CELL + CELL / 2f, (ROWS - 1 - r) * CELL + CELL / 2f, KS[c], 2.0f + r * 0.1f, c == COLS - 1, STYLES[r]);
                    sr.end();
                    Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, w, h);
                    fbo.end();
                    for (int r = 0; r < ROWS; r++)
                        for (int c = 0; c < COLS; c++) {
                            long n = 0;
                            for (int y = r * CELL; y < (r + 1) * CELL; y += 2)
                                for (int x = c * CELL; x < (c + 1) * CELL; x += 2) {
                                    int p = pm.getPixel(x, y);
                                    if (((p >> 24) & 0xFF) > 0 && (((p >> 24) & 0xFF) + ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF)) > 150) n++;
                                }
                            lit[r][c] = n;
                        }
                    new File("build").mkdirs();
                    com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local("build/mandala-all.png"), pm);
                    pm.dispose();
                    sr.dispose();
                    fbo.dispose();
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("mandala render failed", boom.get());
        StringBuilder sb = new StringBuilder("Mandala lit pixels per style (25/50/80/full): ");
        for (int r = 0; r < ROWS; r++) {
            sb.append(STYLES[r]).append(java.util.Arrays.toString(lit[r])).append(' ');
            assertTrue(lit[r][0] > 30, "style " + STYLES[r] + " draws something at 25%: " + lit[r][0]);
            // (the whole tightens as it builds, so raw pixels can dip between two stages; the finished mandala is still far richer than the start of it)
            assertTrue(lit[r][COLS - 1] >= 1.2 * lit[r][0], "style " + STYLES[r] + " is richer at a full charge than at 25%: " + lit[r][COLS - 1] + " vs " + lit[r][0]);
        }
        System.out.println(sb);
    }
}

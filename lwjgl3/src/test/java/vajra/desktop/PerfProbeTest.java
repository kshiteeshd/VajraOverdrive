package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.screen.Probe;
import vajra.screen.Ui;
import vajra.screen.WorldRenderer;
import vajra.world.World;

/** WHERE THE FRAME GOES: the real renderer in three places of the world (the Golden Door, the Mirage Wood, the Hall), each pass timed
 *  alone with the GPU waited on (vajra.screen.Probe), plus the CPU cost of the whole call. Prints a table and fails if the average frame
 *  is slower than a 60 Hz budget. */
class PerfProbeTest {
    private static final int W = 1920, H = 1080;     // a real full-HD window: the pilot's frame, not the hidden 720p one

    @Test
    void everyPassIsAccountedForAndTheFrameFitsSixtyHertz() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        float[] worst = new float[1];
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
                    for (float y : new float[] {3000, 5600, 19000}) {
                        w.ship.x = w.width / 2;
                        w.ship.y = y;
                        cam.position.set(w.ship.x, w.ship.y, 0);
                        cam.update();
                        vp.update(W, H);
                        for (int k = 0; k < 30; k++) r.render(w, cam, vp);          // warm: chunks, JIT, bake
                        Probe.enabled = true;
                        Probe.reset();
                        final int frames = 90;
                        for (int k = 0; k < frames; k++) r.render(w, cam, vp);
                        Probe.enabled = false;
                        long total = 0;
                        StringBuilder sb = new StringBuilder("Perf y=" + y + " (1920x1080, ms per frame):");
                        for (int i = 0; i < Probe.NAMES.length; i++) {
                            total += Probe.NS[i];
                            sb.append(String.format("%n    %-28s %6.2f", Probe.NAMES[i], Probe.NS[i] / 1e6 / frames));
                        }
                        sb.append(String.format("%n    %-28s %6.2f", "TOTAL (GPU waited)", total / 1e6 / frames));
                        // and the plain frame, no waits
                        Gdx.gl.glFinish();
                        long t0 = System.nanoTime();
                        for (int k = 0; k < frames; k++) r.render(w, cam, vp);
                        Gdx.gl.glFinish();
                        float plain = (System.nanoTime() - t0) / 1e6f / frames;
                        sb.append(String.format("%n    %-28s %6.2f", "plain frame", plain));
                        System.out.println(sb);
                        worst[0] = Math.max(worst[0], plain);
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Probe.enabled = false;
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("perf probe crashed", boom.get());
        assertTrue(worst[0] < 16f, "the world renders inside a 60 Hz frame at 1080p: worst plain frame " + worst[0] + " ms");
    }
}

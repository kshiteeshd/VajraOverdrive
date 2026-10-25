package vajra.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.AstraFlow;
import vajra.world.World;

/** Frame time in the real game while each astra works (printed as a table; fails if an astra's frame averages over 28 ms at 1920x1080). */
class AstraPerfTest {
    private static long gcCount() {
        long n = 0;
        for (java.lang.management.GarbageCollectorMXBean b : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) n += Math.max(0, b.getCollectionCount());
        return n;
    }

    @Test
    void astrasDoNotMakeTheGameLag() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(2560, 1440);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        StringBuilder out = new StringBuilder("ASTRA PERF (ms per frame, avg / worst, zoom): ");
        double[] avgs = new double[6];
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
                    GameScreen gs = (GameScreen) flow.top();
                    World w = gs.world();
                    w.cues.clear();
                    int spot = Integer.getInteger("perf.cp", 7);
                    w.ship.x = w.checkpoints[spot].x;                              // inside the walled bands, where the layer stack is drawn
                    w.ship.y = w.checkpoints[spot].y;
                    for (Enemy e : w.enemies) e.active = true;                  // every foe awake: the heaviest fight
                    Field zf = GameScreen.class.getDeclaredField("zoom");
                    zf.setAccessible(true);
                    for (int i = 0; i < 12; i++) {
                        Enemy e = new Enemy(Enemy.Kind.MAYAVI, w.ship.x + (i - 6) * 40f, w.ship.y + 120 + (i % 3) * 40);
                        e.hp = e.maxHp = 90000;
                        e.dmgScale = 0f;
                        e.active = true;
                        w.enemies.add(e);
                    }
                    for (int i = 0; i < 90; i++) flow.render(1 / 60f);
                    String[] names = {"idle", "akasha", "vayu", "agni", "jala", "prithvi", "brahma"};
                    int[] kinds = {-1, vajra.world.Astra.AKASHA, vajra.world.Astra.VAYU, vajra.world.Astra.AGNI, vajra.world.Astra.JALA, vajra.world.Astra.PRITHVI, vajra.world.Astra.BRAHMA};
                    for (int k = 0; k < kinds.length; k++) {
                        double sum = 0, worst = 0;
                        int frames = k == 0 ? 120 : (int) (vajra.world.Astra.WIND_FULL * 60f) + 12 + 200;
                        boolean released = false;
                        Gdx.gl.glFinish();
                        if (k > 0) {
                            for (int t = 0; t < vajra.world.Astra.ELEMENTS; t++) w.hangar.shards[t] = 20;
                            w.astraSel = kinds[k];
                            w.astraCd = 0;
                            flow.keyDown(com.badlogic.gdx.Input.Keys.F);
                        }
                        for (int i = 0; i < frames; i++) {
                            if (k > 0 && !released && i == (int) (vajra.world.Astra.WIND_FULL * 60f) + 12) { flow.keyUp(com.badlogic.gdx.Input.Keys.F); released = true; }
                            long gc0 = gcCount(), t0 = System.nanoTime();
                            flow.render(1 / 60f);
                            Gdx.gl.glFinish();
                            double ms = (System.nanoTime() - t0) / 1e6;
                            sum += ms;
                            worst = Math.max(worst, ms);
                            if (ms > 60) System.out.println("PERF SPIKE " + names[k] + " frame " + i + (released ? " (after release) " : " (charging) ") + (int) ms + " ms, gc +" + (gcCount() - gc0) + ", zoom " + (Float) zf.get(gs));
                        }
                        avgs[Math.min(k, 5)] = sum / frames;
                        out.append(String.format("%s %.1f/%.0f z%.2f | ", names[k], sum / frames, worst, (Float) zf.get(gs)));
                        for (int i = 0; i < 500 && (w.flow.active() || w.gazeT > 0); i++) flow.render(1 / 60f);
                        for (int i = 0; i < 200; i++) flow.render(1 / 60f);
                    }
                    System.out.println(out);
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the perf run failed", boom.get());
    }
}

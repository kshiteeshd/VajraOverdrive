package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.Ui;
import vajra.world.World;

/** THE WHOLE GAME FRAME at 1080p with W held: update (the sim, the AI, the hangar), render (world + HUD), each timed with the GPU waited
 *  on, in three places of the world. Prints the table; fails if the average frame is over the 60 Hz budget. */
class GameFrameProbeTest {
    private static final int W = 1920, H = 1080;

    @Test
    void theWholeFrameFitsSixtyHertz() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        float[] worst = new float[1];
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                Input real = Gdx.input;
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    GameScreen g = new GameScreen(batch, flow, ui);
                    flow.push(g);
                    Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                            (proxy, m, args) -> m.getName().equals("isKeyPressed") && (Integer) args[0] == Input.Keys.W
                                    ? Boolean.TRUE : m.invoke(real, args));
                    Field f = GameScreen.class.getDeclaredField("world");
                    f.setAccessible(true);
                    World w = (World) f.get(g);
                    Field dir = GameScreen.class.getDeclaredField("director");
                    dir.setAccessible(true);
                    for (float y : new float[] {3000, 5600, 14000}) {
                        w.ship.x = w.width / 2;
                        w.ship.y = y;
                        for (int i = 0; i < 40; i++) {                                   // warm and let the scenes pass
                            Object d = dir.get(g);
                            if ((Boolean) d.getClass().getMethod("active").invoke(d)) d.getClass().getMethod("skip").invoke(d);
                            g.update(1 / 60f);
                            g.render();
                        }
                        double up = 0, rn = 0;
                        final int frames = 120;
                        for (int i = 0; i < frames; i++) {
                            Gdx.gl.glFinish();
                            long t0 = System.nanoTime();
                            g.update(1 / 60f);
                            long t1 = System.nanoTime();
                            g.render();
                            Gdx.gl.glFinish();
                            long t2 = System.nanoTime();
                            up += (t1 - t0) / 1e6;
                            rn += (t2 - t1) / 1e6;
                            w.ship.y = y;                                                   // hold the spot
                        }
                        float total = (float) ((up + rn) / frames);
                        System.out.printf("GameFrame y=%.0f: update %.2f ms, render (world+HUD) %.2f ms, total %.2f ms (%d enemies, %d bullets)%n",
                                y, up / frames, rn / frames, total, w.enemies.size(), w.bullets.size());
                        worst[0] = Math.max(worst[0], total);
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.input = real;
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the game frame crashed", boom.get());
        assertTrue(worst[0] < 16f, "the whole frame fits 60 Hz: worst average " + worst[0] + " ms");
    }
}

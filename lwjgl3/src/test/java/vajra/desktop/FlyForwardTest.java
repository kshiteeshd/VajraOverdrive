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

/** The real GameScreen, the real renderer, W held down: the hull must climb from the spawn, through the boulder belt's
 *  pass, and keep going. (The keyboard is a stub and ENTER is pressed through the opening scene, which waits for it; everything
 *  else is the shipped game loop.) */
class FlyForwardTest {
    @Test
    void holdingForwardFliesTheHullUpTheWorld() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        float[] reached = new float[2];
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
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
                    vajra.world.World.walls = false;   // a straight line up the real caverns would hit rock
                    GameScreen g;
                    try {
                        g = new GameScreen(batch, flow, ui);
                    } finally {
                        vajra.world.World.walls = true;
                    }
                    flow.push(g);
                    Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] {Input.class},
                            (proxy, m, args) -> m.getName().equals("isKeyPressed") && (Integer) args[0] == Input.Keys.W
                                    ? Boolean.TRUE : m.invoke(real, args));
                    Field f = GameScreen.class.getDeclaredField("world");
                    f.setAccessible(true);
                    World w = (World) f.get(g);
                    for (int i = 0; i < 60 * 20; i++) {
                        g.update(1 / 60f);
                        g.render();
                        Field dir = GameScreen.class.getDeclaredField("director");
                        dir.setAccessible(true);
                        Object dd = dir.get(g);
                        if ((Boolean) dd.getClass().getMethod("active").invoke(dd)) dd.getClass().getMethod("skip").invoke(dd); // ENTER through every scene
                        if (i % 300 == 0) {
                            Field df = GameScreen.class.getDeclaredField("director");
                            df.setAccessible(true);
                            Object d = df.get(g);
                            boolean active = (Boolean) d.getClass().getMethod("active").invoke(d);
                            Field sf = GameScreen.class.getDeclaredField("shop");
                            sf.setAccessible(true);
                            Object sh = sf.get(g);
                            boolean open = (Boolean) sh.getClass().getMethod("isOpen").invoke(sh);
                            Field wf = GameScreen.class.getDeclaredField("wheelOpen");
                            wf.setAccessible(true);
                            System.out.println("FlyForward: frame " + i + " ship (" + w.ship.x + ", " + w.ship.y + ") clock " + w.clock
                                    + " director " + active + " shop " + open + " wheel " + wf.get(g)
                                    + " hitStop " + w.hitStop + " lives " + w.lives + " hp " + w.ship.hp);
                        }
                        reached[0] = w.ship.x;
                        reached[1] = w.ship.y;
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.input = real;
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the game loop crashed", boom.get());
        assertTrue(reached[1] > 2500, "holding W for 20 s flew the hull to y=" + reached[1] + " (x=" + reached[0] + ")");
    }
}

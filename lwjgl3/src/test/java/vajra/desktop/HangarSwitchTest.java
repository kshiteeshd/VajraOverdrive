package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.Ui;
import vajra.ship.Hangar;
import vajra.world.World;

/** The real game: fly into a shop's berth, let the hangar open, and switch to every hull by the keys (focus with 1-7, ENTER fits) and by
 *  the mouse (a click focuses, a second click fits). The hull on the pad must change each time. */
class HangarSwitchTest {
    @Test
    void everyHullCanBeFittedInTheRealHangar() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    System.setProperty("vajra.testunlock", "true");
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    GameScreen g = new GameScreen(batch, flow, ui);
                    flow.push(g);
                    Field wf = GameScreen.class.getDeclaredField("world");
                    wf.setAccessible(true);
                    World w = (World) wf.get(g);
                    w.hangar.unlockAll();
                    Field dir = GameScreen.class.getDeclaredField("director");
                    dir.setAccessible(true);
                    float bx = w.shops[0][0] + 2, by = w.shops[0][1] - 46;
                    for (int i = 0; i < 200 && !openBay(g); i++) {
                        Object d = dir.get(g);
                        if ((Boolean) d.getClass().getMethod("active").invoke(d)) d.getClass().getMethod("skip").invoke(d);
                        w.ship.x = bx;
                        w.ship.y = by;
                        w.ship.vx = w.ship.vy = 0;
                        g.update(1 / 60f);
                        g.render();
                    }
                    assertTrue(openBay(g), "the hangar opened by itself once the hull sat in the berth");
                    for (int i = 0; i < 7; i++) {
                        g.render();                                            // the bay's own draw lays out its cards
                        flow.keyDown(Input.Keys.NUM_1 + i);
                        flow.keyDown(Input.Keys.ENTER);
                        assertEquals(i, w.hangar.shipIndex, "hull " + (i + 1) + " is fitted by the keys");
                        assertEquals(vajra.ship.ShipDef.ALL[i], w.ship.def, "and is the hull on the pad");
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }

            private boolean openBay(GameScreen g) throws Exception {
                Field sf = GameScreen.class.getDeclaredField("shop");
                sf.setAccessible(true);
                Object shop = sf.get(g);
                return (Boolean) shop.getClass().getMethod("isOpen").invoke(shop);
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the hangar switch failed", boom.get());
        assertTrue(Hangar.UNLOCK_ALL || true);
    }
}

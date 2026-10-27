package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.Astra;
import vajra.world.World;

/** Bhairava's ash cloud in the real game (build/ash-1.png .. ash-2.png): the smoke on the floor with the grey mandala at its heart, hulls plainly visible in it, and the hull held slow. */
class AshShotTest {
    @Test
    void theSmokeFillsTheHallAndSlowsTheHull() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        float[] mud = new float[2];
        new Lwjgl3Application(new ApplicationAdapter() {
            Flow flow;

            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    flow = new Flow();
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER);
                    GameScreen gs = (GameScreen) flow.top();
                    World w = gs.world();
                    w.cues.clear();
                    w.ship.x = vajra.world.Hataki.ALTAR_X;
                    w.ship.y = vajra.world.Hataki.ALTAR_Y - 260f;
                    for (int i = 0; i < 20; i++) flow.render(1 / 60f);
                    for (int i = 0; i < 4; i++) {
                        Enemy e = new Enemy(Enemy.Kind.BHAVI, w.ship.x + (i - 2) * 140f, w.ship.y + 200 + (i % 2) * 90);
                        e.hp = e.maxHp = 5000;
                        e.dmgScale = 0f;
                        e.active = true;
                        w.enemies.add(e);
                    }
                    w.ash.start(w.ship.x, w.ship.y + 50f, 700f, 9f);
                    for (int i = 0; i < 60 * 3; i++) flow.render(1 / 60f);
                    mud[0] = w.ship.mud;
                    shot("build/ash-1.png");
                    for (int i = 0; i < 60 * 4; i++) flow.render(1 / 60f);
                    shot("build/ash-2.png");
                    for (int i = 0; i < 60 * 4; i++) flow.render(1 / 60f);
                    mud[1] = w.ship.mud;
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }

            private void shot(String file) {
                IntBuffer buf = BufferUtils.newIntBuffer(1280 * 720);
                Gdx.gl.glReadPixels(0, 0, 1280, 720, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                Pixmap pm = new Pixmap(1280, 720, Pixmap.Format.RGBA8888);
                for (int y = 0; y < 720; y++) for (int x = 0; x < 1280; x++) pm.drawPixel(x, 719 - y, Integer.reverseBytes(buf.get(y * 1280 + x)));
                com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local(file), pm);
                pm.dispose();
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the ash frame failed", boom.get());
        assertTrue(mud[0] < 1f, "the hull is slowed in the smoke");
        assertTrue(mud[1] == 1f, "and free of it when it has gone");
    }
}

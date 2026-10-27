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

/** The settings screen, tab by tab (build/settings-*.png). */
class SettingsShotTest {
    @Test
    void everyTabDraws() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Flow flow;

            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    flow = new Flow();
                    flow.set(new HomeScreen(flow, ui, batch));
                    for (int i = 0; i < 3; i++) flow.render(1 / 60f);
                    shot("build/settings-home.png");
                    flow.keyDown(Input.Keys.DOWN);
                    flow.keyDown(Input.Keys.DOWN);
                    flow.keyDown(Input.Keys.ENTER);
                    for (int tab = 0; tab < 6; tab++) {
                        flow.keyDown(Input.Keys.S);
                        for (int i = 0; i < 3; i++) flow.render(1 / 60f);
                        shot("build/settings-" + tab + ".png");
                        flow.keyDown(Input.Keys.E);
                    }
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
        if (boom.get() != null) throw new AssertionError("the settings failed", boom.get());
        assertTrue(true);
    }
}

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

/** Bala's broken mirror in the real game (build/mirror-1.png .. mirror-4.png): the cracks running out from the mandala, the glass whole but cracked, every piece turned, mended. */
class MirrorShotTest {
    @Test
    void theScreenCracksTurnsAndMends() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        boolean[] ended = new boolean[1];
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
                    w.ship.y = 800;
                    for (int i = 0; i < 30; i++) flow.render(1 / 60f);
                    for (int i = 0; i < 10; i++) {
                        Enemy e = new Enemy(Enemy.Kind.MAYAVI, w.ship.x + (i - 5) * 90f, w.ship.y + 220 + (i % 3) * 70);
                        e.hp = e.maxHp = 5000;
                        e.dmgScale = 0f;
                        e.active = true;
                        w.enemies.add(e);
                    }
                    w.maya.start(w.ship.x + 120f, w.ship.y + 200f, 5L);
                    float[] at = {2.7f, 6.0f, 8.5f, 11.5f};
                    int shot = 0;
                    for (int i = 0; i < 60 * 16; i++) {
                        flow.render(1 / 60f);
                        if (shot < at.length && w.maya.t >= at[shot]) shot("build/mirror-" + (++shot) + ".png");
                    }
                    ended[0] = !w.maya.active;
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
        if (boom.get() != null) throw new AssertionError("the mirror frame failed", boom.get());
        assertTrue(ended[0], "the glass mends and the effect ends");
    }
}

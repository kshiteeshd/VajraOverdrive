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

/** The real game with an astra at work: the camera pulls back (to see all of Agni's bloom and Vayu's tornado), nothing crashes at that zoom, and it eases back after
 *  (build/astra-zoom-agni.png and build/astra-zoom-vayu.png). */
class AstraZoomTest {
    @Test
    void theViewPullsBackForAnAstraAndComesBack() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        float[] zooms = new float[4];
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
                    Field zf = GameScreen.class.getDeclaredField("zoom");
                    zf.setAccessible(true);
                    for (int i = 0; i < 12; i++) {
                        Enemy e = new Enemy(Enemy.Kind.MAYAVI, w.ship.x + (i - 6) * 70f, w.ship.y + 260 + (i % 3) * 60);
                        e.hp = e.maxHp = 5000;
                        e.dmgScale = 0f;
                        e.active = true;
                        w.enemies.add(e);
                    }
                    for (int pass = 0; pass < 2; pass++) {
                        int kind = pass == 0 ? Astra.AGNI : Astra.VAYU;
                        w.hangar.shards[kind] = 5;
                        w.hangar.owned[w.hangar.shipIndex][1] = true;
                        w.astraSel = kind;
                        w.astraCd = 0;
                        flow.keyDown(Input.Keys.F);
                        for (int i = 0; i < (int) (Astra.WIND_FULL * 60f) + 15; i++) flow.render(1 / 60f);
                        flow.keyUp(Input.Keys.F);
                        for (int i = 0; i < 80; i++) flow.render(1 / 60f);
                        zooms[pass] = (Float) zf.get(gs);
                        shot(pass == 0 ? "build/astra-zoom-agni.png" : "build/astra-zoom-vayu.png");
                        for (int i = 0; i < 60 * 8 && (w.flow.active() || w.gazeT > 0); i++) flow.render(1 / 60f);
                        for (int i = 0; i < 60 * 6; i++) flow.render(1 / 60f);
                        zooms[2 + pass] = (Float) zf.get(gs);
                        w.ship.hp = w.ship.maxHp();
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
        if (boom.get() != null) throw new AssertionError("the zoomed astra frame failed", boom.get());
        assertTrue(zooms[0] > 1.6f, "Agni's bloom pulls the view back: " + zooms[0]);
        assertTrue(zooms[1] > 1.5f, "so does Vayu's tornado: " + zooms[1]);
        assertTrue(zooms[2] < 1.35f && zooms[3] < 1.35f, "and it comes back to the ordinary range (the busy-fight pull is 1.3): " + zooms[2] + ", " + zooms[3]);
    }
}

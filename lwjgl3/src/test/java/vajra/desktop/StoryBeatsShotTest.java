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
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.World;

/** The story beats in the real game (build/beat-*.png): the hull's death (light, the camera travelling to the lamp, light gathering, the hull whole), the stored repairs on the HUD,
 *  the ruler's yielding scene (the camera between him and the pilot), the cloud over the way beyond the gate and its parting, and the piece shown big and then the 1/7 panel. */
class StoryBeatsShotTest {
    @Test
    void theBeatsPlayThrough() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        boolean[] ok = new boolean[3];
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
                    w.ship.x = 2600;
                    w.ship.y = 19300;
                    for (int i = 0; i < 30; i++) flow.render(1 / 60f);

                    // ---- the hull's death
                    w.medkits = 3;
                    w.ship.hp = w.ship.maxHp() * 0.4f;
                    for (int i = 0; i < 5; i++) flow.render(1 / 60f);
                    shot("build/beat-0-hud.png");
                    w.ship.hp = 1;
                    w.ship.invuln = 0;
                    w.ship.damage(999, w);
                    float[] at = {0.5f, 2.2f, 3.4f, 4.15f};
                    int k = 0;
                    for (int i = 0; i < 60 * 6 && w.death.active; i++) {
                        flow.render(1 / 60f);
                        if (k < at.length && w.death.t >= at[k]) shot("build/beat-1-death-" + (++k) + ".png");
                    }
                    ok[0] = k == 4 && !w.death.active;

                    // ---- Bala yields
                    for (int i = 0; i < w.quota.length; i++) w.kills[i] = w.quota[i];
                    w.trySpawnBoss();
                    Enemy b = w.boss;
                    w.ship.x = 2600;
                    w.ship.y = 20250;
                    for (int i = 0; i < 20; i++) flow.render(1 / 60f);
                    shot("build/beat-2-clouds-before.png");
                    w.cues.clear();
                    b.hp = 1;
                    b.hurt(99999, w);
                    int shots = 0;
                    for (int i = 0; i < 60 * 90; i++) {
                        flow.render(1 / 60f);
                        if (i % 90 == 45) {
                            if (shots == 2 || shots == 3 || shots == 5) shot("build/beat-3-scene-" + shots + ".png");
                            flow.keyDown(Input.Keys.ENTER);
                            flow.keyDown(Input.Keys.ENTER);
                            shots++;
                        }
                        if (b.dead) break;
                    }
                    ok[1] = b.dead;
                    float[] after = {1.2f, 2.8f, 4.4f, 6.5f, 8.5f};
                    int a = 0;
                    for (int i = 0; i < 60 * 14 && a < after.length; i++) {
                        flow.render(1 / 60f);
                        float t = (i + 1) / 60f;
                        if (t >= after[a]) shot("build/beat-4-after-" + (++a) + ".png");
                    }
                    ok[2] = w.hangar.pieces[0];
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
        if (boom.get() != null) throw new AssertionError("the beats failed", boom.get());
        assertTrue(ok[0], "the death scene ran its whole length");
        assertTrue(ok[1], "the ruler yielded and let go when the scene was over");
        assertTrue(ok[2], "the piece came to the hull and is held");
    }
}

package vajra.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.Ui;
import vajra.world.Arena;

/** A walk through Vitala for the eyes: one frame of each region and of the ruler's fight (build/vitala-*.png). */
class TmpVitalaTourTest {
    @Test
    void tour() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(1280, 720);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            void shot(String name) throws Exception {
                IntBuffer buf = BufferUtils.newIntBuffer(1280 * 720);
                Gdx.gl.glReadPixels(0, 0, 1280, 720, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                int[] px = new int[1280 * 720];
                buf.get(px);
                BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < 720; y++)
                    for (int x = 0; x < 1280; x++) {
                        int p = px[(719 - y) * 1280 + x];
                        img.setRGB(x, y, ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF));
                    }
                new File("build").mkdirs();
                ImageIO.write(img, "png", new File("build/" + name + ".png"));
            }

            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    Flow flow = new Flow();
                    List<Arena.Scenario> world = Arena.in(Arena.WORLD);
                    int k = 0;
                    for (Arena.Scenario s : world) {
                        if (!s.name.startsWith("VITALA, REGION")) continue; if (true) continue;
                        GameScreen g = GameScreen.arena(batch, flow, ui, s);
                        flow.push(g);
                        for (int i = 0; i < 240; i++) { flow.update(1 / 60f); flow.render(1 / 60f); }
                        shot("vitala-region-" + (++k));
                        flow.pop();
                    }
                    for (Arena.Scenario s : Arena.in(Arena.BOSSES)) {
                        if (!s.name.startsWith("BHAIRAVA")) continue;
                        GameScreen g = GameScreen.arena(batch, flow, ui, s);
                        flow.push(g);
                        System.out.println("BOSSPOS " + s.name + " boss=" + g.world().boss.x + "," + g.world().boss.y + " ship=" + g.world().ship.x + "," + g.world().ship.y + " pit=" + g.world().pitX + "," + g.world().pitY + " wake=" + g.world().bossWakeY);
                        for (int q = 0; q < 3; q++) { flow.keyDown(com.badlogic.gdx.Input.Keys.ENTER); for (int i = 0; i < 12; i++) { flow.update(1 / 60f); flow.render(1 / 60f); } }
                        for (int i = 0; i < 150; i++) { g.world().ship.x = 2600; g.world().ship.y = g.world().pitY - 280; g.world().ship.invuln = 99; flow.update(1 / 60f); flow.render(1 / 60f); }
                        shot("vitala-boss-a-" + s.name.replaceAll("[^A-Z0-9]+", "_"));
                        for (int i = 0; i < 300; i++) { g.world().ship.x = 2600 + 140 * (float) Math.sin(i * 0.02); g.world().ship.y = g.world().pitY - 280; g.world().ship.invuln = 99; flow.update(1 / 60f); flow.render(1 / 60f); }
                        System.out.println("STATE clock=" + g.world().clock + " boss.active=" + g.world().boss.active + " vState=" + g.world().boss.vState + " hp=" + g.world().boss.hp + " ash=" + g.world().ash.active + " t=" + g.world().ash.t + " shipHp=" + g.world().ship.hp + " bossXY=" + g.world().boss.x + "," + g.world().boss.y + " ship=" + g.world().ship.x + "," + g.world().ship.y);
                        shot("vitala-boss-b-" + s.name.replaceAll("[^A-Z0-9]+", "_"));
                        flow.pop();
                    }
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("tour failed", boom.get());
    }
}

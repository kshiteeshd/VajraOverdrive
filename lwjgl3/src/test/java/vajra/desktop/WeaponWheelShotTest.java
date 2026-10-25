package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.profile.Profiles;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.World;

/** The weapon key in a real run: a tap steps to the next pattern, a hold opens the wheel and slows the world, and
 *  letting go fits what the pointer is on. build/wheel-open.png shows the wheel over a run with the vedic HUD. */
class WeaponWheelShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void tapStepsHoldOpensTheWheelAndSlowsTheWorld() {
        List<String> failures = new ArrayList<>();
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Ui ui;
            SpriteBatch batch;
            Flow flow;

            @Override
            public void create() {
                try {
                    ui = new Ui();
                    batch = new SpriteBatch();
                    flow = new Flow();
                    Profiles.clearRun();
                    flow.set(new HomeScreen(flow, ui, batch));
                    flow.keyDown(Input.Keys.ENTER); // NEW GAME (the story is off under test)
                    GameScreen g = (GameScreen) flow.top();
                    World w = g.world();
                    w.cues.clear();
                    for (int i = 1; i <= 4; i++) w.hangar.weaponOwned[i] = true;
                    for (int i = 0; i < 20; i++) frame();

                    // a tap steps to the next pattern you own
                    int before = w.hangar.weaponIndex;
                    flow.keyDown(Input.Keys.V);
                    for (int i = 0; i < 3; i++) frame();           // 0.05 s: well under the hold
                    flow.keyUp(Input.Keys.V);
                    if (w.hangar.weaponIndex != w.hangar.cycleWeapon(before, 1)) failures.add("a tap did not step to the next weapon");

                    // a hold opens the wheel and the world crawls
                    float c0 = w.clock;
                    for (int i = 0; i < 30; i++) frame();
                    float free = w.clock - c0;
                    flow.keyDown(Input.Keys.V);
                    for (int i = 0; i < 20; i++) frame();           // 0.33 s: the wheel is open now
                    float c1 = w.clock;
                    for (int i = 0; i < 30; i++) frame();
                    float slow = w.clock - c1;
                    write(frame(), "build/wheel-open.png");
                    if (!(slow < free * 0.5f)) failures.add("the world was not slowed by the wheel (" + free + " vs " + slow + ")");
                    flow.keyUp(Input.Keys.V);
                    if (!w.hangar.weaponOwned[w.hangar.weaponIndex]) failures.add("letting go fitted a weapon you do not own");
                    float c2 = w.clock;
                    for (int i = 0; i < 30; i++) frame();
                    if (!((w.clock - c2) > free * 0.8f)) failures.add("the world did not speed back up once the wheel closed");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Profiles.clearRun();
                    Gdx.app.exit();
                }
            }

            int[] frame() {
                flow.render(1 / 60f);
                IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                int[] px = new int[W * H];
                buf.get(px);
                return px;
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("wheel flow crashed", boom.get());
        assertEquals(List.of(), failures);
    }

    private static void write(int[] px, String path) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int p = px[(H - 1 - y) * W + x];
                    img.setRGB(x, y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
                }
            new File(path).getParentFile().mkdirs();
            ImageIO.write(img, "png", new File(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

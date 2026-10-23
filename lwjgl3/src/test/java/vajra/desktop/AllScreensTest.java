package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.screen.CodexScreen;
import vajra.screen.ControlScreen;
import vajra.screen.HomeScreen;
import vajra.screen.NetScreen;
import vajra.screen.PauseScreen;
import vajra.screen.ProfileScreen;
import vajra.screen.SettingsScreen;
import vajra.screen.SplashScreen;
import vajra.screen.TutorialScreen;
import vajra.screen.Ui;
import vajra.world.World;

/** Every full-screen page, rendered in the real renderer into build/screen-NAME.png, so the whole
 *  set can be read side by side for consistency. The assertions only prove that each page draws
 *  its frame (the gold corner brackets) and a title; the eyes do the rest. */
class AllScreensTest {
    private static final int W = 1280, H = 720;

    @Test
    void everyPageDrawsTheSameFrame() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        List<String> failures = new ArrayList<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Flow flow;
            Ui ui;
            SpriteBatch batch;

            @Override
            public void create() {
                try {
                    ui = new Ui();
                    batch = new SpriteBatch();
                    flow = new Flow();
                    World w = new World();
                    w.cues.clear();
                    shot("splash", new SplashScreen(flow, ui, batch), true);
                    shot("home", new HomeScreen(flow, ui, batch), false);
                    shot("controls", new ControlScreen(flow, ui, batch), false);
                    shot("tutorials", new TutorialScreen(flow, ui, batch), false);
                    shot("profile", new ProfileScreen(flow, ui, batch), false);
                    shot("settings", new SettingsScreen(flow, ui, batch), false);
                    shot("multiplayer", new NetScreen(flow, ui, batch), false);
                    shot("pause", new PauseScreen(flow, ui, batch, w), false);
                    shot("codex", new CodexScreen(flow, ui, batch, w), false);
                } catch (Throwable t) {
                    boom.set(t);
                }
                Gdx.app.exit();
            }

            void shot(String name, FlowState s, boolean splash) {
                flow.set(s);
                int frames = splash ? 300 : 6; // the splash holds its logo from ~2 s
                for (int i = 0; i < frames; i++) flow.render(1 / 60f);
                IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                int[] px = new int[W * H];
                buf.get(px);
                write(px, "build/screen-" + name + ".png");
                if (!splash && corner(px) < 20) failures.add(name + " has no frame bracket in its top-left corner");
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("a page crashed", boom.get());
        assertTrue(failures.isEmpty(), String.join("; ", failures));
    }

    /** Lit pixels in the top-left bracket's box (UI 28..72 x, 648..692 y, bottom-up). */
    private static int corner(int[] px) {
        int n = 0;
        for (int y = 640; y < 700; y++)
            for (int x = 20; x < 80; x++) {
                int p = px[y * W + x];
                if ((p & 0xff) > 60 || ((p >> 8) & 0xff) > 60 || ((p >> 16) & 0xff) > 60) n++;
            }
        return n;
    }

    private static void write(int[] px, String path) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int p = px[y * W + x];
                    img.setRGB(x, H - 1 - y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
                }
            File out = new File(path);
            out.getParentFile().mkdirs();
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

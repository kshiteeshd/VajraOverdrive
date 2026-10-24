package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.BufferUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.profile.Profiles;
import vajra.screen.FxImages;
import vajra.screen.Hud;
import vajra.screen.ShipRenderer;
import vajra.screen.Ui;
import vajra.world.Score;
import vajra.world.World;

/** The vedic HUD beside the classic one, same world, same frame: build/hud-vedic-*.png and hud-classic-*.png.
 *  Also checks the swap is real (the two draw different pixels), that every instrument lands on pixels, and that
 *  nothing the vedic set does not own has moved (the score and the vitals line stay put). */
class VedicHudShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void theVedicSetDrawsAndTheSwapIsReal() {
        List<String> failures = new ArrayList<>();
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            Ui ui;
            SpriteBatch batch;
            Hud hud;
            ShipRenderer ships;
            FrameBuffer fbo;

            @Override
            public void create() {
                int was = Profiles.active().settings.hudStyle;
                try {
                    ui = new Ui();
                    batch = new SpriteBatch();
                    hud = new Hud(ui);
                    ships = new ShipRenderer(new FxImages());
                    fbo = new FrameBuffer(Pixmap.Format.RGBA8888, W, H, false);

                    World w = scene();
                    Profiles.active().settings.hudStyle = 0;
                    int[] classic = shot(w, dark());
                    write(classic, "build/hud-classic-ready.png");
                    Profiles.active().settings.hudStyle = 1;
                    int[] vedic = shot(w, dark());
                    write(vedic, "build/hud-vedic-ready.png");
                    if (diff(classic, vedic) < 1500) failures.add("the two HUDs draw nearly the same frame");

                    // every instrument puts pixels where it lives (over the dark field)
                    if (lit(vedic, 70, 330, 672, 702) < 500) failures.add("no health tube out of the medallion");
                    if (lit(vedic, 16, 84, 636, 704) < 250) failures.add("no weapon medallion top-left");
                    if (lit(vedic, 100, 230, 646, 672) < 120) failures.add("no life rings under the tube");
                    if (lit(vedic, 14, 150, 14, 150) < 500) failures.add("no minimap dial bottom-left");
                    if (lit(vedic, 14, 40, 40, 130) < 40) failures.add("no shield arc on the west side of the dial");
                    if (lit(vedic, 136, 164, 40, 130) < 40) failures.add("no boost arc on the east side of the dial");
                    if (lit(vedic, 1200, 1270, 14, 75) < 100) failures.add("no astra gem bottom-right");
                    if (lit(vedic, 1100, 1275, 650, 705) > 0) failures.add("something is drawn top-right: the astra count is gone from there");
                    if (lit(vedic, 1200, 1275, 270, 600) < 120) failures.add("no world meter on the right edge");
                    if (lit(vedic, 560, 720, 610, 650) > 0) failures.add("the eye is on screen while Bala is unaware of the pilot");
                    // what the vedic set must NOT own: the score stays gold top-centre in both
                    if (lit(classic, 500, 780, 684, 712) > 0 && lit(vedic, 500, 780, 684, 712) < 200)
                        failures.add("the score moved or vanished under the vedic HUD");

                    w.ship.hp = w.ship.maxHp() * 0.2f; // the low-hull cry
                    w.ship.shieldEnergy = 0;
                    w.lives = 2;
                    w.gazeStage = 2;
                    w.kills[0] = 40; // push the gaze up
                    for (int i = 0; i < 40; i++) shot(w, dark()); // the eye eases open once he notices
                    int[] low = shot(w, dark());
                    if (lit(low, 560, 720, 610, 650) < 80) failures.add("no gaze eye top-centre once Bala has noticed");
                    write(low, "build/hud-vedic-low.png");

                    w.lokaIndex = 6; // Patala pays every element: five shard rows
                    for (int t = 0; t < 5; t++) w.hangar.shards[t] = 2 + t; // 2..6 shards: the loaded astra can fly
                    w.astraSel = 3;
                    write(shot(w, dark()), "build/hud-vedic-patala.png");

                    w.ship.y = 3000;
                    write(shot(w, rock()), "build/hud-vedic-rock.png"); // over bright rock
                    Profiles.active().settings.hudStyle = 0;
                    write(shot(w, rock()), "build/hud-classic-rock.png");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Profiles.active().settings.hudStyle = was;
                    Gdx.app.exit();
                }
            }

            int[] shot(World w, float[] bg) {
                fbo.begin();
                Gdx.gl.glClearColor(bg[0], bg[1], bg[2], 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                hud.render(batch, w, ships, null);
                IntBuffer buf = BufferUtils.newIntBuffer(W * H);
                Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
                fbo.end();
                int[] px = new int[W * H];
                buf.get(px);
                return px;
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("HUD render crashed", boom.get());
        assertEquals(List.of(), failures);
    }

    private static float[] dark() { return new float[] {13 / 255f, 13 / 255f, 26 / 255f}; }

    private static float[] rock() { return new float[] {0.91f, 0.90f, 0.88f}; }

    private static World scene() {
        World w = new World();
        w.ship.y = 4100; // Maya Vana: past the gate, so the meter, the eye and the yojana are alive
        w.ship.hp = w.ship.maxHp() * 0.8f;
        w.ship.shieldEnergy = w.ship.shieldCap() * 0.6f;
        w.ship.boostEnergy = 70;
        w.lives = 3;
        w.score.points = 123456;
        w.score.combo = 7;
        w.score.comboT = Score.WINDOW;
        w.kills[0] = 12;
        return w;
    }

    /** How many pixels differ between two frames. */
    private static int diff(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) n++;
        return n;
    }

    /** Pixels brighter than the dark field in a box (bottom-up buffer, UI y). */
    private static int lit(int[] px, int x0, int x1, int y0, int y1) {
        int n = 0;
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int p = px[y * W + x];
                if ((p & 0xff) + ((p >> 8) & 0xff) + ((p >> 16) & 0xff) > 150) n++;
            }
        return n;
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

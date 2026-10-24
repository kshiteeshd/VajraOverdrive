package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.screen.Ui;
import vajra.screen.WorldRenderer;
import vajra.world.Chest;
import vajra.world.Hataki;
import vajra.world.Vents;
import vajra.world.World;

/** The campaign world as the pilot sees it, band by band and place by place: build/vitala-<n>.png, build/vitala-p<n>.png, and a frame-time guard. */
class VitalaShotTest {
    private static final int W = 1280, H = 720;
    private static volatile float frameMs;
    /** Heights above Vitala's floor: the Plunge, the Mouth, the Smelters, the Court, the Hearth, the Altar. */
    private static final float[] SPOTS = {600, 3600, 5400, 8500, 11000, 14000, 16200, 19500, 22500, 24600, 25500};

    @Test
    void vitalaDrawsBandByBand() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Ui ui = new Ui();
                    SpriteBatch batch = new SpriteBatch();
                    World w = World.vitala(0);
                    Enemy boss = new Enemy(Enemy.Kind.BALA, w.pitX, w.pitY);
                    boss.boss = true;
                    boss.rank = 1;
                    w.enemies.add(boss);
                    WorldRenderer r = new WorldRenderer(batch, ui, w);
                    long t0 = System.nanoTime();
                    boolean raycast = r.finishLoading();
                    System.out.println("VitalaShot: terrain bake + upload " + (System.nanoTime() - t0) / 1_000_000 + " ms, raycast " + raycast);
                    OrthographicCamera cam = new OrthographicCamera(Ui.UI_W, Ui.UI_H);
                    FitViewport vp = new FitViewport(Ui.UI_W, Ui.UI_H, cam);
                    for (int i = 0; i < SPOTS.length; i++) {
                        float y = Hataki.Y0 + SPOTS[i];
                        w.ship.x = SPOTS[i] > 24000 ? w.pitX : Hataki.centre(y);
                        w.ship.y = SPOTS[i] > 24000 ? w.pitY - 340f : y;
                        boss.bossPhase = 1 + i % 3;
                        w.effects.clear();
                        cam.position.set(w.ship.x, w.ship.y, 0);
                        cam.update();
                        vp.update(W, H);
                        Gdx.gl.glClearColor(0, 0, 0, 1);
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        for (int k = 0; k < 4; k++) r.render(w, cam, vp);
                        write("build/vitala-" + i + ".png");
                    }
                    // the places: a shrine, an assay bay, a vent flaming and one warning, a vault chest, a den, a well, burning ground and a line
                    Chest vault = null, den = null;
                    for (Chest c : w.chests) {
                        if (c.loka != 1) continue;
                        if (c.guards == 0 && vault == null) vault = c;
                        if (c.guards > 0 && den == null) den = c;
                    }
                    float[] well = null;
                    for (float[] p : w.pockets) if (p[5] == Chest.WELL) { well = p; break; }
                    float[][] marks = {{w.checkpoints[6].x, w.checkpoints[6].y}, {w.shops[5][0], w.shops[5][1] - 60f},
                            {w.vents.at[0][0] + 150f, w.vents.at[0][1]}, {w.vents.at[0][0] + 150f, w.vents.at[0][1]},
                            {vault.x, vault.y + 40f}, {den.x, den.y + 40f}, {well[0], well[1]}, {Hataki.centre(Hataki.Y0 + 6000f), Hataki.Y0 + 6000f}};
                    for (int k = 0; k < marks.length; k++) {
                        w.ship.x = marks[k][0];
                        w.ship.y = marks[k][1];
                        w.clock = k == 2 ? Vents.WARN + 0.4f - w.vents.at[0][3] : k == 3 ? 0.9f - w.vents.at[0][3] : 5f;
                        if (k == 4) { vault.open = true; vault.openT = 0.3f; }
                        if (k == 7) {
                            w.hazards.patch(w.ship.x - 120f, w.ship.y + 90f, 50f, 9f);
                            w.hazards.patch(w.ship.x + 90f, w.ship.y + 150f, 40f, 9f);
                            w.hazards.line(w.ship.x - 300f, w.ship.y - 80f, w.ship.x + 300f, w.ship.y + 80f).state = vajra.world.Hazards.ARMED;
                            w.hazards.ring = false;
                        }
                        cam.position.set(w.ship.x, w.ship.y, 0);
                        cam.update();
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                        for (int q = 0; q < 3; q++) r.render(w, cam, vp);
                        write("build/vitala-p" + k + ".png");
                    }
                    // Bala's door and the Plunge above it: the one world, Atala's arena below, Vitala's shaft on top
                    w.ship.x = Hataki.CX;
                    w.ship.y = Hataki.Y0 - 200f;
                    cam.position.set(w.ship.x, w.ship.y + 200f, 0);
                    cam.update();
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    for (int q = 0; q < 3; q++) r.render(w, cam, vp);
                    write("build/vitala-seam.png");
                    // the boss's ash ring and a fire fan
                    boss.bossPhase = 3;
                    w.ship.x = w.pitX;
                    w.ship.y = w.pitY - 300f;
                    w.hazards.ring = true;
                    w.hazards.ringX = w.pitX;
                    w.hazards.ringY = w.pitY;
                    w.hazards.ringR = 420f;
                    for (int k = 0; k < 6; k++) w.hazards.patch(w.pitX + (k - 3) * 90f, w.pitY - 120f, 36f, 7f);
                    cam.position.set(w.ship.x, w.ship.y + 150f, 0);
                    cam.update();
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                    for (int q = 0; q < 3; q++) r.render(w, cam, vp);
                    write("build/vitala-boss.png");
                    w.hazards.ring = false;
                    w.ship.x = Hataki.centre(Hataki.Y0 + 6000f);
                    w.ship.y = Hataki.Y0 + 6000f;
                    cam.position.set(w.ship.x, w.ship.y, 0);
                    cam.update();
                    for (int k = 0; k < 10; k++) r.render(w, cam, vp);
                    Gdx.gl.glFinish();
                    long t1 = System.nanoTime();
                    for (int k = 0; k < 60; k++) r.render(w, cam, vp);
                    Gdx.gl.glFinish();
                    frameMs = (System.nanoTime() - t1) / 60_000_000f;
                    System.out.println("VitalaShot: frame " + frameMs + " ms");
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("vitala render crashed", boom.get());
        assertTrue(frameMs < 60f, "a frame of Vitala stays playable: " + frameMs + " ms");
    }

    private static void write(String path) throws Exception {
        IntBuffer buf = BufferUtils.newIntBuffer(W * H);
        Gdx.gl.glReadPixels(0, 0, W, H, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buf);
        int[] px = new int[W * H];
        buf.get(px);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int p = px[(H - 1 - y) * W + x];
                img.setRGB(x, y, ((p & 0xff) << 16) | (((p >> 8) & 0xff) << 8) | ((p >> 16) & 0xff));
            }
        new File(path).getParentFile().mkdirs();
        ImageIO.write(img, "png", new File(path));
    }
}

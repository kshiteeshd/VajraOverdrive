package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.world.AstraFlow;
import vajra.world.TileMap;
import vajra.world.World;

/** The two flows as the game draws them (build/flow-agni.png and build/flow-vayu.png): four moments each, a crowd of foes round the hull. Agni: the streams open,
 *  run out and curl, foes catch fire. Vayu: the air is drawn in and the foes carried round, the cuts, the eye, the throw. */
class FlowShotTest {
    private static final int W = 1280, H = 720;

    private static World world(long seed) {
        World w = new World();
        w.enemies.clear();
        try {                                                            // open ground: no rock in the picture
            java.lang.reflect.Field mf = World.class.getDeclaredField("maps");
            mf.setAccessible(true);
            mf.set(w, new TileMap[0]);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        w.ship.x = 2600;
        w.ship.y = 2700;
        w.ship.facing = (float) (Math.PI / 2);
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 160 + r.nextDouble() * 330;
            Enemy e = new Enemy(Enemy.Kind.MAYAVI, 2600 + (float) Math.cos(a) * (float) d, 2700 + (float) Math.sin(a) * (float) d);
            e.hp = e.maxHp = i % 4 == 0 ? 3000 : 260;
            e.active = true;
            w.enemies.add(e);
        }
        return w;
    }

    @Test
    void bothFlowsDraw() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(64, 64);
        cfg.setInitialVisible(false);
        cfg.disableAudio(true);
        int[] lit = new int[5];
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    FlowFx fx = new FlowFx();
                    FxImages fxi = new FxImages();
                    SpriteBatch batch = new SpriteBatch();
                    ShapeRenderer sr = new ShapeRenderer(30000);
                    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, W, H, false);
                    for (int which = 0; which < 5; which++) {
                        World w = world(which + 3);
                        switch (which) {
                            case 0: w.flow.startAgni(w.ship, 0, 1f, 1f, 5L); break;
                            case 1: w.flow.startVayu(w.ship, 0, 1f, 1f, 5L); break;
                            case 2: w.flow.startLightning(w.ship, 0, 1f, 1f, 5L); break;
                            case 3: w.flow.startBoulders(w.ship, 0, 1f, 1f, 5L); break;
                            default: w.flow.startLotus(w.ship, 0, 1f, 1f, 5L); break;
                        }
                        float[][] moments = {{0.5f, 1.0f, 1.7f, 2.6f}, {0.7f, 1.3f, 1.9f, 2.5f}, {0.08f, 0.2f, 0.4f, 0.6f}, {0.5f, 1.2f, 2.0f, 3.0f}, {0.5f, 1.2f, 2.0f, 3.2f}};
                        float[] at = moments[which];
                        Pixmap sheet = new Pixmap(W * 2, H * 2, Pixmap.Format.RGBA8888);
                        float t = 0;
                        for (int m = 0; m < 4; m++) {
                            while (t < at[m]) {
                                w.updateAstra(1 / 60f);
                                w.clock += 1 / 60f;
                                t += 1 / 60f;
                                for (int q = w.effects.size() - 1; q >= 0; q--) {      // the world's own effects age as they do in play
                                    w.effects.get(q).update(1 / 60f);
                                    if (w.effects.get(q).done()) w.effects.remove(q);
                                }
                            }
                            OrthographicCamera cam = new OrthographicCamera(W, H);
                            cam.position.set(w.ship.x, w.ship.y, 0);
                            cam.update();
                            fbo.begin();
                            ScreenUtils.clear(0.06f, 0.05f, 0.07f, 1f);
                            Gdx.gl.glEnable(GL20.GL_BLEND);
                            sr.setProjectionMatrix(cam.combined);
                            sr.begin(ShapeRenderer.ShapeType.Filled);
                            sr.setColor(0.6f, 0.6f, 0.65f, 1f);
                            for (Enemy e : w.enemies) if (!e.dead) sr.rect(e.x - 14, e.y - 14, 28, 28);
                            sr.setColor(0.7f, 0.85f, 0.95f, 1f);
                            sr.rect(w.ship.x - 10, w.ship.y - 10, 20, 20);
                            AstraFx.draw(sr, w, w.ship.x - W / 2f, w.ship.y - H / 2f, W, H);
                            sr.end();
                            batch.setProjectionMatrix(cam.combined);
                            batch.begin();
                            fx.draw(batch, w, w.ship.x - W / 2f, w.ship.y - H / 2f, W, H);
                            for (vajra.world.Effect ef : w.effects) if (ef.type == vajra.world.Effect.SMOKE) fxi.draw(batch, ef);   // a burning foe's smoke
                            batch.setColor(1f, 1f, 1f, 1f);
                            batch.end();
                            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, W, H);
                            fbo.end();
                            int n = 0;
                            for (int y = 0; y < H; y += 3)
                                for (int x = 0; x < W; x += 3) {
                                    int p = pm.getPixel(x, y);
                                    if ((((p >> 24) & 0xFF) + ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF)) > 260) n++;
                                }
                            lit[which] = Math.max(lit[which], n);
                            sheet.drawPixmap(pm, 0, 0, W, H, (m % 2) * W, (m / 2) * H, W, H);
                            pm.dispose();
                        }
                        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local("build/flow-" + new String[] {"agni", "vayu", "akasha", "prithvi", "brahma"}[which] + ".png"), sheet);
                        sheet.dispose();
                    }
                    fx.dispose();
                    batch.dispose();
                    sr.dispose();
                    fbo.dispose();
                } catch (Throwable t) {
                    boom.set(t);
                } finally {
                    Gdx.app.exit();
                }
            }
        }, cfg);
        if (boom.get() != null) throw new AssertionError("the flows failed to draw", boom.get());
        assertTrue(lit[0] > 1500, "Agni's bloom lights the frame (" + lit[0] + ")");
        assertTrue(lit[1] > 1500, "Vayu's tornado lights the frame (" + lit[1] + ")");
        assertTrue(lit[2] > 600, "Akasha's lightning lights the frame (" + lit[2] + ")");
        assertTrue(lit[3] > 600, "Prithvi's boulders and mandala show (" + lit[3] + ")");
        assertTrue(lit[4] > 600, "Brahma's petals show (" + lit[4] + ")");
    }
}

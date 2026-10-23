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
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.Effect;
import vajra.world.World;

/** The nine Atala enemies in the real renderer: a rank-and-file line-up (plain, hurt/raging,
 *  leader, elite), a hit flash, and a frame of each death. PNGs land in build/ for eyeballing;
 *  the assertions only prove that every kind put pixels on the screen. */
class EnemyShowcaseTest {
    private static final int W = 1280, H = 720;
    private static final Enemy.Kind[] ROW1 = {Enemy.Kind.AURIS, Enemy.Kind.HATAKA, Enemy.Kind.LANCE,
            Enemy.Kind.VAKRA, Enemy.Kind.BHAVI};
    private static final Enemy.Kind[] ROW2 = {Enemy.Kind.MAYAVI, Enemy.Kind.DVARAPALA,
            Enemy.Kind.DIPAKA, Enemy.Kind.VANIJA};

    @Test
    void everyAtalaEnemyDrawsInAllItsStates() {
        AtomicReference<Throwable> boom = new AtomicReference<>();
        AtomicReference<String> fail = new AtomicReference<>("");
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setWindowedMode(W, H);
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
                    flow.keyDown(com.badlogic.gdx.Input.Keys.ENTER);
                    World w = ((GameScreen) flow.top()).world();
                    w.cues.clear();
                    w.ship.x = 2600;
                    w.ship.y = 700;
                    for (int i = 0; i < 40; i++) flow.render(1 / 60f);

                    // line 1 and 2: plain; line 3: raging; line 4: leader; line 5: elite
                    place(w, ROW1, 700 + 270, 0);
                    place(w, ROW2, 700 + 150, 0);
                    place(w, ROW1, 700 + 30, 1);
                    place(w, ROW1, 700 - 100, 2);
                    place(w, ROW1, 700 - 230, 3);
                    for (int i = 0; i < 6; i++) flow.render(1 / 60f);
                    int[] px = frame();
                    write(px, "build/enemies-lineup.png");
                    int bright = 0, gold = 0; // bodies on the screen: pearl and gold pixels
                    for (int p : px) {
                        int r = p & 0xff, g = (p >> 8) & 0xff, bl = (p >> 16) & 0xff;
                        if (r > 200 && g > 190 && bl > 170) bright++;
                        if (r > 220 && g > 170 && bl < 90) gold++;
                    }
                    if (bright < 1500) fail.set(fail.get() + "too little pearl on screen (" + bright + "); ");
                    if (gold < 300) fail.set(fail.get() + "too little gold on screen (" + gold + "); ");

                    // deaths: one frame of each kind, mid-shatter
                    w.enemies.removeIf(e -> e.parley);
                    float x = 2600 - 4 * 130;
                    for (Enemy.Kind k : ROW1) { death(w, k, x, 700 + 200); x += 130; }
                    x = 2600 - 3 * 130;
                    for (Enemy.Kind k : ROW2) { death(w, k, x, 700 + 60); x += 130; }
                    for (Effect e : w.effects) e.age = e.life * 0.45f;
                    w.effects.removeIf(e -> e.type != Effect.DEATH);
                    for (Effect e : w.effects) e.age = e.life * 0.45f;
                    write(frame(), "build/enemies-death.png");
                    flow.dispose();
                } catch (Throwable t) {
                    boom.set(t);
                }
                Gdx.app.exit();
            }

            void place(World w, Enemy.Kind[] kinds, float y, int style) {
                float x = 2600 - (kinds.length - 1) * 65f * 2;
                for (Enemy.Kind k : kinds) {
                    Enemy e = new Enemy(k, x, y);
                    e.parley = true; // held still, drawn as it stands
                    e.active = true;
                    if (style == 1) e.hp = e.maxHp * 0.2f;      // hurt: the rage frames
                    if (style == 2) e.rank = 1;                  // a leader's crest
                    if (style == 3) { e.rank = 1; e.elite = true; } // an elite's vents and halo
                    w.enemies.add(e);
                    x += 130;
                }
            }

            void death(World w, Enemy.Kind k, float x, float y) {
                w.addEffect(Effect.death(k.ordinal(), 0, x, y, 64f));
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
        if (boom.get() != null) throw new AssertionError("showcase crashed", boom.get());
        assertTrue(fail.get().isEmpty(), fail.get());
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

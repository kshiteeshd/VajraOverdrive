package vajra.desktop;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.flow.Flow;
import vajra.screen.GameScreen;
import vajra.screen.HomeScreen;
import vajra.screen.Ui;
import vajra.world.Chatter;
import vajra.world.World;

/** The people of Atala in the real renderer: the three of them side by side in a state each, Maya's
 *  first meeting as a scene, and then the flight afterwards - the job tracker, a toast, a bubble over
 *  a speaking person. PNGs land in build/ for eyeballing. */
class PeopleShotTest {
    private static final int W = 1280, H = 720;

    @Test
    void thePeopleAreVisibleSpeakAndKeepTheirJobsOnTheHud() {
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
                    flow.keyDown(Input.Keys.ENTER);
                    World w = ((GameScreen) flow.top()).world();
                    w.cues.clear();
                    w.enemies.clear();
                    Enemy scout = Chatter.person(w, "SCOUT"), maya = Chatter.person(w, "MAYA"), kal = Chatter.person(w, "KALIYA");
                    // all three in a row, hailing (unmet), in view
                    float y = 2700;
                    scout.x = scout.homeX = 2300; scout.y = scout.homeY = y;
                    maya.x = maya.homeX = 2600; maya.y = maya.homeY = y;
                    kal.x = kal.homeX = 2900; kal.y = kal.homeY = y;
                    w.ship.x = 2600;
                    w.ship.y = y - 300; // outside their sight (260 px): they only hail
                    w.update(1 / 60f, vajra.ship.ShipControls.NONE); // crossing into Atala raises the gate scene: settle it
                    w.cues.clear();
                    for (int i = 0; i < 30; i++) flow.render(1 / 60f);
                    // keep the ship out of their sight radius for the line-up
                    write(frame(), "build/people-hail.png");

                    // Maya's first meeting, as a scene
                    w.ship.x = maya.x + 60;
                    w.ship.y = maya.y - 60;
                    scout.x = scout.homeX = 800; kal.x = kal.homeX = 4400; // out of the frame
                    for (int i = 0; i < 40; i++) flow.render(1 / 60f);
                    for (int i = 0; i < 160; i++) flow.render(1 / 60f);
                    write(frame(), "build/people-maya-scene.png");
                    for (int k = 0; k < 10; k++) { flow.keyDown(Input.Keys.ENTER); flow.keyDown(Input.Keys.ENTER); for (int i = 0; i < 50; i++) flow.render(1 / 60f); }
                    for (int i = 0; i < 60; i++) flow.render(1 / 60f);

                    // afterwards: the job tracker, a toast and a bubble over the speaking person
                    w.missions.salvageTaken = 2;
                    w.toast("SALVAGE", "SIX WRECKS OF MAYA VANA, HOME TO HER HAMMER.");
                    w.say(maya, "THE HAMMER IS WARM.", "TAKE THE PURSE.");
                    for (int i = 0; i < 70; i++) flow.render(1 / 60f);
                    int[] px = frame();
                    write(px, "build/people-flight.png");
                    int blue = 0;
                    for (int p : px) {
                        int r = p & 0xff, g = (p >> 8) & 0xff, b = (p >> 16) & 0xff;
                        if (b > 200 && g > 160 && r < 190) blue++;
                    }
                    if (blue < 150) fail.set(fail.get() + "no sky-blue on screen (" + blue + "); ");
                    // a crowd: all three close together, everyone talking, a toast and a job up
                    scout.voiceMet = maya.voiceMet = kal.voiceMet = true;
                    w.speech.clear();
                    w.toasts.clear();
                    scout.x = scout.homeX = w.ship.x - 90; scout.y = scout.homeY = w.ship.y + 110;
                    maya.x = maya.homeX = w.ship.x + 10; maya.y = maya.homeY = w.ship.y + 120;
                    kal.x = kal.homeX = w.ship.x + 110; kal.y = kal.homeY = w.ship.y + 105;
                    w.say(scout, "THE BEACON BURNS.", "EVERY CAMP IS ON YOUR COMPASS.");
                    w.say(maya, "THE HAMMER IS WARM.", "TAKE THE PURSE AND THE SHARD.");
                    w.say(maya, "A SECOND LINE WAITS ITS TURN.");
                    w.say(kal, "GARUDA'S SHADOW DROVE MY KIN UP THE ROAD.");
                    w.toast("THE BEACON BURNS", "THE WATCH SEES EVERY CAMP. +100c AND A LIFE.");
                    for (int i = 0; i < 90; i++) flow.render(1 / 60f);
                    write(frame(), "build/people-crowd.png");
                    flow.dispose();
                } catch (Throwable t) {
                    boom.set(t);
                }
                Gdx.app.exit();
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
        if (boom.get() != null) throw new AssertionError("people shots crashed", boom.get());
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

package vajra.screen;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import vajra.scene.CutscenePlayer;
import vajra.scene.Shot;
import vajra.world.World;

/** Stages a cutscene inside GameScreen: while one plays the sim does not step, the HUD hides and
 *  the camera belongs to the scene. Two black bands slide onto the top and bottom of the screen
 *  and stay for the whole scene. Words are drawn the way the speaker is:
 *  <ul>
 *  <li>the NARRATOR speaks in the bottom band - plain words, no box;</li>
 *  <li>everyone else gets a temple-panel bubble beside them: chamfered corners, a double hairline,
 *      rangoli diamonds, a name tab, and a lotus bud or a trail of diamonds (a thought, for the
 *      pilot) toward the speaker. It is sized to its line, so a longer line is a bigger box.</li>
 *  </ul>
 *  All timing lives in CutscenePlayer; this class only points the camera and paints. */
public final class Director {
    /** Height of each letterbox band at full slide, UI units. */
    public static final float BAND = 88;
    private static final float EDGE = 14;

    private final Ui ui;
    private final GlyphLayout layout = new GlyphLayout();
    private CutscenePlayer player;
    private List<Shot> shots;
    private Runnable done;
    private float camX, camY, camZoom = 1;
    private World world; // the world the scene plays in: who stands where, so a bubble keeps off them

    public Director(Ui ui) { this.ui = ui; }

    public boolean active() { return player != null; }

    /** The scene being played, for the ears (the sung lines); null when none. */
    public CutscenePlayer player() { return player; }

    /** `onDone` runs when the scene is over, however it ended. */
    public void start(List<Shot> shots, OrthographicCamera cam, Runnable onDone) {
        if (shots.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        this.shots = shots;
        this.done = onDone;
        player = new CutscenePlayer(shots, cam.position.x, cam.position.y, cam.zoom);
    }

    public void press() { if (player != null) player.press(); }

    public void skip() {
        if (player == null) return;
        for (Shot s : shots) s.walkTo(1); // whoever was walking has arrived
        player.skip();
    }

    /** Run the scene's clock and put the camera where it says. */
    public void steer(float dt, OrthographicCamera cam, World world) {
        if (player == null) return;
        this.world = world;
        player.advance(dt, cam.position.x, cam.position.y, cam.zoom);
        Shot s = player.shot();
        if (s != null) s.walkTo(Math.min(1, player.shotT() / s.walkSecs));
        cam.zoom = player.camZoom;
        cam.position.set(player.camX, Math.max(0, Math.min(world.height, player.camY)), 0);
        camX = cam.position.x;
        camY = cam.position.y;
        camZoom = cam.zoom;
        if (player.done()) {
            player = null;
            if (done != null) done.run();
        }
    }

    // --- painting -------------------------------------------------------------------------

    private float ux(float wx) { return Ui.UI_W / 2f + (wx - camX) / camZoom; }
    private float uy(float wy) { return Ui.UI_H / 2f + (wy - camY) / camZoom; }

    public void render(SpriteBatch batch) {
        if (player == null) return;
        float k = player.bands();
        float band = BAND * ease(k);
        Shot s = player.shot();
        String typed = player.text();
        boolean speak = s != null && !typed.isEmpty();

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        bands(band);
        if (speak && !s.narrator()) bubbleShapes(s, player.fullText(), band);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        if (speak && s.narrator()) narrate(batch, typed, band);
        else if (speak) bubbleText(batch, s, typed, player.fullText());
        if (player.lineComplete() && k >= 1f) {
            ui.caption.setColor(1, 1, 1, 0.45f + 0.35f * (float) Math.sin(player.shotT() * 5));
            ui.right(batch, ui.caption, "ENTER", Ui.UI_W - 60, band - 30);
            ui.caption.setColor(1, 1, 1, 1);
        }
        batch.end();
    }

    private static float ease(float t) { return t * t * (3 - 2 * t); }

    /** Two black bands, each with a gold hairline and a small diamond on its inner edge. */
    private void bands(float band) {
        if (band <= 0) return;
        ui.shapes.setColor(0, 0, 0, 1);
        ui.shapes.rect(0, 0, Ui.UI_W, band);
        ui.shapes.rect(0, Ui.UI_H - band, Ui.UI_W, band);
        float cx = Ui.UI_W / 2f;
        for (int i = 0; i < 2; i++) {
            float y = i == 0 ? band : Ui.UI_H - band;
            Ornament.gold(ui, 0.55f);
            ui.shapes.rectLine(60, y, cx - 40, y, 1.2f);
            ui.shapes.rectLine(cx + 40, y, Ui.UI_W - 60, y, 1.2f);
            Ornament.diamond(ui, cx, y, 5);
            Ornament.diamond(ui, cx - 20, y, 2.2f);
            Ornament.diamond(ui, cx + 20, y, 2.2f);
        }
    }

    /** The narrator, in the bottom band: centred, plain, slow. */
    private void narrate(SpriteBatch batch, String typed, float band) {
        float w = 900;
        layout.setText(ui.menu, typed, ui.menu.getColor(), w, Align.center, true);
        float y = band / 2f + layout.height / 2f + 2; // vertically centred in the band
        ui.menu.setColor(1, 0.97f, 0.88f, 1f);
        layout.setText(ui.menu, typed, ui.menu.getColor(), w, Align.center, true);
        ui.menu.draw(batch, layout, Ui.UI_W / 2f - w / 2, y);
        ui.menu.setColor(1, 1, 1, 1);
    }

    private float[] color(Shot s) { return new float[] {s.r, s.g, s.b}; }

    /** Every body a bubble must not cover: the pilot, the people, the ruler, and anyone held for the
     *  scene - as rectangles on the screen. */
    private float[][] actors() {
        if (world == null) return new float[0][];
        java.util.List<float[]> out = new java.util.ArrayList<>();
        add(out, world.ship.x, world.ship.y, 40);
        for (vajra.enemy.Enemy v : world.voices) add(out, v.x, v.y, 56);
        if (world.boss != null) add(out, world.boss.x, world.boss.y, 110);
        for (vajra.enemy.Enemy e : world.enemies) if (e.parley) add(out, e.x, e.y, e.drawSize());
        return out.toArray(new float[0][]);
    }

    private void add(java.util.List<float[]> out, float wx, float wy, float size) {
        float x = ux(wx), y = uy(wy), h = size / camZoom / 2f + 6;
        out.add(new float[] {x - h, y - h, x + h, y + h + 12});
    }

    private float[] box(Shot s, String full, float band) {
        float ax = ux(s.speakerX()), ay = uy(s.speakerY());
        float gap = 28f / camZoom + 14;
        return Bubbles.box(ui, layout, full, ax, ay, gap, s.below, band + EDGE + 8, Ui.UI_H - band - EDGE - 8, null, actors());
    }

    private void bubbleShapes(Shot s, String full, float band) {
        Bubbles.shapes(ui, box(s, full, band), s.player, s.speaker, color(s), 1f,
                player.lineComplete() ? player.shotT() : 0f);
    }

    private void bubbleText(SpriteBatch batch, Shot s, String typed, String full) {
        Bubbles.text(ui, batch, layout, box(s, full, BAND * ease(player.bands())), s.speaker, typed, color(s), 1f);
    }
}

package vajra.screen;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;
import vajra.enemy.Enemy;
import vajra.scene.Palette;
import vajra.world.Speech;
import vajra.world.World;

/** People talking while the world moves: a temple bubble over each speaking person's hull, in sky
 *  blue, the same bubble the cutscenes use. It is drawn in UI space, projected from the world camera,
 *  after the HUD and never during a cutscene.
 *
 *  <p>Nothing here may sit on anything else. The boxes are laid out ONCE a frame, in order, and each
 *  keeps off every body on the screen (the pilot, the people), every HUD panel (HudZones) and every box
 *  already placed; a second line for someone already talking waits its turn (World.heads). */
final class SpeechBubbles {
    private final Ui ui;
    private final GlyphLayout layout = new GlyphLayout();

    SpeechBubbles(Ui ui) { this.ui = ui; }

    private float ux(OrthographicCamera cam, float wx) { return Ui.UI_W / 2f + (wx - cam.position.x) / cam.zoom; }
    private float uy(OrthographicCamera cam, float wy) { return Ui.UI_H / 2f + (wy - cam.position.y) / cam.zoom; }

    private static String nameOf(Speech s) {
        String n = s.who.voiceName == null ? "SCOUT" : s.who.voiceName;
        return n.equals("SCOUT") ? "THE SCOUT" : n.equals("MAYA") ? "THE NINTH MAYA" : "KALIYA";
    }

    /** One laid-out bubble. */
    private static final class Placed {
        Speech s;
        float[] q;
    }

    /** A body's rectangle on the screen: where it is, a little larger than it is drawn. */
    private float[] body(OrthographicCamera cam, float wx, float wy, float size) {
        float x = ux(cam, wx), y = uy(cam, wy), h = size / cam.zoom / 2f + 8;
        return new float[] {x - h, y - h, x + h, y + h + 14};
    }

    private List<Placed> layoutAll(World w, OrthographicCamera cam) {
        List<float[]> hard = new ArrayList<>(HudZones.of(w)); // HUD panels and other boxes: never covered
        List<float[]> soft = new ArrayList<>();               // bodies: covered only if every nearby spot is worse
        soft.add(body(cam, w.ship.x, w.ship.y, 40));
        for (Enemy v : w.voices) soft.add(body(cam, v.x, v.y, 56));
        List<Placed> out = new ArrayList<>();
        for (Speech s : w.heads()) {
            float ax = ux(cam, s.who.x), ay = uy(cam, s.who.y);
            if (ax < -200 || ax > Ui.UI_W + 200 || ay < -200 || ay > Ui.UI_H + 200) continue;
            float gap = 30f / cam.zoom + 8;
            Placed p = new Placed();
            p.s = s;
            p.q = Bubbles.box(ui, layout, s.full(), ax, ay, gap, false, 20, Ui.UI_H - 20,
                    hard.toArray(new float[0][]), soft.toArray(new float[0][]));
            hard.add(new float[] {p.q[0], p.q[1], p.q[0] + p.q[2], p.q[1] + p.q[3] + 16}); // and the next one keeps off this
            out.add(p);
        }
        return out;
    }

    void render(SpriteBatch batch, World w, OrthographicCamera cam) {
        if (w.speech.isEmpty()) return;
        List<Placed> boxes = layoutAll(w, cam);
        if (boxes.isEmpty()) return;
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        for (Placed p : boxes) Bubbles.shapes(ui, p.q, false, nameOf(p.s), Palette.PERSON, p.s.alpha(), 0f);
        ui.shapes.end();
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        for (Placed p : boxes) Bubbles.text(ui, batch, layout, p.q, nameOf(p.s), p.s.typed(), Palette.PERSON, p.s.alpha());
        batch.end();
    }
}

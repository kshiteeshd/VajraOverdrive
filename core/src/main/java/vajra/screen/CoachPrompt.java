package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import vajra.coach.Lesson;
import vajra.coach.Lessons;

/** The one prompt the first flight puts up: the lesson's name, its key caps, and what to do.
 *  Quiet and centred in the upper field - the ship waits low on the screen, so nothing covers it. */
final class CoachPrompt {
    private static final float Y = 470;

    private CoachPrompt() {}

    private static String ask(int lesson) {
        return lesson == Lessons.THRUST ? "FLY THE HULL TO MOVE ON" : "HOLD TO FIRE";
    }

    static void render(Ui ui, SpriteBatch batch, int lesson, float clock) {
        Lesson l = Lessons.ALL[lesson];
        float a = 0.75f + 0.25f * (float) Math.sin(clock * 3.2f); // a slow breath
        float x = Ui.UI_W / 2f - KeyCaps.width(ui, l.keys) / 2f;

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        KeyCaps.shapes(ui, x, Y, l.keys, a);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        KeyCaps.labels(ui, batch, x, Y, l.keys, a);
        Template.title(ui, batch, l.title, Y + 82, 1f);
        Template.line(ui, batch, ask(lesson), Y - 24, 0.6f);
        batch.end();
    }
}

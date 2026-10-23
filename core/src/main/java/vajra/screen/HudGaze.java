package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.LokaTable;
import vajra.world.World;

/** The ruler's gaze, top-centre under the score: how closely the loka's ruler is watching.
 *  A thin bar with a notch where he notices and a notch where he looks up; it fills as the
 *  pilot cuts down his people and burns hotter as it goes. No numbers - the colour and the
 *  word under it are the whole read. Shown from the first Atala band, not in Bhu Loka. */
final class HudGaze {
    static final float W = 200, H = 4, Y = Ui.UI_H - 88; // clear of the score and the combo line
    private static final String[] WORD = {"UNAWARE", "NOTICED", "WATCHING", "WAITING"};

    private float shown; // eased toward the true gaze so a kill reads as a push, not a jump

    boolean visible(World w) { return w.currentRegionIndex() >= 1; }

    void shapes(Ui ui, World w, float dt) {
        if (!visible(w)) return;
        shown += (w.gaze() - shown) * Math.min(1f, dt * 6f);
        float x = Ui.UI_W / 2f - W / 2f;
        ui.shapes.setColor(1, 1, 1, 0.12f);
        ui.shapes.rect(x, Y, W, H);
        float[] c = tint(w.gazeStage);
        float pulse = w.gazeStage >= 2 ? 0.75f + 0.25f * (float) Math.sin(w.clock * 4f) : 1f;
        ui.shapes.setColor(c[0], c[1], c[2], pulse);
        ui.shapes.rect(x, Y, W * Math.min(1f, shown), H);
        ui.shapes.setColor(1, 1, 1, 0.55f); // the two notches: noticed, watching
        ui.shapes.rect(x + W * World.GAZE_NOTICED - 1, Y - 2, 2, H + 4);
        ui.shapes.rect(x + W * World.GAZE_WATCHING - 1, Y - 2, 2, H + 4);
    }

    void label(Ui ui, SpriteBatch batch, World w) {
        if (!visible(w)) return;
        float[] c = tint(w.gazeStage);
        String ruler = LokaTable.at(w.lokaIndex).boss.toUpperCase();
        ui.caption.setColor(1, 1, 1, 0.5f);
        ui.centered(batch, ui.caption, ruler + "'S GAZE  -  " + WORD[Math.min(3, w.gazeStage)], Y + 16);
        ui.caption.setColor(1, 1, 1, 1);
    }

    /** Cool cream while he sleeps, gold as he watches, ember once he waits. */
    private static float[] tint(int stage) {
        switch (stage) {
            case 0: return new float[] {0.85f, 0.85f, 0.75f};
            case 1: return new float[] {1f, 0.84f, 0.3f};
            case 2: return new float[] {1f, 0.55f, 0.15f};
            default: return new float[] {1f, 0.3f, 0.2f};
        }
    }
}

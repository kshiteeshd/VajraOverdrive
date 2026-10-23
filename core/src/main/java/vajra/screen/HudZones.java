package vajra.screen;

import java.util.ArrayList;
import java.util.List;
import vajra.world.World;

/** Where the HUD already is, as rectangles {x0, y0, x1, y1} in UI units, so a speech bubble can keep
 *  off it: the bars, the score and the gaze, the job tracker, a toast, the compass, the right-hand
 *  tape and the weapon plate. */
final class HudZones {
    private HudZones() {}

    static List<float[]> of(World w) {
        List<float[]> z = new ArrayList<>();
        z.add(new float[] {0, Ui.UI_H - 120, 270, Ui.UI_H});                 // bars, lives, vitals
        z.add(new float[] {480, Ui.UI_H - 110, 800, Ui.UI_H});               // score, combo, gaze
        z.add(new float[] {0, 0, 150, 150});                                 // compass
        z.add(new float[] {Ui.UI_W - 170, 0, Ui.UI_W, Ui.UI_H - 90});        // depth tape, readout, weapon plate
        float[] jobs = HudJobs.rect(w);
        if (jobs != null) z.add(jobs);
        if (!w.toasts.isEmpty())
            z.add(new float[] {(Ui.UI_W - HudToast.W) / 2f, HudToast.bottom(w), (Ui.UI_W + HudToast.W) / 2f, HudToast.TOP + 4});
        return z;
    }
}

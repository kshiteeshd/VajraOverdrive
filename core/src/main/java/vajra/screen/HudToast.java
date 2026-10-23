package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.world.Toast;
import vajra.world.World;

/** A job's word to the pilot: a gold temple panel under the gaze bar with a title and one line. It
 *  slides in, holds a few seconds, and slides out. At most two at once, stacked. */
final class HudToast {
    static final float W = 560, H = 56, TOP = Ui.UI_H - 112;

    /** The lowest edge of the toasts now up (UI y), or the top of the screen when there are none: a
     *  speech bubble keeps below it so the two never sit on each other. */
    static float bottom(World w) {
        int n = Math.min(2, w.toasts.size());
        return n == 0 ? Ui.UI_H : TOP - H - (n - 1) * (H + 10);
    }

    void shapes(Ui ui, World w) {
        int n = Math.min(2, w.toasts.size());
        for (int i = 0; i < n; i++) {
            Toast t = w.toasts.get(i);
            float k = t.k();
            float y = TOP - i * (H + 10) - (1 - k) * 18;
            Ornament.panel(ui, (Ui.UI_W - W) / 2f, y - H, W, H, 6, UiTheme.GOLD_R, UiTheme.GOLD_G, UiTheme.GOLD_B,
                    0.75f * k, false);
        }
    }

    void text(Ui ui, SpriteBatch b, World w) {
        int n = Math.min(2, w.toasts.size());
        for (int i = 0; i < n; i++) {
            Toast t = w.toasts.get(i);
            float k = t.k();
            float y = TOP - i * (H + 10) - (1 - k) * 18;
            ui.caption.setColor(UiTheme.GOLD_R, UiTheme.GOLD_G, UiTheme.GOLD_B, k);
            ui.centered(b, ui.caption, t.title, y - 18);
            ui.hud.setColor(1, 1, 1, k);
            ui.centered(b, ui.hud, t.line, y - 38);
        }
        ui.caption.setColor(1, 1, 1, 1);
        ui.hud.setColor(1, 1, 1, 1);
    }
}

package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Key caps in the game's one line-work: an ink tile, a hairline in the accent, the key in the
 *  body face. A "/" between caps reads "or", a "-" reads "to". Two passes like everything else:
 *  {@link #shapes} in the shape pass, {@link #labels} in the batch pass, same x and y. The y is
 *  the bottom of the caps. */
final class KeyCaps {
    static final float H = 30, GAP = 10, PAD = 16, MIN = 34;

    private KeyCaps() {}

    private static boolean joint(String k) { return k.equals("/") || k.equals("-"); }

    private static String jointWord(String k) { return k.equals("/") ? "or" : "to"; }

    private static float capW(Ui ui, String k) {
        return joint(k) ? ui.measure(ui.caption, jointWord(k)) : Math.max(MIN, ui.measure(ui.hud, k) + 2 * PAD);
    }

    static float width(Ui ui, String[] keys) {
        float w = 0;
        for (int i = 0; i < keys.length; i++) w += capW(ui, keys[i]) + (i + 1 < keys.length ? GAP : 0);
        return w;
    }

    static void shapes(Ui ui, float x, float y, String[] keys, float alpha) {
        for (String k : keys) {
            float w = capW(ui, k);
            if (!joint(k)) {
                ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.12f * alpha);
                Ornament.fillChamfer(ui, x, y, w, H, 4);
                ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.9f * alpha);
                Ornament.lineChamfer(ui, x, y, w, H, 4, 1.1f);
            }
            x += w + GAP;
        }
    }

    static void labels(Ui ui, SpriteBatch batch, float x, float y, String[] keys, float alpha) {
        for (String k : keys) {
            float w = capW(ui, k);
            if (joint(k)) {
                ui.caption.setColor(1, 1, 1, 0.5f * alpha);
                ui.caption.draw(batch, jointWord(k), x, y + H / 2 + 4);
                ui.caption.setColor(1, 1, 1, 1);
            } else {
                ui.hud.setColor(1, 1, 1, alpha);
                ui.hud.draw(batch, k, x + (w - ui.measure(ui.hud, k)) / 2, y + H / 2 + 6);
                ui.hud.setColor(1, 1, 1, 1);
            }
            x += w + GAP;
        }
    }
}

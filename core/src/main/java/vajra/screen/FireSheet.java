package vajra.screen;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** THE FIRE SHEET (tools/enemy_art/fire.py): 32 x 32 cells, 8 frames a row, one row per kind - hearth, brazier, ground, exhaust, jet. Every fire in the
 *  game that is not a ship's own is cut from it. Seen from above: the hearth, brazier and ground are round, the jet points right (+x) from its nozzle.
 *  A missing sheet draws nothing, never crashes. */
public class FireSheet {
    public static final int HEARTH = 0, BRAZIER = 1, GROUND = 2, EXHAUST = 3, JET = 4 /* the mouth */, JET_BODY = 5, JET_TIP = 6, CELL = 32, FRAMES = 8;
    private final Texture sheet = ImageKit.texture("effects/fire_sheet.png", true);

    public boolean ready() {
        return sheet != null;
    }

    /** Frame `n` (any integer, wrapped) of a kind. */
    private TextureRegion cell(int kind, int n) {
        return new TextureRegion(sheet, Math.floorMod(n, FRAMES) * CELL, kind * CELL, CELL, CELL);
    }

    /** A round fire centred on (x, y), `size` px across. */
    public void puff(SpriteBatch batch, int kind, int frame, float x, float y, float size) {
        if (sheet == null) return;
        batch.draw(cell(kind, frame), x - size / 2f, y - size / 2f, size, size);
    }

    /** A jet from its nozzle at (x, y) along `deg`, `len` long and `width` wide: the mouth, then the body tiled end to end (the tiles join exactly),
     *  then the tip. The cells are stretched a little so a whole number of bodies fills the length; a short jet is just mouth and tip. The frame runs
     *  the stream outward. Call with the colour and blend already set. */
    public void jet(SpriteBatch batch, int frame, float x, float y, float deg, float len, float width, float alpha) {
        if (sheet == null || len < 1f) return;
        float h = width * 1.25f, cs = h;                                           // (the flame fills about four fifths of its cell)
        int n = Math.max(0, Math.round((len - 2f * cs) / cs));
        int pieces = n + 2;
        float step = len / pieces, r = batch.getColor().a;
        double a = Math.toRadians(deg);
        float cx = (float) Math.cos(a) * step, cy = (float) Math.sin(a) * step;
        batch.getColor().a = r * alpha;
        batch.setColor(batch.getColor());
        for (int i = 0; i < pieces; i++) {
            int kind = i == 0 ? JET : i == pieces - 1 ? JET_TIP : JET_BODY;
            batch.draw(cell(kind, frame), x, y - h / 2f, 0f, h / 2f, step, h, 1f, 1f, deg);
            x += cx;
            y += cy;
        }
        batch.getColor().a = r;
        batch.setColor(batch.getColor());
    }

    public void dispose() {
        if (sheet != null) sheet.dispose();
    }
}

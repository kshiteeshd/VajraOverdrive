package vajra.screen;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/** The one speech bubble, drawn the same way for a cutscene and for a person talking mid-flight: a
 *  temple panel (chamfered, double hairline, rangoli diamonds) with a name tab, a lotus-and-arch
 *  finial, and a lotus bud (speech) or stepping diamonds (a thought) pointing at the speaker.
 *  The box is sized from the WHOLE line so it does not jitter while the line types.
 *
 *  <p>Colour is the speaker's (vajra.scene.Palette): Indra's thoughts are Akasha lavender, the people
 *  of the loka sky blue, the soldiers ivory, the ruler gold. Two passes, like every screen: {@link #shapes} then
 *  {@link #text}. All positions are UI units. */
public final class Bubbles {
    public static final float MAX_W = 360, PAD = 16, NAME_H = 20, CUT = 7;

    private Bubbles() {}

    /** Where a bubble of this size goes - NEAR its speaker, always. The candidates are the spots round
     *  the speaker: over, under, to either side, each at a few distances and slid left and right. A spot
     *  is REJECTED if it covers a HARD rectangle (a HUD panel, another bubble) or leaves the allowed
     *  band; every other spot is scored by how far its box is from the speaker, plus a penalty for the
     *  area of any SOFT rectangle (a body) it covers, plus a small one for not being on the preferred
     *  side. The lowest score wins, so a bubble covers a hull only when every nearby spot would be worse,
     *  and it never wanders off to find empty sky. Rectangles are {x0, y0, x1, y1}. Returns
     *  {x, y, 1 if under the speaker}. Pure, so a test can pin it. */
    public static float[] place(float bw, float bh, float ax, float ay, float gap, boolean below,
                                float minY, float maxY, float[][] hard, float[][] soft) {
        float best = Float.MAX_VALUE;
        float[] pick = null;
        float[] slide = {0, -0.35f, 0.35f, -0.7f, 0.7f};
        for (int r = 0; r < 3; r++) {
            float g = gap + r * 16;
            for (int kind = 0; kind < 4; kind++) { // over, under, left, right
                for (float sl : slide) {
                    float bx, by;
                    boolean under = kind == 1;
                    if (kind == 0) { bx = ax - bw / 2 + sl * bw; by = ay + g; }
                    else if (kind == 1) { bx = ax - bw / 2 + sl * bw; by = ay - g - bh; }
                    else if (kind == 2) { bx = ax - g - bw; by = ay - bh / 2 + sl * bh; }
                    else { bx = ax + g; by = ay - bh / 2 + sl * bh; }
                    bx = Math.max(20, Math.min(Ui.UI_W - 20 - bw, bx));
                    if (by < minY || by + bh > maxY) continue;
                    if (!clear(bx, by, bw, bh, hard)) continue;
                    float cx = bx + bw / 2, cy = by + bh / 2;
                    float dist = (float) Math.sqrt((cx - ax) * (cx - ax) + (cy - ay) * (cy - ay));
                    float score = dist + overlapArea(bx, by, bw, bh, soft) * 0.05f
                            + (under == below && kind < 2 ? 0 : 24) + r * 6;
                    if (score < best) {
                        best = score;
                        pick = new float[] {bx, by, under ? 1f : (kind >= 2 ? (by + bh / 2 < ay ? 1f : 0f) : 0f)};
                    }
                }
            }
        }
        if (pick != null) return pick;
        float by = below ? ay - gap - bh : ay + gap; // nothing was allowed: the preferred spot, held inside the band
        return new float[] {Math.max(20, Math.min(Ui.UI_W - 20 - bw, ax - bw / 2)),
                Math.max(minY, Math.min(maxY - bh, by)), below ? 1f : 0f};
    }

    /** Hard rectangles only (kept for callers and tests that have no soft ones). */
    public static float[] place(float bw, float bh, float ax, float ay, float gap, boolean below,
                                float minY, float maxY, float[][] avoid) {
        return place(bw, bh, ax, ay, gap, below, minY, maxY, avoid, null);
    }

    private static boolean clear(float x, float y, float w, float h, float[][] avoid) {
        if (avoid == null) return true;
        final float m = 6; // a breath between boxes
        for (float[] r : avoid)
            if (x < r[2] + m && x + w > r[0] - m && y < r[3] + m && y + h > r[1] - m) return false;
        return true;
    }

    private static float overlapArea(float x, float y, float w, float h, float[][] rects) {
        if (rects == null) return 0;
        float area = 0;
        for (float[] r : rects) {
            float ox = Math.min(x + w, r[2]) - Math.max(x, r[0]), oy = Math.min(y + h, r[3]) - Math.max(y, r[1]);
            if (ox > 0 && oy > 0) area += ox * oy;
        }
        return area;
    }

    /** The size a line needs: {width, height, textWidth}. */
    public static float[] size(Ui ui, GlyphLayout layout, String full) {
        layout.setText(ui.menu, full, ui.menu.getColor(), MAX_W, Align.left, true);
        float textW = layout.width, textH = layout.height;
        return new float[] {textW + 2 * PAD, textH + NAME_H + 2 * PAD - 4, textW};
    }

    /** A bubble's geometry: {x, y, w, h, tailX, textW, underFlag, speakerX, speakerY}. `ax, ay` is where the speaker is on
     *  screen; `hard` is what the box must not cover (HUD panels, other bubbles), `soft` what it should avoid if it can (bodies). */
    public static float[] box(Ui ui, GlyphLayout layout, String full, float ax, float ay, float gap,
                              boolean below, float minY, float maxY, float[][] hard, float[][] soft) {
        float[] sz = size(ui, layout, full);
        float[] p = place(sz[0], sz[1], ax, ay, gap, below, minY, maxY, hard, soft);
        float tx = Math.max(p[0] + 18, Math.min(p[0] + sz[0] - 18, ax));
        return new float[] {p[0], p[1], sz[0], sz[1], tx, sz[2], p[2], ax, ay};
    }

    /** The panel, its tail and its name tab. `pulse` > 0 lights the corner diamond (line complete). */
    public static void shapes(Ui ui, float[] q, boolean thought, String name, float[] c,
                              float alpha, float pulse) {
        float bx = q[0], by = q[1], bw = q[2], bh = q[3];
        // THE TAIL runs from the nearest point of the box to a spot just short of the speaker, however
        // far the box had to be moved to keep clear of everything: a bubble always points at its owner.
        float ax = q[7], ay = q[8];
        float sx = Math.max(bx + 16, Math.min(bx + bw - 16, ax));
        float sy = ay > by + bh / 2 ? by + bh : by;
        float dx = ax - sx, dy = ay - sy, len = (float) Math.sqrt(dx * dx + dy * dy);
        float stop = 22f; // it ends this far short of the speaker's centre
        float ex = len > stop ? sx + dx / len * (len - stop) : sx, ey = len > stop ? sy + dy / len * (len - stop) : sy;
        if (thought) { // a thought: diamonds stepping toward the head
            for (int i = 0; i < 3; i++) {
                float f = 0.2f + 0.35f * i;
                ui.shapes.setColor(c[0], c[1], c[2], 0.9f * alpha);
                Ornament.diamond(ui, sx + (ex - sx) * f, sy + (ey - sy) * f, 4f - i * 1.2f);
            }
        } else if (len > 4) { // speech: a fine line and a lotus bud pointing at them
            ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.94f * alpha);
            float ux = dx / len, uy = dy / len, px = -uy, py = ux;
            ui.shapes.triangle(sx + px * 9, sy + py * 9, sx - px * 9, sy - py * 9, sx + ux * 12, sy + uy * 12);
            ui.shapes.setColor(c[0], c[1], c[2], 0.85f * alpha);
            ui.shapes.rectLine(sx + px * 9, sy + py * 9, sx + ux * 12, sy + uy * 12, 1.2f);
            ui.shapes.rectLine(sx - px * 9, sy - py * 9, sx + ux * 12, sy + uy * 12, 1.2f);
            if (len > stop + 14) {
                ui.shapes.setColor(c[0], c[1], c[2], 0.55f * alpha);
                ui.shapes.rectLine(sx + ux * 12, sy + uy * 12, ex, ey, 1f);
                ui.shapes.setColor(c[0], c[1], c[2], 0.95f * alpha);
                Ornament.diamond(ui, ex, ey, 3f);
            }
        }
        Ornament.panel(ui, bx, by, bw, bh, CUT, c[0], c[1], c[2], 0.85f * alpha, !thought);
        float nw = ui.measure(ui.caption, name) + 26;
        float ny = by + bh - 8;
        ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, alpha);
        Ornament.fillChamfer(ui, bx + 14, ny, nw, 16, 4);
        ui.shapes.setColor(c[0], c[1], c[2], 0.85f * alpha);
        Ornament.lineChamfer(ui, bx + 14, ny, nw, 16, 4, 1.1f);
        Ornament.diamond(ui, bx + 22, ny + 8, 2f);
        if (pulse > 0) {
            ui.shapes.setColor(c[0], c[1], c[2], (0.5f + 0.5f * (float) Math.sin(pulse * 6)) * alpha);
            Ornament.diamond(ui, bx + bw - 14, by + 14, 3f);
        }
    }

    /** The name and the typed text. */
    public static void text(Ui ui, SpriteBatch batch, GlyphLayout layout, float[] q, String name,
                            String typed, float[] c, float alpha) {
        float bx = q[0], by = q[1], bh = q[3], textW = q[5];
        ui.caption.setColor(c[0], c[1], c[2], alpha);
        ui.caption.draw(batch, name, bx + 14 + 17, by + bh + 4);
        ui.menu.setColor(c[0], c[1], c[2], alpha);
        layout.setText(ui.menu, typed, ui.menu.getColor(), textW + 2, Align.left, true);
        ui.menu.draw(batch, layout, bx + PAD, by + bh - PAD - 4);
        ui.caption.setColor(1, 1, 1, 1);
        ui.menu.setColor(1, 1, 1, 1);
    }
}

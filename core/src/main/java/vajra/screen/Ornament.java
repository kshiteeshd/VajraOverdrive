package vajra.screen;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;

/** The title screen's ornament: a hairline frame with corner brackets, a line-diamond-line
 *  divider, and diamond markers flanking the selected row. All of it gold on ink, every stroke a
 *  hairline at low alpha, so the content stays the loudest thing on screen.
 *
 *  <p>These were private methods on HomeScreen. PauseScreen wants the same three pieces, and two
 *  hand-copied sets of ornament geometry drift the moment one of them is tuned — so it lives here
 *  once and both screens call it. Draws into an ALREADY-OPEN shape pass; it never begins or ends
 *  a batch.
 *
 *  <p>A ShapeRenderer in Filled mode has no outline, so rings and diamonds are built from
 *  primitives. */
public final class Ornament {
    private static final float LINE = 1.5f;  // hairline thickness
    private static final float FRAME_INSET = 2 * UiTheme.SAFE_MARGIN; // the frame sits twice the safe margin in

    private static final float FRAME_LEN = 44;

    // markers measure their own text; one shared layout, read immediately
    private static final GlyphLayout GL = new GlyphLayout();

    private Ornament() {}

    public static void gold(Ui ui, float a) {
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, a);
    }

    /** Four corner brackets, each with a small diamond tucked into the bend. */
    public static void frame(Ui ui) {
        float l = FRAME_INSET, r = Ui.UI_W - FRAME_INSET, b = FRAME_INSET, t = Ui.UI_H - FRAME_INSET;
        gold(ui, 0.5f);
        corner(ui, l, b, 1, 1);
        corner(ui, r, b, -1, 1);
        corner(ui, l, t, 1, -1);
        corner(ui, r, t, -1, -1);
    }

    private static void corner(Ui ui, float x, float y, float dx, float dy) {
        ui.shapes.rectLine(x, y, x + dx * FRAME_LEN, y, LINE);
        ui.shapes.rectLine(x, y, x, y + dy * FRAME_LEN, LINE);
        diamond(ui, x + dx * 12, y + dy * 12, 3);
    }

    /** Line, small dots, diamond, small dots, line — the divider. `half` sets its span. */
    public static void divider(Ui ui, float cx, float y, float half, float alpha) {
        gold(ui, alpha);
        ui.shapes.rectLine(cx - half, y, cx - 30, y, LINE);
        ui.shapes.rectLine(cx + 30, y, cx + half, y, LINE);
        diamond(ui, cx, y, 7);
        diamond(ui, cx - 17, y, 2.5f);
        diamond(ui, cx + 17, y, 2.5f);
        diamond(ui, cx - half, y, 3);
        diamond(ui, cx + half, y, 3);
    }

    /** A gold diamond either side of a row's label, centred on its cap height. Measures the text
     *  so the diamonds sit clear of whatever the row says. */
    public static void markers(Ui ui, float cx, String label, float rowY) {
        GL.setText(ui.hud, label);
        float y = rowY - GL.height / 2f;
        float off = GL.width / 2f + 26;
        gold(ui, Template.A_TITLE);
        diamond(ui, cx - off, y, 6);
        diamond(ui, cx + off, y, 6);
        gold(ui, 0.5f);
        diamond(ui, cx - off - 16, y, 2.5f);
        diamond(ui, cx + off + 16, y, 2.5f);
    }

    /** Diamond [HEADING] diamond — the heading's own markers, flanking the wide-tracked title
     *  whose baseline is `headingY`. The title text is the batch pass's business; this lands
     *  only the diamonds, measured wide enough (tracking included) to clear it. Shape pass. */
    public static void titleMarkers(Ui ui, String heading, float headingY) {
        GL.setText(ui.title, heading);
        float w = GL.width + (heading.length() - 1) * Template.TITLE_TRACK;
        float y = headingY - ui.title.getData().capHeight / 2f;
        float off = w / 2f + 30;
        gold(ui, Template.A_TITLE);
        diamond(ui, Template.COL - off, y, 8);
        diamond(ui, Template.COL + off, y, 8);
        gold(ui, 0.5f);
        diamond(ui, Template.COL - off - 18, y, 3);
        diamond(ui, Template.COL + off + 18, y, 3);
        divider(ui, Template.COL, PAGE_RULE_Y, 330, 0.4f); // the same rule under every page's heading
    }

    /** Where the rule under a page heading sits: below the subtitle, above the content. */
    public static final float PAGE_RULE_Y = UiTheme.TITLE_Y - 42;

    public static void diamond(Ui ui, float x, float y, float r) {
        ui.shapes.triangle(x - r, y, x, y + r, x + r, y);
        ui.shapes.triangle(x - r, y, x + r, y, x, y - r);
    }

    // =============================================================================================
    // THE TEMPLE PANEL - the one box every card, bubble and page uses: chamfered corners (the cut
    // of a temple niche read as a sci-fi panel), a double hairline like a mandala frame, tiny
    // diamonds in the cuts like rangoli dots, a faint scanline wash (the retro screen), and an
    // optional lotus finial on the lintel. Minimal on purpose: one colour, two hairlines.
    // =============================================================================================

    /** The chamfered outline's eight corners, clockwise from the bottom-left cut. */
    private static float[] octagon(float x, float y, float w, float h, float c) {
        return new float[] {x + c, y, x + w - c, y, x + w, y + c, x + w, y + h - c,
                x + w - c, y + h, x + c, y + h, x, y + h - c, x, y + c};
    }

    /** Fill the chamfered shape with the current shape colour. */
    public static void fillChamfer(Ui ui, float x, float y, float w, float h, float c) {
        ui.shapes.rect(x + c, y, w - 2 * c, h);
        ui.shapes.rect(x, y + c, c, h - 2 * c);
        ui.shapes.rect(x + w - c, y + c, c, h - 2 * c);
        ui.shapes.triangle(x, y + c, x + c, y, x + c, y + c);
        ui.shapes.triangle(x + w, y + c, x + w - c, y, x + w - c, y + c);
        ui.shapes.triangle(x, y + h - c, x + c, y + h, x + c, y + h - c);
        ui.shapes.triangle(x + w, y + h - c, x + w - c, y + h, x + w - c, y + h - c);
    }

    /** The chamfered hairline in the current shape colour. */
    public static void lineChamfer(Ui ui, float x, float y, float w, float h, float c, float thick) {
        float[] p = octagon(x, y, w, h, c);
        for (int i = 0; i < 8; i++)
            ui.shapes.rectLine(p[2 * i], p[2 * i + 1], p[(2 * i + 2) % 16], p[(2 * i + 3) % 16], thick);
    }

    /** A padlock in the current shape colour: a body and a shackle. `k` scales it (1 = 18 px tall). */
    public static void lock(Ui ui, float cx, float cy, float k) {
        ui.shapes.rect(cx - 7 * k, cy - 8 * k, 14 * k, 10 * k);
        ui.shapes.rectLine(cx - 4.5f * k, cy + 2 * k, cx - 4.5f * k, cy + 7 * k, 1.6f * k);
        ui.shapes.rectLine(cx + 4.5f * k, cy + 2 * k, cx + 4.5f * k, cy + 7 * k, 1.6f * k);
        ui.shapes.rectLine(cx - 4.5f * k, cy + 7 * k, cx + 4.5f * k, cy + 7 * k, 1.6f * k);
        ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1f);
        ui.shapes.rect(cx - 1 * k, cy - 5 * k, 2 * k, 4 * k); // the keyhole
    }

    /** A lotus bud: a tall centre petal and two leaning ones on a short base line. `s` is its
     *  height in px. */
    public static void lotus(Ui ui, float cx, float by, float s) {
        ui.shapes.triangle(cx - s * 0.22f, by, cx + s * 0.22f, by, cx, by + s);
        ui.shapes.triangle(cx - s * 0.12f, by, cx - s * 0.5f, by + s * 0.62f, cx - s * 0.5f, by);
        ui.shapes.triangle(cx + s * 0.12f, by, cx + s * 0.5f, by + s * 0.62f, cx + s * 0.5f, by);
        ui.shapes.rectLine(cx - s * 0.6f, by, cx + s * 0.6f, by, 1f);
    }

    /** The panel: ink ground with scanlines, the outer and inner hairlines, the rangoli diamonds
     *  in the four cuts, and (finial) a lotus on the top edge. `a` is the line strength. */
    public static void panel(Ui ui, float x, float y, float w, float h, float c,
                             float r, float g, float b, float a, boolean finial) {
        ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.94f);
        fillChamfer(ui, x, y, w, h, c);
        ui.shapes.setColor(1, 1, 1, 0.022f); // scanlines: the retro screen, barely there
        for (float ly = y + c; ly < y + h - c; ly += 4) ui.shapes.rect(x + 3, ly, w - 6, 1);
        ui.shapes.setColor(r, g, b, a);
        lineChamfer(ui, x, y, w, h, c, 1.2f);
        ui.shapes.setColor(r, g, b, a * 0.38f);
        lineChamfer(ui, x + 3.5f, y + 3.5f, w - 7, h - 7, Math.max(1, c - 2.5f), 1f);
        ui.shapes.setColor(r, g, b, Math.min(1f, a + 0.15f));
        float k = c * 0.5f;
        diamond(ui, x + k, y + k, 2.2f);
        diamond(ui, x + w - k, y + k, 2.2f);
        diamond(ui, x + k, y + h - k, 2.2f);
        diamond(ui, x + w - k, y + h - k, 2.2f);
        if (finial) { // a temple arch on the lintel: a small ogee bump, a lotus on its crown
            float cx = x + w / 2f, top = y + h;
            ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.94f);
            ui.shapes.triangle(cx - 22, top, cx - 11, top + 7, cx + 11, top + 7);
            ui.shapes.triangle(cx - 22, top, cx + 11, top + 7, cx + 22, top);
            ui.shapes.setColor(r, g, b, a);
            ui.shapes.rectLine(cx - 22, top, cx - 11, top + 7, 1.2f);
            ui.shapes.rectLine(cx - 11, top + 7, cx + 11, top + 7, 1.2f);
            ui.shapes.rectLine(cx + 11, top + 7, cx + 22, top, 1.2f);
            ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1f);
            ui.shapes.rect(cx - 21, top - 1, 42, 3);              // the lintel opens under the arch
            ui.shapes.setColor(r, g, b, Math.min(1f, a + 0.2f));
            lotus(ui, cx, top + 7, 11);
        }
        // kolam dots along the sill: five, the middle one lit
        ui.shapes.setColor(r, g, b, a * 0.7f);
        for (int i = -2; i <= 2; i++) diamond(ui, x + w / 2f + i * 9, y + 4, i == 0 ? 2.2f : 1.3f);
    }
}

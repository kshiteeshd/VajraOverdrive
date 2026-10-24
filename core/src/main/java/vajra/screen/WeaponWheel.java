package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.util.Locale;
import vajra.ship.Hangar;
import vajra.weapon.WeaponDef;
import vajra.world.World;

/** THE WEAPON WHEEL. Tap the weapon key and the next pattern you own is fitted; HOLD it and the world slows to a
 *  crawl and a wheel opens round the middle of the screen: every pattern you own is a chip on the ring, the one in
 *  your guns is ringed in gold, and the mouse points at the one you want. Let go of the key to fit it; pointing at
 *  the dead centre (or nowhere) keeps what you have. It draws in the vedic HUD's quiet gold and works under either
 *  HUD. The key is V, or B while a session owns V for push-to-talk.
 *
 *  <p>The geometry and the pick are pure so a test can pin them; {@link #render} is the one drawing call. */
final class WeaponWheel {
    /** Seconds the key must be held before the wheel opens: shorter is a tap. */
    static final float OPEN_AFTER = 0.22f;
    /** The world's pace while the wheel is open in a SHARED flight: a crawl (a solo flight pauses outright). */
    static final float SLOW = 0.12f;
    static final float CX = Ui.UI_W / 2f, CY = Ui.UI_H / 2f, RING = 150, CHIP = 30, DEAD = 46;

    private static final float[] GOLD = VedicHud.GOLD;

    private final Ui ui;
    private final WeaponCards cards = new WeaponCards();

    WeaponWheel(Ui ui) { this.ui = ui; }

    /** The patterns on the wheel: every one you own, in the hangar's own order. */
    static int[] owned(Hangar h) {
        int n = 0;
        for (boolean b : h.weaponOwned) if (b) n++;
        int[] out = new int[n];
        int k = 0;
        for (int i = 0; i < h.weaponOwned.length; i++) if (h.weaponOwned[i]) out[k++] = i;
        return out;
    }

    /** Degrees (0 = east, counter-clockwise) of chip i of n: the first sits at the top, the rest follow clockwise. */
    static float angle(int i, int n) { return 90f - i * 360f / n; }

    /** The chip the pointer is on, given its offset from the wheel's centre, or -1 in the dead centre. */
    static int pick(int n, float dx, float dy) {
        if (n <= 0 || dx * dx + dy * dy < DEAD * DEAD) return -1;
        float a = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        int best = 0;
        float bestD = 1e9f;
        for (int i = 0; i < n; i++) {
            float d = Math.abs(((a - angle(i, n) + 540f) % 360f) - 180f);
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    /** Draw the wheel over the frozen-slow world. `hover` is the pointed chip (an index into {@link #owned}) or -1. */
    void render(SpriteBatch batch, World w, int hover) {
        int[] own = owned(w.hangar);
        int n = own.length;
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        ui.shapes.setColor(0, 0, 0, 0.38f);                 // over the blurred flight
        ui.shapes.rect(0, 0, Ui.UI_W, Ui.UI_H);
        // the hub
        ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.9f);
        ui.shapes.circle(CX, CY, DEAD, 40);
        gold(0.85f);
        arc(CX, CY, DEAD, 0, 360, 36, 1.6f);
        if (hover >= 0 && n > 0) { // a spoke to the pointed chip
            float[] p = slot(hover, n);
            gold(0.7f);
            ui.shapes.rectLine(CX + (p[0] - CX) * DEAD / RING, CY + (p[1] - CY) * DEAD / RING, p[0], p[1], 1.4f);
        }
        for (int i = 0; i < n; i++) {
            float[] p = slot(i, n);
            boolean cur = own[i] == w.hangar.weaponIndex, hov = i == hover;
            float r = CHIP * (hov ? 1.18f : 1f);
            ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.94f);
            ui.shapes.circle(p[0], p[1], r, 32);
            gold(hov ? 1f : cur ? 0.95f : 0.45f);
            arc(p[0], p[1], r, 0, 360, 32, hov ? 2.6f : cur ? 2.2f : 1.3f);
            if (cur) { // the fitted one: a lit diamond on its ring
                gold(1f);
                dia(p[0], p[1] + r + 1, 3.2f);
            }
        }
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        for (int i = 0; i < n; i++) {
            float[] p = slot(i, n);
            float size = (CHIP * 2 - 14) * (i == hover ? 1.18f : 1f);
            batch.setColor(1, 1, 1, i == hover || own[i] == w.hangar.weaponIndex ? 1f : 0.7f);
            batch.draw(cards.get(own[i]), p[0] - size / 2, p[1] - size / 2, size, size);
        }
        batch.setColor(1, 1, 1, 1);
        // the hub's words: the pointed pattern (else the fitted one), its temper, and what letting go does
        int shown = hover >= 0 && n > 0 ? own[hover] : w.hangar.weaponIndex;
        ui.hud.setColor(1, 1, 1, 1);
        ui.centered(batch, ui.hud, WeaponDef.ALL[shown].name.toUpperCase(Locale.ROOT), CY + 12);
        ui.hud.setColor(1, 1, 1, 1);
        ui.caption.setColor(GOLD[0], GOLD[1], GOLD[2], 1f);
        ui.centered(batch, ui.caption, "RANK " + ROMAN[Math.min(2, w.hangar.weaponRank[shown])], CY - 8);
        ui.caption.setColor(1, 1, 1, 0.6f);
        ui.centered(batch, ui.caption, "RELEASE " + VedicHud.weaponKey() + " TO FIT", CY - RING - CHIP - 26);
        ui.caption.setColor(1, 1, 1, 1);
        batch.end();
    }

    private static final String[] ROMAN = {"I", "II", "III"};

    private static float[] slot(int i, int n) {
        float a = angle(i, n);
        return new float[] {CX + RING * MathUtils.cosDeg(a), CY + RING * MathUtils.sinDeg(a)};
    }

    void dispose() { cards.dispose(); }

    // --- primitives ---------------------------------------------------------------------------

    private void gold(float a) { ui.shapes.setColor(GOLD[0], GOLD[1], GOLD[2], a); }

    private void dia(float x, float y, float r) {
        ui.shapes.triangle(x - r, y, x, y + r, x + r, y);
        ui.shapes.triangle(x - r, y, x, y - r, x + r, y);
    }

    private void arc(float cx, float cy, float r, float from, float to, int steps, float w) {
        float px = cx + r * MathUtils.cosDeg(from), py = cy + r * MathUtils.sinDeg(from);
        for (int i = 1; i <= steps; i++) {
            float a = from + (to - from) * i / steps;
            float x = cx + r * MathUtils.cosDeg(a), y = cy + r * MathUtils.sinDeg(a);
            ui.shapes.rectLine(px, py, x, y, w);
            px = x;
            py = y;
        }
    }
}

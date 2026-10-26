package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import vajra.world.LokaTable;
import vajra.world.World;

/** THE VAJRA: the piece of the thunderbolt a ruler lets go of, and the count of them.
 *  <ul>
 *  <li>THE SHOWING (World.pieceShowT, 0 .. PIECE_SHOW): the instant the piece is taken, the screen dims a little and the piece is shown BIG at the middle, with its number and the loka that gave it;
 *      after a moment it shrinks and flies to the corner;</li>
 *  <li>THE PANEL (top-right, once the first piece is taken): the blade in miniature, a band lit for every piece held, and the count "1/7 VAJRA".</li>
 *  </ul>
 *  Everything is drawn in UI space after the HUD. */
final class VajraPanel {
    static final float PW = 176, PH = 50, M = VedicHud.M;
    static final float PX = Ui.UI_W - M - PW, PY = Ui.UI_H - M - PH;
    private static final float ICON_X = PX + 30, ICON_Y = PY + PH / 2f;
    private static final String[] ORDINAL = {"FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH", "SIXTH", "SEVENTH"};
    private static final String[] COUNT_WORD = {"NONE", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX"};
    private static final float IN = 0.6f, HOLD_END = 3.9f, FLY_END = 4.9f;

    private final Ui ui;
    private final VajraImages vj = new VajraImages();
    private int shown;                 // the count the panel shows (it lags the true count until the piece has flown home)
    private float pulse;               // the panel's own flash when a piece lands in it

    VajraPanel(Ui ui) { this.ui = ui; }

    private static int held(World w) {
        int n = 0;
        for (boolean b : w.hangar.pieces) if (b) n++;
        return n;
    }

    void render(SpriteBatch batch, World w, float dt) {
        int have = held(w);
        float s = w.pieceShowT;
        boolean showing = s >= 0f;
        if (!showing) shown = have;
        else if (s >= FLY_END - 0.1f && shown != have) { shown = have; pulse = 1f; }
        pulse = Math.max(0f, pulse - dt * 1.4f);
        if (!showing && have == 0) return;

        boolean[] owned = new boolean[VajraImages.BANDS];
        for (int i = 0; i < owned.length; i++) owned[i] = w.hangar.pieces[i] && !(showing && i == w.pieceShowLoka && s < FLY_END - 0.1f);
        float panelA = showing && shown == 0 ? 0f : 1f;                                           // the first panel waits for the piece to arrive

        // ---- shapes: the dimming, the halo behind the big piece, the panel's frame
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        float k = 0f, cx = Ui.UI_W / 2f, cy = Ui.UI_H / 2f + 34f, size = 0f;
        if (showing) {
            float inK = MathUtils.clamp(s / IN, 0f, 1f), outK = MathUtils.clamp((s - HOLD_END) / (FLY_END - HOLD_END), 0f, 1f);
            float ease = outK * outK * (3f - 2f * outK);
            k = inK * (1f - ease);
            ui.shapes.setColor(0f, 0f, 0f, 0.55f * k);
            ui.shapes.rect(0, 0, Ui.UI_W, Ui.UI_H);
            float over = 1f + 0.12f * (float) Math.sin(Math.min(1f, inK) * Math.PI);          // the piece lands with a little overshoot
            cx += (ICON_X - cx) * ease;
            cy += (ICON_Y - cy) * ease;
            size = (360f * over) * (1f - ease) + 34f * ease;
            float t = w.clock;
            for (int i = 0; i < 5; i++) {                                                       // the light round it, in rings
                float r = size * (0.55f + 0.16f * i) * (1f + 0.04f * (float) Math.sin(t * 3f + i));
                ui.shapes.setColor(1f, 0.82f, 0.38f, 0.07f * k * (1f - i * 0.17f));
                ui.shapes.circle(cx, cy, r, 40);
            }
            ui.shapes.setColor(1f, 0.86f, 0.5f, 0.55f * k);
            Ornament.diamond(ui, cx, cy + size * 0.62f, 5f);
            Ornament.diamond(ui, cx, cy - size * 0.62f, 5f);
            ui.shapes.rectLine(cx - 260f * k, cy - size * 0.68f, cx + 260f * k, cy - size * 0.68f, 1.2f);
        }
        if (panelA > 0f) {
            float x = PX + (1f - panelA) * 200f;
            Ornament.panel(ui, x, PY, PW, PH, 6, 0.82f, 0.70f, 0.42f, 0.85f + 0.15f * pulse, false);
            if (pulse > 0f) {
                ui.shapes.setColor(1f, 0.86f, 0.5f, 0.35f * pulse);
                Ornament.fillChamfer(ui, x, PY, PW, PH, 6);
            }
        }
        ui.shapes.end();

        // ---- art and words
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        if (showing && k > 0.01f) {
            batch.setColor(1f, 1f, 1f, k);
            vj.drawBlade(batch, cx, cy, size, owned, w.pieceShowLoka, 0.5f + 0.5f * (float) Math.sin(w.clock * 6f), k);
            batch.setColor(1f, 1f, 1f, 1f);
            if (s > 0.5f && s < HOLD_END + 0.4f) {
                float ta = MathUtils.clamp((s - 0.5f) / 0.6f, 0f, 1f) * (1f - MathUtils.clamp((s - HOLD_END) / 0.5f, 0f, 1f));
                int idx = MathUtils.clamp(have - 1, 0, 6);
                ui.title.setColor(1f, 0.9f, 0.6f, ta);
                ui.centeredSpaced(batch, ui.title, "THE " + ORDINAL[idx] + " PIECE OF THE VAJRAYUDHA", Ui.UI_H / 2f + 270f, 3f);
                ui.title.setColor(1f, 1f, 1f, 1f);
                ui.menu.setColor(0.9f, 0.86f, 0.75f, ta);
                int left = VajraImages.BANDS - have;
                ui.centered(batch, ui.menu, LokaTable.at(w.pieceShowLoka).name.toUpperCase(java.util.Locale.ROOT) + " GIVES IT UP.  " + (left == 0 ? "THE BOLT IS WHOLE." : COUNT_WORD[left] + " REMAIN."), Ui.UI_H / 2f - 250f);
                ui.menu.setColor(1f, 1f, 1f, 1f);
            }
        }
        if (panelA > 0f) {
            float x = PX + (1f - panelA) * 200f;
            batch.setColor(1f, 1f, 1f, 1f);
            boolean[] mini = new boolean[VajraImages.BANDS];
            for (int i = 0; i < mini.length; i++) mini[i] = w.hangar.pieces[i] && !(showing && i == w.pieceShowLoka && s < FLY_END - 0.1f);
            vj.drawBlade(batch, x + 30f, ICON_Y, 34f, mini, -1, 0f, 1f);
            ui.score.setColor(1f, 0.9f, 0.62f, 1f);
            ui.score.draw(batch, shown + "/" + VajraImages.BANDS, x + 62f, PY + PH - 9f);
            ui.score.setColor(1f, 1f, 1f, 1f);
            ui.caption.setColor(0.82f, 0.70f, 0.42f, 0.95f);
            ui.caption.draw(batch, "VAJRA", x + 124f, PY + 17f);
            ui.caption.setColor(1f, 1f, 1f, 1f);
        }
        batch.end();
    }

    void dispose() { vj.dispose(); }
}

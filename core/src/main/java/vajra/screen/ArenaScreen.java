package vajra.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.world.Arena;

/** THE TEST ARENA'S MENU (Settings > TEST): categories on the left, the scenarios of the chosen one in the middle, and what the highlighted scenario is for on the right.
 *  W/S move, D or ENTER goes into a category, A goes back, ENTER on a scenario flies it. Nothing flown here is saved. */
public final class ArenaScreen implements FlowState {
    private static final float CAT_X = 80, CAT_Y = 468, CAT_PITCH = 29;
    private static final float SC_X = 360, SC_Y = 468, SC_PITCH = 25, SC_W = 440;
    static final int VISIBLE = 15;
    private static final float PANEL_X = 850, PANEL_Y = 100, PANEL_W = 350, PANEL_H = 380;

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final GlyphLayout layout = new GlyphLayout();
    private int cat, sel, top;
    private boolean inList;

    public ArenaScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
    }

    private List<Arena.Scenario> list() { return Arena.in(Arena.CATEGORIES[cat]); }

    static int clampTop(int sel, int top, int n) {
        if (sel < top) top = sel;
        if (sel >= top + VISIBLE) top = sel - VISIBLE + 1;
        return Math.max(0, Math.min(Math.max(0, n - VISIBLE), top));
    }

    private void setCat(int c) {
        cat = Math.floorMod(c, Arena.CATEGORIES.length);
        sel = 0;
        top = 0;
    }

    private void setSel(int s) {
        int n = list().size();
        sel = Math.floorMod(s, Math.max(1, n));
        top = clampTop(sel, top, n);
    }

    private void launch() {
        List<Arena.Scenario> l = list();
        if (l.isEmpty()) return;
        flow.push(GameScreen.arena(batch, flow, ui, l.get(sel)));
    }

    @Override
    public void update(float dt) {}

    @Override
    public boolean keyDown(int k) {
        if (k == Input.Keys.ESCAPE) {
            if (inList) inList = false;
            else flow.pop();
            return true;
        }
        boolean up = k == Input.Keys.W || k == Input.Keys.UP, down = k == Input.Keys.S || k == Input.Keys.DOWN;
        if (inList) {
            if (up) { setSel(sel - 1); return true; }
            if (down) { setSel(sel + 1); return true; }
            if (k == Input.Keys.A || k == Input.Keys.LEFT) { inList = false; return true; }
            if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) { launch(); return true; }
            if (k == Input.Keys.PAGE_DOWN) { setSel(Math.min(list().size() - 1, sel + VISIBLE)); return true; }
            if (k == Input.Keys.PAGE_UP) { setSel(Math.max(0, sel - VISIBLE)); return true; }
            return false;
        }
        if (up) { setCat(cat - 1); return true; }
        if (down) { setCat(cat + 1); return true; }
        if (k == Input.Keys.D || k == Input.Keys.RIGHT || k == Input.Keys.ENTER || k == Input.Keys.SPACE) { inList = true; return true; }
        return false;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        Vector2 m = ui.at(sx, sy);
        for (int i = 0; i < Arena.CATEGORIES.length; i++)
            if (Template.box(m, CAT_X - 20, CAT_Y - i * CAT_PITCH - CAT_PITCH / 2, CAT_X + 230, CAT_Y - i * CAT_PITCH + CAT_PITCH / 2)) {
                setCat(i);
                inList = true;
                return true;
            }
        List<Arena.Scenario> l = list();
        for (int i = 0; i < VISIBLE && top + i < l.size(); i++)
            if (Template.box(m, SC_X - 20, SC_Y - i * SC_PITCH - SC_PITCH / 2, SC_X + SC_W, SC_Y - i * SC_PITCH + SC_PITCH / 2)) {
                inList = true;
                if (sel == top + i) launch();
                else setSel(top + i);
                return true;
            }
        return false;
    }

    @Override
    public boolean scrolled(float ax, float ay) {
        if (inList) setSel(sel + (ay > 0 ? 1 : -1));
        else setCat(cat + (ay > 0 ? 1 : -1));
        return true;
    }

    @Override
    public void render() {
        List<Arena.Scenario> l = list();
        Arena.Scenario cur = l.isEmpty() ? null : l.get(sel);
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f);
        Ornament.frame(ui);
        Ornament.titleMarkers(ui, "TEST ARENA", Template.TITLE_Y);
        float cy = CAT_Y - cat * CAT_PITCH;
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, inList ? 0.45f : 0.95f);
        Ornament.diamond(ui, CAT_X - 16, cy + 5, 4f);
        if (inList) {
            float sy = SC_Y - (sel - top) * SC_PITCH;
            ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.95f);
            Ornament.diamond(ui, SC_X - 16, sy + 5, 4f);
            ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.07f);
            ui.shapes.rect(SC_X - 8, sy - SC_PITCH / 2f + 2, SC_W + 8, SC_PITCH - 2);
        }
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.25f);
        ui.shapes.rect(SC_X - 40, CAT_Y - CAT_PITCH * Arena.CATEGORIES.length + 10, 1, CAT_PITCH * Arena.CATEGORIES.length - 10);
        if (l.size() > VISIBLE) {                                                          // a scroll bar
            float h = VISIBLE * SC_PITCH, bh = h * VISIBLE / l.size(), by = SC_Y + 14 - bh - (h - bh) * top / (l.size() - VISIBLE);
            ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.4f);
            ui.shapes.rect(SC_X + SC_W + 8, by, 3, bh);
        }
        Ornament.panel(ui, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 9, Template.accentR, Template.accentG, Template.accentB, 0.6f, false);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        Template.title(ui, batch, "TEST ARENA", Template.TITLE_Y, 1f);
        Template.sub(ui, batch, Arena.count() + " scenarios in " + Arena.CATEGORIES.length + " categories. Nothing here is saved.", Template.TITLE_Y - 26, 1f);
        for (int i = 0; i < Arena.CATEGORIES.length; i++) {
            boolean on = i == cat;
            ui.hud.setColor(on ? Template.accentR : 1, on ? Template.accentG : 1, on ? Template.accentB : 1, on ? 1f : 0.55f);
            ui.hud.draw(batch, Arena.CATEGORIES[i], CAT_X, CAT_Y - i * CAT_PITCH);
            ui.caption.setColor(1, 1, 1, on ? 0.7f : 0.35f);
            ui.caption.draw(batch, Integer.toString(Arena.in(Arena.CATEGORIES[i]).size()), CAT_X + 200, CAT_Y - i * CAT_PITCH);
        }
        for (int i = 0; i < VISIBLE && top + i < l.size(); i++) {
            boolean on = inList && top + i == sel;
            ui.hud.setColor(on ? Template.accentR : 1, on ? Template.accentG : 1, on ? Template.accentB : 1, on ? 1f : 0.75f);
            ui.hud.draw(batch, l.get(top + i).name, SC_X, SC_Y - i * SC_PITCH, SC_W, Align.left, false);
        }
        ui.hud.setColor(1, 1, 1, 1);
        if (cur != null) {
            ui.title.setColor(Template.accentR, Template.accentG, Template.accentB, 1f);
            layout.setText(ui.menu, cur.name, ui.menu.getColor(), PANEL_W - 56, Align.left, true);
            ui.menu.setColor(Template.accentR, Template.accentG, Template.accentB, 1f);
            ui.menu.draw(batch, layout, PANEL_X + 28, PANEL_Y + PANEL_H - 28);
            ui.menu.setColor(1, 1, 1, 1);
            float ty = PANEL_Y + PANEL_H - 40 - layout.height - 16;
            layout.setText(ui.hud, cur.blurb, ui.hud.getColor(), PANEL_W - 56, Align.left, true);
            ui.hud.setColor(1, 1, 1, 0.85f);
            ui.hud.draw(batch, layout, PANEL_X + 28, ty);
            ui.hud.setColor(1, 1, 1, 1);
            ui.caption.setColor(Template.accentR, Template.accentG, Template.accentB, 0.9f);
            ui.caption.draw(batch, inList ? "ENTER  FLY IT" : "D  OPEN THE CATEGORY", PANEL_X + 28, PANEL_Y + 56);
            ui.caption.setColor(1, 1, 1, 0.5f);
            ui.caption.draw(batch, "IN FLIGHT: F4 BAR  F5 GOD  F6 PATTERN  F7 RANK  F8 FOES  F9 CLEAR  F10 REFILL  F12 RESET", PANEL_X + 28, PANEL_Y + 34, PANEL_W - 56, Align.left, true);
            ui.caption.setColor(1, 1, 1, 1);
        }
        Template.keys(ui, batch, UiTheme.FOOT_Y, "W/S", "MOVE", "D", "OPEN", "A", "BACK", "ENTER", "FLY", "ESC", "LEAVE");
        batch.end();
    }
}

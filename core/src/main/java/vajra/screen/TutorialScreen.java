package vajra.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import vajra.coach.Lesson;
import vajra.coach.Lessons;
import vajra.flow.Flow;
import vajra.flow.FlowState;

/** THE PRIMER PAGE: every first time the game asks of a pilot, five sections of a few lessons
 *  each. A/D moves between sections, W/S between lessons, ENTER opens the full key list, ESC
 *  leaves. The same gold-on-ink line-work as the title: a hairline frame, a bordered panel,
 *  diamonds on the selected lesson. Nothing animates - it is a page to read. */
public class TutorialScreen implements FlowState {
    private static final float TAB_Y = 500, TAB_GAP = 44;
    private static final float LIST_X = 120, LIST_Y = 440, LIST_PITCH = 44, LIST_W = 300;
    private static final float PANEL_X = 470, PANEL_Y = 150, PANEL_W = 690, PANEL_H = 320;
    private static final float TEXT_X = PANEL_X + 32, TEXT_W = PANEL_W - 64;

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final GlyphLayout layout = new GlyphLayout();
    private final float[] tabX = new float[Lessons.GROUPS.length];
    private final float[] tabW = new float[Lessons.GROUPS.length];
    private int group, row;

    public TutorialScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        float total = -TAB_GAP;
        for (int i = 0; i < tabW.length; i++) {
            tabW[i] = ui.measure(ui.hud, Lessons.GROUPS[i]);
            total += tabW[i] + TAB_GAP;
        }
        float x = Ui.UI_W / 2f - total / 2f;
        for (int i = 0; i < tabW.length; i++) {
            tabX[i] = x;
            x += tabW[i] + TAB_GAP;
        }
    }

    private List<Lesson> lessons() { return Lessons.inGroup(Lessons.GROUPS[group]); }

    /** Section step, wrapping. Package-private: pinned headless. */
    static int step(int cur, int dir, int n) { return Math.floorMod(cur + dir, n); }

    private void setGroup(int g) {
        group = step(g, 0, Lessons.GROUPS.length);
        row = 0;
    }

    @Override
    public void update(float dt) {
        // a still page
    }

    @Override
    public boolean keyDown(int k) {
        if (k == Input.Keys.ESCAPE) { flow.pop(); return true; }
        if (k == Input.Keys.ENTER) { flow.push(new ControlScreen(flow, ui, batch)); return true; }
        if (k == Input.Keys.A || k == Input.Keys.LEFT) { setGroup(group - 1); return true; }
        if (k == Input.Keys.D || k == Input.Keys.RIGHT) { setGroup(group + 1); return true; }
        if (k == Input.Keys.W || k == Input.Keys.UP) { row = step(row, -1, lessons().size()); return true; }
        if (k == Input.Keys.S || k == Input.Keys.DOWN) { row = step(row, 1, lessons().size()); return true; }
        return false;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        Vector2 m = ui.at(sx, sy);
        for (int i = 0; i < tabX.length; i++)
            if (Template.box(m, tabX[i] - TAB_GAP / 2, TAB_Y - 20, tabX[i] + tabW[i] + TAB_GAP / 2, TAB_Y + 20)) {
                setGroup(i);
                return true;
            }
        for (int i = 0; i < lessons().size(); i++) {
            float y = LIST_Y - i * LIST_PITCH;
            if (Template.box(m, LIST_X - 30, y - LIST_PITCH / 2, LIST_X + LIST_W, y + LIST_PITCH / 2)) {
                row = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void render() {
        List<Lesson> list = lessons();
        Lesson cur = list.get(row);
        boolean learned = vajra.profile.Profiles.active().learned(cur.id);

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f);
        Ornament.frame(ui);
        Ornament.titleMarkers(ui, "TUTORIALS", Template.TITLE_Y);
        // the section underline
        ui.shapes.setColor(Template.accentR, Template.accentG, Template.accentB, 0.9f);
        ui.shapes.rect(tabX[group], TAB_Y - 10, tabW[group], 2);
        // diamond beside the selected lesson
        float dy = LIST_Y - row * LIST_PITCH + 6;
        ui.shapes.rect(LIST_X - 22, dy - 4, 4, 4, 8, 8, 1, 1, 45);
        Ornament.panel(ui, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 9, Template.accentR, Template.accentG, Template.accentB, 0.6f, false);
        KeyCaps.shapes(ui, TEXT_X, PANEL_Y + PANEL_H - 130, cur.keys, 1f);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        Template.title(ui, batch, "TUTORIALS", Template.TITLE_Y, 1f);
        Template.sub(ui, batch, "What a pilot learns, in the order the world teaches it", Template.TITLE_Y - 26, 1f);

        for (int i = 0; i < Lessons.GROUPS.length; i++) {
            boolean on = i == group;
            ui.hud.setColor(on ? Template.accentR : 1, on ? Template.accentG : 1, on ? Template.accentB : 1,
                    on ? 1f : 0.5f);
            ui.hud.draw(batch, Lessons.GROUPS[i], tabX[i], TAB_Y);
        }
        for (int i = 0; i < list.size(); i++) {
            boolean on = i == row;
            ui.hud.setColor(on ? Template.accentR : 1, on ? Template.accentG : 1, on ? Template.accentB : 1,
                    on ? 1f : 0.7f);
            ui.hud.draw(batch, list.get(i).title, LIST_X, LIST_Y - i * LIST_PITCH);
        }
        ui.hud.setColor(1, 1, 1, 1);

        ui.title.setColor(Template.accentR, Template.accentG, Template.accentB, 1f);
        ui.title.draw(batch, cur.title, TEXT_X, PANEL_Y + PANEL_H - 32);
        ui.title.setColor(1, 1, 1, 1);
        if (learned) {
            ui.caption.setColor(Template.accentR, Template.accentG, Template.accentB, 0.9f);
            ui.right(batch, ui.caption, "LEARNED IN FLIGHT", PANEL_X + PANEL_W - 32, PANEL_Y + PANEL_H - 32);
            ui.caption.setColor(1, 1, 1, 1);
        }
        KeyCaps.labels(ui, batch, TEXT_X, PANEL_Y + PANEL_H - 130, cur.keys, 1f);

        float y = PANEL_Y + PANEL_H - (cur.keys.length == 0 ? 80 : 160);
        ui.hud.setColor(1, 1, 1, 0.85f);
        for (String line : cur.body) {
            layout.setText(ui.hud, line, ui.hud.getColor(), TEXT_W, Align.left, true);
            ui.hud.draw(batch, layout, TEXT_X, y);
            y -= layout.height + 14;
        }
        ui.hud.setColor(1, 1, 1, 1);
        ui.caption.setColor(1, 1, 1, 0.4f);
        ui.caption.draw(batch, "FIRST MET: " + cur.when, TEXT_X, PANEL_Y + 24);
        ui.caption.setColor(1, 1, 1, 1);

        Template.keys(ui, batch, UiTheme.FOOT_Y, "A/D", "SECTION", "W/S", "LESSON",
                "ENTER", "ALL KEYS", "ESC", "BACK");
        batch.end();
    }
}

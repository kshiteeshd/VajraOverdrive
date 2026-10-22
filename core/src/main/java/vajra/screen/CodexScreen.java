package vajra.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.world.Codex;
import vajra.world.World;

/** THE CODEX: the pause menu's page-book, read one page at a time — A/D or W/S flips, ESC
 *  closes. An unlocked page (the profile's book OR this run's fresh lifts) shows its body; a
 *  sealed one shows only what it is not. The screen opens on the first unread page, so new
 *  lifts are where the pilot lands. Reading is free: the run beneath stays frozen. */
public class CodexScreen implements FlowState {
    private static final float PITCH = 30; // body line pitch

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final boolean[] seen = new boolean[Codex.COUNT];
    private int page;

    public CodexScreen(Flow flow, Ui ui, SpriteBatch batch, World world) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        boolean[] book = vajra.profile.Profiles.active().codex;
        for (int i = 0; i < Codex.COUNT; i++)
            seen[i] = world.codex[i] || (book != null && i < book.length && book[i]);
        page = 0; // land on the first unread page; a finished book opens on its first page
        for (int i = 0; i < Codex.COUNT; i++)
            if (!seen[i]) {
                page = i;
                break;
            }
    }

    @Override
    public void update(float dt) {
        // nothing to advance: a still book
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.Q) {
            flow.pop();
            return true;
        }
        if (keycode == Input.Keys.A || keycode == Input.Keys.W
                || keycode == Input.Keys.LEFT || keycode == Input.Keys.UP) {
            page = (page + Codex.COUNT - 1) % Codex.COUNT;
            return true;
        }
        if (keycode == Input.Keys.D || keycode == Input.Keys.S
                || keycode == Input.Keys.RIGHT || keycode == Input.Keys.DOWN) {
            page = (page + 1) % Codex.COUNT;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        page = (page + (ui.at(sx, sy).x < Template.COL ? -1 : 1) + Codex.COUNT) % Codex.COUNT;
        return true;
    }

    @Override
    public boolean mouseMoved(int sx, int sy) {
        return false;
    }

    @Override
    public void render() {
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f); // opaque: its own screen, nothing bleeds through from below
        Ornament.frame(ui);
        Ornament.titleMarkers(ui, "CODEX", Template.TITLE_Y);
        ui.shapes.end();

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        Template.title(ui, batch, "CODEX", Template.TITLE_Y, 1);
        Template.sub(ui, batch,
                "PAGE " + (page + 1) + " OF " + Codex.COUNT + "   " + Codex.TITLE[page],
                Template.TITLE_Y - 44, 1);
        if (seen[page]) {
            String[] lines = Codex.BODY[page].split("\n");
            float y = 470;
            for (String line : lines) {
                Template.line(ui, batch, line, y, 0.9f);
                y += PITCH;
            }
        } else {
            Template.line(ui, batch, "STILL SEALED - THE WORLD HAS NOT SPOKEN THIS PAGE.", 470, 0.55f);
        }
        Template.keys(ui, batch, Template.FOOT_Y, "W/S or A/D", "FLIP", "CLICK SIDES", "FLIP", "ESC", "BACK");
        batch.end();
    }
}

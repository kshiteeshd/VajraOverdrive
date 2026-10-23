package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.profile.Profiles;

/** THE STORY: the pre-game comic, one panel at a time, shown before the title menu the first time
 *  the game is opened. {@link StoryScript} holds the panels and the narrator's line for each; the
 *  narrator speaks in a black band under the art, typed slowly like every scene in the game.
 *
 *  <p>SPACE / ENTER / click: finish the line, then bring up the next panel. LEFT / A: back a panel.
 *  ESC: skip the story. The last panel's turn hands over to the menu. With no art on disk the
 *  screen is skipped, so the game runs the same without it. Panels are loaded one ahead and freed
 *  behind, so nineteen big textures are never resident at once. */
public class ComicScreen implements FlowState {
    public static final float FADE = 0.7f;        // a panel fades up from black
    public static final float PUSH = 12f;         // seconds of the slow push-in
    public static final float CPS = 24f;          // narrator typing speed, as in every scene
    public static final float BAND = 150f;        // the narrator's black band, UI units
    private static final float MARGIN = 26f;

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final GlyphLayout layout = new GlyphLayout();
    private final Texture[] tex = new Texture[StoryScript.PANELS.length];
    private int at;
    private float t; // seconds on the current panel

    /** Is the story's art on disk? The first panel decides. */
    public static boolean available() {
        return Gdx.files.internal(StoryScript.PANELS[0].file).exists();
    }

    /** The screen to open the game on: the story if it has art and this pilot has not read it,
     *  else the menu. */
    public static FlowState firstOrHome(Flow flow, Ui ui, SpriteBatch batch) {
        if (!Profiles.active().storySeen && available()) return new ComicScreen(flow, ui, batch);
        return new HomeScreen(flow, ui, batch);
    }

    public ComicScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        load(0);
        load(1);
    }

    // --- pure, so the tests can pin them --------------------------------------------------

    /** The largest rect of aspect w:h that fits the area, centred: {x, y, width, height}. */
    public static float[] fit(float w, float h, float areaW, float areaH) {
        float s = Math.min(areaW / w, areaH / h);
        float fw = w * s, fh = h * s;
        return new float[] {(areaW - fw) / 2f, (areaH - fh) / 2f, fw, fh};
    }

    /** A panel's opacity {@code t} seconds after it appears. */
    public static float alpha(float t) { return Math.max(0f, Math.min(1f, t / FADE)); }

    /** The push-in: the panel grows from 94% to 100% of its fit over {@link #PUSH}, easing out. */
    public static float scale(float t) {
        float k = Math.max(0f, Math.min(1f, t / PUSH));
        return 0.94f + 0.06f * (1f - (1f - k) * (1f - k));
    }

    /** The narrator's line typed so far, {@code t} seconds after the panel appears (the fade first). */
    public static String typed(String full, float t) {
        int n = (int) Math.max(0, (t - FADE * 0.6f) * CPS);
        return full.substring(0, Math.min(full.length(), n));
    }

    /** Seconds until the whole line is typed. */
    public static float typeSeconds(String full) { return FADE * 0.6f + full.length() / CPS; }

    // --- paging ---------------------------------------------------------------------------

    private void load(int i) {
        if (i < 0 || i >= tex.length || tex[i] != null) return;
        Texture x = new Texture(Gdx.files.internal(StoryScript.PANELS[i].file), true);
        x.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear); // painted art, big shrink
        tex[i] = x;
    }

    private void free(int i) {
        if (i >= 0 && i < tex.length && tex[i] != null) { tex[i].dispose(); tex[i] = null; }
    }

    private boolean lineDone() { return t >= typeSeconds(StoryScript.PANELS[at].text); }

    private void turn(int dir) {
        if (dir > 0 && !lineDone()) { t = typeSeconds(StoryScript.PANELS[at].text); return; } // finish the line first
        int to = at + dir;
        if (to < 0) return;
        if (to >= tex.length) { finish(); return; }
        at = to;
        t = 0;
        load(at);
        load(at + 1);
        free(at - 2);
        free(at + 2);
    }

    private void finish() {
        Profiles.active().storySeen = true;
        Profiles.save();
        flow.set(new HomeScreen(flow, ui, batch));
    }

    @Override
    public void update(float dt) { t += dt; }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.ESCAPE: finish(); break;
            case Input.Keys.LEFT: case Input.Keys.A: turn(-1); break;
            default: turn(1); break; // SPACE, ENTER, RIGHT, anything else
        }
        return true;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        turn(button == Input.Buttons.RIGHT ? -1 : 1);
        return true;
    }

    // --- painting -------------------------------------------------------------------------

    @Override
    public void render() {
        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        Template.dim(ui, 1f);
        bandShapes();
        ui.shapes.end();

        Texture p = tex[at];
        float areaW = Ui.UI_W - 2 * MARGIN, areaH = Ui.UI_H - BAND - 2 * MARGIN;
        float[] r = fit(p.getWidth(), p.getHeight(), areaW, areaH);
        float s = scale(t);
        float w = r[2] * s, h = r[3] * s;
        float x = MARGIN + areaW / 2f - w / 2f, y = BAND + MARGIN + areaH / 2f - h / 2f;

        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        batch.setColor(1, 1, 1, alpha(t));
        batch.draw(p, x, y, w, h);
        batch.setColor(1, 1, 1, 1);

        String full = StoryScript.PANELS[at].text;
        float tw = Ui.UI_W - 300;
        layout.setText(ui.score, full, ui.score.getColor(), tw, Align.center, true);
        float ty = BAND / 2f + layout.height / 2f + 2; // vertically centred in the band
        ui.score.setColor(1, 0.97f, 0.88f, 1f);
        layout.setText(ui.score, typed(full, t), ui.score.getColor(), tw, Align.center, true);
        ui.score.draw(batch, layout, Ui.UI_W / 2f - tw / 2, ty);
        ui.score.setColor(1, 1, 1, 1);

        // the page and panel count on the left, the keys on the right
        ui.caption.setColor(1, 1, 1, 0.55f);
        ui.caption.draw(batch, "PAGE " + StoryScript.PANELS[at].page + " / " + StoryScript.pages()
                + "   -   " + (at + 1) + " / " + tex.length, 60, BAND - 16);
        String keys = lineDone() ? (at == tex.length - 1 ? "SPACE  BEGIN" : "SPACE  NEXT")
                : "SPACE  SKIP LINE";
        float pulse = lineDone() ? 0.5f + 0.35f * (float) Math.sin(t * 5) : 0.45f;
        ui.caption.setColor(1, 1, 1, pulse);
        ui.right(batch, ui.caption, keys + "      ESC  SKIP STORY", Ui.UI_W - 60, BAND - 16);
        ui.caption.setColor(1, 1, 1, 1);
        batch.end();
    }

    /** The narrator's band: black with a gold hairline and diamond on its top edge, like the scenes'. */
    private void bandShapes() {
        ui.shapes.setColor(0, 0, 0, 1);
        ui.shapes.rect(0, 0, Ui.UI_W, BAND);
        float cx = Ui.UI_W / 2f;
        Ornament.gold(ui, 0.55f);
        ui.shapes.rectLine(60, BAND, cx - 40, BAND, 1.2f);
        ui.shapes.rectLine(cx + 40, BAND, Ui.UI_W - 60, BAND, 1.2f);
        Ornament.diamond(ui, cx, BAND, 5);
        Ornament.diamond(ui, cx - 20, BAND, 2.2f);
        Ornament.diamond(ui, cx + 20, BAND, 2.2f);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < tex.length; i++) free(i);
    }
}

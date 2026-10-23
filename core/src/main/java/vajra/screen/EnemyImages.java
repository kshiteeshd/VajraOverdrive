package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.enemy.Enemy;

/** Enemy art. A kind with a 64x64 sheet (assets/enemies/atala/<kind>.png, see
 *  tools/gen_enemy_sheets.py) wears it: idle, rage, hit and death, with a crest for a leader and
 *  vents for an elite. A kind without one (Bala) keeps its older idle_N frames, animated in
 *  sequence. Missing files become gold diamonds, never a crash. */
public class EnemyImages {
    private static final int MAX_FRAMES = 16;
    private static final float FPS = 4;
    private final Texture[][] tex = new Texture[Enemy.Kind.values().length][MAX_FRAMES];
    private final TextureRegion[][] reg = new TextureRegion[Enemy.Kind.values().length][MAX_FRAMES];
    private final int[] frames = new int[Enemy.Kind.values().length];
    /** [kind][row][col] of the 8x4 sheet; null for a kind that has none. */
    private final TextureRegion[][][] sheet = new TextureRegion[Enemy.Kind.values().length][][];
    private final Texture[] sheetTex = new Texture[Enemy.Kind.values().length];
    static final int CELL = 64;
    // A body is drawn at Enemy.drawSize() - by kind and rank - which is always smaller than the
    // 64 px cell, so the sheets are filtered Linear: the downscale averages rather than drops.

    public EnemyImages() {
        for (Enemy.Kind k : Enemy.Kind.values()) {
            String sheetPath = "enemies/atala/" + k.name().toLowerCase() + ".png";
            if (Gdx.files.internal(sheetPath).exists()) {
                Texture t = ImageKit.texture(sheetPath, true);
                if (t != null) {
                    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    sheetTex[k.ordinal()] = t;
                    sheet[k.ordinal()] = TextureRegion.split(t, CELL, CELL);
                }
            }
            String base = "atala/enemies/" + k.name().toLowerCase() + "/idle_";
            int f = 0;
            // frame 0 always loads (diamond if absent); the rest stop at the first gap
            while (f < MAX_FRAMES && (f == 0 || Gdx.files.internal(base + f + ".png").exists())) {
                tex[k.ordinal()][f] = load(base + f + ".png");
                reg[k.ordinal()][f] = new TextureRegion(tex[k.ordinal()][f]);
                f++;
            }
            frames[k.ordinal()] = f;
        }
    }

    private Texture load(String path) {
        Texture t = ImageKit.texture(path, true);
        if (t == null) {
            Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            p.setColor(1, 0.84f, 0, 1);
            p.fillTriangle(16, 2, 30, 30, 2, 30);
            t = new Texture(p);
            p.dispose();
        }
        return t;
    }

    /** Which frame shows now: walks the whole cycle, staggered per unit by its phase. */
    public static int frame(float time, float phase, int frames) {
        return Math.floorMod((int) (time * FPS + phase), frames);
    }

    /** One idle frame as a bare region — a cutscene portrait. Frame 0 always loads. */
    public TextureRegion region(Enemy.Kind kind, int frame) {
        return reg[kind.ordinal()][Math.min(frame, frames[kind.ordinal()] - 1)];
    }

    /** Which sheet cell this unit shows now: row (0 plain, 2 leader, 3 elite) and column. Pure, so
     *  a test can pin it: a hit shows the hit frames, rage the right half of its row. */
    static int[] cell(Enemy e, float time) {
        int row = e.elite ? 3 : e.rank >= 1 ? 2 : 0;
        if (e.hitFlash > 0) return new int[] {1, e.hitFlash > 0.06f ? 0 : 1};
        return new int[] {row, (e.raging() ? 4 : 0) + frame(time, e.phase, 4)};
    }

    /** A dead body's frame: six frames over the effect's life. */
    static int deathFrame(float age, float life) {
        return Math.max(0, Math.min(5, (int) (age / life * 6)));
    }

    public boolean hasSheet(Enemy.Kind k) {
        return sheet[k.ordinal()] != null;
    }

    /** The last six frames of a dead unit, over where it fell. */
    public void drawDeath(SpriteBatch batch, vajra.world.Effect e) {
        TextureRegion[][] sh = sheet[e.kind];
        if (sh == null) return;
        TextureRegion r = sh[1][2 + deathFrame(e.age, e.life)];
        batch.setColor(1, 1, 1, 1);
        batch.draw(r, e.x - e.radius / 2, e.y - e.radius / 2, e.radius, e.radius);
    }

    public void draw(SpriteBatch batch, Enemy e, float time) {
        TextureRegion[][] sh = sheet[e.kind.ordinal()];
        if (sh != null) {
            int[] c = cell(e, time);
            float s = e.drawSize();
            if (!e.active && !e.voice && e.hitFlash <= 0) batch.setColor(0.6f, 0.6f, 0.62f, 1); // asleep: dimmed
            batch.draw(sh[c[0]][c[1]], e.x - s / 2, e.y - s / 2, s, s);
            batch.setColor(1, 1, 1, 1);
            return;
        }
        int f = frame(time, e.phase, frames[e.kind.ordinal()]);
        TextureRegion r = reg[e.kind.ordinal()][f];
        if (e.hitFlash > 0) batch.setColor(1f, 1f, 1f, 1);          // hit blink
        else if (e.elite) batch.setColor(1f, 0.84f, 0f, 1);
        else if (!e.active) batch.setColor(0.55f, 0.55f, 0.55f, 1);
        batch.draw(r, e.x - e.w / 2, e.y - e.h / 2, e.w, e.h);
        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() {
        for (Texture[] row : tex) for (Texture t : row) if (t != null) t.dispose();
        for (Texture t : sheetTex) if (t != null) t.dispose();
    }
}

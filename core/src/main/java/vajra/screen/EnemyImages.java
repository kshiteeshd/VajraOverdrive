package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.enemy.Enemy;

/** Enemy art. A kind with a 64x64 sheet (assets/enemies/atala/<kind>.png, see
 *  tools/gen_enemy_sheets.py; 16x4 cells, eight idle frames and eight rage) wears it: idle, rage, hit and death, with a crest for a leader and
 *  vents for an elite. A kind without one (Bala) keeps its older idle_N frames, animated in
 *  sequence. Missing files become gold diamonds, never a crash. */
public class EnemyImages {
    private static final int MAX_FRAMES = 16;
    private static final float FPS = 4;           // the older idle_N frames
    static final int IDLE_N = 8;                  // a sheet's idle (and rage) frames per row
    private static final float SHEET_FPS = 8;     // the sheet loop: eight frames, one second
    private final Texture[][] tex = new Texture[Enemy.Kind.values().length][MAX_FRAMES];
    private final TextureRegion[][] reg = new TextureRegion[Enemy.Kind.values().length][MAX_FRAMES];
    private final int[] frames = new int[Enemy.Kind.values().length];
    /** [kind][row][col] of the 16x4 sheet; null for a kind that has none. */
    private final TextureRegion[][][] sheet = new TextureRegion[Enemy.Kind.values().length][][];
    private final Texture[] sheetTex = new Texture[Enemy.Kind.values().length];
    static final int CELL = 64;
    // A body is drawn at Enemy.drawSize() - by kind and rank - which is always smaller than the
    // 64 px cell, so the sheets are filtered Linear: the downscale averages rather than drops.

    /** Vitala's units wear their own sheets (assets/enemies/vitala/): the six ships and, in the BALA slot, Bhairava - a 1024 x 512 sheet of 128 px
     *  cells: rows 0-2 the three phases' idle, row 3 hit x2 then death x6. A kind with no Vitala sheet (the keepers) wears its Atala sheet
     *  re-coloured into basalt and gold. {@code e.vitala} picks the set, so the one world can hold both. */
    private final TextureRegion[][][] sheetV = new TextureRegion[Enemy.Kind.values().length][][];
    private final Texture[] sheetVTex = new Texture[Enemy.Kind.values().length];
    private boolean bossSheet;
    static final int BOSS_CELL = 128;

    private Texture glowTex;

    /** A soft round glow (white, falling off smoothly): the aura of a ruler. */
    private Texture glow() {
        if (glowTex == null) {
            Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            for (int y = 0; y < 64; y++)
                for (int x = 0; x < 64; x++) {
                    float dx = (x + 0.5f) / 32f - 1f, dy = (y + 0.5f) / 32f - 1f, r = (float) Math.sqrt(dx * dx + dy * dy);
                    float a = Math.max(0f, 1f - r);
                    a = a * a * (3f - 2f * a);
                    pm.drawPixel(x, y, 0xFFFFFF00 | (int) (255 * a * a));
                }
            glowTex = new Texture(pm);
            pm.dispose();
            glowTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        return glowTex;
    }

    /** Bhairava: his aura, the afterimages of a charge, then his body in the pose of the moment (BossPose). */
    private void drawRuler(SpriteBatch batch, Enemy e, TextureRegion region, float s, float time) {
        float[] p = BossPose.of(e, time);
        int src = batch.getBlendSrcFunc(), dst = batch.getBlendDstFunc();
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        float gs = s * (1.5f + 0.1f * (float) Math.sin(time * 2.3f));
        batch.setColor(p[5], p[6], p[7], p[8]);
        batch.draw(glow(), e.x - gs / 2f, e.y - gs / 2f, gs, gs);
        float eye = 0.5f + 0.5f * (float) Math.sin(time * 3.1f);
        batch.setColor(1f, 0.55f + 0.3f * eye, 0.15f, 0.35f + 0.3f * eye + (e.pose == vajra.enemy.BhairavaBrain.CAST ? 0.3f : 0f));
        float es = s * 0.22f;
        batch.draw(glow(), e.x - es / 2f, e.y - s * 0.02f - es / 2f, es, es);           // the eye
        batch.setBlendFunction(src, dst);
        if (e.pose == vajra.enemy.BhairavaBrain.LUNGE) {                                  // afterimages down the line of the run
            for (int i = 3; i >= 1; i--) {
                batch.setColor(1f, 0.55f, 0.2f, 0.16f * (4 - i) / 3f);
                batch.draw(region, e.x - e.vx * 0.028f * i - s / 2f, e.y - e.vy * 0.028f * i - s / 2f, s / 2f, s / 2f, s, s, p[2], p[3], p[4]);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
        if (e.flinchT > 0f) batch.setColor(1f, 0.92f, 0.85f, 1f);
        batch.draw(region, e.x - s / 2f + p[0], e.y - s / 2f + p[1], s / 2f, s / 2f, s, s, p[2], p[3], p[4]);
    }

    public EnemyImages() {
        for (Enemy.Kind k : Enemy.Kind.values()) {
            String name = k.name().toLowerCase();
            String sheetPath = "enemies/atala/" + name + ".png";
            if (Gdx.files.internal(sheetPath).exists()) {
                Texture t = ImageKit.texture(sheetPath, true);
                if (t != null) {
                    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    sheetTex[k.ordinal()] = t;
                    sheet[k.ordinal()] = TextureRegion.split(t, CELL, CELL);
                }
            }
            String vp = "enemies/vitala/" + name + ".png";
            Texture tv = null;
            if (Gdx.files.internal(vp).exists()) tv = ImageKit.texture(vp, true);
            else if (Gdx.files.internal(sheetPath).exists()) {           // a keeper: the Atala sheet in basalt and gold
                Pixmap p = new Pixmap(Gdx.files.internal(sheetPath));
                TileImages.toBasalt(p);
                tv = new Texture(p);
                p.dispose();
            }
            if (tv != null) {
                tv.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                sheetVTex[k.ordinal()] = tv;
                boolean boss = k == Enemy.Kind.BALA && tv.getHeight() == 4 * BOSS_CELL;
                if (boss) bossSheet = true;
                sheetV[k.ordinal()] = boss ? TextureRegion.split(tv, BOSS_CELL, BOSS_CELL) : TextureRegion.split(tv, CELL, CELL);
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
    static int[] bossCell(Enemy e, float time) {
        if (e.hitFlash > 0) return new int[] {3, e.hitFlash > 0.06f ? 0 : 1};
        int phase = Math.max(0, Math.min(2, e.bossPhase - 1));
        return new int[] {phase, Math.floorMod((int) (time * SHEET_FPS + e.phase * 2), IDLE_N)};
    }

    /** A Vitala creature's sheet has thirteen rows: the usual four, then three action clips of sixteen frames for a plain unit, a leader and an elite (rows 4-6, 7-9, 10-12). */
    static final int ACTION_ROWS = 13, ACTION_FRAMES = 16;
    static final float ACTION_FPS = 12f;

    /** Which action clip (0..2) a unit is in at `time`, and how far through it (0..1), or {-1, 0} when it is idling. Every unit keeps its own rhythm (a cycle of 3.6 to 7.5 s, begun
     *  at its own moment) and does an action at the end of about two cycles in three, so a crowd is never in step. Pure, so a test can pin it. */
    static float[] action(Enemy e, float time) {
        int seed = Math.floorMod(Float.floatToIntBits(e.phase) * 31 + (int) (e.spawnX * 0.37f) + (int) (e.spawnY * 0.11f), 9973);
        float cycle = 3.6f + (seed % 8) * 0.55f, off = (seed % 13) * 0.61f, len = ACTION_FRAMES / ACTION_FPS;
        float pt = time + off;
        int n = (int) Math.floor(pt / cycle);
        float local = pt - n * cycle;
        if (local < cycle - len) return new float[] {-1f, 0f};
        int h = n * 73856093 ^ seed * 19349663;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        if (Math.floorMod(h >>> 5, 3) == 0) return new float[] {-1f, 0f};                       // one cycle in three, nothing: it only idles
        return new float[] {Math.floorMod(h >>> 11, 3), (local - (cycle - len)) / len};
    }

    /** The sheet cell of a Vitala creature: the usual rows, but a calm unit now and then does an action. */
    static int[] cellV(Enemy e, float time) {
        int[] base = cell(e, time);
        if (e.hitFlash > 0 || e.raging() || e.smoked || e.inRock || e.shockT > 0f) return base;
        float[] a = action(e, time);
        if (a[0] < 0f) return base;
        int variant = e.elite ? 2 : e.rank >= 1 ? 1 : 0;
        int row = (variant == 0 ? 4 : variant == 1 ? 7 : 10) + (int) a[0];
        return new int[] {row, Math.min(ACTION_FRAMES - 1, (int) (a[1] * ACTION_FRAMES))};
    }

    static int[] cell(Enemy e, float time) {
        int row = e.elite ? 3 : e.rank >= 1 ? 2 : 0;
        if (e.hitFlash > 0) return new int[] {1, e.hitFlash > 0.06f ? 0 : 1};
        return new int[] {row, (e.raging() ? IDLE_N : 0) + Math.floorMod((int) (time * SHEET_FPS + e.phase * 2), IDLE_N)};
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
        boolean v = e.kind >= 100;                                      // (a Vitala unit's death is filed at kind + 100)
        TextureRegion[][] sh = v ? sheetV[e.kind - 100] : sheet[e.kind];
        if (sh == null) return;
        TextureRegion r = sh[1][2 + deathFrame(e.age, e.life)];
        batch.setColor(1, 1, 1, 1);
        batch.draw(r, e.x - e.radius / 2, e.y - e.radius / 2, e.radius, e.radius);
    }

    public void draw(SpriteBatch batch, Enemy e, float time) {
        TextureRegion[][] sh = e.vitala && sheetV[e.kind.ordinal()] != null ? sheetV[e.kind.ordinal()] : sheet[e.kind.ordinal()];
        if (sh != null) {
            int[] c = bossSheet && e.vitala && e.kind == Enemy.Kind.BALA ? bossCell(e, time) : sh.length >= ACTION_ROWS ? cellV(e, time) : cell(e, time);
            float s = e.drawSize();
            if (!e.active && !e.voice && e.hitFlash <= 0) batch.setColor(0.6f, 0.6f, 0.62f, 1); // asleep: dimmed
            if (e.smoked || e.inRock) batch.setColor(1f, 1f, 1f, 0.28f);                      // in smoke or in rock: a ghost of itself
            if (e.depth > 0f) { // down in the shaft: smaller, darker and a little faded the deeper it is
                float k = 1f - 0.65f * e.depth;
                s *= 1f - 0.5f * e.depth;
                batch.setColor(k, k, k * 1.05f, 1f - 0.3f * e.depth);
            }
            float ox = 0f, oy = 0f;
            if (e.shockT > 0f) {                                                              // electric shock: it shakes and flashes blue-white
                ox = (float) Math.sin(time * 173f + e.x) * 3.5f;
                oy = (float) Math.cos(time * 151f + e.y) * 3.5f;
                if (((int) (time * 28f) & 1) == 0) batch.setColor(0.62f, 0.78f, 1f, 1f); else batch.setColor(1f, 1f, 1f, 1f);
            }
            if (e.wetT > 0f && e.shockT <= 0f) batch.setColor(0.6f, 0.82f, 1f, 1f);                     // a soaked hull is blue-dark and heavy
            if (bossSheet && e.vitala && e.kind == Enemy.Kind.BALA && !e.yielding) drawRuler(batch, e, sh[c[0]][c[1]], s, time);
            else batch.draw(sh[c[0]][c[1]], e.x - s / 2 + ox, e.y - s / 2 + oy, s, s);
            batch.setColor(1, 1, 1, 1);
            return;
        }
        int f = frame(time, e.phase, frames[e.kind.ordinal()]);
        TextureRegion r = reg[e.kind.ordinal()][f];
        if (e.hitFlash > 0) batch.setColor(1f, 1f, 1f, 1);          // hit blink
        else if (e.elite) batch.setColor(1f, 0.84f, 0f, 1);
        else if (!e.active) batch.setColor(0.55f, 0.55f, 0.55f, 1);
        if (e.shockT > 0f) batch.setColor(((int) (time * 28f) & 1) == 0 ? 0.62f : 1f, ((int) (time * 28f) & 1) == 0 ? 0.78f : 1f, 1f, 1f);
        float ox = e.shockT > 0f ? (float) Math.sin(time * 173f + e.x) * 3.5f : 0f, oy = e.shockT > 0f ? (float) Math.cos(time * 151f + e.y) * 3.5f : 0f;
        batch.draw(r, e.x - e.w / 2 + ox, e.y - e.h / 2 + oy, e.w, e.h);
        batch.setColor(1, 1, 1, 1);
    }

    public void dispose() {
        if (glowTex != null) glowTex.dispose();
        for (Texture[] row : tex) for (Texture t : row) if (t != null) t.dispose();
        for (Texture t : sheetTex) if (t != null) t.dispose();
        for (Texture t : sheetVTex) if (t != null) t.dispose();
    }
}

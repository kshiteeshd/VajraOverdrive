package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.world.TileMap;

/** Walls draw the user's 16 hand-drawn Wall_Tiles as a DUAL-GRID: one tile per grid VERTEX,
 *  picked by the 2 x 2 cell pattern around that vertex (NW,NE,SW,SE) and drawn centred on the
 *  vertex at 32 px. pattern = (NW?8)|(NE?4)|(SW?2)|(SE?1) indexes P2FILE → tile_XX. Every tile
 *  border is the same half-split of the two cells it passes through, so neighbouring vertex
 *  tiles always meet (tools/fix_wall_tiles.py decodes the table and verifies every seam).
 *  Tiles draw at their NATIVE colours, no tint: the art itself carries gold trim and dark
 *  depth, the all-sky pattern draws their empty tile. A missing file falls back to flat
 *  squares, never a crash. */
public class TileImages {
    private static final int S = 16;
    private static final String DIR = "atala/walls/Wall_Tiles/tile_%02d.png";

    /** dual-grid pattern -> tile file index (derived in tools/fix_wall_tiles.py). */
    private static final int[] P2FILE = { 12, 13, 0, 3, 8, 1, 14, 5, 15, 4, 11, 2, 9, 10, 7, 6 };

    private final Texture[] tile = new Texture[16];  // Atala's ivory; null entries when the folder is missing
    private final Texture[] tileB = new Texture[16]; // Vitala's basalt: the same sixteen tiles re-coloured
    /** The palette the shrine and shop painters are painting in right now (0 Atala, 1 Vitala): see {@link #skin}. */
    static int paintSkin;
    private final TextureRegion[] flat = new TextureRegion[4]; // fallback squares per id

    /** Both palettes at once: the campaign world has Atala's ivory walls and, on their roof, VITALA's black basalt with molten-gold seams (the
     *  shapes and seams are the art's own); each map says which it wears ({@link TileMap#skin}). */
    public TileImages() {
        try {
            for (int i = 0; i < 16; i++) {
                Texture t = ImageKit.texture(String.format(DIR, i), true);
                if (t == null) throw new java.io.FileNotFoundException();
                t.dispose();
                tile[i] = jewel(String.format(DIR, i));
                if (tile[i] == null) throw new java.io.FileNotFoundException();
                tileB[i] = basalt(String.format(DIR, i));
            }
        } catch (Throwable ignored) {
            for (int i = 0; i < 16; i++) {
                if (tile[i] != null) {
                    tile[i].dispose();
                    tile[i] = null;
                }
                if (tileB[i] != null) {
                    tileB[i].dispose();
                    tileB[i] = null;
                }
            }
            flat[TileMap.ROCK] = flatTex(0xE8E6E0FF);
            flat[TileMap.GOLD] = flatTex(0xFFD700FF);
            flat[TileMap.DARK] = flatTex(0x1A1A1EFF);
        }
    }

    /** One wall tile in Vitala's palette: the ivory body goes to charcoal basalt with a warm undertone (its own light and dark kept), the gold
     *  trim becomes molten gold, a touch hotter. Null when the file is missing. */
    private static Texture basalt(String path) {
        if (!Gdx.files.internal(path).exists()) return null;
        Pixmap p = new Pixmap(Gdx.files.internal(path));
        toBasalt(p);
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        p.dispose();
        return t;
    }

    /** One wall tile of ATALA: the ivory art read as pale stone in a sunless hall lit by jewels (cool, quiet, a little darker), its gold trim worn to old bronze. */
    private static Texture jewel(String path) {
        if (!Gdx.files.internal(path).exists()) return null;
        Pixmap p = new Pixmap(Gdx.files.internal(path));
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < p.getHeight(); y++)
            for (int x = 0; x < p.getWidth(); x++) {
                int px = p.getPixel(x, y);
                float r = ((px >>> 24) & 0xff) / 255f, g = ((px >>> 16) & 0xff) / 255f, b = ((px >>> 8) & 0xff) / 255f;
                int a = px & 0xff;
                if (a == 0) continue;
                float lum = 0.299f * r + 0.587f * g + 0.114f * b;
                boolean gold = r > 0.5f && r - b > 0.28f && r > g * 1.05f;
                float or, og, ob;
                if (gold) {
                    float k = 0.42f + 0.40f * lum;
                    or = Math.min(1f, 0.80f * k + 0.08f);
                    og = Math.min(1f, 0.62f * k + 0.07f);
                    ob = Math.min(1f, 0.34f * k + 0.06f);
                } else {
                    float base = (float) Math.pow(lum, 1.12) * 0.50f + 0.06f;
                    or = Math.min(1f, base * 0.90f);
                    og = Math.min(1f, base * 0.97f);
                    ob = Math.min(1f, base * 1.10f);
                }
                p.drawPixel(x, y, ((int) (or * 255f) << 24) | ((int) (og * 255f) << 16) | ((int) (ob * 255f) << 8) | a);
            }
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        p.dispose();
        return t;
    }

    /** In Vitala the shrines and the assay bays are painted in the same palette (the painters call this before they upload).*/
    static void skin(Pixmap p) {
        if (paintSkin == 1) toBasalt(p);
    }

    /** Re-colour a pixmap in place into Vitala's palette (also used for the keepers' sheets): ivory to basalt, gold to molten gold. */
    static void toBasalt(Pixmap p) {
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < p.getHeight(); y++)
            for (int x = 0; x < p.getWidth(); x++) {
                int px = p.getPixel(x, y);
                float r = ((px >>> 24) & 0xff) / 255f, g = ((px >>> 16) & 0xff) / 255f, b = ((px >>> 8) & 0xff) / 255f;
                int a = px & 0xff;
                if (a == 0) continue;
                float lum = 0.299f * r + 0.587f * g + 0.114f * b;
                boolean gold = r > 0.5f && r - b > 0.28f && r > g * 1.05f;
                float or, og, ob;
                if (gold) { // a dull ember seam: the trim keeps its place but no longer shouts (the river is the loka's one gold)
                    float k = 0.38f + 0.34f * lum;
                    or = Math.min(1f, r * k + 0.05f);
                    og = Math.min(1f, g * 0.55f * k);
                    ob = Math.min(1f, b * 0.22f * k);
                } else { // ash basalt, warm in its shadows, with a little rim so it still reads on a black floor
                    float base = (float) Math.pow(lum, 1.15) * 0.25f + 0.04f;
                    or = Math.min(1f, base * 1.08f);
                    og = Math.min(1f, base * 0.94f);
                    ob = Math.min(1f, base * 0.92f);
                }
                p.drawPixel(x, y, ((int) (or * 255f) << 24) | ((int) (og * 255f) << 16) | ((int) (ob * 255f) << 8) | a);
            }
    }

    private static TextureRegion flatTex(int rgba) {
        Pixmap p = new Pixmap(S, S, Pixmap.Format.RGBA8888);
        p.setColor(rgba);
        p.fill();
        Texture t = new Texture(p);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        p.dispose();
        return new TextureRegion(t);
    }

    /** Draws every vertex tile touching the camera bounds. Art is 16px, drawn at 32px. */
    public void draw(SpriteBatch batch, TileMap m, float l, float b, float r, float t) {
        draw(batch, m, l, b, r, t, 0, 0, 1f, 1f, 1f, 1f);
    }

    /** The walls in a tint (the layered test area draws the same walls further back, darker and smaller). */
    public void drawTinted(SpriteBatch batch, TileMap m, float l, float b, float r, float t, float cr, float cg, float cb) {
        draw(batch, m, l, b, r, t, 0, 0, cr, cg, cb, 1f);
    }

    /** ... and with an alpha (the farthest layer of a stack fades in). */
    public void drawTinted(SpriteBatch batch, TileMap m, float l, float b, float r, float t, float cr, float cg, float cb, float ca) {
        draw(batch, m, l, b, r, t, 0, 0, cr, cg, cb, ca);
    }

    /** A dark copy of the walls thrown (offX, offY) px from them at `alpha`: a thin one is the wall's SIDE FACE (it reads as thick), a
     *  wide soft one its shadow on whatever lies behind. */
    public void drawDark(SpriteBatch batch, TileMap m, float l, float b, float r, float t, float offX, float offY, float alpha) {
        draw(batch, m, l, b, r, t, offX, offY, 0f, 0.01f, 0.03f, alpha);
    }

    /** The walls' shadow on the ground: the same tiles, black, thrown down and to the right (they are raised ground). */
    public void drawShadow(SpriteBatch batch, TileMap m, float l, float b, float r, float t) {
        draw(batch, m, l, b, r, t, 9, -12, 0f, 0.01f, 0.03f, 0.34f);
    }

    /** What to do with MIRAGE tiles: draw them as usual, leave them out (the baked chunks), or draw nothing else (the live pass over them). */
    static final int GHOST_ALL = 0, GHOST_SKIP = 1, GHOST_ONLY = 2;
    private int ghostMode = GHOST_ALL;
    private SpriteBatch chunkBatch;

    void setGhostMode(int mode) { ghostMode = mode; }

    /** A batch of its own for baking chunks (so the game's batch is never closed under a caller). */
    SpriteBatch batchForChunks() {
        if (chunkBatch == null) chunkBatch = new SpriteBatch(1024);
        return chunkBatch;
    }

    private float hullX = -1e9f, hullY = -1e9f, clock;

    /** Where the hull is and what time it is, so a mirage can thin out as the hull nears and shimmer while it is far. */
    public void setHull(float x, float y, float time) {
        hullX = x;
        hullY = y;
        clock = time;
    }

    /** A mirage's strength at a distance from the hull: whole from afar, nearly gone within about 110 px. */
    static float mirageFade(float d) {
        return d >= 320f ? 1f : d <= 110f ? 0.08f : 0.08f + 0.92f * (d - 110f) / 210f;
    }

    private void draw(SpriteBatch batch, TileMap m, float l, float b, float r, float t,
                      float offX, float offY, float cr, float cg, float cb, float ca) {
        l -= offX;
        r -= offX;
        b -= offY;
        t -= offY;
        float T = TileMap.TILE;
        int vx0 = Math.max(0, (int) Math.ceil((l - m.ox) / T - 0.5f));
        int vx1 = Math.min(m.cols, (int) Math.floor((r - m.ox) / T + 0.4999f));
        int vy0 = Math.max(0, (int) Math.ceil((b - m.oy) / T - 0.5f));
        int vy1 = Math.min(m.rows, (int) Math.floor((t - m.oy) / T + 0.4999f));
        batch.setColor(cr, cg, cb, ca); // native art colours, untinted (a shadow pass tints them black)
        final Texture[] tile = m.skin == 1 ? this.tileB : this.tile;
        for (int vy = vy0; vy <= vy1; vy++) {
            for (int vx = vx0; vx <= vx1; vx++) {
                int a = m.raw(vx - 1, vy), n = m.raw(vx, vy);
                int c = m.raw(vx - 1, vy - 1), d = m.raw(vx, vy - 1);
                boolean mirage = false;
                if (m.hasGhosts()) { // a vertex is a mirage when every wall cell round it is one
                    int real = 0, ghosts = 0;
                    if (a != TileMap.EMPTY) { if (m.isGhost(vx - 1, vy)) ghosts++; else real++; }
                    if (n != TileMap.EMPTY) { if (m.isGhost(vx, vy)) ghosts++; else real++; }
                    if (c != TileMap.EMPTY) { if (m.isGhost(vx - 1, vy - 1)) ghosts++; else real++; }
                    if (d != TileMap.EMPTY) { if (m.isGhost(vx, vy - 1)) ghosts++; else real++; }
                    mirage = ghosts > 0 && real == 0;
                }
                if (ghostMode == GHOST_SKIP ? mirage : ghostMode == GHOST_ONLY && !mirage) continue;
                int p = (a != TileMap.EMPTY ? 8 : 0) | (n != TileMap.EMPTY ? 4 : 0)
                        | (c != TileMap.EMPTY ? 2 : 0) | (d != TileMap.EMPTY ? 1 : 0);
                int id = TileMap.ROCK;
                if (a == TileMap.GOLD || n == TileMap.GOLD || c == TileMap.GOLD || d == TileMap.GOLD)
                    id = TileMap.GOLD;
                else if (a == TileMap.DARK || n == TileMap.DARK || c == TileMap.DARK || d == TileMap.DARK)
                    id = TileMap.DARK;
                float x = m.ox + vx * T - T / 2 + offX, y = m.oy + vy * T - T / 2 + offY;
                if (mirage) { // shimmering lavender rock that thins out as the hull nears
                    float dx = m.ox + vx * T - hullX, dy = m.oy + vy * T - hullY;
                    float f = mirageFade((float) Math.sqrt(dx * dx + dy * dy));
                    float shimmer = 0.9f + 0.1f * (float) Math.sin(clock * 3f + vx * 0.7f + vy * 0.5f);
                    batch.setColor(cr * 0.93f, cg * 0.9f, cb, ca * f * shimmer);
                }
                if (tile[P2FILE[p]] == null) {
                    if (p != 0) batch.draw(flat[id], x, y, T, T);
                } else batch.draw(tile[P2FILE[p]], x, y, T, T);
                if (mirage) batch.setColor(cr, cg, cb, ca);
            }
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        if (chunkBatch != null) chunkBatch.dispose();
        for (Texture t : tile) if (t != null) t.dispose();
        for (Texture t : tileB) if (t != null) t.dispose();
        for (TextureRegion r : flat) if (r != null) r.getTexture().dispose();
    }
}

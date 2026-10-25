package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import vajra.world.TileMap;
import vajra.world.World;

/** THE WALLS, BAKED. A layer of the stack used to draw every wall tile of the view three times (a shadow, a side face, the tile itself):
 *  thousands of sprites a layer, ten layers over. Walls do not change, so each 512 px square of the world is painted ONCE into a texture
 *  (its soft shadow, its thin side face and the tiles, ghosts left out) and a layer is then a handful of textured quads, tinted and faded
 *  by the batch's colour. The cache is an LRU of 96 chunks (about 96 MB; the pulled-back camera of an astra shows about fifty at once), baked on demand a few a frame; a frame that is missing a
 *  chunk falls back to the per-tile path, so nothing ever pops. Chunks are stored premultiplied, so a baked shadow keeps its edge. */
final class TileChunks {
    static final int CS = 512, KEEP = 96;

    private static final class Chunk {
        FrameBuffer fb;
        int version = -1;
    }

    private final TileImages tiles;
    private final OrthographicCamera cam = new OrthographicCamera(CS, CS);
    private final LinkedHashMap<Long, Chunk> chunks = new LinkedHashMap<>(96, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Chunk> eldest) {
            if (size() <= KEEP) return false;
            eldest.getValue().fb.dispose();
            return true;
        }
    };

    TileChunks(TileImages tiles) { this.tiles = tiles; }

    private static long key(int cx, int cy) { return ((long) cx << 32) ^ (cy & 0xFFFFFFFFL); }

    /** Make sure every chunk touching [l, r] x [b, t] is baked at this wall version, baking up to `budget` of the missing ones (the batch
     *  must NOT be open). True when the whole rectangle is covered. The caller restores its own viewport afterwards if this returns after
     *  baking (it can tell: see {@link #baked}). */
    boolean ensure(World world, float l, float b, float r, float t, int budget) {
        baked = 0;
        boolean missing = false;
        int cx0 = (int) Math.floor(l / CS), cx1 = (int) Math.floor(r / CS), cy0 = (int) Math.floor(b / CS), cy1 = (int) Math.floor(t / CS);
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                if (cx * CS > world.width + CS || cx < -1 || cy < -1 || cy * CS > world.height + CS) continue;   // outside the world: nothing to bake
                Chunk c = chunks.get(key(cx, cy));
                if (c != null && c.version == world.wallVersion) continue;
                if (budget <= 0) { missing = true; continue; }
                bake(world, cx, cy, c);
                budget--;
                baked++;
            }
        return !missing;
    }

    /** How many chunks the last {@link #ensure} baked (so the caller knows the viewport needs restoring). */
    int baked;

    private void bake(World world, int cx, int cy, Chunk c) {
        if (c == null) {
            c = new Chunk();
            c.fb = new FrameBuffer(Pixmap.Format.RGBA8888, CS, CS, false);
            c.fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            chunks.put(key(cx, cy), c);
        }
        c.version = world.wallVersion;
        c.fb.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.setToOrtho(false, CS, CS);
        cam.position.set(cx * CS + CS / 2f, cy * CS + CS / 2f, 0f);
        cam.update();
        SpriteBatch batch = tiles.batchForChunks();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        // premultiplied output: colour is accumulated already multiplied by its alpha, alpha accumulates correctly
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float l = cx * CS - 40f, b = cy * CS - 40f, r = cx * CS + CS + 40f, t = cy * CS + CS + 40f;
        tiles.setGhostMode(TileImages.GHOST_SKIP);
        for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, r, t, 9f, -12f, 0.34f);   // the soft shadow on whatever lies behind
        for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, r, t, 3f, -4f, 0.55f);    // the side face: a wall is thick
        for (TileMap m : world.tileMaps()) tiles.draw(batch, m, l, b, r, t);
        tiles.setGhostMode(TileImages.GHOST_ALL);
        batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        c.fb.end();
    }

    /** Draw the chunks over [l, r] x [b, t] into an open batch, tinted (cr, cg, cb) and faded by ca. */
    void draw(SpriteBatch batch, float l, float b, float r, float t, float cr, float cg, float cb, float ca) {
        int cx0 = (int) Math.floor(l / CS), cx1 = (int) Math.floor(r / CS), cy0 = (int) Math.floor(b / CS), cy1 = (int) Math.floor(t / CS);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);                 // the chunks are premultiplied
        batch.setColor(cr * ca, cg * ca, cb * ca, ca);
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                Chunk c = chunks.get(key(cx, cy));
                if (c == null) continue;
                batch.draw(c.fb.getColorBufferTexture(), cx * CS, cy * CS, CS, CS, 0, 0, 1, 1);
            }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    void dispose() {
        for (Chunk c : chunks.values()) c.fb.dispose();
        chunks.clear();
    }
}

package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
    /** Each chunk is baked with this margin all round (so the softening below can see across a chunk's edge) and only its inside is drawn. */
    static final int M = 24, W = CS + 2 * M;

    /** THE SOFT EDGE: the tiles' silhouette is a grid of 32 px steps. This pass blurs the solid part of a baked chunk and cuts it again at the middle of the blur, which rounds every corner and
     *  melts every stair into a smooth, organic contour (the stone's own colour is carried into the new edge); a thin light is added along the edges that face the light, and the soft shadows
     *  the tiles baked outside the rock are left as they were. */
    private static final String VERT = "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\nuniform mat4 u_projTrans;\nvarying vec2 v_tc;\n"
            + "void main(){ v_tc = a_texCoord0; gl_Position = u_projTrans * a_position; }\n";
    private static final String FRAG = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
            + "varying vec2 v_tc;\nuniform sampler2D u_texture;\nuniform vec2 u_px;\n"
            + "vec4 T(vec2 o){ return texture2D(u_texture, v_tc + o * u_px); }\n"
            + "void main(){\n"
            + "  vec4 c = T(vec2(0.0));\n"
            + "  float sb = 0.0, ws = 0.0; vec3 cb = vec3(0.0);\n"
            + "  for (int j = -4; j <= 4; j++) for (int i = -4; i <= 4; i++) {\n"
            + "    vec2 o = vec2(float(i), float(j)) * 5.0;\n"
            + "    float w = exp(-dot(o, o) / 170.0);\n"
            + "    vec4 t = T(o); float m = step(0.9, t.a);\n"
            + "    sb += m * w; ws += w; cb += (t.rgb / max(t.a, 0.001)) * m * w;\n"
            + "  }\n"
            + "  sb /= ws; vec3 col = cb / max(sb * ws, 0.0001);\n"
            + "  float sa = smoothstep(0.40, 0.60, sb);\n"
            + "  float nb = (step(0.9, T(vec2(-9.0, 9.0)).a) + step(0.9, T(vec2(-13.0, 6.0)).a) + step(0.9, T(vec2(-6.0, 13.0)).a)) / 3.0;\n"
            + "  float rim = sa * (1.0 - nb);\n"
            + "  vec4 shadow = c.a < 0.9 ? c : vec4(0.0, 0.0, 0.01, 0.30);\n"
            + "  col *= 1.0 + 0.45 * rim;\n"
            + "  gl_FragColor = vec4(col * sa, sa) + shadow * (1.0 - sa);\n"
            + "}\n";
    private ShaderProgram soften;
    private boolean softenFailed;
    private FrameBuffer temp;
    private SpriteBatch smoothBatch;

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
            c.fb = new FrameBuffer(Pixmap.Format.RGBA8888, W, W, false);
            c.fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            chunks.put(key(cx, cy), c);
        }
        c.version = world.wallVersion;
        ShaderProgram sh = shader();
        FrameBuffer target = sh != null ? temp() : c.fb;
        target.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.setToOrtho(false, W, W);
        cam.position.set(cx * CS + CS / 2f, cy * CS + CS / 2f, 0f);
        cam.update();
        SpriteBatch batch = tiles.batchForChunks();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        // premultiplied output: colour is accumulated already multiplied by its alpha, alpha accumulates correctly
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float l = cx * CS - M - 40f, b = cy * CS - M - 40f, r = cx * CS + CS + M + 40f, t = cy * CS + CS + M + 40f;
        tiles.setGhostMode(TileImages.GHOST_SKIP);
        for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, r, t, 9f, -12f, 0.34f);   // the soft shadow on whatever lies behind
        for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, r, t, 3f, -4f, 0.55f);    // the side face: a wall is thick
        for (TileMap m : world.tileMaps()) tiles.draw(batch, m, l, b, r, t);
        tiles.setGhostMode(TileImages.GHOST_ALL);
        batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        target.end();
        if (sh != null) {                                                                    // the soft pass: the baked tiles through the shader, into the chunk
            c.fb.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            if (smoothBatch == null) smoothBatch = new SpriteBatch(4);
            smoothBatch.getProjectionMatrix().setToOrtho2D(0, 0, W, W);
            smoothBatch.setShader(sh);
            smoothBatch.begin();
            smoothBatch.disableBlending();
            sh.setUniformf("u_px", 1f / W, 1f / W);
            smoothBatch.setColor(1f, 1f, 1f, 1f);
            smoothBatch.draw(temp.getColorBufferTexture(), 0, 0, W, W, 0, 0, 1, 1);
            smoothBatch.end();
            c.fb.end();
        }
    }

    private FrameBuffer temp() {
        if (temp == null) {
            temp = new FrameBuffer(Pixmap.Format.RGBA8888, W, W, false);
            temp.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        return temp;
    }

    /** The soft-edge shader, or null when this GPU will not compile it (the walls are then drawn as the tiles made them). */
    private ShaderProgram shader() {
        if (softenFailed) return null;
        if (soften == null) {
            try {
                ShaderProgram.pedantic = false;
                soften = new ShaderProgram(VERT, FRAG);
                if (!soften.isCompiled()) throw new IllegalStateException(soften.getLog());
            } catch (Throwable ex) {
                softenFailed = true;
                Gdx.app.error("TileChunks", "the soft wall edge is off: " + ex);
                return null;
            }
        }
        return soften;
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
                batch.draw(c.fb.getColorBufferTexture(), cx * CS, cy * CS, CS, CS, (float) M / W, (float) M / W, (float) (M + CS) / W, (float) (M + CS) / W);
            }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    void dispose() {
        if (temp != null) temp.dispose();
        if (soften != null) soften.dispose();
        if (smoothBatch != null) smoothBatch.dispose();
        for (Chunk c : chunks.values()) c.fb.dispose();
        chunks.clear();
    }
}

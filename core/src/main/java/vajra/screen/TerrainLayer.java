package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import vajra.world.GroundMap;
import vajra.world.PropField;
import vajra.world.TerrainBake;
import vajra.world.World;

/** THE RAYCASTER. The land is one height map (vajra.world.TerrainBake: colour and a height in cut planes per texel) and every
 *  pixel of the view marches a ray down through it, from the camera hanging over the flight plane, to the first plane it
 *  strikes. That gives parallax that is exact (a tall thing is displaced by its height, a near thing moves faster than a far
 *  one), occlusion (a ridge hides what is behind it), vertical sides shaded by the slope the ray finds, and shadows thrown
 *  toward a light up-left - from flat painted planes, with no models in it.
 *
 *  <p>Two passes: A draws everything up to the flight plane (under the hull); B draws only what stands ABOVE it (pillars,
 *  crests), translucent, over the hull. The bake runs on a thread; until it lands, the caller keeps its flat ground. */
final class TerrainLayer {
    private static final String VERT = "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\nvarying vec2 v_tc;\n"
            + "void main(){ v_tc = a_texCoord0; gl_Position = u_projTrans * a_position; }";

    private static final String FRAG = "#ifdef GL_ES\nprecision highp float;\n#endif\n"
            + "varying vec2 v_tc;\n"
            + "uniform sampler2D u_tex;\n"
            + "uniform vec2 u_invWorld, u_view, u_cam;\n"
            + "uniform float u_fly, u_D, u_pass, u_texel, u_ambient, u_haze;\n"
            + "uniform vec3 u_hazeColor;\n"
            + "float H(vec2 xy){ return texture2D(u_tex, xy * u_invWorld).a * 255.0; }\n"
            + "vec2 at(vec2 u, float z){ return u_cam + u * ((u_D + u_fly - z) / u_D); }\n"
            + "void main(){\n"
            + "  vec2 u = vec2(v_tc.x - 0.5, 0.5 - v_tc.y) * u_view;\n"   // the quad's v runs DOWN the screen; world y runs up
            + "  float zTop = u_pass < 0.5 ? min(u_fly, 255.0) : 255.0;\n"
            + "  float zBot = u_pass < 0.5 ? 0.0 : u_fly;\n"
            + "  if (zTop <= zBot + 0.5) discard;\n"
            + "  float z = zTop, zPrev = zTop;\n"
            + "  bool hit = false;\n"
            + "  for (int i = 0; i < 44; i++) {\n"
            + "    z = mix(zTop, zBot, float(i) / 43.0);\n"
            + "    if (H(at(u, z)) >= z) { hit = true; break; }\n"
            + "    zPrev = z;\n"
            + "  }\n"
            + "  if (!hit) discard;\n"
            + "  float lo = z, hi = zPrev;\n"
            + "  for (int j = 0; j < 5; j++) { float m = 0.5 * (lo + hi); if (H(at(u, m)) >= m) lo = m; else hi = m; }\n"
            + "  z = lo;\n"
            + "  if (u_pass > 0.5 && z <= u_fly + 0.5) discard;\n"
            + "  vec2 xy = at(u, z);\n"
            + "  vec3 col = texture2D(u_tex, xy * u_invWorld).rgb;\n"
            + "  float hl = H(xy - vec2(u_texel, 0.0)), hr = H(xy + vec2(u_texel, 0.0));\n"
            + "  float hd = H(xy - vec2(0.0, u_texel)), hu = H(xy + vec2(0.0, u_texel));\n"
            + "  vec3 n = normalize(vec3((hl - hr) / (2.0 * u_texel), (hd - hu) / (2.0 * u_texel), 1.0));\n"
            + "  vec3 L = normalize(vec3(-0.40, 0.50, 0.95));\n"
            + "  float shade = 0.60 + 0.58 * max(dot(n, L), 0.0);\n"
            + "  float sh = 1.0;\n"
            + "  vec2 ld = normalize(L.xy) * 12.0;\n"
            + "  float lz = (L.z / length(L.xy)) * 12.0;\n"
            + "  vec2 p = xy; float zz = z + 1.5;\n"
            + "  for (int k = 0; k < 12; k++) { p += ld; zz += lz; if (H(p) > zz) { sh = 0.62; break; } }\n"
            + "  col *= shade * sh * (0.84 + 0.16 * clamp(z / 100.0, 0.0, 1.0));\n"
            + "  col = mix(col, u_hazeColor, u_haze * (1.0 - clamp(z / 170.0, 0.0, 1.0)));\n"
            + "  vec2 wsz = 1.0 / u_invWorld;\n"
            + "  if (xy.x < 0.0 || xy.y < 0.0 || xy.x > wsz.x || xy.y > wsz.y) col *= 0.55;\n"   // past the world's edge: mirrored land, darkened
            + "  col *= u_ambient;\n"
            + "  gl_FragColor = vec4(col, u_pass < 0.5 ? 1.0 : 0.93);\n"
            + "}\n";

    private static CompletableFuture<TerrainBake> shared;
    private static int sharedKey;

    private final CompletableFuture<TerrainBake> bake;
    private final int cols, rows;
    private ShaderProgram shader;
    private Texture tex;
    private TextureRegion white;
    private boolean failed;

    TerrainLayer(World world, GroundMap map, PropField props) {
        int key = (int) world.width * 31 + (int) world.height + java.util.Arrays.hashCode(world.regionStart) + java.util.Arrays.deepHashCode(world.boulders) * 7;
        synchronized (TerrainLayer.class) {
            if (shared == null || sharedKey != key) {
                float w = world.width, h = world.height;
                float[][] boulders = world.boulders;
                shared = CompletableFuture.supplyAsync(() -> new TerrainBake(map, props, boulders, w, h));
                sharedKey = key;
            }
            bake = shared;
        }
        cols = (int) Math.ceil(world.width / TerrainBake.TEXEL);
        rows = (int) Math.ceil(world.height / TerrainBake.TEXEL);
    }

    /** True once the land is baked and uploaded (the shader and texture are made on the first call after the bake lands). */
    boolean ready() {
        if (failed) return false;
        if (tex != null) return true;
        if (!bake.isDone()) return false;
        try {
            TerrainBake b = bake.join();
            java.nio.IntBuffer maxTex = com.badlogic.gdx.utils.BufferUtils.newIntBuffer(16);
            Gdx.gl.glGetIntegerv(com.badlogic.gdx.graphics.GL20.GL_MAX_TEXTURE_SIZE, maxTex);
            if (maxTex.get(0) > 0 && Math.max(b.cols, b.rows) > maxTex.get(0))        // the campaign world's land is 1300 x 11900 texels: a very old GPU cannot hold it
                throw new IllegalStateException("the land is " + b.cols + " x " + b.rows + " texels and this GPU's largest texture is " + maxTex.get(0));
            Pixmap pm = new Pixmap(b.cols, b.rows, Pixmap.Format.RGBA8888);
            ByteBuffer buf = pm.getPixels();
            buf.clear();
            buf.put(b.rgba);
            buf.rewind();
            tex = new Texture(pm);
            pm.dispose();
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            tex.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.MirroredRepeat); // past an edge the land mirrors, never smears
            ShaderProgram.pedantic = false;
            shader = new ShaderProgram(VERT, FRAG);
            if (!shader.isCompiled()) throw new IllegalStateException(shader.getLog());
            Pixmap one = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
            one.setColor(1, 1, 1, 1);
            one.fill();
            white = new TextureRegion(new Texture(one));
            one.dispose();
            return true;
        } catch (Throwable t) {
            failed = true;
            Gdx.app.error("TerrainLayer", "the raycaster is off, the flat ground stays: " + t);
            return false;
        }
    }

    /** Block until the bake has landed (tests and screenshots; the game never waits). */
    boolean finishLoading() {
        bake.join();
        return ready();
    }

    /** One pass over the view. `cam` is the flight camera; `fly` the plane's height; pass 0 under the hull, 1 above it. */
    void draw(SpriteBatch batch, OrthographicCamera cam, float fly, int pass, float ambient, float[] haze, float hazeAmt) {
        float w = cam.viewportWidth * cam.zoom, h = cam.viewportHeight * cam.zoom;
        batch.setProjectionMatrix(cam.combined);
        batch.setShader(shader);
        batch.begin();
        tex.bind(1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        shader.setUniformi("u_tex", 1);
        shader.setUniformf("u_invWorld", 1f / (cols * TerrainBake.TEXEL), 1f / (rows * TerrainBake.TEXEL));
        shader.setUniformf("u_view", w, h);
        shader.setUniformf("u_cam", cam.position.x, cam.position.y);
        shader.setUniformf("u_fly", fly);
        shader.setUniformf("u_D", Altitude.D);
        shader.setUniformf("u_pass", pass);
        shader.setUniformf("u_texel", TerrainBake.TEXEL);
        shader.setUniformf("u_ambient", ambient);
        shader.setUniformf("u_haze", hazeAmt);
        shader.setUniformf("u_hazeColor", haze[0], haze[1], haze[2]);
        if (pass == 0) batch.disableBlending();
        batch.draw(white, cam.position.x - w / 2, cam.position.y - h / 2, w, h);
        batch.end();
        if (pass == 0) batch.enableBlending();
        batch.setShader(null);
    }

    private FrameBuffer half;

    /** The floor under a deep stack, raycast at HALF the resolution (a quarter of the pixels, the cost of the pass falling the same way) into
     *  a buffer and stretched over the view: it is dark and hazy down there, and the stretch is a soft focus nobody can tell from depth.
     *  `restore` puts the caller's viewport back after the buffer pass, before the stretch. */
    void drawHalf(SpriteBatch batch, OrthographicCamera cam, float fly, float ambient, float[] haze, float hazeAmt, Runnable restore) {
        int fw = Math.max(1, Gdx.graphics.getBackBufferWidth() / 2), fh = Math.max(1, Gdx.graphics.getBackBufferHeight() / 2);
        if (half == null || half.getWidth() != fw || half.getHeight() != fh) {
            if (half != null) half.dispose();
            half = new FrameBuffer(Pixmap.Format.RGBA8888, fw, fh, false);
            half.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        half.begin();
        draw(batch, cam, fly, 0, ambient, haze, hazeAmt);
        half.end();
        restore.run();
        float w = cam.viewportWidth * cam.zoom, h = cam.viewportHeight * cam.zoom;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.disableBlending();
        batch.draw(half.getColorBufferTexture(), cam.position.x - w / 2, cam.position.y - h / 2, w, h, 0, 0, 1, 1);
        batch.end();
        batch.enableBlending();
    }

    void dispose() {
        if (half != null) half.dispose();
        if (tex != null) tex.dispose();
        if (shader != null) shader.dispose();
        if (white != null) white.getTexture().dispose();
    }
}

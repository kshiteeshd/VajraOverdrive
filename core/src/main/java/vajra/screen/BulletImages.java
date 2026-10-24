package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import vajra.enemy.Enemy;
import vajra.weapon.Bullet;
import vajra.weapon.WeaponDef;

/** Bullets, with their own art at their own size, run through a NEGATIVE FILTER: wherever the sprite is opaque the screen
 *  behind it is inverted (`result = lerp(behind, 1 - behind, alpha)`, the sprite's own light and dark riding on top), so a
 *  bullet is bright over dark ground, dark over light ground, magenta over green, lime over violet - the opposite of whatever
 *  is behind it.
 *
 *  <p>It must be drawn ONCE, over the finished frame (the negative of a negative is the ground again), so the world's own
 *  pass no longer draws bullets; the foreground pass does. A missing sprite file becomes a disc, never a crash. */
public class BulletImages {
    private final Map<WeaponDef, TextureRegion> masks = new IdentityHashMap<>();
    private final TextureRegion foe;
    private final TextureRegion disc;                 // the fallback silhouette
    private final ArrayList<Texture> loaded = new ArrayList<>();

    // THE FILTER: a shader that reads the finished frame behind each bullet and paints the bullet's silhouette in its negative,
    // pushed away from mid-grey so it still reads over mid-tones (the plain negative of grey is grey).
    private static final String VERT =
            "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec2 v_tc;\n" +
            "void main(){ v_tc = a_texCoord0; gl_Position = u_projTrans * a_position; }\n";
    private static final String FRAG =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 v_tc;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform sampler2D u_grab;\n" +
            "uniform vec2 u_screen;\n" +
            "void main(){\n" +
            "  vec4 m = texture2D(u_texture, v_tc);\n" +
            "  vec3 bg = texture2D(u_grab, gl_FragCoord.xy / u_screen).rgb;\n" +
            "  vec3 neg = clamp((1.0 - bg - 0.5) * 1.8 + 0.5, 0.0, 1.0);\n" +
            "  float k = m.a > 0.001 ? m.r / m.a : 0.0;\n" +
            "  gl_FragColor = vec4(neg * (0.62 + 0.38 * k), m.a);\n" +
            "}\n";
    private ShaderProgram filter;
    private boolean filterFailed;
    private Texture grab;
    private int gw, gh;
    private boolean filtering;

    public BulletImages() {
        disc = discMask();
        for (WeaponDef d : WeaponDef.ALL) {
            if (d.beam) continue; // lasers draw as a line, they never spawn a sprite
            masks.put(d, load(artPath(d.name)));
        }
        for (WeaponDef d : Enemy.GUN) // enemy signatures: per-kind art
            if (d != null && !d.beam) masks.put(d, load(artPath(d.name)));
        foe = load("atala/bullets/enemy.png");
    }

    /** "Single Bolt" -> "atala/bullets/single-bolt.png" — weapon names are the art key. */
    public static String artPath(String weaponName) {
        return "atala/bullets/" + weaponName.toLowerCase().replace(' ', '-') + ".png";
    }

    /** The sprite's silhouette as a premultiplied white mask (rgb = alpha), or a disc if the file is missing. */
    private TextureRegion load(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Pixmap src = new Pixmap(Gdx.files.internal(path));
                Pixmap m = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);
                m.setBlending(Pixmap.Blending.None);
                for (int y = 0; y < src.getHeight(); y++)
                    for (int x = 0; x < src.getWidth(); x++) {
                        int px = src.getPixel(x, y);
                        float a = (px & 0xFF) / 255f;
                        float lum = (((px >>> 24) & 0xFF) * 0.3f + ((px >>> 16) & 0xFF) * 0.59f + ((px >>> 8) & 0xFF) * 0.11f) / 255f;
                        float k = a * (0.55f + 0.45f * lum);               // the art's own light and dark ride on the negative
                        m.setColor(k, k, k, a);
                        m.drawPixel(x, y);
                    }
                src.dispose();
                Texture t = new Texture(m);
                m.dispose();
                t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                loaded.add(t);
                return new TextureRegion(t);
            }
        } catch (Throwable ignored) {
            // fall through to the disc
        }
        return disc;
    }

    private TextureRegion discMask() {
        Pixmap m = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        m.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                float d = (float) Math.hypot(x - 7.5f, y - 7.5f) / 7f, a = Math.max(0f, Math.min(1f, (1f - d) * 3f));
                m.setColor(a, a, a, a);
                m.drawPixel(x, y);
            }
        Texture t = new Texture(m);
        m.dispose();
        t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        loaded.add(t);
        return new TextureRegion(t);
    }

    /** Start a run of bullets: the frame drawn so far is what they are the negative of. Call once, before the first draw, with the
     *  batch open; pair with {@link #end}. */
    public void begin(SpriteBatch batch) {
        filtering = false;
        int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        if (filterFailed || w <= 0 || h <= 0) return;
        try {
            if (filter == null) {
                ShaderProgram.pedantic = false;
                filter = new ShaderProgram(VERT, FRAG);
                if (!filter.isCompiled()) throw new IllegalStateException(filter.getLog());
            }
            if (grab == null || gw != w || gh != h) {
                if (grab != null) grab.dispose();
                gw = w;
                gh = h;
                Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                grab = new Texture(p);
                p.dispose();
                grab.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            }
            batch.flush();                                     // everything behind the bullets is on screen now
            grab.bind(1);
            Gdx.gl.glCopyTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, 0, 0, w, h, 0);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            batch.setShader(filter);
            filter.setUniformi("u_grab", 1);
            filter.setUniformf("u_screen", w, h);
            filtering = true;
        } catch (Throwable t) {
            filterFailed = true;
            Gdx.app.error("BulletImages", "the negative filter is off, the fixed-function negative stays: " + t);
        }
    }

    /** End the run begun by {@link #begin}. */
    public void end(SpriteBatch batch) {
        if (filtering) batch.setShader(null);
        filtering = false;
    }

    /** Sized to the bullet's hit box, turned along its heading: a dark outline, then the negative. */
    public void draw(SpriteBatch batch, Bullet b) {
        TextureRegion reg = masks.get(b.def);
        if (reg == null) reg = b.hostile ? foe : disc;
        float w = b.radius * 2;
        float deg = (float) Math.toDegrees(Math.atan2(b.vy, b.vx));
        batch.setColor(1f, 1f, 1f, 1f);
        if (filtering) {
            batch.draw(reg, b.x - w / 2, b.y - w / 2, w / 2, w / 2, w, w, 1, 1, deg);
            return;
        }
        batch.setBlendFunction(GL20.GL_ONE_MINUS_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA);   // (no shader: the plain negative)
        batch.draw(reg, b.x - w / 2, b.y - w / 2, w / 2, w / 2, w, w, 1, 1, deg);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        for (Texture t : loaded) t.dispose();
        if (grab != null) grab.dispose();
        if (filter != null) filter.dispose();
    }
}

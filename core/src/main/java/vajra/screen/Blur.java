package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/** Blurs whatever is on screen: grabs the window's pixels, runs a separable gaussian over a quarter-size copy and
 *  draws the result back over the whole window. Used behind the weapon wheel. Never nests an FBO inside another:
 *  call {@link #render} at the top level, after the world has drawn and before the HUD. A failure (no shader, a 0x0
 *  window) just leaves the frame sharp. */
final class Blur {
    private static final int DOWN = 4;
    private static final String VERT = "attribute vec4 a_position;\nattribute vec4 a_color;\nattribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\nvarying vec4 v_color;\nvarying vec2 v_tc;\n"
            + "void main(){ v_color = a_color; v_tc = a_texCoord0; gl_Position = u_projTrans * a_position; }";
    private static final String FRAG = "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
            + "varying vec4 v_color;\nvarying vec2 v_tc;\nuniform sampler2D u_texture;\nuniform vec2 u_dir;\n"
            + "void main(){\n"
            + "  vec4 c = texture2D(u_texture, v_tc) * 0.2270270270;\n"
            + "  c += texture2D(u_texture, v_tc + u_dir * 1.3846153846) * 0.3162162162;\n"
            + "  c += texture2D(u_texture, v_tc - u_dir * 1.3846153846) * 0.3162162162;\n"
            + "  c += texture2D(u_texture, v_tc + u_dir * 3.2307692308) * 0.0702702703;\n"
            + "  c += texture2D(u_texture, v_tc - u_dir * 3.2307692308) * 0.0702702703;\n"
            + "  gl_FragColor = vec4(c.rgb, 1.0) * v_color;\n}";

    private ShaderProgram shader;
    private boolean failed;
    private Texture grab;
    private FrameBuffer a, b;
    private int gw, gh;
    private final Matrix4 proj = new Matrix4();

    void render(SpriteBatch batch, float strength) {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (failed || w <= 0 || h <= 0) return;
        try {
            if (shader == null) {
                ShaderProgram.pedantic = false;
                shader = new ShaderProgram(VERT, FRAG);
                if (!shader.isCompiled()) throw new IllegalStateException(shader.getLog());
            }
            if (grab == null || gw != w || gh != h) {
                dispose();
                gw = w;
                gh = h;
                Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                grab = new Texture(p);
                p.dispose();
                grab.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                int sw = Math.max(1, w / DOWN), sh = Math.max(1, h / DOWN);
                a = new FrameBuffer(Pixmap.Format.RGBA8888, sw, sh, false);
                b = new FrameBuffer(Pixmap.Format.RGBA8888, sw, sh, false);
                a.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                b.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
            grab.bind();
            Gdx.gl.glCopyTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, 0, 0, w, h, 0);
            int sw = a.getWidth(), sh = a.getHeight();
            pass(batch, grab, a, sw, sh, strength / sw, 0);                       // horizontal, down to a quarter size
            pass(batch, a.getColorBufferTexture(), b, sw, sh, 0, strength / sh);   // vertical
            pass(batch, b.getColorBufferTexture(), a, sw, sh, strength / sw * 1.6f, 0); // a second, wider pair
            pass(batch, a.getColorBufferTexture(), b, sw, sh, 0, strength / sh * 1.6f);
            Gdx.gl.glViewport(0, 0, w, h);
            proj.setToOrtho2D(0, 0, w, h);
            batch.setProjectionMatrix(proj);
            batch.setShader(null);
            batch.begin();
            batch.disableBlending();
            batch.draw(b.getColorBufferTexture(), 0, 0, w, h, 0, 0, 1, 1);
            batch.enableBlending();
            batch.end();
        } catch (Throwable t) {
            failed = true;
            Gdx.app.error("Blur", "the wheel's blur is off: " + t);
        }
    }

    private void pass(SpriteBatch batch, Texture src, FrameBuffer dst, int sw, int sh, float dx, float dy) {
        dst.begin();
        proj.setToOrtho2D(0, 0, sw, sh);
        batch.setProjectionMatrix(proj);
        batch.setShader(shader);
        batch.begin();
        batch.disableBlending();
        shader.setUniformf("u_dir", dx, dy);
        batch.draw(src, 0, 0, sw, sh, 0, 0, 1, 1);
        batch.end();
        batch.enableBlending();
        batch.setShader(null);
        dst.end();
    }

    void dispose() {
        if (grab != null) grab.dispose();
        if (a != null) a.dispose();
        if (b != null) b.dispose();
        grab = null;
        a = b = null;
    }
}

package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import vajra.world.Maya;
import vajra.world.World;

import java.util.ArrayList;
import java.util.Random;

/** BALA'S BROKEN MIRROR (world/Maya is the clock): the finished frame - world, HUD and all - is copied, then drawn back as GREAT PIECES of glass. Cracks run out from the heart of the
 *  mandala (jagged rays, then a jagged ring), the glass is cut along them, and each piece shows the WHOLE frame turned to its own angle about its own middle (a little nearer, a little
 *  shifted) - straight and parallel before the break, every piece off at its own angle after it. The piece geometry is fixed to the screen; only what it shows turns. */
final class ShatterFx {
    private static final int RAYS = 11, K = 8, RING = 3;
    private static final float[] RADII = {0.06f, 0.12f, 0.20f, 0.30f, 0.42f, 0.58f, 0.80f, 1.25f};   // of the screen's diagonal

    private static final class Piece { float[] px, py; float cx, cy, theta, ox, oy, tint, delay, bx, by, bw, bh; boolean flipU, flipV, swap; }

    private Texture tex, glass;
    private int tw, th;
    private PolygonSpriteBatch pb;
    private ShapeRenderer sr;
    private final ArrayList<Piece> pieces = new ArrayList<>();
    private final float[][] chx = new float[RAYS][K + 1], chy = new float[RAYS][K + 1];
    private boolean built;
    private float fx, fy, diag;
    private long builtSeed;
    private final Vector3 tmp = new Vector3();

    /** Where the mandala is on the glass, until the cracks begin (then it is frozen: the glass does not slide). */
    private void focus(World w, OrthographicCamera camera, Viewport vp, int bw, int bh) {
        tmp.set(w.maya.x, w.maya.y, 0f);
        camera.project(tmp, vp.getScreenX(), vp.getScreenY(), vp.getScreenWidth(), vp.getScreenHeight());
        fx = MathUtils.clamp(tmp.x, bw * 0.12f, bw * 0.88f);
        fy = MathUtils.clamp(tmp.y, bh * 0.15f, bh * 0.85f);
    }

    private void build(Maya m, int bw, int bh) {
        built = true;
        builtSeed = m.seed;
        diag = (float) Math.sqrt((double) bw * bw + (double) bh * bh);
        Random r = new Random(m.seed * 7919L + 13L);
        for (int i = 0; i < RAYS; i++) {
            float a = (i + (r.nextFloat() - 0.5f) * 0.4f) * MathUtils.PI2 / RAYS;
            chx[i][0] = fx;
            chy[i][0] = fy;
            for (int k = 1; k <= K; k++) {
                float ang = a + (r.nextFloat() - 0.5f) * 0.20f, rad = diag * RADII[k - 1] * (0.92f + 0.16f * r.nextFloat());
                chx[i][k] = fx + MathUtils.cos(ang) * rad;
                chy[i][k] = fy + MathUtils.sin(ang) * rad;
            }
        }
        pieces.clear();
        for (int i = 0; i < RAYS; i++) {
            int j = (i + 1) % RAYS;
            ArrayList<float[]> inner = new ArrayList<>(), outer = new ArrayList<>();
            inner.add(new float[] {fx, fy});
            for (int k = 1; k <= RING; k++) inner.add(new float[] {chx[i][k], chy[i][k]});
            for (int k = RING; k >= 1; k--) inner.add(new float[] {chx[j][k], chy[j][k]});
            for (int k = RING; k <= K; k++) outer.add(new float[] {chx[i][k], chy[i][k]});
            for (int k = K; k >= RING; k--) outer.add(new float[] {chx[j][k], chy[j][k]});
            addPiece(inner, bw, bh, r);
            addPiece(outer, bw, bh, r);
        }
    }

    private void addPiece(ArrayList<float[]> poly, int bw, int bh, Random r) {
        ArrayList<float[]> p = clip(clip(clip(clip(poly, 0, 0f, true), 0, bw, false), 1, 0f, true), 1, bh, false);
        if (p.size() < 3) return;
        Piece pc = new Piece();
        pc.px = new float[p.size()];
        pc.py = new float[p.size()];
        float sx = 0f, sy = 0f;
        for (int i = 0; i < p.size(); i++) { pc.px[i] = p.get(i)[0]; pc.py[i] = p.get(i)[1]; sx += pc.px[i]; sy += pc.py[i]; }
        pc.cx = sx / p.size();
        pc.cy = sy / p.size();
        float sign = r.nextBoolean() ? 1f : -1f;
        pc.theta = sign * (0.12f + 0.20f * r.nextFloat());
        pc.ox = (r.nextFloat() - 0.5f) * 70f;
        pc.oy = (r.nextFloat() - 0.5f) * 70f;
        pc.tint = 0.84f + 0.16f * r.nextFloat();                          // (never above 1: a packed colour channel past 255 spills into the alpha and the piece vanishes)
        pc.flipU = r.nextBoolean();
        pc.flipV = r.nextBoolean();
        pc.swap = r.nextBoolean();
        float x0 = 1e9f, x1 = -1e9f, y0 = 1e9f, y1 = -1e9f;
        for (int i = 0; i < pc.px.length; i++) { x0 = Math.min(x0, pc.px[i]); x1 = Math.max(x1, pc.px[i]); y0 = Math.min(y0, pc.py[i]); y1 = Math.max(y1, pc.py[i]); }
        pc.bx = x0; pc.by = y0; pc.bw = Math.max(1f, x1 - x0); pc.bh = Math.max(1f, y1 - y0);
        pc.delay = (float) Math.hypot(pc.cx - fx, pc.cy - fy) / diag * 2.2f;
        pieces.add(pc);
    }

    /** Sutherland-Hodgman against one edge of the screen: axis 0 is x, 1 is y; `keepAbove` keeps coordinates >= v, else <= v. */
    private static ArrayList<float[]> clip(ArrayList<float[]> in, int axis, float v, boolean keepAbove) {
        ArrayList<float[]> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            float[] a = in.get(i), b = in.get((i + 1) % in.size());
            boolean ia = keepAbove ? a[axis] >= v : a[axis] <= v, ib = keepAbove ? b[axis] >= v : b[axis] <= v;
            if (ia) out.add(a);
            if (ia != ib) {
                float u = (v - a[axis]) / (b[axis] - a[axis]);
                out.add(new float[] {a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u});
            }
        }
        return out;
    }

    /** Called at the very end of the frame: copies it and draws the glass over it. */
    void render(World w, OrthographicCamera camera, Viewport vp) {
        Maya m = w.maya;
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        if (bw < 16 || bh < 16) return;
        if (m.crack() <= 0f) return;                                          // before the first crack the glass is whole: nothing to draw
        if (!built || builtSeed != m.seed || diag <= 0f) { focus(w, camera, vp, bw, bh); build(m, bw, bh); }
        if (tex == null || tw != bw || th != bh) {
            if (tex != null) tex.dispose();
            Pixmap pm = new Pixmap(bw, bh, Pixmap.Format.RGBA8888);
            tex = new Texture(pm);
            pm.dispose();
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            tw = bw;
            th = bh;
            if (pb == null) { pb = new PolygonSpriteBatch(); sr = new ShapeRenderer(); }
            if (glass == null) glass = makeGlass();
            built = false;
            focus(w, camera, vp, bw, bh);
            build(m, bw, bh);
        }
        Gdx.gl.glViewport(0, 0, bw, bh);
        tex.bind();
        Gdx.gl.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, bw, bh);
        float fade = m.fade();
        boolean tilting = m.tiltTime() > 0f;
        if (tilting) {                                                        // the glass is cut: a dark ground shows in whatever gaps the turning leaves
            Gdx.gl.glClearColor(0.02f, 0.02f, 0.035f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            pb.setProjectionMatrix(proj(bw, bh));
            pb.begin();
            for (Piece pc : pieces) drawPiece(pc, m, fade);
            pb.end();
        }
        sr.setProjectionMatrix(proj(bw, bh));
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        cracks(m, fade);
        sr.end();
    }

    private final com.badlogic.gdx.math.Matrix4 pm4 = new com.badlogic.gdx.math.Matrix4();

    private com.badlogic.gdx.math.Matrix4 proj(int bw, int bh) { return pm4.setToOrtho2D(0, 0, bw, bh); }

    private final float[] vbuf = new float[5 * 40];
    private final short[] tbuf = new short[3 * 40];

    private void drawPiece(Piece pc, Maya m, float fade) {
        float u = MathUtils.clamp((m.tiltTime() - pc.delay) / 1.6f, 0f, 1f);
        u = u * u * (3f - 2f * u) * fade;
        float ang = pc.theta * u, ca = MathUtils.cos(-ang), sa = MathUtils.sin(-ang), sc = 1f + 0.14f * u, inv = 1f / sc;
        float shade = Math.min(1f, 1f + (pc.tint - 1f) * u);
        float col = Color.toFloatBits(shade, shade, shade, 1f);
        int n = pc.px.length;
        if (n > 38) n = 38;
        int vi = 0;
        for (int i = -1; i < n; i++) {                                         // vertex -1 is the centroid, the hub of the fan
            float x = i < 0 ? pc.cx : pc.px[i], y = i < 0 ? pc.cy : pc.py[i];
            float dx = x - pc.cx - pc.ox * u, dy = y - pc.cy - pc.oy * u;
            float sx = pc.cx + (dx * ca - dy * sa) * inv, sy = pc.cy + (dx * sa + dy * ca) * inv;
            vbuf[vi++] = x;
            vbuf[vi++] = y;
            vbuf[vi++] = col;
            vbuf[vi++] = sx / tw;
            vbuf[vi++] = sy / th;
        }
        int ti = 0;
        for (int i = 1; i <= n; i++) {
            tbuf[ti++] = 0;
            tbuf[ti++] = (short) i;
            tbuf[ti++] = (short) (i == n ? 1 : i + 1);
        }
        pb.draw(tex, vbuf, 0, (n + 1) * 5, tbuf, 0, ti);
        float ga = (0.45f + 0.55f * u) * fade, gcol = Color.toFloatBits(1f, 1f, 1f, ga);      // the glass over it: a sheen, a bright streak, a darkening toward the edges
        vi = 0;
        for (int i = -1; i < n; i++) {
            float x = i < 0 ? pc.cx : pc.px[i], y = i < 0 ? pc.cy : pc.py[i];
            float gu = (x - pc.bx) / pc.bw, gv = (y - pc.by) / pc.bh;
            if (pc.swap) { float tt = gu; gu = gv; gv = tt; }
            if (pc.flipU) gu = 1f - gu;
            if (pc.flipV) gv = 1f - gv;
            vbuf[vi++] = x;
            vbuf[vi++] = y;
            vbuf[vi++] = gcol;
            vbuf[vi++] = gu;
            vbuf[vi++] = gv;
        }
        pb.draw(glass, vbuf, 0, (n + 1) * 5, tbuf, 0, ti);
    }

    /** The glass: a 128 x 128 overlay (white where the light catches, a cool dark toward the edges), laid over every piece in its own bounding box, flipped and turned a different way
     *  for each so no two pieces catch the light alike. */
    private static Texture makeGlass() {
        int n = 128;
        Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float u = x / (n - 1f), v = y / (n - 1f), d = (u + v) * 0.5f;
                float wide = 0.12f * MathUtils.clamp(1f - Math.abs(d - 0.34f) / 0.20f, 0f, 1f);
                float thin = 0.20f * MathUtils.clamp(1f - Math.abs(d - 0.60f) / 0.035f, 0f, 1f);
                float thin2 = 0.14f * MathUtils.clamp(1f - Math.abs(d - 0.69f) / 0.02f, 0f, 1f);
                float ab = 1f - (1f - wide) * (1f - thin) * (1f - thin2);
                float edge = Math.max(Math.abs(u - 0.5f), Math.abs(v - 0.5f)) * 2f;
                float av = 0.20f * edge * edge;
                float a = 1f - (1f - ab) * (1f - av);
                float cr = a > 0f ? (1f * ab + 0.04f * av * (1f - ab)) / a : 0f, cg = a > 0f ? (1f * ab + 0.07f * av * (1f - ab)) / a : 0f, cb = a > 0f ? (1f * ab + 0.16f * av * (1f - ab)) / a : 0f;
                pm.drawPixel(x, y, Color.rgba8888(MathUtils.clamp(cr, 0f, 1f), MathUtils.clamp(cg, 0f, 1f), MathUtils.clamp(cb, 0f, 1f), MathUtils.clamp(a, 0f, 1f)));
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** The cracks: a dark line with a pale edge, growing ray by ray outward from the heart, then the ring joining them; a white flash at the first. */
    private void cracks(Maya m, float fade) {
        float p = m.crack(), s = p * K, a = fade;
        float flash = MathUtils.clamp(1f - (m.t - Maya.CRACK_AT) / 0.4f, 0f, 1f);
        if (flash > 0f) { sr.setColor(1f, 1f, 1f, 0.55f * flash); sr.circle(fx, fy, 30f + 120f * (1f - flash), 28); }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < RAYS; i++) {
                for (int k = 0; k < K; k++) {
                    float seg = MathUtils.clamp(s - k, 0f, 1f);
                    if (seg <= 0f) break;
                    float x0 = chx[i][k], y0 = chy[i][k], x1 = x0 + (chx[i][k + 1] - x0) * seg, y1 = y0 + (chy[i][k + 1] - y0) * seg;
                    line(pass, x0, y0, x1, y1, a * (k < 2 ? 1f : 0.9f));
                }
            }
            float ring = MathUtils.clamp(s - RING, 0f, 1f);
            if (ring > 0f) {
                for (int i = 0; i < RAYS; i++) {
                    int j = (i + 1) % RAYS;
                    float x0 = chx[i][RING], y0 = chy[i][RING], x1 = chx[j][RING], y1 = chy[j][RING];
                    line(pass, x0, y0, x0 + (x1 - x0) * ring, y0 + (y1 - y0) * ring, a);
                }
            }
        }
    }

    private void line(int pass, float x0, float y0, float x1, float y1, float a) {
        if (pass == 0) { sr.setColor(0f, 0f, 0f, 0.85f * a); sr.rectLine(x0, y0, x1, y1, 3.6f); }
        else { sr.setColor(0.92f, 0.96f, 1f, 0.75f * a); sr.rectLine(x0, y0, x1, y1, 1.2f); }
    }

    void dispose() {
        if (tex != null) tex.dispose();
        if (glass != null) glass.dispose();
        if (pb != null) pb.dispose();
        if (sr != null) sr.dispose();
    }
}

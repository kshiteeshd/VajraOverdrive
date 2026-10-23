import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * SHIPFORGE - procedural 64x64 ship sprite generator (single file, no dependencies).
 *
 *   java ShipForge.java [outDir]        writes sheet + atlas + preview PNGs   (default ./out)
 *   java ShipForge.java --play          opens a small animation player (needs a display)
 *
 * MARK 1 - ATALA LOKA, the netherworld of illusion. Bala, son of Maya, made ninety-six
 * mayas there. Look: moonstone (pale silver with a blue adularescent sheen at grazing
 * angles), cyan crystal, a mirage that shimmers across the hull and leaves ghost copies.
 * Creature: a long-winged moth/dragonfly whose primaries flutter with phase lag.
 *
 * PIPELINE (same for every mark, so the set stays consistent):
 *   parts (tapered capsules / ellipses with a height profile) -> 4x supersampled height
 *   buffer -> normals -> light from the top-left -> palette ramps -> 64x64 downsample
 *   (dominant material per pixel, crisp silhouette) -> edge shading, glow, ghosts, sparks.
 */
public class ShipForge {
    static final int N = 64, S = 4, W = N * S;
    static final int IDLE = 8, HIT = 4, TILT = 3;
    static final int FRAMES = IDLE + HIT + TILT * 2;
    static final String[] TIER = {"a", "b", "c"};

    // ------------------------------------------------------------------ palettes
    enum Mat { MOON, TRIM, CRYSTAL, GOLD, FLAME }

    static int[][] ramp(Mat m) {
        switch (m) {
            case MOON:    return rgb(0x1f2547, 0x3a4678, 0x66769f, 0x93a7cc, 0xc6d6ea, 0xf2f8ff);
            case TRIM:    return rgb(0x232845, 0x3d466f, 0x6a749b, 0x9aa4c4, 0xc9d0e6, 0xeef1fa);
            case CRYSTAL: return rgb(0x0b3552, 0x11708f, 0x1fb4cf, 0x6fe3ee, 0xc6f8ff, 0xf4ffff);
            case GOLD:    return rgb(0x4a361f, 0x7d5f2c, 0xb9954a, 0xe3c46e, 0xf7e49c, 0xfff6cf);
            default:      return rgb(0x0e4a6b, 0x1a8fb0, 0x3fd0e6, 0x9cf0f6, 0xe2fcff, 0xffffff);
        }
    }

    static int[][] rgb(int... c) {
        int[][] o = new int[c.length][3];
        for (int i = 0; i < c.length; i++) { o[i][0] = c[i] >> 16 & 255; o[i][1] = c[i] >> 8 & 255; o[i][2] = c[i] & 255; }
        return o;
    }

    static final int[][][] RAMPS = new int[Mat.values().length][][];
    static { for (Mat m : Mat.values()) RAMPS[m.ordinal()] = ramp(m); }

    // ------------------------------------------------------------------ parts
    static final class Part {
        int kind;                 // 0 capsule, 1 ellipse, 2 ring
        float x1, y1, x2, y2, r1, r2;
        float elev, h, tip, groove, emis, inner;
        Mat mat;
    }

    static Part cap(float x1, float y1, float x2, float y2, float r1, float r2, float elev, float h, Mat m) {
        Part p = new Part();
        p.kind = 0; p.x1 = x1; p.y1 = y1; p.x2 = x2; p.y2 = y2; p.r1 = r1; p.r2 = r2;
        p.elev = elev; p.h = h; p.mat = m;
        return p;
    }

    static Part ell(float cx, float cy, float rx, float ry, float elev, float h, Mat m) {
        Part p = new Part();
        p.kind = 1; p.x1 = cx; p.y1 = cy; p.r1 = rx; p.r2 = ry; p.elev = elev; p.h = h; p.mat = m;
        return p;
    }

    static Part ring(float cx, float cy, float rx, float ry, float inner, float elev, Mat m) {
        Part p = ell(cx, cy, rx, ry, elev, 1.2f, m);
        p.kind = 2; p.inner = inner;
        return p;
    }

    static Part mirror(Part s) {
        Part p = new Part();
        p.kind = s.kind; p.x1 = -s.x1; p.y1 = s.y1; p.x2 = -s.x2; p.y2 = s.y2; p.r1 = s.r1; p.r2 = s.r2;
        p.elev = s.elev; p.h = s.h; p.tip = s.tip; p.groove = s.groove; p.emis = s.emis; p.inner = s.inner; p.mat = s.mat;
        return p;
    }

    static void pair(List<Part> l, Part p) { l.add(p); l.add(mirror(p)); }

    // ------------------------------------------------------------------ pose (one animation frame)
    static final class Pose {
        float t;            // idle phase 0..2pi
        float bob, sx = 1, sy = 1;
        float flap = 0.09f; // feather flutter amplitude (radians)
        float sweep;        // extra swept-back angle (radians)
        float flame = 8;    // exhaust length
        float glow = 1;     // crystal / flame brightness
        float band = -1;    // mirage shimmer position, 0..1 across the loop
        float flash;        // 0..1 white flash
        float bank;         // radians, + = right wing down
        float ghost = 1.5f; // ghost spread in px
        float ghostA = 0.30f;
        int sparks = -1;    // hit frame index for sparks
    }

    static Pose idle(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.bob = 0.55f * (float) Math.sin(p.t);
        p.sx = 1 + 0.012f * (float) Math.sin(p.t + 1.2);
        p.flame = 8 + 1.6f * (float) Math.sin(2 * p.t + 0.7) + 0.8f * (float) Math.sin(3 * p.t);
        p.glow = 0.78f + 0.22f * (float) Math.sin(p.t - 0.6);
        p.band = i / (float) IDLE;
        p.ghost = 1.6f + 0.9f * (float) Math.sin(p.t * 1.0 + 2.0);
        return p;
    }

    static Pose hit(int i) {
        Pose p = idle(0);
        p.sparks = i;
        switch (i) {
            case 0: p.flash = 0.92f; p.bob = 2.0f; p.sx = 1.07f; p.sy = 0.93f; p.sweep = 0.20f; p.glow = 1.3f; p.flame = 3; p.ghost = 6f; p.ghostA = 0.5f; break;
            case 1: p.flash = 0.45f; p.bob = 1.6f; p.sx = 1.04f; p.sy = 0.96f; p.sweep = 0.30f; p.glow = 0.2f; p.flame = 4; p.ghost = 4.2f; p.ghostA = 0.35f; break;
            case 2: p.flash = 0f; p.bob = 0.7f; p.sx = 0.99f; p.sy = 1.01f; p.sweep = 0.14f; p.glow = 0.35f; p.flame = 5.5f; p.ghost = 2.6f; p.ghostA = 0.22f; break;
            default: p.flash = 0f; p.bob = 0.2f; p.sweep = 0.05f; p.glow = 0.7f; p.flame = 6.5f; p.ghost = 1.8f; p.ghostA = 0.28f; break;
        }
        return p;
    }

    static Pose tilt(float bank) {
        Pose p = idle(0);
        p.bank = bank;
        p.sweep = 0.05f + 0.10f * Math.abs(bank);
        p.flame = 6.4f;
        p.ghost = 1.2f + 2.6f * Math.abs(bank);  // the mirage smears sideways when it banks
        return p;
    }

    // ------------------------------------------------------------------ MARK 1 : ATALA
    // tier 0 = A (bare hull), 1 = B (augmented), 2 = C (ascended). Tiers only ADD parts.
    static List<Part> mark1(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float grow = tier == 2 ? 1.06f : 1f;

        // wing base plates
        Part plate = cap(2.5f, -3.5f, 12.5f, 5f, 5.2f, 2.6f, 1.6f, 1.6f, Mat.TRIM);
        plate.groove = 0.4f;
        pair(l, plate);

        // primaries: root x,y -> tip x,y, r0, r1, elev
        float[][] f = {
            {3, -10, 17.5f, -4, 3.5f, 1.0f, 0.5f},
            {3, -4, 23.5f, 5, 4.1f, 1.1f, 0.8f},
            {3, 2, 22, 15.5f, 3.9f, 1.0f, 0.6f},
        };
        List<float[]> fs = new ArrayList<>();
        for (float[] a : f) fs.add(a);
        if (tier >= 1) fs.add(0, new float[]{3, -14.5f, 13.5f, -10.5f, 2.6f, 0.8f, 0.3f});
        if (tier >= 2) fs.add(new float[]{3, 7f, 17f, 21.5f, 3.2f, 0.8f, 0.4f});

        for (int i = 0; i < fs.size(); i++) {
            float[] a = fs.get(i);
            float ang = p.sweep + p.flap * (float) Math.sin(p.t - i * 0.55f + 0.2f);
            float rx = a[0], ry = a[1];
            float dx = (a[2] - rx) * grow, dy = (a[3] - ry) * grow;
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
            float tx = rx + dx * c - dy * s, ty = ry + dx * s + dy * c;
            Part fe = cap(rx, ry, tx, ty, a[4], a[5], a[6], 2.1f, Mat.MOON);
            fe.tip = 1.0f;
            fe.groove = 0.35f;
            pair(l, fe);
        }

        // fuselage: tapered nose to a full belly, then a narrowing tail
        Part nose = cap(0, -25, 0, -4, 0.8f, 5.6f, 2f, 4.6f, Mat.MOON);
        nose.groove = 0.45f;
        Part tail = cap(0, -4, 0, 15, 5.6f, 3.8f, 2f, 4.4f, Mat.MOON);
        tail.groove = 0.45f;
        l.add(nose);
        l.add(tail);

        // tail fins
        pair(l, cap(3f, 10, 7.5f, 19.5f, 1.7f, 0.5f, 1.2f, 1.0f, Mat.TRIM));

        // canopy gem
        Part gem = ell(0, -8f, 2.5f, 5.2f, 5.6f, 2.6f, Mat.CRYSTAL);
        gem.emis = 0.55f * p.glow + 0.2f;
        l.add(gem);

        // engine nacelle + exhaust
        l.add(cap(0, 13.5f, 0, 21, 3.1f, 2.3f, 1f, 2.4f, Mat.TRIM));
        Part fl = cap(0, 21, 0, 21 + p.flame, 2.3f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
        fl.emis = Math.max(0.25f, p.glow);
        l.add(fl);

        if (tier >= 1) {
            // dorsal spine and wing-tip crystals; side pods
            l.add(cap(0, -1, 0, 10, 1.2f, 1.0f, 7.8f, 1.4f, Mat.TRIM));
            for (float[] a : f) {
                if (a == f[1]) {
                    float ang = p.sweep + p.flap * (float) Math.sin(p.t - 1 * 0.55f - 0.55f * 0 + 0.2f);
                    // keep the crystal on the (fluttering) tip of the longest primary
                    float dx = (a[2] - a[0]) * grow, dy = (a[3] - a[1]) * grow;
                    float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
                    float tx = a[0] + dx * c - dy * s, ty = a[1] + dx * s + dy * c;
                    Part tc = ell(tx - 0.8f, ty - 0.8f, 1.7f, 2.5f, 1.5f, 1.6f, Mat.CRYSTAL);
                    tc.emis = 0.5f * p.glow + 0.3f;
                    pair(l, tc);
                }
            }
            Part pod = cap(7.2f, 2.5f, 7.2f, 12.5f, 1.9f, 1.5f, 3.2f, 2.2f, Mat.TRIM);
            pair(l, pod);
            Part podGem = ell(7.2f, 4.2f, 0.9f, 1.5f, 5.6f, 1.0f, Mat.CRYSTAL);
            podGem.emis = 0.5f * p.glow + 0.3f;
            pair(l, podGem);
        }
        if (tier >= 2) {
            // nose lance, gilded trim and the floating ring (maya halo)
            l.add(cap(0, -29.5f, 0, -24, 0.3f, 1.1f, 3.8f, 1.2f, Mat.CRYSTAL));
            Part rg = ring(0, -1.5f, 9.5f, 4.3f, 0.72f, 9f, Mat.GOLD);
            l.add(rg);
            Part gp = cap(2.5f, -3.5f, 12.5f, 5f, 1.6f, 0.9f, 3.3f, 0.8f, Mat.GOLD);
            pair(l, gp);
            Part fin = cap(0, 13f, 0, 19f, 0.5f, 0.2f, 6.2f, 0.8f, Mat.GOLD);
            l.add(fin);
        }
        return l;
    }

    // ------------------------------------------------------------------ renderer
    static final class Buf {
        final float[] h = new float[W * W], tint = new float[W * W], u = new float[W * W], em = new float[W * W];
        final byte[] mat = new byte[W * W];
        final float[] t = new float[W * W];
        Buf() { java.util.Arrays.fill(h, -1e9f); java.util.Arrays.fill(mat, (byte) -1); }
    }

    static void raster(Buf b, Part p, Pose po) {
        float cb = (float) Math.cos(po.bank), sb = (float) Math.sin(po.bank);
        float rf = 0.15f + 0.85f * cb;           // banking foreshortens widths
        float[] pt = proj(p.x1, p.y1, p.elev, po, cb, sb);
        float ax = pt[0], ay = pt[1];
        float bx = ax, by = ay;
        if (p.kind == 0) { pt = proj(p.x2, p.y2, p.elev, po, cb, sb); bx = pt[0]; by = pt[1]; }
        float ss = S;
        float rsc = (po.sx + po.sy) * 0.5f * rf * ss;
        float rx = p.r1 * po.sx * rf * ss, ry = p.r2 * po.sy * ss;
        float minX, maxX, minY, maxY;
        if (p.kind == 0) {
            float rm = Math.max(p.r1, p.r2) * rsc + 2;
            minX = Math.min(ax, bx) - rm; maxX = Math.max(ax, bx) + rm;
            minY = Math.min(ay, by) - rm; maxY = Math.max(ay, by) + rm;
        } else {
            minX = ax - rx - 2; maxX = ax + rx + 2; minY = ay - ry - 2; maxY = ay + ry + 2;
        }
        int x0 = Math.max(0, (int) minX), x1 = Math.min(W - 1, (int) maxX);
        int y0 = Math.max(0, (int) minY), y1 = Math.min(W - 1, (int) maxY);
        float abx = bx - ax, aby = by - ay, len2 = abx * abx + aby * aby;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float px = x + 0.5f, py = y + 0.5f;
                float q, u = 0;
                if (p.kind == 0) {
                    float t = len2 < 1e-6f ? 0 : ((px - ax) * abx + (py - ay) * aby) / len2;
                    t = t < 0 ? 0 : (t > 1 ? 1 : t);
                    float cx = ax + abx * t, cy = ay + aby * t;
                    float dx = px - cx, dy = py - cy;
                    float r = (p.r1 + (p.r2 - p.r1) * t) * rsc;
                    // stretch x-distance a touch so banked wings read as flatter
                    q = (float) Math.sqrt(dx * dx + dy * dy) / Math.max(r, 0.001f);
                    u = t;
                } else {
                    float dx = (px - ax) / Math.max(rx, 0.001f), dy = (py - ay) / Math.max(ry, 0.001f);
                    q = (float) Math.sqrt(dx * dx + dy * dy);
                    if (p.kind == 2 && q < p.inner) continue;
                }
                if (q >= 1) continue;
                float dome = p.kind == 2 ? p.h * (1 - Math.abs(q - (1 + p.inner) / 2) * 3f)
                        : p.h * (float) Math.sqrt(1 - q * q);
                float hh = p.elev + dome;
                if (p.groove > 0 && q < 0.13f) hh -= p.groove;
                int i = y * W + x;
                if (hh > b.h[i]) {
                    b.h[i] = hh;
                    b.mat[i] = (byte) p.mat.ordinal();
                    b.tint[i] = p.tip * u * u;
                    b.u[i] = u;
                    b.em[i] = p.emis * po.glow;
                }
            }
        }
    }

    /** ship-space -> supersample space. Elevated parts slide sideways when the ship banks. */
    static float[] proj(float x, float y, float z, Pose po, float cb, float sb) {
        float xs = x * po.sx * (1 - 0.24f * (float) Math.sin(po.bank) * Math.signum(x)), ys = y * po.sy + po.bob;
        float xb = xs * cb + z * sb * 0.9f;
        return new float[]{(N / 2f + xb) * S, (N / 2f + 0.5f + ys) * S};
    }

    static BufferedImage render(List<Part> parts, Pose po) {
        Buf b = new Buf();
        for (Part p : parts) raster(b, p, po);
        float sb = (float) Math.sin(po.bank);

        // lighting at 4x
        float lx = -0.50f, ly = -0.62f, lz = 0.60f;
        float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        for (int y = 1; y < W - 1; y++) {
            for (int x = 1; x < W - 1; x++) {
                int i = y * W + x;
                if (b.mat[i] < 0) continue;
                Mat m = Mat.values()[b.mat[i]];
                float hc = b.h[i];
                float hl = nb(b, i - 1, hc), hr = nb(b, i + 1, hc), hu = nb(b, i - W, hc), hd = nb(b, i + W, hc);
                float dhdx = (hr - hl) * 0.5f * S - sb * 0.9f;
                float dhdy = (hd - hu) * 0.5f * S;
                float k = 1.1f;
                float nx = -dhdx * k, ny = -dhdy * k, nz = 1;
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                nx /= nl; ny /= nl; nz /= nl;
                float diff = Math.max(0, nx * lx + ny * ly + nz * lz);
                float t = 0.12f + 0.80f * diff;
                // cavity darkening where neighbours stand taller
                int d = 3;
                float avg = (nb(b, i - d, hc) + nb(b, i + d, hc) + nb(b, i - d * W, hc) + nb(b, i + d * W, hc)) * 0.25f;
                t -= Math.min(0.28f, Math.max(0, (avg - hc) * 0.18f));
                float tint = b.tint[i];
                if (m == Mat.MOON || m == Mat.TRIM) {
                    // adularescence: blue sheen at grazing angles
                    float fr = 1 - nz;
                    if (m == Mat.MOON) tint = Math.max(tint, (float) Math.pow(fr, 1.4) * 1.1f);
                    // mirage shimmer band sweeping diagonally across the ship, once per loop
                    if (po.band >= 0) {
                        float pos = (x + y * 0.55f) / (float) (W * 1.55f);
                        float bp = -0.15f + po.band * 1.3f;
                        float dd = (pos - bp) / 0.055f;
                        float bd = (float) Math.exp(-dd * dd);
                        t += bd * 0.45f;
                        if (m == Mat.MOON) tint = Math.max(tint, bd * 0.8f);
                    }
                    if (diff > 0.95f) t += 0.12f;
                } else if (m == Mat.CRYSTAL) {
                    t = Math.min(1f, 0.30f + b.em[i] * 0.7f + diff * 0.25f);
                } else if (m == Mat.FLAME) {
                    t = Math.min(1f, b.em[i] * (0.35f + 0.65f * (1 - b.u[i])) + 0.1f);
                } else if (m == Mat.GOLD) {
                    if (diff > 0.9f) t += 0.2f;
                }
                b.t[i] = Math.max(0, Math.min(1, t));
                b.tint[i] = Math.max(0, Math.min(1, tint));
            }
        }

        // downsample: crisp silhouette, dominant material, quantised ramp
        int[] out = new int[N * N];
        boolean[] solid = new boolean[N * N];
        float[] emiMap = new float[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int[] cnt = new int[Mat.values().length];
                float[] st = new float[cnt.length], sti = new float[cnt.length];
                float emi = 0;
                int filled = 0;
                for (int sy = 0; sy < S; sy++) for (int sx = 0; sx < S; sx++) {
                    int i = (y * S + sy) * W + x * S + sx;
                    if (b.mat[i] < 0) continue;
                    filled++;
                    int m = b.mat[i];
                    cnt[m]++; st[m] += b.t[i]; sti[m] += b.tint[i];
                    if (m == Mat.CRYSTAL.ordinal() || m == Mat.FLAME.ordinal()) emi += b.em[i];
                }
                if (filled * 2 < S * S) continue;
                int best = 0;
                for (int m = 1; m < cnt.length; m++) if (cnt[m] > cnt[best]) best = m;
                float t = st[best] / cnt[best], tin = sti[best] / cnt[best];
                int[][] r = RAMPS[best];
                int idx = Math.round(t * (r.length - 1));
                int[] c = r[idx];
                if (tin > 0.08f && (best == Mat.MOON.ordinal() || best == Mat.TRIM.ordinal())) {
                    int[][] cy = RAMPS[Mat.CRYSTAL.ordinal()];
                    float q = Math.round(tin * 3) / 3f;          // three tint levels keep the palette tight
                    int[] cc = cy[Math.min(cy.length - 1, idx + 1)];
                    c = new int[]{(int) (c[0] + (cc[0] - c[0]) * q * 0.75f), (int) (c[1] + (cc[1] - c[1]) * q * 0.75f), (int) (c[2] + (cc[2] - c[2]) * q * 0.75f)};
                }
                out[y * N + x] = 0xFF000000 | c[0] << 16 | c[1] << 8 | c[2];
                solid[y * N + x] = true;
                emiMap[y * N + x] = emi / Math.max(1, filled);
            }
        }
        postEdge(out, solid);
        return finish(out, solid, emiMap, po);
    }

    static float nb(Buf b, int i, float fallback) {
        if (i < 0 || i >= b.h.length || b.mat[i] < 0) return fallback - 1.2f;   // silhouette falls away
        return b.h[i];
    }

    /** lit edges (top/left) get a rim highlight, shadowed edges (bottom/right) a dark rim: instant volume. */
    static void postEdge(int[] out, boolean[] solid) {
        int[] copy = out.clone();
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            int i = y * N + x;
            if (!solid[i]) continue;
            boolean dark = (x + 1 >= N || !solid[i + 1]) || (y + 1 >= N || !solid[i + N]);
            boolean lit = (x == 0 || !solid[i - 1]) || (y == 0 || !solid[i - N]);
            if (dark && !lit) copy[i] = mul(out[i], 0.55f);
            else if (lit && !dark) copy[i] = mul(out[i], 1.08f);
            else if (dark) copy[i] = mul(out[i], 0.78f);
        }
        System.arraycopy(copy, 0, out, 0, out.length);
    }

    static int mul(int c, float f) {
        int r = Math.min(255, (int) ((c >> 16 & 255) * f)), g = Math.min(255, (int) ((c >> 8 & 255) * f)), bl = Math.min(255, (int) ((c & 255) * f));
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    static BufferedImage finish(int[] out, boolean[] solid, float[] emi, Pose po) {
        // glow: a soft cyan halo from the exhaust and crystals into empty pixels
        float[] g = new float[N * N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            float e = emi[y * N + x];
            if (e <= 0) continue;
            for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= N || yy >= N) continue;
                float f = 1f - (float) Math.sqrt(dx * dx + dy * dy) / 3.6f;
                if (f > 0) g[yy * N + xx] += e * f * 0.22f;
            }
        }
        int[] res = out.clone();
        // ghosts: the mirage. Hollow cyan copies that drift and spread.
        int[][] gh = {{1, 0}, {-1, 0}, {0, 1}};
        float[] gm = {1f, -1f, 0.6f};
        for (int k = 0; k < 3; k++) {
            if (k == 2 && po.ghost < 1.5f) continue;
            float ox = gm[k] * po.ghost, oy = (k == 2 ? 0.6f : 0.25f * gm[k]) * po.ghost;
            int ix = Math.round(ox), iy = Math.round(oy);
            if (ix == 0 && iy == 0) continue;
            float a = po.ghostA * (k == 2 ? 0.7f : 1f);
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                int i = y * N + x;
                if (res[i] != 0 && (res[i] >>> 24) == 255) continue;
                int sx = x - ix, sy = y - iy;
                if (sx < 0 || sy < 0 || sx >= N || sy >= N || !solid[sy * N + sx]) continue;
                boolean edge = sx == 0 || sy == 0 || sx == N - 1 || sy == N - 1
                        || !solid[sy * N + sx - 1] || !solid[sy * N + sx + 1] || !solid[(sy - 1) * N + sx] || !solid[(sy + 1) * N + sx];
                float aa = edge ? a : a * 0.30f;
                int old = res[i] >>> 24;
                int na = Math.min(255, old + (int) (aa * 255));
                res[i] = na << 24 | 0x7be6f0;
            }
        }
        for (int i = 0; i < N * N; i++) {
            if ((res[i] >>> 24) == 255) continue;
            int add = (int) (Math.min(0.30f, g[i]) * 255);
            if (add > 12) {
                int old = res[i] >>> 24;
                if (old < add) res[i] = add << 24 | 0x45d4ee;
            }
        }
        // hit flash
        if (po.flash > 0) {
            for (int i = 0; i < N * N; i++) {
                if ((res[i] >>> 24) != 255) continue;
                int c = res[i];
                int r = (int) ((c >> 16 & 255) + (255 - (c >> 16 & 255)) * po.flash);
                int gg = (int) ((c >> 8 & 255) + (255 - (c >> 8 & 255)) * po.flash);
                int bb = (int) ((c & 255) + (255 - (c & 255)) * po.flash);
                res[i] = 0xFF000000 | r << 16 | gg << 8 | bb;
            }
        }
        // sparks
        if (po.sparks >= 0) {
            int[][][] sp = {
                {{22, 20}, {40, 26}, {30, 12}, {44, 38}, {18, 34}, {35, 44}},
                {{19, 17}, {44, 23}, {29, 8}, {47, 41}, {14, 36}},
                {{17, 14}, {47, 20}},
                {}
            };
            for (int[] s : sp[po.sparks]) put(res, s[0], s[1], 0xFFFFFFFF);
            if (po.sparks <= 1) for (int[] s : sp[po.sparks]) { put(res, s[0] + 1, s[1], 0xCC7be6f0); put(res, s[0], s[1] + 1, 0x997be6f0); }
        }
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, N, N, res, 0, N);
        return img;
    }

    static void put(int[] a, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < N && y < N && (a[y * N + x] >>> 24) < 255) a[y * N + x] = c;
    }

    // ------------------------------------------------------------------ sheet / export
    static BufferedImage[] frames(int tier) {
        BufferedImage[] f = new BufferedImage[FRAMES];
        int k = 0;
        for (int i = 0; i < IDLE; i++) f[k++] = render(mark1(idle(i), tier), idle(i));
        for (int i = 0; i < HIT; i++) f[k++] = render(mark1(hit(i), tier), hit(i));
        float[] bank = {0.28f, 0.58f, 0.88f};
        for (int i = 0; i < TILT; i++) { Pose p = tilt(-bank[i]); f[k++] = render(mark1(p, tier), p); }   // left
        for (int i = 0; i < TILT; i++) { Pose p = tilt(bank[i]); f[k++] = render(mark1(p, tier), p); }    // right
        return f;
    }

    static String name(int frame) {
        if (frame < IDLE) return "idle";
        if (frame < IDLE + HIT) return "hit";
        if (frame < IDLE + HIT + TILT) return "tilt_left";
        return "tilt_right";
    }

    static int index(int frame) {
        if (frame < IDLE) return frame;
        if (frame < IDLE + HIT) return frame - IDLE;
        if (frame < IDLE + HIT + TILT) return frame - IDLE - HIT;
        return frame - IDLE - HIT - TILT;
    }

    public static void main(String[] args) throws Exception {
        BufferedImage[][] all = new BufferedImage[3][];
        for (int t = 0; t < 3; t++) all[t] = frames(t);
        if (args.length > 0 && args[0].equals("--play")) { play(all); return; }

        File dir = new File(args.length > 0 ? args[0] : "out");
        dir.mkdirs();
        BufferedImage sheet = new BufferedImage(N * FRAMES, N * 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        for (int t = 0; t < 3; t++) for (int f = 0; f < FRAMES; f++) g.drawImage(all[t][f], f * N, t * N, null);
        g.dispose();
        ImageIO.write(sheet, "png", new File(dir, "mark1_sheet.png"));

        try (PrintWriter w = new PrintWriter(new File(dir, "mark1.atlas"))) {
            w.println();
            w.println("mark1_sheet.png");
            w.println("size: " + sheet.getWidth() + ", " + sheet.getHeight());
            w.println("format: RGBA8888");
            w.println("filter: Nearest, Nearest");
            w.println("repeat: none");
            for (int t = 0; t < 3; t++) for (int f = 0; f < FRAMES; f++) {
                w.println("m1_" + TIER[t] + "/" + name(f));
                w.println("  rotate: false");
                w.println("  xy: " + f * N + ", " + t * N);
                w.println("  size: " + N + ", " + N);
                w.println("  orig: " + N + ", " + N);
                w.println("  offset: 0, 0");
                w.println("  index: " + index(f));
            }
        }
        preview(all, new File(dir, "mark1_preview.png"));
        overlap(all);
        System.out.println("wrote " + dir.getAbsolutePath());
    }

    /** how much of tier A's silhouette survives in B and C: the "same ship, upgraded" metric. */
    static void overlap(BufferedImage[][] all) {
        for (int t = 1; t < 3; t++) {
            int a = 0, both = 0;
            BufferedImage x = all[0][0], y = all[t][0];
            for (int py = 0; py < N; py++) for (int px = 0; px < N; px++) {
                boolean sa = (x.getRGB(px, py) >>> 24) == 255, sb = (y.getRGB(px, py) >>> 24) == 255;
                if (sa) a++;
                if (sa && sb) both++;
            }
            System.out.printf("tier %s keeps %.1f%% of tier A's silhouette%n", TIER[t].toUpperCase(), 100.0 * both / a);
        }
    }

    static void preview(BufferedImage[][] all, File f) throws Exception {
        int sc = 3, cell = N * sc;
        int[] pick = {0, 2, 4, 6, 8, 9, 10, 11, 14, 17};
        BufferedImage img = new BufferedImage(cell * pick.length, cell * 3 + 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 24), 0, img.getHeight(), new Color(20, 14, 44)));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int t = 0; t < 3; t++) for (int i = 0; i < pick.length; i++)
            g.drawImage(all[t][pick[i]], i * cell, 10 + t * cell, cell, cell, null);
        g.dispose();
        ImageIO.write(img, "png", f);
    }

    // ------------------------------------------------------------------ player
    static void play(BufferedImage[][] all) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame fr = new javax.swing.JFrame("ShipForge - Mark 1 Atala (click: cycle clip, T: tier)");
            final int[] st = {0, 0, 0}; // clip, tier, frame
            int[][] clips = {{0, 8}, {8, 4}, {12, 3}, {15, 3}};
            javax.swing.JPanel pn = new javax.swing.JPanel() {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0;
                    g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 24), 0, getHeight(), new Color(20, 14, 44)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int[] c = clips[st[0]];
                    g.drawImage(all[st[1]][c[0] + st[2] % c[1]], 40, 40, 384, 384, null);
                }
            };
            pn.setPreferredSize(new Dimension(464, 464));
            pn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) { st[0] = (st[0] + 1) % clips.length; st[2] = 0; }
            });
            fr.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) { if (e.getKeyCode() == java.awt.event.KeyEvent.VK_T) st[1] = (st[1] + 1) % 3; }
            });
            fr.add(pn);
            fr.pack();
            fr.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            fr.setVisible(true);
            new javax.swing.Timer(110, e -> { st[2]++; pn.repaint(); }).start();
        });
    }
}
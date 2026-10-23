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
    enum Mat { MOON, TRIM, CRYSTAL, GOLD, FLAME, OBSID, FORGE, MOLTEN }
    static int CUR = 1;   // the mark being rendered

    static int[][] ramp(Mat m) {
        switch (m) {
            case MOON:    return rgb(0x1f2547, 0x3a4678, 0x66769f, 0x93a7cc, 0xc6d6ea, 0xf2f8ff);
            case TRIM:    return rgb(0x232845, 0x3d466f, 0x6a749b, 0x9aa4c4, 0xc9d0e6, 0xeef1fa);
            case CRYSTAL: return rgb(0x0b3552, 0x11708f, 0x1fb4cf, 0x6fe3ee, 0xc6f8ff, 0xf4ffff);
            case GOLD:    return rgb(0x4a361f, 0x7d5f2c, 0xb9954a, 0xe3c46e, 0xf7e49c, 0xfff6cf);
            case OBSID:   return rgb(0x07060c, 0x14121f, 0x25223a, 0x413e5e, 0x74709a, 0xc4c0e6);
            case FORGE:   return rgb(0x17100d, 0x33251b, 0x58402c, 0x85653f, 0xb8925c, 0xe9cc94);
            case MOLTEN:  return rgb(0x4d0c04, 0xa02b07, 0xe2620d, 0xffa51c, 0xffdb64, 0xfff7cc);
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

    /** the ramp for a material in the mark being rendered (Vitala's exhaust burns molten, not cyan) */
    static int[][] R(Mat m) { return (CUR == 2 && m == Mat.FLAME) ? RAMPS[Mat.MOLTEN.ordinal()] : RAMPS[m.ordinal()]; }

    /** the glowing material a surface's heat/sheen tints toward, or null */
    static Mat tintTarget(Mat m) {
        if (m == Mat.MOON || m == Mat.TRIM) return Mat.CRYSTAL;
        if (m == Mat.OBSID || m == Mat.FORGE) return Mat.MOLTEN;
        return null;
    }

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
        float loop;         // 0..1 position in the idle loop (embers ride it)
        float embers = 0;   // ember density
    }

    static final float[] HEART = {1.0f, 0.62f, 0.42f, 0.85f, 0.58f, 0.40f, 0.34f, 0.5f}; // double-beat of the forge

    static Pose idle2(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.bob = 0.42f * (float) Math.sin(p.t);
        p.sx = 1 + 0.018f * (float) Math.sin(p.t + 0.5);
        p.sy = 1 - 0.008f * (float) Math.sin(p.t + 0.5);
        p.flap = 0.05f;
        p.flame = 7.2f + 1.5f * (float) Math.sin(2 * p.t + 0.5) + 0.7f * (float) Math.sin(3 * p.t);
        p.glow = HEART[i];
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle(int i) {
        if (CUR == 2) return idle2(i);
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
        if (CUR == 2) {  // the forge flares, goes dark as the armour cracks, then re-lights
            float[][] hv = {{0.92f, 2.0f, 1.07f, 0.93f, 0.12f, 1.6f, 2.5f}, {0.4f, 1.6f, 1.04f, 0.96f, 0.2f, 0.15f, 3f},
                            {0f, 0.7f, 0.99f, 1.01f, 0.08f, 0.45f, 5f}, {0f, 0.2f, 1f, 1f, 0.02f, 0.8f, 6.5f}};
            float[] h = hv[i];
            p.flash = h[0]; p.bob = h[1]; p.sx = h[2]; p.sy = h[3]; p.sweep = h[4]; p.glow = h[5]; p.flame = h[6];
            p.embers = i == 0 ? 3 : i == 1 ? 2 : 1;
            p.loop = i * 0.21f;
            p.ghost = 0;
            return p;
        }
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
        p.ghost = CUR == 1 ? 1.2f + 2.6f * Math.abs(bank) : 0;  // the mirage smears sideways when it banks
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

    static List<Part> build(Pose p, int tier) { return CUR == 2 ? mark2(p, tier) : mark1(p, tier); }

    static float[] rotTip(float rx, float ry, float tx, float ty, float ang) {
        float dx = tx - rx, dy = ty - ry, c = (float) Math.cos(ang), s = (float) Math.sin(ang);
        return new float[]{rx + dx * c - dy * s, ry + dx * s + dy * c};
    }

    // ------------------------------------------------------------------ MARK 2 : VITALA
    // Hatakeshvara's forge-realm, the river of hataka gold. Obsidian armour, molten veins, a heartbeat.
    static List<Part> mark2(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow;
        float grow = tier == 2 ? 1.05f : 1f;
        float rock = p.flap * (float) Math.sin(p.t);

        // armour slabs (rock gently about their roots, like a breathing flank)
        float[][] slabs = {{6, -2, 22, 8, 6.0f, 3.0f}, {6, 7, 20, 19, 5.0f, 2.4f}};
        List<float[]> sl = new ArrayList<>();
        for (float[] a : slabs) sl.add(a);
        if (tier >= 1) sl.add(new float[]{7, -9, 17.5f, -4.5f, 3.6f, 2.0f});
        for (int i = 0; i < sl.size(); i++) {
            float[] a = sl.get(i);
            float ang = p.sweep + rock * (i % 2 == 0 ? 1 : -1);
            float[] t = rotTip(a[0], a[1], a[0] + (a[2] - a[0]) * grow, a[3] + (a[3] - a[1]) * 0 + (a[3] - a[1]) * (grow - 1), ang);
            Part slab = cap(a[0], a[1], t[0], t[1], a[4], a[5], 1.2f, 2.4f, Mat.OBSID);
            slab.groove = 0.25f;
            pair(l, slab);
            // the vein that runs down the middle of each slab
            float[] v1 = {a[0] + (t[0] - a[0]) * 0.12f, a[1] + (t[1] - a[1]) * 0.12f};
            float[] v2 = {a[0] + (t[0] - a[0]) * 0.80f, a[1] + (t[1] - a[1]) * 0.80f};
            Part vein = cap(v1[0], v1[1], v2[0], v2[1], 0.75f, 0.4f, 3.45f, 0.5f, Mat.MOLTEN);
            vein.emis = 0.85f * g + 0.15f;
            pair(l, vein);
        }
        if (tier >= 2) { // outer horns
            float[] t = rotTip(10, -6, 20f * grow, -23.5f, p.sweep * 0.8f);
            Part h = cap(10, -6, t[0], t[1], 3.4f, 0.7f, 2.2f, 2.4f, Mat.OBSID);
            pair(l, h);
            Part hg = cap(10 + (t[0] - 10) * 0.45f, -6 + (t[1] + 6) * 0.45f, t[0], t[1], 1.0f, 0.3f, 4.2f, 0.4f, Mat.GOLD);
            pair(l, hg);
        }

        // torso: broad, armoured, narrowing to the prow
        Part front = cap(0, -19, 0, -2, 1.6f, 8.4f, 2f, 5.2f, Mat.OBSID);
        front.groove = 0.3f;
        Part back = cap(0, -2, 0, 16, 8.4f, 6.4f, 2f, 5.0f, Mat.OBSID);
        back.groove = 0.3f;
        l.add(front);
        l.add(back);

        // tusks: the horned prow of the beast
        float sway = 0.04f * (float) Math.sin(p.t + 1f) + p.sweep * 0.5f;
        float[] tt = rotTip(4.5f, -8, 8.6f, -26.5f, sway);
        pair(l, cap(4.5f, -8, tt[0], tt[1], 3.0f, 0.6f, 3.2f, 2.6f, Mat.OBSID));
        Part tg = cap(4.5f + (tt[0] - 4.5f) * 0.55f, -8 + (tt[1] + 8) * 0.55f, tt[0], tt[1] + 0.6f, 1.0f, 0.3f, 5.5f, 0.35f, Mat.MOLTEN);
        tg.emis = 0.7f * g + 0.3f;
        pair(l, tg);

        // shoulders
        pair(l, cap(9.5f, -6.5f, 9.5f, -2.5f, 3.4f, 3.0f, 3.4f, 3.0f, Mat.FORGE));

        // spine vein
        Part sp = cap(0, -12, 0, -4.5f, 0.55f, 0.8f, 7.15f, 0.4f, Mat.MOLTEN);
        sp.emis = 0.9f * g + 0.1f;
        l.add(sp);
        Part sp2 = cap(0, 2.5f, 0, 13, 0.9f, 0.5f, 7.15f, 0.4f, Mat.MOLTEN);
        sp2.emis = 0.8f * g + 0.1f;
        l.add(sp2);

        // the forge heart: a core that grows with the tier
        float cr = tier == 0 ? 2.6f : tier == 1 ? 3.1f : 3.4f;
        Part core = ell(0, -1f, cr, cr * 1.25f, 7.2f, 1.5f, Mat.MOLTEN);
        core.emis = 0.85f * g + 0.15f;
        l.add(core);

        // twin exhausts, flickering out of step
        pair(l, cap(3.6f, 14, 3.6f, 22, 2.8f, 2.2f, 1f, 2.4f, Mat.FORGE));
        for (int side = -1; side <= 1; side += 2) {
            float len = p.flame + side * 0.9f * (float) Math.sin(p.t * 2 + 1.1f);
            Part fl = cap(side * 3.6f, 22, side * 3.6f, 22 + Math.max(2, len), 2.2f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
            fl.emis = Math.max(0.3f, g);
            l.add(fl);
        }

        if (tier >= 1) {
            pair(l, cap(8f, 10f, 11.5f, 21f, 2.8f, 1.4f, 1.4f, 2.0f, Mat.FORGE));   // exhaust sponsons
            l.add(cap(0, -22, 0, -8, 0.8f, 1.9f, 7.1f, 1.0f, Mat.FORGE));          // prow ridge
            pair(l, cap(2.9f, 5.5f, 2.9f, 11, 1.25f, 1.1f, 7.0f, 1.7f, Mat.FORGE)); // dorsal stacks
            Part sg = ell(2.9f, 5.4f, 0.9f, 1.0f, 9.0f, 0.5f, Mat.MOLTEN);
            sg.emis = 0.6f * g + 0.4f;
            pair(l, sg);
            // veins branching from the heart to the shoulders
            Part b1 = cap(0, 2f, 4f, 3.4f, 0.65f, 0.55f, 6.5f, 0.4f, Mat.MOLTEN);
            b1.emis = 0.75f * g + 0.15f;
            pair(l, b1);
            Part b2 = cap(4f, 3.4f, 7.4f, 5.4f, 0.55f, 0.45f, 5.0f, 0.4f, Mat.MOLTEN);
            b2.emis = 0.75f * g + 0.15f;
            pair(l, b2);
        }
        if (tier >= 2) {
            l.add(ring(0, -1f, 6.4f, 7.0f, 0.74f, 8.4f, Mat.GOLD));                  // the hearth rim
            pair(l, ell(9.5f, -4.5f, 2.3f, 3.2f, 6.4f, 1.0f, Mat.GOLD));              // gilded pauldrons
            l.add(cap(0, -26.5f, 0, -22, 0.35f, 1.0f, 8f, 0.8f, Mat.GOLD));           // gilded crest
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
                if (m == Mat.OBSID) {   // volcanic glass: nearly black, hard glints, ember heat at the rim
                    t = 0.07f + 0.26f * diff + 0.7f * Math.max(0, diff - 0.76f) / 0.24f;
                    tint = Math.max(tint, (float) Math.pow(1 - nz, 1.7) * 0.9f);
                    t -= Math.min(0.0f, 0);
                } else if (m == Mat.FORGE) {
                    t = 0.10f + 0.80f * diff;
                } else if (m == Mat.MOLTEN) {
                    t = Math.min(0.9f, 0.16f + b.em[i] * 0.52f + diff * 0.10f);
                } else if (m == Mat.MOON || m == Mat.TRIM) {
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
                    if (m == Mat.CRYSTAL.ordinal() || m == Mat.FLAME.ordinal() || m == Mat.MOLTEN.ordinal()) emi += b.em[i];
                }
                if (filled * 2 < S * S) continue;
                int best = 0;
                for (int m = 1; m < cnt.length; m++) if (cnt[m] > cnt[best]) best = m;
                float t = st[best] / cnt[best], tin = sti[best] / cnt[best];
                int[][] r = R(Mat.values()[best]);
                int idx = Math.round(t * (r.length - 1));
                int[] c = r[idx];
                Mat tt = tintTarget(Mat.values()[best]);
                if (tin > 0.08f && tt != null) {
                    int[][] cy = R(tt);
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
            else if (lit && !dark) copy[i] = CUR == 2 ? lift(out[i], 30) : mul(out[i], 1.08f);
            else if (dark) copy[i] = mul(out[i], 0.78f);
        }
        System.arraycopy(copy, 0, out, 0, out.length);
    }

    static int lift(int c, int a) {
        return 0xFF000000 | Math.min(255, (c >> 16 & 255) + a) << 16 | Math.min(255, (c >> 8 & 255) + a * 3 / 4) << 8 | Math.min(255, (c & 255) + a / 2);
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
                if (f > 0) g[yy * N + xx] += e * f * (CUR == 2 ? 0.30f : 0.22f);
            }
        }
        int[] res = out.clone();
        // ghosts: the mirage. Hollow cyan copies that drift and spread.
        int[][] gh = {{1, 0}, {-1, 0}, {0, 1}};
        float[] gm = {1f, -1f, 0.6f};
        for (int k = 0; k < 3 && CUR == 1; k++) {
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
                if (old < add) res[i] = add << 24 | (CUR == 2 ? 0xff7a1a : 0x45d4ee);
            }
        }
        // embers shed from the exhausts, falling away behind the ship (Vitala)
        if (po.embers > 0) {
            int n = (int) (6 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n * 1.0f + (k % 2) * 0.11f) % 1f;
                float x0 = 32 + (((k * 37) % 13) - 6) * 0.95f + (float) Math.sin((ph + k) * 5.1) * 1.4f + po.bank * 2.5f;
                float y = 52 + ((k * 5) % 4) + ph * 11f;
                int a = (int) (255 * (1 - ph) * (1 - ph * 0.3f));
                int col = ph < 0.3f ? 0xffe27a : ph < 0.6f ? 0xff8f26 : 0xc2421a;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
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
            for (int[] s : sp[po.sparks]) put(res, s[0], s[1], CUR == 2 ? 0xFFFFE9A8 : 0xFFFFFFFF);
            if (po.sparks <= 1) for (int[] s : sp[po.sparks]) { put(res, s[0] + 1, s[1], CUR == 2 ? 0xCCff8f26 : 0xCC7be6f0); put(res, s[0], s[1] + 1, CUR == 2 ? 0x99ff8f26 : 0x997be6f0); }
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
        for (int i = 0; i < IDLE; i++) f[k++] = render(build(idle(i), tier), idle(i));
        for (int i = 0; i < HIT; i++) f[k++] = render(build(hit(i), tier), hit(i));
        float[] bank = {0.28f, 0.58f, 0.88f};
        for (int i = 0; i < TILT; i++) { Pose p = tilt(-bank[i]); f[k++] = render(build(p, tier), p); }   // left
        for (int i = 0; i < TILT; i++) { Pose p = tilt(bank[i]); f[k++] = render(build(p, tier), p); }    // right
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

    static final String[] LOKA = {"", "Atala", "Vitala"};
    static final int MARKS = 2;

    public static void main(String[] args) throws Exception {
        int only = 0;
        boolean play = false;
        String out = "out";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--play")) play = true;
            else if (args[i].equals("--mark")) only = Integer.parseInt(args[++i]);
            else out = args[i];
        }
        if (play) { playAll(); return; }
        File dir = new File(out);
        dir.mkdirs();
        for (int mk = 1; mk <= MARKS; mk++) {
            if (only != 0 && mk != only) continue;
            CUR = mk;
            BufferedImage[][] all = new BufferedImage[3][];
            for (int t = 0; t < 3; t++) all[t] = frames(t);
            exportMark(mk, all, dir);
        }
    }

    static void exportMark(int mk, BufferedImage[][] all, File dir) throws Exception {
        String base = "mark" + mk;
        BufferedImage sheet = new BufferedImage(N * FRAMES, N * 3, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        for (int t = 0; t < 3; t++) for (int f = 0; f < FRAMES; f++) g.drawImage(all[t][f], f * N, t * N, null);
        g.dispose();
        ImageIO.write(sheet, "png", new File(dir, base + "_sheet.png"));
        try (PrintWriter w = new PrintWriter(new File(dir, base + ".atlas"))) {
            w.println();
            w.println(base + "_sheet.png");
            w.println("size: " + sheet.getWidth() + ", " + sheet.getHeight());
            w.println("format: RGBA8888");
            w.println("filter: Nearest, Nearest");
            w.println("repeat: none");
            for (int t = 0; t < 3; t++) for (int f = 0; f < FRAMES; f++) {
                w.println("m" + mk + "_" + TIER[t] + "/" + name(f));
                w.println("  rotate: false");
                w.println("  xy: " + f * N + ", " + t * N);
                w.println("  size: " + N + ", " + N);
                w.println("  orig: " + N + ", " + N);
                w.println("  offset: 0, 0");
                w.println("  index: " + index(f));
            }
        }
        preview(all, new File(dir, base + "_preview.png"));
        System.out.print("mark " + mk + " (" + LOKA[mk] + "): ");
        overlap(all);
        System.out.println("wrote " + new File(dir, base + "_sheet.png").getAbsolutePath());
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
    static void playAll() {
        BufferedImage[][][] all = new BufferedImage[MARKS + 1][][];
        for (int mk = 1; mk <= MARKS; mk++) {
            CUR = mk;
            all[mk] = new BufferedImage[3][];
            for (int t = 0; t < 3; t++) all[mk][t] = frames(t);
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame fr = new javax.swing.JFrame("ShipForge - click: clip | T: tier | M: mark");
            final int[] st = {0, 0, 0, 1}; // clip, tier, frame, mark
            int[][] clips = {{0, 8}, {8, 4}, {12, 3}, {15, 3}};
            javax.swing.JPanel pn = new javax.swing.JPanel() {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0;
                    g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 24), 0, getHeight(), new Color(20, 14, 44)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int[] c = clips[st[0]];
                    g.drawImage(all[st[3]][st[1]][c[0] + st[2] % c[1]], 40, 40, 384, 384, null);
                }
            };
            pn.setPreferredSize(new Dimension(464, 464));
            pn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) { st[0] = (st[0] + 1) % clips.length; st[2] = 0; }
            });
            fr.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_T) st[1] = (st[1] + 1) % 3;
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_M) st[3] = st[3] % MARKS + 1;
                }
            });
            fr.add(pn);
            fr.pack();
            fr.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            fr.setVisible(true);
            new javax.swing.Timer(110, e -> { st[2]++; pn.repaint(); }).start();
        });
    }
}
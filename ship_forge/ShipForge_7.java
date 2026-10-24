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
 * (ShipForge_7 adds MARK 6 - RASATALA, the asura war-throne, and MARK 7 - PATALA, the seven-hooded.
 * Run `java ShipForge_7.java ../build/forge --mark 6` to write just one mark's sheet.)
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
    enum Mat { MOON, TRIM, CRYSTAL, GOLD, FLAME, OBSID, FORGE, MOLTEN, IVORY, SAFFRON, LOTUS, RADIANT, VIOLET, BRASS, TEAL, SILVER, VENOM, SULPHUR, ACID,
        ASURA, BLOOD, VOID, RUBY, EMER, SAPH, AMETH, MATTE, REDF }
    static int CUR = 1;   // the mark being rendered

    static int[][] ramp(Mat m) {
        switch (m) {
            case MOON:    return rgb(0x1f2547, 0x3a4678, 0x66769f, 0x93a7cc, 0xc6d6ea, 0xf2f8ff);
            case TRIM:    return rgb(0x232845, 0x3d466f, 0x6a749b, 0x9aa4c4, 0xc9d0e6, 0xeef1fa);
            case CRYSTAL: return rgb(0x0b3552, 0x11708f, 0x1fb4cf, 0x6fe3ee, 0xc6f8ff, 0xf4ffff);
            case GOLD:    return rgb(0x4a361f, 0x7d5f2c, 0xb9954a, 0xe3c46e, 0xf7e49c, 0xfff6cf);
            case IVORY:   return rgb(0x5c4048, 0x9c7d7e, 0xcfb1a2, 0xeed8c3, 0xfcf1e0, 0xffffff);
            case SAFFRON: return rgb(0x4a1d08, 0x8c3b0a, 0xd4680f, 0xf79c24, 0xffcb52, 0xffefae);
            case LOTUS:   return rgb(0x4b1031, 0x8e2b60, 0xd25c8e, 0xf292b2, 0xffc4d8, 0xfff1f6);
            case RADIANT: return rgb(0x8c5c16, 0xd89c2c, 0xffd45c, 0xffed9c, 0xfff9d6, 0xffffff);
            case VIOLET:  return rgb(0x120a24, 0x2a1850, 0x4e2f86, 0x7c55b8, 0xb08ee0, 0xe6d6fd);
            case BRASS:   return rgb(0x24200f, 0x4d4420, 0x857538, 0xb9a352, 0xdccb86, 0xf6ecb8);
            case TEAL:    return rgb(0x06323a, 0x0c6a72, 0x16aeb0, 0x62dcd0, 0xb8f5ec, 0xf2fffc);
            case SILVER:  return rgb(0x2a2838, 0x50506c, 0x8584a3, 0xb4b4cb, 0xdcdcea, 0xf8f8ff);
            case VENOM:   return rgb(0x07200d, 0x123e17, 0x236b2b, 0x3fa13d, 0x7bd355, 0xc6f78c);
            case SULPHUR: return rgb(0x3a3206, 0x7b6b0d, 0xc2aa19, 0xe9d431, 0xf9ef6e, 0xfffcba);
            case ACID:    return rgb(0x1b4a06, 0x4b9b10, 0x9be12b, 0xd9ff5c, 0xf3ffaa, 0xffffe9);
            case ASURA:   return rgb(0x0a0508, 0x1f0b12, 0x3d1219, 0x64202a, 0x96343f, 0xd8636c);
            case BLOOD:   return rgb(0x2a0206, 0x6b0710, 0xb81420, 0xff3a2e, 0xff8a5c, 0xffe3c4);
            case MATTE:   return rgb(0x050507, 0x0b0b10, 0x15151c, 0x22222b, 0x32323e, 0x464655);
            case REDF:    return rgb(0x2a0508, 0x5e0a10, 0xa3121a, 0xd8212a, 0xff4a44, 0xff9a8a);
            case VOID:    return rgb(0x040208, 0x0c0618, 0x1a1030, 0x30245a, 0x5a4a94, 0xa292d6);
            case RUBY:    return rgb(0x2a0510, 0x701028, 0xc41e44, 0xff5c7c, 0xffb0c0, 0xfff0f4);
            case EMER:    return rgb(0x04301c, 0x0a6a3c, 0x14b86a, 0x5ce8a0, 0xb4ffd8, 0xf0fff8);
            case SAPH:    return rgb(0x0a1a52, 0x1a3da8, 0x2f6bff, 0x7aa8ff, 0xc0d8ff, 0xf2f8ff);
            case AMETH:   return rgb(0x240a4a, 0x5018a0, 0x8a3ee0, 0xc084ff, 0xe6c4ff, 0xfaf0ff);
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
    static int[][] R(Mat m) { if (m == Mat.FLAME && CUR == 2) return RAMPS[Mat.MOLTEN.ordinal()];
        if (m == Mat.FLAME && CUR == 3) return RAMPS[Mat.RADIANT.ordinal()];
        if (m == Mat.FLAME && CUR == 4) return RAMPS[Mat.TEAL.ordinal()];
        if (m == Mat.FLAME && CUR == 5) return RAMPS[Mat.ACID.ordinal()];
        if (m == Mat.FLAME && CUR == 6) return RAMPS[Mat.BLOOD.ordinal()];
        if (m == Mat.FLAME && CUR == 7) return RAMPS[Mat.RADIANT.ordinal()];
        return RAMPS[m.ordinal()]; }

    /** the glowing material a surface's heat/sheen tints toward, or null */
    static Mat tintTarget(Mat m) {
        if (m == Mat.MOON || m == Mat.TRIM) return Mat.CRYSTAL;
        if (m == Mat.OBSID) return CUR == 4 ? Mat.TEAL : Mat.MOLTEN;
        if (m == Mat.FORGE) return Mat.MOLTEN;
        if (m == Mat.VENOM) return Mat.SULPHUR;    // sulphur glints along the scales
        if (m == Mat.VIOLET) return Mat.TEAL;      // sorcery leaks out at the edges
        if (m == Mat.IVORY) return Mat.LOTUS;      // a blush of lotus under the ivory at grazing angles
        if (m == Mat.SAFFRON) return Mat.GOLD;     // gilded petal tips
        if (m == Mat.ASURA) return Mat.BLOOD;      // blood-light along every edge
        if (m == Mat.VOID) return Mat.AMETH;       // the dark shot through with amethyst at the rim
        if (m == Mat.LOTUS) return Mat.IVORY;      // pale tips
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
        float scatter;      // how far the orbiting cities are flung out (Talatala)
        int shift;          // Patala: how far the jewel colours have walked round the fan
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

    static Pose idle3(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.bob = 0.6f * (float) Math.sin(p.t);
        p.flap = 0.085f;                       // petal bloom amplitude
        p.glow = 0.72f + 0.28f * (float) Math.sin(p.t - 0.5);
        p.flame = 6.3f + 1.2f * (float) Math.sin(2 * p.t + 0.4f);
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle4(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.bob = 0.5f * (float) Math.sin(p.t + 0.7f);
        p.glow = 0.66f + 0.34f * (float) Math.sin(p.t - 0.4f);
        p.flame = 6f + 1.3f * (float) Math.sin(2 * p.t + 0.3f);
        p.sweep = 0.35f;            // how hard the tendril writhes
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle5(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.bob = 0.45f * (float) Math.sin(p.t);
        p.sx = 1 + 0.015f * (float) Math.sin(p.t + 0.4f);
        p.glow = 0.7f + 0.3f * (float) Math.sin(p.t - 0.5f);
        p.flame = 6f + 1.3f * (float) Math.sin(2 * p.t + 0.2f);
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle6(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.bob = 0.5f * (float) Math.sin(p.t);
        p.sx = 1 + 0.014f * (float) Math.sin(p.t + 0.6f);
        p.flap = 0.045f;                                   // the blades rattle in their sockets
        p.glow = 0.62f + 0.38f * (float) Math.pow(Math.max(0, Math.sin(p.t - 0.3f)), 2);   // a slow, heavy pulse
        p.flame = 6.5f + 1.4f * (float) Math.sin(2 * p.t + 0.3f);
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle7(int i) {
        Pose p = new Pose();
        p.t = (float) (2 * Math.PI * i / IDLE);
        p.loop = i / (float) IDLE;
        p.shift = (int) Math.floor(i * 7 / (double) IDLE);  // the colours walk one hood a frame
        p.bob = 0.45f * (float) Math.sin(p.t + 0.4f);
        p.flap = 0.035f;
        p.sweep = 0.30f;                                    // the tail's writhe
        p.glow = 0.78f + 0.22f * (float) Math.sin(p.t - 0.5f);
        p.flame = 5.6f + 1.2f * (float) Math.sin(2 * p.t + 0.2f);
        p.band = -1;
        p.ghost = 0;
        p.embers = 1;
        return p;
    }

    static Pose idle(int i) {
        if (CUR == 7) return idle7(i);
        if (CUR == 6) return idle6(i);
        if (CUR == 5) return idle5(i);
        if (CUR == 4) return idle4(i);
        if (CUR == 2) return idle2(i);
        if (CUR == 3) return idle3(i);
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
        if (CUR == 6 || CUR == 7) {  // the blades splay (Rasatala) / the hoods flare wide (Patala), then settle
            float[][] hv = {{0.9f, 1.9f, 1.06f, 0.94f, 1.0f, 1.7f, 3f, 0.0f}, {0.4f, 1.5f, 1.03f, 0.97f, 0.75f, 0.25f, 4f, 0.0f},
                            {0f, 0.7f, 0.99f, 1.01f, 0.4f, 0.55f, 5f, 0.0f}, {0f, 0.2f, 1f, 1f, 0.15f, 0.8f, 6f, 0.0f}};
            float[] h = hv[i];
            p.flash = h[0]; p.bob = h[1]; p.sx = h[2]; p.sy = h[3]; p.sweep = h[4]; p.glow = h[5]; p.flame = h[6];
            p.t = 0.8f + i * 0.9f;
            p.embers = i == 0 ? 3 : i == 1 ? 2 : 1;
            p.loop = i * 0.21f;
            p.shift = CUR == 7 ? i * 2 : 0;       // a hit scrambles the lights
            p.ghost = 0;
            return p;
        }
        if (CUR == 5) {  // the heads rear back with hoods flared, then settle
            float[][] hv = {{0.9f, 1.9f, 1.06f, 0.94f, 1.0f, 1.7f, 3f, 0.55f}, {0.4f, 1.5f, 1.03f, 0.97f, 0.8f, 0.25f, 4f, 0.65f},
                            {0f, 0.7f, 0.99f, 1.01f, 0.4f, 0.55f, 5f, 0.3f}, {0f, 0.2f, 1f, 1f, 0.15f, 0.8f, 6f, 0.1f}};
            float[] h = hv[i];
            p.flash = h[0]; p.bob = h[1]; p.sx = h[2]; p.sy = h[3]; p.sweep = h[4]; p.glow = h[5]; p.flame = h[6]; p.scatter = h[7];
            p.t = 0.8f + i * 0.9f;
            p.embers = i == 0 ? 3 : i == 1 ? 2 : 1;
            p.loop = i * 0.21f;
            p.ghost = 0;
            return p;
        }
        if (CUR == 4) {  // the eye flares, the cities are flung wide, the tendril lashes
            float[][] hv = {{0.9f, 1.9f, 1.06f, 0.94f, 1.0f, 1.7f, 3f, 0.8f}, {0.4f, 1.5f, 1.03f, 0.97f, 0.7f, 0.2f, 4f, 0.7f},
                            {0f, 0.7f, 0.99f, 1.01f, 0.45f, 0.5f, 5f, 0.35f}, {0f, 0.2f, 1f, 1f, 0.3f, 0.8f, 6f, 0.1f}};
            float[] h = hv[i];
            p.flash = h[0]; p.bob = h[1]; p.sx = h[2]; p.sy = h[3]; p.sweep = h[4]; p.glow = h[5]; p.flame = h[6]; p.scatter = h[7];
            p.t = 0.8f + i * 0.9f;
            p.embers = i == 0 ? 3 : i == 1 ? 2 : 1;
            p.loop = i * 0.21f;
            p.ghost = 0;
            return p;
        }
        if (CUR == 3) {  // the lotus snaps shut around its core, then reopens
            float[][] hv = {{0.92f, 1.9f, 1.06f, 0.94f, 0.42f, 1.5f, 3f}, {0.42f, 1.5f, 1.03f, 0.97f, 0.52f, 0.25f, 4f},
                            {0f, 0.7f, 0.99f, 1.01f, 0.26f, 0.5f, 5f}, {0f, 0.2f, 1f, 1f, 0.08f, 0.8f, 6f}};
            float[] h = hv[i];
            p.flash = h[0]; p.bob = h[1]; p.sx = h[2]; p.sy = h[3]; p.sweep = h[4]; p.glow = h[5]; p.flame = h[6];
            p.embers = i == 0 ? 3 : i == 1 ? 2 : 1;
            p.loop = i * 0.21f;
            p.ghost = 0;
            return p;
        }
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
        if (CUR == 4) p.t = 1.0f + Math.abs(bank) * 2f;  // the cities keep revolving through the bank
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

    static List<Part> build(Pose p, int tier) { return CUR == 7 ? mark7(p, tier) : CUR == 6 ? mark6(p, tier) : CUR == 5 ? mark5(p, tier) : CUR == 4 ? mark4(p, tier) : CUR == 3 ? mark3(p, tier) : CUR == 2 ? mark2(p, tier) : mark1(p, tier); }

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


    /** a leaf-shaped petal: two tapered capsules meeting at the widest point. ang swings the tip about the root. */
    static void petal(List<Part> l, float rx, float ry, float tx, float ty, float w, float elev, float h, Mat m, float tip, float ang) {
        float[] t = rotTip(rx, ry, tx, ty, ang);
        float mx = rx + (t[0] - rx) * 0.50f, my = ry + (t[1] - ry) * 0.50f;
        Part a = cap(rx, ry, mx, my, 0.8f, w, elev, h, m);
        a.groove = 0.2f;
        Part b = cap(mx, my, t[0], t[1], w, 1.15f, elev, h, m);
        b.tip = tip;
        b.groove = 0.2f;
        pair(l, a);
        pair(l, b);
    }

    // ------------------------------------------------------------------ MARK 3 : SUTALA
    // King Bali's realm, richer than heaven, where Vamana keeps the gate. A lotus-petal ship in three
    // tiers (Vamana's three steps): saffron below, ivory in the middle, lotus pink on top, gilded at the tips.
    static List<Part> mark3(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow;
        float grow = tier == 2 ? 1.06f : 1f;
        float bloom = p.flap;
        float close = p.sweep;

        // tail plumes (peacock-like, saffron)
        Part plume = cap(3.2f, 12, 7.5f, 25, 1.5f, 0.3f, 0.4f, 1.0f, Mat.SAFFRON);
        plume.tip = 0.9f;
        pair(l, plume);

        // step one: the low saffron petals (each layer breathes a beat behind the one above)
        float[][] low = {{3, 4, 25, 13, 5.0f}, {3, 9, 20, 23, 4.4f}};
        float[][] mid = {{3, -2, 24, 0, 4.6f}, {3, 2, 22, 10, 4.3f}};
        float[][] top = {{3, -7, 17.5f, -12, 3.8f}};
        List<float[]> L = new ArrayList<>(), M = new ArrayList<>(), T = new ArrayList<>();
        for (float[] a : low) L.add(a);
        for (float[] a : mid) M.add(a);
        for (float[] a : top) T.add(a);
        if (tier >= 1) {
            L.add(new float[]{3, 12, 15, 25.5f, 3.3f});
            T.add(new float[]{3, -4, 20, -5, 3.1f});
        }
        if (tier >= 2) T.add(new float[]{2, -12, 11, -20, 2.6f});
        for (int i = 0; i < L.size(); i++) {
            float[] a = L.get(i);
            petal(l, a[0], a[1], a[0] + (a[2] - a[0]) * grow, a[1] + (a[3] - a[1]) * grow, a[4], 0.6f, 1.8f, Mat.SAFFRON, 0.9f,
                    -close + bloom * (float) Math.sin(p.t - 1.2f - i * 0.3f));
        }
        for (int i = 0; i < M.size(); i++) {
            float[] a = M.get(i);
            petal(l, a[0], a[1], a[0] + (a[2] - a[0]) * grow, a[1] + (a[3] - a[1]) * grow, a[4], 1.6f, 1.8f, Mat.IVORY, 0.8f,
                    -close + bloom * (float) Math.sin(p.t - 0.6f - i * 0.3f));
        }
        for (int i = 0; i < T.size(); i++) {
            float[] a = T.get(i);
            petal(l, a[0], a[1], a[0] + (a[2] - a[0]) * grow, a[1] + (a[3] - a[1]) * grow, a[4], 2.6f, 1.8f, Mat.LOTUS, 0.9f,
                    -close + bloom * (float) Math.sin(p.t - i * 0.3f));
        }
        if (tier >= 1) { // gold studs on the tips of the ivory petals
            float[] a = mid[0];
            float[] t = rotTip(a[0], a[1], a[2], a[3], -close + bloom * (float) Math.sin(p.t - 0.6f));
            pair(l, ell(t[0] - 1.2f, t[1], 1.7f, 2.2f, 3.2f, 1.6f, Mat.GOLD));
        }

        // the bud: a slender ivory body
        Part nose = cap(0, -21, 0, -2, 1.1f, 5.4f, 2.5f, 4.0f, Mat.IVORY);
        nose.groove = 0.3f;
        Part back = cap(0, -2, 0, 14, 5.4f, 3.2f, 2.5f, 4.0f, Mat.IVORY);
        back.groove = 0.3f;
        l.add(nose);
        l.add(back);

        // the golden crest and the radiant canopy
        l.add(ell(0, -14.5f, 1.6f, 2.8f, 5.2f, 2.0f, Mat.GOLD));
        Part gem = ell(0, -6.5f, 2.4f, 5f, 5.8f, 2.6f, Mat.RADIANT);
        gem.emis = 0.5f * g + 0.25f;
        l.add(gem);

        // engine: a stamen of warm light
        l.add(cap(0, 12, 0, 17, 3.0f, 2.3f, 1f, 2.2f, Mat.IVORY));
        Part fl = cap(0, 17, 0, 17 + p.flame + 1.5f, 2.3f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
        fl.emis = Math.max(0.3f, g);
        l.add(fl);

        if (tier >= 2) { // Vamana's umbrella over the cockpit, and a spire at the nose
            l.add(cap(-5.5f, -10.5f, 5.5f, -10.5f, 1.3f, 1.3f, 7.4f, 1.2f, Mat.GOLD));
            l.add(cap(0, -13.5f, 0, -8.5f, 0.55f, 0.55f, 8.8f, 0.5f, Mat.GOLD));
            l.add(cap(0, -26f, 0, -21f, 0.3f, 1.0f, 4.0f, 1.2f, Mat.RADIANT));
            pair(l, ell(8.8f, 3.4f, 1.0f, 1.0f, 4.6f, 0.8f, Mat.RADIANT));
        }
        return l;
    }


    // ------------------------------------------------------------------ MARK 4 : TALATALA
    // Maya's realm: sorcery, architecture, the three flying cities of Tripura. NOT a mirrored ship: a leaning
    // hull with a heavy citadel tower on one flank, a watching eye, a writhing tendril on the other, and
    // small cities (iron, gold, silver) revolving on a tilted orbit, passing behind and in front of it.
    static List<Part> mark4(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow;
        float grow = tier == 2 ? 1.05f : 1f;

        // hull: leaning, off-axis
        Part front = cap(-4, -21, 0, -3, 1.0f, 6.0f, 2f, 4.5f, Mat.VIOLET);
        front.groove = 0.3f;
        Part back = cap(0, -3, 3, 15, 6.0f, 3.6f, 2f, 4.5f, Mat.VIOLET);
        back.groove = 0.3f;
        l.add(front);
        l.add(back);
        l.add(cap(-5.5f, -3.8f, 5.5f, -3.0f, 1.1f, 1.1f, 6.2f, 1.0f, Mat.BRASS));      // brass collar

        // the heavy flank: a citadel tower and a long spike
        l.add(ell(9.5f, -1.5f, 5.2f, 7.0f, 2.6f, 3.8f, Mat.VIOLET));
        l.add(ell(9.5f, -3.5f, 3.0f, 3.8f, 6.2f, 2.4f, Mat.BRASS));
        Part win = ell(9.5f, -3.5f, 1.1f, 1.7f, 8.4f, 0.5f, Mat.TEAL);
        win.emis = 0.5f * g + 0.3f;
        l.add(win);
        l.add(cap(4, 2.5f, 8.5f, -0.5f, 1.6f, 1.4f, 3.6f, 1.2f, Mat.BRASS));           // buttress
        Part sp = cap(6, 3, 6 + 18 * grow, 3 + 14 * grow, 3.4f, 0.5f, 1.2f, 2.2f, Mat.VIOLET);
        sp.tip = 0.8f;
        sp.groove = 0.3f;
        l.add(sp);

        // the light flank: a stub spire, and the tendril
        l.add(cap(-5, -8, -14, -14, 2.0f, 0.4f, 1.6f, 1.6f, Mat.VIOLET));
        tendril(l, -3, 5, -13, 19, 5, p, 1.0f, 0f);
        if (tier >= 1) {
            l.add(cap(-5, -1, -12.5f, -3f, 1.8f, 0.4f, 1.4f, 1.5f, Mat.VIOLET));
            l.add(ell(12.5f, 8.5f, 3.0f, 4.0f, 2.4f, 3.0f, Mat.VIOLET));                 // a second, lower tower
            l.add(ell(12.5f, 7.5f, 1.9f, 2.4f, 5.0f, 1.6f, Mat.BRASS));
            tendril(l, -2, 9, -7.5f, 22, 4, p, 0.8f, 1.7f);
        }
        if (tier >= 2) {
            l.add(cap(9.5f, -6.5f, 9.5f, -16f, 0.9f, 0.3f, 8.0f, 0.9f, Mat.BRASS));      // the citadel's spire
            Part e2 = ell(1.6f, -1.5f, 1.4f, 1.9f, 6.6f, 1.2f, Mat.TEAL);                // a second, smaller eye
            e2.emis = 0.5f * g + 0.3f;
            l.add(e2);
            tendril(l, -4, 2, -18, 12, 5, p, 0.9f, 3.1f);
        }

        // the eye, and the slit that follows you
        Part eye = ell(-1.5f, -9.5f, 2.5f, 3.6f, 6.4f, 2.0f, Mat.TEAL);
        eye.emis = 0.55f * g + 0.3f;
        l.add(eye);
        float drift = 0.9f * (float) Math.sin(p.t * 1.0f + 0.4f);
        l.add(ell(-1.5f + drift, -9.5f, 0.65f, 2.3f, 8.3f, 0.5f, Mat.OBSID));

        // engine: off to one side, cold fire
        l.add(cap(2.5f, 14, 3, 19.5f, 3.0f, 2.4f, 1f, 2.4f, Mat.BRASS));
        float lean = 0.8f * (float) Math.sin(p.t * 2 + 0.6f);
        Part fl = cap(3, 19.5f, 3 + lean, 19.5f + p.flame, 2.4f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
        fl.emis = Math.max(0.3f, g);
        l.add(fl);

        // the revolving cities of Tripura: 1, 2, 3 by tier; iron, gold, silver
        Mat[] city = {Mat.OBSID, Mat.BRASS, Mat.SILVER};
        int n = tier + 1;
        for (int k = 0; k < n; k++) orbiter(l, p.t + k * (float) (2 * Math.PI / n), p.scatter, city[k], g);
        return l;
    }

    /** a writhing tendril: a chain of tapered capsules whose sway travels down it like a wave. */
    static void tendril(List<Part> l, float x0, float y0, float x1, float y1, int seg, Pose p, float amp, float phase) {
        float px = x0, py = y0;
        for (int k = 1; k <= seg; k++) {
            float s = k / (float) seg;
            float sway = amp * (1.2f + 2.4f * p.sweep) * s * (float) Math.sin(p.t - s * 3.4f + phase);
            float nx = x0 + (x1 - x0) * s + sway, ny = y0 + (y1 - y0) * s;
            float r0 = 1.7f * (1 - (k - 1) / (float) seg) + 0.35f, r1 = 1.7f * (1 - s) + 0.35f;
            boolean tipSeg = k == seg;
            Part c = cap(px, py, nx, ny, r0, r1, 1.2f, 1.3f, tipSeg ? Mat.TEAL : (k % 2 == 0 ? Mat.BRASS : Mat.VIOLET));
            if (tipSeg) c.emis = 0.6f * p.glow + 0.3f;
            l.add(c);
            px = nx;
            py = ny;
        }
    }

    /** one flying city on a tilted orbit: it climbs above the hull on one half of the loop and slips behind on the other. */
    static void orbiter(List<Part> l, float a, float scatter, Mat m, float glow) {
        float R = 22f * (1 + scatter);
        for (int j = 0; j <= 2; j++) {
            float aj = a - 0.30f * j;
            float[] c = orbitAt(aj, R);
            if (j == 0) {
                l.add(ell(c[0], c[1], 3.3f, 3.1f, c[2], 2.6f, m));
                l.add(cap(c[0], c[1] - 0.4f, c[0], c[1] - 4.6f, 0.4f, 1.0f, c[2] + 1.2f, 1.0f, m == Mat.OBSID ? Mat.VIOLET : m));
                Part w = ell(c[0] + 0.8f, c[1] + 0.3f, 1.0f, 1.0f, c[2] + 2.8f, 0.4f, Mat.TEAL);
                w.emis = 0.8f * glow + 0.2f;
                l.add(w);
            } else {   // a wake of pale light
                Part t = ell(c[0], c[1], 1.25f - 0.35f * j, 1.25f - 0.35f * j, c[2] - 0.5f, 0.7f, Mat.TEAL);
                t.emis = 0.55f * glow;
                l.add(t);
            }
        }
    }

    /** a point on the tilted orbit: ship-space x, y and elevation. */
    static float[] orbitAt(float a, float R) {
        float x = R * (float) Math.cos(a);
        float y = 1 + R * 0.50f * (float) Math.sin(a);
        float z = 6 + 8.5f * (float) Math.sin(a);
        float phi = -0.28f, c = (float) Math.cos(phi), s = (float) Math.sin(phi);
        return new float[]{x * c - (y - 1) * s, 1 + x * s + (y - 1) * c, z};
    }


    // ------------------------------------------------------------------ MARK 5 : MAHATALA
    // The great nether-world of the many-headed Kadraveya serpents, who live in fear of Garuda. Symmetric, round:
    // an egg-bulb body, round hoods, round scales, coiled rings, a glowing poison sac. A hydra whose heads sway
    // out of phase (centre head lunges, each pair sways in mirror). Venom green, sulphur yellow, acid glow.
    static float bodyTop(float x, float y) {   // height of the bulb at a point, so scales sit on it
        float qx = x / 10f, qy = (y - 4f) / 10.5f, qq = qx * qx + qy * qy;
        return 2f + 6f * (float) Math.sqrt(Math.max(0, 1 - qq));
    }

    static List<Part> mark5(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow;
        float grow = tier == 2 ? 1.05f : 1f;
        float retract = p.scatter, flare = p.sweep;

        // the bulb and the tail
        l.add(ell(0, 4, 10f * grow, 10.5f * grow, 2f, 6f, Mat.VENOM));
        Part tail = cap(0, 10, 0, 19, 6.5f, 2.2f, 2f, 3.2f, Mat.VENOM);
        l.add(tail);
        // coiled tail rings
        if (tier >= 1) {
            for (int sd = -1; sd <= 1; sd += 2) {
                Part r = ring(sd * 12.5f, 12f, 5.6f, 5.6f, 0.52f, 1.6f, Mat.VENOM);
                r.h = 2.4f;
                l.add(r);
            }
        }
        // round scales
        float[][] sc = {{5, 1}, {7, 7}, {3.5f, 12}, {0, 14.5f}, {5.5f, -3.5f}, {2.2f, 7}, {8.2f, 2.5f}, {0, 1.5f}, {3, -6}};
        int ns = tier == 0 ? 4 : tier == 1 ? 7 : 9;
        for (int i = 0; i < ns; i++) {
            float[] c = sc[i];
            float r = 1.6f;
            l.add(ell(c[0], c[1], r, r, bodyTop(c[0], c[1]) - 0.3f, 1.0f, Mat.SULPHUR));
            if (c[0] != 0) l.add(ell(-c[0], c[1], r, r, bodyTop(c[0], c[1]) - 0.3f, 1.0f, Mat.SULPHUR));
        }
        // the poison sac
        Part sac = ell(0, 7f, 3.0f, 3.4f, 7.6f, 1.6f, Mat.ACID);
        sac.emis = 0.55f * g + 0.3f;
        l.add(sac);

        // exhaust
        l.add(cap(0, 18, 0, 20.5f, 2.6f, 2.0f, 1f, 2f, Mat.SULPHUR));
        Part fl = cap(0, 20.5f, 0, 20.5f + p.flame, 2.2f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
        fl.emis = Math.max(0.3f, g);
        l.add(fl);

        // the hydra: centre head, then pairs. root x,y  head x,y  hood rx,ry  sway phase
        float pull = 1 - 0.38f * retract;
        head(l, p, 0, 0, -2, 0, -20, 6.2f, 5.2f, 0f, tier >= 1, g, pull, flare, true);
        for (int sd = -1; sd <= 1; sd += 2) {
            head(l, p, sd, sd * 4.5f, -1, sd * 11.5f, -16, 5.0f, 4.2f, 1.1f, tier >= 2, g, pull, flare, false);
            if (tier >= 1) head(l, p, sd, sd * 7f, 3, sd * 19f, -7.5f, 4.2f, 3.5f, 2.2f, tier >= 2, g, pull, flare, false);
            if (tier >= 2) head(l, p, sd, sd * 8f, 8, sd * 22.5f, 3.5f, 3.4f, 2.9f, 3.3f, true, g, pull, flare, false);
        }
        if (tier >= 2) {   // a pale nimbus behind the great hood
            Part n = ring(0, -17.5f, 9.5f, 8.5f, 0.82f, 2.6f, Mat.SULPHUR);
            n.h = 1.0f;
            l.add(n);
        }
        return l;
    }

    /** one serpent: a swaying neck from the bulb, a round hood, a snout and two slit eyes. */
    static void head(List<Part> l, Pose p, int side, float rx, float ry, float hx, float hy, float hr, float hrv, float phase,
                     boolean spectacle, float g, float pull, float flare, boolean centre) {
        // pull the head back toward its root when the ship is hit
        hx = rx + (hx - rx) * pull;
        hy = ry + (hy - ry) * pull;
        float sway = (float) Math.sin(p.t - phase);
        float lunge = centre ? 1.8f * sway : 0f;             // the centre head lunges (stays on the axis)
        float lateral = centre ? 0f : side * 1.9f * sway;    // each pair sways in mirror
        hy += lunge;
        hx += lateral;
        // neck: three round capsules along a bowed path
        float bow = centre ? 0f : side * 3.2f;
        float px = rx, py = ry;
        for (int k = 1; k <= 3; k++) {
            float s = k / 3f;
            float nx = rx + (hx - rx) * s + bow * (float) Math.sin(Math.PI * s);
            float ny = ry + (hy - ry) * s;
            float r0 = 2.9f - 0.5f * (k - 1), r1 = 2.9f - 0.5f * k;
            l.add(cap(px, py, nx, ny, r0, r1, 1.8f, 2.4f, Mat.VENOM));
            px = nx;
            py = ny;
        }
        float fl = 1 + 0.10f * (float) Math.sin(p.t - phase + 1f) + 0.30f * flare;
        l.add(ell(hx, hy + 0.8f, hr * fl, hrv * (0.9f + 0.1f * fl), 3.4f, 2.4f, Mat.VENOM));       // the hood
        if (spectacle) {
            Part sp = ring(hx, hy + 1.2f, hr * 0.55f * fl, hrv * 0.55f, 0.55f, 5.85f, Mat.SULPHUR);
            sp.h = 0.5f;
            l.add(sp);
        }
        l.add(cap(hx, hy - 0.5f, hx, hy - 5.2f, 2.1f, 1.2f, 4.4f, 1.5f, Mat.VENOM));              // snout
        for (int e = -1; e <= 1; e += 2) {
            Part eye = ell(hx + e * 1.45f, hy - 2.3f, 0.95f, 1.25f, 6.1f, 0.4f, Mat.ACID);
            eye.emis = 0.6f * g + 0.3f;
            l.add(eye);
            l.add(ell(hx + e * 1.45f, hy - 2.3f, 0.28f, 0.85f, 6.6f, 0.3f, Mat.OBSID));         // slit pupil
        }
    }

    // ------------------------------------------------------------------ MARK 6 : RASATALA
    // The red court of the daityas and danavas: Hiranyapura, the golden city, the Panis who steal and hide.
    // Blood-lit and crowded. NOT a graceful ship: an ASURA KING'S WAR-THRONE. A black-red armoured body with a
    // crown of horns, two great tusks round a glowing maw, burning slit eyes, and on each side a FAN OF BLADES
    // (the court's weapons) that rattle in the idle and splay wide when hit. Tier B wears a thin golden ring with
    // towers (Hiranyapura begins to orbit it); tier C is the whole city: a full ring of towers, lower arms with
    // maces, a gilded crown, and seven blades a side.
    static List<Part> mark6(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow, splay = p.sweep;
        float grow = tier == 2 ? 1.05f : 1f;

        // Hiranyapura: the golden city ring, behind everything, its towers orbiting slowly
        if (tier >= 1) {
            float rr = tier == 2 ? 21.5f : 17f, rv = tier == 2 ? 19.5f : 15.5f;
            l.add(ring(0, 3, rr, rv, 0.90f, 0.5f, Mat.GOLD));
            int towers = tier == 2 ? 6 : 3;
            for (int k = 0; k < towers; k++) {
                float a = p.t * 0.5f + k * (float) (2 * Math.PI / towers);
                float tx = rr * (float) Math.cos(a), ty = 3 + rv * (float) Math.sin(a);
                l.add(ell(tx, ty, 1.7f, 2.4f, 2.4f, 1.6f, Mat.GOLD));
                Part wn = ell(tx, ty - 0.3f, 0.5f, 0.9f, 4.3f, 0.3f, Mat.BLOOD);
                wn.emis = 0.8f * g + 0.2f;
                l.add(wn);
            }
        }

        // the fan of blades: root at the shoulder, each splayed a little more when the ship is hit
        float[][] A = {{-55, 22}, {-12, 27}, {30, 21}};
        List<float[]> blades = new ArrayList<>();
        for (float[] b : A) blades.add(b);
        if (tier >= 1) { blades.add(new float[]{-82, 17}); blades.add(new float[]{54, 16}); }
        if (tier >= 2) blades.add(new float[]{-33, 25});
        for (int k = 0; k < blades.size(); k++) {
            float[] b = blades.get(k);
            float ang = (float) Math.toRadians(b[0]) + splay * 0.35f * (b[0] < -30 ? -1 : 1) * 0.6f
                    + p.flap * (float) Math.sin(p.t * 2 - k * 0.8f);
            float L = b[1] * grow;
            float rx = 6.5f, ry = 2f;
            float tx = rx + L * (float) Math.cos(ang), ty = ry + L * (float) Math.sin(ang);
            Part bl = cap(rx, ry, tx, ty, 4.4f, 0.3f, 1.4f, 2.8f, Mat.ASURA);
            bl.tip = 0.9f;
            bl.groove = 0.25f;
            pair(l, bl);
            float vx1 = rx + (tx - rx) * 0.22f, vy1 = ry + (ty - ry) * 0.22f;
            float vx2 = rx + (tx - rx) * 0.86f, vy2 = ry + (ty - ry) * 0.86f;
            Part edge = cap(vx1, vy1, vx2, vy2, 1.15f, 0.35f, 4.1f, 0.5f, Mat.BLOOD);
            edge.emis = 0.75f * g + 0.15f;
            pair(l, edge);
            if (tier >= 2 && k < 3) { // gilded points
                float gx = rx + (tx - rx) * 0.72f, gy = ry + (ty - ry) * 0.72f;
                pair(l, cap(gx, gy, tx, ty, 1.5f, 0.25f, 4.0f, 0.7f, Mat.GOLD));
            }
        }

        // the armoured body
        Part front = cap(0, -19, 0, -1, 1.6f, 9.8f, 2f, 5.6f, Mat.ASURA);
        front.groove = 0.3f;
        Part back = cap(0, -1, 0, 16, 9.8f, 7.0f, 2f, 5.4f, Mat.ASURA);
        back.groove = 0.3f;
        l.add(front);
        l.add(back);
        pair(l, ell(9.8f, -4.0f, 5.6f, 7.0f, 3.4f, 3.8f, Mat.ASURA));     // shoulder boulders
        pair(l, ell(9.8f, -5.6f, 3.0f, 3.6f, 7.0f, 1.2f, Mat.GOLD));      // gilded pauldron caps
        pair(l, ell(10.2f, 8.5f, 4.2f, 5.6f, 3.0f, 3.0f, Mat.ASURA));     // hip guards
        l.add(ell(0, -3.5f, 8.2f, 0.9f, 7.6f, 0.5f, Mat.GOLD));            // gold collar
        l.add(ell(0, 9.5f, 6.6f, 0.9f, 7.6f, 0.5f, Mat.GOLD));             // gold belt

        // the crown of horns, and the tusks round the maw
        float sw = p.flap * 0.6f * (float) Math.sin(p.t + 1f) + splay * 0.12f;
        float[] hn = rotTip(3.4f, -12, 6.5f, -26f * grow, sw);
        Part horn = cap(3.4f, -12, hn[0], hn[1], 2.5f, 0.35f, 3.2f, 2.6f, Mat.ASURA);
        horn.tip = 0.9f;
        pair(l, horn);
        float[] tu = rotTip(2.6f, -14, 5.4f, -30f * grow, -sw);
        Part tusk = cap(2.6f, -14, tu[0], tu[1], 2.0f, 0.3f, 4.6f, 2.2f, Mat.IVORY);
        tusk.tip = 0.7f;
        pair(l, tusk);
        if (tier >= 1) {
            float[] mn = rotTip(1.5f, -10, 2.4f, -24f, -sw * 0.5f);
            pair(l, cap(1.5f, -10, mn[0], mn[1], 1.3f, 0.25f, 5.6f, 1.4f, Mat.IVORY));
            l.add(cap(0, -18, 0, -27f, 0.5f, 1.4f, 4.4f, 1.4f, Mat.GOLD)); // the third horn, gilded
        }
        if (tier >= 2) {
            for (int k = 0; k < 3; k++) {  // the crown's gold spikes
                float cx = 1.3f + k * 1.9f;
                pair(l, cap(cx, -20, cx + 0.6f, -25.5f - k * 0.8f, 0.7f, 0.2f, 7.4f, 0.9f, Mat.GOLD));
            }
        }

        // the maw and the eyes
        Part maw = ell(0, -14.5f, 2.6f, 3.8f, 6.0f, 1.7f, Mat.BLOOD);
        maw.emis = 0.65f * g + 0.3f;
        l.add(maw);
        for (int sd = -1; sd <= 1; sd += 2) {
            Part eye = ell(sd * 3.0f, -8.5f, 2.0f, 0.9f, 6.9f, 0.6f, Mat.BLOOD);
            eye.emis = 0.9f * g + 0.1f;
            l.add(eye);
            l.add(ell(sd * 3.0f, -8.5f, 0.45f, 0.45f, 7.6f, 0.3f, Mat.OBSID));
        }

        // the heart
        float hr = tier == 0 ? 2.8f : tier == 1 ? 3.3f : 3.7f;
        Part core = ell(0, 3, hr, hr * 1.25f, 7f, 1.6f, Mat.BLOOD);
        core.emis = 0.9f * g + 0.1f;
        l.add(core);
        if (tier >= 1) l.add(ring(0, 3, 5.4f, 6.6f, 0.70f, 6.5f, Mat.GOLD));
        for (int k = 0; k < 3; k++) {   // furnace vents down the lower hull
            Part vent = ell(0, 11.5f + k * 2.6f, 4.8f - k * 0.7f, 0.55f, 7.2f, 0.4f, Mat.BLOOD);
            vent.emis = 0.7f * g + 0.2f;
            l.add(vent);
        }

        // lower arms with maces (the throne-king's second pair)
        if (tier >= 2) {
            float ar = 0.25f * (float) Math.sin(p.t + 0.7f);
            float[] ap = rotTip(8.5f, 8, 14f, 17f, ar);
            pair(l, cap(8.5f, 8, ap[0], ap[1], 2.3f, 1.5f, 4.2f, 1.8f, Mat.ASURA));
            pair(l, ell(ap[0] + 0.6f, ap[1] + 1.4f, 2.5f, 2.9f, 5.0f, 1.6f, Mat.GOLD));
        }

        // triple exhaust, blood-red
        l.add(cap(0, 14, 0, 21, 3.5f, 2.7f, 1f, 2.6f, Mat.ASURA));
        pair(l, cap(5.4f, 11, 5.4f, 19, 2.3f, 1.9f, 1f, 2.1f, Mat.ASURA));
        Part f0 = cap(0, 21, 0, 21 + p.flame + 1, 2.7f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
        f0.emis = Math.max(0.3f, g);
        l.add(f0);
        for (int sd = -1; sd <= 1; sd += 2) {
            float len = p.flame - 1 + sd * 0.8f * (float) Math.sin(p.t * 2 + 1.1f);
            Part fl = cap(sd * 5.4f, 19, sd * 5.4f, 19 + Math.max(2, len), 1.9f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
            fl.emis = Math.max(0.3f, g);
            l.add(fl);
        }
        return l;
    }

    // ------------------------------------------------------------------ MARK 7 : PATALA (the thin one)
    // The least ornamented ship in the set, on purpose: a needle of matte black, two very thin swept blades for
    // wings, a single round gold halo, a hair of gold down the spine, a few thin red flaps on the trailing edges,
    // and one great exhaust. Everything else has been taken away. Tier B lengthens the wings and adds a second
    // pair of fine blades, tier C a double halo, a gold lance and two slim side engines.
    static List<Part> mark7(Pose p, int tier) {
        List<Part> l = new ArrayList<>();
        float g = p.glow, flare = p.sweep;
        float grow = tier == 2 ? 1.05f : 1f;
        float sw = p.flap * (float) Math.sin(p.t);

        // the needle
        Part nose = cap(0, -29, 0, -2, 0.4f, 3.4f, 3f, 3.2f, Mat.MATTE);
        nose.groove = 0.2f;
        Part tail = cap(0, -2, 0, 14, 3.4f, 2.7f, 3f, 3.0f, Mat.MATTE);
        tail.groove = 0.2f;
        l.add(nose);
        l.add(tail);
        l.add(cap(0, -26, 0, 8, 0.28f, 0.28f, 6.3f, 0.3f, Mat.GOLD));           // a hair of gold down the spine
        Part slit = ell(0, -13, 0.95f, 3.4f, 6.2f, 0.7f, Mat.RADIANT);          // the cockpit, a slit of gold
        slit.emis = 0.55f * g + 0.1f;
        l.add(slit);

        // the wings: two very thin swept blades
        float[] t1 = rotTip(1.8f, -4, 25 * grow, 21 * grow, sw + 0.15f * flare);
        pair(l, cap(1.8f, -4, t1[0], t1[1], 3.6f, 0.3f, 1.6f, 1.6f, Mat.MATTE));
        pair(l, cap(2.0f, -5.5f, t1[0] - 1.5f, t1[1] - 2.5f, 0.28f, 0.2f, 3.2f, 0.3f, Mat.GOLD));  // its gold edge
        pair(l, cap(1.4f, -16, 9, -9, 1.3f, 0.2f, 2.2f, 1.0f, Mat.MATTE));        // fine canards
        if (tier >= 1) {
            float[] t2 = rotTip(1.8f, 4, 18 * grow, 25 * grow, -sw + 0.15f * flare);
            pair(l, cap(1.8f, 4, t2[0], t2[1], 2.4f, 0.2f, 1.2f, 1.3f, Mat.MATTE));
        }

        // thin red flaps hung on the trailing edges, beating out of step
        int nf = tier == 0 ? 2 : tier == 1 ? 3 : 4;
        for (int k = 0; k < nf; k++) {
            float t = 0.42f + 0.52f * k / Math.max(1, nf - 1);
            float bx = 1.8f + (t1[0] - 1.8f) * t, by = -4 + (t1[1] + 4) * t + 0.6f;
            float th = -0.30f - 0.28f * (float) Math.sin(p.t - k * 0.9f) - 0.35f * flare;
            float len = 5.2f - 0.4f * k;
            pair(l, cap(bx, by, bx + len * (float) Math.sin(th), by + len * (float) Math.cos(th), 1.7f, 0.9f, 0.9f, 1.2f, Mat.REDF));
        }

        // the halo: one thin round ring of gold around the waist (two at tier C)
        l.add(ring(0, -3, 7.2f, 7.0f, 0.90f, 5.4f, Mat.GOLD));
        if (tier >= 2) l.add(ring(0, -3, 10.2f, 9.8f, 0.93f, 4.6f, Mat.GOLD));

        // one great exhaust: a slim nozzle and an enormous flame
        l.add(ring(0, 14.6f, 3.4f, 1.8f, 0.70f, 3.6f, Mat.GOLD));
        Part gl = ell(0, 14.8f, 2.4f, 1.1f, 3.8f, 0.5f, Mat.RADIANT);
        gl.emis = 0.9f * g;
        l.add(gl);
        float len = p.flame * 1.45f + 5.5f + 0.9f * (float) Math.sin(p.t * 2 + 0.6f);
        Part fl = cap(0, 15, 0, 15 + Math.max(4, len), 3.9f, 0.2f, 0.6f, 1.5f, Mat.FLAME);
        fl.emis = Math.max(0.3f, g);
        l.add(fl);

        if (tier >= 2) { // a gold lance ahead of the nose, and two slim side engines
            l.add(cap(0, -34, 0, -30.5f, 0.2f, 0.7f, 5.4f, 0.8f, Mat.GOLD));
            for (int sd = -1; sd <= 1; sd += 2) {
                float ex = sd * 6.0f;
                l.add(cap(ex, 4, ex, 15, 1.7f, 1.4f, 1.6f, 1.6f, Mat.MATTE));
                Part f2 = cap(ex, 15.5f, ex, 15.5f + Math.max(3, p.flame * 0.8f + 2.5f), 1.7f, 0.2f, 0.6f, 1.2f, Mat.FLAME);
                f2.emis = Math.max(0.3f, g);
                l.add(f2);
            }
        }
        return l;
    }

    static int hue(float h) {
        h = h - (float) Math.floor(h);
        float r = Math.max(0, Math.min(1, Math.abs(h * 6 - 3) - 1)), gg = Math.max(0, Math.min(1, 2 - Math.abs(h * 6 - 2))), b = Math.max(0, Math.min(1, 2 - Math.abs(h * 6 - 4)));
        return ((int) (80 + 175 * r) << 16) | ((int) (80 + 175 * gg) << 8) | (int) (80 + 175 * b);
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
                } else if (m == Mat.IVORY || m == Mat.SAFFRON || m == Mat.LOTUS) {   // soft, sunlit, richly lit
                    t = 0.22f + 0.78f * diff;
                    if (m == Mat.IVORY) tint = Math.max(tint, (float) Math.pow(1 - nz, 1.5) * 0.75f);
                    if (diff > 0.94f) t += 0.10f;
                } else if (m == Mat.VENOM || m == Mat.SULPHUR) {
                    t = 0.10f + 0.82f * diff;
                    if (m == Mat.VENOM) tint = Math.max(tint, (float) Math.pow(1 - nz, 1.6) * 0.7f);
                    if (diff > 0.9f) t += 0.14f;
                } else if (m == Mat.ASURA) {   // black-red armour: dark body, hard glints, blood at the rim
                    t = 0.04f + 0.66f * diff + 0.5f * Math.max(0, diff - 0.78f) / 0.22f;
                    tint = Math.max(tint, (float) Math.pow(1 - nz, 1.5) * 0.85f);
                } else if (m == Mat.MATTE) {   // matte black: it takes the light and gives nothing back. no glints, no sheen
                    t = 0.18f + 0.54f * diff;
                    tint = 0;
                } else if (m == Mat.VOID) {    // the serpent's dark: deep violet-black, bright specular, amethyst edge
                    t = 0.14f + 0.62f * diff + 0.5f * Math.max(0, diff - 0.78f) / 0.22f;
                    tint = Math.max(tint, (float) Math.pow(1 - nz, 1.4) * 0.9f);
                } else if (m == Mat.BLOOD || m == Mat.RUBY || m == Mat.EMER || m == Mat.SAPH || m == Mat.AMETH) {
                    t = Math.min(1f, 0.30f + b.em[i] * 0.70f + diff * 0.20f);   // the lit jewels and veins
                } else if (m == Mat.ACID) {
                    t = Math.min(1f, 0.30f + b.em[i] * 0.70f + diff * 0.20f);
                } else if (m == Mat.VIOLET || m == Mat.BRASS || m == Mat.SILVER) {
                    t = 0.10f + 0.82f * diff;
                    if (m == Mat.VIOLET) tint = Math.max(tint, (float) Math.pow(1 - nz, 1.5) * 0.8f);
                    if (diff > 0.9f) t += 0.14f;
                } else if (m == Mat.TEAL) {
                    t = Math.min(1f, 0.30f + b.em[i] * 0.70f + diff * 0.20f);
                } else if (m == Mat.RADIANT) {
                    t = Math.min(1f, 0.38f + b.em[i] * 0.55f + diff * 0.18f);
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
                    if (m == Mat.CRYSTAL.ordinal() || m == Mat.FLAME.ordinal() || m == Mat.MOLTEN.ordinal() || m == Mat.RADIANT.ordinal() || m == Mat.TEAL.ordinal() || m == Mat.ACID.ordinal()
                            || m == Mat.BLOOD.ordinal() || m == Mat.RUBY.ordinal() || m == Mat.EMER.ordinal() || m == Mat.SAPH.ordinal() || m == Mat.AMETH.ordinal()) emi += b.em[i];
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
        postEdge(out, solid, emiMap);
        return finish(out, solid, emiMap, po);
    }

    static float nb(Buf b, int i, float fallback) {
        if (i < 0 || i >= b.h.length || b.mat[i] < 0) return fallback - 1.2f;   // silhouette falls away
        return b.h[i];
    }

    /** lit edges (top/left) get a rim highlight, shadowed edges (bottom/right) a dark rim: instant volume. */
    static void postEdge(int[] out, boolean[] solid, float[] emi) {
        int[] copy = out.clone();
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            int i = y * N + x;
            if (!solid[i] || emi[i] > 0.12f) continue;   // light sources are never shadowed
            boolean dark = (x + 1 >= N || !solid[i + 1]) || (y + 1 >= N || !solid[i + N]);
            boolean lit = (x == 0 || !solid[i - 1]) || (y == 0 || !solid[i - N]);
            if (CUR == 7 && lit && !dark) copy[i] = rim7(out[i], true);   // the matte hull keeps a thin pale edge
            else if (dark && !lit) copy[i] = mul(out[i], 0.55f);
            else if (lit && !dark) copy[i] = CUR == 2 ? lift(out[i], 30) : CUR == 4 ? lift(out[i], 24) : CUR == 5 ? lift(out[i], 22) : CUR == 6 ? lift(out[i], 28) : CUR == 7 ? mul(out[i], 1.22f) : mul(out[i], 1.08f);
            else if (dark) copy[i] = mul(out[i], 0.78f);
        }
        System.arraycopy(copy, 0, out, 0, out.length);
    }

    /** Mark 7's rim: the hull is matte black and would vanish on a dark field, so every edge pixel is lifted toward a
     *  pale silver-gold: strongest on the lit edge, fainter on the shadowed one. */
    static int rim7(int c, boolean litEdge) {
        int[] t = {0xd8, 0xd0, 0xb8};
        float k = litEdge ? 0.16f : 0.08f;
        int r = (int) ((c >> 16 & 255) + (t[0] - (c >> 16 & 255)) * k);
        int g = (int) ((c >> 8 & 255) + (t[1] - (c >> 8 & 255)) * k);
        int b = (int) ((c & 255) + (t[2] - (c & 255)) * k);
        return 0xFF000000 | r << 16 | g << 8 | b;
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
                if (f > 0) g[yy * N + xx] += e * f * (CUR == 2 ? 0.30f : CUR == 3 ? 0.26f : CUR == 4 ? 0.24f : CUR == 5 ? 0.24f : CUR == 6 ? 0.28f : CUR == 7 ? 0.26f : 0.22f);
            }
        }
        int[] res = out.clone();
        if (CUR == 7) { // a faint pale halo hugging the silhouette, so the black hull reads on a dark field
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                int i = y * N + x;
                if (solid[i]) continue;
                int near = 0;   // 1 = touching the hull, 2 = one pixel out
                for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                    int xx = x + dx, yy = y + dy;
                    if (xx < 0 || yy < 0 || xx >= N || yy >= N || !solid[yy * N + xx]) continue;
                    int d = Math.max(Math.abs(dx), Math.abs(dy));
                    if (near == 0 || d < near) near = d;
                }
                if (near == 1) res[i] = 52 << 24 | 0xd8d4c0;
            }
        }
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
                if (old < add) res[i] = add << 24 | (CUR == 2 ? 0xff7a1a : CUR == 3 ? 0xffd070 : CUR == 4 ? 0x2fd6c8 : CUR == 5 ? 0xb4f03a : CUR == 6 ? 0xff2a2a : CUR == 7 ? 0xffc84a : 0x45d4ee);
            }
        }
        // embers shed from the exhausts, falling away behind the ship (Vitala)
        if (po.embers > 0 && CUR == 7) {   // gold sparks streaming back off the great exhausts
            int n = (int) (7 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n + (k % 2) * 0.12f) % 1f;
                float side = k % 2 == 0 ? 1 : -1;
                float x0 = 32 + side * (5 + ((k * 3) % 7)) + (float) Math.sin((ph + k) * 4.4) * 1.4f + po.bank * 2.5f;
                float y = 42 + ph * 18f;
                int a = (int) (255 * (1 - ph) * (1 - ph * 0.3f));
                int col = ph < 0.3f ? 0xfff2b0 : ph < 0.6f ? 0xffc84a : 0xd88a1c;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
            }
        } else if (po.embers > 0 && CUR == 6) {   // cinders and drops of blood falling off the blades
            int n = (int) (7 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n + (k % 2) * 0.13f) % 1f;
                float side = k % 2 == 0 ? 1 : -1;
                float x0 = 32 + side * (11 + ((k * 5) % 13)) + (float) Math.sin((ph + k) * 3.3) * 1.2f + po.bank * 2f;
                float y = 14 + ph * 36f;
                int a = (int) (240 * Math.sin(Math.PI * ph));
                int col = k % 3 == 0 ? 0xff3a2e : k % 3 == 1 ? 0xffa040 : 0xffe9a0;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
            }
        } else if (po.embers > 0 && CUR == 5) {   // toxic spores rising off the shoulders
            int n = (int) (6 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n + (k % 2) * 0.15f) % 1f;
                float side = k % 2 == 0 ? 1 : -1;
                float x0 = 32 + side * (13 + ((k * 7) % 9)) + (float) Math.sin((ph + k) * 4.0) * 1.5f + po.bank * 2f;
                float y = 50 - ph * 34f;
                int a = (int) (230 * Math.sin(Math.PI * ph));
                int col = k % 3 == 0 ? 0xd9ff5c : k % 3 == 1 ? 0xf3ee6e : 0x9be12b;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
            }
        } else if (po.embers > 0 && CUR == 4) {   // cold wisps drifting off the tendril
            int n = (int) (5 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n + (k % 2) * 0.17f) % 1f;
                float x0 = 20 - ph * 8f + (float) Math.sin((ph + k) * 4.0) * 1.8f - po.bank * 2f + (k % 3) * 2f;
                float y = 40 + ph * 20f + (k * 7 % 5);
                int a = (int) (230 * Math.sin(Math.PI * ph));
                int col = k % 2 == 0 ? 0x62dcd0 : 0xb08ee0;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
            }
        } else if (po.embers > 0 && CUR == 3) {   // golden pollen rising around the ship
            int n = (int) (7 * po.embers);
            for (int k = 0; k < n; k++) {
                float ph = (po.loop + k / (float) n + (k % 3) * 0.13f) % 1f;
                float x0 = 32 + (((k * 29) % 41) - 20) * 0.85f + (float) Math.sin((ph + k) * 4.3) * 1.6f + po.bank * 3f;
                float y = 56 - ph * 46f;
                int a = (int) (255 * Math.sin(Math.PI * ph));
                int col = k % 3 == 0 ? 0xfff0a0 : k % 3 == 1 ? 0xffc4d8 : 0xffffff;
                put(res, Math.round(x0), Math.round(y), a << 24 | col);
            }
        } else if (po.embers > 0) {
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
            for (int[] s : sp[po.sparks]) put(res, s[0], s[1], CUR == 2 ? 0xFFFFE9A8 : CUR == 3 ? 0xFFFFF0B0 : CUR == 4 ? 0xFFD8FFF4 : CUR == 5 ? 0xFFF4FFB0 : CUR == 6 ? 0xFFFFC8A0 : 0xFFFFFFFF);
            if (po.sparks <= 1) for (int[] s : sp[po.sparks]) { put(res, s[0] + 1, s[1], CUR == 2 ? 0xCCff8f26 : CUR == 3 ? 0xCCffcf5a : CUR == 4 ? 0xCC62dcd0 : CUR == 5 ? 0xCCb4f03a : 0xCC7be6f0); put(res, s[0], s[1] + 1, CUR == 2 ? 0x99ff8f26 : CUR == 3 ? 0x99ffc4d8 : CUR == 4 ? 0x99b08ee0 : CUR == 5 ? 0x99f3ee6e : 0x997be6f0); }
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

    static final String[] LOKA = {"", "Atala", "Vitala", "Sutala", "Talatala", "Mahatala", "Rasatala", "Patala"};
    static final int MARKS = 7;

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
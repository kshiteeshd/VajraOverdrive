"""THE ROUND KIT: how a Vitala creature is built. Every creature is a handful of shaded SPHERES, rings, crescents and glows, drawn back to front on a 64 x 64 canvas, so that the silhouette
is round and the light falls on it like it falls on a ball (upper left, four cel tones, a lit rim). A creature is a function of a State (the loop's phase, the action clip it is in and how far
through it is, its rage, its rank, whether it has been hit), so one drawing gives every frame of every row of its sheet.

Sheet (1024 x 832, cells of 64): row 0 idle x8 + rage x8 | row 1 hit x2 + death x6 | row 2 leader | row 3 elite | rows 4-6 three ACTION clips of 16 frames | rows 7-9 the same as a leader |
rows 10-12 the same as an elite. The renderer plays the idle loop and, every few seconds, one of the action clips (EnemyImages.cellV), out of step from unit to unit.
No randomness: the same run writes the same bytes.
"""
import math

import numpy as np
from PIL import Image

S = 64
YY, XX = np.mgrid[0:S, 0:S]
PX = XX + 0.5
PY = YY + 0.5
INK = np.array([36, 14, 8], float)

# ---- palettes: four tones, dark to light
BASALT = [(30, 22, 28), (56, 44, 52), (92, 74, 78), (140, 116, 110)]
ASH = [(50, 48, 54), (88, 86, 94), (128, 126, 134), (176, 174, 182)]
EMBER = [(122, 32, 14), (196, 64, 22), (255, 124, 34), (255, 206, 96)]
COPPER = [(72, 34, 18), (130, 68, 32), (192, 112, 54), (242, 172, 102)]
GOLD = [(112, 68, 14), (182, 122, 26), (242, 178, 42), (255, 232, 132)]
GHOST = [(66, 98, 150), (126, 168, 220), (204, 230, 255), (246, 252, 255)]
ROBE = [(30, 38, 66), (58, 74, 114), (98, 120, 168), (152, 178, 222)]
STEEL = [(40, 36, 46), (78, 72, 88), (122, 114, 130), (178, 170, 186)]
FIRE = [(190, 50, 20), (255, 110, 30), (255, 190, 80), (255, 244, 190)]
HOT = [(190, 60, 30), (255, 110, 40), (255, 190, 100), (255, 245, 200)]
SKIN = [(120, 70, 50), (176, 112, 80), (222, 160, 118), (250, 206, 164)]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def heat(ramp, k):
    """The same tones read hotter: k = 0 as drawn, 1 white-hot (the rage skin)."""
    return [lerp(r, h, k) for r, h in zip(ramp, HOT)]


class State:
    def __init__(self, t=0.0, clip=None, u=0.0, rage=0.0, variant=0, hit=0, dying=-1):
        self.t, self.clip, self.u, self.rage, self.variant, self.hit, self.dying = t, clip, u, rage, variant, hit, dying

    def s(self, k=1, ph=0.0):
        return math.sin(2 * math.pi * (k * self.t + ph))

    def c(self, k=1, ph=0.0):
        return math.cos(2 * math.pi * (k * self.t + ph))

    def env(self, a=0.0, b=1.0):
        """0 -> 1 -> 0 over [a, b] of the clip (a smooth bump); 0 outside it or with no clip."""
        if self.clip is None or self.u < a or self.u > b:
            return 0.0
        x = (self.u - a) / (b - a)
        return math.sin(math.pi * x) ** 2

    def ramp(self, r):
        return heat(r, 0.62 * self.rage) if self.rage > 0 else r

    def acting(self, clip):
        return self.clip == clip


class Frame:
    def __init__(self):
        self.rgb = np.zeros((S, S, 3), float)
        self.a = np.zeros((S, S), float)
        self.glow = np.zeros((S, S, 3), float)
        self.back = np.zeros((S, S), bool)       # what the outline is built from

    # ---- solids -------------------------------------------------------------------------------
    def ball(self, cx, cy, rx, ry=None, ramp=BASALT, rot=0.0, light=(-0.52, -0.58), tex=None, tint=None, cut=None, outline=True):
        """A shaded sphere (or ellipsoid). `tex(nx, ny, nz)` may return an (S, S) array of extra emissive (0..1) pixels (cracks, runes) painted in the lightest tone of `ramp2`;
        `cut` is a boolean mask removed from it (a mouth, a window)."""
        ry = rx if ry is None else ry
        c, s_ = math.cos(rot), math.sin(rot)
        dx, dy = PX - cx, PY - cy
        nx = (dx * c + dy * s_) / rx
        ny = (-dx * s_ + dy * c) / ry
        r2 = nx * nx + ny * ny
        m = r2 <= 1.0
        if cut is not None:
            m &= ~cut
        nz = np.sqrt(np.clip(1.0 - r2, 0, 1))
        ll = math.sqrt(light[0] ** 2 + light[1] ** 2 + 0.64)
        lam = (nx * light[0] + ny * light[1] + nz * 0.8) / ll
        idx = np.zeros((S, S), int)
        idx[lam > -0.25] = 1
        idx[lam > 0.22] = 2
        idx[lam > 0.62] = 3
        idx[(nz < 0.20) & (lam < 0.1)] = 0                       # the dark limb
        rimlit = (nz < 0.24) & (lam > 0.0)
        idx[rimlit] = 3
        col = np.zeros((S, S, 3))
        for i in range(4):
            col[idx == i] = ramp[i]
        if tint is not None:
            col = col * (1 - tint[3]) + np.array(tint[:3]) * tint[3]
        if outline:
            self.back |= m
        self.rgb[m] = col[m]
        self.a[m] = 1.0
        if tex is not None:
            e = tex(nx, ny, nz) * m
            self.glow += (e[:, :, None] * np.array(EMBER[3])[None, None, :] / 255.0)
            self.rgb[e > 0.5] = np.array(EMBER[3])
        return m

    def ring(self, cx, cy, r, w, ramp=GOLD, arc=None, outline=True):
        d = np.sqrt((PX - cx) ** 2 + (PY - cy) ** 2)
        m = np.abs(d - r) <= w / 2.0
        if arc is not None:
            ang = np.arctan2(PY - cy, PX - cx)
            lo, hi = arc
            span = (ang - lo) % (2 * math.pi)
            m &= span <= (hi - lo) % (2 * math.pi)
        lamb = ((PX - cx) * -0.5 + (PY - cy) * -0.58) / max(1.0, r)
        col = np.zeros((S, S, 3))
        col[:] = ramp[1]
        col[lamb > 0.25] = ramp[2]
        col[lamb > 0.7] = ramp[3]
        col[lamb < -0.55] = ramp[0]
        if outline:
            self.back |= m
        self.rgb[m] = col[m]
        self.a[m] = 1.0

    def seg(self, x0, y0, x1, y1, w, color, outline=True, glow=0.0):
        vx, vy = x1 - x0, y1 - y0
        L2 = vx * vx + vy * vy + 1e-9
        t = np.clip(((PX - x0) * vx + (PY - y0) * vy) / L2, 0, 1)
        d = np.sqrt((PX - (x0 + t * vx)) ** 2 + (PY - (y0 + t * vy)) ** 2)
        m = d <= w / 2.0
        if outline:
            self.back |= m
        self.rgb[m] = color
        self.a[m] = 1.0
        if glow > 0:
            self.glow += (np.exp(-(d / (w * 1.5 + 1)) ** 2) * glow)[:, :, None] * np.array(color)[None, None, :] / 255.0

    def spot(self, cx, cy, r, color, outline=False):
        m = (PX - cx) ** 2 + (PY - cy) ** 2 <= r * r
        if outline:
            self.back |= m
        self.rgb[m] = color
        self.a[m] = 1.0
        return m

    def blob(self, mask, color, outline=False):
        if outline:
            self.back |= mask
        self.rgb[mask] = color
        self.a[mask] = 1.0

    def halo(self, cx, cy, r, color, k=1.0):
        d2 = ((PX - cx) ** 2 + (PY - cy) ** 2) / (r * r)
        g = np.exp(-d2 * 2.2) * k
        self.glow += g[:, :, None] * np.array(color)[None, None, :] / 255.0

    def ellipse_mask(self, cx, cy, rx, ry):
        return ((PX - cx) / rx) ** 2 + ((PY - cy) / ry) ** 2 <= 1.0

    # ---- finishing ----------------------------------------------------------------------------
    def finish(self, glow_gain=0.55):
        """The outline (a dark ember-brown, one pixel, outside the drawing), then the glow laid on top additively; returns RGBA uint8."""
        m = self.a > 0.5
        ring = ((self.back | m) & ~m)
        grown = self.back.copy()
        edge = np.zeros((S, S), bool)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx or dy:
                    edge |= np.roll(np.roll(grown, dy, 0), dx, 1)
        edge &= ~grown
        edge &= ~m
        rgb = self.rgb.copy()
        a = self.a.copy()
        rgb[edge] = INK
        a[edge] = 1.0
        g = np.clip(self.glow * glow_gain, 0, 1.2)
        rgb = np.clip(rgb + g * 255.0 * (a[:, :, None] > 0.4) * 0.30 + g * 70.0, 0, 255)
        ga = np.clip(g.max(axis=2) * 0.9, 0, 1)
        a = np.maximum(a, ga)
        out = np.zeros((S, S, 4), np.uint8)
        out[:, :, :3] = rgb.astype(np.uint8)
        out[:, :, 3] = (np.clip(a, 0, 1) * 255).astype(np.uint8)
        return out


# ============================================================================================================================ ranks, hits, deaths
def flame_crown(f, cx, top, st, size=1.0):
    """A leader's three tongues of flame (the three sacrificial fires)."""
    for i, dx in enumerate((-5, 0, 5)):
        h = (7 if i == 1 else 5) * size + 1.5 * math.sin(2 * math.pi * (st.t * (2 + i) + i * 0.3))
        sway = 1.2 * math.sin(2 * math.pi * (st.t * 2 + i * 0.4))
        pts = [(cx + dx * size - 2.2, top + 0.5), (cx + dx * size + sway, top - h), (cx + dx * size + 2.2, top + 0.5)]
        im = Image.new('L', (S, S), 0)
        from PIL import ImageDraw
        ImageDraw.Draw(im).polygon([(round(x), round(y)) for x, y in pts], fill=255)
        mk = np.array(im) > 0
        f.blob(mk, FIRE[1], outline=True)
        core = f.ellipse_mask(cx + dx * size + sway * 0.5, top - h * 0.35, 1.2, h * 0.30) & mk
        f.blob(core, FIRE[3])
        f.halo(cx + dx * size, top - h * 0.5, 7, FIRE[2], 0.45)


def elite_halo(f, cx, cy, r, st):
    """An elite: a thin broken halo above and a glow at each flank."""
    f.ring(cx, cy - r - 3.5, 9, 1.6, GOLD, arc=(math.pi * 1.12 + 0.1 * st.s(1), math.pi * 1.88 + 0.1 * st.s(1)))
    for sx in (-1, 1):
        f.halo(cx + sx * (r + 1), cy + 2, 8, FIRE[2], 0.55 + 0.2 * st.s(2, 0.25 * sx))


def white_hit(rgba, k):
    """A hit throws the silhouette white (k = 1) then scalds it back (k < 1)."""
    out = rgba.copy().astype(float)
    m = out[:, :, 3] > 0
    for c in range(3):
        out[:, :, c] = np.where(m, out[:, :, c] * (1 - k) + 255 * k, out[:, :, c])
    return out.astype(np.uint8)


def death_frame(rgba, i, n=6):
    """Cinders, not glass: the hull flashes white, its seams blow open, the stone goes ember-red and rises as sparks on a fixed dither; four-point glints are what is left."""
    src = rgba.astype(float)
    m = src[:, :, 3] > 0
    k = (i + 1) / n
    out = np.zeros_like(src)
    # a fixed dither: each pixel has a threshold, the same every run
    thr = ((XX * 37 + YY * 91) % 97) / 97.0
    keep = thr > k * 1.05 - 0.05
    ember = np.array(EMBER[1 + (1 if i < 3 else 0)], float)
    col = src[:, :, :3] * (1 - min(1.0, k * 1.2)) + ember * min(1.0, k * 1.2)
    if i == 0:
        col = src[:, :, :3] * 0.2 + 255 * 0.8
    out[:, :, :3] = col
    out[:, :, 3] = np.where(m & keep, 255, 0)
    # sparks rise: the dissolved pixels drift up by i * 2 px
    gone = m & ~keep
    ys, xs = np.nonzero(gone)
    for y, x in zip(ys, xs):
        if (x * 13 + y * 7) % 5 == 0:
            ny = y - int(2 + i * 2.2 + (x % 3))
            if 0 <= ny < S:
                out[ny, x, :3] = EMBER[2 if i < 4 else 1]
                out[ny, x, 3] = max(out[ny, x, 3], 255 * (1 - k * 0.8))
    if i >= 3:                                                 # the four-point glints
        for gx, gy in ((22, 24), (41, 30), (31, 40)):
            if (gx + gy + i) % 2 == 0 or i == 3:
                for d in range(-2, 3):
                    for (xx, yy) in ((gx + d, gy), (gx, gy + d)):
                        if 0 <= xx < S and 0 <= yy < S:
                            out[yy, xx, :3] = 255
                            out[yy, xx, 3] = 255 * (1 - (i - 3) * 0.3)
    return out.astype(np.uint8)


# ============================================================================================================================ the sheet
CLIPS = ('A', 'B', 'C')


def build_sheet(draw, cols=16):
    """draw(State) -> Frame. Returns a PIL image of the whole sheet."""
    sheet = Image.new('RGBA', (cols * S, 13 * S), (0, 0, 0, 0))

    def put(row, col, rgba):
        sheet.paste(Image.fromarray(rgba, 'RGBA'), (col * S, row * S))

    for variant, row in ((0, 0), (1, 2), (2, 3)):
        for i in range(8):
            put(row, i, draw(State(t=i / 8.0, variant=variant)).finish())
            put(row, 8 + i, draw(State(t=i / 8.0, variant=variant, rage=1.0)).finish())
    base = draw(State(t=0.0, variant=0)).finish()
    put(1, 0, white_hit(base, 1.0))
    put(1, 1, white_hit(base, 0.45))
    rage_base = draw(State(t=0.0, variant=0, rage=1.0)).finish()
    for i in range(6):
        put(1, 2 + i, death_frame(rage_base if i > 1 else base, i))
    for variant, row0 in ((0, 4), (1, 7), (2, 10)):
        for ci, clip in enumerate(CLIPS):
            for i in range(16):
                put(row0 + ci, i, draw(State(t=(i % 8) / 8.0, clip=clip, u=i / 16.0, variant=variant)).finish())
    return sheet

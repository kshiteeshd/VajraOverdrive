"""Pixel kit for the enemy sheets: a palette in the loka's colour code, a canvas of palette
letters, shaded solids, hand-drawn ASCII stamps, and the shared sheet logic (rage skin, hit,
gold-glass death, ranks).  No randomness anywhere: every cut, glint and dissolve is a fixed,
designed value, so the same run always writes the same bytes.

A sprite is drawn on a 64x64 canvas facing DOWN (toward the player) and symmetrical about the
line between x = 31 and x = 32.  Stamps are written as the LEFT half only, right-aligned: the last
character of each row touches the centre line, and `mirror=True` paints the other half.
"""
import numpy as np
from PIL import Image

S = 64
CX = 31.5  # the mirror axis

# ----------------------------------------------------------------- the colour code ---------
# Atala: pearl and gold.  Everything below is sampled from the shipped Atala art (the old enemy
# sprites, the wall tiles, the Mk-1 hull) - see docs/ENEMY_BIBLE.md.
PAL = {
    "K": (26, 26, 30),      # aperture / outline
    "H": (255, 255, 244),   # hot highlight
    "W": (255, 252, 241),   # pearl, lit rim
    "P": (232, 230, 224),   # pearl
    "S": (207, 207, 207),   # mist shade
    "D": (166, 164, 176),   # cool deep shade
    "C": (255, 244, 216),   # cream
    "c": (230, 223, 208),   # warm shade
    "G": (255, 215, 0),     # gold
    "g": (254, 194, 61),    # gold mid
    "B": (184, 134, 11),    # bronze
    "A": (133, 77, 0),      # amber deep
    "L": (217, 208, 232),   # Akasha lavender
    "l": (160, 140, 220),   # lavender deep
    "E": (255, 86, 34),     # ember (rage only)
    "e": (190, 52, 22),     # ember dim
    # the friendly blues: a person of the loka is lit from within, never slit-eyed
    "h": (226, 246, 255),   # lamp white-blue
    "n": (150, 205, 255),   # sky
    "N": (64, 116, 206),    # deep sky
}
# The rage skin: the same drawing, read hotter.  Pearl goes cream-hot, gold goes white-hot.
RAGE = {
    "H": (255, 255, 235), "W": (255, 240, 206), "P": (255, 206, 142), "S": (250, 152, 86),
    "D": (192, 86, 46), "C": (255, 232, 182), "c": (255, 192, 122), "G": (255, 247, 196),
    "g": (255, 208, 104), "B": (255, 124, 52), "A": (190, 70, 30), "L": (255, 176, 120),
    "l": (232, 104, 62), "K": (34, 18, 20),
}
KEYS = list(PAL.keys())
IDX = {k: i + 1 for i, k in enumerate(KEYS)}   # 0 = empty
OUTLINE = IDX["K"]


class Canvas:
    def __init__(self):
        self.g = np.zeros((S, S), dtype=np.uint8)

    # ---- masks -------------------------------------------------------------------------
    @staticmethod
    def _ellipse(cx, cy, rx, ry):
        ys, xs = np.mgrid[0:S, 0:S]
        return ((xs + 0.5 - cx) / rx) ** 2 + ((ys + 0.5 - cy) / ry) ** 2 <= 1.0

    @staticmethod
    def dilate(mask, n=1):
        m = mask.copy()
        for _ in range(n):
            m = m | np.roll(m, 1, 0) | np.roll(m, -1, 0) | np.roll(m, 1, 1) | np.roll(m, -1, 1)
        return m

    @staticmethod
    def _poly(pts):
        im = Image.new("L", (S, S), 0)
        from PIL import ImageDraw
        ImageDraw.Draw(im).polygon([(round(x), round(y)) for x, y in pts], fill=255)
        return np.array(im) > 0

    # ---- shaded solids -----------------------------------------------------------------
    def solid(self, mask, ramp="WPSD", light="sym"):
        """Fill `mask` with a 4-tone cel shade: a 1 px lit rim toward the light, the body, a shade
        band and a 1 px deep rim away from it.  ramp = lit, body, shade, deep.  light="sym" is
        top light that wraps both lower flanks, so a mirrored body shades identically on both
        sides; light="tl" is a single light from the top-left."""
        lit, body, shade, deep = (IDX[c] for c in ramp)
        toward = [(-1, 0), (-1, -1), (-1, 1)] if light == "sym" else [(-1, -1)]
        away = [(1, 0), (1, -1), (1, 1)] if light == "sym" else [(1, 1)]

        def steps(dirs):
            best = np.full((S, S), 9, dtype=np.int8)
            for dy, dx in dirs:
                run = np.zeros((S, S), dtype=np.int8)
                for k in range(1, 6):
                    inside = np.roll(np.roll(mask, -k * dy, 0), -k * dx, 1)
                    first = mask & (run == 0) & ~inside
                    run[first] = k
                run[mask & (run == 0)] = 6
                best = np.minimum(best, run)
            return best

        up, dn = steps(toward), steps(away)
        tone = np.full((S, S), body, dtype=np.uint8)
        tone[mask & (dn <= 3)] = shade
        tone[mask & (dn <= 1)] = deep
        tone[mask & (up <= 1)] = lit
        self.g[mask] = tone[mask]

    def ellipse(self, cx, cy, rx, ry, ramp="WPSD", light="sym"):
        self.solid(self._ellipse(cx, cy, rx, ry), ramp, light)

    def poly(self, pts, ramp="WPSD", light="sym"):
        self.solid(self._poly(pts), ramp, light)

    def mpoly(self, pts, ramp="WPSD", light="sym"):
        """A polygon and its mirror image, shaded as one body."""
        self.solid(self._poly(pts) | self._poly([(63 - x, y) for x, y in pts]), ramp, light)

    def flat(self, mask, letter):
        self.g[mask] = IDX[letter]

    def fpoly(self, pts, letter):
        self.flat(self._poly(pts), letter)

    def fmpoly(self, pts, letter):
        self.flat(self._poly(pts) | self._poly([(63 - x, y) for x, y in pts]), letter)

    def fellipse(self, cx, cy, rx, ry, letter):
        self.flat(self._ellipse(cx, cy, rx, ry), letter)

    def px(self, x, y, letter):
        if 0 <= x < S and 0 <= y < S:
            self.g[int(y), int(x)] = IDX[letter]

    def line(self, x0, y0, x1, y1, letter):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) or 1
        for i in range(n + 1):
            self.px(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), letter)

    # ---- hand-drawn stamps -------------------------------------------------------------
    def stamp(self, rows, x, y, mirror=True):
        """rows: list of strings, '.' = leave alone.  Written as the left half; the last column
        sits at x + len - 1, so pass x = 32 - len to touch the centre line."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == ".":
                    continue
                self.px(x + i, y + j, ch)
                if mirror:
                    self.px(63 - (x + i), y + j, ch)

    def stamp_c(self, rows, y, mirror=True):
        """Stamp left-half rows flush against the centre line."""
        w = max(len(r) for r in rows)
        self.stamp([r.rjust(w, ".") for r in rows], 32 - w, y, mirror)

    # ---- finishing ---------------------------------------------------------------------
    def alpha(self):
        return self.g != 0

    def bbox(self):
        ys, xs = np.where(self.alpha())
        return xs.min(), ys.min(), xs.max(), ys.max()

    def outline(self):
        a = self.alpha()
        ring = (np.roll(a, 1, 0) | np.roll(a, -1, 0) | np.roll(a, 1, 1) | np.roll(a, -1, 1)) & ~a
        self.g[ring] = OUTLINE

    def rgba(self, rage=False, glow_boost=0):
        out = np.zeros((S, S, 4), dtype=np.uint8)
        lut = np.zeros((len(KEYS) + 1, 3), dtype=np.uint8)
        for k, i in IDX.items():
            lut[i] = RAGE.get(k, PAL[k]) if rage else PAL[k]
        m = self.alpha()
        out[m, :3] = lut[self.g[m]]
        out[m, 3] = 255
        return out

    def copy(self):
        c = Canvas()
        c.g = self.g.copy()
        return c


# ----------------------------------------------------------------- ranks -------------------
CREST = [  # a gold lotus crest, left half, flush to the centre line
    "......G",
    ".....gGG",
    "..G.gGGH",
    "..gGGGgG",
    "...BgGGG",
    "....BBgg",
]
VENTS = [  # elite: three gold vent slits down each flank
    "KGGK",
    "KGGK",
]


def rank_layers(c, rank, top, left, right, mid):
    """Crown at the brow for a leader; an elite adds vents on the flanks and a halo."""
    if rank >= 1:
        c.stamp_c(CREST, max(0, top - 5))
    if rank >= 2:
        for k, dy in enumerate((-9, 0, 9)):
            c.stamp(VENTS, left - 1, mid + dy, mirror=False)
            c.stamp(VENTS, right - 2, mid + dy, mirror=False)


def halo(arr):
    """A thin broken gold ring behind an elite: eight arcs, drawn in fixed positions."""
    ring = np.zeros_like(arr)
    gold = PAL["g"] + (170,)
    cx = cy = 31.5
    r = 30.0
    import math
    for k in range(64):
        if (k // 4) % 2:
            continue  # dashes
        a = k * 2 * math.pi / 64
        x, y = int(round(cx + math.cos(a) * r)), int(round(cy + math.sin(a) * r))
        if 0 <= x < S and 0 <= y < S:
            ring[y, x] = gold
    ring = np.where(arr[:, :, 3:4] > 0, arr, ring)
    return ring


# ----------------------------------------------------------------- hit and death ---------
def shift(arr, dx, dy):
    out = np.zeros_like(arr)
    ys, xs = np.where(arr[:, :, 3] > 0)
    for y, x in zip(ys, xs):
        nx, ny = x + dx, y + dy
        if 0 <= nx < S and 0 <= ny < S:
            out[ny, nx] = arr[y, x]
    return out


def white(arr, k):
    out = arr.copy()
    m = arr[:, :, 3] > 0
    out[m, :3] = (arr[m, :3] * (1 - k) + np.array([255, 253, 246]) * k).astype(np.uint8)
    return out


def scorch(arr):
    """Hit frame 2: the pearl settles with a gold scald across the struck side."""
    out = arr.copy()
    ys, xs = np.where(arr[:, :, 3] > 0)
    top = ys.min()
    for y, x in zip(ys, xs):
        if y < top + 5 and (x + y) % 3 == 0:
            out[y, x, :3] = PAL["g"]
    return out


BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0
GLINT = [  # a four-point glint
    "..G..",
    "..H..",
    "GHHHG",
    "..H..",
    "..G..",
]
GLINT_S = [
    ".G.",
    "GHG",
    ".G.",
]
CRACK_TO = [(0, 0), (-18, -20), (18, -20), (-20, 14), (20, 14), (0, 22), (0, -24)]


def death(arr):
    """Gold glass. Six fixed frames: flash + cracks; four pieces on gold light; the pieces part,
    take the gold and dissolve on a fixed 4x4 dither; four-point glints are what is left."""
    m = arr[:, :, 3] > 0
    ys, xs = np.where(m)
    cy = int(round((ys.min() + ys.max()) / 2))
    cx = 31  # the mirror axis: the left pieces are x <= 31
    frames = []
    quads = {
        (-1, -1): m & (np.arange(S)[None, :] <= cx) & (np.arange(S)[:, None] <= cy),
        (1, -1): m & (np.arange(S)[None, :] > cx) & (np.arange(S)[:, None] <= cy),
        (-1, 1): m & (np.arange(S)[None, :] <= cx) & (np.arange(S)[:, None] > cy),
        (1, 1): m & (np.arange(S)[None, :] > cx) & (np.arange(S)[:, None] > cy),
    }
    # f0: struck white, cracked along the hull's own seams
    f0 = white(arr, 0.9)
    for tx, ty in CRACK_TO[1:]:
        n = max(abs(tx), abs(ty))
        for i in range(n + 1):
            x, y = int(round(31.5 + tx * i / n)), int(round(cy + ty * i / n))
            if 0 <= x < S and 0 <= y < S and f0[y, x, 3] > 0:
                f0[y, x, :3] = PAL["g"]
    frames.append(f0)
    plan = [  # gap px, down px, gold mix, dissolve, alpha
        (1, 0, 0.10, 0.00, 255),
        (3, 1, 0.30, 0.00, 255),
        (6, 3, 0.55, 0.20, 235),
        (9, 6, 0.75, 0.55, 190),
        (12, 10, 0.90, 0.88, 120),
    ]
    for gap, down, mix, dis, alpha in plan:
        out = np.zeros_like(arr)
        for (qx, qy), qm in quads.items():
            qy_s, qx_s = np.where(qm)
            edge = qm & ~(np.roll(qm, 1, 0) & np.roll(qm, -1, 0) & np.roll(qm, 1, 1) & np.roll(qm, -1, 1))
            for y, x in zip(qy_s, qx_s):
                nx, ny = x + qx * gap, y + qy * gap // 2 + down
                if not (0 <= nx < S and 0 <= ny < S):
                    continue
                if dis and BAYER[ny % 4, nx % 4] < dis:
                    continue
                c = arr[y, x, :3].astype(float)
                c = c * (1 - mix) + np.array(PAL["g"]) * mix
                if edge[y, x]:
                    c = np.array(PAL["G"]) * 0.8 + c * 0.2     # the cut edges catch the light
                out[ny, nx, :3] = c.astype(np.uint8)
                out[ny, nx, 3] = alpha
        frames.append(out)
    # glints: fixed spots around the fall, bigger early and smaller late
    spots = [[(-14, -10), (15, -4), (-4, 15)], [(-12, -6), (13, 2), (0, 13)], [(-10, 0), (11, 8)]]
    for f, which in ((3, 0), (4, 1), (5, 2)):
        for dx, dy in spots[which]:
            glint = GLINT if f == 3 else GLINT_S
            for j, row in enumerate(glint):
                for i, ch in enumerate(row):
                    if ch == ".":
                        continue
                    x, y = 31 + dx + i - len(row) // 2, cy + dy + j - len(glint) // 2
                    if 0 <= x < S and 0 <= y < S:
                        frames[f][y, x, :3] = PAL[ch]
                        frames[f][y, x, 3] = 255 if f < 5 else 200
    return frames


# ----------------------------------------------------------------- assembling a sheet ----
def build_sheet(enemy):
    """enemy: object with .draw(canvas, t, rage) -> dict(top, left, right, mid) for rank placement
    and .bob(t).  Returns the 512x256 sheet."""
    sheet = Image.new("RGBA", (8 * S, 4 * S), (0, 0, 0, 0))

    def put(arr, col, row):
        sheet.alpha_composite(Image.fromarray(arr, "RGBA"), (col * S, row * S))

    def frame(t, rage, rank):
        c = Canvas()
        info = enemy.draw(c, t, rage)
        rank_layers(c, rank, info["top"], info["left"], info["right"], info["mid"])
        c.outline()
        arr = c.rgba(rage=rage)
        dx = [0, 1, 0, -1][t] if rage else 0                   # the rage shudder
        dy = enemy.bob(t)
        arr = shift(arr, dx, dy)
        if rage:
            arr = enemy.rage_overlay(arr, t)
        if rank >= 2:
            arr = halo(arr)
        return arr

    for rank, row in ((0, 0), (1, 2), (2, 3)):
        for t in range(4):
            put(frame(t, False, rank), t, row)
            put(frame(t, True, rank), 4 + t, row)
    base = frame(0, False, 0)
    put(shift(white(base, 1.0), 0, -2), 0, 1)
    put(scorch(shift(white(base, 0.35), 0, -1)), 1, 1)
    for i, f in enumerate(death(base)):
        put(f, 2 + i, 1)
    return sheet


# ----------------------------------------------------------------- shared enemy base -------
FLAME = [  # four frames of a rage flame, hand-drawn: the tip leans left, up, right, up
    ["..E..", ".EgE.", ".EGgE", "EgGGE", ".EgE."],
    [".E...", ".EgE.", "EgGE.", ".EGgE", "..EE."],
    ["..E..", "..EE.", ".EgGE", "EgGgE", ".EEE."],
    ["...E.", ".EgE.", ".EGgE", "EgGE.", ".EE.."],
]


class EnemyArt:
    """Subclass per archetype.  draw() paints the hull for animation step t (0..3) and returns the
    hull's bounds (top, left, right, mid) so ranks and rage can sit on it."""
    name = ""
    cracks = []    # rage: ember polylines, hand-placed, left half (mirrored): [(x0, y0, x1, y1), ...]
    flames = ()    # rage: (x, y) of each flame's top-left, left half (mirrored)
    hover = (0, -1, -1, 0)

    def bob(self, t):
        return self.hover[t]

    def draw(self, c, t, rage):
        raise NotImplementedError

    def rage_overlay(self, arr, t):
        c = Canvas()
        for x0, y0, x1, y1 in self.cracks:
            for sx in (0, 1):
                xa, xb = (x0, x1) if not sx else (63 - x0, 63 - x1)
                c.line(xa, y0, xb, y1, "E")
        # a hot core travels along each crack, one step per frame
        for x0, y0, x1, y1 in self.cracks:
            f = (t + 1) / 5.0
            px, py = round(x0 + (x1 - x0) * f), round(y0 + (y1 - y0) * f)
            c.px(px, py, "G")
            c.px(63 - px, py, "G")
        for fx, fy in self.flames:
            c.stamp(FLAME[t], fx, fy - (t % 2), True)
        over = c.rgba(rage=True)
        # ember overlay only where the hull is (cracks) or in the air (flames)
        m = over[:, :, 3] > 0
        out = arr.copy()
        out[m] = over[m]
        return out


# ----------------------------------------------------------------- the people (NPCs) -------
def build_npc_sheet(person):
    """A person's sheet: 8x2 cells of 64x64 = 512x128.
        row 0   idle x4        talk x4
        row 1   work x4        hail x4
    person.draw(canvas, t, mode) paints the hull for mode in idle / talk / work / hail."""
    sheet = Image.new("RGBA", (8 * S, 2 * S), (0, 0, 0, 0))
    for row, modes in enumerate((("idle", "talk"), ("work", "hail"))):
        for half, mode in enumerate(modes):
            for t in range(4):
                c = Canvas()
                person.draw(c, t, mode)
                c.outline()
                arr = shift(c.rgba(), 0, person.bob(t))
                sheet.alpha_composite(Image.fromarray(arr, "RGBA"), ((half * 4 + t) * S, row * S))
    return sheet

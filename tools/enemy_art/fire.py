"""FIRE, drawn for a top-down game (a proposal sheet: five kinds, eight looping frames each, 32 x 32 cells: the size of every effect sprite in this game).

Fire seen from ABOVE is not a tongue standing up on a page: it is a hot core with a ragged, churning skirt that licks outward in every direction,
brightest at the middle and breaking into dark red and ember at the rim; a jet from a wall is a fan that opens as it travels; an engine's exhaust is a
teardrop with a pulsing core.  So each kind here is a polar or a plume field of animated noise, cut into SIX flat colour bands (white-yellow, yellow, orange,
red, dark red, char) with a Bayer dither at every band edge: pixel art, no smooth gradients.  Every loop closes (time enters only through cos/sin of one
turn), and nothing is random: the same run writes the same bytes.

    python tools/enemy_art/fire.py        ->  build/fire_sheet.png (256 x 224, raw)  and  build/fire_preview.png (the contact sheet, labelled)

Rows: 0 HEARTH (a campfire, a pyre, a forge mouth: a swirl of tongues round a core)   1 BRAZIER (the same fire in a bronze bowl: the lamps, the altars, the
shrine)   2 GROUND (burning ground: Jvala's cinders, Agneya's trail, the ash)   3 EXHAUST (an engine seen from above, streaming up the cell: the ships)
4 JET MOUTH + 5 JET BODY + 6 JET TIP (a vent in a wall flaming to the right: the mouth, then the body tiled end to end, then the tip; the Smelters' vents)."""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

S = 32                 # a cell: every effect (fire, smoke, water ...) is 32 x 32 px
Z = 64 // S            # the shapes below are designed on a 64-unit field and SAMPLED at every Z-th point, so a 32 px cell is drawn natively, never shrunk
N = 8
TAU = math.pi * 2

# the six bands, brightest first (rgb); the seventh colour is the char at the rim
BANDS = np.array([
    (255, 250, 214),   # white-yellow: the core
    (255, 222, 104),   # yellow
    (255, 158, 38),    # orange
    (232, 82, 22),     # red
    (160, 38, 16),     # dark red
    (84, 22, 14),      # char, at the very rim
], dtype=np.uint8)
THRESH = np.array([0.80, 0.58, 0.38, 0.20, 0.07])   # v above each = that band; below the last = char; below 0.0 = nothing

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 - 0.5
GY, GX = np.mgrid[0:S, 0:S]
BAY = BAYER[GY % 4, GX % 4]
YS, XS = GY * Z + (Z - 1) / 2.0, GX * Z + (Z - 1) / 2.0     # the field's own coordinates (0..63)


def _hash(ix, iy, seed):
    n = (ix * 374761393 + iy * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, seed=1):
    """Smooth value noise on the plane."""
    x0, y0 = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64)
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _hash(x0, y0, seed), _hash(x0 + 1, y0, seed)
    c, d = _hash(x0, y0 + 1, seed), _hash(x0 + 1, y0 + 1, seed)
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


def fbm(x, y, seed=1):
    return 0.55 * vnoise(x, y, seed) + 0.30 * vnoise(x * 2.1, y * 2.1, seed + 7) + 0.15 * vnoise(x * 4.3, y * 4.3, seed + 13)


def quantise(v, alpha_floor=0.0):
    """A field 0..1 into the six flat bands, dithered at every edge; below the floor there is no fire at all."""
    out = np.zeros((S, S, 4), dtype=np.uint8)
    vd = v + BAY * 0.07
    idx = np.full(v.shape, 5, dtype=np.int64)               # char by default
    for k, th in reversed(list(enumerate(THRESH))):
        idx = np.where(vd > th, k, idx)
    live = (v + BAY * 0.02) > alpha_floor
    out[live, :3] = BANDS[idx[live]]
    out[live, 3] = 255
    return out


def put(img, x, y, colour, alpha=255):
    xi, yi = int(math.floor(x / Z)), int(math.floor(y / Z))        # (x, y) are field coordinates
    if 0 <= xi < S and 0 <= yi < S:
        img[yi, xi, :3] = colour
        img[yi, xi, 3] = alpha


def embers(img, t, n, r0, r1, cx=31.5, cy=31.5, seed=3, spread=TAU):
    """Sparks that leave the fire: each travels outward over the loop and cools yellow, orange, red as it goes."""
    for k in range(n):
        ph = (t + _hash(k, 1, seed)) % 1.0
        a = (k * 2.399963 + _hash(k, 2, seed) * 0.8) % spread
        r = r0 + (r1 - r0) * ph
        put(img, cx + math.cos(a) * r, cy + math.sin(a) * r, BANDS[1] if ph < 0.35 else BANDS[2] if ph < 0.7 else BANDS[3], 255 if ph < 0.85 else 140)


# ---------------------------------------------------------------- 0. HEARTH: a swirl of tongues round a core, seen from above
def hearth(t):
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    sw = th + 0.05 * d + 0.5 * math.sin(p)
    lobes = 0.5 + 0.5 * np.sin(3 * sw + p + 1.7 * fbm(dx * 0.05, dy * 0.05, 19))      # a few slow tongues, turning, bent by the noise
    lobes2 = 0.5 + 0.5 * np.sin(7 * sw - 2 * p + 2.3 * fbm(dy * 0.06, dx * 0.06, 29))  # a finer set the other way
    n = fbm(dx * 0.10 + math.cos(p) * 1.6, dy * 0.10 + math.sin(p) * 1.6, 11)
    n2 = fbm(dx * 0.21 - math.sin(p) * 1.4, dy * 0.21 + math.cos(p) * 1.4, 17)
    R = 11 + 11 * (0.45 * lobes + 0.25 * lobes2 + 0.55 * n + 0.25 * n2) * (0.7 + 0.5 * n)
    v = 1 - d / R
    v = v + 0.5 * np.exp(-(d / 8.5) ** 2) + 0.12 * (n2 - 0.5)
    img = quantise(np.clip(v, -1, 1))
    embers(img, t, 9, 17, 29)
    return img


# ---------------------------------------------------------------- 1. BRAZIER: a bowl of bronze with a fire breathing in it
def brazier(t):
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    n = fbm(dx * 0.11 + math.cos(p) * 1.2, dy * 0.11 + math.sin(p) * 1.2, 23)
    lobes = 0.5 + 0.5 * np.sin(3 * th - 2 * p)
    R = 7.5 + 5.5 * lobes * (0.6 + 0.8 * n) + 1.5 * math.sin(p)
    v = 1 - d / R + 0.55 * np.exp(-(d / 4.5) ** 2)
    img = np.zeros((S, S, 4), dtype=np.uint8)
    bowl = d < 21.5                                                  # the bowl: a dark bed of coal, a bronze rim lit from the upper left
    img[bowl, :3] = (44, 20, 14)
    img[bowl, 3] = 255
    coals = bowl & (fbm(dx * 0.35, dy * 0.35, 5) > 0.6 + 0.1 * np.sin(p + XS * 0.3)) & (d < 17)
    img[coals, :3] = BANDS[4]
    rim = (d >= 17) & (d < 21.5)
    lit = np.clip(0.5 - (dx + dy) / 40.0, 0, 1)
    rim_rgb = np.stack([120 + 110 * lit, 70 + 80 * lit, 22 + 36 * lit], axis=-1).astype(np.uint8)
    img[rim, :3] = rim_rgb[rim]
    img[(d >= 20.5) & (d < 21.5), :3] = (46, 24, 12)
    fire = quantise(np.clip(v, -1, 1))
    fire[d > 18, 3] = 0
    live = fire[:, :, 3] > 0
    img[live] = fire[live]
    glow = rim & (d < 18.5)
    img[glow & ~live, :3] = (230, 120, 36)                           # the rim's inner edge catches the fire
    embers(img, t, 5, 8, 24, seed=9)
    return img


# ---------------------------------------------------------------- 2. GROUND: burning ground, an irregular patch with a charred edge
def ground(t):
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    shape = 0.5 + 0.9 * fbm(dx * 0.07 + 3.1, dy * 0.07 + 7.7, 31)     # a fixed ragged outline
    mask = np.clip(1 - d / (26.0 * shape * 0.9 + 6), 0, 1)
    f = fbm(dx * 0.12 + math.cos(p) * 1.6, dy * 0.12 + math.sin(p) * 1.6 + 2.0, 41)
    f2 = fbm(dx * 0.22 - math.sin(p) * 1.3, dy * 0.22 + math.cos(p) * 1.3, 43)
    v = mask ** 0.8 * (0.30 + 0.95 * f + 0.25 * f2) - 0.10
    img = quantise(v, alpha_floor=0.02)
    char = (mask > 0.02) & (img[:, :, 3] == 0)                         # the ground round it is scorched black
    img[char, :3] = (34, 16, 12)
    img[char, 3] = 150
    # a thread of smoke drifts off the patch
    for k in range(3):
        ph = (t + k / 3.0) % 1.0
        put(img, 31.5 + 8 * math.sin(ph * 5 + k) + k * 3 - 3, 30 - ph * 26, (96, 84, 80), int(180 * (1 - ph)))
    embers(img, t, 6, 4, 26, seed=17)
    return img


# ---------------------------------------------------------------- 3. EXHAUST: an engine from above, streaming up the cell (nozzle at the foot)
def exhaust(t):
    p = t * TAU
    u = 60.0 - YS                                                     # distance up the plume from the nozzle
    dx = XS - 31.5
    L = 54 + 5 * math.sin(p) + 3 * math.sin(2 * p + 1)                # the plume breathes in length
    w = np.where(u < 5, 4.5 + u * 1.5, 12.0 * np.power(np.clip(1 - u / L, 0, 1), 0.6) + 0.8)
    wob = 0.9 * np.sin(u * 0.22 - 2 * p) * np.clip(u / 34.0, 0, 1)      # the tail whips
    ddx = dx - wob
    n = fbm(ddx * 0.14 + math.cos(p) * 1.1, u * 0.10 - 1.5 * math.sin(p), 53)
    v = 1 - np.abs(ddx) / (w * (0.62 + 0.75 * n))
    v = v * np.clip((L - u) / L + 0.18, 0, 1) ** 0.6
    v = v + 0.55 * np.exp(-(ddx / 3.6) ** 2) * np.clip(1 - u / (L * 0.85), 0, 1)
    v = v + 0.14 * np.sin(0.62 * u - 2 * p) * np.clip(1 - u / L, 0, 1) * (np.abs(ddx) < w * 0.7)        # shock diamonds in the core
    v = np.where((u < -1) | (u > L), -1, v)
    img = quantise(v)
    for k in range(5):                                                # sparks thrown off the tail
        ph = (t + _hash(k, 3, 61)) % 1.0
        put(img, 31.5 + (_hash(k, 4, 61) - 0.5) * 12, 60 - 20 - ph * 34, BANDS[2] if ph < 0.6 else BANDS[3], 255)
    for x in range(23, 41):                                           # the nozzle: a dark lip with a hot centre
        put(img, x, 62, (60, 34, 20))
        put(img, x, 63, (36, 22, 16))
    for x in range(27, 37):
        put(img, x, 61, BANDS[2])
    return img


# ---------------------------------------------------------------- 4-6. JET: a vent in a wall breathing a stream to the right, in THREE pieces that join exactly
# MOUTH (the root, flaring out of the wall) + BODY x n (tiles end to end with no seam) + TIP (the stream breaking into licks and sparks).  The stream is
# one periodic field: its noise is walked round a circle as u goes 0..64, so the body's right edge IS its left edge, and the mouth's right edge and the
# tip's left edge are that same cross-section.  Time slides the field outward by exactly one period over the loop: the fire runs away from the wall.
def _stream(t, grow, fade):
    p = t * TAU
    u = XS.astype(np.float64)
    dy = YS - 31.5
    th = TAU * (u / 64.0 - t)
    n = fbm(1.25 * np.cos(th) + dy * 0.11 + 4.0, 1.25 * np.sin(th) - dy * 0.09 + 9.0, 71)
    puffs = 0.5 + 0.5 * np.sin(2 * th + 2.6 * (n - 0.5))               # billows rolling outward, two to a cell
    R = 22.0 * grow * (0.78 + 0.5 * n)
    v = 1 - np.abs(dy) / np.maximum(R, 1.0)
    v = v * (0.62 + 0.5 * puffs)
    v = v + 0.5 * np.exp(-(dy / (5.0 + 3.0 * grow)) ** 2) * (0.6 + 0.5 * puffs)      # the hot core, a spine down the middle
    return v * fade, th, n


def jet_body(t):
    v, _, _ = _stream(t, 1.0, 1.0)
    return quantise(v)


def jet_mouth(t):
    u = XS.astype(np.float64)
    e = np.clip(u / 60.0, 0, 1)
    grow = 0.28 + 0.72 * e * e * (3 - 2 * e)                           # the stream opens out of the wall and meets the body's width
    v, _, _ = _stream(t, grow, 1.0)
    v = v + 0.45 * np.exp(-((u - 2) / 10.0) ** 2) * np.exp(-((YS - 31.5) / 9.0) ** 2)    # a flash where it leaves the vent
    img = quantise(v)
    for y in range(18, 46):                                            # the lip in the wall
        for x in (0, 1):
            put(img, x, y, (46, 28, 20) if x == 0 else (90, 56, 30))
    return img


def jet_tip(t):
    u = XS.astype(np.float64)
    e = np.clip(u / 58.0, 0, 1)
    fade = np.clip(1 - e ** 1.6, 0, 1)
    v, _, _ = _stream(t, 1.0 - 0.45 * e, 1.0)
    img = quantise(v * fade)
    for k in range(7):                                                 # sparks flung off the end
        ph = (t + _hash(k, 5, 81)) % 1.0
        put(img, 18 + ph * 42, 31.5 + (_hash(k, 6, 81) - 0.5) * (12 + 26 * ph), BANDS[1] if ph < 0.3 else BANDS[2] if ph < 0.65 else BANDS[3], 255 if ph < 0.85 else 120)
    return img


KINDS = [("HEARTH", hearth), ("BRAZIER", brazier), ("GROUND", ground), ("EXHAUST", exhaust), ("JET MOUTH", jet_mouth), ("JET BODY", jet_body), ("JET TIP", jet_tip)]


def build():
    sheet = Image.new("RGBA", (N * S, len(KINDS) * S), (0, 0, 0, 0))
    for r, (_, fn) in enumerate(KINDS):
        for f in range(N):
            sheet.alpha_composite(Image.fromarray(fn(f / N), "RGBA"), (f * S, r * S))
    return sheet


def preview(sheet, scale=6):
    pad, lab = 6, 78
    scale = 6
    W = lab + N * S * scale + pad
    H = len(KINDS) * (S * scale + pad) + pad
    bg = Image.new("RGBA", (W, H), (24, 20, 22, 255))
    d = ImageDraw.Draw(bg)
    for r, (name, _) in enumerate(KINDS):
        y = pad + r * (S * scale + pad)
        # a lighter and a darker ground under each row, to show the fire on both
        d.rectangle([lab, y, lab + N * S * scale // 2, y + S * scale], fill=(40, 36, 38))
        d.rectangle([lab + N * S * scale // 2, y, lab + N * S * scale, y + S * scale], fill=(14, 12, 14))
        row = sheet.crop((0, r * S, N * S, (r + 1) * S)).resize((N * S * scale, S * scale), Image.NEAREST)
        bg.alpha_composite(row, (lab, y))
        d.text((6, y + S * scale // 2 - 6), f"{r + 1}  {name}", fill=(255, 220, 140, 255))
    return bg


if __name__ == "__main__":
    os.makedirs("build", exist_ok=True)
    sh = build()
    sh.save("build/fire_sheet.png")
    preview(sh).convert("RGB").save("build/fire_preview.png")
    print(sh.size)

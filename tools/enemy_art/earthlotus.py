"""EARTH (Prithvi's boulders) and LOTUS (Brahma's petals) sprite sheets, 32 x 32 cells, eight frames a row, in the language of fire.py and flowfx.py (flat colour bands, Bayer dither,
every loop closes, nothing random).

    python tools/enemy_art/earthlotus.py   ->   build/astra_earth.png (256 x 320)   rows: CREATE, BOULDER A (round), B (slab), C (veined), D (jagged), BREAK, CHUNK A, CHUNK B, PEBBLES, DUST,
                                                                                    BREAK SMALL      (that is eleven rows: 256 x 352)
                                                build/astra_lotus.png (256 x 192)   rows: BUD, PETAL OPEN, PETAL FLY, PETAL SPIN, PETAL FADE, MOTES
                                                build/earthlotus_preview.png        both, labelled

The earth sheet: a boulder FORMS (pebbles and dust draw together), there are four VARIETIES of boulder tumbling, a boulder BREAKS into chunks, chunks tumble (two kinds), a chunk BREAKS into
pebbles, pebbles tumble, dust blooms. The lotus sheet is near-white lavender so the game tints it with each element's colour."""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import flowfx as FX
import fire as F

S, N, TAU = F.S, F.N, F.TAU
XS, YS = F.XS, F.YS
EARTH = np.array([(238, 220, 178), (208, 174, 116), (172, 130, 80), (130, 92, 58), (90, 64, 44), (54, 40, 32)], dtype=np.uint8)
LOTUS = np.array([(255, 255, 255), (238, 236, 246), (214, 210, 232), (180, 176, 206), (140, 136, 172), (96, 92, 128)], dtype=np.uint8)
VEIN = (255, 168, 56)
LIGHT = np.array([-0.62, -0.78])           # light from the upper left, in world terms: it does not turn with the rock


def sn(x):
    return max(0.0, math.sin(x))


# ================================================================== EARTH ==================================================================
def rock(cx, cy, r, rot, variety, seed):
    """A lit rock as a 0..1 field (the shading) and its mask. The surface turns with `rot`; the light does not. Varieties: 0 round, 1 slab, 2 veined, 3 jagged."""
    dx, dy = XS - cx, YS - cy
    c, s = math.cos(-rot), math.sin(-rot)
    ux, uy = dx * c - dy * s, dx * s + dy * c                              # the rock's own coordinates
    th = np.arctan2(uy, ux)
    d = np.hypot(ux, uy)
    if variety == 0:
        edge = r * (1 + 0.10 * np.sin(3 * th + seed) + 0.06 * np.sin(5 * th + 2 * seed))
    elif variety == 1:
        edge = r * 1.15 / np.power(np.abs(np.cos(th)) ** 3 + (np.abs(np.sin(th)) * 1.5) ** 3, 1 / 3.0) * 0.85
    elif variety == 2:
        edge = r * (1 + 0.08 * np.sin(4 * th + seed) + 0.05 * np.sin(7 * th))
    else:
        edge = r * (0.78 + 0.34 * np.abs(np.sin(3.5 * th + seed))) * (1 + 0.06 * np.sin(11 * th))
    mask = d < edge
    nx, ny = dx / np.maximum(np.hypot(dx, dy), 1e-6), dy / np.maximum(np.hypot(dx, dy), 1e-6)
    lit = 0.5 + 0.5 * (nx * LIGHT[0] + ny * LIGHT[1])                       # a ball's shading
    z = np.sqrt(np.clip(1 - (np.minimum(d, edge) / np.maximum(edge, 1e-6)) ** 2, 0, 1))
    tex = F.fbm(ux * 0.22 + seed, uy * 0.22, 501 + int(seed))
    v = (0.30 + 0.55 * lit * (0.55 + 0.45 * z) + 0.28 * (tex - 0.5)) * mask
    rim = mask & (d > edge - 1.8)                                          # a dark edge
    v = np.where(rim, v * 0.55, v)
    return np.clip(v, 0, 1), mask, ux, uy


def earth_img(v, mask):
    img = FX.band(v, EARTH, 0.05)
    img[~mask] = 0
    return img


def boulder(variety):
    def fn(t):
        v, mask, ux, uy = rock(31.5, 31.5, 12.5 if variety != 3 else 13.5, t * TAU, variety, 1.7 * variety + 1)
        img = earth_img(v, mask)
        if variety == 2:                                                    # two veins that glow and turn with the rock
            for k in range(2):
                a = 0.5 + k * 2.1
                dist = np.abs(-math.sin(a) * ux + math.cos(a) * uy - (2.0 - 3.0 * k))
                vein = mask & (dist < 0.9 + 0.5 * math.sin(t * TAU + k)) & (np.abs(math.cos(a) * ux + math.sin(a) * uy) < 9.5)
                img[vein, :3] = VEIN
                img[vein, 3] = 255
        return img
    return fn


def chunk(kind):
    def fn(t):
        v, mask, ux, uy = rock(31.5, 31.5, 7.0 if kind == 0 else 6.2, t * TAU * (1 if kind == 0 else -1), 3 if kind == 0 else 0, 3.1 + kind)
        return earth_img(v, mask)
    return fn


def chunk_at(img, cx, cy, r, rot, seed, kind=3):
    v, mask, _, _ = rock(cx, cy, r, rot, kind, seed)
    part = earth_img(v, mask)
    live = part[..., 3] > 0
    img[live] = part[live]


def dust(img, cx, cy, rad, strength, seed):
    n = F.fbm((XS - cx) * 0.16 + seed, (YS - cy) * 0.16, 521)
    d = np.hypot(XS - cx, YS - cy)
    v = np.clip((1 - d / max(rad, 1)) * 0.8 * strength * (0.5 + n), 0, 0.5)
    part = FX.band(v, EARTH, 0.06)
    live = (part[..., 3] > 0) & (img[..., 3] == 0)
    img[live] = part[live]


def create(t):
    s = min(1.0, t * N / (N - 1.0))
    img = np.zeros((S, S, 4), dtype=np.uint8)
    dust(img, 31.5, 31.5, 28 * (1 - 0.4 * s), 1.0 - s * 0.5, 2.0)
    for k in range(16):                                                     # pebbles draw in from the rim
        a = k * 2.399963 + F._hash(k, 1, 531) * 0.4
        r = (30 - 20 * s) * (0.6 + 0.4 * F._hash(k, 2, 531)) * (1 - s * 0.7)
        F.put(img, 31.5 + r * math.cos(a), 31.5 + r * math.sin(a), tuple(EARTH[1 + k % 3]), 255)
    e = max(0.0, (s - 0.2) / 0.8)
    if e > 0:
        v, mask, _, _ = rock(31.5, 31.5, 12.5 * e ** 0.7, s * 2.0, 0, 1.0)
        part = earth_img(v, mask)
        live = part[..., 3] > 0
        img[live] = part[live]
    return img


def brk(t):
    s = min(1.0, t * N / (N - 1.0))
    img = np.zeros((S, S, 4), dtype=np.uint8)
    if s < 0.12:                                                            # the boulder, cracked, about to go
        v, mask, ux, uy = rock(31.5, 31.5, 12.5, 0.4, 0, 1.0)
        img = earth_img(v, mask)
        crack = mask & (np.abs(uy - 0.35 * ux) < 0.7)
        img[crack, :3] = EARTH[5]
        return img
    dust(img, 31.5, 31.5, 8 + 22 * s, 1.0 - s, 4.0)
    for k in range(5):
        a = k * TAU / 5 + 0.4
        dist = 4 + 21 * (1 - (1 - s) ** 2)
        r = 6.5 - 2.0 * s
        chunk_at(img, 31.5 + dist * math.cos(a), 31.5 + dist * math.sin(a), r, a + s * 4, 2.0 + k, 3 if k % 2 == 0 else 0)
    return img


def pebbles(t):
    img = np.zeros((S, S, 4), dtype=np.uint8)
    for k in range(8):
        ph = (t + F._hash(k, 3, 541)) % 1.0
        a = k * 2.399963 + ph * 1.6
        r = 4 + 20 * ph * (0.5 + 0.5 * F._hash(k, 4, 541))
        x, y = 31.5 + r * math.cos(a), 31.5 + r * math.sin(a)
        size = 2.4 - 1.2 * ph
        F.put(img, x, y, tuple(EARTH[1 + k % 3]), 255)
        F.put(img, x + size, y, tuple(EARTH[2 + k % 2]), 255)
        F.put(img, x, y + size, tuple(EARTH[3]), 255)
    return img


def dust_row(t):
    s = min(1.0, t * N / (N - 1.0))
    img = np.zeros((S, S, 4), dtype=np.uint8)
    dust(img, 31.5, 31.5, 6 + 25 * s, (1 - s) ** 0.8 * 1.5, 7.0)
    return img


def break_small(t):
    s = min(1.0, t * N / (N - 1.0))
    img = np.zeros((S, S, 4), dtype=np.uint8)
    if s < 0.12:
        chunk_at(img, 31.5, 31.5, 7.0, 0.3, 3.1)
        return img
    dust(img, 31.5, 31.5, 6 + 14 * s, 0.9 - s * 0.9, 9.0)
    for k in range(9):
        a = k * 2.399963 + 0.3
        dist = 3 + 22 * (1 - (1 - s) ** 2) * (0.5 + 0.5 * F._hash(k, 5, 551))
        x, y = 31.5 + dist * math.cos(a), 31.5 + dist * math.sin(a)
        F.put(img, x, y, tuple(EARTH[1 + k % 3]), 255)
        F.put(img, x + 1.4, y + 0.6, tuple(EARTH[3]), 255)
    return img


EARTH_ROWS = [("CREATE (one-shot)", create), ("BOULDER A round", boulder(0)), ("BOULDER B slab", boulder(1)), ("BOULDER C veined", boulder(2)), ("BOULDER D jagged", boulder(3)),
              ("BREAK (one-shot)", brk), ("CHUNK A", chunk(0)), ("CHUNK B", chunk(1)), ("PEBBLES", pebbles), ("DUST (one-shot)", dust_row), ("BREAK SMALL (one-shot)", break_small)]


# ================================================================== LOTUS ==================================================================
def petal_field(cx, cy, ang, r0, r1, half, curl=0.0):
    """A lens-shaped petal from (cx, cy) outward at angle ang, `half` units wide at its fullest, bent by `curl`: a field that is 1 on its midrib and 0 at its edge."""
    dx, dy = XS - cx, YS - cy
    c, s = math.cos(-ang), math.sin(-ang)
    ux, uy = dx * c - dy * s, dx * s + dy * c
    u = np.clip((ux - r0) / max(r1 - r0, 1e-6), 0, 1)
    mid = curl * np.sin(np.pi * u) * (r1 - r0) * 0.25
    wd = half * np.power(np.clip(4 * u * (1 - u), 0, 1), 0.85) + 0.3
    v = 1 - np.abs(uy - mid) / wd
    v = np.where((ux < r0) | (ux > r1), -1, v)
    return v


def lotus_band(v):
    return FX.band(np.clip(v, 0, 1), LOTUS, 0.05, 0.06)


def bud(t):
    s = min(1.0, t * N / (N - 1.0))
    v = np.full(XS.shape, -1.0)
    for k in range(5):
        a = -math.pi / 2 + k * TAU / 5
        open_ = s ** 0.8
        v = np.maximum(v, petal_field(31.5, 31.5, a, 1.0, 5 + 22 * open_, 1.8 + 6.5 * open_, curl=(1 - open_) * 0.35) * (0.72 + 0.28 * k / 4))
    d = np.hypot(XS - 31.5, YS - 31.5)
    v = np.maximum(v, 0.9 * np.exp(-(d / (2 + 2 * s)) ** 2))
    return lotus_band(v)


def petal_open(t):
    s = min(1.0, t * N / (N - 1.0))
    v = petal_field(4, 32, 0.0, 0, 8 + 42 * s, 1.5 + 9 * s ** 0.8, curl=(1 - s) * 0.45)
    return lotus_band(v)


def petal_fly(t):
    p = t * TAU
    flap = 0.45 + 0.55 * abs(math.cos(p / 2))
    v = petal_field(5, 32, 0.0, 0, 48, 10.5 * flap, curl=0.28 * math.sin(p))
    mid = petal_field(5, 32, 0.0, 0, 48, 0.8, curl=0.28 * math.sin(p))
    return lotus_band(np.maximum(v * 0.85, mid))


def petal_spin(t):
    p = t * TAU
    v = np.full(XS.shape, -1.0)
    for k in range(2):                                                      # the petal edge-on, a blade, with a streak of its turning
        ang = p + k * math.pi
        v = np.maximum(v, 0.95 * petal_field(31.5, 31.5, ang, 2.0, 28, 3.0, curl=0.5) * (1.0 if k == 0 else 0.55))
    d = np.hypot(XS - 31.5, YS - 31.5)
    v = np.maximum(v, 0.8 * np.exp(-(d / 3.0) ** 2))
    return lotus_band(v)


def petal_fade(t):
    s = min(1.0, t * N / (N - 1.0))
    v = petal_field(4, 32, 0.0, 0, 50, 10.0 * (1 - 0.3 * s), curl=0.3)
    holes = F.fbm(XS * 0.35, YS * 0.35, 561) < s * 1.1
    v = np.where(holes, -1, v) * (1 - 0.4 * s)
    img = lotus_band(v)
    for k in range(10):
        ph = min(1.0, s * 1.2 + F._hash(k, 6, 561) * 0.1)
        F.put(img, 8 + 46 * F._hash(k, 7, 561) * (0.4 + ph * 0.6), 32 + (F._hash(k, 8, 561) - 0.5) * 22 * (0.4 + ph), tuple(LOTUS[1 + k % 3]), int(255 * (1 - s * 0.6)))
    return img


def motes(t):
    img = np.zeros((S, S, 4), dtype=np.uint8)
    for k in range(7):
        ph = (t + F._hash(k, 9, 571)) % 1.0
        x = 4 + 54 * ph
        y = 14 + 36 * F._hash(k, 10, 571) + 6 * math.sin(TAU * (ph * 1.2 + F._hash(k, 11, 571)))
        for tr in range(3):
            F.put(img, x - tr * 2.2, y, tuple(LOTUS[1 + tr]), 255)
        F.put(img, x, y + 1.2, tuple(LOTUS[2]), 255)
    return img


LOTUS_ROWS = [("BUD (one-shot)", bud), ("PETAL OPEN (one-shot)", petal_open), ("PETAL FLY", petal_fly), ("PETAL SPIN", petal_spin), ("PETAL FADE (one-shot)", petal_fade), ("MOTES", motes)]


def preview(sheets, scale=4):
    pad, lab = 6, 160
    rows = [(n, i, k) for k, (sh, rs) in enumerate(sheets) for i, (n, _) in enumerate(rs)]
    W = lab + N * S * scale + pad
    H = len(rows) * (S * scale + pad) + pad
    bg = Image.new("RGBA", (W, H), (24, 20, 22, 255))
    d = ImageDraw.Draw(bg)
    for j, (name, r, k) in enumerate(rows):
        y = pad + j * (S * scale + pad)
        d.rectangle([lab, y, lab + N * S * scale // 2, y + S * scale], fill=(46, 42, 46))
        d.rectangle([lab + N * S * scale // 2, y, lab + N * S * scale, y + S * scale], fill=(14, 12, 14))
        row = sheets[k][0].crop((0, r * S, N * S, (r + 1) * S)).resize((N * S * scale, S * scale), Image.NEAREST)
        bg.alpha_composite(row, (lab, y))
        d.text((6, y + S * scale // 2 - 6), f"{'EARTH' if k == 0 else 'LOTUS'}  {name}", fill=(235, 205, 150, 255) if k == 0 else (225, 215, 245, 255))
    return bg


if __name__ == "__main__":
    os.makedirs("build", exist_ok=True)
    earth, lotus = FX.build(EARTH_ROWS), FX.build(LOTUS_ROWS)
    earth.save("build/astra_earth.png")
    lotus.save("build/astra_lotus.png")
    preview([(earth, EARTH_ROWS), (lotus, LOTUS_ROWS)]).convert("RGB").save("build/earthlotus_preview.png")
    print(earth.size, lotus.size)

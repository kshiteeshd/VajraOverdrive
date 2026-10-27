"""WATER sprite sheet for the Jala astra's flood, 32 x 32 cells, eight frames a row, in the language of flowfx.py (flat colour bands, Bayer dither, every loop closes).

    python tools/enemy_art/water.py   ->   build/astra_water.png     (256 x 192)   rows: BODY, FOAM, DROP, CREST, SPLASH, DRENCH
                                           build/water_preview.png   the sheet on dark, labelled

BODY: a lump of water, its edge wobbling, a highlight sliding across it (a loop). FOAM: a cluster of bubbles that drift and pop (a loop). DROP: a falling drop that stretches and
squashes (a loop). CREST: the curl at the front of a wave, pointing right (a loop). SPLASH: where water meets rock or a hull (a one-shot burst). DRENCH: a veil of runnels
laid over a soaked hull (a loop). The game draws them with ordinary blending and some transparency, so the ground shows through the flood."""
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
# foam white, spray, shallow, mid, deep, abyss
WATER = np.array([(240, 250, 255), (178, 230, 252), (112, 198, 242), (62, 152, 218), (32, 102, 186), (20, 62, 134)], dtype=np.uint8)


def body(f):
    ph = f * TAU / N
    dx, dy = XS - 32, YS - 32
    th = np.arctan2(dy, dx)
    d = np.hypot(dx, dy)
    r = 20 + 2.2 * np.sin(3 * th + ph) + 1.5 * np.sin(5 * th - 2 * ph) + 1.0 * np.sin(2 * th + 3 * ph)
    v = np.clip(1.0 - d / r, 0, 1) ** 0.55
    hx, hy = 32 - 7 + 3 * math.cos(ph), 32 - 8 + 2 * math.sin(ph)                    # the highlight, drifting
    v = np.clip(v * 0.82 + 0.42 * np.clip(1 - np.hypot(XS - hx, YS - hy) / 7.0, 0, 1), 0, 1)
    rim = (d > r - 2.4) & (d < r)
    v = np.where(rim, np.maximum(v, 0.5), v)
    return FX.band(v, WATER)


def foam(f):
    ph = f * TAU / N
    v = np.zeros(XS.shape)
    for k in range(7):
        a = k * 2.4 + 0.3
        cx = 32 + 15 * math.cos(a + 0.4 * math.sin(ph + k)) * (0.5 + 0.5 * math.sin(k * 1.7))
        cy = 32 + 15 * math.sin(a + 0.4 * math.cos(ph + k))
        rr = 4.2 + 2.2 * math.sin(ph * 1 + k * 1.3)
        d = np.hypot(XS - cx, YS - cy)
        v = np.maximum(v, np.clip(1 - d / max(rr, 1.0), 0, 1) ** 0.6)
        v = np.maximum(v, 0.9 * ((d < rr) & (d > rr - 1.6)))
    return FX.band(np.clip(v * 1.15, 0, 1), WATER)


def drop(f):
    ph = f * TAU / N
    stretch = 1 + 0.35 * math.sin(ph)
    dx, dy = XS - 32, YS - 34
    u = dy / (13 * stretch)
    w = 7.5 / math.sqrt(stretch)
    hw = np.where(u > 0, w * np.sqrt(np.clip(1 - u * u, 0, 1)), w * np.clip(1 + u * 1.1, 0, 1))
    v = np.clip(1 - np.abs(dx) / np.maximum(hw, 0.5), 0, 1) * (np.abs(u) < 1.0)
    v = np.clip(v * 0.9 + 0.35 * np.clip(1 - np.hypot(XS - 29, YS - 38) / 4.0, 0, 1), 0, 1)
    return FX.band(v, WATER)


def crest(f):
    ph = f * TAU / N
    pts = []
    for i in range(18):
        u = i / 17.0
        a = -1.2 + 3.3 * u
        r = 4 + 17 * (1 - u * 0.55)
        pts.append((24 + r * math.cos(a), 36 + r * math.sin(a) * 0.9 + 1.8 * math.sin(ph + u * 5)))
    v = FX.stroke(pts, [4.5 * (1 - 0.7 * i / 17.0) + 1.2 for i in range(18)], 1.1)
    base = FX.stroke([(2, 40 + 2 * math.sin(ph)), (14, 38), (26, 41)], [3.2, 3.8, 3.0], 1.1)
    v = np.maximum(v, 0.8 * base)
    tip = np.clip(1 - np.hypot(XS - pts[-1][0], YS - pts[-1][1]) / 5.0, 0, 1)
    v = np.maximum(v, tip)
    return FX.band(np.clip(v, 0, 1), WATER)


def splash(f):
    u = f / (N - 1.0)
    v = np.clip(1 - np.hypot(XS - 32, YS - 32) / (12 * (1 - u) + 2), 0, 1) * (1 - u)
    for k in range(10):
        a = k * TAU / 10 + 0.2
        r = 6 + 24 * u
        cx, cy = 32 + r * math.cos(a), 32 + r * math.sin(a) - 8 * u * u * 6
        dd = np.hypot(XS - cx, YS - cy)
        v = np.maximum(v, np.clip(1 - dd / (3.2 * (1 - 0.6 * u) + 0.8), 0, 1))
    return FX.band(np.clip(v * 1.1, 0, 1), WATER)


def drench(f):
    u = f / N
    v = np.zeros(XS.shape)
    for k in range(9):
        x = 8 + (k * 53 % 49)
        y0 = (k * 37 % 60) + 64 * u
        y = (y0 % 64)
        ln = 6 + (k % 4) * 3
        v = np.maximum(v, FX.stroke([(x, y), (x + 0.6, y + ln)], [1.6, 1.0], 1.1) * 0.9)
        v = np.maximum(v, np.clip(1 - np.hypot(XS - x - 0.6, YS - y - ln) / 2.4, 0, 1))
    return FX.band(np.clip(v, 0, 1), WATER)


ROWS = [('BODY', body), ('FOAM', foam), ('DROP', drop), ('CREST', crest), ('SPLASH', splash), ('DRENCH', drench)]


def main():
    img = np.zeros((len(ROWS) * S, N * S, 4), dtype=np.uint8)
    for r, (_, fn) in enumerate(ROWS):
        for f in range(N):
            img[r * S:(r + 1) * S, f * S:(f + 1) * S] = fn(f)
    os.makedirs('build', exist_ok=True)
    Image.fromarray(img, 'RGBA').save('build/astra_water.png')
    big = Image.fromarray(img, 'RGBA').resize((N * S * 3, len(ROWS) * S * 3), Image.NEAREST)
    prev = Image.new('RGBA', (big.width + 90, big.height), (14, 18, 34, 255))
    prev.alpha_composite(big, (90, 0))
    d = ImageDraw.Draw(prev)
    for r, (nm, _) in enumerate(ROWS):
        d.text((6, r * S * 3 + 40), nm, fill=(220, 230, 255, 255))
    prev.save('build/water_preview.png')
    print('ok')


if __name__ == '__main__':
    main()

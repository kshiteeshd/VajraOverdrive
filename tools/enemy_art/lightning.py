"""LIGHTNING sprite sheet for Akasha's astra, 32 x 32 cells, eight frames a row, in the language of fire.py and flowfx.py (flat colour bands, Bayer dither, nothing random
between runs).

    python tools/enemy_art/lightning.py   ->   build/astra_lightning.png (256 x 256)   rows: BOLT A, BOLT B, BOLT C, BOLT D, ZAP, ARC, SHOCK, SPARK
                                               build/lightning_preview.png            the sheet, labelled, on dark

BOLT A-D: four different forked bolts lying LEFT -> RIGHT through the middle of the cell (root at x=0, tip at x=31, both at mid-height) so the game tiles them end to end along any line.
Eight frames each: the bolt FORMS (0-2, racing out from the root), CRACKLES (3-5, the path re-jitters every frame), FADES (6-7, thinning and breaking up).
ZAP: a short thrown bolt that points right and flickers (a loop). ARC: a curl of crackle that crawls round a foe (a loop). SHOCK: a web of sparks laid over a shocked foe (a loop).
SPARK: where a bolt lands (a one-shot burst)."""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import flowfx as FX
import fire as F

S, N, TAU = F.S, F.N, F.TAU
XS, YS = F.XS, F.YS
# white-hot, pale ice, periwinkle, violet, deep violet, shadow
SKY = np.array([(255, 255, 255), (226, 238, 255), (176, 198, 255), (136, 138, 250), (98, 78, 214), (60, 44, 140)], dtype=np.uint8)


def st(pts, ws, soft=1.0):
    """FX.stroke with every width scaled up: a bolt is a few pixels thick in a 32 px cell."""
    return FX.stroke(pts, [w * 2.4 for w in ws], soft)


def path(rng, x0, x1, amp, n, y=32.0):
    """A jagged walk from x0 to x1 about the line y, pinned at both ends."""
    pts = []
    for i in range(n + 1):
        u = i / n
        off = 0.0 if i in (0, n) else (rng.random() - 0.5) * 2 * amp * math.sin(math.pi * u) ** 0.5
        pts.append((x0 + (x1 - x0) * u, y + off))
    return pts


def width(i, n, root, tip):
    u = i / n
    return root + (tip - root) * u


def bolt_field(seed, frame, forks):
    """One frame of a bolt: the main strand with `forks` branches. Frames 0-2 reveal it from the root, 3-5 re-jitter it, 6-7 thin it and break it up."""
    rng = random.Random(seed * 100 + (frame if frame >= 3 else 3))      # the forming frames share the path of the first crackle so the bolt GROWS rather than boils
    n = 14
    main = path(rng, 0.0, 63.0, 6.5, n)
    if frame < 3:
        reveal = (frame + 1) / 3.0
    else:
        reveal = 1.0
    thick = 1.0 if frame < 6 else (0.62 if frame == 6 else 0.38)
    cut = int(round(n * reveal))
    pts = main[:cut + 1]
    w = [(1.9 - 0.9 * i / n) * thick for i in range(len(pts))]
    v = st(pts, [x * 1.6 for x in w], 1.3)
    for k in range(forks):
        at = 3 + (k * 4 + seed) % (n - 5)
        if at >= len(pts):
            continue
        bx, by = pts[at]
        ang = (rng.random() - 0.5) * 1.5 + (0.0 if k % 2 == 0 else 0.0)
        ang += 0.55 if k % 2 == 0 else -0.55
        ln = 9 + rng.random() * 9
        fp = []
        for j in range(5):
            u = j / 4.0
            fp.append((bx + math.cos(ang) * ln * u + (rng.random() - 0.5) * 2.6 * (j > 0), by + math.sin(ang) * ln * u + (rng.random() - 0.5) * 2.6 * (j > 0)))
        v = np.maximum(v, 0.85 * st(fp, [(1.1 - 0.8 * j / 4.0) * 1.4 * thick for j in range(5)], 1.3))
    if frame >= 6:                                                      # breaking up: holes along the strand
        gap = np.hypot(XS - (20 + 18 * (frame - 6)), YS - 32) < 5
        v = np.where(gap, v * 0.2, v)
    return v


def bolt_row(img, row, seed, forks):
    for f in range(N):
        v = bolt_field(seed, f, forks)
        img[row * S:(row + 1) * S, f * S:(f + 1) * S] = FX.band(np.clip(v * 1.15, 0, 1), SKY)


def zap_row(img, row):
    """A short thrown bolt: a bright head on the right and a jagged tail behind it, flickering."""
    for f in range(N):
        rng = random.Random(900 + f)
        pts = path(rng, 6.0, 60.0, 4.0, 8)
        v = st(pts, [(0.6 + 1.9 * (i / 8.0)) for i in range(9)], 1.2)
        head = np.hypot(XS - 56, YS - 32)
        v = np.maximum(v, np.clip(1.0 - head / 8.0, 0, 1) * (0.8 + 0.2 * math.sin(f * 1.7)))
        img[row * S:(row + 1) * S, f * S:(f + 1) * S] = FX.band(np.clip(v * 1.1, 0, 1), SKY)


def arc_row(img, row):
    """A curl of crackle: an arc of a circle that jitters, turning a little each frame so it crawls round."""
    for f in range(N):
        rng = random.Random(400 + f)
        pts = []
        a0 = f * TAU / N
        for j in range(11):
            a = a0 + (j / 10.0) * 2.3
            r = 22 + (rng.random() - 0.5) * 7 * (0 < j < 10)
            pts.append((32 + r * math.cos(a), 32 + r * math.sin(a)))
        v = st(pts, [1.7 * math.sin(math.pi * j / 10.0) ** 0.5 + 0.5 for j in range(11)], 1.2)
        a1 = a0 + math.pi
        pts2 = [(32 + (14 + (rng.random() - 0.5) * 6) * math.cos(a1 + j * 0.2), 32 + (14 + (rng.random() - 0.5) * 6) * math.sin(a1 + j * 0.2)) for j in range(8)]
        v = np.maximum(v, 0.7 * st(pts2, [1.0] * 8, 1.2))
        img[row * S:(row + 1) * S, f * S:(f + 1) * S] = FX.band(np.clip(v * 1.1, 0, 1), SKY)


def shock_row(img, row):
    """A web of sparks over a whole foe: short bolts radiating from the middle and across it, different every frame."""
    for f in range(N):
        rng = random.Random(600 + f)
        v = np.zeros(XS.shape)
        for k in range(5):
            a = rng.random() * TAU
            ln = 14 + rng.random() * 16
            pts = [(32 + math.cos(a) * ln * j / 5.0 + ((rng.random() - 0.5) * 4 if j else 0), 32 + math.sin(a) * ln * j / 5.0 + ((rng.random() - 0.5) * 4 if j else 0)) for j in range(6)]
            v = np.maximum(v, st(pts, [1.4 - 0.7 * j / 5.0 for j in range(6)], 1.2))
        core = np.clip(1 - np.hypot(XS - 32, YS - 32) / (5 + 2 * math.sin(f * 1.3)), 0, 1)
        v = np.maximum(v, 0.8 * core)
        img[row * S:(row + 1) * S, f * S:(f + 1) * S] = FX.band(np.clip(v * 1.1, 0, 1), SKY)


def spark_row(img, row):
    """Where a bolt lands: a white flash, then rays flying out, thinning."""
    for f in range(N):
        rng = random.Random(800)
        u = f / (N - 1.0)
        v = np.clip(1 - np.hypot(XS - 32, YS - 32) / (13 * (1 - u) + 2), 0, 1) * (1 - u * 0.5)
        for k in range(8):
            a = rng.random() * TAU
            r0, r1 = 4 + 24 * u, 8 + 28 * u
            pts = [(32 + math.cos(a) * (r0 + (r1 - r0) * j / 3.0), 32 + math.sin(a) * (r0 + (r1 - r0) * j / 3.0)) for j in range(4)]
            v = np.maximum(v, (1 - u) * st(pts, [1.5 * (1 - u) + 0.4] * 4, 1.2))
        img[row * S:(row + 1) * S, f * S:(f + 1) * S] = FX.band(np.clip(v * 1.1, 0, 1), SKY)


def main():
    img = np.zeros((8 * S, N * S, 4), dtype=np.uint8)
    for r, (seed, forks) in enumerate(((1, 2), (2, 3), (3, 1), (4, 2))):
        bolt_row(img, r, seed, forks)
    zap_row(img, 4)
    arc_row(img, 5)
    shock_row(img, 6)
    spark_row(img, 7)
    os.makedirs('build', exist_ok=True)
    Image.fromarray(img, 'RGBA').save('build/astra_lightning.png')
    names = ['BOLT A', 'BOLT B', 'BOLT C', 'BOLT D', 'ZAP', 'ARC', 'SHOCK', 'SPARK']
    big = Image.fromarray(img, 'RGBA').resize((N * S * 3, 8 * S * 3), Image.NEAREST)
    prev = Image.new('RGBA', (big.width + 90, big.height), (14, 12, 28, 255))
    prev.alpha_composite(big, (90, 0))
    d = ImageDraw.Draw(prev)
    for r, nm in enumerate(names):
        d.text((6, r * S * 3 + 40), nm, fill=(220, 220, 255, 255))
    prev.save('build/lightning_preview.png')
    print('ok')


if __name__ == '__main__':
    main()

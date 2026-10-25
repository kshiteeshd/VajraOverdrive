"""PETAL sprite sheet for the Brahmastra's swarm (Senbonzakura-style petals), 32 x 32 cells, eight frames a row, drawn as chunky pixel art: each petal is made on a 16 x 16 grid and
doubled, so the pixels are 2 x 2 like the reference petal (a pointed teardrop in three flat tones: a light rim up and left, the body, a dark rim down and right).

    python tools/enemy_art/petal.py   ->   build/astra_petal.png    (256 x 192)   rows: PINK, then the five elements (Akasha, Vayu, Agni, Jala, Prithvi)
                                           build/petal_preview.png  the sheet on a dark and a light ground, labelled

Eight frames: the petal TUMBLES, a full turn in the plane while it turns over (it narrows to an edge and shows its darker underside), and the loop closes."""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

G = 16                 # the petal's own grid; the cell is 32 so every pixel is doubled
N = 8
TAU = math.pi * 2

# (light, body, shadow): the pink is the reference's own
ROWS = [
    ('PINK', (255, 163, 255), (255, 136, 255), (198, 112, 198)),
    ('AKASHA', (214, 200, 255), (184, 163, 255), (126, 108, 200)),
    ('VAYU', (240, 248, 255), (219, 240, 255), (150, 176, 208)),
    ('AGNI', (255, 170, 90), (255, 115, 26), (190, 70, 20)),
    ('JALA', (130, 236, 246), (64, 209, 230), (36, 138, 170)),
    ('PRITHVI', (232, 200, 110), (209, 168, 71), (140, 104, 44)),
]


def teardrop(rot, squash):
    """The petal's mask on the 16 x 16 grid: pointed at the top, round at the bottom, turned by rot and narrowed to `squash` across."""
    ys, xs = np.mgrid[0:G, 0:G]
    px, py = xs + 0.5 - G / 2.0, ys + 0.5 - G / 2.0
    c, s = math.cos(-rot), math.sin(-rot)
    ux, uy = px * c - py * s, px * s + py * c                      # back into the petal's own frame
    ux = ux / max(squash, 0.05)
    t = (5.7 - uy) / 11.4                                          # 0 at the round bottom, 1 at the tip
    hw = np.where(t < 0.4, 4.9 * np.sqrt(np.clip(1 - ((t - 0.4) / 0.4) ** 2, 0, 1)), 4.9 * np.clip(1 - (t - 0.4) / 0.6, 0, 1) ** 1.15)
    return (np.abs(ux) < hw) & (t >= 0) & (t <= 1)


def frame(f, pal, under_tint):
    rot = f * TAU / N
    flip = math.cos(f * TAU / N + 0.6)
    squash = math.copysign(max(0.3, abs(flip)), flip)
    m = teardrop(rot, abs(squash))
    hi, body, sh = pal
    if squash < 0:                                                 # the underside: one tone darker
        hi, body, sh = body, sh, tuple(int(v * 0.7) for v in sh)
    out = np.zeros((G, G, 4), dtype=np.uint8)
    up = np.zeros_like(m)
    up[1:, 1:] = m[:-1, :-1]                                       # the neighbour up and left
    dn = np.zeros_like(m)
    dn[:-1, :-1] = m[1:, 1:]                                       # the neighbour down and right
    rim_hi = m & ~up
    rim_sh = m & ~dn
    out[m] = body + (255,)
    out[rim_hi & ~rim_sh] = hi + (255,)
    out[rim_sh] = sh + (255,)
    return out


def main():
    sheet = np.zeros((len(ROWS) * 32, N * 32, 4), dtype=np.uint8)
    for r, (_, hi, body, sh) in enumerate(ROWS):
        for f in range(N):
            cell = frame(f, (hi, body, sh), None)
            big = np.repeat(np.repeat(cell, 2, axis=0), 2, axis=1)
            sheet[r * 32:(r + 1) * 32, f * 32:(f + 1) * 32] = big
    os.makedirs('build', exist_ok=True)
    Image.fromarray(sheet, 'RGBA').save('build/astra_petal.png')
    img = Image.fromarray(sheet, 'RGBA').resize((N * 32 * 3, len(ROWS) * 32 * 3), Image.NEAREST)
    prev = Image.new('RGBA', (img.width + 90, img.height), (22, 20, 34, 255))
    prev.alpha_composite(img, (90, 0))
    light = Image.new('RGBA', (img.width, img.height), (245, 245, 245, 255))
    light.alpha_composite(img, (0, 0))
    both = Image.new('RGBA', (prev.width, prev.height * 2), (0, 0, 0, 255))
    both.paste(prev, (0, 0))
    both.paste(light, (90, prev.height))
    d = ImageDraw.Draw(both)
    for r, row in enumerate(ROWS):
        d.text((6, r * 96 + 44), row[0], fill=(230, 230, 255, 255))
    both.save('build/petal_preview.png')
    print('ok')


if __name__ == '__main__':
    main()

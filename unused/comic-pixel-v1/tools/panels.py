"""The nine illustrations of the lore comic, each drawn at native pixel size by a function that takes
(width, height) and returns an image. Composition notes sit with each panel."""
import math

from PIL import Image, ImageDraw

from art import *  # noqa: F401,F403


def vignette(im, strength=0.55):
    """Darken the corners so the eye stays in the middle of the panel."""
    w, h = im.size
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    px = ov.load()
    for y in range(h):
        for x in range(w):
            dx, dy = (x - w / 2) / (w / 2), (y - h / 2) / (h / 2)
            d = max(0.0, (dx * dx + dy * dy) ** 0.5 - 0.55) / 0.8
            if d * 16 * strength > BAYER[y % 4][x % 4] + 0.5:
                px[x, y] = VOID + (255,)
    im.alpha_composite(ov)


def swarga_city(im, base, scale=1.0, tint=1.0):
    """The golden city of the gods: towers of different heights on a bed of cloud."""
    towers = [(0.06, 0.8, 22, 56), (0.17, 0.5, 18, 80), (0.27, 0.95, 26, 66), (0.38, 0.6, 20, 96),
              (0.50, 0.7, 24, 74), (0.62, 0.45, 18, 100), (0.73, 0.9, 28, 70), (0.85, 0.55, 20, 88),
              (0.94, 0.8, 22, 58)]
    w = im.width
    for fx, depth, tw, th in towers:
        spire(im, w * fx, base, tw * scale, th * scale, mix(PEARL, INDIGO, 0.55 * (1 - depth)), mix(LAV, INDIGO, 0.6 * (1 - depth)), GOLD)


# ============================================================================== PAGE 1
def p1a(w, h):
    """SWARGA. A dawn of gold over a city of towers; Indra on a cloud, the bolt over his head, lightning
    to the corners of the sky. The widest panel on the page: the world the story takes away."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, INDIGO), (0.35, DUSK), (0.62, VIOLET), (0.82, (214, 150, 176)), (1.0, CREAM)])
    stars(im, (0, 0, w, int(h * 0.55)), 70)
    glow(im, w * 0.5, h * 0.78, 130, GOLD, 0.5, 6)
    swarga_city(im, int(h * 0.86), 1.05)
    for i, (cx, cy, cw) in enumerate([(-10, 0.80, 90), (50, 0.9, 110), (150, 0.86, 120), (240, 0.9, 110), (290, 0.82, 80)]):
        cloud(im, cx, int(h * cy), cw)
    # lightning from the bolt to the sky's edges
    bx, by = 222, 52
    for tx, ty in [(150, 0), (300, 0), (90, 30), (318, 40), (190, 8)]:
        lightning(im, bx, by, tx, ty, WHITE, LAVD, 6, 8)
    cloud(im, 180, int(h * 0.80), 100, WHITE, LAV, LAVD)
    indra(im, 232, int(h * 0.835), 1.9)
    vignette(im, 0.5)
    return im


def p1b(w, h):
    """DADHICHI. Warm gold, the sage in the middle, his bones rising out of him as light and curling into
    one line - the bolt that is about to exist."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, (60, 28, 14)), (0.5, (150, 80, 30)), (1, (255, 190, 90))])
    glow(im, w / 2, h * 0.55, 80, CREAM, 0.7, 6)
    for k in range(14):  # rays
        a = k * math.pi / 7
        line(im, [(w / 2, h * 0.5), (w / 2 + math.cos(a) * 140, h * 0.5 + math.sin(a) * 140)], mix(GOLDM, (150, 80, 30), 0.5), 1)
    sage(im, w / 2, h * 0.86, 1.5)
    # the bones, rising in an arc and thinning into a thread of light
    for i in range(7):
        t = i / 6
        x = w * (0.2 + 0.6 * t)
        y = h * (0.34 - 0.2 * math.sin(math.pi * t))
        a = -35 + 70 * t
        bone = Image.new("RGBA", (16, 8), (0, 0, 0, 0))
        bd = ImageDraw.Draw(bone)
        bd.rectangle([3, 3, 12, 4], fill=WHITE)
        for ex in (2, 13):
            bd.ellipse([ex - 2, 1, ex + 1, 3], fill=WHITE)
            bd.ellipse([ex - 2, 4, ex + 1, 6], fill=WHITE)
        paste_rot(im, bone, x, y, a, 1.2)
        glow(im, x, y, 9, GOLD, 0.5, 3)
    line(im, [(w * 0.2, h * 0.34), (w * 0.35, h * 0.2), (w * 0.5, h * 0.17), (w * 0.65, h * 0.2), (w * 0.8, h * 0.34)], mix(GOLD, WHITE, 0.4), 1)
    vignette(im, 0.6)
    return im


def p1c(w, h):
    """THE BOLT. Held up alone in the dark: white-hot at the prongs, gold at the ring, a halo and a few
    forks of lightning leaving it. The most iconic object in the story gets a panel to itself."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.5, INDIGO), (1, DUSK)])
    stars(im, (0, 0, w, h), 40)
    glow(im, w / 2, h / 2, 90, GOLD, 0.6, 7)
    glow(im, w / 2, h / 2, 50, WHITE, 0.5, 5)
    for tx, ty in [(10, 12), (w - 10, 14), (14, h - 14), (w - 12, h - 10)]:
        lightning(im, w / 2, h / 2, tx, ty, WHITE, LAVD, 6, 7)
    sp = vajra(112)
    paste_rot(im, sp, w / 2, h / 2, 28, 1.25)
    ImageDraw.Draw(im).ellipse([w / 2 - 54, h / 2 - 54, w / 2 + 54, h / 2 + 54], outline=mix(GOLD, INDIGO, 0.5))
    ImageDraw.Draw(im).ellipse([w / 2 - 60, h / 2 - 60, w / 2 + 60, h / 2 + 60], outline=mix(GOLD, INDIGO, 0.75))
    vignette(im, 0.55)
    return im


def p1d(w, h):
    """THE SHADOW. Swarga small at the foot of the panel, the sky going out like a lamp, and a foot
    large enough to put out the stars lowering into the top of the frame. The reader sees the foot
    before they are told whose it is."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.6, INDIGO), (1, (190, 130, 150))])
    stars(im, (0, 0, w, int(h * 0.5)), 24)
    swarga_city(im, h - 6, 0.62)
    cloud(im, 0, h - 22, 120)
    cloud(im, 160, h - 20, 150)
    # the foot: drawn dark with a thin rim of cold light on the side the dawn reaches
    big = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    foot(big, w * 0.52, h * 0.42, w * 0.60, h * 1.05, (22, 18, 44), (12, 10, 26), (60, 50, 110), down=True)
    rim = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    foot(rim, w * 0.52 - 2, h * 0.42 + 2, w * 0.60, h * 1.05, LAVD, LAVD, GOLD, down=True)
    im.alpha_composite(rim)
    im.alpha_composite(big)
    return im


# ============================================================================== PAGE 2
def p2a(w, h):
    """THE THREE STEPS, one over the other: the first foot on the earth, the second in the sky, the third
    reaching into the gold of Swarga at the top. A tall panel, to be read bottom to top."""
    im = new(w, h)
    gradient(im, (0, 0, w, int(h * 0.30)), [(0, GOLDM), (1, CREAM)], True)
    gradient(im, (0, int(h * 0.30), w, int(h * 0.68)), [(0, (214, 150, 176)), (0.5, VIOLET), (1, DUSK)], True)
    gradient(im, (0, int(h * 0.68), w, h), [(0, (70, 80, 110)), (1, GREEND)], True)
    stars(im, (0, int(h * 0.3), w, int(h * 0.66)), 18)
    mountains(im, h, GREEND, [(0, h * 0.9), (24, h * 0.84), (50, h * 0.9), (80, h * 0.82), (w, h * 0.92)], w)
    cloud(im, 4, int(h * 0.5), 70)
    cloud(im, 40, int(h * 0.38), 80)
    swarga_city(im, int(h * 0.27), 0.5)
    # the giant: a column of leg climbing off the top, three footprints on a rising line
    shaft = [(w * 0.40, 0), (w * 0.72, 0), (w * 0.8, h * 0.36), (w * 0.28, h * 0.4)]
    poly(im, shaft, mix(INDIGO, VIOLET, 0.35))
    line(im, [(w * 0.40, 0), (w * 0.28, h * 0.4)], LAVD, 1)
    feet = [(w * 0.36, h * 0.86, 0.9), (w * 0.66, h * 0.60, 0.8), (w * 0.52, h * 0.30, 0.7)]
    pts = [(f[0], f[1]) for f in feet]
    for i in range(len(pts) - 1):  # the stride between steps, as a dotted arc
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        for k in range(14):
            t = k / 13
            px, py = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t - math.sin(math.pi * t) * 18
            ell(im, px, py, 1, 1, GOLD)
    for i, (fx, fy, s) in enumerate(feet):
        glow(im, fx, fy, 26, GOLD, 0.35, 3)
        foot(im, fx, fy, 44 * s, 52 * s, mix(PEARL, INDIGO, 0.15 + 0.1 * i), mix(LAV, INDIGO, 0.3), GOLD)
        rings(im, fx, fy + 22 * s, 8, 30, 2, mix(GOLD, INDIGO, 0.3))
    vignette(im, 0.4)
    return im


def p2b(w, h):
    """THE THIRD STEP. The foot comes down through the top of the frame onto the towers; the towers go
    under it, rings of gold spread across the floor of Swarga, and a small figure with a cracked bolt is
    thrown back at the lower left. The loudest panel in the book."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.4, INDIGO), (0.8, (190, 110, 120)), (1, CREAM)])
    stars(im, (0, 0, w, int(h * 0.4)), 24)
    glow(im, w * 0.55, h * 0.82, 110, GOLD, 0.55, 6)
    swarga_city(im, int(h * 0.9), 0.95)
    # the dust of the fall
    for i in range(10):
        cloud(im, w * (0.15 + 0.07 * i), int(h * (0.78 - 0.04 * ((i * 3) % 4))), 38 + 6 * (i % 3), WHITE, LAV, LAVD)
    rings(im, w * 0.55, h * 0.9, 24, 120, 5, GOLD)
    foot(im, w * 0.55, h * 0.30, w * 0.72, h * 1.0, mix(PEARL, INDIGO, 0.2), mix(LAV, INDIGO, 0.4), GOLD, down=True)
    # falling stone
    for i in range(9):
        x = w * (0.18 + golden(i + 2) * 0.64)
        y = h * (0.45 + golden(i + 7) * 0.3)
        poly(im, [(x, y), (x + 4, y + 2), (x + 2, y + 6), (x - 2, y + 3)], mix(PEARL, INDIGO, 0.3))
    # Indra, thrown back, the bolt cracking in his hand
    indra(im, 38, int(h * 0.9), 1.15, True, True)
    for k in range(5):
        a = 30 + k * 28
        line(im, [(26, h * 0.46), (26 + math.cos(math.radians(a)) * 14, h * 0.46 - math.sin(math.radians(a)) * 14)], EMBER, 1)
    vignette(im, 0.5)
    return im


def p2c(w, h):
    """THE FIST. A fist of pearl at the centre, light leaking through the fingers; the bolt is inside it.
    Fine cracks of white run out across the panel - the sound of something breaking."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.5, (30, 20, 50)), (1, (120, 50, 40))])
    glow(im, w / 2, h / 2, 90, EMBER, 0.45, 6)
    glow(im, w / 2, h / 2, 60, GOLD, 0.6, 6)
    for k in range(18):
        a = k * math.pi / 9
        line(im, [(w / 2, h / 2), (w / 2 + math.cos(a) * 160, h / 2 + math.sin(a) * 160)], mix(GOLDM, VOID, 0.35), 1)
    # the fist
    cx, cy = w / 2, h / 2 + 4
    ell(im, cx, cy + 6, 40, 30, mix(PEARL, INDIGO, 0.45))
    for i, dx in enumerate((-27, -9, 9, 27)):
        ell(im, cx + dx, cy - 22, 11, 11, mix(PEARL, INDIGO, 0.25))
        ell(im, cx + dx + 2, cy - 22, 8, 8, PEARL)
        line(im, [(cx + dx + 6, cy - 30), (cx + dx + 6, cy - 14)], mix(LAV, INDIGO, 0.3), 1)
    ell(im, cx - 36, cy + 6, 11, 17, PEARL)
    ell(im, cx, cy + 8, 36, 24, PEARL)
    ell(im, cx + 14, cy + 14, 30, 16, LAV)
    for dx in (-18, 0, 18):  # light between the fingers
        rect(im, cx + dx - 1, cy - 18, cx + dx + 1, cy - 8, GOLD)
        glow(im, cx + dx, cy - 14, 14, GOLD, 0.7, 3)
    # the seven pieces leaving it, fanned up and out
    cols = [LAV, EMBER, TEAL, STONE, GREEN, BLOOD, WHITE]
    for i, c in enumerate(cols):
        a = -160 + i * (140 / 6)
        sx = cx + math.cos(math.radians(a)) * 24
        sy = cy - 10 + math.sin(math.radians(a)) * 20
        ex = cx + math.cos(math.radians(a)) * 74
        ey = cy - 10 + math.sin(math.radians(a)) * 56
        shard(im, ex, ey, 5.5, c, trail=14, angle=a + 180, glowc=c)
    vignette(im, 0.45)
    return im


LOKAS = [("ATALA", LAV, DUSK), ("VITALA", EMBER, (96, 36, 18)), ("SUTALA", TEAL, (16, 72, 90)),
         ("TALATALA", STONE, (70, 52, 36)), ("MAHITALA", GREEN, GREEND), ("RASATALA", BLOOD, (66, 10, 26)),
         ("PATALA", WHITE, (14, 14, 28))]


def p2d(w, h):
    """THE SEVEN LOKAS. The earth as a stack of seven bands, each in its own colour; seven pieces fall
    through them like seven comets and each one stops in its own band with a flare. Labels are added on
    the page, in the margin of each band."""
    im = new(w, h)
    top = 16
    gradient(im, (0, 0, w, top), [(0, DUSK), (1, (210, 150, 170))])
    bh = (h - top) / 7
    for i, (name, col, dark) in enumerate(LOKAS):
        y0 = int(top + i * bh)
        y1 = int(top + (i + 1) * bh)
        gradient(im, (0, y0, w, y1), [(0, mix(dark, col, 0.18)), (1, dark)], True)
        # a cavern floor: a ragged line and a few jewels
        pts = [(0, y1)]
        for k in range(0, w + 1, 14):
            pts.append((k, y1 - 3 - int(4 * golden(k + i * 11))))
        pts.append((w, y1))
        poly(im, pts, mix(dark, VOID, 0.4))
        for j in range(8):
            x = int(30 + golden(j * 4 + i) * (w - 40))
            y = int(y0 + 4 + golden(j * 2 + i * 5) * (bh - 12))
            ImageDraw.Draw(im).point((x, y), fill=col)
        line(im, [(0, y1), (w, y1)], mix(col, VOID, 0.5), 1)
    # seven pieces falling from the top-left to their own band
    for i, (name, col, dark) in enumerate(LOKAS):
        ex = 70 + i * 34
        ey = top + i * bh + bh * 0.55
        sx = ex - 26 - 14 * (6 - i) * 0.4
        for k in range(int(ey)):
            pass
        line(im, [(sx, 0), (ex, ey)], mix(col, VOID, 0.45), 1)
        shard(im, ex, ey, 4.5, col, trail=0, angle=-60, glowc=col)
        glow(im, ex, ey, 16, col, 0.6, 4)
    vignette(im, 0.35)
    return im


# ============================================================================== PAGE 3
def p3a(w, h):
    """THE HOLDER. A hall of pearl and gold, a ruler on his seat with the piece raised in one hand; thin
    lines of light run from the piece to his eyes - the piece is speaking. Nothing about him is cruel."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.55, INDIGO), (1, (60, 44, 74))])
    # pillars, in two receding ranks
    for rank, (x0s, scale) in enumerate([([18, 70, 234, 286], 1.0), ([100, 204], 0.7)]):
        for x in x0s:
            pw = 18 * scale
            rect(im, x - pw / 2, 0, x + pw / 2, h, mix(PEARL, INDIGO, 0.55 + 0.2 * rank))
            rect(im, x + pw * 0.1, 0, x + pw / 2, h, mix(LAV, INDIGO, 0.6 + 0.15 * rank))
            rect(im, x - pw / 2 - 2, 4, x + pw / 2 + 2, 7, GOLD)
            rect(im, x - pw / 2 - 2, h - 8, x + pw / 2 + 2, h - 5, GOLD)
    # jewels that light the hall (no sun here)
    for i in range(30):
        x = int(golden(i * 3) * w)
        y = int(golden(i * 5 + 1) * h * 0.6)
        ImageDraw.Draw(im).point((x, y), fill=(GOLD, SKY, LAV)[i % 3])
    glow(im, w / 2, h * 0.45, 100, GOLD, 0.4, 6)
    # the seat and the ruler
    rect(im, w / 2 - 46, h - 40, w / 2 + 46, h - 8, mix(PEARL, INDIGO, 0.3))
    poly(im, [(w / 2 - 34, h - 40), (w / 2 + 34, h - 40), (w / 2 + 26, h - 88), (w / 2 - 26, h - 88)], PEARL)
    ell(im, w / 2, h - 96, 36, 36, GOLD)         # a halo of gold behind the head
    ell(im, w / 2, h - 96, 31, 31, mix(INDIGO, VIOLET, 0.5))
    poly(im, [(w / 2 - 22, h - 86), (w / 2 + 22, h - 86), (w / 2 + 28, h - 40), (w / 2 - 28, h - 40)], mix(PEARL, INDIGO, 0.15))
    ell(im, w / 2, h - 98, 11, 12, CREAM)
    poly(im, [(w / 2 - 12, h - 106), (w / 2 - 9, h - 120), (w / 2 - 4, h - 111), (w / 2, h - 124), (w / 2 + 4, h - 111),
              (w / 2 + 9, h - 120), (w / 2 + 12, h - 106)], GOLD)
    ell(im, w / 2 - 4, h - 99, 1.4, 1.4, INDIGO)
    ell(im, w / 2 + 4, h - 99, 1.4, 1.4, INDIGO)
    # the raised hand and the piece
    px, py = w / 2 + 54, h - 100
    line(im, [(w / 2 + 22, h - 78), (w / 2 + 44, h - 92), (px, py + 8)], PEARL, 4)
    glow(im, px, py, 34, GOLD, 0.65, 6)
    shard(im, px, py, 8, GOLD, 0, -70, WHITE)
    # lines of whispering light from the piece to his eyes
    for k, dy in enumerate((-2, 3, 8)):
        pts = [(px - 6, py + dy)]
        for t in range(1, 9):
            x = px - 6 - (px - 6 - (w / 2 + 6)) * t / 8
            y = py + dy + (-(py + dy) + (h - 99)) * t / 8 + math.sin(t * 1.2 + k) * 4
            pts.append((x, y))
        line(im, pts, mix(LAV, GOLD, 0.4), 1)
    vignette(im, 0.45)
    return im


RULER_CROWNS = ["pearl", "flame", "wave", "gear", "hood", "horns", "prism"]


def p3b(w, h):
    """SEVEN ALCOVES, seven rulers: one silhouette to a loka, each with a crown of its own and a piece of
    the bolt in its own colour over its head. Nobody is drawn as a villain; every one sits straight."""
    im = new(w, h)
    cw = w / 7
    for i, (name, col, dark) in enumerate(LOKAS):
        x0 = int(i * cw)
        x1 = int((i + 1) * cw)
        gradient(im, (x0, 0, x1, h), [(0, mix(dark, col, 0.12)), (1, dark)], True)
        cx = (x0 + x1) / 2
        glow(im, cx, h * 0.38, 26, col, 0.45, 4)
        # the figure: shoulders, head, a crown
        poly(im, [(cx - 15, h), (cx - 11, h * 0.62), (cx + 11, h * 0.62), (cx + 15, h)], mix(VOID, dark, 0.6))
        ell(im, cx, h * 0.54, 7, 8, mix(VOID, dark, 0.7))
        crown = RULER_CROWNS[i]
        cy = h * 0.54 - 8
        if crown == "pearl":
            poly(im, [(cx - 8, cy), (cx - 6, cy - 10), (cx - 2, cy - 4), (cx, cy - 12), (cx + 2, cy - 4), (cx + 6, cy - 10), (cx + 8, cy)], col)
        elif crown == "flame":
            poly(im, [(cx - 8, cy), (cx - 5, cy - 8), (cx - 2, cy - 2), (cx, cy - 14), (cx + 3, cy - 3), (cx + 6, cy - 9), (cx + 8, cy)], col)
        elif crown == "wave":
            line(im, [(cx - 8, cy - 2), (cx - 4, cy - 8), (cx, cy - 2), (cx + 4, cy - 8), (cx + 8, cy - 2)], col, 2)
        elif crown == "gear":
            rect(im, cx - 8, cy - 7, cx + 8, cy, col)
            for k in range(-2, 3):
                rect(im, cx + k * 4 - 1, cy - 10, cx + k * 4 + 1, cy - 7, col)
        elif crown == "hood":
            ell(im, cx, cy - 4, 12, 10, col)
            ell(im, cx, cy - 4, 8, 7, mix(dark, VOID, 0.5))
        elif crown == "horns":
            poly(im, [(cx - 7, cy), (cx - 12, cy - 12), (cx - 3, cy - 4)], col)
            poly(im, [(cx + 7, cy), (cx + 12, cy - 12), (cx + 3, cy - 4)], col)
        else:
            for k in range(7):
                a = math.pi + k * math.pi / 6
                line(im, [(cx, cy - 4), (cx + math.cos(a) * 14, cy - 4 + math.sin(a) * 12)], WHITE if k % 2 == 0 else LAV, 1)
        shard(im, cx, h * 0.18, 4.5, col, 0, -70, col)
        line(im, [(x1, 0), (x1, h)], VOID, 1)
    return im


def p3c(w, h):
    """THE DESCENT. Indra's ship drops from the gold of Swarga at the top, down a shaft through the dark
    of the earth, toward a ring of light at the bottom: the first door. Long and narrow, to be read as a
    fall."""
    im = new(w, h)
    gradient(im, (0, 0, w, int(h * 0.22)), [(0, GOLDM), (1, (170, 110, 150))])
    gradient(im, (0, int(h * 0.22), w, h), [(0, INDIGO), (0.6, VOID), (1, (40, 30, 50))])
    stars(im, (0, 0, w, int(h * 0.2)), 8)
    # the rock walls of the shaft
    poly(im, [(0, int(h * 0.2)), (w * 0.22, int(h * 0.3)), (w * 0.2, h * 0.7), (w * 0.3, h), (0, h)], mix(INDIGO, VIOLET, 0.25))
    poly(im, [(w, int(h * 0.2)), (w * 0.78, int(h * 0.3)), (w * 0.8, h * 0.7), (w * 0.7, h), (w, h)], mix(INDIGO, VIOLET, 0.25))
    line(im, [(w * 0.22, h * 0.3), (w * 0.2, h * 0.7), (w * 0.3, h)], LAVD, 1)
    line(im, [(w * 0.78, h * 0.3), (w * 0.8, h * 0.7), (w * 0.7, h)], LAVD, 1)
    # the first door: a ring of gold light with pillars
    glow(im, w / 2, h - 12, 70, GOLD, 0.6, 6)
    arch(im, w / 2, h - 2, 74, 40, PEARL, LAV, GOLD)
    # the fall: streaks, then the ship, thrust trailing up
    for k in range(10):
        x = w * (0.34 + 0.32 * golden(k * 2 + 1))
        y = h * (0.3 + 0.5 * golden(k * 3 + 2))
        line(im, [(x, y), (x, y - 18)], mix(LAV, INDIGO, 0.5), 1)
    ship(im, w / 2, h * 0.48, 3)
    glow(im, w / 2, h * 0.56, 28, SKY, 0.5, 4)
    line(im, [(w / 2, h * 0.5), (w / 2, h * 0.3)], SKY, 2)
    vignette(im, 0.4)
    return im


def p3d(w, h):
    """THE DOOR. The gate of Atala seen straight on, the pilot's ship small and bright in the foreground,
    and four of Bala's guard hanging in the arch - the same hulls the player will meet. The last image
    of the comic and the first of the game."""
    im = new(w, h)
    gradient(im, (0, 0, w, h), [(0, VOID), (0.6, INDIGO), (1, (74, 56, 80))])
    glow(im, w / 2, h * 0.5, 90, GOLD, 0.55, 6)
    arch(im, w / 2, h - 4, 120, 96, PEARL, LAV, GOLD)
    for i in range(24):
        ImageDraw.Draw(im).point((int(golden(i * 3) * w), int(golden(i * 5 + 1) * h * 0.5)), fill=(GOLD, SKY, LAV)[i % 3])
    # the four: two spear sentries and two saucer escorts, the game's own art
    sprite(im, "enemies/atala/lance.png", w * 0.34, h * 0.40, 0.55, (0, 0))
    sprite(im, "enemies/atala/lance.png", w * 0.66, h * 0.40, 0.55, (0, 0))
    sprite(im, "enemies/atala/hataka.png", w * 0.22, h * 0.54, 0.6, (0, 0))
    sprite(im, "enemies/atala/hataka.png", w * 0.78, h * 0.54, 0.6, (0, 0))
    ship(im, w / 2, h * 0.84, 3)
    glow(im, w / 2, h * 0.84, 30, SKY, 0.4, 4)
    vignette(im, 0.45)
    return im

"""Pixel-art primitives for the lore comic. Everything is drawn at NATIVE resolution (one pixel here is
four on the page) with non-antialiased shapes, then the page is built at full resolution. Palette is the
game's: ink, indigo, lavender (Akasha), pearl, gold, ember - docs/ENEMY_BIBLE.md has the colour code.

Nothing is random: scatter (stars, jewels, embers) comes from a fixed golden-ratio sequence, so the
same run writes the same pages."""
import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
ASSETS = os.path.join(HERE, "..", "..", "assets")

INK = (13, 13, 26)
VOID = (6, 6, 14)
INDIGO = (24, 22, 52)
DUSK = (44, 38, 88)
VIOLET = (82, 66, 140)
LAV = (217, 208, 232)
LAVD = (160, 140, 220)
PEARL = (232, 230, 224)
WHITE = (255, 252, 241)
CREAM = (255, 244, 216)
GOLD = (255, 215, 0)
GOLDM = (254, 194, 61)
BRONZE = (184, 134, 11)
AMBER = (133, 77, 0)
EMBER = (255, 86, 34)
EMBERD = (190, 52, 22)
SKY = (150, 205, 255)
SKYD = (64, 116, 206)
GREEN = (52, 150, 84)
GREEND = (20, 70, 40)
TEAL = (40, 150, 160)
BLOOD = (150, 30, 52)
STONE = (150, 120, 84)

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def golden(i):
    """The i-th point of a fixed low-discrepancy sequence in [0,1): the 'scatter' of this comic."""
    return (i * 0.6180339887498949) % 1.0


def new(w, h, color=INK):
    return Image.new("RGBA", (w, h), color + (255,))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _color_at(stops, t):
    for (ta, ca), (tb, cb) in zip(stops, stops[1:]):
        if ta <= t <= tb:
            return mix(ca, cb, (t - ta) / max(1e-6, tb - ta))
    return stops[-1][1]


def gradient(im, box, stops, vertical=True, bands=12):
    """A multi-stop gradient as pixel-art BANDS: each band is one flat colour, and only the seam between
    two bands is dithered. It reads as a painted sky, not as noise."""
    x0, y0, x1, y1 = box
    px = im.load()
    n = (y1 - y0) if vertical else (x1 - x0)
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = ((y - y0) if vertical else (x - x0)) / max(1, n - 1)
            u = t * bands
            b = min(bands - 1, int(u))
            k = u - b
            c = _color_at(stops, (b + 0.5) / bands)
            if k > 0.72:
                cn = _color_at(stops, min(1.0, (b + 1.5) / bands))
                if (k - 0.72) / 0.28 * 16 > BAYER[y % 4][x % 4] + 0.5:
                    c = cn
            px[x, y] = c + (255,)


def glow(im, cx, cy, r, color, strength=0.5, steps=6):
    """Banded radial glow: concentric discs at falling alpha."""
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for i in range(steps, 0, -1):
        rr = r * i / steps
        a = int(255 * strength * (1 - i / (steps + 1)) / 1.6)
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=color + (a,))
    im.alpha_composite(ov)


def stars(im, box, n, bright=(WHITE, LAV, GOLDM)):
    x0, y0, x1, y1 = box
    px = im.load()
    for i in range(n):
        x = int(x0 + golden(i * 3 + 1) * (x1 - x0 - 1))
        y = int(y0 + golden(i * 5 + 2) * (y1 - y0 - 1))
        c = bright[i % len(bright)]
        px[x, y] = c + (255,)
        if i % 7 == 0 and x + 1 < x1:
            px[x + 1, y] = c + (255,)


def cloud(im, x, y, w, light=WHITE, shade=LAV, dark=LAVD):
    d = ImageDraw.Draw(im)
    h = max(6, w // 4)
    for k, (dx, dy, rw, rh) in enumerate([(0.0, 0.5, 0.55, 0.5), (0.25, 0.2, 0.5, 0.7), (0.5, 0.45, 0.5, 0.55),
                                          (0.3, 0.55, 0.7, 0.45)]):
        d.ellipse([x + dx * w, y + dy * h, x + (dx + rw) * w, y + (dy + rh) * h * 1.3], fill=dark)
    for dx, dy, rw, rh in [(0.0, 0.4, 0.55, 0.5), (0.25, 0.1, 0.5, 0.7), (0.5, 0.35, 0.5, 0.55), (0.3, 0.45, 0.7, 0.45)]:
        d.ellipse([x + dx * w, y + dy * h, x + (dx + rw) * w, y + (dy + rh) * h * 1.3], fill=shade)
    for dx, dy, rw, rh in [(0.05, 0.3, 0.45, 0.4), (0.28, 0.05, 0.4, 0.5), (0.52, 0.28, 0.4, 0.4)]:
        d.ellipse([x + dx * w, y + dy * h, x + (dx + rw) * w, y + (dy + rh) * h * 1.3], fill=light)


def poly(im, pts, fill, outline=None):
    d = ImageDraw.Draw(im)
    d.polygon([(round(x), round(y)) for x, y in pts], fill=fill, outline=outline)


def line(im, pts, fill, w=1):
    ImageDraw.Draw(im).line([(round(x), round(y)) for x, y in pts], fill=fill, width=w)


def ell(im, cx, cy, rx, ry, fill, outline=None):
    ImageDraw.Draw(im).ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=fill, outline=outline)


def rect(im, x0, y0, x1, y1, fill):
    ImageDraw.Draw(im).rectangle([round(x0), round(y0), round(x1), round(y1)], fill=fill)


# ------------------------------------------------------------------ the pieces of the story ---------

def spire(im, cx, base, w, h, body=PEARL, shade=LAV, trim=GOLD):
    """A temple tower of Swarga: stepped tiers, a gold kalasha on top."""
    tiers = 5
    for i in range(tiers):
        tw = w * (1 - i * 0.17)
        ty1 = base - h * i / tiers * 0.82
        ty0 = base - h * (i + 1) / tiers * 0.82
        rect(im, cx - tw / 2, ty0, cx + tw / 2, ty1, body)
        rect(im, cx + tw * 0.12, ty0, cx + tw / 2, ty1, shade)
        rect(im, cx - tw / 2 - 1, ty0, cx + tw / 2 + 1, ty0 + 1, trim)
    top = base - h * 0.82
    poly(im, [(cx - w * 0.14, top), (cx + w * 0.14, top), (cx, top - h * 0.14)], trim)
    line(im, [(cx, top - h * 0.14), (cx, top - h * 0.2)], trim, 1)
    ell(im, cx, top - h * 0.22, max(1, w * 0.05), max(1, w * 0.05), WHITE)


def mountains(im, y_base, color, tops, width):
    pts = [(0, y_base)]
    for x, y in tops:
        pts.append((x, y))
    pts.append((width, y_base))
    poly(im, pts, color)


def lightning(im, x0, y0, x1, y1, color=WHITE, halo=LAV, kinks=7, jag=9):
    pts = [(x0, y0)]
    for i in range(1, kinks):
        t = i / kinks
        j = (golden(i * 7 + int(x0)) - 0.5) * 2 * jag
        pts.append((x0 + (x1 - x0) * t + j, y0 + (y1 - y0) * t))
    pts.append((x1, y1))
    line(im, pts, halo, 3)
    line(im, pts, color, 1)


def _bez(p0, p1, p2, n=14):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in [i / n for i in range(n + 1)]]


def vajra(size=56, core=GOLD, rib=GOLDM, edge=WHITE):
    """The Vajrayudha, lying along x on its own small canvas: a ribbed sphere for a grip, a lotus ring at
    each end of it, and at each end a CAGE of four curved prongs that close on one point - the way the
    bolt is carved in the old stone. White-hot along the prongs' outer edges."""
    w, h = size, max(20, int(size * 0.62))
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cx, cy = w / 2, h / 2
    d = ImageDraw.Draw(im)
    r = max(2, size // 22)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=BRONZE)                     # the grip
    d.line([(cx - r, cy), (cx + r, cy)], fill=rib)
    for s_ in (-1, 1):
        for k in (1, 2):
            rx = cx + s_ * (r + 1 + k * 2)
            d.ellipse([rx - 1, cy - r - 1, rx + 1, cy + r + 1], fill=core if k == 1 else rib)   # the lotus rings
        bx = cx + s_ * (r + 7)
        tip = cx + s_ * (w / 2 - 1)
        span = (tip - bx) * s_
        for dy, wid in ((-1, 2), (1, 2)):                                           # the two curved outer prongs
            pts = _bez((bx, cy + dy * 2), (bx + s_ * span * 0.5, cy + dy * h * 0.62), (tip, cy))
            d.line([(round(x), round(y)) for x, y in pts], fill=core, width=wid)
            d.line([(round(x), round(y - dy * 0)) for x, y in pts[2:-1]], fill=edge, width=1)
        for dy in (-1, 1):                                                          # the two shorter inner prongs
            pts = _bez((bx, cy + dy), (bx + s_ * span * 0.5, cy + dy * h * 0.2), (tip - s_ * span * 0.18, cy))
            d.line([(round(x), round(y)) for x, y in pts], fill=rib, width=1)
        d.line([(round(bx), round(cy)), (round(tip), round(cy))], fill=edge, width=1)       # the centre spike
        d.ellipse([tip - 1, cy - 1, tip + 1, cy + 1], fill=edge)
    return im


def paste_rot(im, sprite, cx, cy, angle, scale=1):
    sp = sprite.resize((int(sprite.width * scale), int(sprite.height * scale)), Image.NEAREST) if scale != 1 else sprite
    sp = sp.rotate(angle, expand=True, resample=Image.NEAREST)
    im.alpha_composite(sp, (int(cx - sp.width / 2), int(cy - sp.height / 2)))


def indra(im, x, y, s=1.0, arm_up=True, bolt=True, cape=LAVD, glowc=LAV):
    """Indra: a king of the gods in pearl and lavender, a gold crown with a jewel, pauldrons, a dhoti with
    a gold hem, a cape lifting behind him, one arm thrown up with the bolt. (x, y) is between his feet."""
    h = 52 * s
    # cape: a long sweep behind, blown to one side
    poly(im, [(x - 7 * s, y - h * 0.76), (x + 6 * s, y - h * 0.76), (x + 24 * s, y - h * 0.18), (x + 18 * s, y - 1 * s),
              (x + 4 * s, y - h * 0.3), (x - 14 * s, y - 2 * s)], cape)
    poly(im, [(x + 6 * s, y - h * 0.76), (x + 24 * s, y - h * 0.18), (x + 18 * s, y - 1 * s), (x + 4 * s, y - h * 0.3)], mix(cape, INDIGO, 0.35))
    # legs and boots
    for sx in (-1, 1):
        poly(im, [(x + sx * 2 * s, y - h * 0.34), (x + sx * 7 * s, y - h * 0.34), (x + sx * 8.5 * s, y - 3 * s), (x + sx * 3 * s, y - 3 * s)], INDIGO)
        poly(im, [(x + sx * 2 * s, y - 4 * s), (x + sx * 9 * s, y - 4 * s), (x + sx * 10 * s, y), (x + sx * 2 * s, y)], GOLD)
    # dhoti
    poly(im, [(x - 9 * s, y - h * 0.5), (x + 9 * s, y - h * 0.5), (x + 11 * s, y - h * 0.3), (x - 11 * s, y - h * 0.3)], LAV)
    rect(im, x - 11 * s, y - h * 0.3 - max(1, s), x + 11 * s, y - h * 0.3, GOLD)
    # torso: a taper to the waist, a chest plate
    poly(im, [(x - 11 * s, y - h * 0.8), (x + 11 * s, y - h * 0.8), (x + 8 * s, y - h * 0.5), (x - 8 * s, y - h * 0.5)], PEARL)
    poly(im, [(x + 2 * s, y - h * 0.8), (x + 11 * s, y - h * 0.8), (x + 8 * s, y - h * 0.5), (x + 2 * s, y - h * 0.5)], LAV)
    poly(im, [(x - 7 * s, y - h * 0.78), (x + 7 * s, y - h * 0.78), (x + 5 * s, y - h * 0.62), (x - 5 * s, y - h * 0.62)], GOLD)
    line(im, [(x, y - h * 0.78), (x, y - h * 0.62)], BRONZE, 1)
    rect(im, x - 8 * s, y - h * 0.52, x + 8 * s, y - h * 0.52 + max(1, s), GOLD)             # belt
    # pauldrons
    ell(im, x - 11 * s, y - h * 0.78, 4 * s, 3.2 * s, GOLD)
    ell(im, x + 11 * s, y - h * 0.78, 4 * s, 3.2 * s, GOLD)
    # head, hair, crown, halo
    glow(im, x, y - h * 0.9, 16 * s, glowc, 0.55, 5)
    ell(im, x, y - h * 0.9, 4.6 * s, 5.2 * s, CREAM)
    poly(im, [(x - 4.8 * s, y - h * 0.93), (x - 4 * s, y - h * 0.99), (x + 4 * s, y - h * 0.99), (x + 4.8 * s, y - h * 0.93),
              (x + 4 * s, y - h * 0.96), (x - 4 * s, y - h * 0.96)], DUSK)
    poly(im, [(x - 5.4 * s, y - h * 0.97), (x - 4.4 * s, y - h * 1.1), (x - 2 * s, y - h * 1.02), (x, y - h * 1.16),
              (x + 2 * s, y - h * 1.02), (x + 4.4 * s, y - h * 1.1), (x + 5.4 * s, y - h * 0.97)], GOLD)
    ell(im, x, y - h * 1.04, max(1, 1.1 * s), max(1, 1.1 * s), SKY)
    ell(im, x - 1.6 * s, y - h * 0.9, max(0.6, 0.5 * s), max(0.6, 0.5 * s), INDIGO)
    ell(im, x + 1.6 * s, y - h * 0.9, max(0.6, 0.5 * s), max(0.6, 0.5 * s), INDIGO)
    # the arms: the bolt arm thrown up and out, the other open at the side
    sw = max(2, int(3 * s))
    if arm_up:
        line(im, [(x - 11 * s, y - h * 0.78), (x - 17 * s, y - h * 1.0), (x - 15 * s, y - h * 1.22)], PEARL, sw)
        line(im, [(x - 16.5 * s, y - h * 1.04), (x - 15.5 * s, y - h * 1.12)], GOLD, sw + 1)
        if bolt:
            sp = vajra(int(40 * s))
            paste_rot(im, sp, x - 15 * s, y - h * 1.36, 90)
            glow(im, x - 15 * s, y - h * 1.36, 26 * s, GOLD, 0.6, 5)
    else:
        line(im, [(x - 11 * s, y - h * 0.78), (x - 14 * s, y - h * 0.52)], PEARL, sw)
    line(im, [(x + 11 * s, y - h * 0.78), (x + 16 * s, y - h * 0.58)], PEARL, sw)
    ell(im, x + 16.5 * s, y - h * 0.56, 1.8 * s, 1.8 * s, CREAM)


def foot(im, cx, cy, w, h, skin=PEARL, shade=LAV, nail=GOLD, down=False):
    """A colossal bare foot seen from below: a broad sole, a heel, five toes with nails, the line of an
    arch. Toes point UP the canvas by default; `down` flips it so the toes lead - a foot coming down."""
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ell(layer, cx, cy + h * 0.14, w * 0.40, h * 0.40, shade)                         # heel, behind
    ell(layer, cx, cy, w * 0.43, h * 0.46, skin)                                      # the sole
    ell(layer, cx - w * 0.05, cy - h * 0.06, w * 0.30, h * 0.36, mix(skin, shade, 0.35))   # the arch hollow
    for dx, rr in [(-0.35, 0.115), (-0.18, 0.125), (0.0, 0.13), (0.18, 0.12), (0.34, 0.1)]:
        ty = cy - h * (0.5 + 0.05 * (1 - abs(dx) * 1.4))
        ell(layer, cx + dx * w, ty, w * rr * 1.08, w * rr * 1.14, shade)
        ell(layer, cx + dx * w, ty - 1, w * rr, w * rr * 1.06, skin)
        ell(layer, cx + dx * w, ty - w * rr * 0.62, w * rr * 0.5, w * rr * 0.34, nail)    # the nail
    line(layer, [(cx - w * 0.22, cy + h * 0.16), (cx - w * 0.05, cy - h * 0.12), (cx + w * 0.2, cy + h * 0.12)], shade, 2)
    if down:
        layer = layer.transpose(Image.FLIP_TOP_BOTTOM)
        layer = layer.transform(layer.size, Image.AFFINE, (1, 0, 0, 0, 1, -(im.size[1] - 2 * cy)), Image.NEAREST)
    im.alpha_composite(layer)


def rings(im, cx, cy, r0, r1, n, color=GOLD, squash=0.35):
    for i in range(n):
        r = r0 + (r1 - r0) * i / max(1, n - 1)
        ImageDraw.Draw(im).ellipse([cx - r, cy - r * squash, cx + r, cy + r * squash], outline=color)


def shard(im, x, y, size, color, trail=0, angle=-60, glowc=None):
    """A piece of the bolt: a small faceted splinter with a comet tail."""
    a = math.radians(angle)
    ux, uy = math.cos(a), math.sin(a)
    if trail:
        for k in range(int(trail)):
            t = k / trail
            c = mix(color, INK, t)
            ell(im, x - ux * k * 1.3, y - uy * k * 1.3, max(0.6, size * 0.35 * (1 - t)), max(0.6, size * 0.35 * (1 - t)), c)
    pts = [(x + ux * size, y + uy * size), (x - uy * size * 0.45, y + ux * size * 0.45),
           (x - ux * size * 0.7, y - uy * size * 0.7), (x + uy * size * 0.45, y - ux * size * 0.45)]
    poly(im, pts, color)
    poly(im, [pts[0], pts[1], (x, y)], WHITE)
    if glowc:
        glow(im, x, y, size * 2.4, glowc, 0.55, 4)


def sage(im, x, y, s=1.0):
    """Dadhichi, seated in padmasana: crossed legs, a straight back, hands in the mudra, an ash-white
    beard, a bun of hair, a ring of light behind the head. His bones are leaving him as light."""
    robe = (214, 140, 70)
    robed = (150, 86, 36)
    glow(im, x, y - 30 * s, 52 * s, GOLDM, 0.5, 6)
    # the halo: two rings and a lotus of petals
    for k in range(12):
        a = k * math.pi / 6
        px, py = x + math.cos(a) * 19 * s, y - 34 * s + math.sin(a) * 19 * s
        poly(im, [(px, py), (px + math.cos(a) * 5 * s, py + math.sin(a) * 5 * s),
                  (px + math.cos(a + 0.3) * 2 * s, py + math.sin(a + 0.3) * 2 * s)], GOLD)
    ImageDraw.Draw(im).ellipse([x - 17 * s, y - 34 * s - 17 * s, x + 17 * s, y - 34 * s + 17 * s], outline=GOLD)
    # crossed legs
    ell(im, x, y - 3 * s, 21 * s, 6.5 * s, robed)
    ell(im, x - 4 * s, y - 5 * s, 17 * s, 5 * s, robe)
    # torso, shoulders, arms folded to the mudra
    poly(im, [(x - 10 * s, y - 8 * s), (x + 10 * s, y - 8 * s), (x + 8 * s, y - 28 * s), (x - 8 * s, y - 28 * s)], robe)
    poly(im, [(x + 2 * s, y - 8 * s), (x + 10 * s, y - 8 * s), (x + 8 * s, y - 28 * s), (x + 2 * s, y - 28 * s)], robed)
    line(im, [(x - 8 * s, y - 27 * s), (x - 13 * s, y - 14 * s), (x - 6 * s, y - 9 * s)], robe, max(2, int(3 * s)))
    line(im, [(x + 8 * s, y - 27 * s), (x + 13 * s, y - 14 * s), (x + 6 * s, y - 9 * s)], robed, max(2, int(3 * s)))
    ell(im, x, y - 9 * s, 3 * s, 2 * s, CREAM)                                      # the joined hands
    # head: face, closed eyes, beard, hair and a bun
    ell(im, x, y - 33 * s, 6 * s, 6.6 * s, CREAM)
    poly(im, [(x - 6 * s, y - 31 * s), (x + 6 * s, y - 31 * s), (x + 3 * s, y - 24 * s), (x, y - 21 * s), (x - 3 * s, y - 24 * s)], WHITE)
    poly(im, [(x - 6.4 * s, y - 35 * s), (x - 5 * s, y - 40 * s), (x + 5 * s, y - 40 * s), (x + 6.4 * s, y - 35 * s)], DUSK)
    ell(im, x, y - 42.5 * s, 3.2 * s, 3.2 * s, DUSK)
    line(im, [(x - 3.4 * s, y - 34 * s), (x - 1.4 * s, y - 34 * s)], AMBER, 1)
    line(im, [(x + 1.4 * s, y - 34 * s), (x + 3.4 * s, y - 34 * s)], AMBER, 1)
    ell(im, x, y - 38 * s, max(0.7, 0.6 * s), max(0.7, 0.6 * s), GOLD)              # the mark on the brow


def ship(im, x, y, scale=2, name="mk_1/ship_1.png"):
    sp = Image.open(os.path.join(ASSETS, "vajra_ships", name)).convert("RGBA")
    sp = sp.resize((sp.width * scale, sp.height * scale), Image.NEAREST)
    im.alpha_composite(sp, (int(x - sp.width / 2), int(y - sp.height / 2)))


def sprite(im, path, x, y, scale=1, cell=None, flip=False):
    """A frame of one of the game's own sheets (cell = (col, row) of a 64x64 grid) or a plain image."""
    sp = Image.open(os.path.join(ASSETS, path)).convert("RGBA")
    if cell is not None:
        sp = sp.crop((cell[0] * 64, cell[1] * 64, cell[0] * 64 + 64, cell[1] * 64 + 64))
    if scale != 1:
        sp = sp.resize((int(sp.width * scale), int(sp.height * scale)), Image.NEAREST)
    if flip:
        sp = sp.transpose(Image.FLIP_LEFT_RIGHT)
    im.alpha_composite(sp, (int(x - sp.width / 2), int(y - sp.height / 2)))


def arch(im, cx, base, w, h, col=PEARL, shade=LAV, trim=GOLD, label=None):
    """The grand gate: two pillars, a stepped arch, a gold keystone."""
    pw = w * 0.16
    for sx in (-1, 1):
        x0 = cx + sx * (w / 2 - pw / 2)
        rect(im, x0 - pw / 2, base - h, x0 + pw / 2, base, col)
        rect(im, x0 + (0 if sx < 0 else -pw / 2), base - h, x0 + pw / 2 * (1 if sx < 0 else 0), base, shade)
        rect(im, x0 - pw / 2 - 2, base - h, x0 + pw / 2 + 2, base - h + 2, trim)
        rect(im, x0 - pw / 2 - 2, base - 3, x0 + pw / 2 + 2, base, trim)
    ImageDraw.Draw(im).pieslice([cx - w / 2, base - h - w * 0.28, cx + w / 2, base - h + w * 0.28], 180, 360, fill=col)
    ImageDraw.Draw(im).pieslice([cx - w * 0.36, base - h - w * 0.2, cx + w * 0.36, base - h + w * 0.2], 180, 360, fill=INK)
    ImageDraw.Draw(im).arc([cx - w / 2, base - h - w * 0.28, cx + w / 2, base - h + w * 0.28], 180, 360, fill=trim)
    poly(im, [(cx - 5, base - h - w * 0.28 + 4), (cx + 5, base - h - w * 0.28 + 4), (cx, base - h - w * 0.28 - 4)], trim)

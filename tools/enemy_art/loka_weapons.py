"""The 21 loka patterns' art: a 16x16 bullet (atala/bullets/<kebab-name>.png, white core with a coloured halo -
the game draws it as a negative) and a 16x16 card (weapon_display_images/<snake_name>.png, the pattern's own colour).
Run from the repo root:  python tools/enemy_art/loka_weapons.py
"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets')
S = 16


def new():
    im = Image.new('L', (S, S), 0)
    return im, ImageDraw.Draw(im)


def mirror():
    im, d = new()
    d.polygon([(0, 8), (3, 4), (6, 8), (3, 12)], fill=255)
    d.polygon([(9, 8), (12, 4), (15, 8), (12, 12)], fill=255)
    d.line([(6, 8), (9, 8)], fill=255)
    return im


def scythe():
    im, d = new()
    d.ellipse((2, 1, 14, 14), fill=255)
    d.ellipse((6, 1, 18, 14), fill=0)
    return im


def weave():
    im, d = new()
    pts = [(x, 8 + 4 * math.sin(x * 0.85)) for x in range(0, 16)]
    d.line(pts, fill=255, width=2)
    return im


def ember():
    im, d = new()
    d.ellipse((6, 4, 14, 12), fill=255)
    d.polygon([(0, 8), (8, 4), (8, 12)], fill=255)
    return im


def ash():
    im, d = new()
    for x, y in [(2, 5), (6, 2), (9, 7), (4, 10), (12, 4), (11, 11), (1, 9), (7, 13)]:
        d.rectangle((x, y, x + 1, y + 1), fill=255)
    return im


def cinder():
    im, d = new()
    d.ellipse((4, 4, 11, 11), fill=255)
    for k in range(8):
        a = k * math.pi / 4
        x, y = 7.5 + 6.3 * math.cos(a), 7.5 + 6.3 * math.sin(a)
        d.point((round(x), round(y)), fill=255)
    return im


def tide():
    im, d = new()
    d.polygon([(15, 8), (8, 3), (2, 8), (8, 13)], fill=255)
    d.line([(3, 8), (6, 6), (9, 10), (12, 8)], fill=0)
    return im


def ripple():
    im, d = new()
    d.ellipse((1, 1, 14, 14), outline=255, width=2)
    return im


def boon():
    im, d = new()
    d.rectangle((6, 1, 9, 14), fill=255)
    d.rectangle((1, 6, 14, 9), fill=255)
    return im


def ricochet():
    im, d = new()
    d.line([(1, 12), (5, 4), (9, 12), (14, 5)], fill=255, width=2)
    d.polygon([(11, 2), (15, 4), (12, 8)], fill=255)
    return im


def arc():
    im, d = new()
    d.line([(1, 13), (6, 9), (5, 7), (11, 5), (14, 1)], fill=255, width=2)
    return im


def mine():
    im, d = new()
    d.polygon([(7.5, 0), (15, 11), (0, 11)], outline=255)
    d.polygon([(7.5, 15), (15, 4), (0, 4)], outline=255)
    d.ellipse((6, 6, 9, 9), fill=255)
    return im


def coil():
    im, d = new()
    pts = [(x, 8 + 3 * math.sin(x * 0.7)) for x in range(0, 13)]
    d.line(pts, fill=255, width=2)
    d.ellipse((11, 5, 15, 10), fill=255)
    return im


def fang():
    im, d = new()
    d.polygon([(0, 6), (15, 8), (0, 10)], fill=255)
    return im


def cloud():
    im, d = new()
    d.ellipse((1, 6, 8, 13), fill=255)
    d.ellipse((5, 2, 13, 10), fill=255)
    d.ellipse((8, 7, 15, 14), fill=255)
    return im


def well():
    im, d = new()
    d.ellipse((0, 0, 15, 15), outline=255)
    d.ellipse((3, 3, 12, 12), outline=255)
    d.ellipse((6, 6, 9, 9), fill=255)
    return im


def quake():
    im, d = new()
    d.polygon([(4, 1), (11, 1), (14, 4), (14, 11), (11, 14), (4, 14), (1, 11), (1, 4)], fill=255)
    d.line([(7, 1), (8, 6), (6, 9), (8, 14)], fill=0)
    return im


def shard():
    im, d = new()
    d.polygon([(0, 1), (8, 4), (2, 9)], fill=255)
    d.polygon([(9, 1), (15, 8), (8, 10)], fill=255)
    d.polygon([(3, 11), (10, 11), (7, 15)], fill=255)
    return im


def prism():
    im, d = new()
    d.polygon([(0, 8), (8, 0), (15, 8), (8, 15)], outline=255)
    d.line([(0, 8), (15, 8)], fill=255)
    d.line([(8, 0), (8, 15)], fill=255)
    return im


def execute():
    im, d = new()
    d.line([(2, 2), (13, 13)], fill=255, width=2)
    d.line([(13, 2), (2, 13)], fill=255, width=2)
    d.ellipse((6, 6, 9, 9), fill=255)
    return im


def lastlight():
    im, d = new()
    for k in range(8):
        a = k * math.pi / 4
        d.line([(7.5, 7.5), (7.5 + 7.5 * math.cos(a), 7.5 + 7.5 * math.sin(a))], fill=255)
    d.ellipse((4, 4, 11, 11), fill=255)
    return im


# name, shape, colour
ROWS = [
    ('Mirror Shot', mirror, (0.85, 0.9, 1.0)), ('Wind Scythe', scythe, (0.6, 1.0, 0.9)), ('Maya Weave', weave, (0.9, 0.6, 1.0)),
    ('Ember Dart', ember, (1.0, 0.55, 0.2)), ('Ash Spray', ash, (0.75, 0.7, 0.65)), ('Cinder Bomb', cinder, (1.0, 0.4, 0.15)),
    ('Tide Bolt', tide, (0.3, 0.7, 1.0)), ('Ripple Ring', ripple, (0.5, 0.85, 1.0)), ('Boon Shot', boon, (1.0, 0.85, 0.4)),
    ('Ricochet', ricochet, (0.6, 1.0, 0.5)), ('Arc Chain', arc, (0.7, 0.8, 1.0)), ('Sigil Mine', mine, (0.9, 0.5, 1.0)),
    ('Serpent Coil', coil, (0.4, 1.0, 0.5)), ('Venom Fang', fang, (0.5, 1.0, 0.2)), ('Naga Cloud', cloud, (0.4, 0.9, 0.4)),
    ('Gravity Well', well, (0.6, 0.4, 1.0)), ('Quake Shell', quake, (0.8, 0.6, 0.35)), ('Shatter Shard', shard, (0.8, 0.9, 1.0)),
    ('Jewel Prism', prism, (1.0, 0.6, 0.9)), ('Executioner', execute, (1.0, 0.25, 0.3)), ('Last Light', lastlight, (1.0, 1.0, 0.85)),
]


def dilate(m):
    out = Image.new('L', (S, S), 0)
    px, po = m.load(), out.load()
    for y in range(S):
        for x in range(S):
            if any(0 <= x + dx < S and 0 <= y + dy < S and px[x + dx, y + dy] for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                po[x, y] = 255
    return out


def erode(m):
    out = Image.new('L', (S, S), 0)
    px, po = m.load(), out.load()
    for y in range(S):
        for x in range(S):
            if all(0 <= x + dx < S and 0 <= y + dy < S and px[x + dx, y + dy] for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                po[x, y] = 255
    return out


def main():
    for name, fn, (r, g, b) in ROWS:
        core = fn().point(lambda v: 255 if v > 100 else 0)
        halo = dilate(core)
        inner = erode(core)
        col = (int(r * 255), int(g * 255), int(b * 255))
        bullet = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        card = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        pc, ph, pi = core.load(), halo.load(), inner.load()
        pb, pk = bullet.load(), card.load()
        for y in range(S):
            for x in range(S):
                if pc[x, y]:
                    pb[x, y] = (255, 250, 235, 245)
                    pk[x, y] = (255, 255, 255, 255) if pi[x, y] else col + (255,)
                elif ph[x, y]:
                    pb[x, y] = col + (150,)
                    pk[x, y] = (int(col[0] * 0.35), int(col[1] * 0.35), int(col[2] * 0.35), 255)
        bullet.save(os.path.join(ROOT, 'atala', 'bullets', name.lower().replace(' ', '-') + '.png'))
        card.save(os.path.join(ROOT, 'weapon_display_images', name.lower().replace(' ', '_') + '.png'))
    print('wrote', len(ROWS))


if __name__ == '__main__':
    main()

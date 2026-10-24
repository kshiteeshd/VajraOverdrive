"""BHAIRAVA, Herald of Cinders: the Vitala boss sheet (docs/VITALA_ENEMIES.md).  REDESIGNED: not a gold-masked winged ship but the lord of the
cremation ground himself, built as a capital ship out of BONE and ASH - the one pale thing in a world of black and gold.

Seen from above, nose DOWN, on a 128 px cell (the kit is size-independent: kit.S is switched for the build):
  the SKULL is the prow - a horned cranium with a crescent moon on the brow, two blazing sockets, a THIRD EYE opening in the forehead, a jaw whose
  two gold fangs are cannons; behind it a RIBCAGE is the hull, a furnace burning in it, a gold spine with vertebrae down its middle, a GARLAND of small
  skulls across the chest; two long bone ARMS reach out and down, the left holding a TRISHULA, the right a flaming skull-lantern on a pole; and
  streaming up behind the whole ship, his matted hair (jata), five locks of fire that are the engines.
1024 x 512 = 8 x 4 cells:
    row 0  phase 1, STOKING   idle x8      the third eye shut, the hair low, the furnace dull
    row 1  phase 2, KINDLING  idle x8      the eye half open, the hair high, the furnace red
    row 2  phase 3, THE FLOOD idle x8      the eye wide and white, the hair blazing gold, the sockets white-hot
    row 3  hit x2, death x6
Nothing is random: the same run writes the same bytes."""
import math

from PIL import Image

import kit
import vitala  # noqa: F401  (re-colours the kit and installs the cinder death)
from kit import Canvas

BS = 128                 # the boss's cell
BX = 63.5                # his mirror axis
TAU = math.pi * 2
GOLD = "HGgB"
BONE = "hLlN"            # lit, body, shade, deep (the letters are re-coloured to bone for this sheet: see build_boss_sheet)
BONE_PAL = {"h": (246, 238, 215), "L": (222, 210, 182), "l": (172, 154, 128), "N": (112, 94, 80), "n": (200, 188, 160)}
BONE_RAGE = {"h": (255, 250, 230), "L": (255, 236, 200), "l": (230, 170, 120), "N": (160, 90, 60), "n": (255, 220, 180)}
LETTERS = (("e", "E", "g", "G"), ("E", "g", "G", "H"), ("g", "G", "H", "H"))
FURNACE = (("A", "e", "E", "g", "G"), ("e", "E", "g", "G", "H"), ("E", "g", "G", "H", "H"))


def sn(t, k=1, o=0.0):
    return math.sin((t * k / 8.0 + o) * TAU)


def plume(c, cx, by, h, w, lean, letters):
    """A tall tongue of flame, base centre (cx, by): four nested layers, outermost to the core."""
    for k, letter in enumerate(letters):
        f = 1 - 0.24 * k
        pts = [(cx - w * f, by), (cx - w * 0.82 * f, by - h * f * 0.34), (cx - w * 0.32 * f + lean * 0.3, by - h * f * 0.7),
               (cx + lean * f, by - h * f), (cx + w * 0.4 * f + lean * 0.3, by - h * f * 0.66), (cx + w * 0.86 * f, by - h * f * 0.3),
               (cx + w * f, by)]
        c.fpoly(pts, letter)


def jet_down(c, x, y, h, wide=1):
    """A gun-prong's flame streaming down from its tip."""
    for i in range(h):
        k = i / max(1, h - 1)
        letter = "H" if i == 0 else "G" if k < 0.4 else "g" if k < 0.75 else "E"
        for dx in range(-wide, wide + 1):
            if abs(dx) <= wide * (1 - 0.6 * k):
                c.px(x + dx, y + i, letter)


def skull(c, x, y, r):
    """A small skull, the garland's bead."""
    c.fellipse(x, y, r, r * 0.9, "h")
    c.fellipse(x, y + r * 0.7, r * 0.6, r * 0.45, "L")
    c.px(x - r * 0.4, y - 0.3, "K"); c.px(x + r * 0.4, y - 0.3, "K")
    c.px(x - r * 0.4, y - 1.3, "K"); c.px(x + r * 0.4, y - 1.3, "K")


class BhairavaArt:
    name = "bala"

    def bob(self, t):
        return (0, 0, -2, -2, -2, -2, 0, 0)[t]

    def draw(self, c, t, ph):
        heights = (46, 58, 72)[ph]
        # ---- his hair, five locks of fire streaming up behind the whole ship: these are the engines
        for dx, hf, w, off in ((-34, 0.78, 10, 0.0), (-17, 1.0, 12, 0.2), (0, 1.22, 14, 0.45), (17, 1.0, 12, 0.7), (34, 0.78, 10, 0.9)):
            plume(c, BX + dx, 84 - abs(dx) * 0.3, heights * hf + 2.5 * sn(t, 2, off), w, 4.0 * sn(t, 1, off), LETTERS[ph])
        # ---- the arms: long bone limbs reaching out and down, a hand at the end of each
        c.mpoly([(44, 50), (34, 52), (18, 66), (10, 86), (17, 88), (27, 74), (44, 64)], BONE)
        for sx in (-1, 1):
            hx = BX + sx * 52.5
            c.ellipse(hx, 88, 5.5, 5.5, BONE)                                                       # the knuckles
        # the left hand holds a TRISHULA: a long gold shaft, three prongs, the middle one longest
        c.fpoly([(10, 88), (13, 88), (13, 116), (10, 116)], "g")
        c.fpoly([(10, 88), (11, 88), (11, 116), (10, 116)], "G")
        c.poly([(9, 108), (4, 104), (2, 114), (3, 124), (8, 114)], GOLD)
        c.poly([(14, 108), (19, 104), (21, 114), (20, 124), (15, 114)], GOLD)
        c.poly([(9, 108), (14, 108), (13.5, 124), (11.5, 128), (9.5, 124)], GOLD)
        # the right hand holds a flaming skull on a pole (a lantern): the shaft, the skull, the fire above it
        c.fpoly([(114, 88), (117, 88), (117, 112), (114, 112)], "l")
        c.fpoly([(114, 88), (115, 88), (115, 112), (114, 112)], "L")
        c.fellipse(115.5, 118, 7, 6.4, "h")
        c.fellipse(115.5, 122, 4.4, 3.2, "L")
        c.fellipse(112.5, 117, 2, 2.3, "K"); c.fellipse(118.5, 117, 2, 2.3, "K")
        c.fellipse(112.5, 117.4, 1.1, 1.3, "E"); c.fellipse(118.5, 117.4, 1.1, 1.3, "E")
        plume(c, 115.5, 111, 12 + 3 * sn(t, 2, 0.3), 4.5, 1.5 * sn(t, 1, 0.1), LETTERS[ph])
        # ---- the hull is a ribcage with a furnace burning inside it
        c.fellipse(BX, 64, 27, 31, "K")
        w = FURNACE[ph]
        for rx, ry, letter in ((25, 28, w[0]), (21, 24, w[1]), (16, 19, w[2]), (10, 12, w[3]), (5, 6, w[4])):
            c.fellipse(BX, 66, rx + 0.7 * sn(t, 1, rx / 30.0), ry + 0.7 * sn(t, 1, ry / 30.0), letter)
        for i in range(6):                                                                          # six pairs of ribs, bowed out and down
            y0 = 36 + i * 8
            for sx in (-1, 1):
                for s100 in range(0, 101, 4):
                    s = s100 / 100.0
                    x = BX + sx * (1.5 + s * (31 - i * 1.6))
                    y = y0 + 15 * s * s - 5 * s
                    c.fellipse(x, y, 2.1, 2.1, "L")
                    c.px(x, y - 2, "h")
                    c.px(x, y + 2, "l")
        # the spine: a gold thread down the middle, a vertebra every nine pixels
        c.fpoly([(62.5, 20), (65, 20), (65, 100), (62.5, 100)], "g")
        for k in range(9):
            c.fellipse(BX, 24 + k * 9.4, 4.2, 3.2, "L")
            c.px(BX - 1.5, 23 + k * 9.4, "h")
        # the garland: a chain of skulls across the chest
        for k in range(7):
            skull(c, BX + (k - 3) * 10.5, 74 + 3.2 * abs(k - 3), 3.8)
        # ---- the prow: a horned skull
        horn = [(47, 94), (36, 92), (24, 80), (18, 62), (28, 70), (36, 80), (48, 88)]
        c.mpoly(horn, BONE)
        for k, y in enumerate((84, 76, 68)):                                                         # gold bands round each horn, a hot tip
            c.line(26 + k * 2, y, 38 - k, y + 4, "G"); c.line(101 - k * 2 + 0, y, 89 + k, y + 4, "G")
        c.px(18, 62, "H"); c.px(109, 62, "H")
        c.ellipse(BX, 99, 24, 19, BONE)                                                              # the cranium
        c.ellipse(BX, 115, 16, 9.5, BONE)                                                            # the jaw
        c.fellipse(BX, 108, 20, 4, "L")                                                              # the cheekbones join the two
        for x in range(54, 74, 3):                                                                   # teeth: dark gaps between them
            c.line(x, 111, x, 120, "N")
        c.line(50, 111, 77, 111, "K")
        sock = FURNACE[ph]
        for sx in (-1, 1):                                                                           # two sockets, a fire in each
            ex = BX + sx * 12.5
            c.fellipse(ex, 99, 8.4, 9.2, "K")
            f = 1.0 + 0.12 * sn(t, 1, 0.2 * sx)
            c.fellipse(ex, 100, 6.4 * f, 7.0 * f, sock[1] if ph < 2 else "G")
            c.fellipse(ex, 100.5, 4.0 * f, 4.6 * f, sock[3] if ph < 2 else "H")
            c.px(ex - 2, 96, "H")
        c.fpoly([(BX - 3, 106), (BX + 3, 106), (BX + 0.5, 111)], "K")                                 # the nose
        # the crescent moon on the brow, and the third eye beneath it
        moon = c._ellipse(BX, 79, 13, 9) & ~c._ellipse(BX, 83, 12, 9)
        c.flat(moon, "G")
        c.px(BX - 12, 81, "H"); c.px(BX + 11, 81, "H")
        ry = (2.0, 5.2, 9.5)[ph]
        if ph == 2:
            c.fellipse(BX, 88, 5.4, ry + 2.2, "g")
        c.fellipse(BX, 88, 3.2, ry, "K")
        c.fellipse(BX, 88, 1.8, max(1.0, ry - 1.5), ("e", "E", "H")[ph])
        # the two fangs of the jaw are cannons
        c.mpoly([(BX - 15, 118), (BX - 8, 118), (BX - 9, 127), (BX - 12.5, 127)], GOLD)
        return dict(top=4, left=2, right=125, mid=64)

    def fx(self, c, t, ph):
        for k in range(16):                                                                          # ash falling over the whole ship, a fixed loop
            y = (k * 13.7 + t * 12.5) % 100 + 6
            x = 14 + k * 6.6 + 3 * math.sin(k * 1.7 + t * 0.8)
            c.px(x, y, "W" if k % 3 else "S")
        for k in range(10):                                                                          # embers climbing past it
            y = 100 - ((k * 17.3 + t * 12.5) % 90)
            x = 20 + k * 9.8 + 2.5 * math.sin(k * 2.3 + t * 0.9)
            c.px(x, y, "E" if k % 2 else "G")
        jet_down(c, round(BX - 10.7), 128 - 1 - (3 + (0, 2, 4)[ph]), 3 + (0, 2, 4)[ph], 1) if False else None


def build_boss_sheet():
    """The 1024 x 512 boss sheet."""
    art = BhairavaArt()
    saved = {k: kit.PAL[k] for k in BONE_PAL}
    saved_rage = {k: kit.RAGE[k] for k in BONE_RAGE}
    kit.PAL.update(BONE_PAL)
    kit.RAGE.update(BONE_RAGE)
    kit.S = BS
    try:
        sheet = Image.new("RGBA", (8 * BS, 4 * BS), (0, 0, 0, 0))

        def frame(t, ph):
            c = Canvas()
            art.draw(c, t, ph)
            c.outline()
            art.fx(c, t, ph)
            return kit.shift(c.rgba(), 0, art.bob(t))

        def put(arr, col, row):
            sheet.alpha_composite(Image.fromarray(arr, "RGBA"), (col * BS, row * BS))

        for ph in range(3):
            for t in range(8):
                put(frame(t, ph), t, ph)
        base = frame(0, 0)
        put(kit.shift(kit.white(base, 1.0), 0, -3), 0, 3)
        put(kit.scorch(kit.shift(kit.white(base, 0.35), 0, -2)), 1, 3)
        for i, f in enumerate(kit.death(frame(0, 2))):
            put(f, 2 + i, 3)
        return sheet
    finally:
        kit.S = 64
        kit.PAL.update(saved)
        kit.RAGE.update(saved_rage)

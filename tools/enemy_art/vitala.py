"""The six Vitala enemies, drawn by hand (docs/VITALA_ENEMIES.md).  They are SHIPS - nose down, engines streaming up, like the pilot's
own hulls - but never only ships: each carries a mythic shape (a skull canopy, a trident prow, a serpent's chain, a ram's hammerhead, a
mortar ring).  Atala's were round pearl creatures; Vitala's are forged black basalt and gold, lit from inside.

Same pixel kit as Atala (kit.py), a second palette, a flame crown for rank and a CINDER death.  Importing this module re-colours the kit;
nothing is random - the same run always writes the same bytes.

Coordinates are on the 64x64 cell, y down, the nose pointing DOWN; the mirror axis is between x = 31 and x = 32."""
import math

import numpy as np

import kit
from kit import Canvas, EnemyArt

# ----------------------------------------------------------------- the second palette ------
kit.PAL.update({
    "K": (40, 16, 10), "H": (255, 246, 205), "W": (172, 142, 130), "P": (66, 56, 62), "S": (44, 35, 44),
    "D": (28, 21, 31), "C": (140, 70, 46), "c": (92, 50, 40), "G": (255, 200, 40), "g": (240, 150, 30),
    "B": (180, 106, 20), "A": (110, 50, 10), "L": (205, 228, 255), "l": (120, 160, 235), "E": (255, 110, 30),
    "e": (190, 60, 22), "h": (232, 246, 255), "n": (150, 205, 255), "N": (64, 116, 206),
})
kit.RAGE.clear()
kit.RAGE.update({
    "H": (255, 255, 235), "W": (255, 150, 80), "P": (140, 52, 36), "S": (100, 36, 28), "D": (64, 22, 22),
    "C": (255, 170, 90), "c": (220, 110, 60), "G": (255, 250, 215), "g": (255, 225, 130), "B": (255, 150, 60),
    "A": (200, 80, 30), "L": (255, 230, 200), "l": (255, 170, 120), "K": (40, 10, 8), "E": (255, 255, 225),
    "e": (255, 170, 80), "h": (255, 244, 220), "n": (255, 200, 150), "N": (230, 120, 80),
})
kit.CREST[:] = [  # the three sacrificial fires: a flame crown, left half, flush to the centre line
    "......E",
    ".....EgG",
    "..E.EgGH",
    "..gEgGGH",
    "...BgGGG",
    "....BBgg",
]

GOLD = "HGgB"      # lit, body, shade, deep for molten gold
BASALT = "WPSD"
GHOST = "hLnN"
TAU = math.pi * 2
BAYER = kit.BAYER


def sn(t, k=1, o=0.0):
    return math.sin((t * k / 8.0 + o) * TAU)


def cs(t, k=1, o=0.0):
    return math.cos((t * k / 8.0 + o) * TAU)


def _h(x, y, k=0):
    n = (int(x) * 73856093) ^ (int(y) * 19349663) ^ (k * 83492791)
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def mote(c, x, y, core="H", arm="G"):
    x, y = round(x), round(y)
    c.px(x, y, core)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        c.px(x + dx, y + dy, arm)


def eye_socket(c, cx, cy, rx, ry, ring="B"):
    c.fellipse(cx, cy, rx + 1, ry + 1, ring)
    c.fellipse(cx, cy, rx, ry, "K")


def ember_eye(c, cx, cy, rx, ry, look=0.0, slit=True):
    """A forge-eye: bronze socket, black well, ember iris, gold core, a black slit and a catch-light."""
    eye_socket(c, cx, cy, rx, ry)
    c.fellipse(cx + look * 0.5, cy, rx * 0.72, ry * 0.8, "e")
    c.fellipse(cx + look, cy, rx * 0.5, ry * 0.62, "E")
    c.fellipse(cx + look, cy, rx * 0.3, ry * 0.4, "G")
    if slit:
        c.px(cx + look, cy - 1, "K"); c.px(cx + look, cy, "K"); c.px(cx + look, cy + 1, "K")
    c.px(cx + look - rx * 0.4, cy - ry * 0.5, "H")


def flame(c, x, y, h, lean=0, glow=False):
    """A little upright flame: base at (x, y), h pixels tall, the tip leaning `lean`."""
    for i in range(h):
        k = i / max(1, h - 1)
        px = x + round(lean * k * k)
        c.px(px, y - i, "H" if i == 0 else "G" if k < 0.45 else "g" if k < 0.8 else "E")
        if i < h - 2:
            c.px(px - 1, y - i, "E" if glow else "g"); c.px(px + 1, y - i, "E" if glow else "g")


# ----------------------------------------------------------------- shared ship parts --------
def mline(c, x0, y0, x1, y1, letter):
    """A line and its mirror image."""
    c.line(x0, y0, x1, y1, letter)
    c.line(63 - x0, y0, 63 - x1, y1, letter)


def exhaust(c, x, y, h, lean=0, ghost=False, wide=1):
    """An engine's exhaust: base at the nozzle (x, y), streaming UP the cell (these ships fly nose-down), h pixels long."""
    for i in range(h):
        k = i / max(1, h - 1)
        px = x + round(lean * k * k)
        if ghost:
            c.px(px, y - i, "h" if i == 0 else "n" if k < 0.5 else "l")
            if i < h - 2:
                c.px(px - 1, y - i, "l"); c.px(px + 1, y - i, "l")
        else:
            c.px(px, y - i, "H" if i == 0 else "G" if k < 0.45 else "g" if k < 0.8 else "E")
            if i < h - 2:
                for d in range(1, wide + 1):
                    c.px(px - d, y - i, "g" if k < 0.5 else "E"); c.px(px + d, y - i, "g" if k < 0.5 else "E")


def nozzle(c, x0, x1, y):
    c.fpoly([(x0, y), (x1, y), (x1, y + 3), (x0, y + 3)], "B")
    c.line(x0, y, x1, y, "G")


TILT = 2.2      # px a wing tip rises or falls in the bank


def roll_of(t):
    """The ship's slow roll over the idle loop: -1 (left wing high) .. +1 (right wing high), one full cycle in eight frames."""
    return math.sin(t * TAU / 8.0)


def wings(c, pts, roll, ramp=BASALT):
    """A pair of wings.  `pts` is the LEFT wing's outline as (x, y, w); w in 0..1 is how far out along the wing the point lies, and the roll
    lifts one wing tip and drops the other: the hull banking.  Returns the two outlines as drawn."""
    left = [(x, y + roll * w * TILT) for x, y, w in pts]
    right = [(63 - x, y - roll * w * TILT) for x, y, w in pts]
    c.poly(left, ramp)
    c.poly(right, ramp)
    return left, right


def edge(c, outlines, i, j, letter):
    """A trim line along the i-th to j-th points of both wings."""
    for o in outlines:
        c.line(o[i][0], o[i][1], o[j][0], o[j][1], letter)


# ----------------------------------------------------------------- 1. the skull (scout; AURIS) ---
class Skull(EnemyArt):
    """The scout: a ghost-white SKULL on a ragged tail of mist, blue fire in the sockets, a wide many-toothed grin, a ring of hataka gold on
    the brow.  (The pilot kept this one.)"""
    name = "auris"
    cracks = [(26, 26, 29, 31), (36, 28, 39, 33)]
    flames = [(18, 14)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        for k, (x0, ln) in enumerate(((25, 15), (31.5, 20), (38, 15))):                                  # the tail: three wisps
            for j in range(ln):
                x = x0 + round(2.6 * sn(t, 1, -j * 0.045 + k * 0.33) * (j / ln))
                w = 2 if j < ln * 0.55 else 1
                col = "L" if j < ln * 0.35 else "n" if j < ln * 0.7 else "l"
                for dx in range(-w + 1, w):
                    c.px(x + dx, 41 + j, col)
        c.ellipse(31.5, 29, 13.5, 12, GHOST)                                                              # the cranium
        c.ellipse(31.5, 40, 9.5, 6.5, GHOST)                                                              # the jaw
        c.fellipse(31.5, 35, 12, 4, "L")
        c.fellipse(31.5, 33, 11, 2.2, "n")
        for sx in (-1, 1):                                                                                # hollow sockets with blue fire
            ex = 31.5 + sx * 6.6
            c.fellipse(ex, 30, 4.6, 5.2, "K")
            c.fellipse(ex, 30.5, 3.3, 3.9, "N")
            fl = (3, 4, 3, 5, 4, 3, 4, 5)[t] if sx < 0 else (4, 3, 5, 3, 4, 5, 3, 4)[t]
            c.fellipse(ex, 31.5 - (fl - 3) * 0.5, 1.7, fl * 0.55, "n")
            c.px(ex, 30 - (fl - 3) * 0.5, "H")
        c.stamp_c(["....K", "...KK"], 34)                                                                # the nose
        drop = 1 if t in (3, 4, 5) else 0                                                                 # the grin, the jaw dropping at the low point
        for x in range(23, 32):
            y = 39 + (1 if x > 29 else 0)
            for yy in range(y, y + 3 + drop):
                c.px(x, yy, "K"); c.px(63 - x, yy, "K")
            if x % 2 == 1:
                c.px(x, y, "h"); c.px(63 - x, y, "h")
                c.px(x, y + 2 + drop, "h"); c.px(63 - x, y + 2 + drop, "h")
        for x in range(21, 32):                                                                           # the hataka ring on the brow, a gem between
            y = 20 + int((31 - x) * 0.18)
            c.px(x, y, "G"); c.px(63 - x, y, "G")
            c.px(x, y + 1, "B"); c.px(63 - x, y + 1, "B")
        c.px(31, 18, "G"); c.px(32, 18, "G"); c.px(31, 19, "H"); c.px(32, 19, "H")
        return dict(top=18, left=13, right=50, mid=33)

    def fx(self, c, t, rage):
        for k, sx in ((0, 12), (1, 51)):                                                                  # smoke rises past the skull
            u = ((t + 3 * k) % 8) / 8.0
            c.px(sx + round(1.5 * sn(t, 1, k * 0.4)), 34 - u * 16, "L" if u < 0.5 else "l")
        if t in (1, 5):
            mote(c, 31.5, 17.5, "H", "G")


# ----------------------------------------------------------------- 2. the wraith (turncoat; HATAKA) -
class Wraith(EnemyArt):
    """The turncoat: a hooded wraith with its arms held WIDE in welcome, long sleeves trailing, a hem of torn mist, a dark face with a wide
    white grin and blue fire eyes, hataka-gold bangles on the wrists.  It embraces - and bites.  (The pilot kept this one too.)"""
    name = "hataka"
    cracks = [(28, 26, 30, 31), (33, 26, 35, 31)]
    flames = [(24, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def _arm(self, c, side, dy):
        x = lambda v: v if side < 0 else 63 - v
        c.poly([(x(25), 25), (x(9), 28 + dy), (x(8), 33 + dy), (x(25), 32)], GHOST)
        c.poly([(x(10), 28 + dy), (x(2), 33 + dy), (x(3), 47 + dy), (x(8), 42 + dy), (x(7), 36 + dy), (x(12), 34 + dy)], GHOST)
        for k in range(2):
            c.px(x(3 + k * 3), 47 + dy + k, "n")
        for fy in (-3, 0, 3):
            c.line(x(4), 29 + dy + fy // 2, x(0), 28 + dy + fy, "h")
        c.fpoly([(x(14), 27 + dy), (x(17), 27 + dy), (x(17), 33 + dy), (x(14), 33 + dy)], "G")
        c.px(x(15), 28 + dy, "H")

    def draw(self, c, t, rage):
        for k, x0 in enumerate((18, 23, 27, 31.5, 36, 40, 45)):                                           # the hem: torn mist, each wisp its own pace
            ln = 8 + (k % 3) * 3
            for j in range(ln):
                x = x0 + round(2.4 * sn(t, 1, -j * 0.05 + k * 0.17) * (j / ln))
                c.px(x, 52 + j, "L" if j < ln * 0.35 else "n" if j < ln * 0.7 else "l")
        c.mpoly([(31, 21), (25, 24), (21, 40), (15, 52), (19, 55), (23, 52), (27, 56)], GHOST)
        c.fmpoly([(31, 30), (28, 30), (27, 52), (31, 52)], "n")
        self._arm(c, -1, round(2 * sn(t)))
        self._arm(c, 1, round(2 * sn(t, 1, 0.5)))
        c.ellipse(31.5, 17, 10, 11, GHOST)
        c.fellipse(31.5, 19, 6.8, 7.6, "K")
        fl = (3, 4, 3, 5, 4, 3, 4, 5)[t]
        for sx in (-1, 1):
            ex = 31.5 + sx * 3.2
            ff = fl if sx < 0 else 8 - fl
            c.fellipse(ex, 17.5, 1.9, 1.5 + ff * 0.25, "n")
            c.px(ex, 17 - (ff - 3) * 0.4, "H")
        drop = 1 if t in (3, 4, 5) else 0
        for x in range(26, 32):
            y = 23 + (0 if x > 27 else -1)
            for yy in range(y, y + 2 + drop):
                c.px(x, yy, "N"); c.px(63 - x, yy, "N")
            if x % 2 == 0:
                c.px(x, y, "h"); c.px(63 - x, y, "h")
                c.px(x, y + 1 + drop, "h"); c.px(63 - x, y + 1 + drop, "h")
        for x in range(22, 32):
            y = 8 + int((31 - x) * 0.22)
            c.px(x, y, "G"); c.px(63 - x, y, "G")
        c.px(31, 6, "G"); c.px(32, 6, "G"); c.px(31, 7, "H"); c.px(32, 7, "H")
        return dict(top=6, left=2, right=61, mid=30)

    def fx(self, c, t, rage):
        for k, sx in ((0, 17), (1, 46)):
            u = ((t + 3 * k) % 8) / 8.0
            c.px(sx + round(1.5 * sn(t, 1, k * 0.4)), 24 - u * 14, "L" if u < 0.5 else "l")


# ----------------------------------------------------------------- 3. the needle (sentry; LANCE) ---
class Shula(EnemyArt):
    """The sentry: a NEEDLE.  A long, thin fuselage with a gold nose-spike, short swept stub wings far aft, little canards at the nose, a
    tall ember canopy and one big engine.  The simplest hull in the loka: it holds a line and fires straight down it."""
    name = "lance"
    cracks = [(29, 18, 29, 28), (28, 36, 28, 46)]
    flames = [(28, 3)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)
    WING = [(28, 29, 0.0), (19, 25, 0.5), (9, 17, 0.95), (6, 11, 1.0), (13, 13, 0.8), (21, 16, 0.4), (28, 20, 0.0)]

    def draw(self, c, t, rage):
        r = roll_of(t)
        w = wings(c, self.WING, r)
        edge(c, w, 0, 3, "G")                                                                              # gold leading edges
        for o in w:                                                                                        # a gun pod on each tip
            c.fellipse(o[3][0] + (3 if o[3][0] < 31 else -3), o[3][1] + 3, 1.6, 3.2, "G")
        c.mpoly([(29, 47), (23, 45), (21, 49), (28, 52)], BASALT)                                         # canards
        mline(c, 28, 46, 22, 46, "G")
        c.mpoly([(31, 63), (29, 53), (28, 31), (29, 13), (31, 9)], BASALT)                                # the fuselage
        c.mpoly([(31, 63), (30, 55), (31, 55)], GOLD)                                                     # the nose-spike
        for y in (22, 40):
            c.fmpoly([(28, y), (31, y), (31, y + 1), (28, y + 1)], "G")
        look = round(1.0 * sn(t))
        c.fellipse(31.5, 34, 3.4, 7, "B")                                                                  # the canopy
        c.fellipse(31.5, 34, 2.5, 6, "K")
        c.fellipse(31.5 + look * 0.3, 35, 1.6, 4.2, "E")
        c.fellipse(31.5 + look * 0.5, 36, 0.9, 2.2, "G")
        c.px(30, 31, "H")
        nozzle(c, 28, 35, 10)
        return dict(top=7, left=6, right=57, mid=32)

    def fx(self, c, t, rage):
        exhaust(c, 31, 9, (7, 10, 8, 11, 8, 7, 10, 11)[t], lean=1 if t % 4 == 1 else 0, wide=2)
        y = 14 + (t * 6) % 44                                                                              # a light runs down the hull
        c.px(31, y, "H"); c.px(32, y, "H")
        r = roll_of(t)
        mote(c, 6 + 0, 11 + r * 2.2, "H", "E") if t % 4 < 2 else mote(c, 57, 11 - r * 2.2, "H", "E")       # wing-tip lights, in turn


# ----------------------------------------------------------------- 4. the boomerang (skirmisher; VAKRA)
class Naga(EnemyArt):
    """The skirmisher: a BOOMERANG.  Two broad swept-back wings meeting in a chevron at a small ember canopy, a row of gold serpent-scales
    along each, twin engines on the wing roots and two gold fangs at the nose.  Wide and fast; it rolls as it weaves."""
    name = "vakra"
    cracks = [(24, 20, 17, 28), (28, 42, 28, 48)]
    flames = [(26, 2)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)
    WING = [(28, 46, 0.0), (18, 39, 0.45), (7, 27, 0.9), (1, 12, 1.0), (8, 14, 0.9), (16, 24, 0.5), (26, 33, 0.0)]

    def draw(self, c, t, rage):
        r = roll_of(t)
        w = wings(c, self.WING, r)
        edge(c, w, 0, 3, "G")
        for o in w:                                                                                        # gold serpent-scales: chevron ticks along the wing
            for k in range(4):
                f = (k + 0.7) / 4.5
                x = o[0][0] + (o[3][0] - o[0][0]) * f
                y = o[0][1] + (o[3][1] - o[0][1]) * f
                dx = 1 if x < 31 else -1
                c.line(x + dx * 6, y + 6, x + dx * 1, y + 1, "g") if False else None
                c.px(x + dx * 3, y + 3, "g"); c.px(x + dx * 4, y + 4, "g"); c.px(x + dx * 5, y + 3, "g")
        c.mpoly([(31, 57), (28, 49), (26, 34), (27, 19), (31, 14)], BASALT)                                # the body
        c.mpoly([(29, 56), (27, 62), (31, 58)], GOLD)                                                      # fangs
        c.fellipse(31.5, 42, 3.8, 5, "B")
        c.fellipse(31.5, 42, 3.0, 4.2, "K")
        c.fellipse(31.5 + 0.5 * sn(t), 43, 2.0, 2.8, "E")
        c.fellipse(31.5 + 0.7 * sn(t), 43.5, 1.0, 1.4, "G")
        c.px(30, 40, "H")
        for sx in (-1, 1):                                                                                 # twin engines on the wing roots
            ex = 31.5 + sx * 7
            nozzle(c, int(ex - 2), int(ex + 2), 17 + round(-r * sx * 0.8))
        return dict(top=8, left=1, right=62, mid=36)

    def fx(self, c, t, rage):
        r = roll_of(t)
        exhaust(c, 24, 16 + round(r * 0.8), (6, 8, 7, 9, 7, 6, 8, 9)[t], lean=1 if t % 3 == 0 else 0)
        exhaust(c, 39, 16 + round(-r * 0.8), (8, 7, 9, 7, 6, 8, 7, 9)[t], lean=-1 if t % 3 == 1 else 0)
        mote(c, 2, 12 + r * 2.2, "H", "E") if t % 4 < 2 else mote(c, 61, 12 - r * 2.2, "H", "E")


# ----------------------------------------------------------------- 5. the ram (rusher; BHAVI) -----
class Kuta(EnemyArt):
    """The rusher: a RAM.  A blunt, wide body with short thick wings, an armoured prow-plate across the nose with a molten strip and gold
    tusks, a visor-slit canopy and two big thruster pods astern.  All of its weight is at the front, and it flies at you."""
    name = "bhavi"
    cracks = [(20, 22, 24, 30), (27, 40, 30, 47)]
    flames = [(22, 3)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)
    WING = [(19, 44, 0.0), (9, 42, 0.4), (3, 34, 0.9), (2, 24, 1.0), (9, 21, 0.8), (18, 25, 0.0)]

    def draw(self, c, t, rage):
        r = roll_of(t)
        w = wings(c, self.WING, r)
        edge(c, w, 0, 3, "G")
        for sx in (-1, 1):                                                                                 # big thruster pods astern
            px_ = 31.5 + sx * 14.5
            c.ellipse(px_, 17, 5.8, 8, BASALT)
            nozzle(c, int(px_ - 3), int(px_ + 3), 9)
        c.mpoly([(31, 56), (21, 56), (17, 44), (17, 26), (21, 13), (31, 11)], BASALT)                      # the blunt body
        c.fmpoly([(18, 30), (31, 30), (31, 31), (18, 31)], "B")
        c.fmpoly([(18, 22), (31, 22), (31, 23), (18, 23)], "B")
        c.solid(c._poly([(12, 50), (51, 50), (55, 57), (48, 63), (15, 63), (8, 57)]), BASALT)             # the prow-plate
        c.fpoly([(10, 52), (53, 52), (54, 55), (9, 55)], "g")                                              # the molten strip across it
        hot = 10 + (t * 6) % 42
        c.px(hot, 53, "H"); c.px(hot + 1, 53, "H")
        c.mpoly([(14, 62), (12, 63), (18, 63)], GOLD)
        c.mpoly([(26, 63), (27, 63), (28, 63)], GOLD)
        c.fellipse(31.5, 40, 8.5, 3.0, "B")                                                                 # the visor-slit canopy
        c.fellipse(31.5, 40, 7.5, 2.2, "K")
        c.fellipse(31.5 + 3 * sn(t), 40, 3.6, 1.3, "E")
        c.fellipse(31.5 + 3 * sn(t), 40, 1.8, 0.7, "G")
        return dict(top=8, left=2, right=61, mid=44)

    def fx(self, c, t, rage):
        exhaust(c, 17, 8, (7, 9, 8, 10, 8, 7, 9, 10)[t], lean=1 if t % 3 == 0 else 0, wide=2)
        exhaust(c, 46, 8, (9, 8, 10, 8, 7, 9, 8, 10)[t], lean=-1 if t % 3 == 2 else 0, wide=2)
        if t in (2, 3):
            mote(c, 10, 62, "H", "E"); mote(c, 53, 62, "H", "E")


# ----------------------------------------------------------------- 6. the mortar (artillery; MAYAVI)
class Dhupa(EnemyArt):
    """The artillery: a MORTAR GUNSHIP.  A stout hull with a great cannon in the nose, its muzzle glowing molten, and wings that sweep
    FORWARD to a pod of molten gold at each tip - the opposite stance to the boomerang.  Twin engines astern; it hangs back and lobs."""
    name = "mayavi"
    cracks = [(26, 20, 28, 28), (24, 40, 28, 46)]
    flames = [(24, 3)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)
    WING = [(24, 26, 0.0), (14, 31, 0.5), (6, 41, 0.95), (4, 51, 1.0), (10, 49, 0.9), (17, 41, 0.5), (24, 38, 0.0)]

    def draw(self, c, t, rage):
        r = roll_of(t)
        w = wings(c, self.WING, r)
        edge(c, w, 0, 3, "G")
        for o in w:                                                                                        # a molten pod at each wing tip
            px_, py_ = (o[3][0] + (3 if o[3][0] < 31 else -3)), o[3][1] + 4
            c.ellipse(px_, py_, 4.2, 5.2, GOLD)
            c.fellipse(px_, py_ + 0.5, 2.8, 3.4, "e")
            c.fellipse(px_, py_ + 0.5, 1.8, 2.3, "E" if t % 4 < 2 else "G")
        c.mpoly([(31, 58), (25, 51), (23, 36), (24, 18), (31, 12)], BASALT)                                # the hull
        c.fmpoly([(24, 24), (31, 24), (31, 25), (24, 25)], "B")
        c.mpoly([(30, 62), (28, 50), (31, 50)], BASALT)                                                    # the cannon
        c.fellipse(31.5, 62, 4.4, 2.6, "B")                                                                # its muzzle: a molten ring
        c.fellipse(31.5, 62, 3.4, 1.8, "K")
        c.fellipse(31.5, 62, 2.6, 1.3, "E" if t % 4 < 2 else "g")
        c.fellipse(31.5, 33, 3.6, 5.2, "G")                                                                # the canopy: gold, ember behind it
        c.fellipse(31.5, 33, 2.8, 4.4, "K")
        c.fellipse(31.5, 34, 1.8, 2.6, "E")
        c.px(30, 31, "H")
        nozzle(c, 25, 29, 12); nozzle(c, 34, 38, 12)
        return dict(top=8, left=3, right=60, mid=36)

    def fx(self, c, t, rage):
        exhaust(c, 27, 11, (5, 7, 6, 8, 6, 5, 7, 8)[t], lean=1 if t % 3 == 0 else 0)
        exhaust(c, 36, 11, (7, 6, 8, 6, 5, 7, 6, 8)[t], lean=-1 if t % 3 == 1 else 0)
        if t in (0, 4):
            mote(c, 31.5, 63, "H", "G")


# ----------------------------------------------------------------- the cinder death --------
def cinder_death(arr):
    """Six fixed frames: struck white-hot with the seams blown open; the stone goes ember-red and starts to rise; cinders climb and
    thin on a fixed dither; four-point glints are what is left."""
    S = kit.S
    K = S // 64                                         # the boss's 128 cell moves twice as far
    m = arr[:, :, 3] > 0
    ys, xs = np.where(m)
    cy = int(round((ys.min() + ys.max()) / 2))
    frames = []
    f0 = kit.white(arr, 0.85)
    for k in range(7):                                  # the seams blow open: gold lines out from the core
        ang = k * TAU / 7 + 0.4
        for i in range(S // 2):
            x, y = int(round(S / 2 - 0.5 + math.cos(ang) * i)), int(round(cy + math.sin(ang) * i))
            if 0 <= x < S and 0 <= y < S and f0[y, x, 3] > 0:
                f0[y, x, :3] = kit.PAL["G"]
    frames.append(f0)
    plan = [  # rise px, spread px, ember mix, dissolve, alpha
        (1, 1, 0.25, 0.00, 255),
        (3, 2, 0.55, 0.15, 240),
        (6, 4, 0.80, 0.45, 210),
        (10, 6, 0.95, 0.75, 150),
    ]
    ember = np.array(kit.PAL["E"], float)
    gold = np.array(kit.PAL["G"], float)
    for f, (rise, spread, mix, dis, alpha) in enumerate(plan):
        out = np.zeros_like(arr)
        for y, x in zip(ys, xs):
            h1, h2 = _h(x, y, 1), _h(x, y, 2)
            ny = y - int(K * rise * (0.25 + h1)) + (1 if f == 0 else 0)
            nx = x + int(round(K * spread * (h2 - 0.5) * 2))
            if not (0 <= nx < S and 0 <= ny < S):
                continue
            if dis and BAYER[ny % 4, nx % 4] < dis * (0.4 + h1):
                continue
            c = arr[y, x, :3].astype(float)
            hot = ember if h2 > 0.35 else gold
            c = c * (1 - mix) + hot * mix
            out[ny, nx, :3] = c.astype(np.uint8)
            out[ny, nx, 3] = alpha
        frames.append(out)
    frames.append(np.zeros_like(arr))                   # frame 5: nothing but glints
    spots = [[(-14, -12), (15, -6), (-6, -18), (8, 4)], [(-12, -16), (13, -10), (0, -22)], [(-9, -20), (10, -16)]]
    for f, which in ((3, 0), (4, 1), (5, 2)):
        for dx, dy in spots[which]:
            glint = kit.GLINT if f == 3 else kit.GLINT_S
            for j, row in enumerate(glint):
                for i, ch in enumerate(row):
                    if ch == ".":
                        continue
                    x, y = S // 2 - 1 + dx * K + i - len(row) // 2, cy + dy * K + j - len(glint) // 2
                    if 0 <= x < S and 0 <= y < S:
                        frames[f][y, x, :3] = kit.PAL[ch]
                        frames[f][y, x, 3] = 255 if f < 5 else 200
    return frames


kit.death = cinder_death


def scorch_embers(arr):
    """Hit frame 2: the stone settles with a cherry-red scald across the struck side."""
    out = arr.copy()
    ys, xs = np.where(arr[:, :, 3] > 0)
    top = ys.min()
    for y, x in zip(ys, xs):
        if y < top + 5 and (x + y) % 3 == 0:
            out[y, x, :3] = kit.PAL["E"]
    return out


kit.scorch = scorch_embers

ENEMIES = {e.name: e for e in (Skull(), Wraith(), Shula(), Naga(), Kuta(), Dhupa())}

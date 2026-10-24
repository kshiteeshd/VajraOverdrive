"""The nine Atala enemies, drawn by hand.  Each class is one design from docs/ENEMY_BIBLE.md:
the silhouette, the shading ramps, the stamped details and the motion are chosen, not rolled.

Coordinates are on the 64x64 cell, y down, mirror axis between x = 31 and x = 32.  Every shape
is built in the order a painter would: back parts first, the hull, then trim, then the face."""
import math
from kit import Canvas, EnemyArt, IDX

GOLD = "HGgB"      # lit, body, shade, deep for gold metal
LAV = "HLll"       # lavender glass
TAU = math.pi * 2


def sn(t, k=1, o=0.0):
    """A smooth loop over the 8 idle frames: sine, k cycles per loop, phase o (in cycles)."""
    return math.sin((t * k / 8.0 + o) * TAU)


def cs(t, k=1, o=0.0):
    return math.cos((t * k / 8.0 + o) * TAU)


def mote(c, x, y, core="H", arm="G"):
    """A loose spark: a hot pixel with four dim arms."""
    x, y = round(x), round(y)
    c.px(x, y, core)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        c.px(x + dx, y + dy, arm)


def eye_socket(c, cx, cy, rx, ry, ring="B"):
    c.fellipse(cx, cy, rx + 1, ry + 1, ring)
    c.fellipse(cx, cy, rx, ry, "K")


class Auris(EnemyArt):
    """The listening orb: pearl sphere, one slit-eye, two ear-fins, a bead-necklace of gems."""
    name = "auris"
    cracks = [(24, 29, 27, 35), (21, 40, 26, 42)]
    flames = [(20, 14)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        fy = -round(1.5 * (1 - cs(t)))   # the fins beat out of phase with the hover
        # ear-fins: a swept fan with a gold spine, behind the orb
        c.mpoly([(20, 37), (15, 35), (9, 31 + fy), (5, 25 + fy), (4, 20 + fy), (9, 22 + fy),
                 (13, 27 + fy), (19, 31)])
        c.fmpoly([(19, 33), (14, 30 + fy), (9, 25 + fy), (7, 22 + fy)], "G")
        c.px(7, 21 + fy, "H"); c.px(56, 21 + fy, "H")
        c.ellipse(31.5, 34, 13.5, 13.5)
        # warm belly crescent, hand-placed
        c.stamp_c(["...............CCCC", "..............CCCCC", "............ccCCCCC",
                   "..........ccccCCCC"], 41)
        # brow: a gold arch with a bead at the crown
        c.stamp_c([".......gGGG", "....gGGgBBB", "..gGgB", ".GgB"], 24)
        c.px(31, 23, "H"); c.px(32, 23, "H")
        # the eye: bronze socket, black slit, gold iris that looks about
        if t == 6:                                                   # a slow blink
            eye_socket(c, 31.5, 34, 7.5, 1.8)
        else:
            eye_socket(c, 31.5, 34, 7.5, 4.6)
            ix = 31.5 + sn(t) * 2.2                                  # the iris tracks left to right and back
            c.fellipse(ix, 34, 3.2, 3.4, "g")
            c.fellipse(ix, 34, 2.3, 2.5, "G")
            c.px(ix - 0.5, 34, "K"); c.px(ix + 0.5, 34, "K")
            c.px(ix - 0.5, 35, "K"); c.px(ix + 0.5, 35, "K")
            c.px(ix - 1.5, 32.5, "H")
        # six gems on a necklace under the eye; the ring turns one step per frame
        for k in range(6):
            a = (k * 60 + t * 7.5) * math.pi / 180
            gx, gy = 31.5 + math.cos(a) * 11.5, 42.5 + math.sin(a) * 2.8
            if math.sin(a) > -0.2:
                c.fellipse(gx, gy, 1.5, 1.5, "G")
                c.px(gx - 0.5, gy - 0.5, "H")
        return dict(top=20, left=14, right=49, mid=34)

    def fx(self, c, t, rage):
        # two motes circle the orb, one in front while the other is behind it
        for k in (0, 1):
            a = (t * 45 + k * 180) * math.pi / 180
            if math.sin(a) > 0 or abs(math.cos(a)) > 0.76:
                mote(c, 31.5 + math.cos(a) * 19, 33 + math.sin(a) * 8)
        # the fin tips take turns to glint as they listen
        fy = -round(1.5 * (1 - cs(t)))
        if t in (3, 4):
            mote(c, 7, 21 + fy)
        if t in (7, 0):
            mote(c, 56, 21 + fy)


class Hataka(EnemyArt):
    """The gold-draught saucer: a flat pearl disc, a domed black eye, a fanged gold smile."""
    name = "hataka"
    cracks = [(24, 36, 29, 40), (36, 33, 40, 38)]
    flames = [(16, 21)]
    SMILE = (3.0, 3.4, 3.8, 3.4)    # the smile widens at the low point of the hover

    def draw(self, c, t, rage):
        c.ellipse(31.5, 37, 21, 8.5)                                 # the disc
        c.fellipse(31.5, 41.5, 18, 2, "c")                           # warm underside
        for k in range(6):                                          # rim lights rotate
            a = (k * 60 + t * 7.5) * math.pi / 180
            if math.sin(a) > -0.15:
                gx, gy = 31.5 + math.cos(a) * 18.5, 37 + math.sin(a) * 6.2
                c.fellipse(gx, gy, 1.4, 1.4, "G")
                c.px(gx - 0.5, gy - 0.5, "H")
        c.ellipse(31.5, 31, 12.5, 9)                                 # the dome
        c.stamp_c(["....................W", "...................WW"], 24)
        eye_socket(c, 31.5, 31.5, 6.6, 4.8)
        look = round(sn(t) * 1.5)                                    # the black pupil scans the room
        c.fellipse(31.5, 31.5, 3.3, 3.0 + 0.4 * max(0, sn(t, 2)), "G")
        c.fellipse(31.5 + look, 31.5, 1.6, 2.2, "K")
        c.px(30 + look, 30, "H")
        # the smile: a black band along a fixed curve, three gold fangs hanging in it
        curve = [(18, 37), (21, 38.5), (25, 40), (29, 41.2), (31, 41.6)]

        def y_at(x):
            for (x0, y0), (x1, y1) in zip(curve, curve[1:]):
                if x0 <= x <= x1:
                    return y0 + (y1 - y0) * (x - x0) / (x1 - x0)
            return curve[-1][1]

        wide = 1 if t in (3, 4, 5) else 0              # the grin deepens at the low point
        for x in range(18, 32):
            yb = round(y_at(x))
            for y in range(yb, yb + 3 + wide):
                c.px(x, y, "K"); c.px(63 - x, y, "K")
        for xt in (22, 26, 31):
            yb = round(y_at(xt))
            for y in (yb + 1, yb + 2 + wide):
                c.px(xt, y, "G"); c.px(63 - xt, y, "G")
            c.px(xt, yb + 1, "H"); c.px(63 - xt, yb + 1, "H")
        return dict(top=22, left=10, right=53, mid=37)

    def fx(self, c, t, rage):
        # a glint rides the dome from left to right
        if t < 7:
            a = (-150 + t * 17) * math.pi / 180
            mote(c, 31.5 + math.cos(a) * 10.5, 31 + math.sin(a) * 7.5, "H", "W")
        # gold draught drips from the underside and falls: two drops, half a loop apart
        for x, off in ((24, 0), (39, 4)):
            u = (t + off) % 8
            y = 46 + u * 1.7
            c.px(x, y, "G"); c.px(x, y + 1, "g" if u < 6 else "B")
            if u < 2:
                c.px(x, y - 1, "H")


class Lance(EnemyArt):
    """The spear sentry: a long pearl blade-head, a gold spine, a slot with a bead that tracks."""
    name = "lance"
    cracks = [(28, 18, 30, 26), (26, 40, 29, 48)]
    flames = [(22, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        # blade-fins: a sentry stands still, but the fin tips flex with every breath
        fl = round(1.2 * sn(t))
        c.mpoly([(21, 28), (14, 26 + fl), (10, 31 + fl), (11, 38 + fl), (16, 43), (22, 38)])
        c.fmpoly([(21, 30), (14, 29 + fl), (12, 33 + fl)], "G")
        c.px(11, 30 + fl, "H"); c.px(52, 30 + fl, "H")
        # the blade: a long kite, point down
        c.mpoly([(28, 8), (23, 15), (21, 27), (24, 41), (28, 52), (31, 60)])
        # gold spine from brow to point, tapering
        c.fpoly([(31, 12), (32, 12), (32, 48), (31, 48)], "g")
        c.fpoly([(31, 49), (32, 49), (31, 57), (31, 57)], "g")
        c.stamp_c(["..G", ".gG", ".gG"], 5)                           # finial cap
        c.stamp_c(["G", "g", "G"], 9)
        # the slot and its bead
        c.fellipse(31.5, 27, 4.2, 9.6, "B")
        c.fellipse(31.5, 27, 3.2, 8.6, "K")
        by = 24 - round(4 * cs(t))                                   # the bead rides the slot, top to bottom
        c.fellipse(31.5, by + 2, 2.2, 2.4, "G")
        c.px(31, by + 1, "H")
        c.stamp_c(["...G", "..GG"], 38)                               # gold chevrons below the slot
        c.stamp_c(["..G", ".GG"], 43)
        return dict(top=5, left=10, right=53, mid=30)

    def fx(self, c, t, rage):
        # a pulse of light runs down the spine, brow to point
        y = 12 + t * 5
        for dy in (0, 1):
            c.px(31, y + dy, "H"); c.px(32, y + dy, "H")
        c.px(31, y - 1, "G"); c.px(32, y - 1, "G")
        # the finial burns: a tiny gold flame, flickering at its own pace
        h = (2, 3, 2, 4, 3, 2, 3, 4)[t]
        for i in range(h):
            c.px(31, 3 - i, "H" if i == 0 else "G" if i < h - 1 else "g")
            c.px(32, 3 - i, "H" if i == 0 else "G" if i < h - 1 else "g")
        # sparks leave the fin tips as the fins flex
        fl = round(1.2 * sn(t))
        if fl:
            mote(c, 8 if fl > 0 else 9, 29 + fl * 3); mote(c, 55 if fl > 0 else 54, 29 + fl * 3)


class Vakra(EnemyArt):
    """The crescent weaver: a pearl crescent, horns up, a gold eye in the cup, a gold lip."""
    name = "vakra"
    cracks = [(14, 34, 18, 42), (22, 46, 27, 48)]
    flames = [(10, 16)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        sw = round(1.4 * sn(t))
        outer = c._ellipse(31.5, 36, 24, 16)
        inner = c._ellipse(31.5, 29, 20.5, 12.5)
        crescent = outer & ~inner
        # horn tips lean with the sway
        crescent |= c._poly([(9, 31), (7, 24 + sw), (12, 28)]) | c._poly([(54, 31), (56, 24 + sw), (51, 28)])
        c.solid(crescent)
        lip = c.dilate(inner, 1) & crescent
        c.flat(lip, "g")
        c.flat(lip & ~c.dilate(inner, 1) | (lip & np_roll_up(inner)), "G")
        # the eye floats in the cup
        eye_socket(c, 31.5, 33, 7, 5)
        c.fellipse(31.5, 33, 7, 5, "K")
        ix = 31.5 - 1.8 * cs(t)
        c.fellipse(ix, 33, 3.4, 3.6, "g")
        c.fellipse(ix, 33, 2.4, 2.6, "G")
        c.px(ix - 0.5, 33, "K"); c.px(ix + 0.5, 33, "K")
        c.px(ix - 1.5, 31.5, "H")
        # filigree on the keel
        c.stamp_c(["....G", "...gG", "..G"], 47)
        c.stamp(["G", "gG"], 22, 45, True)
        return dict(top=24, left=7, right=56, mid=40)

    def fx(self, c, t, rage):
        sw = round(1.4 * sn(t))
        # lightning leaps between the horn tips, across the cup above the eye; it jitters frame to frame
        pts = []
        for i, x in enumerate(range(12, 52, 3)):
            pts.append((x, 24 + round(2.4 * sn(t, 3, i * 0.19)) + abs(sw)))
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            c.line(x0, y0, x1, y1, "L" if t % 2 else "l")
        for x, y in pts[1::2]:
            c.px(x, y, "H")
        # tassels under the keel sway like a pennant
        for sx in (-5, 5):
            for j in range(7):
                x = 31.5 + sx + round(1.7 * sn(t, 1, -j * 0.07) * j / 6.0 * (1 if sx > 0 else -1))
                c.px(x, 53 + j, "G" if j % 3 == 0 else "g")
            mote(c, 31.5 + sx, 60, "G", "B") if t in (2, 3) else None


def np_roll_up(mask):
    import numpy as np
    return np.roll(mask, -1, 0)


class Bhavi(EnemyArt):
    """The ram: a stout pearl shell, a gold brow, a black visor, two bone tusks."""
    name = "bhavi"
    cracks = [(20, 24, 26, 30), (16, 38, 22, 46)]
    flames = [(10, 12)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        d = round(0.5 - 0.5 * cs(t))
        # tusks, behind the shell, dip on the low point of the hover
        c.mpoly([(17, 44), (11, 53 + d), (13, 59 + d), (19, 55 + d), (24, 48)], "WCcS")
        # shoulder horns
        c.mpoly([(12, 30), (6, 21), (9, 20), (17, 27)], GOLD)
        c.ellipse(31.5, 35, 22.5, 20)                                # the shell
        c.stamp_c(["..............ccccCCCCC", "..........ccccCCCCCCCC", "..............CCCCCC"], 50)
        # gold brow plate, a heavy chevron
        c.mpoly([(11, 30), (14, 22), (22, 19), (31, 26), (31, 30), (22, 25), (16, 28)], GOLD)
        c.stamp_c(["G", "gG"], 16)
        # the visor, with two slit eyes that glow through
        c.fellipse(31.5, 36, 13.5, 4.6, "K")
        c.flat(c._ellipse(31.5, 35.5, 13.5, 4.6) & ~c._ellipse(31.5, 37.5, 13.5, 4.6), "S")
        scan = round(2 * sn(t))                                       # both slit eyes sweep the room together
        for sx in (-8, 8):
            c.fellipse(31.5 + sx + scan, 36, 2.2, 1.6 + 0.5 * max(0, sn(t, 2, 0.25)), "G")
            c.px(31.5 + sx + scan - 1, 35.5, "H")
        # cheek rivets
        for sx in (-17, 17):
            c.px(31.5 + sx, 43, "g")
        return dict(top=14, left=6, right=57, mid=35)

    def fx(self, c, t, rage):
        # steam vents from the shoulder horns: two puffs a half loop apart, rising and thinning
        for k in (0, 1):
            u = ((t + 4 * k) % 8) / 8.0
            y = 19 - u * 14
            for sx in (8, 55):
                x = sx + round(1.6 * sn(u * 8, 1, k * 0.5))
                c.px(x, y, "H" if u < 0.3 else "S" if u < 0.7 else "D")
                if u < 0.5:
                    c.px(x + 1, y + 1, "S")
        # the cheek rivets flash in turn
        if t in (2, 3):
            mote(c, 14.5, 43); mote(c, 48.5, 43)


class Mayavi(EnemyArt):
    """The mirror-bearer: a pearl egg with a split face and a ring of lavender glass."""
    name = "mayavi"
    cracks = [(24, 24, 28, 31), (21, 40, 26, 46)]
    flames = [(14, 12)]
    BLINK = ((4.8, 4.8), (4.8, 4.8), (1.4, 4.8), (4.8, 4.8), (4.8, 4.8), (4.8, 1.4), (4.8, 1.4), (4.8, 4.8))
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def _shards(self, c, t, front):
        for k in range(6):
            a = (k * 60 + t * 7.5) * math.pi / 180
            if (math.sin(a) > 0) != front:
                continue
            gx, gy = 31.5 + math.cos(a) * 26, 36 + math.sin(a) * 10
            c.poly([(gx, gy - 6), (gx + 3.4, gy), (gx, gy + 6), (gx - 3.4, gy)], LAV)

    def draw(self, c, t, rage):
        self._shards(c, t, False)
        c.ellipse(31.5, 36, 16, 19)
        c.stamp_c(["..............ccccCC", "............ccCCCCCC", "..............CCCCC"], 50)
        # the seam that splits the face, and a gold belt
        for y in range(21, 54):
            c.px(31, y, "D"); c.px(32, y, "D")
        c.flat(c._ellipse(31.5, 36, 16, 19) & (np_rows(46, 48)), "B")
        c.flat(c._ellipse(31.5, 36, 16, 19) & (np_rows(46, 47)), "g")
        # two eyes, each its own: lavender iris, gold core
        for side, ry in ((-1, self.BLINK[t][0]), (1, self.BLINK[t][1])):
            ex = 31.5 + side * 7.5
            c.fellipse(ex, 34, 4.6, ry + 0.8, "B")
            c.fellipse(ex, 34, 3.8, ry, "K")
            if ry > 3:
                c.fellipse(ex, 34, 2.6, 3.0, "L")
                c.fellipse(ex, 34, 1.5, 1.8, "G")
                c.px(ex - 1, 32.5, "H")
        c.stamp_c([".....gG", "....gGG", "...gG"], 14)                # gold finial
        pul = 0.5 * max(0, sn(t, 1, 0.125))                          # the third jewel breathes
        c.fellipse(31.5, 25, 2 + pul, 2.4 + pul, "l"); c.fellipse(31.5, 25, 1.2 + pul, 1.5 + pul, "L"); c.px(31, 24, "H")
        self._shards(c, t, True)
        return dict(top=14, left=5, right=58, mid=36)

    def fx(self, c, t, rage):
        # a bead of light runs down the seam
        y = 21 + t * 4
        for dy in (0, 1):
            c.px(31, y + dy, "H"); c.px(32, y + dy, "H")
        c.px(31, y - 1, "L"); c.px(32, y - 1, "L")
        # the glass shards twinkle in turn
        for k in range(6):
            a = (k * 60 + t * 7.5) * math.pi / 180
            if math.sin(a) > 0 and (k + t) % 4 == 0:
                mote(c, 31.5 + math.cos(a) * 26, 36 + math.sin(a) * 10 - 1, "H", "L")


def np_rows(a, b):
    import numpy as np
    m = np.zeros((64, 64), dtype=bool)
    m[a:b + 1, :] = True
    return m


class Dvarapala(EnemyArt):
    """The door-ward: an arched pearl door-shield, a gold panel with a keyhole eye, pauldrons."""
    name = "dvarapala"
    cracks = [(20, 24, 24, 32), (18, 40, 23, 50)]
    flames = [(8, 10)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        p = round(0.5 - 0.5 * cs(t))
        # pauldrons: gold pads with studs, settling on the hover
        c.mpoly([(15, 25 + p), (6, 22 + p), (2, 30 + p), (4, 40 + p), (14, 38 + p)], GOLD)
        for dy in (28, 34):
            c.px(7, dy + p, "B"); c.px(56, dy + p, "B")
        # feet: two pearl pylons
        c.mpoly([(19, 55), (27, 55), (27, 61), (19, 61)], "WPSD")
        # the door: a rectangle under an arch
        body = c._ellipse(31.5, 26, 18.5, 17) | (np_rect(13, 26, 50, 57))
        c.solid(body)
        # inset gold panel
        panel = c._ellipse(31.5, 29, 13, 10) | np_rect(18, 29, 45, 52)
        c.solid(panel, GOLD)
        c.stamp_c([".......GGGG"], 21)
        # panel seam and studs
        for y in range(40, 52):
            c.px(31, y, "B"); c.px(32, y, "B")
        for dy in (33, 41, 49):
            c.px(21, dy, "B"); c.px(42, dy, "B")
            c.px(21, dy - 1, "H"); c.px(42, dy - 1, "H")
        # the keyhole eye: a round socket, a narrow stem, a gold iris that swells
        c.fellipse(31.5, 33, 5.2, 5.2, "K")
        c.flat(np_rect(30, 36, 33, 46), "K")
        ir = 2.7 - 0.5 * cs(t) + 0.0                                  # the iris swells and eases
        c.fellipse(31.5, 33, ir, ir, "G")
        c.px(30, 31, "H")
        c.stamp_c(["....GG", "...gGG"], 6)                           # keystone
        return dict(top=7, left=2, right=61, mid=34)

    def fx(self, c, t, rage):
        # a scan line crosses the gold panel, top to bottom
        y = 20 + t * 4
        for x in range(19, 45):
            if c.g[y, x] == IDX["G"]:
                c.g[y, x] = IDX["H"]
            elif c.g[y, x] == IDX["g"]:
                c.g[y, x] = IDX["G"]
        # the studs light in a chase: pauldron, pauldron, then down the panel
        p = round(0.5 - 0.5 * cs(t))
        studs = [(7, 28 + p), (7, 34 + p), (21, 33), (21, 41), (21, 49)]
        if t < len(studs):
            x, y = studs[t]
            c.px(x, y, "H"); c.px(63 - x, y, "H")
            c.px(x, y - 1, "G"); c.px(63 - x, y - 1, "G")
        # the keyhole breathes a ring of light at its widest
        if t in (3, 4, 5):
            for a in range(0, 360, 45):
                c.px(31.5 + math.cos(math.radians(a)) * 6.3, 33 + math.sin(math.radians(a)) * 6.3, "G")


def np_rect(x0, y0, x1, y1):
    import numpy as np
    m = np.zeros((64, 64), dtype=bool)
    m[y0:y1 + 1, x0:x1 + 1] = True
    return m


class Dipaka(EnemyArt):
    """The lamp-keeper: a pearl teardrop lamp, a lavender flame in its belly, a gold three-tongue crown."""
    name = "dipaka"
    cracks = [(24, 38, 28, 44), (26, 50, 30, 55)]
    flames = [(20, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        lift = [-round(1.0 + sn(t, 2, o)) for o in (0.0, 0.33, 0.66)]   # three tongues, each its own flicker
        bowl = c._ellipse(31.5, 41, 14.5, 13.5) | c._poly([(18, 47), (31, 61), (32, 61), (45, 47)])
        # handles
        c.mpoly([(18, 38), (11, 36), (9, 43), (12, 48), (18, 47)], GOLD)
        c.solid(bowl)
        # the window: bronze frame, lavender glass, hot core that swells
        g = 1 + sn(t)
        c.fellipse(31.5, 41, 9.8, 9.3, "B")
        c.fellipse(31.5, 41, 8.8, 8.3, "K")
        c.fellipse(31.5, 41, 7.8 + g * 0.3, 7.3 + g * 0.3, "l")
        c.fellipse(31.5, 41, 6.0 + g * 0.4, 5.6 + g * 0.4, "L")
        c.fellipse(31.5, 42, 3.4 + g * 0.4, 3.4 + g * 0.4, "H")
        a = t * 45 * math.pi / 180                                    # a swirl of light turns in the glass
        c.px(31.5 + math.cos(a) * 4.6, 41 + math.sin(a) * 4.2, "H")
        c.px(31.5 - math.cos(a) * 4.6, 41 - math.sin(a) * 4.2, "L")
        c.px(27, 37, "H"); c.px(28, 36, "H")
        # gold collar
        c.solid(np_rect(21, 26, 42, 30), GOLD)
        # the crown: three tongues of gold flame, each lifting out of step
        for cxp, base_half, tip, lf in ((24, 3.2, 14, lift[0]), (31.5, 4.0, 6, lift[1]), (39, 3.2, 14, lift[2])):
            c.poly([(cxp - base_half, 27), (cxp - 1.2, 18 + lf), (cxp, tip + lf), (cxp + 1.2, 18 + lf),
                    (cxp + base_half, 27)], "HGgB")
            c.fpoly([(cxp - 1, 26), (cxp, tip + 5 + lf), (cxp + 1, 26)], "H")
        return dict(top=6, left=9, right=54, mid=40)

    def fx(self, c, t, rage):
        # embers lift off the crown and gutter out
        for x, off, drift in ((20, 0, -1), (31, 3, 0), (43, 5, 1), (26, 6, -1), (37, 1, 1)):
            u = (t + off) % 8
            y = 13 - u * 1.7
            x2 = x + round(drift * u / 4.0 + 0.8 * sn(u, 1))
            c.px(x2, y, "H" if u < 2 else "G" if u < 5 else "B")


class Vanija(EnemyArt):
    """The bay-master: a broad pearl hull, a gold scale-beam with tipping pans, a ring of coins."""
    name = "vanija"
    cracks = [(14, 38, 20, 42), (24, 44, 28, 48)]
    flames = [(8, 22)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        tl = round(2 * sn(t))
        # the beam and post
        c.fpoly([(6, 19), (57, 19), (57, 21), (6, 21)], "g")
        c.fpoly([(6, 21), (57, 21), (57, 22), (6, 22)], "B")
        c.fpoly([(30, 21), (33, 21), (33, 30), (30, 30)], "g")
        c.ellipse(31.5, 16, 3.6, 3.6, GOLD)
        # pans on strings: they tip alternately
        for px_, d in ((11, tl), (52, -tl)):
            py = 32 + d
            c.line(7 if px_ < 30 else 56, 22, px_ - 7, py, "g")
            c.line(7 if px_ < 30 else 56, 22, px_ + 7, py, "g")
            c.solid(c._ellipse(px_, py + 1, 8.5, 3), GOLD)
        # the hull
        c.ellipse(31.5, 41, 24, 12.5)
        c.stamp_c(["..........cccccccCCCC", "..............CCCCCCC"], 48)
        # the eye: lavender-gold, takes in the pilot
        if t == 7:                                                    # a blink
            eye_socket(c, 31.5, 39, 10, 1.8)
        else:
            eye_socket(c, 31.5, 39, 10, 5.4)
            lk = round(2.2 * sn(t, 1, 0.25))                          # takes in the bay: left, then right
            c.fellipse(31.5 + lk * 0.5, 39, 5.2, 4.6, "L")
            c.fellipse(31.5 + lk, 39, 3.1, 3.1, "G")
            c.px(31 + lk, 39, "K"); c.px(32 + lk, 39, "K"); c.px(31 + lk, 40, "K"); c.px(32 + lk, 40, "K")
            c.px(29 + lk, 37, "H")
        # the coin ring along the lower edge
        for i, cx_ in enumerate((19.5, 25.5, 31.5, 37.5, 43.5)):      # a wave of hops runs along the coins
            hop = 2 if t == i + 1 else 1 if t in (i, i + 2) and 0 <= i else 0
            c.solid(c._ellipse(cx_, 50.5 - hop, 3, 3), GOLD)
            c.px(cx_ - 1, 49 - hop, "H")
        return dict(top=13, left=5, right=58, mid=40)

    def fx(self, c, t, rage):
        # the jewel on the post flashes, and the beam ends twinkle in turn
        if t in (0, 1):
            mote(c, 31.5, 15.5)
        if t == 3:
            mote(c, 6, 19)
        if t == 7:
            mote(c, 57, 19)
        # a coin glints as the hop passes it
        for i, cx_ in enumerate((19.5, 25.5, 31.5, 37.5, 43.5)):
            if t == i + 1:
                mote(c, cx_, 46.5 - 2, "H", "W")


ENEMIES = {e.name: e for e in (Auris(), Hataka(), Lance(), Vakra(), Bhavi(), Mayavi(),
                               Dvarapala(), Dipaka(), Vanija())}

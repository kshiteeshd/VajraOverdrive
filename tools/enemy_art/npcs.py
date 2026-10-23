"""The three people of Atala, drawn by hand. They are not enemies and must never read as enemies:
where a soldier has a black slit for an eye, a person has a lamp; where a soldier is pearl and gold
alone, a person is lit from within in sky blue, and wears a thin halo ring (the diya the people of
the lokas keep over their heads). docs/NPC_BIBLE.md says who each one is and what they do."""
import math
from kit import Canvas, EnemyArt, S

GOLD = "HGgB"
SKY = "hnNK"      # lit, body, shade, deep - the blue of a person
LAV = "HLll"


def halo(c, cx, cy, rx, ry, widen=0):
    """A broken ring of sky-blue pixels over the head, brightest at the ends."""
    rx += widen
    for k in range(-5, 6):
        a = math.pi / 2 + k * 0.28
        x, y = cx + math.cos(a) * rx, cy - math.sin(a) * ry
        c.px(round(x), round(y), "h" if abs(k) in (0, 5) else "n")


class Person(EnemyArt):
    hover = (0, -1, -1, 0)

    def draw(self, c, t, mode):
        raise NotImplementedError


class Scout(Person):
    """Uparichara's Scout: a watch-craft of the old king's fleet, wing broken, lamp still lit."""
    name = "scout"
    SMOKE = ((0, 0), (1, -2), (2, -4), (1, -6))

    def draw(self, c, t, mode):
        # the intact wing, left
        c.poly([(25, 27), (9, 33), (4, 43), (13, 41), (25, 37)])
        c.fpoly([(24, 30), (11, 34), (8, 39)], "n")
        # the broken wing, right: a short jag and a crack through it
        c.poly([(39, 27), (50, 31), (55, 36), (49, 37), (53, 42), (45, 40), (39, 37)])
        c.line(46, 33, 50, 40, "K")
        c.line(47, 38, 44, 41, "K")
        # smoke from the wound
        for i, (dx, dy) in enumerate(self.SMOKE):
            c.px(50 + dx + (t % 2), 29 - i * 2 + (dy if t % 2 == 0 else dy + 1), "S" if i % 2 == 0 else "D")
        # the hull: a pearl lens with a gold spine
        c.ellipse(32, 34, 10, 16)
        c.fpoly([(31, 22), (32, 22), (32, 44), (31, 44)], "g")
        c.stamp_c(["......cccC", ".....cCCCC"], 40)
        # the beacon pod on the back: a lamp that breathes, and in work frames, flares
        pulse = (1.6, 2.2, 2.8, 2.2)[t] + (0.8 if mode == "work" else 0)
        c.fellipse(32, 20, 4.5, 4.5, "B")
        c.fellipse(32, 20, 3.3, 3.3, "K")
        c.fellipse(32, 20, pulse, pulse, "h")
        if mode == "work":
            for ang in range(0, 360, 45):
                r = 8 + (t % 2) * 2
                c.px(round(32 + math.cos(math.radians(ang)) * r), round(20 + math.sin(math.radians(ang)) * r), "n")
        # the watcher's lens on a short stalk, facing down
        c.line(32, 49, 32, 52, "B")
        c.fellipse(32, 55, 5.5, 4.5, "B")
        c.fellipse(32, 55, 4.3, 3.4, "K")
        bright = 3.2 if mode == "talk" else 2.4
        c.fellipse(32, 55, bright, bright * 0.8, "h")
        c.px(30, 54, "H")
        if mode == "talk":  # a few ticks of signal beside the lens
            k = t % 2
            for sx in (-1, 1):
                c.px(32 + sx * (9 + k), 55, "n")
                c.px(32 + sx * (11 + k), 54, "n")
                c.px(32 + sx * (11 + k), 56, "n")
        halo(c, 32, 11, 13, 4, widen=2 if mode == "hail" else 0)
        return dict(top=8, left=4, right=59, mid=34)


class Maya(Person):
    """The Ninth Maya: the last wright of a forge-line. A pearl-and-bronze hull with an anvil for a
    prow, a hammer arm that rests up and swings when she works, a forge lit in her belly."""
    name = "maya"
    HAMMER = ((55, 24), (57, 30), (54, 38), (50, 44))   # head positions along the swing

    def draw(self, c, t, mode):
        # the hull
        c.ellipse(32, 35, 21, 14)
        c.fellipse(32, 44, 18, 4, "B")
        c.fellipse(32, 43, 17, 3, "g")
        # the anvil prow, point down
        c.poly([(21, 46), (43, 46), (39, 51), (35, 51), (35, 55), (29, 55), (29, 51), (25, 51)], GOLD)
        c.px(23, 47, "H"); c.px(24, 47, "H")
        # the forge window: bronze frame, black, then fire that flickers
        c.fellipse(32, 34, 9.5, 7.5, "B")
        c.fellipse(32, 34, 8.5, 6.5, "K")
        flick = (6.0, 5.2, 6.4, 5.6)[t] + (0.8 if mode == "talk" else 0)
        c.fellipse(32, 34, flick, flick * 0.75, "g")
        c.fellipse(32, 34, flick - 2.2, (flick - 2.2) * 0.75, "G")
        c.px(29, 32, "H"); c.px(30, 31, "H")
        # the tongs, left: a pair of thin rods
        c.line(12, 33, 8, 42, "B"); c.line(13, 35, 10, 43, "B")
        # the hammer arm, right: a rod to a block; it rests up, and swings in work frames
        hx, hy = self.HAMMER[t] if mode == "work" else self.HAMMER[0]
        c.line(46, 35, hx, hy + 2, "B")
        c.poly([(hx - 5, hy - 3), (hx + 4, hy - 3), (hx + 4, hy + 3), (hx - 5, hy + 3)], GOLD)
        if mode == "work" and t in (2, 3):  # the strike throws sparks
            for dx, dy in ((-4, 2), (3, 4), (-1, 6)):
                c.px(hx + dx, hy + dy + 4, "H")
        halo(c, 32, 17, 14, 4, widen=2 if mode == "hail" else 0)
        return dict(top=14, left=8, right=58, mid=36)


class Kaliya(Person):
    """Kaliya the Tide-Pilot: a naga courier, blue scales, a hood with three gold jewels. The body is
    an S that travels down the frame, head at the foot, facing the player."""
    name = "kaliya"

    def draw(self, c, t, mode):
        amp = 7 + (2 if mode == "work" else 0)
        ph = t * math.pi / 2
        # the body: a chain of scale discs, thin at the tail, thick at the neck
        pts = []
        for i in range(8):
            y = 10 + i * 5.2
            x = 32 + amp * math.sin(ph + i * 0.85)
            pts.append((x, y, 2.6 + i * 0.55))
        for x, y, r in pts:
            c.ellipse(x, y, r, r + 1.4, SKY)
        for i, (x, y, r) in enumerate(pts[1:], 1):  # a scale mark across each disc
            c.px(round(x - 1), round(y), "N"); c.px(round(x + 1), round(y), "N")
            if i % 2 == 0:
                c.px(round(x), round(y - 2), "h")
        # the hood: a wide fan round the head, three jewels
        wide = 17 if mode == "talk" else 15
        hy = 50
        c.ellipse(32, hy, wide, 9, SKY)
        c.fellipse(32, hy + 1, wide - 5, 6, "L")
        c.fellipse(32, hy + 1, wide - 7, 4, "H")
        glow = "H" if mode == "talk" else "G"
        for dx, dy in ((-7, -3), (0, -5), (7, -3)):
            c.fellipse(32 + dx, hy + dy, 2, 2, "B")
            c.fellipse(32 + dx, hy + dy, 1.2, 1.2, glow)
        # the face: two lamps, a small mouth
        c.fellipse(28, hy + 3, 2.2, 1.8, "K")
        c.fellipse(36, hy + 3, 2.2, 1.8, "K")
        c.px(28, hy + 3, "h"); c.px(36, hy + 3, "h")
        c.px(31, hy + 6, "K"); c.px(32, hy + 6, "K")
        halo(c, 32, 5, 12, 3, widen=2 if mode == "hail" else 0)
        return dict(top=2, left=14, right=50, mid=36)


PEOPLE = {p.name: p for p in (Scout(), Maya(), Kaliya())}

"""The two people of Vitala, drawn by hand (docs/NPC_BIBLE.md, docs/VITALA_ENEMIES.md): the Gold-Seller, a bhuta of Hara's court who weighs hataka
gold, and Gauri's Maid, who keeps the shrine lamps.  They are people and must never read as enemies: lit from within in the cold blue of the loka's
ghosts, a thin halo over the head, and a LAMP where a soldier has a slit.  (Importing vitala puts the kit in Vitala's palette.)"""
import math

import vitala  # noqa: F401
from kit import Canvas, EnemyArt
from npcs import GOLD, SKY, Person, halo


class Seller(Person):
    """The Gold-Seller: a ghost-blue lens of a hull with two lamp-eyes and a smile, a great balance on its back - the beam tips as it talks, an
    ingot of hataka gold in one pan and a blue shard in the other."""
    name = "seller"

    def draw(self, c, t, mode):
        ph = t * math.pi / 2
        tilt = round((3.2 if mode in ("talk", "work") else 1.4) * math.sin(ph))
        # the scale: a pole up from the hull, a beam across, two chains and two pans
        c.line(32, 29, 32, 12, "g")
        c.line(9, 12 + tilt, 55, 12 - tilt, "G")
        c.px(9, 11 + tilt, "H"); c.px(55, 11 - tilt, "H")
        for end, sgn in ((9, 1), (55, -1)):
            ey = 12 + sgn * tilt
            px = end + (2 if sgn > 0 else -2)
            c.line(end, ey, px - 5, ey + 11, "B"); c.line(end, ey, px + 5, ey + 11, "B")
            c.ellipse(px, ey + 12, 7, 2.6, GOLD)
        c.fellipse(11, 23 + tilt, 4.2, 2.2, "G")                     # an ingot of hataka gold on the left
        c.px(9, 22 + tilt, "H"); c.px(10, 22 + tilt, "H")
        c.poly([(53, 19 - tilt), (56, 22 - tilt), (53, 24 - tilt), (50, 22 - tilt)], "hnNK")   # a blue shard on the right
        # the hull and its two lamp-eyes
        c.ellipse(32, 41, 17, 11, SKY)
        c.fellipse(32, 48, 14, 3, "N")
        for sx in (-1, 1):
            c.fellipse(32 + sx * 6.5, 40, 3.0, 2.5, "B")
            c.fellipse(32 + sx * 6.5, 40, 2.3, 1.9, "K")
            c.fellipse(32 + sx * 6.5, 40, 1.4, 1.2, "H" if mode == "talk" else "h")
        for x in range(27, 38):                                     # a small smile
            c.px(x, 45 - (1 if abs(x - 32) > 3 else 0), "K")
        if mode == "work" and t in (1, 2):                          # a coin hops from pan to pan
            c.px(32 + (t - 1.5) * 10, 18 - abs(t - 1.5) * 6, "G")
        halo(c, 32, 5, 12, 3, widen=2 if mode == "hail" else 0)
        return dict(top=4, left=9, right=55, mid=38)


class Maid(Person):
    """Gauri's Maid: a slim lamp-bearer, lotus-petal wings, a diya held out in front of her with a flame that licks, two lamps for eyes."""
    name = "maid"

    def draw(self, c, t, mode):
        sway = round(1.5 * math.sin(t * math.pi / 2))
        # the petals: a lotus of wings, three to a side, folded out as she talks
        spread = 3 if mode == "talk" else 0
        for k, (x1, y1) in enumerate(((10 - spread, 28), (6 - spread, 38 + sway), (12 - spread, 48))):
            c.poly([(26, 38), (x1, y1), (x1 + 7, y1 + 6), (27, 43)], GOLD)
            c.poly([(37, 38), (63 - x1, y1), (63 - x1 - 7, y1 + 6), (36, 43)], GOLD)
        # the slim hull, pointed toward the player
        c.mpoly([(32, 14), (27, 20), (25, 34), (27, 46), (31, 54)], SKY)
        c.fpoly([(31, 18), (32, 18), (32, 50), (31, 50)], "N")
        for sx in (-1, 1):                                          # her two lamps
            c.fellipse(32 + sx * 3.6, 31, 2.2, 1.9, "K")
            c.px(32 + sx * 3.6 - 0.5, 30.5, "h")
        # the diya she carries at the nose: a bowl of gold, a flame that licks, brighter when she works
        c.ellipse(32, 58, 7, 3, GOLD)
        h = (4, 6, 5, 7)[t] + (3 if mode == "work" else 0)
        for i in range(h):
            c.px(32 + (1 if t % 2 and i > h - 3 else 0), 55 - i, "H" if i == 0 else "G" if i < h - 2 else "E")
            if i < h - 2:
                c.px(31, 55 - i, "g"); c.px(33, 55 - i, "g")
        halo(c, 32, 8, 12, 3, widen=2 if mode == "hail" else 0)
        return dict(top=6, left=6, right=57, mid=36)


PEOPLE = {p.name: p for p in (Seller(), Maid())}

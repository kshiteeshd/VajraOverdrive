"""The three KEEPERS of Vitala, drawn by hand (docs/VITALA_ENEMIES.md): the people with a post.  Same ship language as the six - nose down, engines
streaming up, black basalt and gold - but each is built round its job.  They are named for what they became in the second loka:
  DVARAPALA -> the gate-bull   (a heavy door-ward with the sweeping horns of Hara's bull)
  DIPAKA    -> the fire-priest (a lamp-ship that carries three flames and mends its comrades)
  VANIJA    -> the assayer     (a barge with a balance, docking claws, and a coin ring: it runs the Gold Assay)
Their sheet files keep the kind names (dvarapala.png, dipaka.png, vanija.png), so EnemyImages finds them with the rest of Vitala's."""
import math

import vitala
from kit import EnemyArt
from vitala import BASALT, GOLD, TAU, ember_eye, exhaust, flame, mote, nozzle, sn


class GateBull(EnemyArt):
    """DVARAPALA: the door-ward.  Wide shield of a hull, heavy shoulder plates, two great horns that sweep out and down to gold points, a visor with
    an ember that scans, twin engines.  It stands in a doorway and does not move."""
    name = "dvarapala"
    cracks = [(24, 26, 27, 34), (36, 40, 38, 46)]
    flames = [(22, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        c.mpoly([(22, 31), (12, 27), (5, 33), (4, 45), (8, 56), (12, 47), (12, 39), (19, 37)], GOLD)            # the horns
        c.mpoly([(24, 22), (10, 22), (5, 31), (12, 37), (24, 35)], BASALT)                                    # the pauldrons
        c.mpoly([(31, 61), (22, 55), (16, 40), (16, 22), (22, 12), (31, 10)], BASALT)                         # the shield of a hull
        c.fmpoly([(17, 40), (31, 47), (31, 51), (17, 44)], "g")                                              # gold chevron plates
        c.fmpoly([(17, 48), (31, 55), (31, 58), (17, 52)], "B")
        c.fmpoly([(18, 20), (31, 22), (31, 24), (18, 22)], "B")
        c.fellipse(31.5, 30, 10, 3.4, "K")                                                                   # the visor
        look = 2.2 * sn(t)
        c.fellipse(31.5 + look, 30, 5.5, 1.9, "E")
        c.fellipse(31.5 + look, 30, 2.6, 1.0, "G")
        nozzle(c, 21, 26, 9); nozzle(c, 37, 42, 9)
        for sx in (-1, 1):
            c.px(31.5 + sx * 11, 46, "g")
        return dict(top=8, left=4, right=59, mid=34)

    def fx(self, c, t, rage):
        exhaust(c, 23, 8, (4, 6, 5, 7, 5, 4, 6, 7)[t], wide=1)
        exhaust(c, 40, 8, (6, 5, 7, 5, 4, 6, 5, 7)[t], wide=1)
        if t in (2, 3):
            mote(c, 7, 56, "H", "E"); mote(c, 56, 56, "H", "E")


class FirePriest(EnemyArt):
    """DIPAKA: the lamp-keeper.  A slim hull like a lamp-post, two curved arms out to bowls that burn, a brazier at the nose with a three-tongue flame
    (the three fires), a small engine.  Its flames brighten when it mends its comrades."""
    name = "dipaka"
    cracks = [(29, 24, 29, 34), (33, 40, 34, 46)]
    flames = [(24, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        c.mpoly([(27, 36), (17, 31), (9, 35), (8, 44), (13, 42), (19, 40), (27, 42)], GOLD)                   # the lamp-arms
        for sx in (-1, 1):
            c.ellipse(31.5 + sx * 20.5, 44, 4.2, 2.6, GOLD)                                                  # a bowl at each tip
        c.mpoly([(31, 60), (27, 52), (26, 32), (28, 18), (31, 13)], BASALT)                                   # the lamp-post hull
        for y in (22, 30, 38):
            c.fmpoly([(26, y), (31, y), (31, y + 1), (26, y + 1)], "G")                                      # gold collars
        for sx in (-1, 1):
            ember_eye(c, 31.5 + sx * 3.4, 27, 2.0, 1.8, slit=False)
        c.ellipse(31.5, 56, 8.5, 3.2, GOLD)                                                                  # the brazier at the nose
        c.fellipse(31.5, 56, 6.2, 2.0, "e")
        nozzle(c, 28, 35, 10)
        return dict(top=8, left=8, right=56, mid=34)

    def fx(self, c, t, rage):
        exhaust(c, 31, 9, (4, 6, 5, 7, 5, 4, 6, 7)[t])
        flame(c, 31, 53, (5, 7, 6, 8, 6, 5, 7, 8)[t], lean=1 if t % 3 == 0 else 0)                          # the three fires
        flame(c, 26, 54, (3, 5, 4, 5, 4, 3, 5, 4)[(t + 3) % 8], lean=-1)
        flame(c, 37, 54, (4, 3, 5, 4, 3, 5, 4, 5)[t], lean=1)
        for sx in (-1, 1):
            flame(c, 31 + sx * 20 - (1 if sx > 0 else 0), 42, (2, 3, 2, 4, 3, 2, 3, 3)[(t + (2 if sx > 0 else 0)) % 8])


class Assayer(EnemyArt):
    """VANIJA: the bay-master of the Gold Assay.  A broad barge with a balance on its back (the beam tips), docking claws at its sides, a lens for an
    eye and a ring of coins that hop in a wave along its prow."""
    name = "vanija"
    cracks = [(20, 26, 24, 34), (34, 38, 38, 44)]
    flames = [(22, 4)]
    hover = (0, 0, -1, -1, -1, -1, 0, 0)

    def draw(self, c, t, rage):
        tilt = round(2.4 * sn(t))
        c.mpoly([(12, 34), (5, 38), (3, 49), (8, 55), (9, 46), (14, 44)], GOLD)                               # the docking claws
        c.mpoly([(31, 57), (18, 53), (10, 41), (10, 24), (16, 15), (31, 13)], BASALT)                         # the barge
        c.fmpoly([(11, 26), (31, 28), (31, 30), (11, 28)], "B")
        c.line(31, 12, 31, 24, "g")                                                                          # the balance: a pole, a beam, two pans
        c.line(15, 17 + tilt, 48, 17 - tilt, "G")
        for end, sg in ((15, 1), (48, -1)):
            ey = 17 + sg * tilt
            c.line(end, ey, end - 3, ey + 6, "B"); c.line(end, ey, end + 3, ey + 6, "B")
            c.ellipse(end, ey + 7, 4.4, 1.8, GOLD)
        ember_eye(c, 31.5, 38, 7, 4.8, look=round(1.8 * sn(t, 1, 0.25)))                                     # the lens
        for i, cx in enumerate((17.5, 24.5, 31.5, 38.5, 45.5)):                                              # a wave of hops runs along the coins
            hop = 2 if t == i + 1 else 1 if t in (i, i + 2) else 0
            c.ellipse(cx, 50 - hop, 2.8, 2.8, GOLD)
        nozzle(c, 20, 25, 11); nozzle(c, 38, 43, 11)
        return dict(top=9, left=3, right=60, mid=36)

    def fx(self, c, t, rage):
        exhaust(c, 22, 10, (4, 6, 5, 7, 5, 4, 6, 7)[t])
        exhaust(c, 40, 10, (6, 5, 7, 5, 4, 6, 5, 7)[t])
        if t in (0, 4):
            mote(c, 31.5, 15, "H", "G")


KEEPERS = {k.name: k for k in (GateBull(), FirePriest(), Assayer())}

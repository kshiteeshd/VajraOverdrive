"""THE ROUND CREATURES OF VITALA: nine of them, each drawn from spheres (roundkit.py), each with an idle loop and three action clips that it does now and then.
Run:  python tools/enemy_art/vitala_round.py [kind ...]      -> assets/enemies/vitala/<kind>.png (and build/vitala_<kind>_preview.png)

 agneya  the Ember        VAKRA slot    a cracked basalt shell round a molten heart, a crest of flame, two fire-puffs for wings
 jvala   the Cinder       MAYAVI slot   a round ash-lantern with an ember mouth, three cinders orbiting it, smoke rising
 tamra   the Ore          BHAVI slot    a copper-plated armadillo: dome, three plates, a glowing visor slit, two stub thrusters
 kila    the Nail         LANCE slot    a small gold ball with one long forward spike, a ring of short ones, one big lens
 dhuma   the Smoke        AURIS slot    a round white skull-ghost on a trailing hem of mist, blue fire in the sockets
 rekha   the Line         HATAKA slot   a round hooded wraith, arms wide in welcome, gold bangles, a wide grin
 dvarapala  the gate-ward   (keeper)     a heavy round guardian with a helm band and a round shield
 dipaka     the lamp-keeper (keeper)     a glass lantern-spirit with a flame inside, small feet and hands
 vanija     the bay-master  (keeper)     a plump round merchant with a gold sash and orbiting coins
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import roundkit as R
from roundkit import (ASH, BASALT, COPPER, EMBER, FIRE, GHOST, GOLD, ROBE, SKIN, STEEL, S, Frame, PX, PY)

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'enemies', 'vitala')
EYE = (255, 214, 90)
EYE_HOT = (255, 250, 220)


def decorate(f, st, cx, top, cy, r):
    if st.variant == 1:
        R.flame_crown(f, cx, top, st)
    elif st.variant == 2:
        R.elite_halo(f, cx, cy, r, st)


def eye(f, x, y, r, st, color=EYE, lid=0.0, look=0.0, glow=0.8):
    """A round glowing eye: a dark socket, the lit ball, a pupil that can look aside, a lid that can close (lid 0..1)."""
    f.spot(x, y, r + 0.9, (24, 10, 8))
    c = R.lerp(color, EYE_HOT, 0.5 * st.rage)
    f.spot(x, y, r, c)
    f.spot(x + look * 0.9 - 0.6, y - 0.5, max(0.8, r * 0.38), (255, 255, 255))
    f.spot(x + look * 1.1 + 0.5, y + 0.4, max(0.7, r * 0.34), (40, 12, 6))
    f.halo(x, y, r * 3, c, glow * (0.8 + 0.3 * st.rage))
    if lid > 0:
        m = (PX - x) ** 2 + (PY - y) ** 2 <= (r + 0.9) ** 2
        cut = m & (PY < y - r + 2 * (r + 0.9) * lid)
        f.rgb[cut] = (60, 40, 46)
        f.a[cut] = 1.0


def sparks(f, cx, cy, u, n=8, reach=20, color=EMBER[3], start=0.0):
    """A scatter of embers thrown out and falling off (clip progress u from `start`)."""
    if u <= start:
        return
    k = min(1.0, (u - start) / (1.0 - start))
    for i in range(n):
        a = i * 2 * math.pi / n + 0.4
        d = 3 + reach * k * (0.6 + 0.4 * ((i * 7) % 5) / 5.0)
        x, y = cx + math.cos(a) * d, cy + math.sin(a) * d * 0.8 + 6 * k * k
        if k < 0.95:
            f.spot(x, y, 1.4 * (1 - 0.6 * k), color)
            f.halo(x, y, 4, color, 0.3 * (1 - k))


def smoke_puff(f, x, y, k, r0=3.0, ramp=ASH):
    """A puff of smoke that rises and grows and fades (k 0..1)."""
    if k <= 0 or k >= 1:
        return
    r = r0 + 5 * k
    yy = y - 14 * k
    col = R.lerp(ramp[2], ramp[0], k)
    m = (PX - x) ** 2 + (PY - yy) ** 2 <= r * r
    dither = ((PX.astype(int) + PY.astype(int)) % 2 == 0) | (k < 0.4)
    f.blob(m & dither, col)


# ============================================================================================================================ AGNEYA, the Ember
def agneya(st):
    f = Frame()
    b = st.ramp(BASALT)
    em = st.ramp(EMBER)
    cx, cy = 32.0, 35.0 + 1.3 * st.s(1)
    spin = st.env(0.05, 0.95) if st.acting('C') else 0.0
    sneeze = st.env(0.2, 0.55) if st.acting('B') else 0.0
    sx, sy = 1.0 + 0.07 * st.s(2) + 0.14 * sneeze, 1.0 - 0.07 * st.s(2) - 0.16 * sneeze
    r = 15.5
    # the wings: two puffs of fire that bob out of step, and whirl when it spins
    for side in (-1, 1):
        ph = 0.5 if side > 0 else 0.0
        ang = (st.s(1, ph) * 0.35) + spin * (st.u * 14 * side)
        wx = cx + side * (r + 3.0) * sx + math.cos(ang) * 2.0 * (1 + 2 * spin)
        wy = cy - 2 + math.sin(ang) * 3.0 * (1 + 2 * spin) + 1.5 * st.s(2, ph)
        f.ball(wx, wy, 5.2, 5.6, em, outline=True)
        f.halo(wx, wy, 11, EMBER[2], 0.5)
    # a short trail of embers off the back (the top of the sprite)
    for i in range(3):
        f.spot(cx + 1.5 * st.s(2, i * 0.2), cy - r * sy - 2 - i * 3.2, 2.2 - 0.5 * i, em[2 if i == 0 else 1])
    # the shell: basalt, cracked, the cracks lit from within and sliding when it spins
    ph1, ph2 = st.t * 2 * math.pi * 0 + spin * st.u * 9.0, 0.7
    heartbeat = 0.5 + 0.5 * st.s(1)

    def cracks(nx, ny, nz):
        v = np.abs(np.sin(5.2 * nx + 3.1 * ny + ph1)) * np.abs(np.sin(4.0 * ny - 2.4 * nx + ph2 + ph1 * 0.5))
        return ((v < 0.035 + 0.02 * heartbeat) & (nz > 0.3)).astype(float)

    f.ball(cx, cy, r * sx, r * sy, b, tex=cracks)
    f.halo(cx, cy, 24, EMBER[1], 0.35 + 0.25 * heartbeat + 0.4 * st.rage)
    # the crest of flame
    for i, dx in enumerate((-4.5, 0.0, 4.5)):
        h = (6.0 if i == 1 else 4.2) + 1.8 * st.s(2 + i, i * 0.3) + 5 * sneeze
        top = cy - r * sy + 1.5
        pts = [(cx + dx - 2.2, top), (cx + dx + 0.8 * st.s(3, i * 0.2), top - h), (cx + dx + 2.2, top)]
        from PIL import Image, ImageDraw
        im = Image.new('L', (S, S), 0)
        ImageDraw.Draw(im).polygon([(round(x), round(y)) for x, y in pts], fill=255)
        m = np.array(im) > 0
        f.blob(m, em[1], outline=True)
        f.blob(f.ellipse_mask(cx + dx, top - h * 0.3, 1.1, max(1.0, h * 0.28)) & m, em[3])
    # the eyes
    look = 0.0
    lid = 0.0
    if st.acting('A'):
        look = math.sin(st.u * 2 * math.pi * 2) * 1.0
        lid = 1.0 if 0.12 < st.u < 0.3 else (0.5 if 0.3 <= st.u < 0.36 or 0.06 < st.u <= 0.12 else 0.0)
    for side in (-1, 1):
        eye(f, cx + side * 6.2 * sx, cy + 4.0 * sy, 3.1, st, EYE, lid, look)
    if sneeze > 0.4 or (st.acting('B') and st.u > 0.5):
        sparks(f, cx, cy - r * sy, st.u, 10, 24, EMBER[3], 0.45)
    decorate(f, st, cx, cy - r * sy - 6, cy, r)
    return f


# ============================================================================================================================ JVALA, the Cinder
def jvala(st):
    f = Frame()
    a = st.ramp(ASH)
    em = st.ramp(EMBER)
    cx, cy = 32.0, 33.0 + 1.6 * st.s(1)
    yawn = st.env(0.15, 0.7) if st.acting('A') else 0.0
    r = 15.0
    sx, sy = 1.0 + 0.03 * st.s(2), 1.0 - 0.03 * st.s(2) + 0.05 * yawn
    # smoke rising off the top, always: two puffs a loop, out of step
    for i in range(2):
        k = (st.t + i * 0.5) % 1.0
        smoke_puff(f, cx + 3 * (i - 0.5) + 2 * st.s(1, i * 0.5), cy - r - 1, k, 2.4, ASH)
    # the cinders, orbiting: three, on a tilted ring; in clip C they swap their way round quickly, in clip B one is let fall
    n = 3
    cinders = []
    for i in range(n):
        spin = 1.0 if not st.acting('C') else 1.0 - 2.0 * st.env(0.1, 0.9)
        ang = 2 * math.pi * (st.t * spin + i / n) + (st.u * 6.28 * 2 if st.acting('C') else 0)
        ox, oy = cx + math.cos(ang) * (r + 6), cy + math.sin(ang) * 6.5 + 2
        cinders.append((oy, ox, i, ang))
    cinders.sort()
    behind = [c for c in cinders if math.sin(c[3]) < 0]
    front = [c for c in cinders if math.sin(c[3]) >= 0]
    for oy, ox, i, ang in behind:
        f.ball(ox, oy, 3.0, 3.0, em, outline=True)
        f.halo(ox, oy, 8, EMBER[2], 0.5)
    # the body: a round ash lantern, with an ember seam round it
    f.ball(cx, cy, r * sx, r * sy, a)
    f.halo(cx, cy, 22, EMBER[1], 0.18 + 0.12 * st.s(1) + 0.3 * st.rage)
    for yy_, ww in ((cy - 5.5, 7.0), (cy + 9.5, 6.0)):                        # two bands of dull ember, as on a kiln
        m = f.ellipse_mask(cx, yy_, ww * 1.5, 1.1) & f.ellipse_mask(cx, cy, r * sx - 0.5, r * sy - 0.5)
        f.blob(m, em[1])
    # the face: slit eyes of ember and a wide mouth of ember, that opens to yawn
    for side in (-1, 1):
        ex, ey = cx + side * 6.0, cy + 1.0
        m = f.ellipse_mask(ex, ey, 3.2, 1.7 - 0.5 * (1.0 if st.acting('B') and 0.3 < st.u < 0.4 else 0))
        f.blob(m, (30, 14, 12), outline=False)
        f.blob(f.ellipse_mask(ex, ey + 0.2, 2.2, 0.9) & m, EYE)
        f.halo(ex, ey, 7, EMBER[2], 0.6)
    mouth_h = 1.2 + 6.5 * yawn
    mm = f.ellipse_mask(cx, cy + 9.0 + 1.0 * yawn, 6.0 + 1.0 * yawn, mouth_h)
    f.blob(mm, (34, 12, 10))
    inner = f.ellipse_mask(cx, cy + 9.4 + 1.0 * yawn, 4.6 + yawn, max(0.6, mouth_h - 1.2))
    f.blob(inner, em[1 + (1 if st.s(2) > 0 else 0)])
    f.halo(cx, cy + 9, 9, EMBER[2], 0.5 + 0.9 * yawn)
    if yawn > 0.3:
        smoke_puff(f, cx, cy + 6, min(0.99, (st.u - 0.25) / 0.6), 3.5, ASH)
    if st.acting('B') and st.u > 0.3:                                          # one cinder let fall: it drops below, glowing, and gutters
        k = min(1.0, (st.u - 0.3) / 0.6)
        f.ball(cx + 6, cy + 14 + 14 * k, 3.0 * (1 - 0.4 * k), 3.0 * (1 - 0.4 * k), em, outline=True)
        f.halo(cx + 6, cy + 14 + 14 * k, 9, EMBER[2], 0.6 * (1 - k))
    for oy, ox, i, ang in front:
        f.ball(ox, oy, 3.4, 3.4, em, outline=True)
        f.halo(ox, oy, 9, EMBER[2], 0.6)
    decorate(f, st, cx, cy - r * sy - 6, cy, r)
    return f


# ============================================================================================================================ TAMRA, the Ore
def tamra(st):
    f = Frame()
    c = st.ramp(COPPER)
    steel = st.ramp(STEEL)
    cx, cy = 32.0, 34.0 + 0.7 * st.s(1)
    rock = 0.09 * st.s(1)                                                       # it rocks slowly from side to side
    stomp = st.env(0.2, 0.6) if st.acting('C') else 0.0
    vent = st.env(0.1, 0.8) if st.acting('A') else 0.0
    open_ = st.env(0.1, 0.9) if st.acting('B') else 0.0
    sx, sy = 1.0 + 0.1 * stomp, 1.0 - 0.12 * stomp
    r = 17.0
    # the two stub thrusters: on the back of the dome (the top of the sprite, as on a ship), their flames streaming up past it
    for side in (-1, 1):
        tx, ty = cx + side * 9.5, cy - r * 0.78
        fl = 3.5 + 2.4 * st.s(3, 0.3 * side) + 3.5 * stomp
        f.ball(tx, ty - fl * 0.8 - 1.5, 2.6, fl * 1.0, st.ramp(FIRE), outline=True)
        f.halo(tx, ty - 4, 9, EMBER[2], 0.55)
        f.ball(tx, ty + 1.0, 4.2, 3.4, steel)
    f.blob(f.ellipse_mask(cx, cy - r + 1.0, 2.2, 3.6), c[1], outline=True)               # a short ridge between them

    # steam, in clip A: white plumes from two side vents
    if vent > 0.05:
        for side in (-1, 1):
            for i in range(3):
                k = (st.u * 1.4 + i * 0.22) % 1.0
                f.blob(f.ellipse_mask(cx + side * (r + 2 + 5 * k), cy - 2 - 10 * k, 2 + 3 * k, 2 + 3 * k), R.lerp((240, 240, 244), (150, 150, 160), k))
    # the dome and its three plates
    ang = rock
    f.ball(cx, cy, r * sx, r * sy, c, rot=ang)
    plates = [(-8.5, 11.0), (-1.0, 14.0), (7.0, 12.5)]
    gap = 1.2 + 3.2 * open_
    for i, (py, hw) in enumerate(plates):
        yy_ = cy + py * sy + gap * (i - 1)
        m = f.ellipse_mask(cx, yy_, hw * sx + 3.5, 1.5) & f.ellipse_mask(cx, cy, r * sx + 0.5, r * sy + 0.5)
        f.blob(m, c[0], outline=False)
        f.blob(f.ellipse_mask(cx, yy_ - 0.6, hw * sx + 3.0, 0.8) & m, c[3])
        if open_ > 0.1:                                                          # the seam glows when the plates part
            f.blob(f.ellipse_mask(cx, yy_ + 2.0, hw * sx + 1.5, 0.9 * open_ + 0.3) & f.ellipse_mask(cx, cy, r * sx, r * sy), EMBER[3])
            f.halo(cx, yy_ + 2, 10, EMBER[2], 0.6 * open_)
        for rv in (-1, 1):                                                       # rivets
            f.spot(cx + rv * (hw * sx * 0.62), yy_ - 0.2, 0.9, c[3])
    for sd in (-1, 1):                                                           # armour studs down the flanks
        for k in range(3):
            f.spot(cx + sd * (r * sx - 3.0 - k * 0.6), cy - 6.0 + k * 6.0, 1.1, c[3])
            f.spot(cx + sd * (r * sx - 3.0 - k * 0.6) + 0.4, cy - 5.6 + k * 6.0, 0.5, c[0])
    # the visor: a slit that glows, with a pupil that tracks
    vis = f.ellipse_mask(cx, cy + 4.5 * sy, 9.0, 2.6 - 0.8 * (0.5 + 0.5 * st.s(1, 0.5) > 0.9))
    f.blob(vis, (26, 12, 10), outline=True)
    glow = 0.7 + 0.3 * st.s(1)
    f.blob(f.ellipse_mask(cx + 2.0 * st.s(1, 0.25), cy + 4.7 * sy, 3.2, 1.2) & vis, R.lerp(EYE, EYE_HOT, glow * 0.5 + 0.5 * st.rage))
    f.halo(cx, cy + 4.5, 12, EMBER[2], 0.55 * glow + 0.4 * st.rage)
    if stomp > 0.5:                                                              # the thud: a ring of dust under it
        d = np.abs(np.sqrt((PX - cx) ** 2 + ((PY - (cy + r)) * 2.2) ** 2) - (10 + 14 * st.u)) < 1.1
        f.blob(d & (PY > cy + r * 0.6), (150, 130, 120))
    decorate(f, st, cx, cy - r * sy - 5, cy, r)
    return f


# ============================================================================================================================ KILA, the Nail
def kila(st):
    f = Frame()
    g = st.ramp(GOLD)
    cx, cy = 32.0, 33.0 + 1.2 * st.s(1)
    scope = st.env(0.1, 0.85) if st.acting('A') else 0.0
    retract = st.env(0.15, 0.85) if st.acting('B') else 0.0
    twirl = st.u if st.acting('C') else 0.0
    r = 11.5
    # the long spike, forward (down): it trembles, and draws back in clip B
    L = 21.0 - 9.0 * retract
    tipx = cx + 0.6 * st.s(4)
    f.seg(cx, cy + r * 0.6, tipx, cy + r * 0.6 + L, 5.2, g[1])
    f.seg(cx - 0.8, cy + r * 0.6, tipx - 0.5, cy + r * 0.6 + L - 1, 1.6, g[3], outline=False)
    f.spot(tipx, cy + r * 0.6 + L, 1.6, g[3], outline=True)
    # the ring of short spikes round the body, quivering; in clip C they wheel through a full turn
    n = 8
    for i in range(n):
        a = 2 * math.pi * (i / n) + 2 * math.pi * twirl + 0.1 * st.s(2, i * 0.13) - math.pi / 2 + math.pi / n
        ln = (6.5 - 4.0 * retract) + 0.9 * st.s(3, i * 0.17)
        x0, y0 = cx + math.cos(a) * (r - 1.5), cy + math.sin(a) * (r - 1.5)
        x1, y1 = cx + math.cos(a) * (r + ln), cy + math.sin(a) * (r + ln)
        f.seg(x0, y0, x1, y1, 3.2, g[1])
        f.spot(x1, y1, 1.0, g[3])
    # the body
    f.ball(cx, cy, r, r, g)
    f.halo(cx, cy, 20, EMBER[2], 0.2 + 0.25 * st.rage)
    # the lens: one big eye, with an iris that narrows and a ring that opens when it takes aim
    f.spot(cx, cy + 1.5, 6.4, (30, 14, 10), outline=True)
    f.spot(cx, cy + 1.5, 5.2 + 0.9 * scope, R.lerp(EMBER[1], EMBER[2], 0.5 + 0.5 * st.s(1)))
    iris = 3.6 - 1.6 * scope
    f.spot(cx + 1.1 * st.s(1, 0.25) * (1 - scope), cy + 1.5, iris, (255, 236, 150))
    f.spot(cx + 1.1 * st.s(1, 0.25) * (1 - scope), cy + 1.5, max(0.8, iris - 1.6), (40, 14, 8))
    f.spot(cx - 1.8, cy - 0.2, 1.1, (255, 255, 255))
    if scope > 0.1:
        f.ring(cx, cy + 1.5, 8.0 + 6 * scope, 1.0, GOLD)
        f.halo(cx, cy + 1.5, 16, EMBER[3], 0.5 * scope)
    f.halo(cx, cy + 1.5, 10, EMBER[2], 0.7 + 0.4 * st.rage)
    decorate(f, st, cx, cy - r - 4 - 7 * 0, cy, r)
    return f


# ============================================================================================================================ DHUMA, the Smoke
def dhuma(st):
    f = Frame()
    gh = st.ramp(GHOST)
    cx, cy = 32.0, 30.0 + 1.5 * st.s(1)
    laugh = st.env(0.1, 0.9) if st.acting('A') else 0.0
    darting = st.acting('B')
    fade = st.env(0.15, 0.85) if st.acting('C') else 0.0
    r = 13.5
    shake = 1.6 * laugh * math.sin(st.u * 2 * math.pi * 7) if st.acting('A') else 0.0
    cx += shake
    # the hem of mist: a ragged tail trailing behind (above) and a few wisps either side
    for i in range(5):
        wx = cx + (i - 2) * 5.0 + 2.0 * st.s(1, i * 0.2)
        wy = cy - r - 3 - 4 * ((i % 2) * 0.7) + 1.5 * st.s(2, i * 0.3)
        rr = 4.0 - 0.5 * abs(i - 2)
        f.blob(f.ellipse_mask(wx, wy, rr, rr * 1.4), R.lerp(gh[1], gh[0], 0.4 + 0.1 * i % 3), outline=False)
    for side in (-1, 1):
        for j in range(2):
            wx = cx + side * (r + 2 + 2 * j) + 1.5 * st.s(1, side * 0.25 + j * 0.3)
            wy = cy + 4 - 4 * j
            f.blob(f.ellipse_mask(wx, wy, 2.2, 2.8), gh[1], outline=False)
    # the skull: a round head, big sockets with blue fire, a many-toothed grin whose jaw drops
    f.ball(cx, cy, r, r * 0.96, gh)
    if st.variant != 1:
        f.ring(cx, cy - r + 1.0, 7.0, 1.4, GOLD, arc=(math.pi * 1.1, math.pi * 1.9))
    lookx = (math.sin(st.u * 2 * math.pi * 3) * 1.4) if darting else 0.0
    looky = (math.cos(st.u * 2 * math.pi * 2) * 1.0) if darting else 0.0
    for side in (-1, 1):
        ex, ey = cx + side * 5.2, cy + 0.5
        f.spot(ex, ey, 4.2, (24, 30, 60), outline=True)
        f.spot(ex + lookx, ey + looky + 0.3, 2.6 + 0.4 * st.s(2, side * 0.2), (96, 170, 255) if st.rage < 0.5 else (255, 190, 120))
        f.spot(ex + lookx - 0.6, ey + looky - 0.4, 1.0, (235, 250, 255))
        f.halo(ex, ey, 9, (96, 170, 255) if st.rage < 0.5 else EMBER[2], 0.7)
    nose = f.ellipse_mask(cx, cy + 4.5, 1.2, 1.8)
    f.blob(nose, (24, 30, 60))
    jaw = 1.0 + 3.8 * laugh + 0.7 * st.s(1)
    mouth = f.ellipse_mask(cx, cy + 9.0 + jaw * 0.35, 7.0, 1.2 + jaw * 0.5) & f.ellipse_mask(cx, cy, r, r * 0.96)
    f.blob(mouth, (24, 30, 60))
    for i in range(-3, 4):                                                       # the teeth
        tx = cx + i * 2.0
        f.blob(f.ellipse_mask(tx, cy + 8.2, 0.8, 1.4) & mouth, (245, 250, 255))
    if fade > 0:                                                                  # clip C: it thins to mist and comes back
        keep = (((XX_ := np.mgrid[0:S, 0:S])[0] * 3 + XX_[1] * 5) % 7) / 7.0 > fade * 0.85
        f.a = np.where(keep, f.a, f.a * 0.25)
    decorate(f, st, cx, cy - r - 3, cy, r)
    return f


# ============================================================================================================================ REKHA, the Line
def rekha(st):
    f = Frame()
    rb = st.ramp(ROBE)
    sk = st.ramp(SKIN)
    cx, cy = 32.0, 33.0 + 1.3 * st.s(1)
    draw_ = st.env(0.05, 0.9) if st.acting('A') else 0.0
    wave = st.env(0.05, 0.95) if st.acting('B') else 0.0
    peek = st.env(0.15, 0.85) if st.acting('C') else 0.0
    r = 12.5
    # the hem: a round skirt of robe with a torn edge of mist
    f.ball(cx, cy + 10.0, 15.0, 9.0, rb)
    for i in range(6):
        wx = cx + (i - 2.5) * 5.2
        f.blob(f.ellipse_mask(wx, cy + 18.0 + 1.6 * st.s(1, i * 0.18), 2.6, 2.2 + 0.6 * st.s(2, i * 0.3)), R.lerp(rb[1], rb[0], 0.3), outline=True)
    # the arms: wide, in long round sleeves, the right one drawing a line in the air in clip A, both waving in clip B
    for side in (-1, 1):
        lift = (-8.0 * wave * (1 if side > 0 else -1) * math.sin(st.u * 2 * math.pi * 3)) if st.acting('B') else 0.0
        ax = cx + side * 15.5
        ay = cy + 3 + 1.5 * st.s(1, 0.25 * side) + lift
        if side > 0 and draw_ > 0:
            ax += 4.0 * draw_
            ay -= 2.0 * draw_
        f.seg(cx + side * 8, cy + 4, ax, ay, 6.0, rb[1])
        f.ball(ax, ay, 4.4, 4.4, rb)
        f.ring(ax - side * 1.0, ay + 1.5, 3.4, 1.3, GOLD)
        f.spot(ax + side * 3.2, ay + 0.6, 1.5, sk[2], outline=True)
        if side > 0 and draw_ > 0.05:                                             # the line: a thread of gold drawn from the fingertip, standing, then thinning
            k = st.u
            x0 = ax + 3.2
            y0 = ay + 0.6
            x1 = x0 + 6.0 * min(1.0, k * 2.0)
            f.seg(x0, y0, x0 + 3, y0 + 18 * min(1.0, k * 2.0), 1.6, GOLD[3], outline=False, glow=0.7)
    # the hood: a round head, the face a dark oval, a wide white grin, eyes of blue fire
    tilt = 0.15 * peek * math.sin(st.u * math.pi)
    f.ball(cx, cy - 2.0, r, r * 1.02, rb, rot=tilt)
    face = f.ellipse_mask(cx + 2 * tilt * 10, cy + 0.5, 8.0, 7.0)
    f.blob(face, (22, 20, 34), outline=True)
    flare = 1.0 + 1.0 * (1 if st.acting('C') and 0.4 < st.u < 0.6 else 0)
    for side in (-1, 1):
        ex = cx + side * 3.6 + 2 * tilt * 10
        big = flare if (side > 0 or not peek) else 1.0
        f.spot(ex, cy - 0.5, 1.9 * big, (110, 190, 255) if st.rage < 0.5 else (255, 190, 120))
        f.spot(ex, cy - 0.5, 0.8, (240, 252, 255))
        f.halo(ex, cy - 0.5, 8 * big, (110, 190, 255) if st.rage < 0.5 else EMBER[2], 0.7)
    gm = f.ellipse_mask(cx + 2 * tilt * 10, cy + 3.8, 5.0, 1.5 + 0.4 * st.s(2)) & face
    f.blob(gm, (245, 250, 255))
    f.blob(f.ellipse_mask(cx + 2 * tilt * 10, cy + 3.8, 5.0, 0.4) & gm, (30, 28, 44))
    decorate(f, st, cx, cy - r - 4, cy, r)
    return f


# ============================================================================================================================ DVARAPALA, the gate-ward
def dvarapala(st):
    f = Frame()
    s_ = st.ramp(STEEL)
    g = st.ramp(GOLD)
    cx, cy = 32.0, 33.0 + 0.8 * st.s(1)
    tap = st.env(0.1, 0.5) if st.acting('A') else 0.0
    look = math.sin(st.u * 2 * math.pi * 1.5) if st.acting('B') else 0.0
    thump = st.env(0.2, 0.6) if st.acting('C') else 0.0
    r = 19.0
    sx, sy = 1.0 + 0.09 * thump, 1.0 - 0.1 * thump
    # the fists: two round gauntlets either side
    for side in (-1, 1):
        f.ball(cx + side * (r + 1.5), cy + 6 + 1.2 * st.s(1, 0.3 * side), 5.0, 5.0, s_)
        f.ring(cx + side * (r + 1.5), cy + 6, 5.0, 1.2, GOLD, arc=(0.2, 3.0))
    # the body: a heavy round of steel
    f.ball(cx, cy, r * sx, r * sy, s_)
    # the helm band: a gold ring across the brow with a visor slit that looks aside in clip B
    band = f.ellipse_mask(cx, cy - 3.0, r * sx - 0.5, 4.0 * sy) & f.ellipse_mask(cx, cy, r * sx - 0.3, r * sy - 0.3)
    f.blob(band, g[1])
    f.blob(f.ellipse_mask(cx, cy - 4.4, r * sx - 2.5, 1.0) & band, g[3])
    vis = f.ellipse_mask(cx + 3.0 * look, cy - 3.0, 8.0, 2.0)
    f.blob(vis, (24, 12, 10))
    f.blob(f.ellipse_mask(cx + 3.0 * look + 1.8 * st.s(1, 0.2), cy - 3.0, 3.0, 1.1) & vis, EYE)
    f.halo(cx + 3.0 * look, cy - 3.0, 12, EMBER[2], 0.6 + 0.4 * st.rage)
    # the shield: a round disc in front, with a boss of gold that catches the light and rings when tapped
    shx, shy = cx + 1.0 * st.s(1, 0.5), cy + 9.0 + 3.0 * tap
    f.ball(shx, shy, 13.0, 9.0, s_)
    f.ring(shx, shy, 11.2, 1.4, GOLD)
    f.ball(shx, shy, 4.0, 3.2, g)
    if tap > 0.3:
        f.ring(shx, shy, 6 + 10 * (1 - tap), 1.0, GOLD)
        f.halo(shx, shy, 16, EMBER[3], 0.5 * tap)
    glint = (st.t * 2) % 1.0
    f.spot(shx - 7 + 14 * glint, shy - 4 + 2 * math.sin(glint * 6), 1.0, (255, 255, 255))
    if thump > 0.5:
        d = np.abs(np.sqrt((PX - cx) ** 2 + ((PY - (cy + r)) * 2.2) ** 2) - (10 + 16 * st.u)) < 1.1
        f.blob(d & (PY > cy + r * 0.55), (150, 130, 120))
    decorate(f, st, cx, cy - r * sy - 3, cy, r)
    return f


# ============================================================================================================================ DIPAKA, the lamp-keeper
def dipaka(st):
    f = Frame()
    gl = st.ramp(GHOST)
    g = st.ramp(GOLD)
    cx, cy = 32.0, 33.0 + 1.4 * st.s(1)
    tend = st.env(0.1, 0.9) if st.acting('A') else 0.0
    sneeze = st.env(0.25, 0.55) if st.acting('B') else 0.0
    pour = st.env(0.1, 0.9) if st.acting('C') else 0.0
    r = 13.5
    tilt = 0.4 * pour * math.sin(st.u * math.pi)
    # the little feet and hands
    for side in (-1, 1):
        f.ball(cx + side * 6, cy + r + 1.5 + 0.8 * st.s(2, 0.5 * side), 3.4, 2.4, g)
        hx = cx + side * (r + 3.0)
        hy = cy + 3.0 + 1.2 * st.s(1, 0.25 * side)
        if side > 0 and tend > 0:
            hx -= 6 * tend
            hy -= 10 * tend
        f.ball(hx, hy, 3.2, 3.2, g)
    # the ring handle on top
    f.ring(cx, cy - r - 2.0, 3.6, 1.6, GOLD)
    # the lantern: a glass sphere, a gold cap and foot, the flame dancing inside
    f.ball(cx, cy, r, r, gl, rot=tilt, tint=(255, 220, 160, 0.30))
    f.blob(f.ellipse_mask(cx, cy - r + 1.0, 5.0, 2.0), g[1], outline=True)
    f.blob(f.ellipse_mask(cx, cy + r - 1.0, 5.0, 1.8), g[1], outline=True)
    # the flame: a teardrop that leans and flickers; it jumps when the keeper sneezes
    fl = 8.0 + 2.4 * st.s(3) + 6.0 * sneeze
    lean = 1.8 * st.s(2) + 3.0 * tilt * 10
    from PIL import Image, ImageDraw
    im = Image.new('L', (S, S), 0)
    ImageDraw.Draw(im).polygon([(round(cx - 3.4), round(cy + 5)), (round(cx + lean), round(cy + 5 - fl * 1.6)), (round(cx + 3.4), round(cy + 5))], fill=255)
    m = np.array(im) > 0
    m |= f.ellipse_mask(cx, cy + 4.0, 3.8, 3.2)
    f.blob(m, st.ramp(FIRE)[1])
    f.blob(f.ellipse_mask(cx + lean * 0.3, cy + 3.5 - fl * 0.2, 1.8, max(1.2, fl * 0.35)), st.ramp(FIRE)[3])
    f.halo(cx, cy, 22, EMBER[2], 0.7 + 0.3 * st.s(3) + 0.4 * sneeze + 0.3 * st.rage)
    # the face: two small round eyes and a little round mouth at the glass's foot
    for side in (-1, 1):
        f.spot(cx + side * 4.6, cy + 7.6, 1.6, (40, 22, 30))
        f.spot(cx + side * 4.6 - 0.4, cy + 7.2, 0.6, (255, 255, 255))
    f.spot(cx, cy + 10.2, 1.0 + 0.8 * sneeze, (60, 24, 30))
    if sneeze > 0.3 or (st.acting('B') and st.u > 0.5):
        sparks(f, cx, cy - 3, st.u, 8, 20, EMBER[3], 0.4)
    decorate(f, st, cx, cy - r - 5, cy, r)
    return f


# ============================================================================================================================ VANIJA, the bay-master
def vanija(st):
    f = Frame()
    sk = st.ramp(SKIN)
    g = st.ramp(GOLD)
    rb = st.ramp(ROBE)
    cx, cy = 32.0, 34.0 + 1.0 * st.s(1)
    count = st.env(0.05, 0.95) if st.acting('A') else 0.0
    polish = st.env(0.1, 0.9) if st.acting('B') else 0.0
    nod = st.env(0.15, 0.85) if st.acting('C') else 0.0
    r = 16.5
    # the coins: five, orbiting at a distance; in clip A they flip, one by one
    for i in range(5):
        a = 2 * math.pi * (st.t + i / 5.0)
        ox, oy = cx + math.cos(a) * (r + 6.5), cy + math.sin(a) * 7.0 + 3
        flip = abs(math.cos(a * 2 + (st.u * 8 if st.acting('A') else 0)))
        if math.sin(a) < 0:
            f.ball(ox, oy, 3.0 * max(0.35, flip), 3.0, g, outline=True)
    # the body: plump and round, in robe, with a gold sash across the belly
    f.ball(cx, cy + 3.0, r, r * 0.95, rb)
    sash = f.ellipse_mask(cx, cy + 6.5, r - 1.0, 3.2) & f.ellipse_mask(cx, cy + 3.0, r - 0.4, r * 0.95 - 0.4)
    f.blob(sash, g[1])
    f.blob(f.ellipse_mask(cx, cy + 5.5, r - 3.0, 1.0) & sash, g[3])
    f.ball(cx + 7.0, cy + 9.0, 4.2, 4.2, g)                                      # the purse on the sash
    f.spot(cx + 7.0, cy + 6.0, 1.4, g[0])
    # the arms: short round sleeves; one polishes a scale (a little balance) in clip B
    for side in (-1, 1):
        ax, ay = cx + side * (r + 0.5), cy + 4.0 + 1.0 * st.s(1, 0.25 * side)
        if side < 0 and polish > 0:
            ax += 5.0 * polish
            ay -= 3.0 * polish
        f.ball(ax, ay, 4.6, 4.6, rb)
        f.spot(ax + side * 2.4, ay + 2.2, 1.8, sk[2], outline=True)
    if polish > 0.1:
        sx_, sy_ = cx - 10 + 5 * polish, cy + 2 - 3 * polish
        f.seg(sx_ - 4, sy_ + 2 + 1.2 * math.sin(st.u * 20), sx_ + 4, sy_ + 2 - 1.2 * math.sin(st.u * 20), 1.0, g[3])
        f.spot(sx_ - 4, sy_ + 4, 1.4, g[2])
        f.spot(sx_ + 4, sy_ + 4, 1.4, g[2])
    # the head: a round face, a round turban of gold, a smile; it nods in clip C
    hy = cy - 10.5 + 2.2 * nod * math.sin(st.u * 2 * math.pi * 2)
    f.ball(cx, hy, 9.5, 9.0, sk)
    f.ball(cx, hy - 5.0, 10.0, 6.5, g)
    f.ring(cx, hy - 3.0, 9.0, 1.2, GOLD, arc=(math.pi * 0.05, math.pi * 0.95))
    f.spot(cx, hy - 8.5, 1.4, (200, 40, 40), outline=True)
    for side in (-1, 1):
        blink = 1.0 if (st.t * 4) % 1.0 > 0.93 else 0.0
        f.spot(cx + side * 3.4, hy + 0.8, 1.4 - 0.9 * blink, (30, 18, 16))
    f.blob(f.ellipse_mask(cx, hy + 4.8, 3.6, 1.2 + 0.5 * st.s(1)) & f.ellipse_mask(cx, hy, 9.5, 9.0), (150, 50, 50))
    for i in range(5):
        a = 2 * math.pi * (st.t + i / 5.0)
        if math.sin(a) >= 0:
            ox, oy = cx + math.cos(a) * (r + 6.5), cy + math.sin(a) * 7.0 + 3
            flip = abs(math.cos(a * 2 + (st.u * 8 if st.acting('A') else 0)))
            f.ball(ox, oy, 3.4 * max(0.35, flip), 3.4, g, outline=True)
    decorate(f, st, cx, hy - 11, cy, r)
    return f


CREATURES = {'vakra': agneya, 'mayavi': jvala, 'bhavi': tamra, 'lance': kila, 'auris': dhuma, 'hataka': rekha, 'dvarapala': dvarapala, 'dipaka': dipaka, 'vanija': vanija}


def preview(name, sheet):
    """A contact sheet at 3x: the idle loop, the rage loop, a leader, an elite, the hit and death frames, and each action clip."""
    from PIL import Image
    rows = [(0, 8, 0), (0, 8, 8), (2, 8, 0), (3, 8, 0), (1, 8, 0), (4, 16, 0), (5, 16, 0), (6, 16, 0)]
    W = 16 * S
    img = Image.new('RGBA', (W, len(rows) * S), (46, 34, 40, 255))
    for ri, (row, n, c0) in enumerate(rows):
        for i in range(n):
            cell = sheet.crop(((c0 + i) * S, row * S, (c0 + i + 1) * S, (row + 1) * S))
            img.alpha_composite(cell, (i * S, ri * S))
    img = img.resize((img.width * 2, img.height * 2), Image.NEAREST)
    os.makedirs('build', exist_ok=True)
    img.save('build/vitala_%s_preview.png' % name)


def big(name, sheet):
    """One creature large (6x): idle, rage, leader, elite, and the middle of each action clip."""
    from PIL import Image
    cells = [(0, 0), (0, 2), (0, 8), (2, 0), (3, 0), (1, 0), (4, 6), (5, 10), (6, 7)]
    img = Image.new('RGBA', (len(cells) * S, S), (46, 34, 40, 255))
    for i, (row, col) in enumerate(cells):
        img.alpha_composite(sheet.crop((col * S, row * S, (col + 1) * S, (row + 1) * S)), (i * S, 0))
    img = img.resize((img.width * 3, img.height * 3), Image.NEAREST)
    img.save('build/vitala_%s_big.png' % name)


if __name__ == '__main__':
    only = sys.argv[1:] or list(CREATURES)
    os.makedirs(OUT, exist_ok=True)
    for k in only:
        sh = R.build_sheet(CREATURES[k])
        sh.save(os.path.join(OUT, k + '.png'))
        preview(k, sh)
        big(k, sh)
        print('wrote', k)

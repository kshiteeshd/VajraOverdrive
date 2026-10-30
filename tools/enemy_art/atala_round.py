"""THE ROUND CREATURES OF ATALA: six of them (the lands of illusion and wind, sunless and jewel-lit), each drawn from spheres (roundkit.py), each with an idle loop and three action clips
that it does now and then. The keepers (gate-ward, lamp-keeper, bay-master) are Vitala's round figures turned to the jewel palette.
Run:  python tools/enemy_art/atala_round.py [kind ...]      -> assets/enemies/atala/<kind>.png (and build/atala_<kind>_preview.png)

 mayavi  the Mirage   a glass bubble with a flickering face, two ghost copies of itself drifting about it; it splits in three and draws itself together again
 vakra   the Gale     a round puff of pearl feathers with two whirling wing-fans and wind-lines about it; it spins up a gust, puffs its cheeks, loops
 bhavi   the Dream    a drowsy jade pearl in a collar of lotus petals, half-lidded eyes; it yawns, blows a sleep bubble, nods off and starts awake
 lance   the Shard    a faceted amethyst orb with one long prism spike and three orbiting splinters; it refracts a rainbow, spins its splinters, glints
 auris   the Whisper  a round owl-ghost: huge ear-tufts, a ringed pair of eyes, a pale hem of mist; it tilts its head, hoots a ring of sound, blinks slowly
 hataka  the Veil     a round silk lantern-jelly with long ribbons trailing; the ribbons twirl, the lantern pulses, it waves
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import roundkit as R
from roundkit import GOLD, S, Frame, PX, PY
import vitala_round as V

OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'enemies', 'atala')
GLASS = [(52, 70, 120), (104, 146, 206), (186, 218, 250), (250, 255, 255)]
JADE = [(16, 66, 58), (32, 122, 102), (86, 196, 160), (190, 250, 226)]
AMETHYST = [(54, 32, 94), (104, 68, 166), (160, 122, 222), (226, 200, 255)]
PEARL = [(108, 102, 130), (174, 168, 196), (226, 222, 240), (255, 252, 255)]
SAPPHIRE = [(18, 38, 100), (38, 82, 190), (88, 148, 240), (184, 224, 255)]
ROSE = [(120, 38, 78), (190, 80, 126), (240, 140, 176), (255, 208, 226)]
AQUA = [(20, 90, 110), (50, 160, 180), (120, 220, 226), (224, 255, 252)]
EYE = (255, 244, 190)
SOCKET = (22, 16, 40)


def decorate(f, st, cx, top, cy, r):
    """A leader wears a small fan of gems; an elite a broken halo of gold."""
    if st.variant == 1:
        for i, dx in enumerate((-5, 0, 5)):
            h = 4.5 + (2.0 if i == 1 else 0.0) + 0.8 * st.s(2, i * 0.3)
            f.ball(cx + dx, top - h * 0.5 + 1.5, 2.1, h * 0.55 + 1.2, AQUA, outline=True)
        f.halo(cx, top - 3, 10, AQUA[2], 0.45)
    elif st.variant == 2:
        R.elite_halo(f, cx, cy, r, st)


def eye(f, x, y, r, st, lid=0.0, look=0.0):
    f.spot(x, y, r + 0.9, SOCKET)
    c = R.lerp(EYE, (255, 250, 230), 0.5 * st.rage)
    f.spot(x, y, r, c)
    f.spot(x + look * 0.9, y + 0.2, max(0.9, r * 0.5), (36, 24, 70))
    f.spot(x + look * 0.9 - 0.7, y - 0.6, max(0.6, r * 0.22), (255, 255, 255))
    if lid > 0:
        m = (PX - x) ** 2 + (PY - y) ** 2 <= (r + 0.9) ** 2
        cut = m & (PY < y - r + 2 * (r + 0.9) * lid)
        f.rgb[cut] = (84, 70, 120)
        f.a[cut] = 1.0


def sparkle(f, x, y, size, k):
    """A four-point glint, k 0..1 (it opens and closes)."""
    if k <= 0:
        return
    s = size * math.sin(math.pi * min(1.0, k))
    f.seg(x - s, y, x + s, y, 1.0, (255, 255, 255), outline=False)
    f.seg(x, y - s, x, y + s, 1.0, (255, 255, 255), outline=False)
    f.halo(x, y, size * 2.2, (230, 240, 255), 0.5 * math.sin(math.pi * min(1.0, k)))


def ring_wave(f, cx, cy, r0, k, color=(220, 236, 255), w=1.4):
    if 0 < k < 1:
        f.ring(cx, cy, r0 + 16 * k, w, [color] * 4, outline=False)


# ============================================================================================================================ MAYAVI, the Mirage
def mayavi(st):
    f = Frame()
    g = st.ramp(GLASS)
    cx, cy = 32.0, 33.0 + 1.3 * st.s(1)
    split = st.env(0.1, 0.9) if st.acting('A') else 0.0
    wink = st.env(0.2, 0.7) if st.acting('B') else 0.0
    shimmer = st.u if st.acting('C') else 0.0
    r = 12.5
    # the two ghosts: pale copies that drift about it (in clip A they pull wide and the three trade places)
    for i, side in enumerate((-1, 1)):
        ang = 2 * math.pi * (0.5 * st.t * side + i * 0.5)
        d = 9.0 + 12.0 * split
        gx, gy = cx + math.cos(ang) * d, cy + 2 + math.sin(ang) * d * 0.4
        f.ball(gx, gy, r * 0.62, r * 0.62, [R.lerp(c, (200, 220, 255), 0.4) for c in g], outline=False)
        f.halo(gx, gy, 14, g[2], 0.35)
        f.spot(gx - 2, gy + 0.5, 1.1, SOCKET)
        f.spot(gx + 2, gy + 0.5, 1.1, SOCKET)
    f.ball(cx, cy, r, r, g)
    f.halo(cx, cy, 24, g[2], 0.3 + 0.2 * st.s(1) + 0.3 * st.rage)
    # the glass: a bright crescent on the upper left and a small one on the lower right
    f.blob(f.ellipse_mask(cx - 5.0, cy - 5.5, 3.6, 1.6) & f.ellipse_mask(cx, cy, r - 1, r - 1), (255, 255, 255))
    f.blob(f.ellipse_mask(cx + 6.0, cy + 6.0, 2.0, 0.9) & f.ellipse_mask(cx, cy, r - 1, r - 1), g[3])
    # the face flickers: the eyes slide, one winks in clip B, and in clip C a ripple of colour runs over it
    for side in (-1, 1):
        lid = 1.0 if (side > 0 and wink > 0.4) else 0.0
        eye(f, cx + side * 4.4, cy + 0.5, 2.6, st, lid, look=1.2 * st.s(2, 0.1))
    f.blob(f.ellipse_mask(cx, cy + 6.2, 3.2, 1.0 + 0.6 * wink), (46, 30, 84))
    if shimmer > 0:
        band = np.abs((PX + PY) / 1.414 - (cx + cy) / 1.414 - (shimmer * 2 - 1) * 26) < 3.0
        m = band & f.ellipse_mask(cx, cy, r, r)
        f.rgb[m] = (np.array(f.rgb[m]) * 0.4 + np.array([255, 190, 230]) * 0.6)
        sparkle(f, cx + (shimmer * 2 - 1) * 8, cy - 4, 4.0, (shimmer * 3) % 1.0)
    decorate(f, st, cx, cy - r - 2, cy, r)
    return f


# ============================================================================================================================ VAKRA, the Gale
def vakra(st):
    f = Frame()
    p = st.ramp(PEARL)
    cx, cy = 32.0, 34.0 + 1.1 * st.s(1)
    gust = st.env(0.05, 0.95) if st.acting('A') else 0.0
    puff = st.env(0.15, 0.7) if st.acting('B') else 0.0
    loop = st.u if st.acting('C') else 0.0
    r = 14.5 + 2.4 * puff
    sx, sy = 1.0 + 0.1 * puff, 1.0 - 0.04 * puff
    # wind-lines streaming past it, forward to back
    for i in range(3):
        k = (st.t * 2 + i / 3.0 + gust * st.u * 3) % 1.0
        x = cx + (i - 1) * 11
        f.seg(x, cy + 8 + 22 * k - 14, x + 0.8 * (i - 1), cy + 8 + 22 * k - 4, 1.0, R.lerp(AQUA[2], (255, 255, 255), 0.4), outline=False)
    # the wing-fans: a hub with four blades each, whirling (fast in clip A)
    for side in (-1, 1):
        wx, wy = cx + side * (r * sx + 5.0), cy - 2.0 + 1.0 * st.s(2, 0.3 * side)
        spin = 2 * math.pi * (st.t * (1 if side > 0 else -1) * 2 + gust * st.u * 8)
        for b in range(4):
            a = spin + b * math.pi / 2
            f.seg(wx, wy, wx + math.cos(a) * 6.5, wy + math.sin(a) * 6.5 * 0.7, 3.0, AQUA[1])
            f.spot(wx + math.cos(a) * 6.5, wy + math.sin(a) * 4.5, 1.3, AQUA[3])
        f.spot(wx, wy, 2.0, GOLD[2], outline=True)
        f.halo(wx, wy, 12, AQUA[2], 0.35 + 0.3 * gust)
    # the puffball: feathers, as scalloped bands
    f.ball(cx, cy, r * sx, r * sy, p)
    for k, yy in enumerate((-6.0, -1.0, 4.5)):
        for j in range(-2, 3):
            f.ball(cx + j * 5.0 * sx, cy + yy + 0.8 * abs(j), 3.0, 2.3, [R.lerp(c, AQUA[1], 0.18) for c in p], outline=False)
    f.halo(cx, cy, 24, AQUA[2], 0.22 + 0.25 * gust + 0.25 * st.rage)
    # the face: round eyes, a beak of gold that opens a little when it puffs
    for side in (-1, 1):
        eye(f, cx + side * 5.4 * sx, cy + 1.5, 2.9, st, 1.0 if (st.t * 3) % 1.0 > 0.94 else 0.0, look=-1.0 * loop)
    f.ball(cx, cy + 7.0 + 0.6 * puff, 2.6, 2.2 + 1.0 * puff, GOLD)
    # clip C: a loop-the-loop is hinted by a curved streak of wind round the body
    if loop > 0:
        f.ring(cx, cy, r + 5, 1.4, [AQUA[2]] * 4, arc=(2 * math.pi * loop, 2 * math.pi * loop + 3.0), outline=False)
    decorate(f, st, cx, cy - r * sy - 1, cy, r)
    return f


# ============================================================================================================================ BHAVI, the Dream
def bhavi(st):
    f = Frame()
    j = st.ramp(JADE)
    rs = st.ramp(ROSE)
    cx, cy = 32.0, 35.0 + 1.0 * st.s(1)
    yawn = st.env(0.1, 0.8) if st.acting('A') else 0.0
    bubble = st.env(0.0, 1.0) if st.acting('B') else 0.0
    nod = st.env(0.1, 0.9) if st.acting('C') else 0.0
    r = 14.5
    tilt = 0.1 * st.s(1) + 0.35 * nod * math.sin(st.u * math.pi * 1.5)
    # the lotus collar: nine petals round the lower half, breathing
    for i in range(9):
        a = math.pi * (0.05 + 0.9 * i / 8.0)
        l = 6.5 + 0.8 * st.s(1, i * 0.05) + 2.0 * yawn
        x0, y0 = cx + math.cos(a) * (r - 2), cy + math.sin(a) * (r - 4) + 1
        x1, y1 = cx + math.cos(a) * (r + l), cy + math.sin(a) * (r + l * 0.7) + 1
        f.seg(x0, y0, x1, y1, 4.0, rs[1])
        f.spot(x1, y1, 1.8, rs[3])
    f.ball(cx, cy, r, r, j, rot=tilt)
    f.halo(cx, cy, 24, JADE[2], 0.22 + 0.2 * st.rage)
    # the forehead jewel
    f.ball(cx + 0.5 * math.sin(tilt) * 8, cy - 7.5, 2.2, 2.6, ROSE, outline=True)
    f.halo(cx, cy - 7.5, 8, ROSE[3], 0.5)
    # half-lidded eyes; the lids lift on the yawn and drop in the nod; the mouth opens round
    lid = 0.55 - 0.45 * yawn + 0.45 * nod * (0.5 + 0.5 * math.sin(st.u * math.pi * 3))
    for side in (-1, 1):
        eye(f, cx + side * 5.6 + 2.0 * math.sin(tilt) * 4, cy - 0.5, 2.8, st, min(0.95, lid), look=0.6 * st.s(1))
    f.ball(cx, cy + 6.5, 2.4 + 1.6 * yawn, 1.4 + 3.2 * yawn, [(30, 10, 30), (60, 16, 50), (110, 30, 70), (150, 50, 90)], outline=False)
    # clip B: a sleep bubble grows from the nostril, wobbles, and pops in a spray
    if bubble > 0.02:
        k = st.u
        if k < 0.85:
            br = 1.2 + 7.5 * min(1.0, k / 0.8)
            bx, by = cx + 9 + 1.2 * math.sin(st.u * 20), cy + 4 + 4 * k
            f.ring(bx, by, br, 1.2, [AQUA[2]] * 4, outline=True)
            f.blob(f.ellipse_mask(bx - br * 0.35, by - br * 0.35, br * 0.25, br * 0.18), (255, 255, 255))
            f.halo(bx, by, br * 2, AQUA[2], 0.3)
        else:
            u2 = (k - 0.85) / 0.15
            for i in range(6):
                a = i * math.pi / 3
                f.spot(cx + 10 + math.cos(a) * 9 * u2, cy + 7 + math.sin(a) * 9 * u2, 1.2 * (1 - u2), AQUA[3])
    decorate(f, st, cx, cy - r - 2, cy, r)
    return f


# ============================================================================================================================ LANCE, the Shard
def lance(st):
    f = Frame()
    a = st.ramp(AMETHYST)
    cx, cy = 32.0, 32.0 + 1.1 * st.s(1)
    rainbow = st.env(0.1, 0.85) if st.acting('A') else 0.0
    spin = st.u if st.acting('B') else 0.0
    glint = st.u if st.acting('C') else 0.0
    r = 11.5
    # the prism spike, forward (down): a long gold-lit crystal
    L = 20.0
    f.seg(cx, cy + r * 0.6, cx, cy + r * 0.6 + L, 5.4, a[1])
    f.seg(cx - 1.0, cy + r * 0.6, cx - 0.6, cy + r * 0.6 + L - 1, 1.8, a[3], outline=False)
    f.spot(cx, cy + r * 0.6 + L, 1.6, a[3], outline=True)
    if rainbow > 0:                                                               # clip A: the tip throws a fan of coloured light
        cols = [(255, 90, 110), (255, 190, 80), (140, 240, 120), (90, 190, 255), (180, 120, 255)]
        for i, c in enumerate(cols):
            ang = math.pi / 2 + (i - 2) * 0.28
            ln = 14 * rainbow
            f.seg(cx, cy + r * 0.6 + L, cx + math.cos(ang) * ln, cy + r * 0.6 + L + math.sin(ang) * ln, 1.6, c, outline=False, glow=0.35)
    # the splinters: three small crystals orbiting (they race through a full turn in clip B)
    for i in range(3):
        ang = 2 * math.pi * (st.t + i / 3.0 + 2 * spin)
        ox, oy = cx + math.cos(ang) * (r + 6), cy + math.sin(ang) * 6.0 + 1
        f.ball(ox, oy, 2.2, 3.4, AMETHYST, rot=ang * 0.5, outline=True)
        f.halo(ox, oy, 8, AMETHYST[3], 0.4)
    # the body: an orb, cut with facets (lighter planes sliding over it as it turns)
    def facets(nx, ny, nz):
        v = np.abs(np.sin(3.2 * nx + 2.0 * ny + 2 * math.pi * st.t * 0.5)) * np.abs(np.sin(2.6 * ny - 2.4 * nx))
        return ((v < 0.04) & (nz > 0.25)).astype(float) * 0.0
    f.ball(cx, cy, r, r, a)
    for k in range(4):                                                             # facet lines
        ang = math.pi * (k / 4.0) + 2 * math.pi * st.t * 0.25
        x0, y0 = cx + math.cos(ang) * (r - 1), cy + math.sin(ang) * (r - 1)
        f.seg(x0, y0, 2 * cx - x0, 2 * cy - y0, 0.9, a[3], outline=False)
    f.halo(cx, cy, 22, AMETHYST[2], 0.3 + 0.3 * st.rage + 0.3 * rainbow)
    # one eye, a round lens
    f.spot(cx, cy + 1.0, 5.4, SOCKET, outline=True)
    f.spot(cx, cy + 1.0, 4.4, R.lerp(AQUA[1], AQUA[2], 0.5 + 0.5 * st.s(1)))
    f.spot(cx + 0.9 * st.s(1, 0.25), cy + 1.0, 2.8, EYE)
    f.spot(cx + 0.9 * st.s(1, 0.25), cy + 1.0, 1.4, (30, 20, 60))
    f.spot(cx - 1.6, cy - 0.6, 1.0, (255, 255, 255))
    if glint > 0:
        sparkle(f, cx - 5, cy - 6, 5.0, (glint * 1.4) % 1.0)
        sparkle(f, cx + 6, cy + 4, 3.5, (glint * 1.4 + 0.5) % 1.0)
    decorate(f, st, cx, cy - r - 2, cy, r)
    return f


# ============================================================================================================================ AURIS, the Whisper
def auris(st):
    f = Frame()
    p = st.ramp(PEARL)
    sa = st.ramp(SAPPHIRE)
    cx, cy = 32.0, 33.0 + 1.4 * st.s(1)
    tilt = st.env(0.05, 0.95) if st.acting('A') else 0.0
    hoot = st.env(0.2, 0.6) if st.acting('B') else 0.0
    slow = st.env(0.1, 0.9) if st.acting('C') else 0.0
    r = 14.0
    head = 0.5 * tilt * math.sin(st.u * math.pi * 2) + 0.05 * st.s(1)
    # the hem of mist: three trailing puffs below, swaying
    for i in range(3):
        f.ball(cx + (i - 1) * 7 + 2 * st.s(1, i * 0.3), cy + r - 2 + 2 * abs(i - 1), 5.0 - abs(i - 1), 3.4, [R.lerp(c, SAPPHIRE[2], 0.25) for c in p], outline=False)
    # the ear-tufts: two big feather horns, flicking
    for side in (-1, 1):
        base = cx + side * (r * 0.62), cy - r * 0.7
        flick = 0.25 * st.s(2, 0.25 * side) + 0.4 * hoot * side
        tipx, tipy = base[0] + side * 5 + math.sin(flick) * 3 + 6 * math.sin(head) * -1, base[1] - 10 - math.cos(flick) * 2
        f.seg(base[0], base[1], tipx, tipy, 6.0, sa[1])
        f.seg(base[0] - side * 0.5, base[1], tipx, tipy, 2.2, sa[3], outline=False)
    f.ball(cx, cy, r, r, p, rot=head)
    f.halo(cx, cy, 24, SAPPHIRE[2], 0.2 + 0.2 * st.rage)
    # the facial discs: two big ringed eyes; the lids come down slowly in clip C
    for side in (-1, 1):
        ex, ey = cx + side * 5.6 + 3 * math.sin(head), cy - 1.0
        f.ball(ex, ey, 5.6, 5.6, [R.lerp(c, SAPPHIRE[2], 0.35) for c in p], outline=False)
        f.ring(ex, ey, 5.6, 1.0, SAPPHIRE, outline=False)
        eye(f, ex, ey, 3.4, st, lid=min(0.95, 0.9 * slow * math.sin(st.u * math.pi) ** 0.5), look=1.2 * tilt * side)
    f.ball(cx + 3 * math.sin(head), cy + 4.2, 2.0, 2.6 + 1.2 * hoot, GOLD)
    for i in range(3):
        if hoot > 0.05:
            ring_wave(f, cx, cy + 4, 4, (st.u * 1.6 + i * 0.3) % 1.0)
    decorate(f, st, cx, cy - r - 8, cy, r)
    return f


# ============================================================================================================================ HATAKA, the Veil
def hataka(st):
    f = Frame()
    r_ = st.ramp(ROSE)
    pr = st.ramp(PEARL)
    cx, cy = 32.0, 26.0 + 1.3 * st.s(1)
    twirl = st.env(0.05, 0.95) if st.acting('A') else 0.0
    pulse = st.env(0.15, 0.8) if st.acting('B') else 0.0
    wave = st.env(0.1, 0.9) if st.acting('C') else 0.0
    r = 12.5 + 1.6 * pulse
    # the ribbons: four long streamers below, each a chain of round links that trail and twirl
    for i in range(4):
        base = cx + (i - 1.5) * 5.0
        x, y = base, cy + r * 0.75
        for k in range(8):
            ph = 0.12 * k
            sway = 3.2 * math.sin(2 * math.pi * (st.t + ph + i * 0.2)) + 6.0 * twirl * math.sin(2 * math.pi * (st.u * 2 + ph))
            x = base + sway * (k / 7.0 + 0.2)
            y = cy + r * 0.75 + k * 3.3
            col = [AQUA, ROSE, PEARL, AMETHYST][i][1 + (k % 2)]
            f.spot(x, y, 2.5 - 0.15 * k, col, outline=(k < 7))
    f.ball(cx, cy, r, r * 0.92, r_)
    # the silk panels: three soft lighter lobes down the front, and a gold rim
    for j in (-1, 0, 1):
        f.blob(f.ellipse_mask(cx + j * 5.4, cy + 1.5, 2.2, 8.0) & f.ellipse_mask(cx, cy, r - 1, r * 0.92 - 1), [R.lerp(c, (255, 255, 255), 0.2) for c in r_][2])
    f.ring(cx, cy - r * 0.55, r * 0.55, 1.4, GOLD, arc=(math.pi * 1.05, math.pi * 1.95), outline=True)
    f.halo(cx, cy, 24, ROSE[3], 0.3 + 0.3 * pulse + 0.25 * st.rage)
    # a round, calm face: two eyes and a small smile; one hand-flap waves in clip C
    for side in (-1, 1):
        eye(f, cx + side * 4.6, cy + 0.5, 2.5, st, 1.0 if (st.t * 3) % 1.0 > 0.95 else 0.0, look=0.5 * st.s(1))
    f.blob(f.ellipse_mask(cx, cy + 6.0, 2.6, 0.9 + 0.3 * pulse), (110, 30, 70))
    if wave > 0:
        ax = cx + r + 3 + 1.0 * math.sin(st.u * 28)
        f.ball(ax, cy - 2 - 6 * wave, 3.4, 3.4, pr, outline=True)
    decorate(f, st, cx, cy - r * 0.92 - 1, cy, r)
    return f


# ============================================================================================================================ the keepers, turned to jewel colours
def jewel(sheet):
    """Vitala's keeper turned to the jewel palette: ember to aqua, gold stays, ash and robe to sapphire."""
    a = np.array(sheet).astype(float)
    r, g, b = a[..., 0].copy(), a[..., 1].copy(), a[..., 2].copy()
    warm = np.clip((r - b) / 255.0, 0, 1)
    a[..., 0] = r * (1 - 0.55 * warm) + b * 0.25 * warm
    a[..., 1] = g * (1 - 0.1 * warm) + r * 0.45 * warm
    a[..., 2] = b + 70 * warm
    gold = (r > 170) & (g > 110) & (b < 90)                                       # gold stays gold
    a[gold, 0], a[gold, 1], a[gold, 2] = r[gold], g[gold], b[gold]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), 'RGBA')


CREATURES = {'mayavi': mayavi, 'vakra': vakra, 'bhavi': bhavi, 'lance': lance, 'auris': auris, 'hataka': hataka}
KEEPERS = ('dvarapala', 'dipaka', 'vanija')


def preview(name, sheet):
    V.preview(name, sheet)
    os.replace('build/vitala_%s_preview.png' % name, 'build/atala_%s_preview.png' % name)
    V.big(name, sheet)
    os.replace('build/vitala_%s_big.png' % name, 'build/atala_%s_big.png' % name)


if __name__ == '__main__':
    only = sys.argv[1:] or list(CREATURES) + list(KEEPERS)
    os.makedirs(OUT, exist_ok=True)
    for k in only:
        if k in CREATURES:
            sh = R.build_sheet(CREATURES[k])
        else:
            sh = jewel(R.build_sheet(V.CREATURES[k]))
        sh.save(os.path.join(OUT, k + '.png'))
        preview(k, sh)
        print('wrote', k)

"""BALA at 128 x 128, EXACTLY as he was drawn: the three 48 px idle frames of assets/atala/enemies/bala are scaled up (edge-smoothed, sharpened) and every clip is made FROM them - the face, the
ring of fire, the horns, the tendrils and the orbs are the same pixels; only what a scenario changes is changed (the eyes' light, the grin opening, the ring's heat, the tendrils lifting, the
mask gathering out of sparks).
Run:  python tools/enemy_art/bala_boss.py     -> assets/enemies/atala/bala.png (1024 x 1024: 8 x 8 cells of 128) and build/bala_boss_preview.png

Rows:  0 idle, the Commander (8)   1 idle, the Duel (8)   2 idle, the Berserk (8)   3 hit x2 then death x6
       4 the tell of an attack (8: the eyes flare, the grin opens, the ring heats)   5 the release (8: the shout, the ring flares and settles)
       6 the Maya-astra (8: the eyes shut, the ring burning white, the tendrils lifted)   7 arriving after a blink (8: he gathers out of sparks)
"""
import math
import os

import numpy as np
from PIL import Image, ImageFilter

S = 128
SRC = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'atala', 'enemies', 'bala')
OUT = os.path.join(os.path.dirname(__file__), '..', '..', 'assets', 'enemies', 'atala', 'bala.png')
YY, XX = np.mgrid[0:S, 0:S]
PX, PY = XX + 0.5, YY + 0.5
K = S / 48.0


def up(path):
    """48 -> 128: scaled in premultiplied colour (no dark fringes), then sharpened, the alpha edge cut crisp."""
    im = Image.open(path).convert('RGBA')
    a = np.array(im).astype(float)
    pm = a[:, :, :3] * (a[:, :, 3:4] / 255.0)
    pim = Image.fromarray(np.dstack([pm, a[:, :, 3]]).astype(np.uint8), 'RGBA')
    big = pim.resize((S * 2, S * 2), Image.NEAREST).filter(ImageFilter.GaussianBlur(2.1)).resize((S, S), Image.LANCZOS)
    b = np.array(big).astype(float)
    al = np.clip((b[:, :, 3] - 90.0) / 80.0, 0, 1)
    rgb = b[:, :, :3] / np.maximum(b[:, :, 3:4], 1.0) * 255.0
    rgb = np.clip(rgb, 0, 255)
    out = np.dstack([rgb, al * 255.0])
    sh = Image.fromarray(out.astype(np.uint8), 'RGBA').filter(ImageFilter.UnsharpMask(radius=1.6, percent=140, threshold=2))
    r = np.array(sh).astype(float)
    r[:, :, 3] = al * 255.0
    return r


FRAMES = [up(os.path.join(SRC, 'idle_%d.png' % i)) for i in range(3)]
BASE = FRAMES[0]
# the parts, in the 128 px frame (found on the 48 px original: eyes y 18-27, grin y 31-37, orbs at the bottom corners)
HEAD_CX = 24.0 * K
EYE_Y0, EYE_Y1 = 17 * K, 28 * K
GRIN_Y0, GRIN_Y1 = 31 * K, 38 * K
TEND_Y = 38 * K


def is_yellow(a):
    return (a[:, :, 0] > 235) & (a[:, :, 1] > 180) & (a[:, :, 3] > 100)


def is_fire(a):
    return (a[:, :, 0] > 190) & (a[:, :, 1] > 60) & (a[:, :, 2] < 120) & (a[:, :, 3] > 100)


def glow_add(f, cx, cy, r, color, k):
    """Light added over the frame: on the body it brightens, in the air it is a soft coloured haze (premultiplied, so it never dims what is under it)."""
    d2 = ((PX - cx) ** 2 + (PY - cy) ** 2) / (r * r)
    g = np.clip(np.exp(-d2 * 2.2) * k, 0, 1)
    a = f[:, :, 3] / 255.0
    solid = a > 0.4
    col = np.array(color, float)
    f[:, :, :3][solid] = np.clip(f[:, :, :3][solid] + (g[solid][:, None] * col[None, :]) * 0.45, 0, 255)
    air = ~solid
    ga = g[air] * 0.85
    a0 = a[air]
    na = a0 + ga * (1 - a0)
    rgb = (f[:, :, :3][air] * a0[:, None] + col[None, :] * ga[:, None] * (1 - a0)[:, None]) / np.maximum(na, 1e-4)[:, None]
    f[:, :, :3][air] = rgb
    f[:, :, 3][air] = na * 255.0


def tendrils(f, lift, sway):
    """Lift and sway the two tendrils (the part of the frame below the cheeks and outside the head) about where they leave the face."""
    out = f.copy()
    reg = (PY >= TEND_Y - 6) & (np.abs(PX - HEAD_CX) > 15.5 * K)
    layer = np.where(reg[:, :, None], f, 0.0)
    out[reg] = 0
    for side in (-1, 1):
        half = (PX - HEAD_CX) * side > 0
        anchor = (HEAD_CX + side * 14.0 * K, TEND_Y - 4)
        ang = side * (-lift * 0.9 + 0.09 * sway)
        c, s = math.cos(-ang), math.sin(-ang)
        dx, dy = PX - anchor[0], PY - anchor[1]
        sx, sy = anchor[0] + dx * c - dy * s, anchor[1] + dx * s + dy * c
        xi, yi = np.clip(sx.astype(int), 0, S - 1), np.clip(sy.astype(int), 0, S - 1)
        inb = (sx >= 0) & (sx < S) & (sy >= 0) & (sy < S)
        src_mask = half & True
        got = layer[yi, xi] * inb[:, :, None]
        srcreg = reg[yi, xi] & ((XX[yi, xi] + 0.5 - HEAD_CX) * side > 0) & inb
        got = got * srcreg[:, :, None]
        m = got[:, :, 3] > 0
        out[m] = got[m]
    return out


def lids(f, shut):
    """Lids over the eyes in the face's own brown, leaving a thin glowing slit: shut 0 open .. 1 shut but for the slit."""
    if shut <= 0:
        return f
    out = f.copy()
    face = np.array([150.0, 84.0, 22.0])
    eyes = is_yellow(f) & (PY > EYE_Y0) & (PY < EYE_Y1)
    ys = np.where(eyes.any(axis=1))[0]
    if len(ys) == 0:
        return f
    y0, y1 = ys.min(), ys.max()
    mid = (y0 + y1) / 2.0
    slit = (1.0 - shut) * (y1 - y0) / 2.0 + 0.9
    cut = eyes & (np.abs(PY - mid) > slit)
    out[:, :, :3][cut] = face
    out[:, :, 3][cut] = 255
    return out


def heated(f, k):
    """The same pixels read hotter (the ring and the eyes first): k 0 as drawn .. 1 white-hot."""
    out = f.copy()
    m = is_fire(f) | is_yellow(f)
    col = out[:, :, :3]
    col[m] = col[m] * (1 - 0.5 * k) + np.array([255, 235, 190]) * 0.5 * k
    ringm = is_fire(f) & (PY < GRIN_Y0)
    col[ringm] = np.clip(col[ringm] * (1 + 0.25 * k), 0, 255)
    return out


def tint_phase(f, ph):
    out = f.copy()
    if ph >= 2:
        warm = (out[:, :, 3] > 100) & (out[:, :, 0] > 90)
        k = 0.10 if ph == 2 else 0.22
        out[:, :, 1][warm] *= 1 - 0.55 * k
        out[:, :, 2][warm] *= 1 - 0.6 * k
        out[:, :, 0][warm] = np.clip(out[:, :, 0][warm] * (1 + 0.15 * k), 0, 255)
    return out


def mouth(f, open_):
    """The grin opens: the dark of a throat, with heat in it, below the line of the teeth and inside the mask."""
    if open_ <= 0.02:
        return f
    out = f.copy()
    face = f[:, :, 3] > 100
    cy = GRIN_Y0 + 4.0 * K
    h = (1.0 + 4.2 * open_) * K
    inside = ((PX - HEAD_CX) / (6.2 * K)) ** 2 + ((PY - (cy + h * 0.5)) / h) ** 2 <= 1.0
    inside &= face & (PY > cy)
    out[:, :, :3][inside] = np.array([60.0, 18.0, 8.0]) + np.array([120.0, 50.0, 14.0]) * np.clip((PY[inside] - cy) / (2 * h), 0, 1)[:, None] * 0.6
    glow_add(out, HEAD_CX, cy + h * 0.6, 16, (255, 150, 50), 0.35 * open_)
    return out


def sparks(f, t, k, n=16, ring=True):
    if k <= 0:
        return f
    out = f.copy()
    for i in range(n):
        a = i * 2.399 + 1.3
        ph = (i * 0.37 + t) % 1.0
        d = 40 + 70 * ph * k
        sx, sy = HEAD_CX + math.cos(a) * d * 1.05, 30 * K * 0.78 + math.sin(a) * d * 0.55 - 20 * ph * k
        glow_add(out, sx, sy, 4.5, (255, 200, 90), 0.9 * (1 - ph) * k)
    return out


def idle_frame(ph, i):
    t = i / 8.0
    seq = [0, 0, 1, 0, 0, 2, 0, 1]                                # his own three frames: eyes forward, eyes glancing, eyes half-shut
    if ph == 3:
        seq = [0, 1, 0, 1, 0, 1, 0, 1]                            # the berserk never blinks
    f = FRAMES[seq[i]]
    f = tint_phase(f, ph)
    f = tendrils(f, 0.0, math.sin(2 * math.pi * t))
    glow_add(f, HEAD_CX, 22 * K * 0.6, 42, (255, 140, 40), 0.18 + 0.1 * math.sin(2 * math.pi * t) + 0.12 * (ph - 1))
    if ph == 3:
        f = sparks(f, t, 0.7, 12)
    return f


def tell_frame(i):
    k = i / 7.0
    e = k * k * (3 - 2 * k)
    f = FRAMES[0]
    f = heated(f, 0.8 * e)
    f = tendrils(f, 0.25 * e, math.sin(2 * math.pi * k))
    f = mouth(f, 0.55 * e)
    glow_add(f, HEAD_CX - 11 * K * 0.9, 22 * K, 20, (255, 235, 160), 0.9 * e)
    glow_add(f, HEAD_CX + 11 * K * 0.9, 22 * K, 20, (255, 235, 160), 0.9 * e)
    glow_add(f, HEAD_CX, 20, 40, (255, 120, 30), 0.2 + 0.3 * e)
    return sparks(f, k, 0.4 * e, 8)


def release_frame(i):
    k = i / 7.0
    shout = math.sin(math.pi * min(1.0, k * 1.4)) ** 0.7
    f = FRAMES[0]
    f = heated(f, 0.9 * shout)
    f = tendrils(f, 0.12, math.sin(2 * math.pi * k * 2))
    f = mouth(f, 0.95 * shout)
    glow_add(f, HEAD_CX, 20, 44, (255, 240, 200), 0.6 * shout)
    return sparks(f, k, 0.9 * shout, 14)


def cast_frame(i):
    k = i / 8.0
    f = FRAMES[0]
    f = lids(f, 0.9)
    f = heated(f, 0.5 + 0.3 * math.sin(2 * math.pi * k))
    f = tendrils(f, 0.9, math.sin(2 * math.pi * k))
    glow_add(f, HEAD_CX, 20, 44, (255, 245, 220), 0.35 + 0.2 * math.sin(2 * math.pi * k))
    return sparks(f, k, 0.6, 12)


def arrive_frame(i):
    k = i / 7.0
    gather = 0.15 + 0.85 * k ** 0.7
    f = tint_phase(FRAMES[0], 3).copy()
    keep = np.hypot(PX - HEAD_CX, PY - 64) < 150 * gather
    f[~keep] = 0
    f = heated(f, 0.8 * (1 - k))
    for j in range(26):
        a = j * 2.399
        d = (1 - gather) * 70 + 6 + (j % 5) * 3
        glow_add(f, HEAD_CX + math.cos(a) * d, 64 + math.sin(a) * d * 0.8, 5, (255, 200, 90), 0.9 * (1 - k) + 0.2)
    glow_add(f, HEAD_CX, 64, 46, (255, 230, 170), 0.6 * (1 - k) ** 2)
    return f


def white(f, k):
    out = f.copy()
    m = out[:, :, 3] > 0
    out[:, :, :3][m] = out[:, :, :3][m] * (1 - k) + 255 * k
    return out


def death(base, i, n=6):
    out = base.copy()
    k = i / (n - 1)
    rng = np.random.RandomState(7)
    noise = rng.rand(S, S)
    noise = (noise + np.roll(noise, 1, 0) + np.roll(noise, 1, 1)) / 3.0
    thr = 0.12 + 0.95 * k - 0.35 * (YY / S) * k
    gone = noise < thr * 0.9 * (k > 0.15)
    out[gone, 3] = 0
    out = white(out, max(0.0, 0.85 - 1.4 * k))
    out = np.roll(out, -int(round(8 * k * k)), 0)
    out[:, :, 3] *= 1.0 - 0.6 * max(0.0, k - 0.6)
    return out


def u8(f):
    return np.clip(f, 0, 255).astype(np.uint8)


def sheet():
    sh = Image.new('RGBA', (8 * S, 8 * S), (0, 0, 0, 0))

    def put(r, c, f):
        sh.paste(Image.fromarray(u8(f), 'RGBA'), (c * S, r * S))

    for ph in (1, 2, 3):
        for i in range(8):
            put(ph - 1, i, idle_frame(ph, i))
    put(3, 0, white(FRAMES[0], 1.0))
    put(3, 1, white(FRAMES[0], 0.45))
    for i in range(6):
        put(3, 2 + i, death(FRAMES[0], i))
    for i in range(8):
        put(4, i, tell_frame(i))
        put(5, i, release_frame(i))
        put(6, i, cast_frame(i))
        put(7, i, arrive_frame(i))
    return sh


if __name__ == '__main__':
    sh = sheet()
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sh.save(OUT)
    os.makedirs('build', exist_ok=True)
    big = Image.new('RGBA', sh.size, (46, 34, 40, 255))
    big.alpha_composite(sh)
    big.save('build/bala_boss_preview.png')
    old = Image.new('RGBA', (3 * 144, 144), (46, 34, 40, 255))
    for i in range(3):
        old.alpha_composite(Image.open(os.path.join(SRC, 'idle_%d.png' % i)).convert('RGBA').resize((144, 144), Image.NEAREST), (i * 144, 0))
    old.save('build/bala_old3x.png')
    print('wrote', OUT)

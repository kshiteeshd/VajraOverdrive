"""WIND and FIRE-FLOW sprite sheets for the Vayu and Agni astras, 32 x 32 cells, eight frames a row (the size of every effect sprite in this game), in the
language of fire.py: flat colour bands with a Bayer dither, every loop closes, nothing random.  The sprites point to the RIGHT (+x), their root at the left, so the
game turns each one to its heading.  A sprite is a union of discs walked along a tapered curve, then thresholded into bands.

    python tools/enemy_art/flowfx.py   ->   build/astra_wind.png       (256 x 192)   rows: GUST, SWIRL, WISP, DEBRIS, FUNNEL, BLADE
                                            build/astra_fireflow.png   (256 x 160)   rows: STREAM, LICK, EMBER, BLOOM, SCORCH
                                            build/flowfx_preview.png   the two sheets, labelled
                                            build/flowfx_scene.png     both effects SIMULATED (particles on a flow field, enemies pulled, cut, flung, burned)

Agni (the reference drawing): flame streams leave the ship in every direction, each ending in a curl, with embers between them.
Vayu: everything is drawn in to a tornado (gusts spiralling inward, enemies carried round the funnel and cut down one by one), held at the eye, then thrown outward."""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fire as F

S, N, TAU = F.S, F.N, F.TAU
XS, YS = F.XS, F.YS                      # the 64-unit field, sampled at the centre of every pixel
BAY = F.BAY
FIRE = F.BANDS
THRESH = F.THRESH

# the wind's own six bands, brightest first: a grey with a hint of blue (white-grey, pale slate, slate, blue-grey, deep blue-grey, shadow)
WIND = np.array([(240, 244, 250), (206, 216, 230), (170, 184, 204), (132, 150, 176), (94, 112, 140), (60, 74, 96)], dtype=np.uint8)


def band(v, palette, alpha_floor=0.04, dither=0.07):
    """A 0..1 field into six flat bands of `palette`, dithered at every edge; below the floor, nothing."""
    out = np.zeros((S, S, 4), dtype=np.uint8)
    vd = v + BAY * dither
    idx = np.full(v.shape, 5, dtype=np.int64)
    for k, th in reversed(list(enumerate(THRESH))):
        idx = np.where(vd > th, k, idx)
    live = (v + BAY * 0.02) > alpha_floor
    out[live, :3] = palette[idx[live]]
    out[live, 3] = 255
    return out


def sn(x):
    return max(0.0, math.sin(x))


def put(img, x, y, colour, alpha=255):
    xi, yi = int(math.floor(x / F.Z)), int(math.floor(y / F.Z))
    if 0 <= xi < S and 0 <= yi < S:
        img[yi, xi, :3] = colour
        img[yi, xi, 3] = alpha


def stroke(pts, widths, soft=1.0):
    """A field that is 1 on a curve and falls to 0 at the width: the union of discs along the polyline (64-unit coordinates)."""
    v = np.zeros(XS.shape, dtype=np.float64)
    for (x, y), w in zip(pts, widths):
        d = np.hypot(XS - x, YS - y)
        v = np.maximum(v, 1.0 - (d / max(w, 0.4)) ** soft)
    return v


def curve(f, n=40):
    return [f(i / (n - 1)) for i in range(n)]


def taper(s, root=1.0, belly=0.35, tip=0.1):
    """Width along a stroke: full at the root, easing to the tip."""
    return root * (1 - s) ** 0.9 * (1 - belly) + belly * root * sn(math.pi * min(1.0, s * 1.15)) * 0.0 + tip


# ================================================================== WIND ==================================================================
def gust(t):
    """A long curved streak of air, tapered at both ends, with two thinner streaks lagging beside it and bands of light running along it."""
    p = t * TAU
    n = 44
    main = curve(lambda u: (3 + 57 * u, 32 + 3.0 * math.sin(TAU * (u * 0.9) - p) * sn(math.pi * u)), n)
    w = [1.0 + 4.6 * sn(math.pi * i / (n - 1)) ** 0.8 for i in range(n)]
    v = stroke(main, w)
    for off, lag, ln in ((-8, 0.35, 0.7), (8, 0.65, 0.6)):
        side = curve(lambda u: (8 + 46 * ln * u, 32 + off + 2.0 * math.sin(TAU * (u * 0.9 + lag) - p) * sn(math.pi * u)), 30)
        v = np.maximum(v, 0.8 * stroke(side, [0.6 + 1.6 * sn(math.pi * i / 29) for i in range(30)]))
    u = np.clip((XS - 3) / 57.0, 0, 1)
    stripes = 0.62 + 0.38 * np.sin(TAU * (u * 3.0 - t))                 # light runs down the streak
    img = band(np.clip(v * stripes, 0, 1), WIND)
    for k in range(5):                                                    # motes caught in it
        ph = (t + F._hash(k, 1, 301)) % 1.0
        put(img, 8 + ph * 50, 32 + (F._hash(k, 2, 301) - 0.5) * 22, WIND[1], 255 if ph < 0.85 else 130)
    return img


def swirl(t):
    """Two spiral arms seen from above, turning: the body of the funnel's flow."""
    phi = t * math.pi                                                     # two arms: a half turn closes the loop
    v = np.zeros(XS.shape)
    for arm in range(2):
        pts, ws = [], []
        for i in range(48):
            th = i / 47 * 4.6
            r = 3.0 + 25.0 * (i / 47) ** 0.8
            a = th + phi + arm * math.pi
            pts.append((31.5 + r * math.cos(a), 31.5 + r * math.sin(a)))
            ws.append(0.9 + 3.2 * sn(math.pi * (0.1 + 0.9 * i / 47)) ** 0.9)
        v = np.maximum(v, stroke(pts, ws))
    dx, dy = XS - 31.5, YS - 31.5
    v = v * np.clip(1.15 - np.hypot(dx, dy) / 34.0, 0, 1)
    return band(np.clip(v, 0, 1), WIND)


def wisp(t):
    """A small curl of wind: a short streak ending in a hook, the curls at the tips of the drawing, in air."""
    p = t * TAU
    pts, ws = [], []
    n = 52
    for i in range(n):
        s = i / (n - 1)
        if s < 0.62:
            q = s / 0.62
            pts.append((6 + 34 * q, 38 - 5 * q + 2.2 * math.sin(TAU * q - p)))
        else:
            q = (s - 0.62) / 0.38
            a = -0.5 + q * 4.4
            r = 8.0 * (1 - 0.65 * q)
            pts.append((40 + 4 + r * math.sin(a), 29 + 4 + r * math.cos(a) * -1.0 + 6))
        ws.append(0.8 + 2.8 * sn(math.pi * (0.05 + 0.9 * s)) ** 0.9)
    v = stroke(pts, ws)
    u = np.clip((XS - 6) / 50.0, 0, 1)
    return band(np.clip(v * (0.65 + 0.35 * np.sin(TAU * (u * 2.5 - t))), 0, 1), WIND)


def debris(t):
    """Dust, leaves and grit carried in the wind: five bits tumbling along curved paths with short trails."""
    img = np.zeros((S, S, 4), dtype=np.uint8)
    for k in range(5):
        ph = (t + F._hash(k, 3, 311)) % 1.0
        for tr in range(4):
            q = max(0.0, ph - tr * 0.035)
            x = 6 + 52 * q
            y = 14 + 36 * F._hash(k, 4, 311) + 7 * math.sin(TAU * (q * 1.3 + F._hash(k, 5, 311)))
            c = WIND[1] if tr == 0 else WIND[3] if tr < 3 else WIND[4]
            put(img, x, y, c, 255 if q > 0 else 0)
        if k % 2 == 0:                                                     # a leaf: two pixels wide, turning
            put(img, 6 + 52 * ph + 2, 14 + 36 * F._hash(k, 4, 311) + 7 * math.sin(TAU * (ph * 1.3 + F._hash(k, 5, 311))) + 2, WIND[2], 255)
    return img


def funnel(t):
    """The tornado from above: three arms spiralling in to a dark eye, a ring at the rim, and grit at the edge. It is drawn big in the game (several copies, scaled)."""
    phi = t * TAU / 3.0                                                    # three arms: a third of a turn closes the loop
    v = np.zeros(XS.shape)
    for arm in range(3):
        pts, ws = [], []
        for i in range(60):
            th = i / 59 * 3.4
            r = 29.0 * (1 - i / 59) ** 0.9 + 4.0
            a = th + phi + arm * TAU / 3
            pts.append((31.5 + r * math.cos(a), 31.5 + r * math.sin(a)))
            ws.append(1.4 + 3.2 * sn(math.pi * (0.08 + 0.92 * i / 59)) ** 0.8)
        v = np.maximum(v, stroke(pts, ws))
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    v = np.maximum(v * np.clip((d - 3.5) / 5.0, 0, 1), 0.7 * np.exp(-((d - 29.5) / 1.2) ** 2))
    return band(np.clip(v, 0, 1), WIND)


def blade(t):
    """A crescent of cutting wind, a one-shot: it sweeps out (frames 0-4) and thins away (5-7)."""
    s = min(1.0, t * N / (N - 1.0))
    sweep = math.radians(150) * min(1.0, s * 1.8)
    fade = 1.0 - max(0.0, (s - 0.55) / 0.45)
    pts, ws = [], []
    for i in range(36):
        u = i / 35
        a = math.radians(-75) - sweep * 0.5 + sweep * u + math.radians(75) * 0.0
        r = 22 + 5 * s
        pts.append((31.5 + r * math.cos(a - math.pi / 2 + math.pi / 2), 40 + r * math.sin(a) * 0.9))
        ws.append((0.7 + 4.2 * sn(math.pi * u) ** 1.2) * (0.5 + 0.5 * fade))
    v = stroke(pts, ws) * fade
    v = np.maximum(v, 0.55 * stroke([(x, y - 3) for x, y in pts], [w * 0.5 for w in ws]) * fade)
    return band(np.clip(v, 0, 1), WIND, 0.05)


# ================================================================== FIRE FLOW ==================================================================
def stream(t):
    """A flame stream leaving the ship: wide and hot at the root, thinning as it runs out, and ending in a curl - the streams of the reference drawing."""
    p = t * TAU
    pts, ws = [], []
    n = 60
    for i in range(n):
        s = i / (n - 1)
        if s < 0.7:
            q = s / 0.7
            pts.append((3 + 40 * q, 36 - 3 * q + 3.2 * math.sin(TAU * (q * 0.8) - p) * q))
        else:
            q = (s - 0.7) / 0.3
            a = q * 4.6 - 0.2
            r = 7.5 * (1 - 0.7 * q)
            pts.append((43 + 1 + r * math.sin(a), 27 + 9 + r * math.cos(a) * -1.0))
        ws.append(0.9 + 7.0 * (1 - s) ** 1.1 + 1.0 * sn(math.pi * s) * 0.0 + 0.3)
    v = stroke(pts, ws)
    n1 = F.fbm(XS * 0.12 - 1.6 * math.cos(p), YS * 0.14 + math.sin(p) * 1.2, 401)
    core = stroke(pts, [w * 0.45 for w in ws])
    v = np.clip(v * (0.62 + 0.55 * n1) + 0.45 * core, 0, 1)
    img = band(v, FIRE, 0.06)
    for k in range(4):                                                    # embers peeling off
        ph = (t + F._hash(k, 6, 411)) % 1.0
        put(img, 18 + ph * 40, 36 + (F._hash(k, 7, 411) - 0.5) * 24 * ph, FIRE[1] if ph < 0.3 else FIRE[2] if ph < 0.65 else FIRE[3], 255 if ph < 0.85 else 120)
    return img


def lick(t):
    """A shorter curved tongue of flame, the lesser streams between the great ones."""
    p = t * TAU
    n = 40
    pts = curve(lambda s: (4 + 38 * s, 34 - 6 * sn(math.pi * s * 0.9) + 2.4 * math.sin(TAU * s - p) * s), n)
    ws = [0.8 + 5.4 * (1 - i / (n - 1)) ** 1.1 for i in range(n)]
    v = stroke(pts, ws)
    n1 = F.fbm(XS * 0.13 + math.cos(p) * 1.4, YS * 0.15 - 1.3 * math.sin(p), 421)
    v = np.clip(v * (0.6 + 0.6 * n1) + 0.4 * stroke(pts, [w * 0.4 for w in ws]), 0, 1)
    return band(v, FIRE, 0.06)


def ember(t):
    """Three or four sparks with short trails, cooling as they go."""
    img = np.zeros((S, S, 4), dtype=np.uint8)
    for k in range(4):
        ph = (t + F._hash(k, 8, 431)) % 1.0
        for tr in range(5):
            q = max(0.0, ph - tr * 0.03)
            x = 4 + 56 * q
            y = 20 + 22 * F._hash(k, 9, 431) + 8 * math.sin(TAU * (q * 1.1 + F._hash(k, 10, 431)))
            c = FIRE[1] if tr == 0 and ph < 0.4 else FIRE[2] if ph < 0.65 else FIRE[3] if ph < 0.85 else FIRE[4]
            put(img, x, y, c, 255 if q > 0 else 0)
    return img


def bloom(t):
    """The fire at the ship, the root of the streams: a hot round core with spikes of flame all round it."""
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    spikes = 0.5 + 0.5 * np.sin(14 * th + 2 * p + 1.5 * F.fbm(dx * 0.08, dy * 0.08, 441))
    n = F.fbm(dx * 0.11 + math.cos(p) * 1.5, dy * 0.11 + math.sin(p) * 1.5, 451)
    r = 9 + 15 * spikes ** 2.2 * (0.7 + 0.5 * n)
    v = 1 - d / r
    v = np.clip(v + 0.7 * np.exp(-(d / 5.5) ** 2), 0, 1)
    return band(v, FIRE, 0.05)


def scorch(t):
    """What the fire leaves on the ground: a patch of char with embers breathing in it."""
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    n = F.fbm(dx * 0.10, dy * 0.10, 461)
    n2 = F.fbm(dx * 0.22 + math.cos(p), dy * 0.22 + math.sin(p), 463)
    v = np.clip((1 - d / (26 * (0.7 + 0.5 * n))) * 0.30 + 0.30 * np.clip(n2 - 0.60, 0, 1) * 2.0 * (d < 22), 0, 0.46)
    return band(v, FIRE, 0.05)


WIND_ROWS = [("GUST", gust), ("SWIRL", swirl), ("WISP", wisp), ("DEBRIS", debris), ("FUNNEL", funnel), ("BLADE (one-shot)", blade)]
FIRE_ROWS = [("STREAM (with curl)", stream), ("LICK", lick), ("EMBER", ember), ("BLOOM (at the ship)", bloom), ("SCORCH (ground)", scorch)]


def build(rows):
    sheet = Image.new("RGBA", (N * S, len(rows) * S), (0, 0, 0, 0))
    for r, (_, fn) in enumerate(rows):
        for f in range(N):
            sheet.alpha_composite(Image.fromarray(fn(f / N), "RGBA"), (f * S, r * S))
    return sheet


def cell(sheet, row, frame):
    return sheet.crop((frame % N * S, row * S, (frame % N + 1) * S, (row + 1) * S))


def preview(sheets, scale=4):
    pad, lab = 6, 150
    rows = [(n, i, k) for k, (sh, rs) in enumerate(sheets) for i, (n, _) in enumerate(rs)]
    W = lab + N * S * scale + pad
    H = len(rows) * (S * scale + pad) + pad
    bg = Image.new("RGBA", (W, H), (24, 20, 22, 255))
    d = ImageDraw.Draw(bg)
    for j, (name, r, k) in enumerate(rows):
        y = pad + j * (S * scale + pad)
        d.rectangle([lab, y, lab + N * S * scale // 2, y + S * scale], fill=(40, 36, 38))
        d.rectangle([lab + N * S * scale // 2, y, lab + N * S * scale, y + S * scale], fill=(14, 12, 14))
        row = sheets[k][0].crop((0, r * S, N * S, (r + 1) * S)).resize((N * S * scale, S * scale), Image.NEAREST)
        bg.alpha_composite(row, (lab, y))
        d.text((6, y + S * scale // 2 - 6), f"{'WIND' if k == 0 else 'FIRE'}  {name}", fill=(255, 220, 140, 255) if k else (150, 235, 230, 255))
    return bg


# ================================================================== THE SIMULATION (a preview of what the game will do) ==================================================================
PW, PH, ZOOM = 320, 240, 3


def rot_sprite(img, ang_deg, size):
    im = img.resize((size, size), Image.NEAREST).rotate(-ang_deg, resample=Image.NEAREST, expand=True)
    return im


def add(canvas, im, cx, cy, alpha=1.0):
    """Additive blend of a sprite centred at (cx, cy) onto a float canvas (H, W, 3)."""
    a = np.asarray(im).astype(np.float32)
    h, w = a.shape[:2]
    x0, y0 = int(round(cx - w / 2)), int(round(cy - h / 2))
    xs, ys = max(0, x0), max(0, y0)
    xe, ye = min(canvas.shape[1], x0 + w), min(canvas.shape[0], y0 + h)
    if xe <= xs or ye <= ys:
        return
    src = a[ys - y0:ye - y0, xs - x0:xe - x0]
    k = src[..., 3:4] / 255.0 * alpha
    canvas[ys:ye, xs:xe] = np.clip(canvas[ys:ye, xs:xe] + src[..., :3] * k, 0, 255)


def noise1(a, b, c=0.0):
    return F.vnoise(np.array([a]), np.array([b]), int(c) + 7)[0]


def agni_scene(sheet, times, seed=3):
    """The Pyre Bloom: eighteen streams leave the hull, each particle a flame sprite on a flow field (outward, swirling, drawn toward foes), burning what it touches."""
    rng = np.random.RandomState(seed)
    C = np.array([PW / 2, PH / 2])
    foes = []
    for i in range(11):
        a = rng.uniform(0, TAU)
        r = rng.uniform(62, 112)
        foes.append({"p": C + r * np.array([math.cos(a), math.sin(a)]), "hp": 1.0, "burn": 0.0})
    dt = 1 / 60.0
    out = []
    parts = []
    streams = 18
    t = 0.0
    snaps = list(times)
    canvasses = []
    step = 0
    while snaps:
        if t < 2.4:
            ramp = min(1.0, t / 0.5)
            for s in range(streams):
                if step % 5 == s % 5:
                    base = s * TAU / streams + 0.25 * math.sin(t * 0.9) + (0.07 if s % 2 else -0.07)
                    parts.append({"p": C + 14 * np.array([math.cos(base), math.sin(base)]), "h": base, "age": 0.0, "life": 1.15 + 0.35 * rng.rand(), "sp": 140 + 60 * rng.rand(),
                                  "side": 1 if s % 2 else -1, "kind": 0 if rng.rand() < 0.7 else 1, "f": rng.randint(0, N), "scale": 0.55 + 0.45 * ramp})
        for q in parts:
            q["age"] += dt
            u = q["age"] / q["life"]
            # the flow field: runs outward, curls over as it ages (the hook at the tip), and leans toward the nearest living foe
            q["h"] += q["side"] * (0.5 * math.sin(q["age"] * 5 + q["f"]) + 4.4 * max(0.0, u - 0.55) ** 1.4) * dt
            best, bd = None, 70.0
            for f in foes:
                if f["hp"] <= 0:
                    continue
                d = np.hypot(*(f["p"] - q["p"]))
                if d < bd:
                    best, bd = f, d
            if best is not None:
                want = math.atan2(*(best["p"] - q["p"])[::-1])
                dh = (want - q["h"] + math.pi) % TAU - math.pi
                q["h"] += dh * 2.0 * dt
            q["p"] = q["p"] + q["sp"] * (1 - 0.5 * u) * dt * np.array([math.cos(q["h"]), math.sin(q["h"])])
            for f in foes:
                if f["hp"] > 0 and np.hypot(*(f["p"] - q["p"])) < 11:
                    f["burn"] = 1.0
        for f in foes:
            if f["burn"] > 0:
                f["hp"] -= dt * 0.33
        parts = [q for q in parts if q["age"] < q["life"]]
        t += dt
        step += 1
        if snaps and t >= snaps[0]:
            snaps.pop(0)
            cv = np.zeros((PH * ZOOM, PW * ZOOM, 3), dtype=np.float32)
            cv[:] = (14, 12, 16)
            # the ground: scorch under what has died, a faint grid
            for f in foes:
                if f["hp"] <= 0:
                    add(cv, rot_sprite(cell(sheet, 4, int(t * 8)), 0, 30 * ZOOM), f["p"][0] * ZOOM, f["p"][1] * ZOOM, 0.7)
            for q in parts:
                u = q["age"] / q["life"]
                sz = int((26 + 18 * sn(math.pi * min(1.0, u + 0.05))) * q["scale"] * ZOOM)
                spr = rot_sprite(cell(sheet, q["kind"], q["f"] + int(q["age"] * 14)), math.degrees(q["h"]), max(8, sz))
                add(cv, spr, q["p"][0] * ZOOM, q["p"][1] * ZOOM, 0.62 * (1 - 0.6 * max(0, u - 0.6) / 0.4))
            for q in parts[::5]:
                add(cv, rot_sprite(cell(sheet, 2, q["f"] + int(q["age"] * 10)), math.degrees(q["h"]), 24 * ZOOM), q["p"][0] * ZOOM, q["p"][1] * ZOOM, 0.7)
            for f in foes:
                if f["hp"] > 0:
                    x, y = f["p"] * ZOOM
                    cv[int(y) - 14:int(y) + 14, int(x) - 14:int(x) + 14] = (150, 150, 160) if f["burn"] == 0 else (200, 120, 70)
                    if f["burn"] > 0:
                        add(cv, rot_sprite(cell(sheet, 0, int(t * 12)), -90, 30 * ZOOM), x, y - 18, 0.9)
            add(cv, rot_sprite(cell(sheet, 3, int(t * 12)), 0, int((26 + 10 * min(1, t / 0.5)) * ZOOM)), C[0] * ZOOM, C[1] * ZOOM, 1.0)
            cv[int(C[1] * ZOOM) - 6:int(C[1] * ZOOM) + 6, int(C[0] * ZOOM) - 6:int(C[0] * ZOOM) + 6] = (180, 220, 245)
            canvasses.append(Image.fromarray(cv.astype(np.uint8), "RGB"))
    return canvasses


def vayu_scene(sheet, times, seed=5):
    """The tornado: the air is drawn in (gusts spiral to the eye, foes are carried round the funnel and cut down one by one), held, then thrown outward."""
    rng = np.random.RandomState(seed)
    C = np.array([PW / 2, PH / 2])
    foes = []
    for i in range(9):
        a = rng.uniform(0, TAU)
        r = rng.uniform(70, 115)
        foes.append({"r": r, "th": a, "state": "caught", "cut": 0.55 + 0.2 * i, "flash": 0.0, "out": 0.0, "x": 0.0})
    # the three that survive the cutting are flung
    bullets = [{"r": 150 + rng.rand() * 40, "th": rng.uniform(0, TAU)} for _ in range(26)]
    dt = 1 / 60.0
    t, step = 0.0, 0
    parts = []
    snaps = list(times)
    out = []
    T_SUCK, T_EYE, T_THROW = 1.6, 2.0, 2.0
    while snaps:
        phase = "suck" if t < T_SUCK else "eye" if t < T_EYE else "throw"
        # emit gusts
        if step % 2 == 0:
            for k in range(7 if phase != "eye" else 2):
                a = rng.uniform(0, TAU)
                if phase == "suck":
                    r = rng.uniform(110, 160)
                    parts.append({"r": r, "th": a, "age": 0.0, "life": 1.2, "f": rng.randint(0, N), "kind": 0 if rng.rand() < 0.55 else 2, "mode": "in"})
                elif phase == "throw":
                    r = rng.uniform(16, 40)
                    parts.append({"r": r, "th": a, "age": 0.0, "life": 0.85, "f": rng.randint(0, N), "kind": 0 if rng.rand() < 0.65 else 2, "mode": "out"})
        for q in parts:
            q["age"] += dt
            if q["mode"] == "in":                                          # inward and round: faster the closer it is
                q["r"] -= (70 + 150 * min(1.0, t / T_SUCK) + 90 * (1 - q["r"] / 160)) * dt
                q["th"] += (1.5 + 3.4 * (1 - q["r"] / 160)) * dt
                if q["r"] < 8:
                    q["age"] = 99
            else:
                q["r"] += (360 + 420 * q["age"]) * dt
                q["th"] += 1.0 * dt
        parts = [q for q in parts if q["age"] < q["life"]]
        for f in foes:
            if f["state"] == "caught":
                f["r"] = max(26, f["r"] - (22 + 55 * min(1, t / T_SUCK)) * dt)
                f["th"] += (1.4 + 3.6 * (1 - f["r"] / 115)) * dt
                if t >= f["cut"] and f["cut"] < 1.9 and f["x"] == 0:     # one by one the wind cuts them down
                    f["flash"] = 0.28
                    f["x"] = 1
                    if f["cut"] < 1.45:
                        f["state"] = "dead"
                if f["state"] == "caught" and phase == "throw":
                    f["state"] = "flung"
            elif f["state"] == "flung":
                f["r"] += (300 + 700 * f["out"]) * dt
                f["out"] += dt
                f["th"] += 1.4 * dt
            f["flash"] = max(0, f["flash"] - dt)
        for b in bullets:                                                    # foes' rounds: pulled into the funnel, then blown out and away
            if phase == "suck":
                b["r"] -= (60 + 120 * min(1, t / T_SUCK)) * dt
                b["th"] += 2.0 * dt
            elif phase == "throw":
                b["r"] += 520 * dt
        t += dt
        step += 1
        if snaps and t >= snaps[0]:
            snaps.pop(0)
            cv = np.zeros((PH * ZOOM, PW * ZOOM, 3), dtype=np.float32)
            cv[:] = (14, 12, 16)
            fx = np.clip(1 - abs(t - 1.8) / 1.2, 0, 1)
            add(cv, rot_sprite(cell(sheet, 4, int(t * 8)), 0, int(200 * ZOOM)), C[0] * ZOOM, C[1] * ZOOM, 0.32 * (1 if phase != "throw" else max(0, 1 - (t - T_EYE) / 0.5)))
            add(cv, rot_sprite(cell(sheet, 1, int(t * 10)), 0, int(120 * ZOOM)), C[0] * ZOOM, C[1] * ZOOM, 0.7 * (1 if phase != "throw" else 0.2))
            for q in parts:
                x, y = C + q["r"] * np.array([math.cos(q["th"]), math.sin(q["th"])])
                if q["mode"] == "in":
                    tang = q["th"] + math.pi / 2 + 0.55 * 1.0
                    head = tang + math.pi * (1 if True else 0) * 0
                    ang = math.degrees(q["th"] + math.pi / 2 + 0.45) + 180
                else:
                    ang = math.degrees(q["th"] + 0.35)
                sz = int((30 + 14 * sn(math.pi * min(1.0, q["age"] / q["life"]))) * ZOOM)
                add(cv, rot_sprite(cell(sheet, q["kind"], q["f"] + int(q["age"] * 14)), ang, sz), x * ZOOM, y * ZOOM, 0.9 * min(1.0, (q["life"] - q["age"]) * 3))
            for b in bullets:
                x, y = C + b["r"] * np.array([math.cos(b["th"]), math.sin(b["th"])])
                if 0 <= x < PW and 0 <= y < PH:
                    cv[int(y * ZOOM) - 3:int(y * ZOOM) + 4, int(x * ZOOM) - 3:int(x * ZOOM) + 4] = (255, 200, 120)
            for f in foes:
                x, y = C + f["r"] * np.array([math.cos(f["th"]), math.sin(f["th"])])
                if f["state"] != "dead" and -20 < x < PW + 20 and -20 < y < PH + 20:
                    cv[max(0, int(y * ZOOM) - 14):int(y * ZOOM) + 14, max(0, int(x * ZOOM) - 14):int(x * ZOOM) + 14] = (150, 150, 160)
                if f["flash"] > 0:
                    add(cv, rot_sprite(cell(sheet, 5, int((0.28 - f["flash"]) / 0.28 * 7)), math.degrees(f["th"]) + 90, int(60 * ZOOM)), x * ZOOM, y * ZOOM, 1.0)
            add(cv, rot_sprite(cell(sheet, 4, int(t * 14)), 0, int(36 * ZOOM)), C[0] * ZOOM, C[1] * ZOOM, 0.9)
            cv[int(C[1] * ZOOM) - 6:int(C[1] * ZOOM) + 6, int(C[0] * ZOOM) - 6:int(C[0] * ZOOM) + 6] = (180, 220, 245)
            out.append(Image.fromarray(cv.astype(np.uint8), "RGB"))
    return out


def scene(wind, fireflow):
    from PIL import ImageFont
    a = agni_scene(fireflow, [0.5, 1.0, 1.7, 2.6])
    v = vayu_scene(wind, [0.7, 1.35, 2.15, 2.6])
    W, H = a[0].size
    out = Image.new("RGB", (W * 4 + 25, H * 2 + 22), (30, 26, 32))
    d = ImageDraw.Draw(out)
    for i in range(4):
        out.paste(a[i], (5 + i * (W + 5), 5))
        out.paste(v[i], (5 + i * (W + 5), H + 12))
    d.text((10, 8), "AGNI  the pyre bloom: streams flow out of the ship, curl, and burn everything they touch", fill=(255, 220, 140))
    d.text((10, H + 15), "VAYU  the tornado: the air is drawn in, foes are carried round the funnel and cut down one by one, then it is thrown outward", fill=(150, 235, 230))
    return out


if __name__ == "__main__":
    os.makedirs("build", exist_ok=True)
    wind, fireflow = build(WIND_ROWS), build(FIRE_ROWS)
    wind.save("build/astra_wind.png")
    fireflow.save("build/astra_fireflow.png")
    preview([(wind, WIND_ROWS), (fireflow, FIRE_ROWS)]).convert("RGB").save("build/flowfx_preview.png")
    scene(wind, fireflow).save("build/flowfx_scene.png")
    print(wind.size, fireflow.size)

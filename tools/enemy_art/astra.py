"""THE ASTRAS, drawn: one 32 x 32 sheet per astra, eight frames a row, in the language of fire.py (six flat colour bands, Bayer dither, every loop closes,
nothing is random).  This file holds the AGNI ASTRA ("the pyre": 12 fireballs fall in a circle the pilot aims, each bursts, ignites and leaves a burning patch).

    python tools/enemy_art/astra.py     ->  build/astra_agni.png (256 x 128, raw)   build/astra_agni_preview.png (labelled)   build/astra_agni_scene.png (the release, mocked)

Rows: 0 HELD   (the shard that rides the yantra while the mantra is held)    1 BALL  (the falling fireball: head and a tail of embers, tail up, turn it in code)
      2 BURST  (the impact, a ONE-SHOT: flash, ring, embers, gone - not a loop)    3 BURN  (the flame that rides a foe while it burns)
The burning PATCH left on the ground is the fire sheet's own GROUND row (fire.py), and the yantra, the reticle and the shadows are drawn as shapes in the game."""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fire as F                                  # the bands, the dither, the noise and the 32 px cell are fire.py's, so the two sheets sit together

S, Z, N, TAU = F.S, F.Z, F.N, F.TAU
XS, YS = F.XS, F.YS
BANDS = F.BANDS


def hot(v):
    return np.clip(v, 0.0, 1.0)


# ---------------------------------------------------------------- 0. HELD: a small ball of flame, tongues licking round it
def held(t):
    p = t * TAU
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    n = F.fbm(dx * 0.16 + math.cos(p) * 1.4, dy * 0.16 + math.sin(p) * 1.4, 101)
    lobes = 0.5 + 0.5 * np.sin(5 * th + p + 2.0 * n)                     # five tongues, turning once a loop
    r = 11.5 * (0.84 + 0.3 * lobes) * (0.88 + 0.26 * n)
    v = 1.0 - d / r
    v = v + 0.55 * np.exp(-(d / 4.6) ** 2)                               # the white-hot core
    img = F.quantise(hot(v) * (d < 17), 0.04)
    F.embers(img, t, 3, 10, 20, seed=7)
    return img


# ---------------------------------------------------------------- 1. BALL: a fireball dropping out of the sky, tail of embers up the cell
def ball(t):
    p = t * TAU
    hx, hy = 31.5, 44.0                                                    # the head sits low; the tail climbs the cell
    dx, dy = XS - hx, YS - hy
    d = np.hypot(dx, dy)
    u = hy - YS                                                            # up the tail from the head
    n = F.fbm(dx * 0.15 + math.cos(p) * 1.2, u * 0.12 - 1.8 * math.sin(p), 131)
    head = 1.0 - d / (10.5 * (0.85 + 0.35 * n))
    w = np.where(u > 0, 9.0 * np.clip(1 - u / 40.0, 0, 1) ** 0.8 + 0.6, 0.0)
    tail = 1.0 - np.abs(dx - 1.6 * np.sin(u * 0.28 - 2 * p)) / np.maximum(w * (0.6 + 0.8 * n), 0.5)
    tail = tail * np.clip(1.0 - u / 42.0, 0, 1) ** 0.7 * (u > 0)
    v = np.maximum(head, tail * 0.92) + 0.5 * np.exp(-(d / 4.2) ** 2)
    img = F.quantise(hot(v), 0.05)
    for k in range(6):                                                     # sparks shed from the tail
        ph = (t + F._hash(k, 3, 141)) % 1.0
        F.put(img, 31.5 + (F._hash(k, 4, 141) - 0.5) * 18, 40 - ph * 34, BANDS[1] if ph < 0.35 else BANDS[2] if ph < 0.7 else BANDS[3], 255 if ph < 0.85 else 130)
    return img


# ---------------------------------------------------------------- 2. BURST: the impact, a one-shot over eight frames (flash, shock ring, ember spray, smoke of char)
def burst(t):
    s = t * (N - 1) / (N - 1.0)                                            # 0 .. 1 over the eight frames (t is f/N, so t is at most 7/8)
    s = min(1.0, t * N / (N - 1.0))
    dx, dy = XS - 31.5, YS - 31.5
    d = np.hypot(dx, dy)
    th = np.arctan2(dy, dx)
    n = F.fbm(dx * 0.1, dy * 0.1, 161 + int(s * 3))
    ring_r = 3 + 26 * (1 - (1 - s) ** 2)                                   # the shock ring runs out fast and slows
    ring = np.exp(-((d - ring_r) / (2.0 + 2.0 * s)) ** 2) * (1.0 - s) ** 1.4 * (0.35 + 0.65 * min(1.0, s * 4))
    core = np.exp(-(d / (3.5 + 12 * math.sin(math.pi * min(1.0, s * 1.5)))) ** 2) * max(0.0, 1.4 - 1.5 * s)
    spray = (0.5 + 0.5 * np.sin(9 * th + 6 * n)) * np.exp(-((d - ring_r * 0.85) / 4.0) ** 2) * (1 - s) * 0.8
    v = hot(ring * 0.95 + core + spray * 0.6) * (0.75 + 0.4 * n) * (1.0 - 0.9 * s ** 3)
    img = F.quantise(v, 0.06)
    for k in range(10):                                                    # embers thrown outward, cooling as they go
        a = k * 2.399963 + F._hash(k, 5, 171) * 0.5
        r = 4 + (6 + 24 * F._hash(k, 6, 171)) * s
        if r < 30:
            F.put(img, 31.5 + math.cos(a) * r, 31.5 + math.sin(a) * r, BANDS[1] if s < 0.3 else BANDS[2] if s < 0.55 else BANDS[3] if s < 0.8 else BANDS[4], 255 if s < 0.85 else 120)
    return img


# ---------------------------------------------------------------- 3. BURN: the little flame that rides a burning foe (tip up, base at the foot of the cell)
def burn(t):
    p = t * TAU
    u = 62.0 - YS
    dx = XS - 31.5
    L = 36 + 4 * math.sin(p) + 2 * math.sin(2 * p + 1)
    w = np.where(u < 4, 3.0 + u, 8.5 * np.clip(1 - u / L, 0, 1) ** 0.65 + 0.6)
    wob = 1.2 * np.sin(u * 0.35 - 2 * p) * np.clip(u / 20.0, 0, 1)
    n = F.fbm((dx - wob) * 0.16 + math.cos(p), u * 0.12 - 1.6 * math.sin(p), 191)
    v = 1 - np.abs(dx - wob) / (w * (0.62 + 0.7 * n))
    v = v * np.clip((L - u) / L + 0.2, 0, 1) ** 0.6 + 0.45 * np.exp(-(dx / 3.0) ** 2) * np.clip(1 - u / (L * 0.8), 0, 1)
    v = np.where((u < -1) | (u > L), -1, v)
    img = F.quantise(v, 0.05)
    for k in range(3):
        ph = (t + F._hash(k, 7, 201)) % 1.0
        F.put(img, 31.5 + (F._hash(k, 8, 201) - 0.5) * 12, 56 - ph * 40, BANDS[2] if ph < 0.6 else BANDS[3], 255)
    return img


ROWS = [("HELD", held), ("BALL", ball), ("BURST (one-shot)", burst), ("BURN (on a foe)", burn)]


def build():
    sheet = Image.new("RGBA", (N * S, len(ROWS) * S), (0, 0, 0, 0))
    for r, (_, fn) in enumerate(ROWS):
        for f in range(N):
            sheet.alpha_composite(Image.fromarray(fn(f / N), "RGBA"), (f * S, r * S))
    return sheet


def cell(sheet, row, frame):
    return sheet.crop((frame * S, row * S, (frame + 1) * S, (row + 1) * S))


def preview(sheet, scale=6):
    pad, lab = 6, 130
    W = lab + N * S * scale + pad
    H = len(ROWS) * (S * scale + pad) + pad
    bg = Image.new("RGBA", (W, H), (24, 20, 22, 255))
    d = ImageDraw.Draw(bg)
    for r, (name, _) in enumerate(ROWS):
        y = pad + r * (S * scale + pad)
        d.rectangle([lab, y, lab + N * S * scale // 2, y + S * scale], fill=(40, 36, 38))
        d.rectangle([lab + N * S * scale // 2, y, lab + N * S * scale, y + S * scale], fill=(14, 12, 14))
        row = sheet.crop((0, r * S, N * S, (r + 1) * S)).resize((N * S * scale, S * scale), Image.NEAREST)
        bg.alpha_composite(row, (lab, y))
        d.text((6, y + S * scale // 2 - 6), f"{r + 1}  {name}", fill=(255, 220, 140, 255))
    return bg


def additive(base, img, x, y, alpha=1.0, scale=1):
    """Lay a sprite on the scene the way the game does: additive light."""
    if scale != 1:
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    a = np.asarray(img).astype(np.float32)
    b = np.asarray(base).astype(np.float32)
    x0, y0 = int(x), int(y)
    h, w = a.shape[:2]
    ys, xs = max(0, y0), max(0, x0)
    ye, xe = min(b.shape[0], y0 + h), min(b.shape[1], x0 + w)
    if ye <= ys or xe <= xs:
        return base
    src = a[ys - y0:ye - y0, xs - x0:xe - x0]
    k = (src[..., 3:4] / 255.0) * alpha
    b[ys:ye, xs:xe, :3] = np.clip(b[ys:ye, xs:xe, :3] + src[..., :3] * k, 0, 255)
    return Image.fromarray(b.astype(np.uint8), "RGBA")


def scene(sheet, scale=3, frames=4):
    """The release, mocked: the pilot's hull at the foot, the reticle ahead, balls falling, bursts, patches. Four moments side by side."""
    fire = Image.open(os.path.join("build", "fire_sheet.png")).convert("RGBA") if os.path.exists(os.path.join("build", "fire_sheet.png")) else None
    cw, ch = 200, 150
    out = Image.new("RGBA", (cw * frames * scale + 8 * (frames + 1), ch * scale + 16), (24, 20, 22, 255))
    for m in range(frames):
        pane = Image.new("RGBA", (cw, ch), (16, 14, 18, 255))
        d = ImageDraw.Draw(pane)
        cx, cy, R = 100, 60, 46
        for g in range(0, cw, 20):
            d.line([(g, 0), (g, ch)], fill=(24, 22, 28, 255))
        for g in range(0, ch, 20):
            d.line([(0, g), (cw, g)], fill=(24, 22, 28, 255))
        # the reticle: a ring that grew with the charge, a faint fill
        d.ellipse([cx - R, cy - R, cx + R, cy + R], outline=(150, 60, 30, 255))
        d.ellipse([cx - R + 3, cy - R + 3, cx + R - 3, cy + R - 3], outline=(90, 40, 24, 255))
        d.polygon([(100, 140), (94, 150), (106, 150)], fill=(150, 200, 230, 255))   # the hull, a mark
        pane = additive(pane, cell(sheet, 0, (m * 2) % N), 94 - 16, 118, 0.9)         # a held shard at the yantra before release
        hits = [(-24, -18, 0), (20, -26, 1), (-6, 8, 2), (30, 14, 3), (-34, 18, 4), (4, -38, 5), (14, 30, 6), (-18, -4, 7)]
        for i, (ox, oy, k) in enumerate(hits):
            born = 0.7 * i / len(hits)
            lt = (m + 1) / frames - born                                       # seconds-ish since this one was loosed (0..1)
            px, py = cx + ox, cy + oy
            if lt <= 0:
                continue
            if lt < 0.30:                                                       # falling: the shadow grows, the ball is higher up as it nears
                fall = lt / 0.30
                d2 = ImageDraw.Draw(pane)
                r = 2 + 8 * fall
                d2.ellipse([px - r, py - r * 0.6, px + r, py + r * 0.6], fill=(8, 6, 8, 255))
                pane = additive(pane, cell(sheet, 1, (i * 3 + m) % N), px - 16, py - 16 - int(70 * (1 - fall)), 1.0)
            else:                                                               # burst, then the patch
                b = min(N - 1, int((lt - 0.30) / 0.15 * (N - 1)))
                if fire is not None and lt > 0.35:
                    pane = additive(pane, cell(fire, 2, (i + m) % N), px - 16, py - 16, min(0.8, (lt - 0.30) * 2))
                if lt < 0.6:
                    pane = additive(pane, cell(sheet, 2, b), px - 16, py - 16, 1.0)
        # one foe, burning
        pane = additive(pane, cell(sheet, 3, (m * 3) % N), 140, 20, 1.0)
        d3 = ImageDraw.Draw(pane)
        d3.rectangle([148, 44, 156, 52], fill=(120, 110, 100, 255))
        out.alpha_composite(pane.resize((cw * scale, ch * scale), Image.NEAREST), (8 + m * (cw * scale + 8), 8))
    return out


if __name__ == "__main__":
    os.makedirs("build", exist_ok=True)
    if not os.path.exists("build/fire_sheet.png"):
        F.build().save("build/fire_sheet.png")
    sh = build()
    sh.save("build/astra_agni.png")
    preview(sh).convert("RGB").save("build/astra_agni_preview.png")
    scene(sh).convert("RGB").save("build/astra_agni_scene.png")
    print(sh.size)

"""THE MANDALAS: what opens round the hull while an astra is held (the invocation), drawn so you can judge the design before it is wired into AstraFx.

The Akasha mandala today is two rings and two counter-turning triangles. The enriched one is built in LAYERS that open as the charge builds, so the pilot reads the
charge off the picture: every stage adds a ring of new detail, the whole shrinks toward the hull as it tightens, and a full charge pulses and spills light outward.

    python tools/enemy_art/mandala.py   ->  build/mandala_agni.png   (the Agni mandala at 25%, 60% and full charge, beside Akasha's current one)

Drawn additively (light, not paint), on a dark field, 4x supersampled and shrunk so the hairlines are soft like the game's."""
import math
import os

import numpy as np
from PIL import Image

TAU = math.pi * 2
SS = 4                      # supersample
SZ = 320                    # a panel, in game pixels
CX = CY = SZ / 2


class Canvas:
    def __init__(self):
        self.a = np.zeros((SZ * SS, SZ * SS, 3), dtype=np.float32)

    def _stamp(self, x, y, w, col, alpha):
        r = max(1, int(w * SS / 2))
        xi, yi = int(x * SS), int(y * SS)
        x0, x1, y0, y1 = max(0, xi - r), min(self.a.shape[1], xi + r + 1), max(0, yi - r), min(self.a.shape[0], yi + r + 1)
        if x1 <= x0 or y1 <= y0:
            return
        self.a[y0:y1, x0:x1] += np.array(col, dtype=np.float32) * alpha

    def line(self, x0, y0, x1, y1, w, col, alpha):
        n = max(2, int(math.hypot(x1 - x0, y1 - y0) * SS / max(1, w * SS / 3)))
        for i in range(n + 1):
            t = i / n
            self._stamp(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w * 1.25, col, alpha / max(1.0, w * SS / 3) * 1.5)

    def ring(self, r, w, col, alpha, dash=0, spin=0.0, n=160):
        for i in range(n):
            if dash and (i // dash) % 2:
                continue
            a0, a1 = spin + i * TAU / n, spin + (i + 1) * TAU / n
            self.line(CX + r * math.cos(a0), CY + r * math.sin(a0), CX + r * math.cos(a1), CY + r * math.sin(a1), w, col, alpha)

    def poly(self, pts, w, col, alpha):
        for i in range(len(pts)):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % len(pts)]
            self.line(x0, y0, x1, y1, w, col, alpha)

    def disc(self, x, y, r, col, alpha):
        for k in range(5, 0, -1):
            self._stamp(x, y, r * 2 * k / 5, col, alpha / 5)

    def image(self):
        img = np.clip(self.a, 0, 1)
        img = (img ** 0.9 * 255).astype(np.uint8)
        im = Image.fromarray(img, "RGB").resize((SZ, SZ), Image.LANCZOS)
        return im


GOLD = (1.0, 0.86, 0.5)
VERM = (1.0, 0.34, 0.12)
EMBER = (1.0, 0.58, 0.18)
WHITE = (1.0, 1.0, 1.0)


def pt(r, a):
    return CX + r * math.cos(a), CY + r * math.sin(a)


def petal(c, a, r0, r1, half, col, alpha, w=1.2):
    """A pointed petal from r0 out to r1 at angle a, `half` radians wide at its widest."""
    left, right = [], []
    steps = 10
    for i in range(steps + 1):
        t = i / steps
        r = r0 + (r1 - r0) * t
        tt = t ** 0.75
        sp = half * 4 * tt * (1 - tt)                                   # a lens: pointed at both ends, fullest near the root
        left.append(pt(r, a - sp))
        right.append(pt(r, a + sp))
    pts = left + right[::-1]
    for i in range(len(pts) - 1):
        c.line(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], w, col, alpha)


def flame_glyph(c, a, r, size, col, alpha):
    """A small teardrop flame standing on the ring, point outward."""
    x, y = pt(r, a)
    ca, sa = math.cos(a), math.sin(a)
    nx, ny = -sa, ca
    tip = (x + ca * size * 1.5, y + sa * size * 1.5)
    l = (x + nx * size * 0.6, y + ny * size * 0.6)
    rr = (x - nx * size * 0.6, y - ny * size * 0.6)
    base = (x - ca * size * 0.4, y - sa * size * 0.4)
    c.poly([tip, l, base, rr], 1.1, col, alpha)
    c.disc(x, y, size * 0.35, WHITE, alpha * 0.6)


def akasha_now(k, t, full):
    """Today's yantra, for comparison: two rings, two turning triangles."""
    c = Canvas()
    r = 128 - 70 * k
    a = 0.14 + 0.34 * k
    c.ring(r, 1.4, (1.0, 0.86, 0.5), a * 2.2)
    c.ring(r * 0.62, 1.0, (1.0, 0.86, 0.5), a * 2.2)
    col = (0.74, 0.55, 1.0)
    for rot in (t * 0.6, math.radians(60) - t * 0.6):
        c.poly([pt(r, rot + i * TAU / 3) for i in range(3)], 1.5, col, a * 2.6)
    return c.image()


def agni(k, t, full):
    """The enriched Agni mandala. Stages open with the charge: 0 tick ring, .15 petals, .35 triangles, .55 glyphs + inner lotus, .8 bindu and spill."""
    c = Canvas()
    R = 128 - 62 * k                                              # the whole tightens toward the hull as it builds
    pulse = (0.82 + 0.18 * math.sin(t * 14)) if full else 1.0
    base = 0.30 + 0.5 * k

    # stage 0: the outer double ring and sixty-four ticks, turning slowly
    c.ring(R, 1.6, VERM, base * 1.3 * pulse)
    c.ring(R * 0.965, 0.8, GOLD, base * 0.9)
    for i in range(64):
        a = i * TAU / 64 + t * 0.12
        long_ = 3.2 if i % 8 == 0 else 1.7
        x0, y0 = pt(R * 1.0, a)
        x1, y1 = pt(R * 1.0 + long_, a)
        c.line(x0, y0, x1, y1, 1.0, GOLD if i % 8 == 0 else EMBER, base * (1.1 if i % 8 == 0 else 0.7))
    c.ring(R * 0.90, 0.9, EMBER, base * 0.7, dash=2, spin=-t * 0.25)       # a dashed ring turning the other way

    # stage 1 (k > .15): twelve lotus petals, then an inner set of twelve offset between them
    s1 = max(0.0, min(1.0, (k - 0.15) / 0.2))
    if s1 > 0:
        for i in range(12):
            a = i * TAU / 12 + t * 0.18
            petal(c, a, R * 0.62, R * 0.88, 0.17, VERM, base * 1.5 * s1)
            petal(c, a + TAU / 24, R * 0.60, R * 0.74, 0.10, GOLD, base * 1.0 * s1)
        c.ring(R * 0.60, 1.0, GOLD, base * 0.9 * s1)

    # stage 2 (k > .35): the two triangles, the fire (upward) one heavy, the water (downward) one a ghost; a hexagon in them
    s2 = max(0.0, min(1.0, (k - 0.35) / 0.2))
    if s2 > 0:
        rot = t * 0.35
        up = [pt(R * 0.58, rot - math.pi / 2 + i * TAU / 3) for i in range(3)]
        dn = [pt(R * 0.58, rot + math.pi / 2 + i * TAU / 3) for i in range(3)]
        c.poly(up, 2.2, VERM, base * 1.9 * s2)
        c.poly(dn, 1.0, GOLD, base * 0.9 * s2)
        hexa = [pt(R * 0.30, -rot * 1.4 + i * TAU / 6) for i in range(6)]
        c.poly(hexa, 1.1, EMBER, base * 1.2 * s2)
        c.ring(R * 0.44, 0.9, GOLD, base * 0.8 * s2, dash=3, spin=t * 0.4)

    # stage 3 (k > .55): eight flame glyphs on the petal ring, eight on the inner ring, and an eight-petal lotus at the heart
    s3 = max(0.0, min(1.0, (k - 0.55) / 0.2))
    if s3 > 0:
        for i in range(8):
            a = i * TAU / 8 + t * 0.25 + 0.1
            flame_glyph(c, a, R * 0.90, 4.2 + 1.2 * math.sin(t * 9 + i), EMBER, base * 1.5 * s3)
        for i in range(8):
            a = i * TAU / 8 - t * 0.3
            petal(c, a, R * 0.08, R * 0.27, 0.22, GOLD, base * 1.3 * s3, w=1.0)
        for i in range(6):                                          # six bindu dots on the hexagon's corners
            a = -t * 1.4 * 0.35 * 4 + i * TAU / 6
            x, y = pt(R * 0.30, -t * 0.35 * 1.4 + i * TAU / 6)
            c.disc(x, y, 1.8, WHITE, base * 1.2 * s3)

    # stage 4 (k > .8): the bindu, a breathing glow in the fire triangle, sparks rising from the circle, a ripple spilling out when full
    s4 = max(0.0, min(1.0, (k - 0.8) / 0.2))
    if s4 > 0:
        c.disc(CX, CY, 3.4 + 1.0 * math.sin(t * 10), WHITE, 0.95 * s4)
        c.disc(CX, CY, 9, VERM, 0.28 * s4 * pulse)
        for j in range(14):
            ph = (t * 0.55 + j * 0.0713) % 1.0
            a = j * 2.399963
            x, y = pt(R * (0.55 + 0.45 * ph), a)
            c.disc(x, y - 6 * ph, 1.3 * (1 - ph) + 0.4, GOLD, 0.9 * (1 - ph) * s4)
    if full:
        ph = (t * 1.8) % 1.0
        c.ring(R + ph * 56, 2.0, VERM, 0.6 * (1 - ph))
        ph2 = (t * 1.8 + 0.5) % 1.0
        c.ring(R + ph2 * 56, 1.2, GOLD, 0.35 * (1 - ph2))
    return c.image()


def sheet():
    t = 2.0
    panels = [("AKASHA mandala today (60%)", akasha_now(0.6, t, False)),
              ("AGNI 25%", agni(0.25, t, False)), ("AGNI 60%", agni(0.6, t, False)), ("AGNI 100% (full, pulsing)", agni(1.0, t + 0.2, True))]
    out = Image.new("RGB", (SZ * 2 + 12, SZ * 2 + 12), (10, 8, 12))
    for i, (name, im) in enumerate(panels):
        out.paste(im, (4 + (i % 2) * (SZ + 4), 4 + (i // 2) * (SZ + 4)))
    return out


if __name__ == "__main__":
    os.makedirs("build", exist_ok=True)
    sheet().save("build/mandala_agni.png")
    print("ok")

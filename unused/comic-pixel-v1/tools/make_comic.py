"""Builds the three pages of the lore comic into docs/comic/page-1.png .. page-3.png.
    python tools/comic/make_comic.py
Illustrations are pixel art at native size (panels.py); each page is composed at 4x with temple-panel
borders, caption boxes and speech balloons in the game's own style (chamfered corners, a double
hairline, gold on ink)."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

import panels  # noqa: E402
from art import ASSETS, GOLD, GOLDM, INK, LAV, LAVD, VOID, WHITE, CREAM, PEARL, SKY  # noqa: E402

S = 4                      # native pixel -> page pixels
PW, PH = 320, 452          # a page, native
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "docs", "comic")

FONT = os.path.join(ASSETS, "fonts", "Rajdhani-SemiBold.ttf")  # the body face reads K as H at this size; Rajdhani does not
TITLE = os.path.join(ASSETS, "fonts", "Rajdhani-SemiBold.ttf")


def font(size, title=False):
    return ImageFont.truetype(TITLE if title else FONT, size)


def chamfer(box, c):
    x0, y0, x1, y1 = box
    return [(x0 + c, y0), (x1 - c, y0), (x1, y0 + c), (x1, y1 - c), (x1 - c, y1), (x0 + c, y1), (x0, y1 - c), (x0, y0 + c)]


def panel(page, key, box, c=16):
    """Render one panel's art into its chamfered window and give it the game's double hairline."""
    x0, y0, x1, y1 = [v * S for v in box]
    w, h = (x1 - x0) // S, (y1 - y0) // S
    art = getattr(panels, key)(w, h).resize((w * S, h * S), Image.NEAREST)
    mask = Image.new("L", art.size, 0)
    ImageDraw.Draw(mask).polygon(chamfer((0, 0, art.width - 1, art.height - 1), c), fill=255)
    page.paste(art.convert("RGB"), (x0, y0), mask)
    d = ImageDraw.Draw(page)
    d.polygon(chamfer((x0, y0, x1 - 1, y1 - 1), c), outline=GOLD, width=4)
    d.polygon(chamfer((x0 + 9, y0 + 9, x1 - 10, y1 - 10), max(4, c - 9)), outline=GOLDM, width=1)
    for cx, cy in [(x0 + c / 2, y0 + c / 2), (x1 - c / 2, y0 + c / 2), (x0 + c / 2, y1 - c / 2), (x1 - c / 2, y1 - c / 2)]:
        diamond(d, cx, cy, 6, GOLD)


def diamond(d, x, y, r, fill):
    d.polygon([(x, y - r), (x + r, y), (x, y + r), (x - r, y)], fill=fill)


def wrap(text, fnt, width, d):
    words, lines, cur = text.split(), [], ""
    for wd in words:
        t = (cur + " " + wd).strip()
        if d.textlength(t, font=fnt) <= width:
            cur = t
        else:
            lines.append(cur)
            cur = wd
    lines.append(cur)
    return lines


def caption(page, xy, text, width=520, size=27, tint=CREAM, name=None, align="left"):
    """A narrator's caption: an ink box with a gold bar down its left edge, set in the game's own face."""
    d = ImageDraw.Draw(page, "RGBA")
    f = font(size)
    lines = wrap(text, f, width - 40, d)
    lh = int(size * 1.35)
    h = len(lines) * lh + 26 + (22 if name else 0)
    x, y = xy
    d.polygon(chamfer((x, y, x + width, y + h), 10), fill=INK + (235,), outline=GOLDM)
    d.rectangle([x + 3, y + 10, x + 8, y + h - 10], fill=GOLD)
    ty = y + 13
    if name:
        d.text((x + 24, ty), name, font=font(19), fill=GOLD)
        ty += 22
    for ln in lines:
        d.text((x + 24, ty), ln, font=f, fill=tint)
        ty += lh
    return (x, y, x + width, y + h)


def balloon(page, xy, text, tail, width=420, size=27, tint=LAV, name="INDRA"):
    """A speech balloon in the panel language: chamfered, double hairline, a name tab, a tail to the speaker."""
    d = ImageDraw.Draw(page, "RGBA")
    f = font(size)
    lines = wrap(text, f, width - 36, d)
    lh = int(size * 1.35)
    h = len(lines) * lh + 30 + 14
    x, y = xy
    tx, ty = tail
    base = max(x + 30, min(x + width - 30, tx))
    d.polygon([(base - 16, y + h - 2), (base + 16, y + h - 2), (tx, ty)], fill=INK + (240,), outline=tint)
    d.polygon(chamfer((x, y, x + width, y + h), 12), fill=INK + (240,), outline=tint, width=2)
    d.polygon(chamfer((x + 6, y + 6, x + width - 6, y + h - 6), 8), outline=tint + (110,))
    d.line([(base - 14, y + h - 2), (base + 14, y + h - 2)], fill=INK, width=4)
    tabw = d.textlength(name, font=font(18)) + 34
    d.polygon(chamfer((x + 20, y - 14, x + 20 + tabw, y + 12), 6), fill=INK, outline=tint)
    d.text((x + 36, y - 11), name, font=font(18), fill=tint)
    ty2 = y + 20
    for ln in lines:
        d.text((x + 20, ty2), ln, font=f, fill=tint)
        ty2 += lh


def header(page, numeral, title, sub):
    d = ImageDraw.Draw(page, "RGBA")
    d.text((36 * 1, 14), "VAJRA OVERDRIVE", font=font(22, True), fill=GOLDM)
    t = f"{numeral}  -  {title}"
    d.text((PW * S - 36 - d.textlength(t, font=font(26, True)), 12), t, font=font(26, True), fill=GOLD)
    d.line([(36, 52), (PW * S - 36, 52)], fill=GOLDM, width=1)
    diamond(d, PW * S / 2, 52, 6, GOLD)
    d.text((PW * S / 2 - d.textlength(sub, font=font(18)) / 2, 58), sub, font=font(18), fill=(150, 145, 130))


def footer(page, n):
    d = ImageDraw.Draw(page, "RGBA")
    y = PH * S - 22
    d.line([(PW * S / 2 - 60, y), (PW * S / 2 + 60, y)], fill=GOLDM, width=1)
    diamond(d, PW * S / 2, y, 5, GOLD)
    d.text((PW * S / 2 - d.textlength(str(n), font=font(18)) / 2 - 80, y - 12), "", font=font(18), fill=GOLD)


def page1():
    page = Image.new("RGB", (PW * S, PH * S), INK)
    header(page, "I", "THE BOLT", "")
    panel(page, "p1a", (8, 22, 312, 180))
    panel(page, "p1b", (8, 185, 156, 316))
    panel(page, "p1c", (161, 185, 312, 316))
    panel(page, "p1d", (8, 321, 312, 446))
    caption(page, (46, 100), "IN SWARGA, INDRA KEPT THE THREE WORLDS. HE KEPT THEM WITH ONE WEAPON.", 470)
    caption(page, (46, 768), "THE VAJRAYUDHA WAS MADE FROM THE BONES OF THE SAGE DADHICHI, WHO GAVE THEM FREELY.", 540, 25)
    caption(page, (668, 768), "IT BROKE VRITRA. IT HELD THE SKY. IT NEVER LEFT INDRA'S HAND.", 540, 25)
    caption(page, (46, 1520), "THEN A SHADOW CROSSED THE SKY.", 470, 29)
    caption(page, (700, 1668), "TRIVIKRAMA.", 340, 38, GOLD)
    footer(page, 1)
    return page


def page2():
    page = Image.new("RGB", (PW * S, PH * S), INK)
    header(page, "II", "THE THIRD STEP", "")
    panel(page, "p2a", (8, 22, 112, 283))
    panel(page, "p2b", (117, 22, 312, 153))
    panel(page, "p2c", (117, 158, 312, 283))
    panel(page, "p2d", (8, 288, 312, 446))
    caption(page, (36, 700), "THE FIRST STEP COVERED THE EARTH. THE SECOND, THE SKY.", 380, 24)
    caption(page, (500, 128), "THE THIRD FELL ON SWARGA.", 440, 29)
    caption(page, (500, 688), "WHEN IT LIFTED, THE VAJRAYUDHA WAS GONE. TRIVIKRAMA BROKE IT IN SEVEN.", 520, 25)
    caption(page, (36, 1180), "AND HID ONE PIECE IN EACH LOKA BELOW.", 520, 29)
    # the loka names, down the right edge of the bands
    d = ImageDraw.Draw(page, "RGBA")
    top = 288 * S + 16 * S
    bh = (446 * S - top) / 7
    for i, (name, col, dark) in enumerate(panels.LOKAS):
        y = top + i * bh + bh / 2 - 12
        d.text((312 * S - 24 - d.textlength(name, font=font(21)), y), name, font=font(21), fill=col)
    footer(page, 2)
    return page


def page3():
    page = Image.new("RGB", (PW * S, PH * S), INK)
    header(page, "III", "THE ONES WHO HOLD A PIECE", "")
    panel(page, "p3a", (8, 22, 312, 170))
    panel(page, "p3b", (8, 175, 312, 262), 12)
    panel(page, "p3c", (8, 267, 156, 446))
    panel(page, "p3d", (161, 267, 312, 446))
    caption(page, (46, 100), "A PIECE OF THE BOLT TELLS ITS HOLDER ONE THING: THE WORLD AROUND YOU IS YOURS.", 600)
    caption(page, (46, 1000), "SEVEN LOKAS. SEVEN RULERS. NOBODY HERE IS A VILLAIN. THEY BELIEVE WHAT THE METAL SAYS.", 1150, 24)
    balloon(page, (40, 1180), "I AM INDRA. I AM TAKING IT BACK.", (330, 1400), 480)
    caption(page, (684, 1300), "ATALA. THE FIRST DOOR.", 520, 30)
    d = ImageDraw.Draw(page, "RGBA")
    tf = font(46, True)
    t = "VAJRA OVERDRIVE"
    d.text((161 * S + (151 * S - d.textlength(t, font=tf)) / 2, 446 * S - 100), t, font=tf, fill=GOLD)
    footer(page, 3)
    return page


def main():
    os.makedirs(OUT, exist_ok=True)
    for n, fn in enumerate((page1, page2, page3), 1):
        fn().save(os.path.join(OUT, f"page-{n}.png"))
        print("wrote page", n)


if __name__ == "__main__":
    main()

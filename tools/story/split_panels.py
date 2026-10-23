"""Cuts the three story pages (story_panels/N.png) into their separate panels.

    python tools/story/split_panels.py

The pages are manga-style spreads divided by thin white slanted gutters. A panel is a connected region
of non-gutter pixels; each is cleaned (edge-preserving smoothing, 2x Lanczos, gentle sharpen), its slanted
border is kept as transparency, and it is written to assets/story/pN-K.png in reading order (rows top to
bottom, then left to right inside a row). A contact sheet for review goes to build/story_sheet.png."""
import os

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage as ndi
from skimage.restoration import denoise_bilateral

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SRC = os.path.join(ROOT, "story_panels")
OUT = os.path.join(ROOT, "assets", "story")
MIN_AREA = 0.02  # a panel is at least 2% of the page; smaller regions are art inside a gutter


# A gutter the page drew without a white line (the top splash runs into the close-up under it):
# page -> [((x0, y0), (x1, y1))] in page pixels, cut through the mask.
CUTS = {1: [((0, 345), (1672, 433))]}


def gutters(arr, page=0):
    """True where the pixel is gutter white. Art highlights are rarely white over a whole line, so the
    mask is opened first to keep only thin LONG white runs."""
    white = (arr[..., 0] > 225) & (arr[..., 1] > 225) & (arr[..., 2] > 225)
    # keep only white that is part of a long straight run: open along both diagonals' worth of
    # directions by requiring a dilated-then-eroded line support
    lab, n = ndi.label(white, structure=np.ones((3, 3)))
    sizes = ndi.sum(white, lab, range(1, n + 1))
    keep = np.zeros_like(white)
    for i, s in enumerate(sizes, 1):
        if s > 150:  # a gutter segment is hundreds of pixels long; sparkles are not
            keep |= lab == i
    from PIL import ImageDraw
    cut = Image.new("L", (arr.shape[1], arr.shape[0]), 0)
    for a, b in CUTS.get(page, []):
        ImageDraw.Draw(cut).line([a, b], fill=255, width=6)
    keep |= np.asarray(cut) > 0
    return ndi.binary_dilation(keep, iterations=6)


def order(boxes):
    """Reading order: group into rows by vertical centre, then left to right."""
    items = sorted(boxes, key=lambda b: b["cy"])
    rows, cur = [], [items[0]]
    for b in items[1:]:
        if b["cy"] - np.mean([c["cy"] for c in cur]) > 110:
            rows.append(cur)
            cur = [b]
        else:
            cur.append(b)
    rows.append(cur)
    return [b for r in rows for b in sorted(r, key=lambda b: b["cx"])]


def clean(img):
    """Simplify: bilateral smoothing flattens the pixel noise but keeps edges; then 2x and sharpen."""
    a = np.asarray(img).astype(np.float32) / 255.0
    a = denoise_bilateral(a, sigma_color=0.03, sigma_spatial=1.0, channel_axis=-1)
    im = Image.fromarray((a * 255).clip(0, 255).astype(np.uint8))
    im = im.resize((im.width * 2, im.height * 2), Image.LANCZOS)
    return im.filter(ImageFilter.UnsharpMask(radius=2, percent=70, threshold=2))


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
    sheet_tiles = []
    for page in (1, 2, 3):
        img = Image.open(os.path.join(SRC, f"{page}.png")).convert("RGB")
        arr = np.asarray(img)
        g = gutters(arr, page)
        lab, n = ndi.label(~g)
        area = arr.shape[0] * arr.shape[1]
        boxes = []
        for i in range(1, n + 1):
            m = lab == i
            if m.sum() < MIN_AREA * area:
                continue
            ys, xs = np.where(m)
            boxes.append(dict(i=i, cx=xs.mean(), cy=ys.mean(), box=(xs.min(), ys.min(), xs.max() + 1, ys.max() + 1)))
        for k, b in enumerate(order(boxes), 1):
            m = lab == b["i"]
            m = ndi.binary_fill_holes(m)
            m = ndi.binary_erosion(m, iterations=1)  # shave the gutter's white fringe
            x0, y0, x1, y1 = b["box"]
            crop = img.crop((x0, y0, x1, y1))
            alpha = Image.fromarray((m[y0:y1, x0:x1] * 255).astype(np.uint8))
            alpha = alpha.filter(ImageFilter.GaussianBlur(1.2))  # a soft, un-jagged edge
            out = clean(crop).convert("RGBA")
            out.putalpha(alpha.resize(out.size, Image.LANCZOS))
            name = f"p{page}-{k}.png"
            out.save(os.path.join(OUT, name))
            print(name, out.size)
            tile = Image.new("RGB", (out.width // 3, out.height // 3), (20, 20, 30))
            tile.paste(out.resize(tile.size), mask=out.resize(tile.size).split()[3])
            sheet_tiles.append((name, tile))
    # contact sheet
    w = 4
    tw = max(t.width for _, t in sheet_tiles)
    th = max(t.height for _, t in sheet_tiles)
    rows = (len(sheet_tiles) + w - 1) // w
    sheet = Image.new("RGB", (w * (tw + 8), rows * (th + 8)), (40, 40, 50))
    from PIL import ImageDraw
    d = ImageDraw.Draw(sheet)
    for j, (name, t) in enumerate(sheet_tiles):
        x, y = (j % w) * (tw + 8), (j // w) * (th + 8)
        sheet.paste(t, (x, y))
        d.text((x + 6, y + 6), name, fill=(255, 255, 0))
    sheet.save(os.path.join(ROOT, "build", "story_sheet.png"))


if __name__ == "__main__":
    main()

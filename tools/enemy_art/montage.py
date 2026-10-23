"""One image with every enemy's frames: idle0, rage0, hit0, death mid, at 4x.
python tools/enemy_art/montage.py"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
import atala, kit

cols = [(0, 0), (4, 0), (0, 1), (1, 1), (3, 1), (5, 1), (0, 2), (0, 3)]
names = list(atala.ENEMIES)
scale = 2
W = len(cols) * 64
img = Image.new("RGBA", (W, 64 * len(names)), (13, 13, 26, 255))
for r, n in enumerate(names):
    sh = kit.build_sheet(atala.ENEMIES[n])
    for c, (cx, cy) in enumerate(cols):
        cell = sh.crop((cx * 64, cy * 64, cx * 64 + 64, cy * 64 + 64))
        img.alpha_composite(cell, (c * 64, r * 64))
img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
os.makedirs("build", exist_ok=True)
img.save("build/enemy_montage.png")
print(img.size)

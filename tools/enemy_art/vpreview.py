"""Contact sheet of the Vitala enemies: python tools/enemy_art/vpreview.py [scale] -> build/vitala_montage.png
columns: idle0 idle2 idle4 idle6 | rage0 rage4 | hit0 | death 0..5 | leader | elite"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
import vitala, kit

scale = int(sys.argv[1]) if len(sys.argv) > 1 else 3
cols = [(0, 0), (2, 0), (4, 0), (6, 0), (8, 0), (12, 0), (0, 1), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1), (7, 1), (0, 2), (0, 3)]
import vitala_keepers
ALL = dict(vitala.ENEMIES); ALL.update(vitala_keepers.KEEPERS)
names = list(ALL)
img = Image.new("RGBA", (len(cols) * 64, 64 * len(names)), (20, 16, 18, 255))
for r, n in enumerate(names):
    sh = kit.build_sheet(ALL[n])
    for c, (cx, cy) in enumerate(cols):
        img.alpha_composite(sh.crop((cx * 64, cy * 64, cx * 64 + 64, cy * 64 + 64)), (c * 64, r * 64))
img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
os.makedirs("build", exist_ok=True)
img.save("build/vitala_montage.png")
print(img.size)

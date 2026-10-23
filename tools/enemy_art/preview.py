"""Contact sheet: python tools/enemy_art/preview.py <name> [scale]  -> build/enemy_<name>.png"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
import importlib

name = sys.argv[1]
scale = int(sys.argv[2]) if len(sys.argv) > 2 else 3
mod = importlib.import_module("atala")
import kit
art = mod.ENEMIES[name]
sheet = kit.build_sheet(art)
bg = Image.new("RGBA", sheet.size, (13, 13, 26, 255))
bg.alpha_composite(sheet)
bg = bg.resize((bg.width * scale, bg.height * scale), Image.NEAREST)
os.makedirs("build", exist_ok=True)
bg.save(f"build/enemy_{name}.png")
print(bg.size)

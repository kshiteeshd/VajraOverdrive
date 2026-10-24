"""The boss sheet: python tools/enemy_art/bpreview.py -> build/vitala_boss.png"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image
import vitala_boss
sh = vitala_boss.build_boss_sheet()
bg = Image.new("RGBA", sh.size, (20, 16, 18, 255)); bg.alpha_composite(sh)
os.makedirs("build", exist_ok=True)
bg.save("build/vitala_boss.png")
print(bg.size)

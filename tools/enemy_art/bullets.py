"""The keepers' three rounds, drawn by hand on 16x16 (Atala: pearl, lavender, gold).
python tools/enemy_art/bullets.py  ->  assets/atala/bullets/<gun>.png"""
import os
from PIL import Image

PAL = {"K": (70, 56, 104), "L": (217, 208, 232), "l": (160, 140, 220), "W": (255, 252, 241),
       "H": (255, 255, 244), "G": (255, 215, 0), "g": (254, 194, 61), "B": (184, 134, 11),
       "A": (133, 77, 0), "c": (255, 244, 216)}

ART = {
    # the door-ward's shell: a heavy pearl stone ringed in lavender, a gold heart
    "dvarapala-shell": [
        "................",
        ".....KKKKKK.....",
        "...KKLLLLLLKK...",
        "..KLLLLWWLLLLK..",
        "..KLLWWWWWWLLK..",
        ".KLLWWHHHHWWLLK.",
        ".KLWWHHGGHHWWLK.",
        ".KLWWHGGGGHWWLK.",
        ".KLWWHGGGGHWWLK.",
        ".KLWWHHGGHHWWLK.",
        ".KLLWWHHHHWWLLK.",
        "..KLLWWWWWWLLK..",
        "..KLLLLWWLLLLK..",
        "...KKLLLLLLKK...",
        ".....KKKKKK.....",
        "................",
    ],
    # the lamp-keeper's spark: a small gold diamond with a white-hot core
    "dipaka-spark": [
        "................",
        "................",
        "................",
        ".......GG.......",
        "......gGGg......",
        ".....gGHHGg.....",
        "....gGHHHHGg....",
        "....gGHHHHGg....",
        ".....gGHHGg.....",
        "......gGGg......",
        ".......GG.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    # the bay-master's toll: a gold coin with a square hole and a lit rim
    "vanija-toll": [
        "................",
        ".....BBBBBB.....",
        "...BBGGGGGGBB...",
        "..BGGHHggggGGB..",
        "..BGHHggggggGB..",
        ".BGHgggggggggGB.",
        ".BGgggAAAAgggGB.",
        ".BGgggAKKAgggGB.",
        ".BGgggAKKAgggGB.",
        ".BGgggAAAAgggGB.",
        ".BGgggggggggGgB.",
        "..BGgggggggGGB..",
        "..BBGGggggGGBB..",
        "...BBBGGGGBBB...",
        ".....BBBBBB.....",
        "................",
    ],
}

out = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "atala", "bullets")
for name, rows in ART.items():
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), PAL[ch] + (255,))
    im.save(os.path.join(out, name + ".png"))
print("wrote", len(ART))

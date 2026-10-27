"""Render every sound effect: the first pass (sfx.py), then the second pass (sfx2.py) over it.   python tools/sound/render_all.py [prefix ...]"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import sfx
import sfx2  # noqa: F401  (registers its recipes over the first pass's)

if __name__ == '__main__':
    pre = sys.argv[1:]
    names = [k for k in sfx.REG if not pre or any(k.startswith(p) for p in pre)]
    os.makedirs(sfx.OUT, exist_ok=True)
    for k in names:
        sfx.render(k)
    print('rendered', len(names))

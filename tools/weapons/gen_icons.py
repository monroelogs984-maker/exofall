"""Material-ID maps (32x32) for the weapons' flat inventory sprites, rendered from the 3D models.

Each material becomes one flat key colour; textures-src/item/weapon/<name>.px then takes each key colour onto its own
layer and shades it. Thin parts (grips, hafts) are thickened first so they come out 2 px wide.
    python3 tools/weapons/gen_icons.py
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_weapons as G  # noqa: E402
from model3d import model_quads, render  # noqa: E402

OUT = os.path.join(G.ROOT, "textures-src", "item", "weapon_icon")
KEYS = ["#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#FF8000", "#8000FF", "#0080FF", "#80FF00"]
FAT = {"grip", "haft", "band", "band_low", "band_high", "pommel", "pommel_gem", "root_knot",
       }


def id_map(name):
    m, mats = G.BUILDERS[name]()
    for b in m.boxes:
        tag = (b.tag or "").split("_")[0] if b.tag not in FAT else b.tag
        if b.tag in FAT or tag in ("wing",):
            grow = 0.15 if b.tag in FAT else 0.0
            b.lo[0] -= grow
            b.hi[0] += grow
    names = sorted(mats)
    keyed = {k: (lambda c: (lambda ctx: c))(tuple(int(KEYS[i][j:j + 2], 16) for j in (1, 3, 5)) + (255,))
             for i, k in enumerate(names)}
    m.paint(keyed)
    q = model_quads(m)
    im = render(q, m.atlas, G.display_for(m)["gui"], size=256, extent=0.5, bg=(0, 0, 0), flat=True)
    a = np.array(im)[4::8, 4::8]
    alpha = np.where(a.sum(-1) == 0, 0, 255).astype(np.uint8)
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.dstack([a, alpha])).save(os.path.join(OUT, name + "_ids.png"))
    return {k: KEYS[i] for i, k in enumerate(names)}


if __name__ == "__main__":
    for n in G.BUILDERS:
        print(n, id_map(n))

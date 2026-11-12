"""Builds Exofall's four 3D weapon models + their texture atlases, and preview sheets.

    python3 tools/weapons/gen_weapons.py            # write models/textures into src/main/resources
    python3 tools/weapons/gen_weapons.py --preview DIR [name ...]   # also write preview PNGs to DIR
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mats as M  # noqa: E402
from model3d import Model, model_quads, render, sprite_quads, write  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "exofall")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
TEX_DIR = os.path.join(ASSETS, "textures", "item", "weapon")


def sq(m, cv, cw, half, u0, u1, mat, **k):
    """Square-section box centred on (v, w) = (cv, cw)."""
    return m.add(cv - half, cv + half, u0, u1, cw - half, cw + half, mat, **k)


# ---- Quadblade of the Sands (desert) --------------------------------------------------------------------------------
def quadblade():
    m = Model("quadblade_of_the_sands", density=2, scale=0.7)
    mats = {
        "gold": M.metal(M.GOLD, base=0.55, streak=0.8),
        "bronze": M.metal(M.PBRONZE, base=0.42, streak=0.5, jit=0.8),
        "blade": M.estoc(M.PBRONZE, grad=(9, 28, 1.0)),
        "wrap": M.leather_wrap(M.LEATHER),
        "turq": M.gem(M.TURQ),
    }
    # pommel + gem cap
    sq(m, 0, 0, 1.6, -8, -6, "gold", tag="pommel")
    sq(m, 0, 0, 1.0, -9, -8, "turq", tag="pommel_gem")
    # grip with gold bands
    sq(m, 0, 0, 1.0, -6, 5, "wrap", tag="grip")
    sq(m, 0, 0, 1.25, -6, -5.2, "gold", tag="band_low")
    sq(m, 0, 0, 1.25, 4.2, 5, "gold", tag="band_high")
    # neck flaring into the wide mount
    sq(m, 0, 0, 1.8, 5, 6.5, "bronze", tag="neck")
    sq(m, 0, 0, 4.0, 6.5, 9, "bronze", tag="mount")
    sq(m, 0, 0, 4.25, 8.4, 9.4, "gold", tag="mount_trim")
    sq(m, 0, 0, 2.6, 6, 6.5, "gold", tag="mount_lip")
    # turquoise gems on the mount's four faces
    m.add(-1, 1, 7, 8.6, 4.0, 4.5, "turq", tag="gem_s")
    m.add(-1, 1, 7, 8.6, -4.5, -4.0, "turq", tag="gem_n")
    m.add(4.0, 4.5, 7, 8.6, -1, 1, "turq", tag="gem_e")
    m.add(-4.5, -4.0, 7, 8.6, -1, 1, "turq", tag="gem_w")
    # four estoc blades in a + pattern, tapering and drawing slightly together near the tip
    for dv, dw in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        # 1.5x the old 18.6-long blades, thinner (estocs)
        segs = [(3.0, 0.55, 9.4, 27.0), (2.85, 0.42, 27.0, 32.5), (2.7, 0.3, 32.5, 35.8), (2.6, 0.16, 35.8, 37.3)]
        sq(m, dv * 3.0, dw * 3.0, 0.85, 9.4, 10.8, "gold", tag="collar")
        sq(m, dv * 3.0, dw * 3.0, 0.75, 19.4, 20.2, "gold", tag="mid_ring")
        for off, half, u0, u1 in segs:
            sq(m, dv * off, dw * off, half, u0, u1, "blade", tag="blade")
    # a sun-disc finial between the blades
    sq(m, 0, 0, 0.9, 9.4, 11.4, "gold", tag="finial")
    sq(m, 0, 0, 0.6, 11.4, 12.6, "turq", tag="finial_gem")
    return m, mats


# ---- Royal Grandhammer (hive) --------------------------------------------------------------------------------------
def grandhammer():
    m = Model("royal_grandhammer", density=2)
    mats = {
        "hgold": M.metal(M.HONEY, base=0.42, streak=0.6, jit=0.8),
        "gold": M.metal(M.GOLD, base=0.6, streak=0.6),
        "face": M.honeycomb(M.GOLD, M.HONEY[:6], cell=1.1),
        "comb": M.honeycomb(M.COMBGLOW, M.WAX[:4], cell=1.5),
        "wood": M.metal(M.DARKWOOD, base=0.5, streak=1.6, jit=0.6),
        "wrap": M.leather_wrap(M.LEATHER, pitch=1.3),
        "amber": M.gem(M.AMBER),
        "drip": M.metal(M.AMBER, base=0.65, streak=0.2, jit=0.4),
    }
    # pommel
    sq(m, 0, 0, 1.4, -9.5, -8, "gold", tag="pommel")
    sq(m, 0, 0, 0.9, -10.5, -9.5, "amber", tag="pommel_gem")
    # long haft, leather grip, wax-gold bands
    sq(m, 0, 0, 0.8, -8, 16, "wood", tag="haft")
    sq(m, 0, 0, 0.95, -6, 3, "wrap", tag="grip")
    for u in (4.5, 9, 13):
        sq(m, 0, 0, 1.05, u, u + 0.8, "hgold", tag="band")
    sq(m, 0, 0, 1.5, 15, 16.6, "gold", tag="collar")
    # head: honeycomb core in a gold frame
    m.add(-3.5, 3.5, 16.6, 25.4, -3.5, 3.5, "comb", tag="core")
    sq(m, 0, 0, 4.0, 16.4, 17.4, "gold", tag="frame_low")
    sq(m, 0, 0, 4.0, 24.6, 25.6, "gold", tag="frame_high")
    for s in (1, -1):
        m.add(s * 3.5, s * 7.5, 17.6, 24.4, -3.0, 3.0, "hgold", tag="arm")
        m.add(s * 4.5, s * 5.2, 17.2, 24.8, -3.3, 3.3, "gold", tag="arm_ring")
        # ends dip toward the haft
        m.add(s * 7.5, s * 9.2, 16.4, 23.8, -3.4, 3.4, "hgold", tag="end_in")
        m.add(s * 9.2, s * 10.8, 15.0, 22.8, -3.7, 3.7, "hgold", tag="end")
        m.add(s * 10.8, s * 11.8, 14.4, 23.2, -4.0, 4.0, "gold", tag="strike_face")
        m.add(s * 11.8, s * 12.2, 16.4, 21.2, -1.4, 1.4, "amber", tag="face_gem")
        # wax drips under the arms
        m.add(s * 5.8, s * 6.6, 16.4, 17.6, -0.4, 0.4, "drip", tag="drip")
        m.add(s * 8.0, s * 8.8, 15.4, 16.4, -0.5, 0.5, "drip", tag="drip")
        m.add(s * 8.2, s * 8.6, 14.6, 15.4, -0.3, 0.3, "drip", tag="drip")
    # crown spike with an amber gem
    sq(m, 0, 0, 1.2, 25.6, 26.6, "gold", tag="crown")
    sq(m, 0, 0, 0.7, 26.6, 28, "amber", tag="crown_gem")
    return m, mats


# ---- Glasswing Scythe (swamp) --------------------------------------------------------------------------------------
def scythe():
    m = Model("glasswing_scythe", density=2, render_type="minecraft:translucent", mirror=True)
    mats = {
        "reed": M.reed(M.REED, node_pitch=5.5, node_off=1.0),
        "root": M.metal(M.REED[:5], base=0.4, streak=1.2),
        "wrap": M.leather_wrap(gradramp_wrap(), pitch=1.2),
        "socket": M.chitin(M.VEIN, base=0.45),
        "eye": M.eye(M.BOGEYE, (26, 14, 4)),
        "wing": M.wing(M.MEMBRANE, M.VEIN, alpha=150),
    }
    sq(m, 0, 0, 1.0, -10.5, -9, "root", tag="root_knot")
    sq(m, 0, 0, 0.75, -9, 20, "reed", tag="haft")
    sq(m, 0, 0, 0.9, -5, 2, "wrap", tag="grip")
    # eye socket: a cup and four prongs clasping the glowing Bog Eye
    sq(m, 0, 0, 1.9, 19.0, 20.6, "socket", tag="cup")
    sq(m, 0, 0, 1.6, 20.4, 23.6, "eye", glow=True, tag="bog_eye")
    for dv in (1, -1):
        m.add(dv * 1.55, dv * 2.0, 20.0, 24.2, -0.5, 0.5, "socket", tag="prong")
    # wing blade: a chain of segments leaving the socket to the right and curling down towards the haft
    pv, pu = 1.8, 22.6
    segs = [(90, 6.5, 2.8, 0), (67.5, 5.5, 2.3, 3), (45, 4.5, 1.7, 2), (22.5, 3.0, 1.0, 0), (0, 1.6, 0.4, 0)]
    for i, (tilt, length, hw, cross) in enumerate(segs):
        b = m.add(pv - hw, pv + hw, pu - length, pu + 0.6, -0.8, -0.3, "wing", tilt=tilt, pivot=(pv, pu),
                  tag=f"wing_{i}")
        b.cross = cross
        if i == 2:
            b.stigma = (0.45, 0.7)
        a = math.radians(tilt)
        pv, pu = pv + length * math.sin(a), pu - length * math.cos(a)
    return m, mats


def gradramp_wrap():
    from model3d import gradramp
    return gradramp(6, "#1a1e10", "#2e3418", "#465022", "#5e6a2e")


# ---- Serrated Longsword (rainforest) -------------------------------------------------------------------------------
def longsword():
    m = Model("serrated_longsword", density=2)
    mats = {
        "claw": M.chitin(M.MANTIS, base=0.55),
        "edge": M.estoc(M.SILVER, grad=(5, 26, 1.0)),
        "gold": M.metal(M.GOLD, base=0.55, streak=0.7),
        "emerald": M.gem(M.EMERALD),
        "wrap": M.leather_wrap(gradramp_green_wrap(), pitch=1.3),
        "tooth": M.chitin(M.MANTIS, base=0.75),
    }
    sq(m, 0, 0, 1.2, -6.5, -5, "gold", tag="pommel")
    sq(m, 0, 0, 0.8, -7.5, -6.5, "emerald", tag="pommel_gem")
    sq(m, 0, 0, 0.75, -5, 4, "wrap", tag="grip")
    # guard: gold bar with emeralds, ends swept toward the tip
    m.add(-4.5, 4.5, 4, 5.4, -1.0, 1.0, "gold", tag="guard")
    m.add(-1.2, 1.2, 3.6, 5.8, -1.3, 1.3, "gold", tag="guard_core")
    m.add(-0.8, 0.8, 4.0, 5.4, 1.3, 1.7, "emerald", tag="guard_gem_s")
    m.add(-0.8, 0.8, 4.0, 5.4, -1.7, -1.3, "emerald", tag="guard_gem_n")
    for s in (1, -1):
        m.add(s * 4.5, s * 5.6, 4.6, 6.8, -0.8, 0.8, "gold", tag="guard_tip")
        m.add(s * 4.6, s * 5.5, 6.8, 7.6, -0.6, 0.6, "emerald", tag="guard_tip_gem")
    # blade: honed silver edge (right) fused to green mantis-claw chitin (left) with forward-raked teeth
    m.add(-0.3, 1.5, 5.4, 24, -0.4, 0.4, "edge", tag="edge")
    m.add(-1.8, -0.3, 5.4, 22.5, -0.6, 0.6, "claw", tag="claw")
    m.add(-1.2, 0.4, 24, 26, -0.4, 0.4, "edge", tag="edge_tip")
    m.add(-1.6, -0.2, 22.5, 25.0, -0.55, 0.55, "claw", tag="claw_tip")
    # hooked claw point, leaning back (left) like a mantis foreleg
    m.add(-1.0, 0.0, 25.0, 28.0, -0.45, 0.45, "claw", tilt=22.5, pivot=(-0.5, 25.0), tag="hook")
    for i, u in enumerate((7.5, 10.4, 13.3, 16.2, 19.1)):
        pv = -1.7
        m.add(pv - 0.55, pv + 0.45, u, u + 1.5, -0.45, 0.45, "claw", tilt=22.5, pivot=(pv, u), tag=f"tooth_{i}")
        m.add(pv - 0.3, pv + 0.2, u + 1.5, u + 2.5, -0.3, 0.3, "tooth", tilt=22.5, pivot=(pv, u), tag=f"tooth_tip_{i}")
    # emerald inlays along the claw
    for u in (9.0, 15.0):
        m.add(-1.4, -0.7, u, u + 1.0, 0.6, 0.8, "emerald", tag="inlay_s")
        m.add(-1.4, -0.7, u, u + 1.0, -0.8, -0.6, "emerald", tag="inlay_n")
    return m, mats


def gradramp_green_wrap():
    from model3d import gradramp
    return gradramp(6, "#0e1a0e", "#1c3018", "#2c4822", "#3e602c")


BUILDERS = {"quadblade_of_the_sands": quadblade, "royal_grandhammer": grandhammer, "glasswing_scythe": scythe,
            "serrated_longsword": longsword}


def display_for(m):
    k = 1.0 / m.scale
    dz = -90 if m.mirror else 0

    def hand(rot, tr, sc):
        # left-hand transforms get y/z negated by the game, so the extra turn is negated there too
        z = rot[2] + (dz if rot[1] < 0 else -dz)
        return {"rotation": [rot[0], rot[1], z], "translation": tr, "scale": [round(sc * k, 4)] * 3}
    gui = m.fit_display(15.5, rot=(0, 0, dz))
    fixed = dict(gui)
    fixed["rotation"] = [0, 180, 0]
    s = gui["scale"][0]
    return {
        "thirdperson_righthand": hand([0, -90, 55], [0, 4.0, 0.5], 0.85),
        "thirdperson_lefthand": hand([0, 90, -55], [0, 4.0, 0.5], 0.85),
        "firstperson_righthand": hand([0, -90, 25], [1.13, 3.2, 1.13], 0.68),
        "firstperson_lefthand": hand([0, 90, -25], [1.13, 3.2, 1.13], 0.68),
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "gui": gui,
        "fixed": fixed,
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [round(s * 0.6, 4)] * 3},
    }


def preview(m, out_dir):
    """Sheet: slot-size GUI (1x and 4x), a 3/4 view, and the in-hand framing beside a vanilla iron sword."""
    q = model_quads(m)
    disp = display_for(m)
    gui4 = render(q, m.atlas, disp["gui"], size=64, extent=0.5)
    gui1 = gui4.resize((16, 16), Image.BOX).resize((64, 64), Image.NEAREST)
    # held framing: as the hand transforms see it (scaled back up, mirrored models turned back onto the diagonal),
    # beside a vanilla iron sword in the same frame
    held = {"rotation": [0, 0, -90 if m.mirror else 0], "scale": [1 / m.scale] * 3}
    big = render(q, m.atlas, held, view=(-25, 35, 0), size=512, extent=1.6)
    front = render(q, m.atlas, held, size=512, extent=1.6)
    sq_, sim = sprite_quads(os.path.join(os.path.dirname(__file__), "ref", "iron_sword.png"))
    sword = np.array(render(sq_, sim, None, size=512, extent=1.6, bg=(0, 0, 0)))
    f = np.array(front)
    mask = sword.sum(-1) > 0
    f[mask] = (f[mask] * 0.35 + sword[mask] * 0.65).astype(np.uint8)
    front = Image.fromarray(f)
    sheet = Image.new("RGB", (64 * 2 + 512 * 3 + 40, 520), (24, 24, 28))
    sheet.paste(gui1, (4, 4))
    sheet.paste(gui4, (4, 72))
    sheet.paste(front, (136, 4))
    sheet.paste(big, (136 + 516, 4))
    close = render(q, m.atlas, held, view=(-25, 35, 0), size=512, extent=0.8)
    sheet.paste(close, (136 + 1032, 4))
    path = os.path.join(out_dir, m.name + "_preview.png")
    sheet.save(path)
    return path


def main():
    args = sys.argv[1:]
    pdir = None
    if args and args[0] == "--preview":
        pdir = args[1]
        args = args[2:]
        os.makedirs(pdir, exist_ok=True)
    names = args or list(BUILDERS)
    for name in names:
        m, mats = BUILDERS[name]()
        m.paint(mats)
        write(m, mats, MODEL_DIR, TEX_DIR, display_for(m))
        if pdir:
            print(preview(m, pdir))
    print("weapons:", ", ".join(names))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Shared parts for Exofall mob models: palette, paths, decor, bone chains, gait, grounding.

    python3 tools/mobs/gen_mobs.py            # all mobs
    python3 tools/mobs/preview.py <geo> <png> <out.png> [scale]   # front / side / 3/4 / top contact sheet

Coordinates are Blockbench/Bedrock pixels: feet at y=0, model faces north (-Z), 16px = 1 block.
Colours may carry an alpha suffix, e.g. 'wing@110' (wings are drawn see-through).

Jointed parts (legs, antennae, abdomen segments) are chains of bones: each segment is laid out
straight in model space and bent at its pivot by the bone rotation, so animations can add to it.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mobkit  # noqa: E402
from mobkit import cube, build, write_json, ASSETS  # noqa: E402,F401

mobkit.PALETTE.update({
    # Mudgrub: pale mud-tan larva, dark chitin head
    'grub': '#cdb68a', 'grub_fold': '#a68d5f', 'grub_back': '#b89f70', 'grub_head': '#4f3826',
    'grub_brow': '#3a281a', 'mandible': '#22160e', 'grub_eye': '#0e0a08', 'grub_leg': '#6b4c33',
    'spiracle': '#5e4a30',
    # Glasswing: metallic teal dragonfly, glassy wings
    'gw_body': '#2e8a80', 'gw_dark': '#173f44', 'gw_light': '#5cc4b4', 'gw_eye': '#7fe6d4', 'gw_face': '#c9d86a',
    'gw_leg': '#1d2a2c', 'gw_wing': '#d4f4f0', 'gw_vein': '#4f8f8a', 'gw_stigma': '#1d3a3a',
    # Bloodgnat: soot-black mosquito with a swollen blood-red abdomen
    'gn_body': '#2a2224', 'gn_stripe': '#8c8478', 'gn_belly': '#8e1f24', 'gn_belly_lt': '#c0393a',
    'gn_leg': '#1a1414', 'gn_band': '#9a948a', 'gn_eye': '#d0383c', 'gn_wing': '#c8c8d0', 'gn_plume': '#5a5250',
})


def geo(name):
    return os.path.join(ASSETS, 'geo', 'entity', f'{name}.geo.json')


def tex(name):
    return os.path.join(ASSETS, 'textures', 'entity', f'{name}.png')


def anim(name, animations):
    write_json(os.path.join(ASSETS, 'animations', 'entity', f'{name}.animation.json'),
               {'format_version': '1.8.0', 'animations': animations})


def sin(speed, amp, phase=0, base=None):
    s = f'math.sin(query.anim_time * {speed} + {phase}) * {amp}'
    return s if base is None else f'{base} + {s}'


# ---- decor ---------------------------------------------------------------------------------

def stripes_x(color, every=2, start=0):
    def d(put, w, h):
        for x in range(start, w, every):
            for y in range(h):
                put(x, y, color)
    return d


def stripes_y(color, every=2, start=0):
    def d(put, w, h):
        for y in range(start, h, every):
            for x in range(w):
                put(x, y, color)
    return d


def edge(color, side='right'):
    """A darker joint line on one edge of a face (right = back end on east/west faces)."""
    def d(put, w, h):
        for y in range(h):
            put(w - 1 if side == 'right' else 0, y, color)
    return d


def dots(color, pts):
    def d(put, w, h):
        for x, y in pts:
            put(x if x >= 0 else w + x, y if y >= 0 else h + y, color)
    return d


def wing_veins(vein, stigma=None):
    """Leading-edge vein along y=0, cross veins, a dark cell (pterostigma) near the tip."""
    def d(put, w, h):
        for x in range(w):
            put(x, 0, vein)
            if x % 4 == 1 and x < w - 2:
                for y in range(h):
                    put(x, y, vein)
        for x in range(w):
            if (x * 3) % 7 == 0:
                put(x, h // 2, vein)
        if stigma:
            for x in range(w - 4, w - 2):
                put(x, 0, stigma)
                put(x, 1, stigma)
    return d


def both(*decors):
    def d(put, w, h):
        for dd in decors:
            dd(put, w, h)
    return d


# ---- chains --------------------------------------------------------------------------------

def chain_x(prefix, parent, root, side, segs, bones, cubes):
    """A jointed limb laid out along +X (side=1, left) or -X (side=-1, right) from root (x, y, z).

    segs: list of (length, thickness or (height, depth), rotation [rx, ry, rz] for the LEFT side, colour, material).
    +rz bends a left limb down, +rx RAISES the far end of a +Z body segment (verified
    numerically 2026-11-11; use negative rx to droop a tail). Rotations apply X, then Y, then Z, so a
    yawed segment's own rz only rolls it: to pitch a forward-pointing limb, start with a 0-length joint
    that yaws, then bend the next segments with rz (they bend in the joint's turned frame).
    The right side mirrors X, so ry and rz flip sign. Returns the bone names.
    """
    x, y, z = root
    names = []
    for i, (length, t, rot, color, mat) in enumerate(segs):
        name = f'{prefix}{i}'
        r = [rot[0], rot[1] * side, rot[2] * side]
        bones.append(dict(name=name, parent=parent, pivot=[x, y, z], rotation=r))
        ox = x if side > 0 else x - length
        th, td = (t, t) if not isinstance(t, (tuple, list)) else t
        if length > 0:   # length 0 = a bare joint (e.g. a shoulder that only yaws the rest of the limb)
            cubes.append(cube(name, [ox, y - th / 2, z - td / 2], [length, th, td], color, mat=mat))
        x += length * side
        parent = name
        names.append(name)
    return names


def chain_z(prefix, parent, start_z, segs, bones, cubes):
    """Body segments laid out backwards along +Z. segs: (length, width, height, centre_y, rx, colour, mat, decor)."""
    z = start_z
    names = []
    for i, (length, w, h, cy, rx, color, mat, decor) in enumerate(segs):
        name = f'{prefix}{i}'
        bones.append(dict(name=name, parent=parent, pivot=[0, cy, z], rotation=[rx, 0, 0] if rx else None))
        cubes.append(cube(name, [-w / 2, cy - h / 2, z], [w, h, length], color, mat=mat, decor=decor))
        z += length
        parent = name
        names.append(name)
    return names, z


def box(bone, c, s, color, mat=None, decor=None, inflate=0.0, rim=True, noise=5, glow=False):
    """Cube by centre + size."""
    d = cube(bone, [c[0] - s[0] / 2, c[1] - s[1] / 2, c[2] - s[2] / 2], list(s), color, decor=decor,
             inflate=inflate, noise=noise, rim=rim, mat=mat)
    if glow:
        d['glow'] = True
    return d


def glowtex(name):
    return os.path.join(ASSETS, 'textures', 'entity', f'{name}_glowmask.png')


def leg_pairs(prefix, parent, pairs, bones, cubes):
    """pairs: list of (left root (x, y, z), segs). Mirrored onto the right. Returns [(names, pair, side)]."""
    legs = []
    for i, (root, segs) in enumerate(pairs):
        for side in (1, -1):
            names = chain_x(f'{prefix}{i}{"l" if side > 0 else "r"}', parent, (side * root[0], root[1], root[2]), side, segs, bones, cubes)
            legs.append((names, i, side))
    return legs


def gait(legs, speed, swing, lift, out=None):
    """Alternating tripod gait: L0, R1, L2 step together, then R0, L1, R2."""
    out = {} if out is None else out
    for names, i, side in legs:
        ph = 0 if (i + (side > 0)) % 2 == 0 else 180
        out[names[0]] = {'rotation': [0, sin(speed, swing * side, ph),
                                      f'math.max(0, math.sin(query.anim_time * {speed} + {ph + 90})) * {-lift * side}']}
    return out


def leg_sway(legs, speed, amp, out=None):
    out = {} if out is None else out
    for names, i, side in legs:
        out[names[0]] = {'rotation': [sin(speed, amp, i * 70 + (side > 0) * 40), 0, 0]}
    return out


def flap(wings, speed, amp, phase_of=lambda n: 0, extra=None):
    """wings: {bone: side}. Beats around the bone's rest angle."""
    out = {}
    for n, side in wings.items():
        out[n] = {'rotation': [0, 0, sin(speed, amp * side, phase_of(n))]}
    return out


def ground(bones, cubes, floor=0.0):
    """Shift the whole model so its lowest point in the rest pose sits at y = floor. Returns extents."""
    import preview
    jb = [dict(name=b['name'], pivot=b['pivot'], parent=b.get('parent'), rotation=b.get('rotation') or [0, 0, 0]) for b in bones]
    xf = preview.bone_transforms(jb)
    lo, hi, xs, zs = 1e9, -1e9, [], []
    for c in cubes:
        m = xf[c['bone']]
        (ox, oy, oz), (sx, sy, sz) = c['origin'], c['size']
        for dx in (0, sx):
            for dy in (0, sy):
                for dz in (0, sz):
                    p = (-(ox + dx), oy + dy, oz + dz)
                    y = m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2] + m[1][3]
                    x = m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2] + m[0][3]
                    z = m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2] + m[2][3]
                    lo, hi = min(lo, y), max(hi, y)
                    xs.append(x)
                    zs.append(z)
    dy = floor - lo
    for b in bones:
        b['pivot'] = [b['pivot'][0], b['pivot'][1] + dy, b['pivot'][2]]
    for c in cubes:
        c['origin'] = [c['origin'][0], c['origin'][1] + dy, c['origin'][2]]
    return dict(height=(hi - lo) / 16, width=(max(xs) - min(xs)) / 16, length=(max(zs) - min(zs)) / 16)


def make(name, bones, cubes, seed, bounds, glow=False, floor=0.0, grounded=True):
    ext = ground(bones, cubes, floor) if grounded else None
    build(name, bones, cubes, seed, geo(name), tex(name), f'geometry.exofall.{name}', bounds=bounds,
          out_glow=glowtex(name) if glow else None)
    if ext:
        print(f'   {name}: {ext["width"]:.2f} wide x {ext["length"]:.2f} long x {ext["height"]:.2f} tall (blocks)')


def outline(color):
    def d(put, w, h):
        for x in range(w):
            put(x, 0, color)
            put(x, h - 1, color)
        for y in range(h):
            put(0, y, color)
            put(w - 1, y, color)
    return d

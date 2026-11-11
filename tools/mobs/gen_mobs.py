#!/usr/bin/env python3
"""Generates Exofall mob models (GeckoLib geo.json), textures and animations.

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
from mobkit import cube, build, write_json, ASSETS  # noqa: E402

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

    segs: list of (length, thickness, rotation [rx, ry, rz] for the LEFT side, colour, material).
    The right side mirrors X, so ry and rz flip sign. Returns the bone names.
    """
    x, y, z = root
    names = []
    for i, (length, t, rot, color, mat) in enumerate(segs):
        name = f'{prefix}{i}'
        r = [rot[0], rot[1] * side, rot[2] * side]
        bones.append(dict(name=name, parent=parent, pivot=[x, y, z], rotation=r))
        ox = x if side > 0 else x - length
        cubes.append(cube(name, [ox, y - t / 2, z - t / 2], [length, t, t], color, mat=mat))
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


# ---- Mudgrub -------------------------------------------------------------------------------

def mudgrub():
    """Fat, ridged larva about 1.4 blocks long, head forward (-Z). Segments are a bone chain so the
    crawl ripples back along the body; three pairs of small jointed legs at the front, stubby
    prolegs under the belly, chitin head with brow plate and hooked mandibles."""
    bones = [dict(name='root', pivot=[0, 0, 0])]
    cubes = []
    side_decor = lambda: {'east': both(edge('grub_fold'), dots('spiracle', [(1, 3)])),
                          'west': both(edge('grub_fold', 'left'), dots('spiracle', [(-2, 3)])),
                          'up': stripes_y('grub_fold', 99, 0)}
    # (length, width, height, centre y, curl rx, colour, mat, decor). Body sits 1px up for the prolegs.
    segs = [
        (4, 12, 11, 6.5, 0, 'grub', 'flesh', side_decor()),
        (5, 14, 13, 7.5, 0, 'grub', 'flesh', side_decor()),
        (5, 15, 14, 8.0, 0, 'grub', 'flesh', side_decor()),
        (5, 14, 13, 7.5, 0, 'grub', 'flesh', side_decor()),
        (4, 12, 11, 6.5, 6, 'grub', 'flesh', side_decor()),
        (4, 9, 8, 5.0, 10, 'grub', 'flesh', side_decor()),
    ]
    names, _ = chain_z('seg', 'root', -8, segs, bones, cubes)
    # fold rings between segments (slightly narrower, darker) and a dorsal plate on each segment
    z = -8
    for (length, w, h, cy, _, _, _, _), name in zip(segs, names):
        cubes.append(cube(name, [-(w - 2) / 2, cy - (h - 2) / 2, z - 1], [w - 2, h - 2, 1], 'grub_fold', mat='flesh'))
        cubes.append(cube(name, [-(w - 6) / 2, cy + h / 2, z + 1], [w - 6, 1, length - 2], 'grub_back', mat='flesh'))
        z += length
    # prolegs: little nubs under the 4th and 5th segments
    for name, zz, w in (('seg3', 13, 14), ('seg4', 18, 12)):
        for sx in (-1, 1):
            cubes.append(cube(name, [sx * (w / 2 - 3) - 1, 0, zz], [2, 1, 2], 'grub_fold', mat='flesh'))

    # head
    bones.append(dict(name='head', parent='seg0', pivot=[0, 6, -8]))
    cubes += [
        cube('head', [-5, 1.5, -14], [10, 9, 6], 'grub_head', mat='chitin',
             decor={'east': dots('grub_eye', [(1, 2), (2, 3)]), 'west': dots('grub_eye', [(-2, 2), (-3, 3)])}),
        cube('head', [-4, 9.5, -13], [8, 2, 4], 'grub_brow', mat='chitin'),            # brow plate
        cube('head', [-3, 2.5, -15], [6, 3, 1], 'grub_brow', mat='leather'),           # labrum
        cube('head', [-4.5, 9, -15], [1, 1, 2], 'grub_leg'),                           # antenna nubs
        cube('head', [3.5, 9, -15], [1, 1, 2], 'grub_leg'),
    ]
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 2.5, 3, -15], rotation=[0, side * 18, 0]))
        cubes.append(cube(n, [side * 2.5 - (0 if side > 0 else 2), 2, -18], [2, 2, 3], 'mandible', mat='chitin'))
        cubes.append(cube(n, [side * 2.5 - (1 if side > 0 else 0) - (0 if side > 0 else 1) + (0 if side > 0 else 1), 2, -19], [1, 2, 1], 'mandible'))

    # three pairs of small jointed legs on the first three segments
    legs = []
    for i, (seg, zz, half) in enumerate((('seg0', -6, 6), ('seg1', -2, 7), ('seg2', 3, 7.5))):
        for side in (1, -1):
            legs.append(chain_x(f'leg{i}{"l" if side > 0 else "r"}', seg, (side * (half - 1), 3, zz), side, [
                (3, 2, [0, 0, 35], 'grub_leg', 'chitin'),
                (3, 1, [0, 0, 50], 'grub_head', 'chitin'),
            ], bones, cubes))

    build('mudgrub', bones, cubes, 11, geo('mudgrub'), tex('mudgrub'), 'geometry.exofall.mudgrub',
          bounds=(2, 1.5, 0.5))

    def ripple(speed, amp):
        out = {}
        for i, b in enumerate(names):
            out[b] = {'rotation': [sin(speed, amp * 0.6, -i * 60), sin(speed / 2, amp, -i * 60), 0]}
        out['head'] = {'rotation': [sin(speed, amp, 60), sin(speed / 2, amp * 1.5, 60), 0]}
        for j, leg in enumerate(legs):
            out[leg[0]] = {'rotation': [sin(speed * 2, 25, (j // 2) * 120 + (j % 2) * 180), 0, 0]}
        return out
    idle = ripple(90, 2)
    for side in ('l', 'r'):
        idle[f'mandible_{side}'] = {'rotation': [0, sin(240, 10 if side == 'l' else -10), 0]}
    anim('mudgrub', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.0, 'bones': ripple(360, 5)},
    })


# ---- Glasswing -----------------------------------------------------------------------------

def glasswing():
    """Dragonfly ~3.5 blocks long, ~4 wide. Wrap-around compound eyes, tilted two-part thorax,
    ten-segment abdomen chain ending in claspers, four veined glass wings with a leading-edge vein
    and dark wing spot, three pairs of jointed legs held forward like a basket."""
    bones = [dict(name='body', pivot=[0, 14, -4])]
    cubes = []
    # thorax: synthorax (big, striped) + prothorax
    bones.append(dict(name='thorax', parent='body', pivot=[0, 14, -4], rotation=[-8, 0, 0]))
    cubes += [
        cube('thorax', [-4, 10, -10], [8, 9, 12], 'gw_body', mat='chitin',
             decor={'east': stripes_x('gw_dark', 4, 1), 'west': stripes_x('gw_dark', 4, 2),
                    'up': stripes_x('gw_light', 7, 3)}),
        cube('thorax', [-3, 11, -13], [6, 6, 3], 'gw_dark', mat='chitin'),
    ]
    # head
    bones.append(dict(name='head', parent='body', pivot=[0, 15, -13]))
    cubes += [
        cube('head', [-1.5, 13, -15], [3, 3, 2], 'gw_dark', mat='leather'),            # neck
        cube('head', [1, 12, -21], [5, 7, 7], 'gw_eye', mat='facet'),                 # compound eyes
        cube('head', [-6, 12, -21], [5, 7, 7], 'gw_eye', mat='facet'),
        cube('head', [-2.5, 11, -22], [5, 6, 3], 'gw_face', mat='chitin',
             decor={'north': dots('gw_dark', [(1, 4), (3, 4)])}),                       # frons
        cube('head', [-2, 9, -22], [4, 2, 2], 'gw_dark', mat='chitin'),               # mouthparts
        cube('head', [-1.5, 17, -22], [1, 2, 1], 'gw_leg'),                            # antennae
        cube('head', [0.5, 17, -22], [1, 2, 1], 'gw_leg'),
    ]
    # abdomen: chain of ten tapering segments with a pale ring at each joint
    ab_decor = lambda: {'east': edge('gw_light', 'left'), 'west': edge('gw_light'), 'up': both(stripes_y('gw_dark', 99), edge('gw_light', 'left'))}
    segs = [(4, 6, 6, 14, 0, 'gw_body', 'chitin', ab_decor()),
            (4, 5, 5, 14, 0, 'gw_body', 'chitin', ab_decor())]
    segs += [(5, 4, 4, 14, 0, 'gw_body', 'chitin', ab_decor()) for _ in range(3)]
    segs += [(5, 3, 3, 14, 0, 'gw_body', 'chitin', ab_decor()) for _ in range(3)]
    segs += [(3, 3, 3, 14, 0, 'gw_dark', 'chitin', None)]
    abdomen, end = chain_z('ab', 'body', 2, segs, bones, cubes)
    for side in (1, -1):    # claspers
        n = f'clasper_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent=abdomen[-1], pivot=[side * 1, 14, end], rotation=[0, side * 15, 0]))
        cubes.append(cube(n, [side * 1 - 0.5, 13.5, end], [1, 1, 4], 'gw_dark', mat='chitin'))
    # wings: membrane (0 thick, see-through) + leading-edge vein; hindwings broader
    wings = {}
    for side in (1, -1):
        s = 'l' if side > 0 else 'r'
        for kind, zf, chord, span in (('f', -9, 5, 28), ('b', -3, 7, 26)):
            n = f'wing_{kind}{s}'
            wings[n] = side
            bones.append(dict(name=n, parent='thorax', pivot=[side * 3, 19, zf + 1], rotation=[0, 0, side * 4]))
            ox = side * 3 if side > 0 else -3 - span
            cubes.append(cube(n, [ox, 19, zf + 1], [span, 0, chord], 'gw_wing@110', rim=False, noise=2,
                              decor={'up': wing_veins('gw_vein', 'gw_stigma'), 'down': wing_veins('gw_vein', 'gw_stigma')}))
            cubes.append(cube(n, [ox, 18.5, zf], [span, 1, 1], 'gw_vein', mat='chitin'))
    # legs: femur, tibia, tarsus; held forward and folded under the thorax
    legs = []
    for i, (zz, fwd) in enumerate(((-11, 40), (-8, 15), (-5, -10))):
        for side in (1, -1):
            legs.append(chain_x(f'leg{i}{"l" if side > 0 else "r"}', 'thorax', (side * 3, 10.5, zz), side, [
                (6, 2, [0, fwd, 45], 'gw_dark', 'chitin'),
                (6, 1, [0, 0, 75], 'gw_leg', 'chitin'),
                (3, 1, [0, 0, -30], 'gw_leg', None),
            ], bones, cubes))

    build('glasswing', bones, cubes, 23, geo('glasswing'), tex('glasswing'), 'geometry.exofall.glasswing',
          bounds=(5, 3, 1))

    def flap(speed, amp):
        # front and back pairs beat out of phase, like a real dragonfly
        out = {}
        for n, side in wings.items():
            phase = 0 if n.startswith('wing_f') else 90
            out[n] = {'rotation': [0, 0, sin(speed, amp * side, phase)]}
        return out

    def abdomen_wave(speed, amp):
        return {b: {'rotation': [sin(speed, amp, -i * 40), 0, 0]} for i, b in enumerate(abdomen)}

    hover = {**flap(2400, 35), **abdomen_wave(90, 1.5)}
    hover['body'] = {'position': [0, sin(180, 0.8), 0]}
    fly = {**flap(2600, 40), **abdomen_wave(120, 1)}
    fly['body'] = {'rotation': [-8, 0, 0]}
    # Wind-up tell: hangs still, wings blur, abdomen curls down segment by segment, head drops to aim.
    windup = flap(3600, 45)
    for i, b in enumerate(abdomen):
        windup[b] = {'rotation': {'0.0': [0, 0, 0], '0.5': [-5 - i * 0.6, 0, 0]}}
    windup['head'] = {'rotation': {'0.0': [0, 0, 0], '0.5': [18, 0, 0]}}
    windup['body'] = {'position': [0, sin(1800, 0.3), 0]}
    for leg in legs:
        windup[leg[0]] = {'rotation': {'0.0': [0, 0, 0], '0.5': [-20, 0, 0]}}   # legs reach forward
    # Dive: wings swept back along the abdomen, abdomen straight, legs forward.
    dive = {'body': {'rotation': [-15, 0, 0]}}
    for n, side in wings.items():
        dive[n] = {'rotation': [0, (-55 if n.startswith('wing_f') else -65) * side, 0]}
    for leg in legs:
        dive[leg[0]] = {'rotation': [-30, 0, 0]}
    anim('glasswing', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': hover},
        'move.fly': {'loop': True, 'animation_length': 3.0, 'bones': fly},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': windup},
        'attack.dive': {'loop': True, 'animation_length': 1.0, 'bones': dive},
    })


# ---- Bloodgnat -----------------------------------------------------------------------------

def bloodgnat():
    """Mosquito ~1.5 blocks long with legs. Humped striped thorax, small head with big red eyes,
    a long needle proboscis between two palps, plumed antennae, a six-segment swollen blood-red
    abdomen, two narrow veined wings with halteres, and three pairs of very long jointed legs with
    pale knee bands, knees raised above the body."""
    bones = [dict(name='body', pivot=[0, 13, -1])]
    cubes = [
        cube('body', [-3, 10, -4], [6, 6, 7], 'gn_body', mat='chitin',
             decor={'up': stripes_x('gn_stripe', 99, 2), 'east': stripes_y('gn_stripe', 99, 1), 'west': stripes_y('gn_stripe', 99, 1)}),
        cube('body', [-2.5, 15.5, -3], [5, 2, 5], 'gn_body', mat='chitin',
             decor={'up': both(stripes_x('gn_stripe', 99, 1), stripes_x('gn_stripe', 99, 3))}),   # hump
    ]
    bones.append(dict(name='head', parent='body', pivot=[0, 12.5, -4], rotation=[15, 0, 0]))
    cubes += [
        cube('head', [-2, 10.5, -8], [4, 4, 4], 'gn_body', mat='chitin'),
        cube('head', [-3.5, 11, -7.5], [2, 3, 3], 'gn_eye', mat='facet'),
        cube('head', [1.5, 11, -7.5], [2, 3, 3], 'gn_eye', mat='facet'),
        cube('head', [-2, 11, -10], [1, 1, 3], 'gn_body'),     # palps
        cube('head', [1, 11, -10], [1, 1, 3], 'gn_body'),
    ]
    bones.append(dict(name='proboscis', parent='head', pivot=[0, 11.5, -8], rotation=[20, 0, 0]))
    cubes.append(cube('proboscis', [-0.5, 11, -18], [1, 1, 10], 'gn_leg', mat='chitin'))
    for side in (1, -1):
        n = f'antenna_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 1, 13.5, -8], rotation=[-35, side * 20, 0]))
        cubes.append(cube(n, [side * 1 - 0.5, 13, -14], [1, 1, 6], 'gn_plume'))
        cubes.append(cube(n, [side * 1 - 1.5, 13.5, -13], [3, 0, 4], 'gn_plume@200', rim=False))   # plume
    # abdomen: tilted up a little, swollen in the middle
    belly = lambda: {'up': stripes_x('gn_body', 99, 0), 'east': edge('gn_body'), 'west': edge('gn_body', 'left'),
                     'down': stripes_y('gn_belly_lt', 2, 1)}
    segs = [(3, 5, 5, 12, -12, 'gn_belly', 'flesh', belly()),
            (4, 7, 7, 12, 0, 'gn_belly', 'flesh', belly()),
            (4, 7, 6, 12, 0, 'gn_belly', 'flesh', belly()),
            (3, 6, 5, 12, 0, 'gn_belly', 'flesh', belly()),
            (3, 4, 4, 12, 4, 'gn_belly', 'flesh', belly()),
            (2, 2, 3, 12, 6, 'gn_body', 'chitin', None)]
    abdomen, _ = chain_z('ab', 'body', 3, segs, bones, cubes)
    # wings + halteres
    wings = []
    for side in (1, -1):
        n = f'wing_{"l" if side > 0 else "r"}'
        wings.append((n, side))
        bones.append(dict(name=n, parent='body', pivot=[side * 2.5, 17, -2], rotation=[0, side * -25, side * 8]))
        ox = side * 2.5 if side > 0 else -2.5 - 14
        cubes.append(cube(n, [ox, 17, -2], [14, 0, 3], 'gn_wing@100', rim=False, noise=2,
                          decor={'up': wing_veins('gn_body'), 'down': wing_veins('gn_body')}))
        cubes.append(cube(n, [ox, 16.5, -2.5], [14, 1, 1], 'gn_body'))
        cubes.append(cube('body', [side * 2.5 - 0.5, 16, 2], [1, 1, 1], 'gn_stripe'))   # haltere knob
    # legs: femur up-and-out, long tibia down, tarsus splayed; pale band at each knee
    legs = []
    for i, (zz, yaw) in enumerate(((-3, 35), (-0.5, 0), (2, -35))):
        for side in (1, -1):
            names = chain_x(f'leg{i}{"l" if side > 0 else "r"}', 'body', (side * 3, 10.5, zz), side, [
                (8, 1, [0, yaw, -40], 'gn_leg', 'chitin'),
                (10, 1, [0, 0, 110], 'gn_leg', None),
                (8, 1, [0, 0, -35], 'gn_leg', None),
            ], bones, cubes)
            legs.append(names)
            for k, n in enumerate(names[1:]):     # knee bands
                bx = (side * 3 + side * 8) if k == 0 else (side * 3 + side * 21)
                cubes.append(cube(n, [bx - (0 if side > 0 else 1), 10, zz - 0.5], [1, 1, 1], 'gn_band'))

    build('bloodgnat', bones, cubes, 37, geo('bloodgnat'), tex('bloodgnat'), 'geometry.exofall.bloodgnat',
          bounds=(3, 2.5, 0.6))
    buzz = {
        'body': {'position': [sin(140, 0.6), sin(220, 0.9), 0]},
        'proboscis': {'rotation': [sin(100, 4), 0, 0]},
    }
    for n, side in wings:
        buzz[n] = {'rotation': [0, sin(3000, -12 * side), sin(3000, 40 * side)]}
    for j, leg in enumerate(legs):
        buzz[leg[0]] = {'rotation': [sin(120, 6, j * 60), 0, 0]}
        buzz[leg[1]] = {'rotation': [0, 0, sin(150, 6, j * 45)]}
    for i, b in enumerate(abdomen):
        buzz[b] = {'rotation': [sin(140, 2, -i * 50), 0, 0]}
    bite = {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.1': [25, 0, 0], '0.35': [25, 0, 0], '0.5': [0, 0, 0]},
                 'position': {'0.0': [0, 0, 0], '0.1': [0, -1, -2], '0.35': [0, -1, -2], '0.5': [0, 0, 0]}},
        'proboscis': {'rotation': {'0.0': [0, 0, 0], '0.1': [10, 0, 0], '0.5': [0, 0, 0]}},
        'ab1': {'scale': {'0.1': [1, 1, 1], '0.35': [1.2, 1.2, 1.1], '0.5': [1, 1, 1]}},
        'ab2': {'scale': {'0.1': [1, 1, 1], '0.35': [1.2, 1.2, 1.1], '0.5': [1, 1, 1]}},
    }
    anim('bloodgnat', {
        'move.fly': {'loop': True, 'animation_length': 6.0, 'bones': buzz},
        'attack.bite': {'loop': False, 'animation_length': 0.5, 'bones': bite},
    })


if __name__ == '__main__':
    only = sys.argv[1:]
    for fn in (mudgrub, glasswing, bloodgnat):
        if not only or fn.__name__ in only:
            fn()

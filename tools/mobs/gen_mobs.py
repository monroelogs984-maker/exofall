#!/usr/bin/env python3
"""Generates Exofall mob models (GeckoLib geo.json), textures and animations.

    python3 tools/mobs/gen_mobs.py            # all mobs
    python3 tools/mobs/preview.py <geo> <png> <out.png>   # contact sheet to look at

Coordinates are Blockbench/Bedrock pixels: feet at y=0, model faces north (-Z), 16px = 1 block.
Colours may carry an alpha suffix, e.g. 'wing@110' (wings are drawn see-through).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import mobkit  # noqa: E402
from mobkit import cube, build, write_json, ASSETS  # noqa: E402

mobkit.PALETTE.update({
    # Mudgrub: pale mud-tan larva, dark chitin head
    'grub': '#c8b083', 'grub_band': '#9c8358', 'grub_head': '#4a3424', 'mandible': '#22160e', 'grub_eye': '#0e0a08',
    # Glasswing: metallic teal dragonfly, glassy wings
    'gw_body': '#2e8a80', 'gw_dark': '#173f44', 'gw_eye': '#8ff0d8', 'gw_leg': '#1d2a2c',
    'gw_wing': '#d4f4f0', 'gw_vein': '#5fa9a4',
    # Bloodgnat: soot-black mosquito with a swollen blood-red abdomen
    'gn_body': '#2a2224', 'gn_belly': '#8e1f24', 'gn_leg': '#1a1414', 'gn_eye': '#d0383c',
    'gn_wing': '#c8c8d0',
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

def bands(color, every=2):
    def d(put, w, h):
        for x in range(0, w, every):
            for y in range(h):
                put(x, y, color)
    return d


def rings(color):
    """Dark ring at the back edge of a segment's side/top faces."""
    def d(put, w, h):
        for y in range(h):
            put(w - 1, y, color)
    return d


def eye_dot(color, x=None, y=None):
    def d(put, w, h):
        put(w // 2 if x is None else x, h // 2 if y is None else y, color)
    return d


def wing_veins(color):
    def d(put, w, h):
        for x in range(w):
            put(x, 0, color)          # leading edge
            if x % 3 == 0:
                for y in range(h):
                    put(x, y, color)  # cross veins
    return d


# ---- Mudgrub -------------------------------------------------------------------------------

def mudgrub():
    """Fat segmented larva, head forward (-Z). Each segment is its own bone so the walk ripples."""
    bones = [
        dict(name='root', pivot=[0, 0, 0]),
        dict(name='head', parent='root', pivot=[0, 2.5, -6]),
        dict(name='seg1', parent='root', pivot=[0, 0, -4]),
        dict(name='seg2', parent='root', pivot=[0, 0, 0]),
        dict(name='seg3', parent='root', pivot=[0, 0, 4]),
        dict(name='tail', parent='root', pivot=[0, 0, 7]),
    ]
    side_band = {'east': rings('grub_band'), 'west': rings('grub_band'), 'up': bands('grub_band', 4)}
    cubes = [
        cube('head', [-2.5, 0.5, -9], [5, 4, 3], 'grub_head', mat='leather',
             decor={'north': lambda put, w, h: (put(1, 1, 'grub_eye'), put(3, 1, 'grub_eye'))}),
        cube('head', [-2, 0.5, -10], [1, 1, 1], 'mandible'),
        cube('head', [1, 0.5, -10], [1, 1, 1], 'mandible'),
        cube('seg1', [-3, 0, -6], [6, 5, 4], 'grub', mat='flesh', decor=side_band),
        cube('seg2', [-3.5, 0, -2], [7, 6, 4], 'grub', mat='flesh', decor=side_band),
        cube('seg3', [-3, 0, 2], [6, 5, 4], 'grub', mat='flesh', decor=side_band),
        cube('tail', [-2, 0, 6], [4, 3, 3], 'grub', mat='flesh', decor=side_band),
    ]
    build('mudgrub', bones, cubes, 11, geo('mudgrub'), tex('mudgrub'), 'geometry.exofall.mudgrub',
          bounds=(1.2, 1, 0.3))

    def ripple(speed, amp):
        out = {}
        for i, b in enumerate(['head', 'seg1', 'seg2', 'seg3', 'tail']):
            out[b] = {'position': [0, f'math.max(0, {sin(speed, amp, -i * 70)})', 0],
                      'rotation': [0, sin(speed / 2, amp * 3, -i * 60), 0]}
        return out
    idle = ripple(120, 0.3)
    idle['head']['rotation'] = [sin(90, 6), sin(45, 12), 0]
    anim('mudgrub', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.0, 'bones': ripple(360, 0.8)},
    })


# ---- Glasswing -----------------------------------------------------------------------------

def glasswing():
    """Dragonfly, ~1.5 blocks long. Four flat glass wings on the thorax, long abdomen behind."""
    bones = [
        dict(name='body', pivot=[0, 8, -4]),
        dict(name='head', parent='body', pivot=[0, 9, -8]),
        dict(name='abdomen', parent='body', pivot=[0, 9, -1]),
        dict(name='abdomen_tip', parent='abdomen', pivot=[0, 9, 8]),
        dict(name='legs', parent='body', pivot=[0, 7, -5], rotation=[25, 0, 0]),
        dict(name='wing_fl', parent='body', pivot=[1.5, 11, -6]),
        dict(name='wing_fr', parent='body', pivot=[-1.5, 11, -6]),
        dict(name='wing_bl', parent='body', pivot=[1.5, 11, -3]),
        dict(name='wing_br', parent='body', pivot=[-1.5, 11, -3]),
    ]
    wing_decor = {'up': wing_veins('gw_vein'), 'down': wing_veins('gw_vein')}
    cubes = [
        # thorax
        cube('body', [-2, 7, -8], [4, 4, 7], 'gw_body', mat='metal'),
        # head + big compound eyes
        cube('head', [-1.5, 7.5, -10], [3, 3, 2], 'gw_dark', mat='leather'),
        cube('head', [-3, 8, -11], [2, 3, 3], 'gw_eye', mat='glow'),
        cube('head', [1, 8, -11], [2, 3, 3], 'gw_eye', mat='glow'),
        # abdomen: two long segments
        cube('abdomen', [-1, 8, -1], [2, 2, 9], 'gw_body', mat='metal',
             decor={'east': bands('gw_dark', 3), 'west': bands('gw_dark', 3), 'up': lambda put, w, h: [put(x, y, 'gw_dark') for y in range(h) for x in range(w) if y % 3 == 0]}),
        cube('abdomen_tip', [-0.5, 8.5, 8], [1, 1, 7], 'gw_dark', mat='metal'),
        # legs: three pairs tucked under the thorax
        *[cube('legs', [x, 4.5, z], [1, 3, 1], 'gw_leg') for x in (-2, 1) for z in (-7, -5, -3)],
        # wings (zero thickness, see-through)
        cube('wing_fl', [1.5, 11, -7], [14, 0, 3], 'gw_wing@120', decor=wing_decor, rim=False, noise=2),
        cube('wing_fr', [-15.5, 11, -7], [14, 0, 3], 'gw_wing@120', decor=wing_decor, rim=False, noise=2),
        cube('wing_bl', [1.5, 11, -4], [12, 0, 3], 'gw_wing@120', decor=wing_decor, rim=False, noise=2),
        cube('wing_br', [-13.5, 11, -4], [12, 0, 3], 'gw_wing@120', decor=wing_decor, rim=False, noise=2),
    ]
    build('glasswing', bones, cubes, 23, geo('glasswing'), tex('glasswing'), 'geometry.exofall.glasswing',
          bounds=(2.5, 1.5, 0.5))

    def flap(speed, amp, base=8):
        # front and back pairs beat out of phase, like a real dragonfly
        return {
            'wing_fl': {'rotation': [0, 0, sin(speed, amp, 0, base)]},
            'wing_fr': {'rotation': [0, 0, sin(speed, -amp, 0, -base)]},
            'wing_bl': {'rotation': [0, 0, sin(speed, amp, 90, base)]},
            'wing_br': {'rotation': [0, 0, sin(speed, -amp, 90, -base)]},
        }
    hover = flap(2400, 35)
    hover['body'] = {'position': [0, sin(180, 0.6), 0]}
    hover['abdomen'] = {'rotation': [sin(90, 3), 0, 0]}
    fly = flap(2600, 40)
    fly['body'] = {'rotation': [-8, 0, 0]}
    fly['abdomen'] = {'rotation': [4, 0, 0]}
    # Wind-up tell: hangs still, wings blur, abdomen curls down and the head drops to aim.
    windup = flap(3600, 45)
    windup['abdomen'] = {'rotation': {'0.0': [0, 0, 0], '0.5': [-28, 0, 0]}}
    windup['abdomen_tip'] = {'rotation': {'0.0': [0, 0, 0], '0.5': [-20, 0, 0]}}
    windup['head'] = {'rotation': {'0.0': [0, 0, 0], '0.5': [18, 0, 0]}}
    windup['body'] = {'position': [0, sin(1800, 0.25), 0]}
    # Dive: wings swept back flat along the abdomen.
    dive = {
        'body': {'rotation': [-15, 0, 0]},
        'wing_fl': {'rotation': [0, -55, 4]}, 'wing_fr': {'rotation': [0, 55, -4]},
        'wing_bl': {'rotation': [0, -65, 4]}, 'wing_br': {'rotation': [0, 65, -4]},
        'abdomen': {'rotation': [6, 0, 0]},
    }
    anim('glasswing', {
        'misc.idle': {'loop': True, 'animation_length': 2.0, 'bones': hover},
        'move.fly': {'loop': True, 'animation_length': 2.0, 'bones': fly},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': windup},
        'attack.dive': {'loop': True, 'animation_length': 1.0, 'bones': dive},
    })


# ---- Bloodgnat -----------------------------------------------------------------------------

def bloodgnat():
    """Mosquito, ~half a block. Needle proboscis, swollen red abdomen, long dangling legs."""
    bones = [
        dict(name='body', pivot=[0, 5, 0]),
        dict(name='head', parent='body', pivot=[0, 6, -2]),
        dict(name='abdomen', parent='body', pivot=[0, 6, 1], rotation=[-15, 0, 0]),
        dict(name='legs_l', parent='body', pivot=[1, 5, -1], rotation=[0, 0, -20]),
        dict(name='legs_r', parent='body', pivot=[-1, 5, -1], rotation=[0, 0, 20]),
        dict(name='wing_l', parent='body', pivot=[1, 7, -1]),
        dict(name='wing_r', parent='body', pivot=[-1, 7, -1]),
    ]
    cubes = [
        cube('body', [-1, 5, -2], [2, 2, 3], 'gn_body', mat='leather'),
        cube('head', [-1, 5.5, -4], [2, 2, 2], 'gn_body', mat='leather',
             decor={'north': lambda put, w, h: (put(0, 0, 'gn_eye'), put(1, 0, 'gn_eye')),
                    'east': eye_dot('gn_eye', 0, 0), 'west': eye_dot('gn_eye', 1, 0)}),
        cube('head', [-0.5, 5.5, -8], [1, 1, 4], 'gn_leg'),  # proboscis
        cube('abdomen', [-1.5, 4.5, 1], [3, 3, 5], 'gn_belly', mat='flesh',
             decor={'up': bands('gn_body', 2)}),
        *[cube('legs_l', [1, 0, z], [1, 5, 1], 'gn_leg') for z in (-2, 0)],
        *[cube('legs_r', [-2, 0, z], [1, 5, 1], 'gn_leg') for z in (-2, 0)],
        cube('wing_l', [1, 7, -1], [6, 0, 2], 'gn_wing@110', rim=False, noise=2,
             decor={'up': wing_veins('gn_body'), 'down': wing_veins('gn_body')}),
        cube('wing_r', [-7, 7, -1], [6, 0, 2], 'gn_wing@110', rim=False, noise=2,
             decor={'up': wing_veins('gn_body'), 'down': wing_veins('gn_body')}),
    ]
    build('bloodgnat', bones, cubes, 37, geo('bloodgnat'), tex('bloodgnat'), 'geometry.exofall.bloodgnat',
          bounds=(1, 1, 0.3))
    buzz = {
        'wing_l': {'rotation': [0, sin(3000, 10, 0, -20), sin(3000, 40, 0, 10)]},
        'wing_r': {'rotation': [0, sin(3000, -10, 0, 20), sin(3000, -40, 0, -10)]},
        'body': {'position': [sin(140, 0.4), sin(220, 0.6), 0]},
        'legs_l': {'rotation': [sin(160, 8), 0, 0]},
        'legs_r': {'rotation': [sin(160, 8, 90), 0, 0]},
    }
    bite = {
        'body': {'rotation': {'0.0': [0, 0, 0], '0.1': [25, 0, 0], '0.35': [25, 0, 0], '0.5': [0, 0, 0]},
                 'position': {'0.0': [0, 0, 0], '0.1': [0, -0.5, -1], '0.35': [0, -0.5, -1], '0.5': [0, 0, 0]}},
        'abdomen': {'scale': {'0.1': [1, 1, 1], '0.35': [1.2, 1.2, 1.1], '0.5': [1, 1, 1]}},
    }
    anim('bloodgnat', {
        'move.fly': {'loop': True, 'animation_length': 2.0, 'bones': buzz},
        'attack.bite': {'loop': False, 'animation_length': 0.5, 'bones': bite},
    })


if __name__ == '__main__':
    mudgrub()
    glasswing()
    bloodgnat()

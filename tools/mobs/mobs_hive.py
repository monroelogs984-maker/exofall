"""Wasp hive meteor mobs."""
from mobparts import *  # noqa: F401,F403
import mobparts
from mobs_templates import wasp


def combwasp():
    """Worker: amber and black, ~1.2 blocks."""
    wasp('combwasp', {'dark': '#2a1e14', 'band': '#d89a2a', 'eye': '#1a1410', 'sting': '#3a2a1a',
                      'wing': '#c8b890', 'vein': '#6a5030'}, 51)


def stinger():
    """Soldier: 1.5x a worker, red-amber with waxy bronze armour plates, big mandibles, long sting."""
    wasp('stinger', {'dark': '#2a1410', 'band': '#b8562a', 'plate': '#9a7a46', 'eye': '#3a0e0a', 'sting': '#1a0a08',
                     'wing': '#b8a080', 'vein': '#5a3020'}, 53, k=1.5, kind='soldier')


def nursemaid():
    """Healer: fuzzy cream and honey, swollen abdomen, carries a glowing wax drop."""
    mobparts.mobkit.GLOW_KEYS.add('nursemaid_wax')
    wasp('nursemaid', {'dark': '#5a4026', 'band': '#e8c070', 'fuzz': '#f2e6c8', 'eye': '#2a1e14', 'sting': '#5a4026',
                       'wing': '#e8e0c8', 'vein': '#a08860', 'wax': '#ffd36a'}, 55, k=1.2, kind='nurse')


def waxmite():
    """Scavenger mite ~0.9 blocks: round waxy body with amber spots, domed back, small head with
    chelicerae and palps, eight splayed legs."""
    mobparts.mobkit.PALETTE.update({'wm_wax': '#efe6cf', 'wm_spot': '#c89a40', 'wm_leg': '#b89a60', 'wm_dark': '#6a4a20'})
    spots = dots('wm_spot', [(1, 1), (4, 2), (7, 1), (2, 4), (6, 4), (8, 3)])
    bones = [dict(name='body', pivot=[0, 5, 0])]
    cubes = [
        box('body', (0, 5, 0.5), (9, 6, 11), 'wm_wax', 'flesh', decor={'up': spots, 'east': spots, 'west': spots}),
        box('body', (0, 8.5, 1), (7, 1, 9), 'wm_wax', 'flesh', decor={'up': dots('wm_spot', [(2, 2), (5, 5), (3, 7)])}),
        box('body', (0, 9.5, 1.5), (4, 1, 5), 'wm_wax', 'flesh'),
    ]
    bones.append(dict(name='head', parent='body', pivot=[0, 4.5, -5]))
    cubes.append(box('head', (0, 4.5, -6), (4, 3, 2), 'wm_dark', 'chitin'))
    for side in (1, -1):
        n = f'chelicera_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 1, 4, -7]))
        cubes.append(box(n, (side * 1, 4, -8), (1, 1, 2), 'wm_dark', 'chitin'))
        chain_x(f'palp_{"l" if side > 0 else "r"}', 'head', (side * 2, 4.5, -6.5), side,
                [(0, 1, [0, 70, 0], 'wm_leg', None), (2, 1, [0, 0, 20], 'wm_leg', None), (2, 1, [0, 0, 30], 'wm_dark', None)], bones, cubes)
    legs = leg_pairs('leg', 'body', [
        ((4, 4.5, z), [(4, 1, [0, yaw, -30], 'wm_leg', 'chitin'), (5, 1, [0, 0, 85], 'wm_leg', None), (3, 1, [0, 0, 25], 'wm_dark', None)])
        for z, yaw in ((-3.5, 50), (-1, 15), (1.5, -15), (4, -50))
    ], bones, cubes)
    make('waxmite', bones, cubes, 57, bounds=(1.8, 1, 0.3))
    walk = gait(legs, 480, 14, 12)
    walk['body'] = {'position': [0, sin(960, 0.3), 0]}
    idle = {f'palp_{s}1': {'rotation': [0, 0, sin(200, 12, 0 if s == 'l' else 90)]} for s in 'lr'}
    anim('waxmite', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 0.75, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [20, 0, 0], '0.35': [0, 0, 0]}},
            'body': {'position': {'0.0': [0, 0, 0], '0.1': [0, 0, -1], '0.35': [0, 0, 0]}}}},
    })


def larva_cocoon():
    """Wax-silk pod ~1.1 blocks tall stuck to the floor with a wax blob. Amber veins glow through the
    silk; it twitches, faster as it gets close to hatching."""
    mobparts.mobkit.PALETTE.update({'lc_silk': '#ece2c6', 'lc_strand': '#d6c79e', 'lc_wax': '#e0b860', 'lc_vein': '#ff9a2a'})
    mobparts.mobkit.GLOW_KEYS.add('lc_vein')

    def veins(put, w, h):
        x = w // 2
        for y in range(h):
            x = max(1, min(w - 2, x + (1 if (y * 7) % 3 == 0 else -1 if (y * 5) % 4 == 0 else 0)))
            put(x, y, 'lc_vein')
            if y % 3 == 1:
                put(x + 1, y, 'lc_vein')
    bones = [dict(name='base', pivot=[0, 0, 0]), dict(name='pod', parent='base', pivot=[0, 1, 0])]
    cubes = [
        box('base', (0, 0.5, 0), (12, 1, 12), 'lc_wax', 'flesh'),
        box('base', (4, 1.5, 3), (3, 1, 3), 'lc_wax', 'flesh'),
        box('base', (-3, 1.5, -4), (4, 1, 3), 'lc_wax', 'flesh'),
        box('pod', (0, 3.5, 0), (8, 5, 8), 'lc_silk', 'cloth', decor={'all': veins}),
        box('pod', (0, 9, 0), (10, 6, 10), 'lc_silk', 'cloth', decor={'all': veins}),
        box('pod', (0, 14, 0), (8, 4, 8), 'lc_silk', 'cloth', decor={'all': veins}),
        box('pod', (0, 17, 0), (5, 2, 5), 'lc_silk', 'cloth'),
        box('pod', (0, 6.5, 0), (10, 1, 10), 'lc_strand', 'cloth', inflate=0.3),
        box('pod', (0, 11.5, 0), (10, 1, 10), 'lc_strand', 'cloth', inflate=0.3),
        box('pod', (0, 18.5, 0), (1, 2, 1), 'lc_strand', 'cloth'),
    ]
    make('larva_cocoon', bones, cubes, 59, bounds=(1.2, 1.5, 0.5), glow=True)
    anim('larva_cocoon', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': {
            'pod': {'rotation': [sin(60, 3), 0, sin(45, 3, 90)],
                    'scale': [f'1 + {sin(120, 0.03)}', f'1 + {sin(120, 0.03, 180)}', f'1 + {sin(120, 0.03)}']}}},
        'misc.shake': {'loop': False, 'animation_length': 0.5, 'bones': {
            'pod': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, 0, 12], '0.2': [0, 0, -10], '0.3': [0, 0, 6], '0.5': [0, 0, 0]}}}},
    })


ALL = [combwasp, stinger, nursemaid, waxmite, larva_cocoon]

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


def propolis_spitter():
    """Spitter: 1.1x a worker, dark resin-brown with a swollen abdomen whose belly glows amber
    through the plates, and a glowing resin crop under the head."""
    mobparts.mobkit.GLOW_KEYS.add('propolis_spitter_resin')
    wasp('propolis_spitter', {'dark': '#24160c', 'band': '#8a5422', 'eye': '#120a06', 'sting': '#24160c',
                              'wing': '#b89c70', 'vein': '#5a3a1a', 'resin': '#ffae2a'}, 59, k=1.1, kind='spitter')


def honeycomb_face(wall, cell):
    """Hex cells on a face: dark wax walls around lit cells."""
    def d(put, w, h):
        for y in range(h):
            for x in range(w):
                row = y // 3
                xx = x + (2 if row % 2 else 0)
                if y % 3 == 0 or xx % 4 == 0:
                    put(x, y, wall)
                elif (xx // 4 + row) % 3 == 0:
                    put(x, y, cell)
    return d


def honeyguard():
    """Hive guard ~1.7 blocks tall: a heavy, wingless wasp soldier walking on four legs, bronze
    plates over a dark banded body, huge mandibles, and its front legs braced behind a tall wax
    shield of honeycomb. The shield is its own bone so the game can hide it once broken."""
    pal = {'dark': '#2a1a10', 'band': '#c8862a', 'plate': '#a87a3a', 'wax': '#f0c860', 'wall': '#a8742a', 'cell': '#ffe09a',
           'eye': '#2a0e08', 'leg': '#3a2414', 'wing': '#b8a080'}
    P = lambda k: f'honeyguard_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})
    bones = [dict(name='body', pivot=[0, 17, 0], rotation=[-18, 0, 0])]
    cubes = [
        box('body', (0, 17, 0), (10, 9, 10), P('dark'), 'chitin', decor={'east': stripes_x(P('band'), 4, 1), 'west': stripes_x(P('band'), 4, 2)}),
        box('body', (0, 22, -0.5), (8, 1, 8), P('plate'), 'metal', decor={'up': outline(P('wall'))}),
        box('body', (5.5, 17, 0), (1, 6, 8), P('plate'), 'metal'),
        box('body', (-5.5, 17, 0), (1, 6, 8), P('plate'), 'metal'),
        box('body', (0, 16, 6), (3, 3, 2), P('dark'), 'chitin'),     # waist
        # folded stub wings: guards don't fly
        box('body', (2.5, 21.8, 6), (4, 0, 7), P('wing') + '@170', rim=False, decor={'up': stripes_y(P('leg'), 3, 1)}),
        box('body', (-2.5, 21.8, 6), (4, 0, 7), P('wing') + '@170', rim=False, decor={'up': stripes_y(P('leg'), 3, 1)}),
    ]
    bandc = [P('band'), P('dark')]
    segs = [(L, w, h, 16, rx, bandc[i % 2], 'chitin', {'up': edge(P('dark'), 'left'), 'east': edge(P('dark')), 'west': edge(P('dark'), 'left')})
            for i, (L, w, h, rx) in enumerate([(3, 8, 8, -35), (3, 10, 9, -8), (3, 10, 9, -8), (3, 8, 7, -8), (3, 5, 5, -8), (2, 3, 3, -8)])]
    abdomen, end = chain_z('ab', 'body', 7, segs, bones, cubes)
    cubes.append(box(abdomen[-1], (0, 16, end + 1.5), (1, 1, 3), P('dark'), 'chitin'))
    bones.append(dict(name='head', parent='body', pivot=[0, 18, -5], rotation=[18, 0, 0]))
    cubes += [
        box('head', (0, 18, -8), (9, 8, 6), P('band'), 'chitin', decor={'north': dots(P('dark'), [(4, 0), (4, 1), (3, 0), (5, 0)]), 'up': stripes_x(P('dark'), 99, 4)}),
        box('head', (4.2, 19, -8), (2, 5, 4), P('eye'), 'facet'),
        box('head', (-4.2, 19, -8), (2, 5, 4), P('eye'), 'facet'),
        box('head', (0, 22.5, -7), (6, 1, 3), P('plate'), 'metal'),
    ]
    for side in (1, -1):
        s_ = 'l' if side > 0 else 'r'
        n = f'mandible_{s_}'
        bones.append(dict(name=n, parent='head', pivot=[side * 2.5, 15, -11], rotation=[0, -25 * side, 0]))
        cubes += [box(n, (side * 2.5, 15, -13), (3, 2, 5), P('dark'), 'chitin'), box(n, (side * 1.5, 15, -15.5), (1, 2, 1), P('dark'), 'chitin')]
        chain_x(f'antenna_{s_}', 'head', (side * 2, 22, -10.5), side,
                [(0, 1, [0, 65, 0], P('dark'), None), (5, 1, [0, 0, -45], P('dark'), None), (7, 1, [0, 0, 60], P('band'), None)], bones, cubes)
    # the shield, braced on the forelegs
    bones.append(dict(name='shield', parent='body', pivot=[0, 14, -10], rotation=[18, 0, 0]))
    comb = honeycomb_face(P('wall'), P('cell'))
    cubes += [
        box('shield', (0, 15, -16), (18, 22, 2), P('wax'), 'flesh', decor={'north': both(comb, outline(P('wall'))), 'south': outline(P('wall'))}),
        box('shield', (0, 26.5, -16), (12, 1, 2), P('wax'), 'flesh', decor={'north': stripes_x(P('wall'), 4, 1)}),
        box('shield', (0, 3.5, -16), (12, 1, 2), P('wax'), 'flesh'),
        box('shield', (9.5, 15, -16), (1, 14, 2), P('wax'), 'flesh'),
        box('shield', (-9.5, 15, -16), (1, 14, 2), P('wax'), 'flesh'),
        box('shield', (0, 15, -17.5), (4, 4, 1), P('plate'), 'metal', decor={'north': outline(P('wall'))}),
    ]
    for side in (1, -1):   # forelegs: from the thorax down to the back of the shield
        chain_x(f'arm_{"l" if side > 0 else "r"}', 'body', (side * 4, 15, -3), side,
                [(0, 2, [0, 70, 0], P('leg'), None), (6, 2, [0, 0, 25], P('dark'), 'chitin'), (6, 2, [0, 0, -40], P('band'), 'chitin')], bones, cubes)
    legs = leg_pairs('leg', 'body', [
        ((4.5, 13.5, z), [(7, 2, [0, yaw, -15], P('dark'), 'chitin'), (10, 2, [0, 0, 100], P('band'), 'chitin'), (4, 1, [0, 0, -20], P('leg'), None)])
        for z, yaw in ((0.5, 10), (3.5, -40))
    ], bones, cubes)
    make('honeyguard', bones, cubes, 85, bounds=(3, 3, 1.2))
    ant = {f'antenna_{s}1': {'rotation': [0, 0, sin(110, 9, 0 if s == 'l' else 90)]} for s in 'lr'}
    idle = dict(ant)
    idle['body'] = {'position': [0, sin(60, 0.3), 0]}
    idle['ab0'] = {'rotation': [sin(60, 2), 0, 0]}
    walk = gait(legs, 260, 14, 12, dict(ant))
    walk['shield'] = {'position': [0, sin(520, 0.4), 0], 'rotation': [0, 0, sin(260, 1.5)]}
    walk['ab0'] = {'rotation': [sin(520, 2), 0, 0]}
    bash = {'shield': {'position': {'0.0': [0, 0, 0], '0.15': [0, 0, 2], '0.25': [0, 0, -5], '0.6': [0, 0, 0]},
                       'rotation': {'0.0': [0, 0, 0], '0.15': [6, 0, 0], '0.25': [-8, 0, 0], '0.6': [0, 0, 0]}},
            'body': {'position': {'0.0': [0, 0, 0], '0.15': [0, 0, 1], '0.25': [0, 0, -2], '0.6': [0, 0, 0]}}}
    anim('honeyguard', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.4, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.6, 'bones': bash},
        'misc.block': {'loop': False, 'animation_length': 0.3, 'bones': {
            'shield': {'rotation': {'0.0': [0, 0, 0], '0.05': [-5, 0, 3], '0.15': [3, 0, -2], '0.3': [0, 0, 0]}}}},
        'attack.bite': {'loop': False, 'animation_length': 0.35, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.35': [0, 0, 0]}},
            'mandible_l': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, -30, 0], '0.3': [0, 0, 0]}},
            'mandible_r': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, 30, 0], '0.3': [0, 0, 0]}}}},
    })


ALL = [combwasp, stinger, nursemaid, waxmite, larva_cocoon, propolis_spitter, honeyguard]

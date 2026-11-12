"""Desert beetle meteor mobs."""
from mobparts import *  # noqa: F401,F403
import mobparts


def _antennae(parent, root, pal_a, pal_b, bones, cubes, a=4, b=3):
    for side in (1, -1):
        chain_x(f'antenna_{"l" if side > 0 else "r"}', parent, (side * root[0], root[1], root[2]), side,
                [(0, 1, [0, 60, 0], pal_a, None), (a, 1, [0, 0, -35], pal_a, None), (b, 1, [0, 0, 25], pal_b, None)], bones, cubes)


def _ant_idle(out=None):
    out = {} if out is None else out
    for s in 'lr':
        out[f'antenna_{s}1'] = {'rotation': [0, sin(70, 10, 0 if s == 'l' else 120), sin(90, 6)]}
    return out


def sanddiver():
    """Burrowing ambusher ~1.3 blocks: fat round sand-speckled abdomen fringed with bristles, small
    thorax, flat head with huge sickle jaws that hook inward, six short sturdy legs."""
    mobparts.mobkit.PALETTE.update({'sd_sand': '#d8bf8a', 'sd_spot': '#8a6a3e', 'sd_dark': '#3a2a1a', 'sd_jaw': '#5a3a1e', 'sd_bristle': '#b89a62'})
    spots = dots('sd_spot', [(1, 1), (4, 1), (7, 1), (10, 1), (2, 3), (6, 3), (9, 3), (3, 5), (8, 5)])
    bones = [dict(name='body', pivot=[0, 5, 2])]
    cubes = [
        box('body', (0, 5, 5), (12, 6, 12), 'sd_sand', 'leather', decor={'up': spots, 'east': spots, 'west': spots}),
        box('body', (0, 8.5, 5), (9, 1, 9), 'sd_sand', 'leather', decor={'up': dots('sd_spot', [(2, 2), (6, 4), (4, 7)])}),
        box('body', (0, 4.5, 11.5), (8, 4, 2), 'sd_sand', 'leather'),
        box('body', (0, 5, -3), (8, 5, 5), 'sd_sand', 'leather', decor={'up': stripes_x('sd_spot', 99, 3)}),
    ]
    for z in (0.5, 3.5, 6.5, 9.5):
        for sx in (-1, 1):
            cubes.append(box('body', (sx * 6.5, 6, z), (1, 1, 1), 'sd_bristle'))
    bones.append(dict(name='head', parent='body', pivot=[0, 5, -5.5]))
    cubes += [
        box('head', (0, 5, -7.5), (7, 3, 4), 'sd_sand', 'leather', decor={'up': stripes_x('sd_dark', 99, 3)}),
        box('head', (3, 6.5, -8.5), (1, 1, 1), 'sd_dark'),
        box('head', (-3, 6.5, -8.5), (1, 1, 1), 'sd_dark'),
    ]
    jaws = []
    for side in (1, -1):
        jaws.append(chain_x(f'jaw_{"l" if side > 0 else "r"}', 'head', (side * 2.5, 5, -9.5), side, [
            (0, 2, [0, 35, 0], 'sd_jaw', None),
            (4, 2, [0, 0, 0], 'sd_jaw', 'chitin'),
            (4, (2, 1), [0, 30, 0], 'sd_jaw', 'chitin'),
            (3, 1, [0, 45, 0], 'sd_dark', 'chitin'),
        ], bones, cubes))
    legs = leg_pairs('leg', 'body', [
        ((3.5, 4, z), [(3, 2, [0, yaw, 40], 'sd_spot', 'leather'), (3, 1, [0, 0, 60], 'sd_dark', None), (2, 1, [0, 0, -20], 'sd_dark', None)])
        for z, yaw in ((-4.5, 40), (-3, 0), (-1.5, -40))
    ], bones, cubes)
    make('sanddiver', bones, cubes, 61, bounds=(2, 1.2, 0.3))
    jaw_open = lambda a: {j[0]: {'rotation': [0, a * (1 if j[0].startswith('jaw_l') else -1), 0]} for j in jaws}
    idle = {j[0]: {'rotation': [0, sin(60, 8 * (1 if j[0].startswith('jaw_l') else -1)), 0]} for j in jaws}
    strike = {j[0]: {'rotation': {'0.0': [0, 25 * s, 0], '0.08': [0, -25 * s, 0], '0.3': [0, 0, 0]}}
              for j, s in zip(jaws, (1, -1))}
    strike['head'] = {'rotation': {'0.0': [0, 0, 0], '0.08': [12, 0, 0], '0.3': [0, 0, 0]}}
    anim('sanddiver', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 0.9, 'bones': gait(legs, 400, 18, 14, {'body': {'rotation': [0, 0, sin(400, 2)]}})},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': strike},
        'misc.emerge': {'loop': False, 'animation_length': 0.45, 'bones': {
            'body': {'position': {'0.0': [0, -10, 0], '0.3': [0, 1, 0], '0.45': [0, 0, 0]},
                     'rotation': {'0.0': [-25, 0, 0], '0.3': [5, 0, 0], '0.45': [0, 0, 0]}},
            **{k: {'rotation': {'0.0': v['rotation'], '0.25': v['rotation'], '0.45': [0, 0, 0]}} for k, v in jaw_open(30).items()}}},
    })


def shieldback():
    """Armoured beetle ~1.4 blocks wide, low and heavy, rust shell with ridges, and a huge bronze
    shield plate across its front hiding the head. Thick legs."""
    mobparts.mobkit.PALETTE.update({'sb_shell': '#8a3a22', 'sb_ridge': '#5a2414', 'sb_plate': '#b08a4a', 'sb_emboss': '#7a5a2a',
                                    'sb_dark': '#2a1a12', 'sb_leg': '#3a2418'})
    bones = [dict(name='body', pivot=[0, 8, 3])]
    cubes = [
        box('body', (0, 8, 4), (16, 8, 18), 'sb_shell', 'chitin', decor={'up': stripes_x('sb_ridge', 4, 1), 'east': stripes_y('sb_ridge', 99, 5), 'west': stripes_y('sb_ridge', 99, 5)}),
        box('body', (0, 12.5, 4), (12, 1, 15), 'sb_shell', 'chitin', decor={'up': stripes_x('sb_ridge', 4, 1)}),
        box('body', (0, 13.5, 5), (8, 1, 10), 'sb_shell', 'chitin', decor={'up': stripes_x('sb_ridge', 99, 4)}),
        box('body', (0, 7, 13.5), (12, 6, 1), 'sb_ridge', 'chitin'),
    ]
    bones.append(dict(name='shield', parent='body', pivot=[0, 8, -5]))
    cubes += [
        box('shield', (0, 10, -6.5), (20, 13, 2), 'sb_plate', 'metal', decor={'north': outline('sb_emboss'), 'south': outline('sb_emboss')}),
        box('shield', (0, 10, -8), (12, 7, 1), 'sb_emboss', 'metal', decor={'north': both(outline('sb_plate'), dots('sb_plate', [(5, 3), (6, 3)]))}),
        box('shield', (0, 17, -6.5), (6, 1, 2), 'sb_plate', 'metal'),
    ]
    for sx in (-1, 1):
        cubes += [box('shield', (sx * 9.5, 16.5, -6.5), (2, 2, 2), 'sb_emboss', 'metal'),
                  box('shield', (sx * 10, 4.5, -6.5), (2, 2, 2), 'sb_emboss', 'metal')]
    bones.append(dict(name='head', parent='body', pivot=[0, 5, -6]))
    cubes.append(box('head', (0, 4.5, -8.5), (7, 4, 4), 'sb_dark', 'chitin'))
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 2, 3.5, -10.5], rotation=[0, -15 * side, 0]))
        cubes.append(box(n, (side * 2, 3.5, -12), (2, 2, 3), 'sb_leg', 'chitin'))
    legs = leg_pairs('leg', 'body', [
        ((7, 6, z), [(4, 3, [0, yaw, 35], 'sb_shell', 'chitin'), (5, 2, [0, 0, 55], 'sb_leg', 'chitin'), (3, 2, [0, 0, -25], 'sb_dark', None)])
        for z, yaw in ((-2, 35), (4, 0), (10, -35))
    ], bones, cubes)
    make('shieldback', bones, cubes, 63, bounds=(2.5, 1.8, 0.6))
    walk = gait(legs, 240, 14, 10)
    walk['body'] = {'rotation': [0, 0, sin(240, 2.5)], 'position': [0, sin(480, 0.3), 0]}
    anim('shieldback', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': {'body': {'position': [0, sin(60, 0.2), 0]}}},
        'move.walk': {'loop': True, 'animation_length': 1.5, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.5, 'bones': {
            'shield': {'rotation': {'0.0': [0, 0, 0], '0.15': [12, 0, 0], '0.25': [-10, 0, 0], '0.5': [0, 0, 0]}},
            'body': {'position': {'0.0': [0, 0, 0], '0.15': [0, 0, 1], '0.25': [0, 0, -2], '0.5': [0, 0, 0]}}}},
        'misc.block': {'loop': False, 'animation_length': 0.3, 'bones': {
            'shield': {'rotation': {'0.0': [0, 0, 0], '0.05': [-6, 0, 3], '0.15': [3, 0, -2], '0.3': [0, 0, 0]}}}},
    })


def ember_beetle():
    """Prey beetle ~0.8 blocks: charcoal shell with ember-orange edges, a glowing orange tail lamp
    and two glowing spots on the pronotum. Elytra lift and flutter."""
    mobparts.mobkit.PALETTE.update({'eb_shell': '#2a2422', 'eb_edge': '#7a3a1a', 'eb_glow': '#ff8a2a', 'eb_leg': '#1e1a18'})
    mobparts.mobkit.GLOW_KEYS.add('eb_glow')
    bones = [dict(name='body', pivot=[0, 5, 1])]
    cubes = [
        box('body', (0, 4.5, 1.5), (8, 4, 10), 'eb_shell', 'chitin', decor={'down': stripes_x('eb_glow', 3, 1)}),
        box('body', (0, 4.5, 8), (6, 3, 3), 'eb_glow', 'glow', glow=True),
        box('body', (0, 5.5, -5), (7, 3, 3), 'eb_shell', 'chitin', decor={'up': dots('eb_glow', [(1, 1), (5, 1)])}),
    ]
    for side in (1, -1):
        n = f'elytron_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='body', pivot=[side * 0.5, 7.5, -3.5]))
        cubes.append(box(n, (side * 2, 7, 1.5), (4, 2, 10), 'eb_shell', 'chitin', decor={'up': outline('eb_edge'), 'east' if side > 0 else 'west': stripes_y('eb_edge', 99, 1)}))
    bones.append(dict(name='head', parent='body', pivot=[0, 5, -6.5]))
    cubes.append(box('head', (0, 5, -7.5), (4, 3, 2), 'eb_shell', 'chitin', decor={'north': dots('eb_glow', [(1, 0), (2, 0)])}))
    _antennae('head', (1.5, 6, -8.5), 'eb_leg', 'eb_edge', bones, cubes)
    legs = leg_pairs('leg', 'body', [
        ((3, 3.5, z), [(3, 1, [0, yaw, 40], 'eb_leg', 'chitin'), (3, 1, [0, 0, 60], 'eb_leg', None), (2, 1, [0, 0, -20], 'eb_edge', None)])
        for z, yaw in ((-3, 40), (0, 0), (3, -40))
    ], bones, cubes)
    make('ember_beetle', bones, cubes, 65, bounds=(1.2, 1, 0.3), glow=True)
    idle = _ant_idle({'elytron_l': {'rotation': [sin(240, 4, 0, -6), 0, sin(240, -4, 0, -6)]},
                      'elytron_r': {'rotation': [sin(240, 4, 0, -6), 0, sin(240, 4, 0, 6)]}})
    anim('ember_beetle', {
        'misc.idle': {'loop': True, 'animation_length': 3.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 0.6, 'bones': gait(legs, 600, 18, 14, _ant_idle())},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': {'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.3': [0, 0, 0]}}}},
    })


def _ball(bone, c, cubes):
    for s in ((12, 6, 6), (6, 12, 6), (6, 6, 12), (10, 10, 6), (10, 6, 10), (6, 10, 10), (8, 8, 8)):
        cubes.append(box(bone, c, s, 'rl_ball', 'stone', decor={'all': dots('rl_ball_dk', [(1, 1), (4, 3), (6, 6), (2, 6), (7, 2)])}))


def roller():
    """Dung-roller beetle ~1.6 blocks with its boulder: blue-black shell with a steel sheen, serrated
    shovel head with a short horn, broad toothed digging forelegs, a packed sand-dung ball in front."""
    mobparts.mobkit.PALETTE.update({'rl_shell': '#1e2a3e', 'rl_sheen': '#4a6a9a', 'rl_dark': '#12161e', 'rl_leg': '#1a1e28',
                                    'rl_ball': '#8a6a42', 'rl_ball_dk': '#5e4628'})
    bones = [dict(name='body', pivot=[0, 7, 3])]
    cubes = [
        box('body', (0, 7, 4), (12, 7, 13), 'rl_shell', 'chitin', decor={'up': stripes_x('rl_sheen', 99, 2), 'east': stripes_y('rl_sheen', 99, 1), 'west': stripes_y('rl_sheen', 99, 1)}),
        box('body', (0, 11, 4), (9, 1, 10), 'rl_shell', 'chitin', decor={'up': stripes_x('rl_dark', 99, 4)}),
        box('body', (0, 7.5, -4.5), (11, 5, 5), 'rl_shell', 'chitin', decor={'up': stripes_x('rl_sheen', 99, 1)}),
    ]
    bones.append(dict(name='head', parent='body', pivot=[0, 6, -7]))
    cubes += [
        box('head', (0, 5.5, -8.5), (10, 2, 3), 'rl_shell', 'chitin', decor={'north': dots('rl_dark', [(0, 1), (2, 1), (4, 1), (6, 1), (8, 1)])}),
        box('head', (0, 7.5, -8.5), (2, 2, 2), 'rl_shell', 'chitin'),
    ]
    bones.append(dict(name='boulder', parent='body', pivot=[0, 7, -15]))
    _ball('boulder', (0, 7, -15.5), cubes)
    legs = leg_pairs('leg', 'body', [
        ((5, 5, -5), [(4, 2, [0, 50, 40], 'rl_shell', 'chitin'), (4, (1, 3), [0, 0, 50], 'rl_leg', 'chitin'), (2, 1, [0, 0, -20], 'rl_dark', None)]),
        ((6, 5, 1), [(5, 2, [0, 0, 35], 'rl_shell', 'chitin'), (6, 1, [0, 0, 55], 'rl_leg', None), (3, 1, [0, 0, -25], 'rl_dark', None)]),
        ((6, 5, 7), [(6, 2, [0, -40, 30], 'rl_shell', 'chitin'), (7, 1, [0, 0, 55], 'rl_leg', None), (3, 1, [0, 0, -25], 'rl_dark', None)]),
    ], bones, cubes)
    make('roller', bones, cubes, 67, bounds=(2.5, 1.5, 0.5))
    hind = [n for n, i, s in legs if i == 2]
    windup = {'body': {'rotation': {'0.0': [0, 0, 0], '0.8': [-14, 0, 0]}, 'position': {'0.0': [0, 0, 0], '0.8': [0, 1, 1]}}}
    for names in hind:
        windup[names[0]] = {'rotation': [0, sin(1200, 25), 0]}
    anim('roller', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': {'head': {'rotation': [sin(60, 3), 0, 0]}}},
        'move.walk': {'loop': True, 'animation_length': 1.0, 'bones': gait(legs, 360, 16, 12)},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': windup},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': {
            'body': {'rotation': {'0.0': [-14, 0, 0], '0.1': [6, 0, 0], '0.4': [0, 0, 0]}},
            'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [-15, 0, 0], '0.4': [0, 0, 0]}}}},
    })
    # the rolled boulder entity
    bones = [dict(name='ball', pivot=[0, 6, 0])]
    cubes = []
    _ball('ball', (0, 6, 0), cubes)
    make('rolled_boulder', bones, cubes, 69, bounds=(1, 1, 0.4))
    anim('rolled_boulder', {'misc.idle': {'loop': True, 'animation_length': 1.0, 'bones': {
        'ball': {'rotation': ['query.anim_time * -540', 0, 0]}}}})


def golden_scarab():
    """Rare scarab ~1 block: gold-encrusted domed shell studded with glowing turquoise gems, a
    raked head, dark-gold toothed legs. Placeholder name 'Gold-Encrusted Scarab'."""
    mobparts.mobkit.PALETTE.update({'gs_gold': '#e0b040', 'gs_dark': '#8a6418', 'gs_gem': '#38d0c0', 'gs_leg': '#5a4010'})
    mobparts.mobkit.GLOW_KEYS.add('gs_gem')
    bones = [dict(name='body', pivot=[0, 6, 1])]
    cubes = [
        box('body', (0, 6, 1.5), (10, 6, 11), 'gs_gold', 'metal', decor={'up': stripes_x('gs_dark', 99, 5), 'east': dots('gs_gem', [(3, 2), (8, 2)]), 'west': dots('gs_gem', [(3, 2), (8, 2)])}),
        box('body', (0, 9.5, 1.5), (8, 1, 9), 'gs_gold', 'metal', decor={'up': both(stripes_x('gs_dark', 99, 4), dots('gs_gem', [(1, 1), (6, 1), (1, 7), (6, 7)]))}),
        box('body', (0, 10.5, 2), (5, 1, 6), 'gs_gold', 'metal', decor={'up': stripes_x('gs_dark', 99, 2)}),
        box('body', (2.5, 10.5, -0.5), (1, 1, 1), 'gs_gem', 'glow', glow=True),
        box('body', (-2.5, 10.5, -0.5), (1, 1, 1), 'gs_gem', 'glow', glow=True),
        box('body', (0, 11.5, 2.5), (1, 1, 1), 'gs_gem', 'glow', glow=True),
        box('body', (0, 6.5, -5.5), (9, 4, 4), 'gs_gold', 'metal', decor={'up': dots('gs_gem', [(2, 1), (6, 1)])}),
    ]
    bones.append(dict(name='head', parent='body', pivot=[0, 5.5, -7.5]))
    cubes.append(box('head', (0, 5.5, -8.5), (7, 2, 3), 'gs_gold', 'metal', decor={'north': dots('gs_dark', [(0, 1), (2, 1), (4, 1), (6, 1)])}))
    _antennae('head', (2, 6, -9.5), 'gs_dark', 'gs_gold', bones, cubes, a=3, b=2)
    legs = leg_pairs('leg', 'body', [
        ((4, 4.5, z), [(3, 2, [0, yaw, 40], 'gs_dark', 'metal'), (4, 1, [0, 0, 55], 'gs_leg', 'chitin'), (2, 1, [0, 0, -20], 'gs_dark', None)])
        for z, yaw in ((-4, 40), (0, 0), (4, -40))
    ], bones, cubes)
    make('golden_scarab', bones, cubes, 71, bounds=(1.5, 1.2, 0.3), glow=True)
    anim('golden_scarab', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': _ant_idle()},
        'move.walk': {'loop': True, 'animation_length': 0.5, 'bones': gait(legs, 720, 18, 14, _ant_idle())},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': {'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.3': [0, 0, 0]}}}},
    })


def antlion():
    """Pit trapper ~1.6 blocks: broad ash-brown teardrop abdomen with dark chevrons and bristle
    tufts, a short hunched thorax, a flat wide head, and two long sickle jaws lined with pale teeth.
    Sits buried to the jaws at the bottom of its sand funnel (misc.buried)."""
    from mobs_templates import chevrons
    pal = {'body': '#7a6650', 'dark': '#3a2e22', 'chev': '#4e3e2c', 'bristle': '#a8946e', 'jaw': '#2e2014', 'tooth': '#d8c8a0', 'eye': '#120c08'}
    P = lambda k: f'antlion_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})
    bones = [dict(name='body', pivot=[0, 6, 0])]
    cubes = [
        box('body', (0, 6, 6), (14, 7, 12), P('body'), 'leather', decor={'up': both(chevrons(P('chev'), 3, 1), stripes_x(P('dark'), 99, 7)),
                                                                        'east': dots(P('chev'), [(2, 2), (6, 3), (9, 2)]), 'west': dots(P('chev'), [(2, 2), (6, 3), (9, 2)])}),
        box('body', (0, 6, 6), (12, 9, 9), P('body'), 'leather', decor={'up': chevrons(P('chev'), 3, 0)}),
        box('body', (0, 5.5, 13), (8, 5, 3), P('body'), 'leather'),
        box('body', (0, 5, 15), (4, 3, 2), P('dark'), 'leather'),
        box('body', (0, 6.5, -2.5), (9, 6, 5), P('body'), 'leather', decor={'up': stripes_x(P('dark'), 99, 4)}),
    ]
    for z in (1, 4, 7, 10):     # bristle tufts down both flanks
        for sx in (-1, 1):
            cubes.append(box('body', (sx * 7.5, 7 + (z % 2), z + 1), (1, 2, 1), P('bristle'), 'fur'))
    for x in (-3, 0, 3):
        cubes.append(box('body', (x, 10.8, 3), (1, 1, 1), P('bristle'), 'fur'))
    bones.append(dict(name='head', parent='body', pivot=[0, 6, -5]))
    cubes += [
        box('head', (0, 6, -7.5), (10, 3, 5), P('body'), 'leather', decor={'up': both(stripes_x(P('dark'), 99, 4), stripes_x(P('dark'), 99, 5))}),
        box('head', (4, 7.5, -8.5), (1, 1, 1), P('eye'), 'facet'),
        box('head', (-4, 7.5, -8.5), (1, 1, 1), P('eye'), 'facet'),
        box('head', (4.5, 7.5, -7.5), (1, 1, 1), P('eye'), 'facet'),
        box('head', (-4.5, 7.5, -7.5), (1, 1, 1), P('eye'), 'facet'),
    ]
    jaws = []
    for side in (1, -1):
        names = chain_x(f'jaw_{"l" if side > 0 else "r"}', 'head', (side * 3.5, 6, -10), side, [
            (0, 2, [0, 70, 0], P('jaw'), None),
            (5, 2, [0, 0, 0], P('jaw'), 'chitin'),
            (5, (2, 1), [0, 12, 0], P('jaw'), 'chitin'),
            (4, 1, [0, 28, 0], P('jaw'), 'chitin'),
        ], bones, cubes)
        jaws.append(names)
        for j, (seg, x0) in enumerate(((names[1], 1.5), (names[2], 1.5), (names[2], 3.5))):   # teeth on the inner edge
            root = side * 3.5
            cubes.append(box(seg, (root + side * (x0 + (5 if seg == names[2] else 0)), 6, -10 + 1.3), (1, 1, 1), P('tooth'), 'bone'))
    legs = leg_pairs('leg', 'body', [
        ((4, 4.5, z), [(3, 2, [0, yaw, 40], P('body'), 'leather'), (4, 1, [0, 0, 55], P('dark'), None), (2, 1, [0, 0, -20], P('dark'), None)])
        for z, yaw in ((-4, 45), (-2.5, 5), (-1, -40))
    ], bones, cubes)
    make('antlion', bones, cubes, 87, bounds=(2.2, 1.2, 0.3))
    side = lambda j: 1 if j[0].startswith('jaw_l') else -1
    snap = lambda wide: {j[0]: {'rotation': [0, wide * side(j), 0]} for j in jaws}
    buried = {'body': {'position': [0, -6, 0], 'rotation': [-8, 0, 0]}, 'head': {'rotation': [8, 0, 0]}}
    buried.update({j[0]: {'rotation': [0, f'{30 * side(j)} + math.sin(query.anim_time * 70) * {4 * side(j)}', 0]} for j in jaws})
    strike = {j[0]: {'rotation': {'0.0': [0, 30 * side(j), 0], '0.06': [0, -28 * side(j), 0], '0.3': [0, 0, 0]}} for j in jaws}
    strike['head'] = {'rotation': {'0.0': [0, 0, 0], '0.06': [10, 0, 0], '0.3': [0, 0, 0]}}
    anim('antlion', {
        'misc.idle': {'loop': True, 'animation_length': 5.0, 'bones': {j[0]: {'rotation': [0, sin(70, 6 * side(j)), 0]} for j in jaws}},
        'move.walk': {'loop': True, 'animation_length': 1.0, 'bones': gait(legs, 360, 16, 12, {'body': {'rotation': [0, 0, sin(360, 2)]}})},
        'misc.buried': {'loop': True, 'animation_length': 6.0, 'bones': buried},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': strike},
    })


def tomb_scorpion():
    """Tomb guardian scorpion ~1.8 blocks: black-lacquer plates trimmed in gold, faded linen
    wrappings round the tail, big pincers, the tail curled high over the back ending in a glowing
    turquoise sting (same gems as the Gold-Encrusted Scarab)."""
    pal = {'shell': '#2a2420', 'gold': '#c8962e', 'dark': '#141010', 'claw': '#3a2e26', 'sting': '#38d0c0', 'wrap': '#c8b890', 'eye': '#38d0c0'}
    P = lambda k: f'tomb_scorpion_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})
    mobparts.mobkit.GLOW_KEYS.update({P('sting'), P('eye')})
    trim = lambda: {'up': both(edge(P('gold'), 'left'), stripes_x(P('gold'), 99, 0)), 'east': edge(P('gold')), 'west': edge(P('gold'), 'left')}
    bones = [dict(name='body', pivot=[0, 6, -4])]
    cubes = [box('body', (0, 6, -4.5), (9, 4, 8), P('shell'), 'chitin',
                 decor={'up': both(outline(P('gold')), dots(P('eye'), [(3, 1), (5, 1)])), 'north': dots(P('eye'), [(1, 0), (7, 0)])})]
    mesos, z = chain_z('meso', 'body', -0.5, [(3, w, 4, 6, 0, P('shell'), 'chitin', trim()) for w in (10, 11, 11, 10, 9, 8)], bones, cubes)
    tail_segs = [(3, 5, 5, 6.5, 40, P('shell'), 'chitin', {'up': stripes_y(P('wrap'), 99, 1), 'east': stripes_x(P('wrap'), 99, 1), 'west': stripes_x(P('wrap'), 99, 1)}),
                 (3, 5, 5, 6.5, 40, P('shell'), 'chitin', {'up': stripes_y(P('gold'), 99, 2)}),
                 (3, 4, 4, 6.5, 38, P('shell'), 'chitin', {'up': stripes_y(P('wrap'), 99, 1), 'east': stripes_x(P('wrap'), 99, 1), 'west': stripes_x(P('wrap'), 99, 1)}),
                 (3, 4, 4, 6.5, 35, P('shell'), 'chitin', {'up': stripes_y(P('gold'), 99, 2)}),
                 (3, 3, 3, 6.5, 30, P('dark'), 'chitin', None)]
    tail, tz = chain_z('tail', mesos[-1], z, tail_segs, bones, cubes)
    cubes += [box(tail[-1], (0, 6.5, tz + 2), (4, 4, 4), P('gold'), 'metal'),
              box(tail[-1], (0, 6.5, tz + 5), (1, 1, 3), P('sting'), 'glow', glow=True),
              box(tail[-1], (0, 5.5, tz + 6), (1, 1, 1), P('sting'), 'glow', glow=True)]
    claws = []
    for side in (1, -1):
        s_ = 'l' if side > 0 else 'r'
        names = chain_x(f'arm_{s_}', 'body', (side * 4, 6, -7), side, [
            (0, 2, [0, 55, 0], P('shell'), None),
            (5, 2, [0, 0, 0], P('shell'), 'chitin'),
            (5, 2, [0, 30, 0], P('claw'), 'chitin'),
        ], bones, cubes)
        hand = f'claw_{s_}'
        hx = side * (4 + 10)
        bones.append(dict(name=hand, parent=names[2], pivot=[hx, 6, -7]))
        cubes += [box(hand, (hx + side * 3, 6, -7), (6, 5, 5), P('shell'), 'chitin', decor={'up': both(edge(P('gold'), 'left'), stripes_x(P('gold'), 99, 2))}),
                  box(hand, (hx + side * 7.5, 6.5, -6), (4, 2, 2), P('claw'), 'chitin'),
                  box(hand, (hx + side * 9, 6.5, -6.5), (1, 2, 1), P('dark'), 'chitin')]
        finger = f'finger_{s_}'
        bones.append(dict(name=finger, parent=hand, pivot=[hx + side * 5, 5, -8]))
        cubes.append(box(finger, (hx + side * 7.5, 5, -8.3), (4, 2, 2), P('claw'), 'chitin'))
        claws.append((names, hand, finger, side))
    legs = leg_pairs('leg', 'body', [
        ((5, 4.5, z), [(4, 1, [0, yaw, -25], P('shell'), 'chitin'), (5, 1, [0, 0, 85], P('claw'), None), (3, 1, [0, 0, 20], P('dark'), None)])
        for z, yaw in ((-3, 25), (-1, 0), (1, -20), (3, -45))
    ], bones, cubes)
    make('tomb_scorpion', bones, cubes, 89, bounds=(2.6, 1.8, 0.6), glow=True)
    tail_idle = {b: {'rotation': [sin(60, 3, -i * 30), 0, 0]} for i, b in enumerate(tail)}
    pinch = {f[2]: {'rotation': [0, sin(90, 10 * f[3]), 0]} for f in claws}
    windup = {b: {'rotation': {'0.0': [0, 0, 0], '0.75': [12 if i < 3 else -8, 0, 0]}} for i, b in enumerate(tail)}
    windup['body'] = {'position': {'0.0': [0, 0, 0], '0.75': [0, -1, 1]}}
    sting = {b: {'rotation': {'0.0': [12 if i < 3 else -8, 0, 0], '0.08': [-16, 0, 0], '0.4': [0, 0, 0]}} for i, b in enumerate(tail)}
    snip = {}
    for names, hand, finger, side in claws:
        snip[names[1]] = {'rotation': {'0.0': [0, 0, 0], '0.1': [0, -20 * side, 0], '0.35': [0, 0, 0]}}
        snip[finger] = {'rotation': {'0.0': [0, 25 * side, 0], '0.1': [0, -5 * side, 0], '0.35': [0, 0, 0]}}
    anim('tomb_scorpion', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': tail_idle | pinch},
        'move.walk': {'loop': True, 'animation_length': 0.8, 'bones': gait(legs, 450, 16, 12, dict(tail_idle))},
        'attack.strike': {'loop': False, 'animation_length': 0.35, 'bones': snip},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 0.75, 'bones': windup},
        'attack.sting': {'loop': False, 'animation_length': 0.4, 'bones': sting},
    })


ALL = [sanddiver, shieldback, ember_beetle, roller, golden_scarab, antlion, tomb_scorpion]

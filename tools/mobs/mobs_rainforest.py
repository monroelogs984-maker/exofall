"""Hemlock rainforest meteor mobs."""
from mobparts import *  # noqa: F401,F403
import mobparts
from mobs_templates import mantis


def twigwalker():
    """Stick insect ~2.5 blocks long, stilt-legged: knobbly bark-brown twig body in segments with
    cyan lichen tufts (Veilpine lichen), tiny head, very long antennae, six long thin legs; the
    front pair reaches forward like a twig."""
    mobparts.mobkit.PALETTE.update({'tw_bark': '#6a5a48', 'tw_dark': '#3e342a', 'tw_light': '#8e7c62', 'tw_lichen': '#58c8c0'})
    lichen = dots('tw_lichen', [(0, 1), (1, 3)])
    bones = [dict(name='root', pivot=[0, 12, 0])]
    cubes = []
    segs = [(5, 2, 2, 12, 0, 'tw_bark', 'wood', {'up': lichen}),
            (8, 2, 2, 12, 0, 'tw_bark', 'wood', {'up': dots('tw_light', [(0, 2), (1, 5)])}),
            (6, 3, 3, 12, 0, 'tw_bark', 'wood', {'up': lichen}),
            (5, 2, 2, 12, 2, 'tw_bark', 'wood', None),
            (5, 2, 2, 12, 3, 'tw_bark', 'wood', {'up': lichen}),
            (4, 2, 2, 12, 3, 'tw_bark', 'wood', None),
            (4, 1, 1, 12, 5, 'tw_dark', 'wood', None)]
    body, end = chain_z('seg', 'root', -8, segs, bones, cubes)
    z = -8
    for (length, w, h, cy, *_), name in zip(segs[:-1], body):      # knots at the joints, lichen tufts
        cubes.append(box(name, (0, cy, z + length - 0.5), (w + 1, h + 1, 1), 'tw_dark', 'wood'))
        z += length
    cubes += [box('seg2', (0, 14, -1), (2, 1, 2), 'tw_lichen', 'fur'), box('seg4', (0, 13.5, 13), (1, 1, 2), 'tw_lichen', 'fur')]
    bones.append(dict(name='head', parent='seg0', pivot=[0, 12, -8]))
    cubes.append(box('head', (0, 12, -9.5), (3, 3, 3), 'tw_light', 'wood', decor={'north': dots('tw_dark', [(0, 0), (2, 0)])}))
    for side in (1, -1):
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 1, 13, -11), side,
                [(0, 1, [0, 75, 0], 'tw_dark', None), (6, 1, [0, 0, -20], 'tw_dark', None), (6, 1, [0, 0, 10], 'tw_light', None)], bones, cubes)
    legs = []
    specs = [  # parent, root, segs
        ('seg0', (1, 11, -5), [(0, 1, [0, 60, 0], 'tw_bark', None), (8, 1, [0, 0, -15], 'tw_bark', 'wood'), (10, 1, [0, 0, 80], 'tw_bark', 'wood'), (4, 1, [0, 0, -30], 'tw_dark', None)]),
        ('seg1', (1, 11, 2), [(9, 1, [0, 10, -25], 'tw_bark', 'wood'), (11, 1, [0, 0, 105], 'tw_bark', 'wood'), (4, 1, [0, 0, -40], 'tw_dark', None)]),
        ('seg2', (1.5, 11, 8), [(10, 1, [0, -35, -25], 'tw_bark', 'wood'), (12, 1, [0, 0, 105], 'tw_bark', 'wood'), (4, 1, [0, 0, -40], 'tw_dark', None)]),
    ]
    for i, (parent, root, sg) in enumerate(specs):
        for side in (1, -1):
            names = chain_x(f'leg{i}{"l" if side > 0 else "r"}', parent, (side * root[0], root[1], root[2]), side, sg, bones, cubes)
            legs.append((names[1:] if sg[0][0] == 0 else names, i, side))
    make('twigwalker', bones, cubes, 73, bounds=(3, 2, 0.8))
    sway = {'root': {'rotation': [0, 0, sin(40, 3)], 'position': [sin(40, 0.6), 0, 0]}}
    walk = gait(legs, 200, 14, 10, dict(sway))
    kick = {}
    for names, i, side in legs:
        if i == 2:
            kick[names[0]] = {'rotation': {'0.0': [0, 0, 0], '0.12': [0, -45 * side, -20 * side], '0.4': [0, 0, 0]}}
    anim('twigwalker', {
        'misc.idle': {'loop': True, 'animation_length': 9.0, 'bones': sway},
        'move.walk': {'loop': True, 'animation_length': 1.8, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': kick},
        'misc.freeze': {'loop': True, 'animation_length': 4.0, 'bones': {'root': {'rotation': [0, 0, sin(90, 0.5)]}}},
    })


def bloommantis():
    """Flower-mimic mantis: petal-white and pink, petal lobes on the walking legs and abdomen."""
    mantis('bloommantis', {'body': '#f0d8e4', 'dark': '#b07090', 'light': '#ffffff', 'eye': '#e04a8a',
                           'wing': '#f6e6ee', 'vein': '#d8a0bc', 'petal': '#f4b8d4'}, 75, petals=True)


def leafcutter():
    """Leafcutter ant ~1.1 blocks plus its leaf: big heart-shaped rust head with hooked mandibles
    and elbowed antennae, spiny thorax, two waist nodes, dark gaster; carries a cut green leaf held
    up like a sail."""
    mobparts.mobkit.PALETTE.update({'la_ant': '#9a3a1e', 'la_dark': '#5a1e10', 'la_leg': '#6a2a14', 'la_leaf': '#5ab048', 'la_vein': '#3a7a2e'})
    bones = [dict(name='body', pivot=[0, 7, 0])]
    cubes = [
        box('body', (0, 7, -0.5), (4, 4, 6), 'la_ant', 'chitin'),
        box('body', (0, 7, 3.5), (2, 3, 2), 'la_ant', 'chitin'),
        box('body', (0, 7, 5.5), (2, 3, 2), 'la_ant', 'chitin'),
    ]
    for sx in (-1, 1):
        cubes += [box('body', (sx * 1, 9.8, -2), (1, 2, 1), 'la_dark', 'chitin'), box('body', (sx * 1, 9.3, 1.5), (1, 2, 1), 'la_dark', 'chitin')]
    bones.append(dict(name='gaster', parent='body', pivot=[0, 7, 6.5]))
    cubes.append(box('gaster', (0, 7, 10), (7, 6, 7), 'la_dark', 'chitin', decor={'up': stripes_y('la_ant', 2, 1)}))
    bones.append(dict(name='head', parent='body', pivot=[0, 7.5, -3.5]))
    cubes += [
        box('head', (0, 7.5, -6.5), (7, 6, 5), 'la_ant', 'chitin', decor={'north': dots('la_dark', [(0, 2), (6, 2)]), 'up': stripes_x('la_dark', 99, 3)}),
        box('head', (2, 10.8, -5), (3, 1, 3), 'la_ant', 'chitin'),
        box('head', (-2, 10.8, -5), (3, 1, 3), 'la_ant', 'chitin'),
    ]
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 1.5, 6, -9], rotation=[0, -20 * side, 0]))
        cubes += [box(n, (side * 1.5, 6, -10.5), (2, 1, 3), 'la_dark', 'chitin'), box(n, (side * 0.5, 6, -11.5), (1, 1, 1), 'la_dark')]
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 2, 9.5, -8.5), side,
                [(0, 1, [0, 50, 0], 'la_leg', None), (5, 1, [0, 0, -50], 'la_leg', None), (5, 1, [0, 0, 70], 'la_ant', None)], bones, cubes)
    bones.append(dict(name='leaf', parent='head', pivot=[0, 7, -10], rotation=[-25, 0, 0]))
    cubes += [box('leaf', (0, 10, -10), (1, 6, 1), 'la_vein', 'wood'),
              box('leaf', (0, 17, -9), (0, 9, 11), 'la_leaf', rim=False, noise=4,
                  decor={'east': both(stripes_y('la_vein', 99, 4), outline('la_vein')), 'west': both(stripes_y('la_vein', 99, 4), outline('la_vein'))})]
    legs = leg_pairs('leg', 'body', [
        ((2, 5.5, z), [(5, 1, [0, yaw, -15], 'la_leg', 'chitin'), (6, 1, [0, 0, 80], 'la_leg', None), (4, 1, [0, 0, -20], 'la_dark', None)])
        for z, yaw in ((-2, 40), (0, 0), (2, -40))
    ], bones, cubes)
    make('leafcutter', bones, cubes, 77, bounds=(2, 2, 0.6))
    ant = {f'antenna_{s}1': {'rotation': [0, 0, sin(150, 10, 0 if s == 'l' else 90)]} for s in 'lr'}
    walk = gait(legs, 700, 16, 12, dict(ant))
    walk['gaster'] = {'rotation': [sin(700, 3), 0, 0]}
    walk['leaf'] = {'rotation': [sin(700, 2), 0, sin(350, 3)]}
    anim('leafcutter', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': ant},
        'move.walk': {'loop': True, 'animation_length': 0.5, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.35, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.35': [0, 0, 0]}},
            'mandible_l': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, -30, 0], '0.3': [0, 0, 0]}},
            'mandible_r': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, 30, 0], '0.3': [0, 0, 0]}}}},
    })


def lantern_moth():
    """Moth ~1 block long, 2.3 wide: fuzzy slate body, plumed antennae, broad dusky wings with
    glowing cyan eyespots (Veilpine lichen glow)."""
    mobparts.mobkit.PALETTE.update({'lm_fur': '#5a6478', 'lm_dark': '#2a3040', 'lm_wing': '#6a7890', 'lm_edge': '#3a4458', 'lm_glow': '#7ff0e8', 'lm_plume': '#8a94a8'})
    mobparts.mobkit.GLOW_KEYS.add('lm_glow')

    def eyespot(cx, cy, r):
        def d(put, w, h):
            x0, y0 = int(w * cx), int(h * cy)
            for y in range(-r - 1, r + 2):
                for x in range(-r - 1, r + 2):
                    dd = (x * x + y * y) ** 0.5
                    if dd <= r - 0.5:
                        put(x0 + x, y0 + y, 'lm_glow')
                    elif dd <= r + 0.7:
                        put(x0 + x, y0 + y, 'lm_dark')
        return d
    bones = [dict(name='body', pivot=[0, 10, 0])]
    cubes = [
        box('body', (0, 10, -1), (5, 5, 5), 'lm_fur', 'fur'),
        box('body', (0, 10, -3), (6, 6, 1), 'lm_fur', 'fur', inflate=0.4),
    ]
    abdomen, _ = chain_z('ab', 'body', 1.5, [(3, 4, 4, 9.5, -10, 'lm_fur', 'fur', {'up': stripes_y('lm_dark', 99, 2)}),
                                            (3, 4, 4, 9.5, -4, 'lm_fur', 'fur', {'up': stripes_y('lm_dark', 99, 2)}),
                                            (3, 3, 3, 9.5, -4, 'lm_fur', 'fur', None),
                                            (2, 2, 2, 9.5, -4, 'lm_dark', 'fur', None)], bones, cubes)
    bones.append(dict(name='head', parent='body', pivot=[0, 10, -3.5]))
    cubes += [box('head', (0, 10, -4.5), (4, 4, 2), 'lm_fur', 'fur'),
              box('head', (2, 10.5, -4.5), (1, 2, 2), 'lm_dark', 'facet'),
              box('head', (-2, 10.5, -4.5), (1, 2, 2), 'lm_dark', 'facet')]
    for side in (1, -1):
        names = chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 1, 12, -5.5), side,
                        [(0, 1, [0, 55, 0], 'lm_plume', None), (6, 1, [0, 0, -35], 'lm_plume', None)], bones, cubes)
        cubes.append(cube(names[1], [side * 1 if side > 0 else side * 1 - 6, 12, -6.5], [6, 0, 2], 'lm_plume@220', rim=False))
    wings = {}
    for side in (1, -1):
        s = 'l' if side > 0 else 'r'
        for kind, z0, chord, span, spot in (('f', -3, 6, 16, (0.62, 0.5, 2)), ('b', 3, 5, 12, (0.55, 0.5, 1))):
            n = f'wing_{kind}{s}'
            wings[n] = side
            bones.append(dict(name=n, parent='body', pivot=[side * 2.5, 12, z0], rotation=[0, 0, -12 * side]))
            ox = side * 2.5 if side > 0 else -2.5 - span
            dec = both(outline('lm_edge'), eyespot(spot[0] if side > 0 else 1 - spot[0], spot[1], spot[2]))
            cubes.append(cube(n, [ox, 12, z0], [span, 0, chord], 'lm_wing@235', rim=False, noise=4, decor={'up': dec, 'down': dec}))
            inner = int(span * 0.6)    # trailing inner lobe: stepped, tapering wing
            ix = side * 2.5 if side > 0 else -2.5 - inner
            cubes.append(cube(n, [ix, 12, z0 + chord], [inner, 0, 3], 'lm_wing@235', rim=False, noise=4,
                              decor={'up': outline('lm_edge'), 'down': outline('lm_edge')}))
            cubes.append(cube(n, [ox, 11.5, z0 - 0.5], [span, 1, 1], 'lm_edge', mat='fur'))
    legs = leg_pairs('leg', 'body', [
        ((2, 8, z), [(3, 1, [0, yaw, 50], 'lm_fur', None), (4, 1, [0, 0, 45], 'lm_dark', None)])
        for z, yaw in ((-2.5, 30), (-1, 0), (0.5, -30))
    ], bones, cubes)
    make('lantern_moth', bones, cubes, 79, bounds=(3, 1.5, 0.5), glow=True)

    def fly(speed, amp):
        out = flap(wings, speed, amp, lambda n: 0 if n.startswith('wing_f') else 25)
        out['body'] = {'position': [0, sin(speed, 0.8, 180), 0]}
        for i, b in enumerate(abdomen):
            out[b] = {'rotation': [sin(speed, 3, -i * 40), 0, 0]}
        return leg_sway(legs, 100, 5, out)
    anim('lantern_moth', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': fly(720, 30)},
        'move.fly': {'loop': True, 'animation_length': 2.0, 'bones': fly(900, 40)},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': {'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.3': [0, 0, 0]}}}},
    })


ALL = [twigwalker, bloommantis, leafcutter, lantern_moth]

"""Swamp meteor mobs."""
from mobparts import *  # noqa: F401,F403
import mobparts  # noqa: F401


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
        (4, 12, 11, 6.5, -6, 'grub', 'flesh', side_decor()),
        (4, 9, 8, 5.0, -10, 'grub', 'flesh', side_decor()),
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


# ---- Divebell Beetle -----------------------------------------------------------------------

def divebell_beetle():
    """Diving beetle ~1.5 blocks long: smooth domed bottle-green shell with a yellow margin, bronze
    underside, flat hairy oar hind legs, and the silvery air bubble it carries under the shell tip."""
    mobparts.mobkit.PALETTE.update({'db_shell': '#1f3a2c', 'db_rim': '#c9b13a', 'db_under': '#6b5a2a',
                                    'db_eye': '#c08a3a', 'db_leg': '#2a2a1e', 'db_hair': '#8a8058', 'db_bubble': '#dfeff5'})
    bones = [dict(name='body', pivot=[0, 7, 1])]
    cubes = [
        box('body', (0, 7.5, -6), (12, 5, 5), 'db_shell', 'chitin', decor={'up': outline('db_rim'), 'north': stripes_y('db_rim', 99, 0)}),
        box('body', (3, 8, 4.5), (6, 6, 15), 'db_shell', 'chitin', decor={'up': edge('db_rim', 'right'), 'east': stripes_y('db_rim', 99, 4)}),
        box('body', (-3, 8, 4.5), (6, 6, 15), 'db_shell', 'chitin', decor={'up': edge('db_rim', 'left'), 'west': stripes_y('db_rim', 99, 4)}),
        box('body', (0, 11.5, 4.5), (10, 1, 13), 'db_shell', 'chitin'),
        box('body', (0, 12.5, 5), (6, 1, 10), 'db_shell', 'chitin'),
        box('body', (0, 4.5, 3), (11, 2, 15), 'db_under', 'chitin'),
        box('body', (0, 7, 12.8), (6, 4, 2), 'db_bubble@140', rim=False, noise=1),
    ]
    bones.append(dict(name='head', parent='body', pivot=[0, 7, -8.5]))
    cubes += [
        box('head', (0, 6.5, -10.5), (8, 4, 4), 'db_shell', 'chitin', decor={'up': stripes_y('db_rim', 99, 0)}),
        box('head', (4, 7.5, -10.5), (1, 2, 2), 'db_eye', 'facet'),
        box('head', (-4, 7.5, -10.5), (1, 2, 2), 'db_eye', 'facet'),
        box('head', (0, 5.5, -13), (5, 2, 1), 'db_under', 'chitin'),
    ]
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 1.5, 5, -13], rotation=[0, -15 * side, 0]))
        cubes.append(box(n, (side * 1.5, 5, -14), (1, 1, 2), 'db_leg', 'chitin'))
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 2.5, 8, -12), side,
                [(5, 1, [0, 60, -20], 'db_leg', None), (4, 1, [0, 0, 25], 'db_hair', None)], bones, cubes)
    legs = leg_pairs('leg', 'body', [
        ((4.5, 5, -6), [(3, 1, [0, 45, 40], 'db_leg', 'chitin'), (4, 1, [0, 0, 50], 'db_leg', None), (3, 1, [0, 0, -30], 'db_hair', None)]),
        ((5.5, 5, -1), [(4, (2, 1), [0, 5, 45], 'db_leg', 'chitin'), (5, 1, [0, 0, 45], 'db_leg', None), (3, 1, [0, 0, -30], 'db_hair', None)]),
        ((5.5, 5, 4), [(6, 2, [0, -55, 25], 'db_leg', 'chitin'), (8, (1, 3), [0, 0, 10], 'db_hair', 'fur'), (5, (1, 2), [0, 0, -10], 'db_hair', 'fur')]),
    ], bones, cubes)
    make('divebell_beetle', bones, cubes, 41, bounds=(2.5, 1.5, 0.5))
    ants = {f'antenna_{s}0': {'rotation': [sin(80, 6, 0 if s == 'l' else 90), sin(50, 8), 0]} for s in 'lr'}
    swim = dict(ants)
    for names, i, side in legs:
        amp = {0: 6, 1: 18, 2: 38}[i]
        swim[names[0]] = {'rotation': [0, sin(420, amp * side, 0), 0]}   # both sides row together
    swim['body'] = {'position': [0, sin(420, 0.4, 90), 0]}
    anim('divebell_beetle', {
        'misc.idle': {'loop': True, 'animation_length': 4.0, 'bones': ants},
        'move.walk': {'loop': True, 'animation_length': 1.0, 'bones': gait(legs, 360, 16, 12, dict(ants))},
        'move.swim': {'loop': True, 'animation_length': 6.0, 'bones': swim},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.1': [15, 0, 0], '0.35': [0, 0, 0]},
                     'position': {'0.0': [0, 0, 0], '0.1': [0, 0, -1.5], '0.35': [0, 0, 0]}},
            'mandible_l': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, -30, 0], '0.3': [0, 0, 0]}},
            'mandible_r': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, 30, 0], '0.3': [0, 0, 0]}}}},
    })


# ---- Reedmantis ----------------------------------------------------------------------------

def reedmantis():
    """Olive reed-striped mantis that hides in Sapcane."""
    from mobs_templates import mantis
    mantis('reedmantis', {'body': '#7d8a3a', 'dark': '#4a5226', 'light': '#b7b56a', 'eye': '#d8d070',
                          'wing': '#8f9a4c', 'vein': '#5a6230'}, 43)




def bogspinner():
    """Fishing spider ~1.8 blocks across the legs: olive-brown fur with pale side stripes, a glossy
    ovoid abdomen with a silk-white chevron, a row of black eyes with two big glinting medians,
    banded legs, silk spinnerets under the tail."""
    from mobs_templates import spider_legs, chevrons
    pal = {'body': '#4e4630', 'dark': '#221e14', 'band': '#6e6444', 'legband': '#6e6444', 'stripe': '#d8d0a0', 'eye': '#0e0c0a',
           'glint': '#e8f4e0', 'silk': '#e8f0e0', 'fang': '#120e0a'}
    P = lambda k: f'bogspinner_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})
    side_stripe = stripes_y(P('stripe'), 99, 1)
    bones = [dict(name='body', pivot=[0, 8, -2])]
    cubes = [
        box('body', (0, 8, -3), (8, 4, 8), P('body'), 'fur', decor={'up': both(stripes_x(P('stripe'), 99, 0), stripes_x(P('stripe'), 99, 7),
                                                                              stripes_x(P('dark'), 99, 3), stripes_x(P('dark'), 99, 4)),
                                                                   'east': side_stripe, 'west': side_stripe}),
        box('body', (0, 10.5, -4), (5, 1, 5), P('body'), 'fur', decor={'up': dots(P('dark'), [(2, 1), (2, 2)])}),
        # eye row on the front of the head: two big medians with a glint, small laterals
        box('body', (1.5, 10, -7.5), (2, 2, 1), P('eye'), 'facet', decor={'north': dots(P('glint'), [(0, 0)])}),
        box('body', (-1.5, 10, -7.5), (2, 2, 1), P('eye'), 'facet', decor={'north': dots(P('glint'), [(0, 0)])}),
        box('body', (3.2, 10.6, -7), (1, 1, 1), P('eye'), 'facet'),
        box('body', (-3.2, 10.6, -7), (1, 1, 1), P('eye'), 'facet'),
    ]
    for side in (1, -1):   # chelicerae with hooked fangs
        n = f'chelicera_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='body', pivot=[side * 1.3, 8, -7]))
        cubes += [box(n, (side * 1.3, 7, -7.8), (2, 3, 2), P('body'), 'fur'),
                  box(n, (side * 0.9, 5.3, -8.2), (1, 1, 1), P('fang'), 'chitin')]
        chain_x(f'palp_{"l" if side > 0 else "r"}', 'body', (side * 2.5, 7.5, -7), side,
                [(0, 1, [0, 75, 0], P('band'), None), (3, 1, [0, 0, 25], P('band'), 'chitin'), (3, 1, [0, 0, 40], P('dark'), None)], bones, cubes)
    bones.append(dict(name='abdomen', parent='body', pivot=[0, 8.5, 1], rotation=[8, 0, 0]))
    cubes += [
        box('abdomen', (0, 8.5, 6.5), (9, 7, 11), P('body'), 'chitin', decor={'up': edge(P('dark'), 'left'), 'east': side_stripe, 'west': side_stripe}),
        box('abdomen', (0, 8.5, 6.5), (11, 5, 9), P('body'), 'chitin', decor={'east': side_stripe, 'west': side_stripe}),
        box('abdomen', (0, 8.5, 6.5), (7, 9, 9), P('body'), 'chitin', decor={'up': both(chevrons(P('dark'), 4, 0), chevrons(P('silk'), 4, 2))}),
        box('abdomen', (0, 8.5, 12.5), (6, 5, 1), P('dark'), 'chitin'),
        box('abdomen', (0.8, 6.5, 13.3), (1, 1, 2), P('silk'), 'fur'),
        box('abdomen', (-0.8, 6.5, 13.3), (1, 1, 2), P('silk'), 'fur'),
    ]
    legs = spider_legs(P, [(3.5, -5.5, 60), (3.8, -3.8, 25), (3.8, -2, -15), (3.5, -0.5, -50)], 7, 9, 5, bones, cubes, 8)
    make('bogspinner', bones, cubes, 81, bounds=(3, 1.6, 0.4))
    palps = {f'palp_{s}1': {'rotation': [0, 0, sin(180, 14, 0 if s == 'l' else 90)]} for s in 'lr'}
    idle = dict(palps)
    idle['abdomen'] = {'rotation': [sin(70, 2, 0, 8), 0, 0], 'scale': [1, sin(70, 0.02, 0, 1), 1]}
    walk = gait(legs, 520, 16, 14, dict(palps))
    walk['abdomen'] = {'rotation': [sin(1040, 2, 0, 8), sin(520, 3), 0]}
    strike = {'body': {'rotation': {'0.0': [0, 0, 0], '0.1': [10, 0, 0], '0.35': [0, 0, 0]}, 'position': {'0.0': [0, 0, 0], '0.1': [0, -1, -2], '0.35': [0, 0, 0]}}}
    for s, sg in (('l', 1), ('r', -1)):
        strike[f'chelicera_{s}'] = {'rotation': {'0.0': [0, 0, 0], '0.1': [-35, 0, 0], '0.25': [20, 0, 0], '0.35': [0, 0, 0]}}
    # tell before a web shot: rears up on the back legs, abdomen tips under to aim the spinnerets
    windup = {'body': {'rotation': {'0.0': [0, 0, 0], '0.75': [-22, 0, 0]}, 'position': {'0.0': [0, 0, 0], '0.75': [0, 2, 0]}},
              'abdomen': {'rotation': {'0.0': [8, 0, 0], '0.75': [-30, 0, 0]}}}
    windup.update({k: v for k, v in palps.items()})
    anim('bogspinner', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 0.7, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.35, 'bones': strike},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 0.75, 'bones': windup},
        'attack.shoot': {'loop': False, 'animation_length': 0.4, 'bones': {
            'abdomen': {'rotation': {'0.0': [-30, 0, 0], '0.08': [-45, 0, 0], '0.4': [8, 0, 0]}},
            'body': {'rotation': {'0.0': [-22, 0, 0], '0.4': [0, 0, 0]}, 'position': {'0.0': [0, 2, 0], '0.4': [0, 0, 0]}}}},
    })


def mire_leech():
    """Leech ~1.4 blocks long, flat and glossy: olive-black with two orange dorsal stripes and a
    row of dark spots, a narrow head with a ringed oral sucker, a wide rear sucker disc."""
    pal = {'body': '#2e3622', 'dark': '#161a10', 'belly': '#5a5a32', 'stripe': '#c8742a', 'spot': '#0c0e08', 'mouth': '#3a1a14', 'ring': '#8a6a4a'}
    P = lambda k: f'mire_leech_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})

    def back(w):
        return both(stripes_x(P('stripe'), 99, 1), stripes_x(P('stripe'), 99, w - 2), dots(P('spot'), [(w // 2, 1)]))
    bones = [dict(name='root', pivot=[0, 2, -10])]
    widths = [4, 5, 7, 8, 9, 9, 9, 8, 7, 6]
    segs = [(2, w, 3 if i == 0 else 4, 2, 0, P('body'), 'flesh',
             {'up': back(w), 'down': stripes_y(P('belly'), 1), 'east': stripes_x(P('dark'), 99, 1), 'west': stripes_x(P('dark'), 99, 0)})
            for i, w in enumerate(widths)]
    body, end = chain_z('seg', 'root', -10, segs, bones, cubes := [])
    cubes += [
        box('seg0', (0, 2, -10.5), (4, 3, 1), P('ring'), 'flesh', decor={'north': both(outline(P('ring')), dots(P('mouth'), [(1, 1), (2, 1)]))}),
        box(body[-1], (0, 1.5, end + 2), (8, 3, 5), P('body'), 'flesh', decor={'up': both(outline(P('dark')), stripes_y(P('stripe'), 99, 1))}),
        box(body[-1], (0, 0.5, end + 2), (10, 1, 6), P('belly'), 'flesh'),
    ]
    make('mire_leech', bones, cubes, 83, bounds=(1.5, 0.8, 0.2))
    swim = {b: {'rotation': [0, sin(400, 9, -i * 45), 0]} for i, b in enumerate(body)}
    crawl = {b: {'rotation': [sin(240, 7, -i * 35), 0, 0]} for i, b in enumerate(body)}
    latch = {b: {'rotation': [sin(300, 5, -i * 30), 0, 0]} for i, b in enumerate(body[3:], 3)}
    latch['seg0'] = {'rotation': [-35, 0, 0]}
    latch['seg1'] = {'rotation': [-25, 0, 0]}
    for i, b in enumerate(body[2:8]):
        latch.setdefault(b, {})['scale'] = [f'1 + math.max(0, math.sin(query.anim_time * 360 - {i * 50})) * 0.12', 1, 1]
    anim('mire_leech', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': {b: {'rotation': [0, sin(60, 3, -i * 30), 0]} for i, b in enumerate(body)}},
        'move.walk': {'loop': True, 'animation_length': 1.5, 'bones': crawl},
        'move.swim': {'loop': True, 'animation_length': 0.9, 'bones': swim},
        'misc.latch': {'loop': True, 'animation_length': 1.0, 'bones': latch},
        'attack.strike': {'loop': False, 'animation_length': 0.3, 'bones': {'seg0': {'rotation': {'0.0': [0, 0, 0], '0.1': [-30, 0, 0], '0.3': [0, 0, 0]}}}},
    })


ALL = [mudgrub, glasswing, bloodgnat, divebell_beetle, reedmantis, bogspinner, mire_leech]

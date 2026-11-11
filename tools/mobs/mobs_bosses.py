"""The four meteor bosses: Terror of the Bog, Waxen Queen, Dozen-legged Wrath, Apex Mantis.

Same builder and conventions as the lesser mobs, at 3-4x their size with more parts. Every boss has the
animations its Java class triggers: misc.idle, move.walk / move.fly, misc.dormant (lying still before the
fight), misc.intro (5 s, played during the cinematic, holds its last frame) and attack.<move> per move.
"""
from mobparts import *  # noqa: F401,F403
import mobparts


def kf(*pairs):
    """Keyframes: kf((0, [x, y, z]), (0.5, [...])) -> {'0.0': [...], '0.5': [...]}."""
    return {f'{t:.2f}'.rstrip('0').rstrip('.') if t else '0.0': v for t, v in pairs}


def kf_hold(t_end, v):
    return kf((0, v), (t_end, v))


def mirror(prefix, rot_l):
    """Per-side rotation dicts for an l/r pair: right side flips Y and Z."""
    return {f'{prefix}_l': rot_l, f'{prefix}_r': [rot_l[0], -rot_l[1], -rot_l[2]]}


def pair_kf(name_l, name_r, *pairs):
    """Keyframes for an l/r bone pair; values are given for the left bone, the right one is mirrored."""
    return {name_l: {'rotation': kf(*pairs)},
            name_r: {'rotation': kf(*[(t, [v[0], -v[1], -v[2]]) for t, v in pairs])}}


def merge(*dicts):
    out = {}
    for d in dicts:
        for k, v in d.items():
            out.setdefault(k, {}).update(v)
    return out


# ---- Terror of the Bog -------------------------------------------------------------------------

def terror_of_the_bog():
    """Giant water scorpion ~4.5 blocks long: broad flat leaf-shaped body in mud brown with olive algae
    mottling and veined wing covers, small head with bulging glowing eyes and a hooked beak, huge
    jackknife raptorial forelegs held forward, four oar-like walking legs fringed with hairs, and two
    long breathing siphons trailing behind."""
    P = {'tb_mud': '#5a4428', 'tb_dark': '#2e2214', 'tb_light': '#8a7046', 'tb_algae': '#6a7a2a', 'tb_algae2': '#8a9a3a',
         'tb_vein': '#3a2c18', 'tb_eye': '#d8e060', 'tb_beak': '#1e160c', 'tb_hair': '#a08a5a', 'tb_claw': '#c8b080'}
    mobparts.mobkit.PALETTE.update(P)
    mobparts.mobkit.GLOW_KEYS.add('tb_eye')
    algae = dots('tb_algae', [(2, 2), (3, 2), (9, 4), (10, 5), (5, 8), (14, 3), (15, 9), (20, 6), (7, 14), (17, 15), (24, 11), (12, 20), (26, 18)])
    algae2 = dots('tb_algae2', [(4, 3), (11, 5), (16, 10), (8, 15), (25, 12), (13, 21)])

    def veins(put, w, h):
        for y in range(h):
            put(w // 2, y, 'tb_vein')
            if y % 5 == 2:
                for x in range(w):
                    if (x + y) % 3 == 0:
                        put(x, y, 'tb_vein')
        for x in range(w):
            put(x, 0, 'tb_light')
    bones = [dict(name='body', pivot=[0, 14, 6])]
    cubes = [
        # thorax shield + abdomen plate, tapering at the rear
        box('body', (0, 14, -6), (30, 9, 18), 'tb_mud', 'chitin', decor={'up': both(algae, algae2), 'east': stripes_y('tb_dark', 99, 7), 'west': stripes_y('tb_dark', 99, 7)}),
        box('body', (0, 14, 13), (38, 8, 22), 'tb_mud', 'chitin', decor={'up': both(algae, algae2), 'east': stripes_x('tb_dark', 5, 2), 'west': stripes_x('tb_dark', 5, 2)}),
        box('body', (0, 13.5, 30), (32, 7, 12), 'tb_mud', 'chitin', decor={'up': algae, 'east': stripes_x('tb_dark', 5, 2), 'west': stripes_x('tb_dark', 5, 2)}),
        box('body', (0, 13, 40), (22, 6, 8), 'tb_mud', 'chitin', decor={'up': algae2}),
        box('body', (0, 12.5, 46), (12, 5, 4), 'tb_dark', 'chitin'),
        # flat side flanges (the leaf-like rim)
        box('body', (20.5, 12, 14), (3, 2, 30), 'tb_light', 'chitin'),
        box('body', (-20.5, 12, 14), (3, 2, 30), 'tb_light', 'chitin'),
        box('body', (17.5, 12, 32), (3, 2, 12), 'tb_light', 'chitin'),
        box('body', (-17.5, 12, 32), (3, 2, 12), 'tb_light', 'chitin'),
        # belly plates
        box('body', (0, 9, 10), (26, 2, 32), 'tb_dark', 'chitin', decor={'down': stripes_y('tb_mud', 4, 1)}),
        # wing covers lying over the abdomen, a ridge down the seam
        box('body', (8.5, 18.6, 18), (16, 1, 36), 'tb_light', 'leather', decor={'up': both(veins, dots('tb_algae', [(3, 6), (9, 20), (5, 30)]))}),
        box('body', (-8.5, 18.6, 18), (16, 1, 36), 'tb_light', 'leather', decor={'up': both(veins, dots('tb_algae', [(10, 9), (4, 24)]))}),
        box('body', (0, 19.2, 18), (2, 1, 34), 'tb_dark', 'chitin'),
        # pronotum bumps
        box('body', (6, 19, -8), (8, 2, 8), 'tb_mud', 'chitin', decor={'up': algae2}),
        box('body', (-6, 19, -8), (8, 2, 8), 'tb_mud', 'chitin', decor={'up': algae}),
        box('body', (0, 19.5, -2), (16, 1, 4), 'tb_dark', 'chitin'),
    ]
    # algae clumps hanging off the rim
    for x, z in ((19, 6), (-19, 20), (16, 36), (-15, 30), (20, 24)):
        cubes.append(box('body', (x, 10.5, z), (3, 3, 3), 'tb_algae', 'leather'))
    # head
    bones.append(dict(name='head', parent='body', pivot=[0, 14, -15]))
    cubes += [
        box('head', (0, 14, -19), (14, 8, 8), 'tb_mud', 'chitin', decor={'up': stripes_x('tb_dark', 99, 6), 'north': dots('tb_dark', [(6, 1), (7, 1), (6, 2), (7, 2)])}),
        box('head', (0, 16.5, -18), (8, 4, 4), 'tb_dark', 'chitin'),
        box('head', (8, 15.5, -20), (5, 6, 6), 'tb_eye', 'facet', glow=True),
        box('head', (-8, 15.5, -20), (5, 6, 6), 'tb_eye', 'facet', glow=True),
        box('head', (8, 15.5, -20), (6, 2, 7), 'tb_dark', 'chitin', inflate=0.2),
    ]
    bones.append(dict(name='beak', parent='head', pivot=[0, 12, -23], rotation=[35, 0, 0]))
    cubes += [box('beak', (0, 12, -26), (4, 4, 6), 'tb_beak', 'chitin'), box('beak', (0, 11.5, -30), (2, 2, 4), 'tb_beak', 'chitin')]
    # breathing siphons
    siphons = []
    for side in (1, -1):
        n = f'siphon_{"l" if side > 0 else "r"}'
        bones.append(dict(name=f'{n}0', parent='body', pivot=[side * 2.5, 13, 47], rotation=[-6, side * 8, 0]))
        cubes.append(box(f'{n}0', (side * 2.5, 13, 54), (2, 2, 14), 'tb_dark', 'chitin'))
        prev = f'{n}0'
        for i, (L, rx) in enumerate(((14, 4), (14, 4), (12, 3))):
            z0 = 61 + i * 14
            bones.append(dict(name=f'{n}{i + 1}', parent=prev, pivot=[side * 2.5, 13, z0], rotation=[rx, side * 2, 0]))
            cubes.append(box(f'{n}{i + 1}', (side * 2.5, 13, z0 + L / 2), (2 if i < 2 else 1, 2 if i < 2 else 1, L), 'tb_mud' if i % 2 else 'tb_dark', 'chitin'))
            prev = f'{n}{i + 1}'
        siphons.append(n)
    # raptorial forelegs: shoulder turns them forward, femur juts forward and up, tibia folds back under it
    arms = []
    for side in (1, -1):
        names = chain_x(f'arm_{"l" if side > 0 else "r"}', 'body', (side * 9, 13, -13), side, [
            (0, 4, [0, 80, 0], 'tb_mud', None),
            (8, 6, [0, 0, 25], 'tb_mud', 'chitin'),
            (24, (7, 6), [0, 0, -45], 'tb_light', 'chitin'),
            (20, (5, 4), [0, 0, 155], 'tb_mud', 'chitin'),
            (7, 2, [0, 0, 35], 'tb_claw', 'chitin'),
        ], bones, cubes)
        # spines along the femur's inner edge and a hooked claw tip
        for j in range(4):
            cubes.append(box(names[2], (side * (9 + 8 + 4 + j * 5), 9.5, -13), (1.5, 3, 1.5), 'tb_claw', 'chitin'))
        cubes.append(box(names[3], (side * (9 + 8 + 24 + 6), 10.5, -13), (1, 3, 1), 'tb_claw', 'chitin'))
        arms.append(names)
    # oar legs: long, flattened, fringed
    legs = leg_pairs('leg', 'body', [
        ((14, 10, 4), [(12, 3, [0, -20, 8], 'tb_mud', 'chitin'), (16, (2, 4), [0, 0, 28], 'tb_light', 'chitin'), (10, (1, 4), [0, 0, -10], 'tb_mud', None)]),
        ((15, 10, 16), [(14, 3, [0, -50, 6], 'tb_mud', 'chitin'), (18, (2, 5), [0, 0, 26], 'tb_light', 'chitin'), (12, (1, 5), [0, 0, -10], 'tb_mud', None)]),
    ], bones, cubes)
    for names, i, side in legs:   # swimming hairs on the tibiae
        L = 16 if i == 0 else 18
        cubes.append(box(names[1], (side * ((14 if i == 0 else 15) + (12 if i == 0 else 14) + L / 2), 10, (4 if i == 0 else 16) + 3), (L - 2, 0, 3), 'tb_hair@200', rim=False))
    make('terror_of_the_bog', bones, cubes, 101, bounds=(6, 3, 1), glow=True)

    A = lambda i, s: arms[0 if s == 'l' else 1][i]
    def arms_pose(coxa, femur, tibia, claw=35, yaw=0, t=None):
        out = {}
        for s, sg in (('l', 1), ('r', -1)):
            out[A(0, s)] = [0, (80 + yaw) * sg, 0]
            out[A(1, s)] = [0, 0, coxa * sg]
            out[A(2, s)] = [0, 0, femur * sg]
            out[A(3, s)] = [0, 0, tibia * sg]
            out[A(4, s)] = [0, 0, claw * sg]
        return out

    def arm_keys(*frames):
        """frames: (t, (coxa, femur, tibia[, claw[, yaw]])) -> bone keyframes (absolute, minus the rest pose)."""
        rest = arms_pose(25, -45, 155)
        out = {}
        for t, args in frames:
            pose = arms_pose(*args)
            for b, v in pose.items():
                d = [v[k] - rest[b][k] for k in range(3)]
                out.setdefault(b, {}).setdefault('rotation', {})[f'{t}' if t else '0.0'] = d
        return out

    def siphon_sway(speed, amp):
        out = {}
        for n in siphons:
            for i in range(4):
                out[f'{n}{i}'] = {'rotation': [sin(speed, amp * 0.6, -i * 60), sin(speed * 0.7, amp, -i * 45 + (0 if n.endswith('l') else 90)), 0]}
        return out

    idle = merge(siphon_sway(80, 4), {'body': {'position': [0, sin(60, 0.3), 0]}, 'head': {'rotation': [sin(50, 2), sin(35, 4), 0]}},
                 {A(3, s): {'rotation': [0, 0, sin(90, 3)]} for s in 'lr'})
    walk = merge(gait(legs, 260, 22, 16), siphon_sway(140, 6), {'body': {'rotation': [0, sin(260, 2), sin(260, 1.5)]}})
    dormant = merge({'body': {'position': [0, -6, 0], 'rotation': [4, 0, 0]}, 'head': {'rotation': [12, 0, 0]}},
                    arm_keys((0, (45, -10, 170, 40))), siphon_sway(30, 1.5))
    for names, i, side in legs:
        dormant[names[1]] = {'rotation': [0, 0, -30 * side]}
    # intro: rise from the mud, spread the forelegs, roar with the beak open, drop into a fighting crouch
    intro = merge(
        {'body': {'position': kf((0, [0, -6, 0]), (1.0, [0, -6, 0]), (1.8, [0, 1, 0]), (2.2, [0, 2, 0]), (4.2, [0, 1, 0]), (5.0, [0, 0, 0])),
                  'rotation': kf((0, [4, 0, 0]), (1.0, [4, 0, 0]), (1.8, [-14, 0, 0]), (2.4, [-20, 0, 0]), (3.9, [-18, 0, 0]), (4.6, [-4, 0, 0]), (5.0, [0, 0, 0]))},
         'head': {'rotation': kf((0, [12, 0, 0]), (1.0, [12, 0, 0]), (2.0, [-8, 0, 0]), (2.4, [-22, 0, 0]), (3.6, [-22, 0, 0]), (4.4, [0, 0, 0]))},
         'beak': {'rotation': kf((0, [0, 0, 0]), (2.3, [0, 0, 0]), (2.5, [30, 0, 0]), (3.6, [30, 0, 0]), (3.9, [0, 0, 0]))}},
        arm_keys((0, (45, -10, 170, 40)), (1.0, (45, -10, 170, 40)), (2.0, (0, -70, 60, 10, -25)), (2.4, (-5, -80, 40, 0, -35)),
                 (3.6, (-5, -80, 40, 0, -35)), (4.4, (15, -60, 120, 30)), (5.0, (25, -45, 155, 35))),
        siphon_sway(120, 8))
    for names, i, side in legs:
        intro[names[1]] = {'rotation': kf((0, [0, 0, -30 * side]), (1.0, [0, 0, -30 * side]), (1.8, [0, 0, 0]))}
    # shake during the roar
    intro['body']['rotation'] = intro['body']['rotation']
    snap = merge(arm_keys((0, (25, -45, 155)), (0.6, (-5, -85, 70, 10)), (0.7, (-5, -85, 70, 10)), (0.78, (30, -5, 168, 40)), (1.1, (25, -45, 155))),
                 {'body': {'rotation': kf((0, [0, 0, 0]), (0.6, [-10, 0, 0]), (0.78, [4, 0, 0]), (1.1, [0, 0, 0])),
                           'position': kf((0, [0, 0, 0]), (0.7, [0, 0, 2]), (0.78, [0, 0, -5]), (1.1, [0, 0, 0]))}})
    grab = merge(arm_keys((0, (25, -45, 155)), (0.9, (-5, -75, 50, 5, -40)), (1.0, (-5, -75, 50, 5, -40)), (1.1, (25, -20, 165, 40, 12)),
                          (3.4, (25, -20, 165, 40, 12)), (3.9, (25, -45, 155))),
                 {'body': {'rotation': kf((0, [0, 0, 0]), (0.9, [-12, 0, 0]), (1.1, [4, 0, 0]), (3.4, [4, 0, 0]), (3.9, [0, 0, 0])),
                           'position': kf((0, [0, 0, 0]), (1.0, [0, 0, 2]), (1.1, [0, 0, -4]), (3.4, [0, 0, -4]), (3.9, [0, 0, 0]))},
                  'head': {'rotation': kf((0, [0, 0, 0]), (1.2, [10, 0, 0]), (1.5, [0, 0, 0]), (1.8, [10, 0, 0]), (2.1, [0, 0, 0]), (2.4, [10, 0, 0]), (2.7, [0, 0, 0]), (3.0, [10, 0, 0]), (3.4, [0, 0, 0]))}})
    throw = merge(arm_keys((0, (25, -20, 165, 40, 12)), (0.15, (-10, -70, 40, 0, -20)), (0.6, (25, -45, 155))),
                  {'body': {'rotation': kf((0, [4, 0, 0]), (0.15, [-12, 0, 0]), (0.6, [0, 0, 0]))}})
    submerge = {'body': {'position': kf((0, [0, 0, 0]), (0.2, [0, 2, 0]), (0.6, [0, -26, 0])), 'rotation': kf((0, [0, 0, 0]), (0.6, [18, 0, 0]))}}
    erupt = merge({'body': {'position': kf((0, [0, -18, 0]), (0.15, [0, 6, 0]), (0.5, [0, 0, 0])), 'rotation': kf((0, [-30, 0, 0]), (0.2, [-18, 0, 0]), (0.8, [0, 0, 0]))}},
                  arm_keys((0, (-5, -80, 40, 0, -35)), (0.4, (-5, -80, 40, 0, -35)), (0.8, (25, -45, 155))))
    spit = {'body': {'rotation': kf((0, [0, 0, 0]), (0.75, [-16, 0, 0]), (0.82, [8, 0, 0]), (1.3, [0, 0, 0]))},
            'head': {'rotation': kf((0, [0, 0, 0]), (0.75, [-25, 0, 0]), (0.82, [20, 0, 0]), (1.3, [0, 0, 0]))},
            'beak': {'rotation': kf((0, [0, 0, 0]), (0.75, [-10, 0, 0]), (0.8, [35, 0, 0]), (1.1, [0, 0, 0]))}}
    roar = merge({'body': {'rotation': kf((0, [0, 0, 0]), (0.4, [-20, 0, 0]), (1.2, [-20, 0, 0]), (1.5, [0, 0, 0])),
                           'position': kf((0, [0, 0, 0]), (0.4, [0, 2, 0]), (1.2, [0, 2, 0]), (1.5, [0, 0, 0]))},
                  'head': {'rotation': kf((0, [0, 0, 0]), (0.4, [-22, 0, 0]), (1.2, [-22, 0, 0]), (1.5, [0, 0, 0]))},
                  'beak': {'rotation': kf((0, [0, 0, 0]), (0.4, [30, 0, 0]), (1.2, [30, 0, 0]), (1.4, [0, 0, 0]))}},
                 arm_keys((0, (25, -45, 155)), (0.4, (-5, -80, 40, 0, -35)), (1.2, (-5, -80, 40, 0, -35)), (1.5, (25, -45, 155))))
    stagger = merge({'body': {'rotation': kf((0, [0, 0, 0]), (0.2, [6, 0, 14]), (0.6, [4, 0, -10]), (1.0, [6, 0, 8]), (1.5, [3, 0, -4]), (2.0, [0, 0, 0])),
                              'position': kf((0, [0, 0, 0]), (0.2, [0, -3, 0]), (1.6, [0, -3, 0]), (2.0, [0, 0, 0]))}},
                    arm_keys((0, (25, -45, 155)), (0.3, (55, 10, 120, 10)), (1.6, (55, 10, 120, 10)), (2.0, (25, -45, 155))))
    anim('terror_of_the_bog', {
        'misc.idle': {'loop': True, 'animation_length': 8.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.4, 'bones': walk},
        'misc.dormant': {'loop': True, 'animation_length': 8.0, 'bones': dormant},
        'misc.intro': {'loop': 'hold_on_last_frame', 'animation_length': 5.0, 'bones': intro},
        'attack.strike': {'loop': False, 'animation_length': 1.1, 'bones': snap},
        'attack.snap': {'loop': False, 'animation_length': 1.1, 'bones': snap},
        'attack.grab': {'loop': False, 'animation_length': 3.9, 'bones': grab},
        'attack.throw': {'loop': False, 'animation_length': 0.6, 'bones': throw},
        'attack.submerge': {'loop': 'hold_on_last_frame', 'animation_length': 0.6, 'bones': submerge},
        'attack.erupt': {'loop': False, 'animation_length': 0.8, 'bones': erupt},
        'attack.spit': {'loop': False, 'animation_length': 1.3, 'bones': spit},
        'attack.roar': {'loop': False, 'animation_length': 1.5, 'bones': roar},
        'attack.stagger': {'loop': False, 'animation_length': 2.0, 'bones': stagger},
    })


# ---- Waxen Queen -------------------------------------------------------------------------------

def waxen_queen():
    """Queen wasp ~4.5 blocks long, wingspan ~6: dark thorax with a cream fur collar, huge drooping abdomen
    ringed in waxy cream and amber plates with glowing wax beads on the joints, a long sting, big head
    with amber compound eyes and a crown of wax spikes, heavy mandibles, elbowed antennae, two pairs of
    amber wings, dangling legs."""
    P = {'qw_dark': '#2a1a10', 'qw_wax': '#efe0b8', 'qw_amber': '#d8962a', 'qw_band': '#5a3418', 'qw_fur': '#f2e6c8',
         'qw_eye': '#b8501a', 'qw_crown': '#ffe08a', 'qw_drip': '#ffc84a', 'qw_wing': '#e8c888', 'qw_vein': '#8a5a20',
         'qw_leg': '#3a2414', 'qw_sting': '#1a0e08'}
    mobparts.mobkit.PALETTE.update(P)
    mobparts.mobkit.GLOW_KEYS.update({'qw_drip', 'qw_crown'})
    bones = [dict(name='body', pivot=[0, 30, 0])]
    cubes = [
        box('body', (0, 30, -2), (16, 15, 20), 'qw_dark', 'chitin', decor={'up': stripes_x('qw_amber', 99, 7), 'east': stripes_x('qw_amber', 6, 2), 'west': stripes_x('qw_amber', 6, 3)}),
        box('body', (0, 38, 3), (10, 2, 8), 'qw_amber', 'chitin'),   # scutellum
        box('body', (0, 30, -10), (18, 16, 5), 'qw_fur', 'fur', inflate=0.8),   # fur collar
        box('body', (0, 29, 9.5), (6, 6, 5), 'qw_dark', 'chitin'),   # waist
        box('body', (0, 23.5, -2), (12, 2, 16), 'qw_band', 'chitin'),
    ]
    # abdomen: eight banded segments drooping behind
    segs = []
    drips = dots('qw_drip', [(1, 0), (4, 0), (7, 0), (10, 0), (13, 0), (16, 0), (19, 0)])
    for i, (L, w, h, rx) in enumerate([(8, 18, 17, -22), (8, 22, 20, -6), (8, 24, 21, -6), (8, 23, 20, -7), (7, 20, 17, -8), (7, 16, 13, -8), (6, 11, 9, -9), (5, 6, 6, -10)]):
        col = 'qw_wax' if i % 2 == 0 else 'qw_amber'
        segs.append((L, w, h, 29, rx, col, 'chitin', {'up': both(edge('qw_band', 'left'), drips), 'east': both(edge('qw_band'), drips), 'west': both(edge('qw_band', 'left'), drips), 'down': edge('qw_band', 'left')}))
    abdomen, end = chain_z('ab', 'body', 12, segs, bones, cubes)
    for i, b in enumerate(abdomen[:6]):   # raised wax plates on the back of each segment
        w = segs[i][1]
        cubes.append(box(b, (0, 29 + segs[i][2] / 2 + 0.5, 12 + sum(x[0] for x in segs[:i]) + segs[i][0] / 2), (w - 6, 1, segs[i][0] - 2), 'qw_wax', 'chitin'))
    cubes.append(box(abdomen[-1], (0, 29, end + 6), (2, 2, 12), 'qw_sting', 'chitin'))
    cubes.append(box(abdomen[-1], (0, 29, end + 13), (1, 1, 3), 'qw_drip', 'glow', glow=True))
    # head with crown
    bones.append(dict(name='head', parent='body', pivot=[0, 31, -12]))
    cubes += [
        box('head', (0, 31, -18), (18, 14, 12), 'qw_amber', 'chitin', decor={'north': dots('qw_band', [(8, 2), (9, 2), (8, 3), (9, 3), (8, 4), (9, 4)]), 'up': stripes_x('qw_band', 99, 8)}),
        box('head', (0, 25, -21), (10, 4, 6), 'qw_wax', 'chitin'),   # clypeus
        box('head', (9, 32, -18), (5, 11, 9), 'qw_eye', 'facet'),
        box('head', (-9, 32, -18), (5, 11, 9), 'qw_eye', 'facet'),
        box('head', (3, 37.5, -23), (2, 2, 1), 'qw_dark', 'chitin'),
        box('head', (-3, 37.5, -23), (2, 2, 1), 'qw_dark', 'chitin'),
        box('head', (0, 39, -16), (14, 2, 8), 'qw_crown', 'glow', glow=True),
    ]
    crown = []
    for k, (x, z, h, tilt) in enumerate(((0, -18, 9, -10), (5, -17, 7, -8), (-5, -17, 7, -8), (8, -14, 5, -2), (-8, -14, 5, -2))):
        n = f'crown{k}'
        bones.append(dict(name=n, parent='head', pivot=[x, 40, z], rotation=[tilt, 0, -x * 2]))
        cubes.append(box(n, (x, 40 + h / 2, z), (3, h, 3), 'qw_crown', 'glow', glow=True))
        cubes.append(box(n, (x, 40 + h + 0.5, z), (1, 1, 1), 'qw_wax', 'chitin'))
        crown.append(n)
    mand = []
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[side * 4, 24, -22], rotation=[0, -25 * side, 0]))
        cubes.append(box(n, (side * 4, 24, -26), (4, 3, 8), 'qw_band', 'chitin'))
        cubes.append(box(n, (side * 2.5, 24, -30), (2, 2, 3), 'qw_dark', 'chitin'))
        mand.append(n)
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 3, 36, -23), side,
                [(0, 2, [0, 65, 0], 'qw_dark', None), (9, 2, [0, 0, -50], 'qw_dark', 'chitin'), (14, 1, [0, 0, 70], 'qw_amber', None), (4, 1, [0, 0, 10], 'qw_dark', None)], bones, cubes)
    wings = {}
    for side in (1, -1):
        s_ = 'l' if side > 0 else 'r'
        for kw, zf, chord, span in (('f', -8, 14, 46), ('b', 2, 10, 34)):
            n = f'wing_{kw}{s_}'
            wings[n] = side
            bones.append(dict(name=n, parent='body', pivot=[side * 6, 38, zf], rotation=[0, -24 * side, 8 * side]))
            ox = 6 if side > 0 else -6 - span
            cubes.append(cube(n, [ox, 38, zf], [span, 0, chord], 'qw_wing@130', rim=False, noise=2,
                              decor={'up': wing_veins('qw_vein', 'qw_band'), 'down': wing_veins('qw_vein', 'qw_band')}))
            cubes.append(cube(n, [ox, 37.5, zf - 0.5], [span, 1, 1.5], 'qw_vein', mat='chitin'))
    legs = leg_pairs('leg', 'body', [
        ((7, 23, -8), [(10, 3, [0, 25, 45], 'qw_dark', 'chitin'), (13, 2, [0, 0, 50], 'qw_amber', 'chitin'), (9, 2, [0, 0, -20], 'qw_leg', None), (3, 1, [0, 0, -30], 'qw_dark', None)]),
        ((7, 23, -2), [(10, 3, [0, 0, 50], 'qw_dark', 'chitin'), (14, 2, [0, 0, 50], 'qw_amber', 'chitin'), (9, 2, [0, 0, -20], 'qw_leg', None), (3, 1, [0, 0, -30], 'qw_dark', None)]),
        ((7, 23, 4), [(12, 3, [0, -30, 50], 'qw_dark', 'chitin'), (15, 2, [0, 0, 50], 'qw_amber', 'chitin'), (10, 2, [0, 0, -20], 'qw_leg', None), (3, 1, [0, 0, -30], 'qw_dark', None)]),
    ], bones, cubes)
    make('waxen_queen', bones, cubes, 102, bounds=(7, 5, 1), glow=True)

    def fly(speed, amp, extra=None):
        out = flap(wings, speed, amp, lambda n: 0 if n.startswith('wing_f') else 60)
        out['body'] = {'position': [0, sin(120, 1.2), 0]}
        out = leg_sway(legs, 90, 7, out)
        for i, b in enumerate(abdomen):
            out[b] = {'rotation': [sin(100, 2.5, -i * 45), 0, 0]}
        for s_ in 'lr':
            out[f'antenna_{s_}2'] = {'rotation': [0, 0, sin(80, 8, 0 if s_ == 'l' else 90)]}
        return merge(out, extra or {})
    fold = {n: {'rotation': [0, -62 * sd, -6 * sd]} for n, sd in wings.items()}
    curl = lambda a: {b: {'rotation': [a * w, 0, 0]} for b, w in zip(abdomen, (1.0, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.3))}
    ab_kf = lambda *pairs: {b: {'rotation': kf(*[(t, [a * w, 0, 0]) for t, a in pairs])} for b, w in zip(abdomen, (1.0, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.3))}
    legs_tuck = {names[1]: {'rotation': [0, 0, -35 * side]} for names, i, side in legs}
    dormant = merge(fold, legs_tuck, {'body': {'position': [0, -10, 0], 'rotation': [6, 0, 0]}, 'head': {'rotation': [15, 0, 0]}},
                    {b: {'rotation': [8 + 0 * i, 0, 0]} for i, b in enumerate(abdomen)})
    wing_kf = lambda *pairs: {n: {'rotation': kf(*[(t, [0, a[0] * sd, a[1] * sd]) for t, a in pairs])} for n, sd in wings.items()}
    intro = merge(
        {'body': {'position': kf((0, [0, -10, 0]), (1.2, [0, -10, 0]), (2.2, [0, 2, 0]), (2.6, [0, 4, 0]), (4.0, [0, 4, 0]), (5.0, [0, 0, 0])),
                  'rotation': kf((0, [6, 0, 0]), (1.2, [6, 0, 0]), (2.2, [-14, 0, 0]), (3.8, [-16, 0, 0]), (5.0, [0, 0, 0]))},
         'head': {'rotation': kf((0, [15, 0, 0]), (1.2, [15, 0, 0]), (2.4, [-22, 0, 0]), (3.8, [-22, 0, 0]), (4.6, [0, 0, 0]))}},
        wing_kf((0, (-62, -6)), (1.2, (-62, -6)), (1.8, (10, 40)), (2.2, (0, 10)), (2.4, (5, 45)), (2.6, (0, 5)), (2.8, (5, 45)), (3.0, (0, 5)),
                (3.2, (5, 45)), (3.4, (0, 5)), (3.6, (5, 45)), (3.8, (0, 5)), (5.0, (0, 0))),
        pair_kf(mand[0], mand[1], (0, [0, 0, 0]), (2.3, [0, 0, 0]), (2.5, [0, 30, 0]), (3.7, [0, 30, 0]), (4.0, [0, 0, 0])),
        ab_kf((0, 8), (1.2, 8), (2.2, -10), (3.0, 16), (3.8, -6), (5.0, 0)),
        {names[1]: {'rotation': kf((0, [0, 0, -35 * side]), (1.2, [0, 0, -35 * side]), (2.0, [0, 0, 0]))} for names, i, side in legs},
        {c: {'rotation': kf((0, [0, 0, 0]), (2.4, [0, 0, 0]), (2.7, [-8, 0, 0]), (3.6, [-8, 0, 0]), (4.0, [0, 0, 0]))} for c in crown})
    dive = merge(fly(3200, 44), ab_kf((0, 0), (1.0, 55), (1.2, 60), (1.35, -10), (1.8, 0)),
                 {'body': {'rotation': kf((0, [0, 0, 0]), (1.0, [-10, 0, 0]), (1.2, [35, 0, 0]), (1.8, [10, 0, 0]))}})
    recover = merge(fold, legs_tuck, {'body': {'position': kf((0, [0, -6, 0]), (1.3, [0, -6, 0]), (1.6, [0, 0, 0])),
                                               'rotation': kf((0, [8, 0, 10]), (0.4, [8, 0, -8]), (0.8, [8, 0, 6]), (1.2, [6, 0, -3]), (1.6, [0, 0, 0]))},
                                      'head': {'rotation': kf((0, [14, 0, 0]), (1.3, [14, 0, 0]), (1.6, [0, 0, 0]))}})
    spray = merge(fly(2800, 38), ab_kf((0, 0), (0.6, 50), (0.75, 70), (0.85, 40), (1.4, 0)),
                  {'ab3': {'scale': kf((0, [1, 1, 1]), (0.3, [1.1, 1.1, 1]), (0.5, [1, 1, 1]), (0.7, [1.15, 1.15, 1]), (0.8, [0.95, 0.95, 1]), (1.4, [1, 1, 1]))}})
    lay = merge(fly(2400, 30), ab_kf((0, 0), (0.6, 40), (1.0, 50), (1.3, 30), (2.0, 0)),
                {'ab5': {'scale': kf((0, [1, 1, 1]), (0.8, [1.2, 1.2, 1]), (1.0, [0.9, 0.9, 1]), (2.0, [1, 1, 1]))}})
    buffet = merge(wing_kf((0, (0, 0)), (0.5, (0, 70)), (0.6, (0, 70)), (0.7, (0, -45)), (1.1, (0, 0))),
                   {'body': {'position': kf((0, [0, 0, 0]), (0.5, [0, 3, 0]), (0.7, [0, -2, 0]), (1.1, [0, 0, 0]))}})
    screech = merge(fly(3600, 30), {'head': {'rotation': kf((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (1.2, [-30, 0, 0]), (1.5, [0, 0, 0]))}},
                    pair_kf(mand[0], mand[1], (0, [0, 0, 0]), (0.3, [0, 35, 0]), (1.2, [0, 35, 0]), (1.5, [0, 0, 0])),
                    {c: {'rotation': kf((0, [0, 0, 0]), (0.3, [-10, 0, 0]), (1.2, [-10, 0, 0]), (1.5, [0, 0, 0]))} for c in crown})
    anim('waxen_queen', {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': fly(2600, 34)},
        'move.fly': {'loop': True, 'animation_length': 6.0, 'bones': fly(3000, 40, {'body': {'rotation': [-8, 0, 0]}})},
        'move.walk': {'loop': True, 'animation_length': 6.0, 'bones': fly(3000, 40)},
        'misc.dormant': {'loop': True, 'animation_length': 8.0, 'bones': dormant},
        'misc.intro': {'loop': 'hold_on_last_frame', 'animation_length': 5.0, 'bones': intro},
        'attack.dive': {'loop': False, 'animation_length': 1.8, 'bones': dive},
        'attack.recover': {'loop': False, 'animation_length': 1.6, 'bones': recover},
        'attack.spray': {'loop': False, 'animation_length': 1.4, 'bones': spray},
        'attack.lay': {'loop': False, 'animation_length': 2.0, 'bones': lay},
        'attack.buffet': {'loop': False, 'animation_length': 1.1, 'bones': buffet},
        'attack.screech': {'loop': False, 'animation_length': 1.5, 'bones': screech},
    })


# ---- Dozen-legged Wrath ------------------------------------------------------------------------

def dozen_legged_wrath():
    """Giant red centipede ~11 blocks long: broad flat head with glowing eye clusters, long segmented
    antennae and huge curved venom claws; six body segments each with a pair of long splayed orange legs
    (twelve legs), dark-edged red back plates; a tail segment with two long trailing terminal legs.
    Segment bones seg1..seg7 chain backwards; DozenLeggedWrath turns each by the heading history."""
    P = {'dw_red': '#a8241a', 'dw_dark': '#5a1210', 'dw_edge': '#1a0806', 'dw_belly': '#d8703a', 'dw_leg': '#e07a2a',
         'dw_joint': '#2a0e08', 'dw_eye': '#ffb040', 'dw_claw': '#1e0a06', 'dw_claw_tip': '#c8a070'}
    mobparts.mobkit.PALETTE.update(P)
    mobparts.mobkit.GLOW_KEYS.add('dw_eye')
    Y = 10
    plate = lambda w: {'up': both(stripes_y('dw_edge', 99, 0), edge('dw_dark', 'left'), dots('dw_dark', [(w // 2 - 1, 3), (w // 2, 3), (w // 2 - 1, 8), (w // 2, 8)])),
                       'east': stripes_y('dw_edge', 99, 1), 'west': stripes_y('dw_edge', 99, 1)}
    bones = [dict(name='body', pivot=[0, Y, 0])]
    cubes = [box('body', (0, Y, 0), (20, 8, 16), 'dw_red', 'chitin', decor=plate(20)),
             box('body', (0, Y - 4.5, 0), (16, 1, 14), 'dw_belly', 'chitin')]
    # head
    bones.append(dict(name='head', parent='body', pivot=[0, Y, -8]))
    cubes += [
        box('head', (0, Y + 0.5, -16), (24, 7, 16), 'dw_red', 'chitin', decor={'up': both(stripes_x('dw_dark', 99, 11), stripes_x('dw_dark', 99, 12), stripes_y('dw_edge', 99, 15)), 'north': stripes_y('dw_edge', 99, 6)}),
        box('head', (0, Y + 4.5, -14), (16, 2, 10), 'dw_dark', 'chitin'),
        box('head', (0, Y - 3.5, -18), (14, 2, 10), 'dw_belly', 'chitin'),
    ]
    for side in (1, -1):
        for k, (dx, dz) in enumerate(((9, -21), (10.5, -19), (8.5, -18.5), (10, -17))):   # eye clusters
            cubes.append(box('head', (side * dx, Y + 4.4, dz), (2, 1.2, 2), 'dw_eye', 'glow', glow=True))
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 5, Y + 2, -24), side,
                [(0, 2, [0, 70, 0], 'dw_dark', None)] + [(6, 2 if i < 3 else 1, [0, 8 if i else 0, -8 if i == 0 else 4], 'dw_red' if i % 2 else 'dw_dark', 'chitin') for i in range(7)], bones, cubes)
    claws = []
    for side in (1, -1):
        claws.append(chain_x(f'claw_{"l" if side > 0 else "r"}', 'body', (side * 7, Y - 3, -8), side, [
            (0, 3, [0, 55, 0], 'dw_claw', None),
            (8, (4, 4), [0, 0, 8], 'dw_dark', 'chitin'),
            (9, (3, 3), [0, 40, 0], 'dw_claw', 'chitin'),
            (7, (2, 2), [0, 45, 0], 'dw_claw', 'chitin'),
            (3, 1, [0, 30, 0], 'dw_claw_tip', 'chitin'),
        ], bones, cubes))
    # trailing segments with one leg pair each, then the tail
    L = 22
    widths = [20, 20, 19, 18, 16, 14, 12]
    legs = []
    prev = 'body'
    for i in range(1, 8):
        n = f'seg{i}'
        z0 = 8 + (i - 1) * L
        w = widths[i - 1]
        h = 8 - (i - 1) * 0.4
        bones.append(dict(name=n, parent=prev, pivot=[0, Y, z0]))
        cubes.append(box(n, (0, Y, z0 + 1), (w - 6, h - 2, 2), 'dw_joint', 'chitin'))   # soft joint
        cubes.append(box(n, (0, Y, z0 + 2 + (L - 2) / 2), (w, h, L - 2), 'dw_red', 'chitin', decor=plate(w)))
        cubes.append(box(n, (0, Y - h / 2 - 0.5, z0 + 2 + (L - 2) / 2), (w - 4, 1, L - 4), 'dw_belly', 'chitin'))
        cubes.append(box(n, (0, Y + h / 2 + 0.5, z0 + 2 + (L - 2) / 2), (w - 8, 1, L - 6), 'dw_dark', 'chitin'))
        if i <= 6:
            for side in (1, -1):
                ln = chain_x(f'leg{i}{"l" if side > 0 else "r"}', n, (side * (w / 2 - 1), Y - 2, z0 + 11), side, [
                    (11, 3, [0, -10 - i * 4, 22], 'dw_leg', 'chitin'),
                    (14, 2, [0, 0, 50], 'dw_leg', 'chitin'),
                    (8, 1.5, [0, 0, -15], 'dw_joint', None),
                ], bones, cubes)
                legs.append((ln, i, side))
        prev = n
    # terminal legs trailing from the tail
    for side in (1, -1):
        chain_x(f'cerci_{"l" if side > 0 else "r"}', 'seg7', (side * 5, Y, 8 + 6 * L + 16), side, [
            (0, 2, [0, -75, 0], 'dw_leg', None), (14, 2, [0, 0, 6], 'dw_leg', 'chitin'), (12, 2, [0, 6, 4], 'dw_joint', 'chitin'), (8, 1, [0, 4, 0], 'dw_leg', None)], bones, cubes)
    make('dozen_legged_wrath', bones, cubes, 103, bounds=(10, 3, 1), glow=True)

    segs = [f'seg{i}' for i in range(1, 8)]
    def seg_rot(fn):
        return {s_: {'rotation': fn(i)} for i, s_ in enumerate(segs)}

    def walk_legs(speed, swing, lift):
        out = {}
        for names, i, side in legs:
            ph = i * 60 + (0 if side > 0 else 180)
            out[names[0]] = {'rotation': [0, sin(speed, swing * side, ph), f'math.max(0, math.sin(query.anim_time * {speed} + {ph + 90})) * {-lift * side}']}
        return out
    claws_kf = lambda *pairs: pair_kf(claws[0][2], claws[1][2], *pairs)
    ant = {f'antenna_{s_}{k}': {'rotation': [0, sin(70, 5, k * 40 + (0 if s_ == 'l' else 90)), sin(90, 4, k * 30)]} for s_ in 'lr' for k in (1, 3, 5)}
    idle = merge(seg_rot(lambda i: [0, sin(40, 2, -i * 50), 0]), ant, {'head': {'rotation': [sin(50, 2), sin(30, 4), 0]}},
                 {names[0]: {'rotation': [0, 0, sin(60, 3, i * 40)]} for names, i, side in legs})
    walk = merge(seg_rot(lambda i: [0, sin(300, 4, -i * 55), 0]), walk_legs(600, 20, 14), ant)
    run = merge(seg_rot(lambda i: [0, sin(500, 5, -i * 55), 0]), walk_legs(1000, 24, 16), ant)
    coil = merge(seg_rot(lambda i: [3, 30, 0]), {'head': {'rotation': [14, 25, 0]}, 'body': {'position': [0, -4, 0]}},
                 {names[1]: {'rotation': [0, 0, -35 * side]} for names, i, side in legs})
    rear = lambda a: {'body': [-a, 0, 0], 'seg1': [a * 0.6, 0, 0], 'seg2': [a * 0.4, 0, 0]}
    intro = merge(
        {s_: {'rotation': kf((0, [3, 30, 0]), (1.0, [3, 30, 0]), (2.2, [0, 0, 0]))} for s_ in segs},
        {'body': {'position': kf((0, [0, -4, 0]), (1.0, [0, -4, 0]), (2.2, [0, 0, 0]), (2.6, [0, 8, 0]), (4.0, [0, 8, 0]), (4.8, [0, 0, 0])),
                  'rotation': kf((0, [0, 0, 0]), (2.2, [0, 0, 0]), (2.6, [-38, 0, 0]), (4.0, [-38, 0, 0]), (4.8, [0, 0, 0]))},
         'seg1': {'rotation': kf((0, [3, 30, 0]), (1.0, [3, 30, 0]), (2.2, [0, 0, 0]), (2.6, [24, 0, 0]), (4.0, [24, 0, 0]), (4.8, [0, 0, 0]))},
         'seg2': {'rotation': kf((0, [3, 30, 0]), (1.0, [3, 30, 0]), (2.2, [0, 0, 0]), (2.6, [14, 0, 0]), (4.0, [14, 0, 0]), (4.8, [0, 0, 0]))},
         'head': {'rotation': kf((0, [14, 25, 0]), (1.0, [14, 25, 0]), (2.2, [0, 0, 0]), (2.6, [10, 0, 0]), (3.0, [-6, 8, 0]), (3.4, [6, -8, 0]), (4.0, [0, 0, 0]))}},
        claws_kf((0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.8, [0, -40, 0]), (3.8, [0, -40, 0]), (4.2, [0, 0, 0])),
        {names[1]: {'rotation': kf((0, [0, 0, -35 * side]), (1.0, [0, 0, -35 * side]), (2.0, [0, 0, 0]))} for names, i, side in legs},
        {names[0]: {'rotation': kf((0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.8, [0, 0, -30 * side]), (4.0, [0, 0, -30 * side]), (4.6, [0, 0, 0]))} for names, i, side in legs if i <= 2})
    bite = merge(seg_rot(lambda i: [0, 0, 0]), {'head': {'rotation': kf((0, [0, 0, 0]), (0.45, [-22, 0, 0]), (0.52, [18, 0, 0]), (0.9, [0, 0, 0]))},
                                                'body': {'position': kf((0, [0, 0, 0]), (0.45, [0, 0, 3]), (0.52, [0, 0, -5]), (0.9, [0, 0, 0]))}},
                 claws_kf((0, [0, 0, 0]), (0.45, [0, -45, 0]), (0.52, [0, 30, 0]), (0.9, [0, 0, 0])))
    charge = merge(
        {'body': {'rotation': kf((0, [0, 0, 0]), (0.5, [-34, 0, 0]), (0.95, [-34, 0, 0]), (1.05, [4, 0, 0]), (1.3, [0, 0, 0])),
                  'position': kf((0, [0, 0, 0]), (0.5, [0, 7, 0]), (0.95, [0, 7, 0]), (1.05, [0, 0, 0]))},
         'seg1': {'rotation': kf((0, [0, 0, 0]), (0.5, [22, 0, 0]), (0.95, [22, 0, 0]), (1.05, [0, 0, 0]))},
         'seg2': {'rotation': kf((0, [0, 0, 0]), (0.5, [12, 0, 0]), (0.95, [12, 0, 0]), (1.05, [0, 0, 0]))},
         'head': {'rotation': kf((0, [0, 0, 0]), (0.3, [-8, 6, 0]), (0.55, [-8, -6, 0]), (0.8, [-8, 6, 0]), (1.0, [0, 0, 0]))}},
        claws_kf((0, [0, 0, 0]), (0.4, [0, -40, 0]), (0.95, [0, -40, 0]), (1.1, [0, 0, 0])))
    stun = merge(seg_rot(lambda i: [0, 0, 0]), {'head': {'rotation': kf((0, [20, 0, 0]), (2.2, [20, 0, 0]), (2.5, [0, 0, 0]))},
                                                'body': {'rotation': kf((0, [0, 0, 8]), (0.6, [0, 0, -8]), (1.2, [0, 0, 6]), (1.8, [0, 0, -4]), (2.5, [0, 0, 0]))}},
                 {names[1]: {'rotation': kf((0, [0, 0, -25 * side]), (2.2, [0, 0, -25 * side]), (2.5, [0, 0, 0]))} for names, i, side in legs})
    burrow = {'body': {'position': kf((0, [0, 0, 0]), (0.7, [0, -34, 0])), 'rotation': kf((0, [0, 0, 0]), (0.3, [30, 0, 0]), (0.7, [40, 0, 0]))}}
    erupt = merge({'body': {'position': kf((0, [0, -24, 0]), (0.2, [0, 10, 0]), (0.8, [0, 0, 0])), 'rotation': kf((0, [-50, 0, 0]), (0.3, [-30, 0, 0]), (0.8, [0, 0, 0]))},
                   'seg1': {'rotation': kf((0, [40, 0, 0]), (0.8, [0, 0, 0]))}, 'seg2': {'rotation': kf((0, [30, 0, 0]), (0.8, [0, 0, 0]))}},
                  claws_kf((0, [0, -40, 0]), (0.5, [0, -40, 0]), (0.8, [0, 0, 0])))
    sweep = {s_: {'rotation': kf((0, [0, 0, 0]), (0.55, [0, 22 + i * 2, 0]), (0.75, [0, -30 - i * 3, 0]), (1.1, [0, 0, 0]))} for i, s_ in enumerate(segs)}
    anim('dozen_legged_wrath', {
        'misc.idle': {'loop': True, 'animation_length': 8.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.2, 'bones': walk},
        'misc.dormant': {'loop': True, 'animation_length': 8.0, 'bones': merge(coil, seg_rot(lambda i: [3, f'30 + {sin(20, 1, i * 40)}', 0]))},
        'misc.intro': {'loop': 'hold_on_last_frame', 'animation_length': 5.0, 'bones': intro},
        'attack.bite': {'loop': False, 'animation_length': 0.9, 'bones': bite},
        'attack.charge': {'loop': False, 'animation_length': 2.6, 'bones': merge(run, charge)},
        'attack.stun': {'loop': False, 'animation_length': 2.5, 'bones': stun},
        'attack.burrow': {'loop': 'hold_on_last_frame', 'animation_length': 0.7, 'bones': burrow},
        'attack.erupt': {'loop': False, 'animation_length': 0.8, 'bones': erupt},
        'attack.sweep': {'loop': False, 'animation_length': 1.1, 'bones': sweep},
    })


# ---- Apex Mantis -------------------------------------------------------------------------------

def apex_mantis():
    """Towering green mantis ~4.4 blocks tall: raised prothorax, wide triangular head with big glowing
    eyes and a crest, huge blade-arms (spined femur, long serrated tibia blade ending in a hook), leaf-veined
    wing covers that flare open for a threat display, a lobed leaf-like abdomen and four long legs."""
    P = {'am_body': '#5aa040', 'am_light': '#a8d870', 'am_dark': '#2a5a20', 'am_edge': '#e8f0b0', 'am_eye': '#d8f07a',
         'am_wing': '#8cc860', 'am_vein': '#3a7028', 'am_hwing': '#c8e8a0', 'am_spot': '#c84a6a'}
    mobparts.mobkit.PALETTE.update(P)
    mobparts.mobkit.GLOW_KEYS.add('am_eye')
    k = 2.6
    S = lambda v: v * k
    I = lambda v: max(1, int(round(v * k)))
    bones = [dict(name='body', pivot=[0, S(10), 0])]
    cubes = [box('body', (0, S(10), S(-0.5)), (I(5), I(4), I(7)), 'am_body', 'chitin', decor={'up': stripes_x('am_light', 99, I(2.5)), 'east': edge('am_dark'), 'west': edge('am_dark', 'left')})]
    ab_decor = {'up': both(stripes_x('am_light', 99, 1), edge('am_dark', 'left')), 'east': edge('am_dark'), 'west': edge('am_dark', 'left'), 'down': stripes_y('am_dark', 3, 1)}
    segs = [(I(4), I(7), I(4), S(10.5), -8, 'am_body', 'chitin', ab_decor), (I(4), I(8), I(4), S(10.5), 0, 'am_body', 'chitin', ab_decor),
            (I(4), I(7), I(4), S(10.5), 6, 'am_body', 'chitin', ab_decor), (I(4), I(6), I(3), S(10.5), 6, 'am_body', 'chitin', ab_decor),
            (I(3), I(3), I(3), S(10.5), 8, 'am_dark', 'chitin', None)]
    abdomen, _ = chain_z('ab', 'body', S(3), segs, bones, cubes)
    for i, b in enumerate(abdomen[:4]):   # leafy lobes on the abdomen sides
        w = segs[i][1]
        for sx in (-1, 1):
            cubes.append(box(b, (sx * (w / 2 + 2), S(10.5), S(3) + sum(x[0] for x in segs[:i]) + segs[i][0] / 2), (4, 1, segs[i][0] - 2), 'am_light', 'leather', decor={'up': edge('am_dark')}))
    # wing covers + hind wings, each side its own bone so they can flare
    wings = []
    for side in (1, -1):
        sd = 'l' if side > 0 else 'r'
        n = f'wing_{sd}'
        bones.append(dict(name=n, parent='body', pivot=[side * 1, S(12.8), S(1)], rotation=[0, 0, -4 * side]))
        cubes.append(box(n, (side * S(2), S(12.9), S(9)), (I(4.5), 1, I(16)), 'am_wing', 'leather',
                         decor={'up': both(stripes_y('am_vein', 6, 2), stripes_x('am_vein', 99, I(2))), 'down': stripes_y('am_vein', 6, 2)}))
        cubes.append(box(n, (side * S(3.2), S(13.1), S(6)), (3, 1, 3), 'am_spot', 'leather'))   # eyespot shown in the display
        h = f'hwing_{sd}'
        bones.append(dict(name=h, parent='body', pivot=[side * 1, S(12.4), S(1)], rotation=[0, 0, -2 * side]))
        cubes.append(cube(h, [0 if side > 0 else -I(5), S(12.4), S(1)], [I(5), 0, I(15)], 'am_hwing@140', rim=False, noise=2,
                          decor={'up': stripes_y('am_vein', 5, 1), 'down': stripes_y('am_vein', 5, 1)}))
        wings.append((n, h, side))
    bones.append(dict(name='neck', parent='body', pivot=[0, S(11), S(-4)], rotation=[-50, 0, 0]))
    cubes.append(box('neck', (0, S(11), S(-9)), (I(3), I(3), I(10)), 'am_body', 'chitin', decor={'up': both(stripes_y('am_light', 99, 1), stripes_x('am_dark', 99, I(1.5)))}))
    cubes.append(box('neck', (0, S(11), S(-5.5)), (I(4.5), I(2.5), I(3)), 'am_body', 'chitin'))
    bones.append(dict(name='head', parent='neck', pivot=[0, S(11), S(-14)], rotation=[50, 0, 0]))
    cubes += [
        box('head', (0, S(12), S(-15.5)), (I(8), I(3), I(3)), 'am_body', 'chitin', decor={'north': dots('am_dark', [(I(4) - 1, 1), (I(4), 1)]), 'up': stripes_x('am_light', 99, I(4))}),
        box('head', (0, S(9.5), S(-15.5)), (I(4), I(2), I(3)), 'am_body', 'chitin'),
        box('head', (0, S(8), S(-15.5)), (I(2), I(1), I(2)), 'am_dark', 'chitin'),
        box('head', (S(4.2), S(12.6), S(-15.5)), (I(2.2), I(3.2), I(3)), 'am_eye', 'facet', glow=True),
        box('head', (S(-4.2), S(12.6), S(-15.5)), (I(2.2), I(3.2), I(3)), 'am_eye', 'facet', glow=True),
        box('head', (0, S(14), S(-15.5)), (I(1.2), I(1.4), I(1.2)), 'am_light', 'chitin'),   # crest
    ]
    for side in (1, -1):
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * S(1), S(13.5), S(-16)), side,
                [(0, 2, [0, 70, 0], 'am_dark', None), (I(5), 2, [0, 0, -55], 'am_dark', 'chitin'), (I(5), 1, [0, 0, 30], 'am_dark', None)], bones, cubes)
    # blade-arms
    arms = []
    for side in (1, -1):
        names = chain_x(f'arm_{"l" if side > 0 else "r"}', 'body', (side * S(1.5), S(15.5), S(-9.5)), side, [
            (0, 4, [0, 90, 0], 'am_body', None),
            (I(5), (5, 5), [0, 0, 60], 'am_body', 'chitin'),
            (I(8), (6, 5), [0, 0, -100], 'am_light', 'chitin'),
            (I(8), (4, 3), [0, 0, 160], 'am_body', 'chitin'),
            (I(2), (3, 2), [0, 0, 40], 'am_dark', 'chitin'),
        ], bones, cubes)
        x0 = side * S(1.5)
        for j in range(5):   # femur spines
            cubes.append(box(names[2], (x0 + side * (I(5) + 3 + j * 3.6), S(15.5) - 3.5, S(-9.5)), (1.5, 3, 1.5), 'am_dark', 'chitin'))
        # pale cutting edge along the tibia blade, serrated
        cubes.append(box(names[3], (x0 + side * (I(5) + I(8) + I(8) / 2), S(15.5) - 2, S(-9.5)), (I(8) - 2, 1, 2), 'am_edge', 'metal',
                         decor={'all': stripes_x('am_dark', 3, 1)}))
        arms.append(names)
    legs = leg_pairs('leg', 'body', [
        ((S(2.5), S(9), S(0.5)), [(I(6), 3, [0, 10, 35], 'am_body', 'chitin'), (I(8), 2, [0, 0, 55], 'am_dark', 'chitin'), (I(3), 2, [0, 0, -40], 'am_dark', None)]),
        ((S(2.5), S(9), S(2.5)), [(I(7), 3, [0, -30, 30], 'am_body', 'chitin'), (I(9), 2, [0, 0, 60], 'am_dark', 'chitin'), (I(3), 2, [0, 0, -40], 'am_dark', None)]),
    ], bones, cubes)
    make('apex_mantis', bones, cubes, 104, bounds=(6, 6, 1.5), glow=True)

    A = lambda i, s_: arms[0 if s_ == 'l' else 1][i]
    def arm_kf(*frames):
        """frames: (t, (femur, tibia[, coxa[, yaw]])) as deltas from the rest pose; right side mirrored."""
        out = {}
        for t, args in frames:
            f, tb = args[0], args[1]
            cx = args[2] if len(args) > 2 else 0
            yw = args[3] if len(args) > 3 else 0
            for s_, sg in (('l', 1), ('r', -1)):
                for i, v in ((0, [0, yw * sg, 0]), (1, [0, 0, cx * sg]), (2, [0, 0, f * sg]), (3, [0, 0, tb * sg])):
                    out.setdefault(A(i, s_), {}).setdefault('rotation', {})[f'{t}' if t else '0.0'] = v
        return out
    def wing_kf(*frames):
        """frames: (t, flare): wing covers lift and spread, hind wings fan out."""
        out = {}
        for t, fl in frames:
            for n, h, sd in wings:
                out.setdefault(n, {}).setdefault('rotation', {})[f'{t}' if t else '0.0'] = [-fl * 0.5, -fl * 0.6 * sd, -fl * sd]
                out.setdefault(h, {}).setdefault('rotation', {})[f'{t}' if t else '0.0'] = [-fl * 0.3, -fl * 0.9 * sd, -fl * 0.8 * sd]
        return out
    idle = {'body': {'position': [sin(60, 0.6), 0, 0], 'rotation': [0, 0, sin(60, 2)]}, 'neck': {'rotation': [sin(45, 3), sin(30, 7), 0]},
            'head': {'rotation': [0, sin(25, 10), sin(40, 4)]}}
    walk = gait(legs, 280, 18, 14)
    walk['body'] = {'rotation': [0, 0, sin(280, 2)]}
    walk['neck'] = {'rotation': [sin(560, 2), 0, 0]}
    dormant = merge({'body': {'position': [0, -8, 0]}, 'neck': {'rotation': [25, 0, 0]}, 'head': {'rotation': [-12, 0, 0]}},
                    {names[1]: {'rotation': [0, 0, 20 * side]} for names, i, side in legs}, arm_kf((0, (10, 10))))
    intro = merge(
        {'body': {'position': kf((0, [0, -8, 0]), (1.0, [0, -8, 0]), (2.0, [0, 0, 0])), 'rotation': kf((0, [0, 0, 0]), (2.0, [0, 0, 0]), (2.6, [-8, 0, 0]), (4.2, [-8, 0, 0]), (5.0, [0, 0, 0]))},
         'neck': {'rotation': kf((0, [25, 0, 0]), (1.0, [25, 0, 0]), (2.0, [-6, 0, 0]), (2.6, [-12, 0, 0]), (4.2, [-12, 0, 0]), (5.0, [0, 0, 0]))},
         'head': {'rotation': kf((0, [-12, 0, 0]), (1.0, [-12, 0, 0]), (1.6, [0, 0, 0]), (2.0, [0, 0, 25]), (2.4, [0, 0, -15]), (2.8, [0, 0, 0]), (4.0, [0, 0, 0]))}},
        {names[1]: {'rotation': kf((0, [0, 0, 20 * side]), (1.0, [0, 0, 20 * side]), (2.0, [0, 0, 0]))} for names, i, side in legs},
        arm_kf((0, (10, 10)), (1.6, (0, 0)), (2.6, (-60, -120, -30, -25)), (4.2, (-60, -120, -30, -25)), (5.0, (0, 0))),
        wing_kf((0, 0), (2.2, 0), (2.7, 70), (4.2, 70), (4.8, 0)))
    combo = merge(arm_kf((0, (0, 0)), (0.5, (-40, -60, -20)), (0.6, (-40, -60, -20)), (0.65, (20, 0, 10, 15)), (0.9, (-30, -50, -15, -10)),
                         (1.1, (-30, -50, -15, -10)), (1.15, (20, 0, 10, -15)), (1.4, (-70, -100, -40)), (1.75, (-70, -100, -40)), (1.82, (35, 10, 20)), (2.3, (0, 0))),
                  {'body': {'rotation': kf((0, [0, 0, 0]), (0.6, [0, 12, 0]), (0.65, [6, -14, 0]), (1.1, [0, -10, 0]), (1.15, [6, 14, 0]), (1.75, [-10, 0, 0]), (1.82, [12, 0, 0]), (2.3, [0, 0, 0]))},
                   'neck': {'rotation': kf((0, [0, 0, 0]), (1.75, [-10, 0, 0]), (1.82, [10, 0, 0]), (2.3, [0, 0, 0]))}})
    leap = merge({'body': {'position': kf((0, [0, 0, 0]), (0.6, [0, -7, 0]), (0.7, [0, -7, 0]), (0.8, [0, 2, 0]), (1.5, [0, 0, 0]), (1.6, [0, -4, 0]), (2.0, [0, 0, 0])),
                           'rotation': kf((0, [0, 0, 0]), (0.7, [8, 0, 0]), (0.9, [-15, 0, 0]), (1.4, [10, 0, 0]), (1.6, [12, 0, 0]), (2.0, [0, 0, 0]))}},
                 {names[1]: {'rotation': kf((0, [0, 0, 0]), (0.6, [0, 0, 30 * side]), (0.75, [0, 0, -10 * side]), (1.4, [0, 0, 0]), (1.6, [0, 0, 20 * side]), (2.0, [0, 0, 0]))} for names, i, side in legs},
                 arm_kf((0, (0, 0)), (0.7, (10, 10)), (0.9, (-70, -110, -40)), (1.4, (-70, -110, -40)), (1.55, (30, 10, 20)), (2.0, (0, 0))),
                 wing_kf((0, 0), (0.7, 0), (0.9, 40), (1.4, 40), (1.6, 0)))
    guard = merge(arm_kf((0, (0, 0)), (0.2, (-50, -30, -10, 30)), (1.4, (-50, -30, -10, 30)), (1.6, (0, 0))),
                  {'neck': {'rotation': kf((0, [0, 0, 0]), (0.2, [10, 0, 0]), (1.4, [10, 0, 0]), (1.6, [0, 0, 0]))}})
    riposte = merge(arm_kf((0, (-50, -30, -10, 30)), (0.1, (30, 5, 15, -10)), (0.5, (0, 0))),
                    {'body': {'rotation': kf((0, [0, 0, 0]), (0.1, [10, 0, 0]), (0.5, [0, 0, 0]))}})
    fade = merge(arm_kf((0, (0, 0)), (0.5, (10, 10))), {'body': {'position': kf((0, [0, 0, 0]), (0.5, [0, -4, 0]))}})
    ambush = merge(arm_kf((0, (10, 10)), (0.4, (-70, -110, -40)), (0.5, (-70, -110, -40)), (0.56, (35, 10, 20)), (1.1, (0, 0))),
                   wing_kf((0, 0), (0.3, 70), (0.6, 70), (1.0, 0)),
                   {'body': {'position': kf((0, [0, -4, 0]), (0.4, [0, 2, 0]), (1.1, [0, 0, 0]))}})
    stagger = merge({'body': {'rotation': kf((0, [0, 0, 0]), (0.2, [0, 0, 12]), (0.6, [0, 0, -8]), (1.2, [0, 0, 0]))},
                     'neck': {'rotation': kf((0, [0, 0, 0]), (0.2, [15, 0, 0]), (1.2, [0, 0, 0]))}},
                    arm_kf((0, (0, 0)), (0.2, (30, 30, 20)), (1.2, (0, 0))))
    anim('apex_mantis', {
        'misc.idle': {'loop': True, 'animation_length': 8.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.3, 'bones': walk},
        'misc.dormant': {'loop': True, 'animation_length': 8.0, 'bones': dormant},
        'misc.intro': {'loop': 'hold_on_last_frame', 'animation_length': 5.0, 'bones': intro},
        'attack.combo': {'loop': False, 'animation_length': 2.3, 'bones': combo},
        'attack.leap': {'loop': False, 'animation_length': 2.0, 'bones': leap},
        'attack.guard': {'loop': False, 'animation_length': 1.6, 'bones': guard},
        'attack.riposte': {'loop': False, 'animation_length': 0.5, 'bones': riposte},
        'attack.fade': {'loop': 'hold_on_last_frame', 'animation_length': 0.5, 'bones': fade},
        'attack.ambush': {'loop': False, 'animation_length': 1.1, 'bones': ambush},
        'attack.stagger': {'loop': False, 'animation_length': 1.2, 'bones': stagger},
    })


ALL = [terror_of_the_bog, waxen_queen, dozen_legged_wrath, apex_mantis]

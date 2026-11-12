"""Shared body plans: mantis (Reedmantis, Bloommantis) and wasp (Combwasp, Stinger, Nursemaid)."""
from mobparts import *  # noqa: F401,F403
import mobparts  # noqa: F401


# ---- Mantis --------------------------------------------------------------------------------

def mantis(name, pal, seed, petals=False, arm=((5, [0, 0, 60]), (8, [0, 0, -100]), (7, [0, 0, 160]))):
    """Upright mantis ~1.6 blocks tall: long raised prothorax, triangular head with corner eyes,
    folded raptorial forelegs, four long walking legs, wing covers over a tapering abdomen.
    petals=True gives flower-mimic lobes on the walking legs and a broad petal-edged abdomen."""
    P = lambda k: f'{name}_{k}'
    mobparts.mobkit.PALETTE.update({P(k): v for k, v in pal.items()})
    bones = [dict(name='body', pivot=[0, 10, 0])]
    cubes = [box('body', (0, 10, -0.5), (5, 4, 7), P('body'), 'chitin',
                 decor={'up': stripes_x(P('light'), 99, 2)})]
    widen = 3 if petals else 0
    ab_decor = lambda: {'east': edge(P('dark')), 'west': edge(P('dark'), 'left'),
                        'up': stripes_x(P('light'), 99, 1) if not petals else edge(P('light'))}
    segs = [(4, 6 + widen, 4, 10.5, -8, P('body'), 'chitin', ab_decor()),
            (4, 7 + widen, 4, 10.5, 0, P('body'), 'chitin', ab_decor()),
            (4, 6 + widen, 4, 10.5, 6, P('body'), 'chitin', ab_decor()),
            (4, 5 + widen, 3, 10.5, 6, P('body'), 'chitin', ab_decor()),
            (3, 3, 3, 10.5, 8, P('dark'), 'chitin', None)]
    abdomen, _ = chain_z('ab', 'body', 3, segs, bones, cubes)
    if petals:   # petal fringe on the abdomen sides
        for i, b in enumerate(abdomen[:4]):
            w = segs[i][1]
            for sx in (-1, 1):
                cubes.append(box(b, (sx * (w / 2 + 1), 10.5, 3 + i * 4 + 2), (2, 0, 4), P('petal'), rim=False))
    # wing covers lying on the abdomen
    cubes.append(box('ab0', (0, 12.8, 10), (6 + widen, 0, 16), P('wing') + '@215', rim=False,
                     decor={'up': stripes_y(P('vein'), 4, 1), 'down': stripes_y(P('vein'), 4, 1)}))
    # prothorax "neck", raised steeply, head tipped back down to face forward
    bones.append(dict(name='neck', parent='body', pivot=[0, 11, -4], rotation=[-50, 0, 0]))
    cubes.append(box('neck', (0, 11, -9), (3, 3, 10), P('body'), 'chitin', decor={'up': stripes_y(P('light'), 99, 1)}))
    bones.append(dict(name='head', parent='neck', pivot=[0, 11, -14], rotation=[50, 0, 0]))
    cubes += [
        box('head', (0, 12, -15.5), (8, 3, 3), P('body'), 'chitin', decor={'north': dots(P('dark'), [(3, 0), (4, 0)])}),
        box('head', (0, 9.5, -15.5), (4, 2, 3), P('body'), 'chitin'),
        box('head', (0, 8, -15.5), (2, 1, 2), P('dark'), 'chitin'),
        box('head', (4, 12.5, -15.5), (2, 3, 3), P('eye'), 'facet'),
        box('head', (-4, 12.5, -15.5), (2, 3, 3), P('eye'), 'facet'),
    ]
    for side in (1, -1):
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (side * 1, 13.5, -16), side,
                [(5, 1, [0, 70, -55], P('dark'), None), (4, 1, [0, 0, 30], P('dark'), None)], bones, cubes)
    # raptorial forelegs: coxa forward-down, femur folded back up, tibia folded down (praying pose)
    arms = []
    for side in (1, -1):
        arms.append(chain_x(f'arm_{"l" if side > 0 else "r"}', 'body', (side * 1.5, 15.5, -9.5), side, [
            (0, 2, [0, 90, 0], P('body'), None),                      # shoulder: turn the arm forward
            (arm[0][0], 2, arm[0][1], P('body'), 'chitin'),
            (arm[1][0], 2, arm[1][1], P('light'), 'chitin'),
            (arm[2][0], 1, arm[2][1], P('body'), 'chitin'),
        ], bones, cubes))
    for names in arms:     # spines along the femur
        side = 1 if names[0].startswith('arm_l') else -1
        for j in (1.5, 3.5, 5.5):
            cubes.append(box(names[2], (side * (1.5 + arm[0][0] + j), 14, -9.5), (1, 1, 1), P('dark'), 'chitin'))
    legs = leg_pairs('leg', 'body', [
        ((2.5, 9, 0.5), [(6, 1, [0, 10, 35], P('body'), 'chitin'), (8, 1, [0, 0, 55], P('dark'), None), (3, 1, [0, 0, -40], P('dark'), None)]),
        ((2.5, 9, 2.5), [(7, 1, [0, -30, 30], P('body'), 'chitin'), (9, 1, [0, 0, 60], P('dark'), None), (3, 1, [0, 0, -40], P('dark'), None)]),
    ], bones, cubes)
    if petals:   # flat petal lobe on each walking femur
        for names, i, side in legs:
            root = (2.5 * side, 9, 0.5 if i == 0 else 2.5)
            length = 6 if i == 0 else 7
            cubes.append(box(names[0], (root[0] + side * length / 2, 9.5, root[2]), (length - 1, 0, 5), P('petal'), rim=False,
                             decor={'up': edge(P('light')), 'down': edge(P('light'))}))
    make(name, bones, cubes, seed, bounds=(3, 2.5, 0.8))

    def arm_pose(f, t):
        return {a[2]: {'rotation': [0, 0, f * (1 if a[0].startswith('arm_l') else -1)]} for a in arms} | \
               {a[3]: {'rotation': [0, 0, t * (1 if a[0].startswith('arm_l') else -1)]} for a in arms}
    idle = {'body': {'position': [sin(60, 0.4), 0, 0], 'rotation': [0, 0, sin(60, 2)]},
            'neck': {'rotation': [sin(45, 3), sin(30, 6), 0]}}
    walk = gait(legs, 300, 18, 14)
    walk['body'] = {'rotation': [0, 0, sin(300, 2)]}
    walk['neck'] = {'rotation': [sin(600, 2), 0, 0]}
    strike = {}
    for a in arms:
        s = 1 if a[0].startswith('arm_l') else -1
        strike[a[2]] = {'rotation': {'0.0': [0, 0, 0], '0.08': [0, 0, 45 * s], '0.35': [0, 0, 0]}}
        strike[a[3]] = {'rotation': {'0.0': [0, 0, 0], '0.08': [0, 0, -150 * s], '0.35': [0, 0, 0]}}
    strike['neck'] = {'rotation': {'0.0': [0, 0, 0], '0.08': [15, 0, 0], '0.35': [0, 0, 0]}}
    windup = {'body': {'rotation': [0, 0, sin(900, 6)], 'position': [sin(900, 0.8), 0, 0]},
              'neck': {'rotation': {'0.0': [0, 0, 0], '0.4': [-12, 0, 0]}}}
    windup |= {k: {'rotation': {'0.0': [0, 0, 0], '0.4': v['rotation']}} for k, v in arm_pose(-25, 20).items()}
    hide = {'body': {'position': [0, -2, 0]}, 'neck': {'rotation': [22, 0, 0]}, 'head': {'rotation': [-10, 0, 0]}}
    for names, i, side in legs:
        hide[names[1]] = {'rotation': [0, 0, 18 * side]}
    anim(name, {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': idle},
        'move.walk': {'loop': True, 'animation_length': 1.2, 'bones': walk},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': strike},
        'attack.windup': {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': windup},
        'misc.hide': {'loop': True, 'animation_length': 8.0, 'bones': hide},
    })


# ---- Wasp ----------------------------------------------------------------------------------

def wasp(name, pal, seed, k=1.0, kind='worker'):
    """Wasp ~1.2 blocks at k=1: banded thorax, pinched waist, six-segment banded abdomen hanging
    down with a stinger, big head with compound eyes, elbowed antennae, mandibles, two pairs of
    smoky wings, dangling legs. kind: worker / soldier (armour plates, big mandibles, long sting) /
    nurse (fuzzy, swollen honey abdomen, carries a glowing wax drop)."""
    P = lambda key: f'{name}_{key}'
    mobparts.mobkit.PALETTE.update({P(key): v for key, v in pal.items()})
    S = lambda v: v * k
    I = lambda v: max(1, int(round(v * k)))
    bones = [dict(name='body', pivot=[0, S(12), 0])]
    cubes = [
        box('body', (0, S(12), S(-1)), (I(6), I(6), I(7)), P('dark'), 'chitin',
            decor={'up': stripes_x(P('band'), 99, I(2)), 'east': stripes_x(P('band'), 4, 1), 'west': stripes_x(P('band'), 4, 2)}),
        box('body', (0, S(15.3), S(1.5)), (I(4), 1, I(3)), P('band'), 'chitin'),        # scutellum
        box('body', (0, S(11.5), S(3.5)), (I(2), I(2), I(2)), P('dark'), 'chitin'),    # waist
    ]
    if kind == 'soldier':
        cubes += [box('body', (0, S(15.5), S(-2.5)), (I(5), 1, I(4)), P('plate'), 'metal'),
                  box('body', (S(3.4), S(12), S(-1)), (1, I(4), I(5)), P('plate'), 'metal'),
                  box('body', (S(-3.4), S(12), S(-1)), (1, I(4), I(5)), P('plate'), 'metal')]
    if kind == 'nurse':
        cubes.append(box('body', (0, S(12), S(-1)), (I(6), I(6), I(7)), P('fuzz'), 'fur', inflate=0.6))
    swell = 1.35 if kind == 'nurse' else 1.25 if kind == 'spitter' else 1.0
    bandc = [P('band'), P('dark')]
    segs = []
    for i, (L, w, h, rx) in enumerate([(3, 6, 6, -18), (3, 7, 7, -4), (3, 7, 6, -4), (3, 6, 5, -4), (2, 4, 4, -4), (2, 2, 2, -4)]):
        col = bandc[i % 2] if kind != 'nurse' else (P('band') if i % 2 == 0 else P('fuzz'))
        dec = {'up': edge(P('dark'), 'left'), 'east': edge(P('dark')), 'west': edge(P('dark'), 'left')}
        if kind == 'spitter' and 0 < i < 5:   # glowing resin belly showing through the plates
            dec['down'] = both(stripes_y(P('resin'), 1, 0), edge(P('dark'), 'left'))
            dec['east'] = both(stripes_y(P('resin'), 99, I(h * swell) - 2), stripes_y(P('resin'), 99, I(h * swell) - 1), edge(P('dark')))
            dec['west'] = both(stripes_y(P('resin'), 99, I(h * swell) - 2), stripes_y(P('resin'), 99, I(h * swell) - 1), edge(P('dark'), 'left'))
        segs.append((I(L), I(w * swell), I(h * swell), S(11.5), rx, col, 'fur' if kind == 'nurse' else 'chitin', dec))
    abdomen, end = chain_z('ab', 'body', S(4.5), segs, bones, cubes)
    sting = 5 if kind == 'soldier' else 3
    cubes.append(box(abdomen[-1], (0, S(11.5), end + sting / 2), (1, 1, sting), P('sting'), 'chitin'))
    # head
    bones.append(dict(name='head', parent='body', pivot=[0, S(12.5), S(-4.5)]))
    cubes += [
        box('head', (0, S(12.5), S(-6.5)), (I(6), I(5), I(4)), P('band'), 'chitin',
            decor={'north': dots(P('dark'), [(I(3) - 1, 0), (I(3), 0), (I(3) - 1, 1)])}),
        box('head', (S(3), S(13), S(-6.5)), (I(2), I(4), I(3)), P('eye'), 'facet'),
        box('head', (S(-3), S(13), S(-6.5)), (I(2), I(4), I(3)), P('eye'), 'facet'),
    ]
    mand = (I(2) + (1 if kind == 'soldier' else 0), I(1) + (1 if kind == 'soldier' else 0), I(3) + (2 if kind == 'soldier' else 0))
    for side in (1, -1):
        n = f'mandible_{"l" if side > 0 else "r"}'
        bones.append(dict(name=n, parent='head', pivot=[S(1.5) * side, S(10.5), S(-8.5)], rotation=[0, -20 * side, 0]))
        cubes.append(box(n, (S(1.5) * side, S(10.5), S(-8.5) - mand[2] / 2), mand, P('dark'), 'chitin'))
        chain_x(f'antenna_{"l" if side > 0 else "r"}', 'head', (S(1.2) * side, S(14.5), S(-8)), side,
                [(0, 1, [0, 65, 0], P('dark'), None), (I(4), 1, [0, 0, -50], P('dark'), None), (I(6), 1, [0, 0, 65], P('band'), None)], bones, cubes)
    if kind == 'spitter':   # swollen resin crop under the head and a drip at the mouth
        cubes += [box('head', (0, S(10), S(-6)), (I(4), I(2), I(3)), P('resin'), 'glow', glow=True),
                  box('head', (0, S(9), S(-8.5)), (1, 1, 1), P('resin'), 'glow', glow=True)]
    if kind == 'nurse':   # glowing wax drop held in the mandibles
        cubes.append(box('head', (0, S(9.5), S(-10)), (3, 3, 3), P('wax'), 'glow', glow=True))
    # wings
    wings = {}
    for side in (1, -1):
        s = 'l' if side > 0 else 'r'
        for kind_w, zf, chord, span in (('f', -3, 4, 14), ('b', 0, 3, 10)):
            n = f'wing_{kind_w}{s}'
            wings[n] = side
            bones.append(dict(name=n, parent='body', pivot=[S(2.5) * side, S(15.5), S(zf)], rotation=[0, -22 * side, 6 * side]))
            sp, ch = I(span * (0.8 if kind == 'nurse' else 1)), I(chord)
            ox = S(2.5) if side > 0 else S(-2.5) - sp
            cubes.append(cube(n, [ox, S(15.5), S(zf)], [sp, 0, ch], P('wing') + '@120', rim=False, noise=2,
                              decor={'up': wing_veins(P('vein')), 'down': wing_veins(P('vein'))}))
            cubes.append(cube(n, [ox, S(15.5) - 0.5, S(zf) - 0.5], [sp, 1, 1], P('vein'), mat='chitin'))
    legs = leg_pairs('leg', 'body', [
        ((S(2.5), S(9.5), S(-3)), [(I(4), 1, [0, 25, 40], P('dark'), 'chitin'), (I(5), 1, [0, 0, 55], P('band'), None), (I(4), 1, [0, 0, -25], P('band'), None)]),
        ((S(2.5), S(9.5), S(-1)), [(I(4), 1, [0, 0, 45], P('dark'), 'chitin'), (I(5), 1, [0, 0, 55], P('band'), None), (I(4), 1, [0, 0, -25], P('band'), None)]),
        ((S(2.5), S(9.5), S(1)), [(I(5), 1, [0, -30, 45], P('dark'), 'chitin'), (I(6), 1, [0, 0, 55], P('band'), None), (I(4), 1, [0, 0, -25], P('band'), None)]),
    ], bones, cubes)
    make(name, bones, cubes, seed, bounds=(3 * k, 2.5 * k, 0.8), glow=(kind in ('nurse', 'spitter')))

    def fly(speed, amp):
        out = flap(wings, speed, amp, lambda n: 0 if n.startswith('wing_f') else 60)
        out['body'] = {'position': [0, sin(200, 0.7), 0]}
        out = leg_sway(legs, 130, 6, out)
        for i, b in enumerate(abdomen):
            out[b] = {'rotation': [sin(160, 3, -i * 50), 0, 0]}
        for side in ('l', 'r'):
            out[f'antenna_{side}1'] = {'rotation': [0, 0, sin(120, 8, 0 if side == 'l' else 90)]}
        return out
    strike = {'ab0': {'rotation': {'0.0': [0, 0, 0], '0.12': [-55, 0, 0], '0.4': [0, 0, 0]}},
              'ab1': {'rotation': {'0.0': [0, 0, 0], '0.12': [-20, 0, 0], '0.4': [0, 0, 0]}},
              'mandible_l': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, -25, 0], '0.3': [0, 0, 0]}},
              'mandible_r': {'rotation': {'0.0': [0, 0, 0], '0.1': [0, 25, 0], '0.3': [0, 0, 0]}}}
    anims = {
        'misc.idle': {'loop': True, 'animation_length': 6.0, 'bones': fly(2400, 30)},
        'move.fly': {'loop': True, 'animation_length': 6.0, 'bones': fly(2800, 38) | {'body': {'rotation': [-8, 0, 0]}}},
        'attack.strike': {'loop': False, 'animation_length': 0.4, 'bones': strike},
    }
    if kind == 'soldier':
        windup = flap(wings, 3400, 42)
        windup['ab0'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-70, 0, 0]}}
        windup['ab1'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-25, 0, 0]}}
        windup['ab2'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-15, 0, 0]}}
        windup['body'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-15, 0, 0]}, 'position': [sin(1500, 0.3), 0, 0]}
        anims['attack.windup'] = {'loop': 'hold_on_last_frame', 'animation_length': 1.2, 'bones': windup}
        anims['attack.strike']['bones']['ab0'] = {'rotation': {'0.0': [-70, 0, 0], '0.08': [-95, 0, 0], '0.4': [0, 0, 0]}}
    if kind == 'spitter':   # tell: hovers still, head tips back, the glowing belly pumps for 1 s
        windup = flap(wings, 3000, 34)
        windup['head'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-22, 0, 0]}}
        windup['body'] = {'rotation': {'0.0': [0, 0, 0], '1.0': [-10, 0, 0]}}
        for i, b in enumerate(abdomen[1:5]):
            windup[b] = {'scale': [f'1 + math.max(0, math.sin(query.anim_time * 720 - {i * 40})) * 0.14'] * 3}
        anims['attack.windup'] = {'loop': 'hold_on_last_frame', 'animation_length': 1.0, 'bones': windup}
        anims['attack.strike'] = {'loop': False, 'animation_length': 0.4, 'bones': {
            'head': {'rotation': {'0.0': [-22, 0, 0], '0.08': [18, 0, 0], '0.4': [0, 0, 0]}},
            'body': {'position': {'0.0': [0, 0, 0], '0.08': [0, 0, 1.5], '0.4': [0, 0, 0]}}}}
    if kind == 'nurse':
        anims['attack.tend'] = {'loop': False, 'animation_length': 0.8, 'bones': {
            'head': {'rotation': {'0.0': [0, 0, 0], '0.2': [25, 0, 0], '0.5': [25, 0, 0], '0.8': [0, 0, 0]}},
            'ab1': {'scale': {'0.0': [1, 1, 1], '0.3': [1.12, 1.12, 1.05], '0.8': [1, 1, 1]}}}}
    anim(name, anims)


# ---- Spider --------------------------------------------------------------------------------

def spider_legs(P, pairs, femur, tibia, tarsus, bones, cubes, root_y, thick=2, raise_front=0):
    """Four leg pairs on the cephalothorax. pairs: [(x, z, yaw)], front first. femur/tibia/tarsus: lengths.
    Each leg rises from the body (knee up), then drops to the floor: the classic spider stance."""
    specs = []
    for i, (x, z, yaw) in enumerate(pairs):
        k = 1.15 if i in (0, 3) else 1.0          # front and back pairs are the longest
        up = -40 - (raise_front if i == 0 else 0)
        specs.append(((x, root_y, z), [(int(round(femur * k)), thick, [0, yaw, up], P('body'), 'fur' if thick > 1 else 'chitin'),
                                       (int(round(tibia * k)), 1, [0, 0, 95 + (raise_front * 0.5 if i == 0 else 0)], P('legband'), 'chitin'),
                                       (tarsus, 1, [0, 0, 30], P('dark'), None)]))
    return leg_pairs('leg', 'body', specs, bones, cubes)


def chevrons(color, step=3, start=1):
    """Arrow-head marks pointing forward (y=0 is the front edge on an 'up' face)."""
    def d(put, w, h):
        mid = (w - 1) / 2
        for y0 in range(start, h, step):
            for x in range(w):
                y = y0 + int(abs(x - mid) * 0.6)
                if y < h:
                    put(x, y, color)
    return d

#!/usr/bin/env python3
"""Model + texture builder for Exofall mobs (adapted from BROMAX's Pestilence tools/gen_assets.py).

Each model is a list of cubes (bone, origin, size, colour, optional face decor). The script packs
every cube face into its own UV rect, paints the rect (base colour, per-face light, noise, darker
rim, then decor), and writes GeckoLib geo.json + texture. Edit PALETTE or the cube lists and rerun:

    python3 tools/mobs/gen_mobs.py

Coordinates are Blockbench/Bedrock pixels: feet at y=0, model faces north (-Z), 16px = 1 block.
"""
import json
import os
import random
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'exofall')

PALETTE = {}  # filled in by gen_mobs.py


def rgba(name_or_hex, a=255):
    if '@' in name_or_hex:
        name_or_hex, a = name_or_hex.split('@')[0], int(name_or_hex.split('@')[1])
    s = PALETTE.get(name_or_hex, name_or_hex).lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def scale(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3]) + (c[3],)


def _handmade():
    path = os.path.join(HERE, 'handmade.txt')
    if not os.path.exists(path):
        return set()
    return {os.path.normpath(os.path.join(ASSETS, l.strip())) for l in open(path) if l.strip() and not l.startswith('#')}


def write_png(path, img, force=False):
    """Write an RGBA PNG. Files listed in tools/handmade.txt are hand-painted and left alone unless forced."""
    if not force and os.path.normpath(path) in _handmade() and os.path.exists(path):
        return
    h, w = len(img), len(img[0])
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in img)

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# ---------------------------------------------------------------------------------------------
# Model building
# ---------------------------------------------------------------------------------------------

FACE_LIGHT = {'up': 1.15, 'down': 0.7, 'north': 1.0, 'south': 0.92, 'east': 0.85, 'west': 0.85}


def cube(bone, origin, size, color, decor=None, inflate=0.0, noise=5, rim=True, mat=None):
    return dict(bone=bone, origin=origin, size=size, color=color, decor=decor or {}, inflate=inflate,
                noise=noise, rim=rim, mat=mat)


# ---- materials: how a surface is shaded (remastered models) ----------------------------------
FACE_T = {'up': 0.22, 'down': -0.45, 'north': 0.0, 'south': -0.12, 'east': -0.18, 'west': -0.18}


def tone(c, t):
    """Pixel-art shading: darker shades drift cool and richer, lighter ones warm and paler."""
    import colorsys
    h, l, s_ = colorsys.rgb_to_hls(c[0] / 255, c[1] / 255, c[2] / 255)
    if t < 0:
        l *= 1 + t * 0.75
        h = h + (0.62 - h) * min(0.25, -t * 0.12) if s_ > 0.05 else h
        s_ = min(1, s_ * (1 - t * 0.25))
    else:
        l = l + (1 - l) * t * 0.55
        h = h + (0.11 - h) * min(0.25, t * 0.12) if s_ > 0.05 else h
        s_ *= 1 - t * 0.15
    r, g, b = colorsys.hls_to_rgb(h % 1, max(0, min(1, l)), max(0, min(1, s_)))
    return (int(r * 255), int(g * 255), int(b * 255), c[3])


def material_t(mat, xx, yy, w, h, face, rng, cell):
    side = face not in ('up', 'down')
    t = FACE_T[face] + ((0.12 - 0.26 * yy / max(1, h - 1)) if side and h > 2 else 0)
    if mat == 'cloth':
        t += (-0.15 if (xx + cell(xx, 0) % 2) % 4 == 0 else 0) + rng.uniform(-0.04, 0.04)
        if side and yy == h - 1:
            t -= 0.12
    elif mat == 'leather':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.035 + rng.uniform(-0.03, 0.03)
        if side and xx + yy == 1:
            t += 0.3
    elif mat == 'fur':
        t += (cell(xx, yy // 2) % 5 - 2) * 0.06 + (0.08 if yy == 0 and side else 0)
    elif mat == 'metal':
        band = max(1, int(h * 0.3))
        t += 0.4 if yy == band else (-0.18 if yy > h * 0.65 else 0.05) + rng.uniform(-0.02, 0.02)
    elif mat == 'wood':
        t += (-0.14 if xx % 3 == 0 else 0) + (cell(xx, yy // 3) % 3 - 1) * 0.04
    elif mat == 'bone':
        t += rng.uniform(-0.05, 0.05) + (-0.45 if cell(xx, yy) % 23 == 0 else 0)
    elif mat == 'flesh':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.06 + rng.uniform(-0.04, 0.04)
    elif mat == 'stone':
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.07
    elif mat == 'glow':
        t = 0.1
    elif mat == 'chitin':
        # glossy shell: bright sheen line near the top of side faces, faint mottling
        t += (cell(xx // 2, yy // 2) % 5 - 2) * 0.03
        if side and h >= 3 and yy == 1:
            t += 0.28
        if face == 'up' and (xx + yy) % 5 == 0:
            t += 0.12
    elif mat == 'facet':
        # compound eye: checker of lenses with a highlight corner
        t = (0.18 if (xx + yy) % 2 == 0 else -0.12) + (0.3 if xx <= 1 and yy <= 1 else 0)
    if mat != 'glow' and w >= 3 and h >= 3 and (xx == w - 1 or yy == h - 1):
        t -= 0.1
    return t


def face_dims(size):
    sx, sy, sz = size
    return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy),
            'up': (sx, sz), 'down': (sx, sz)}


def pack(rects, width):
    """Shelf-pack (key, w, h) rects; returns {key: (u, v)} and used height."""
    placed, x, y, shelf = {}, 0, 0, 0
    for key, w, h in sorted(rects, key=lambda r: (-r[2], -r[1])):
        if x + w > width:
            x, y, shelf = 0, y + shelf, 0
        placed[key] = (x, y)
        x += w
        shelf = max(shelf, h)
    return placed, y + shelf


def build(name, bones, cubes, seed, out_geo, out_tex, identifier, bounds=(2, 3, 1.25), mats=None):
    """mats: {colour key: material} for cubes that don't name their own material (remastered models)."""
    rects = []
    for i, c in enumerate(cubes):
        for face, (w, h) in face_dims(c['size']).items():
            rects.append(((i, face), int(w), int(h)))
    width = 32
    while True:
        placed, used = pack(rects, width)
        height = 16
        while height < used:
            height *= 2
        if height <= width * 2:
            break
        width *= 2
    img = [[(0, 0, 0, 0)] * width for _ in range(height)]
    rng = random.Random(seed)

    for i, c in enumerate(cubes):
        base = rgba(c['color'])
        mat = c.get('mat') or (mats or {}).get(c['color'])
        for face, (w, h) in face_dims(c['size']).items():
            u, v = placed[(i, face)]
            w, h = int(w), int(h)
            salt = zlib.crc32(f'{name}{i}{face}'.encode())
            cell = lambda a, b, salt=salt: zlib.crc32(f'{salt},{a},{b}'.encode()) % 997
            for yy in range(h):
                for xx in range(w):
                    if mat:
                        col = tone(base, material_t(mat, xx, yy, w, h, face, rng, cell))
                    else:
                        col = scale(base, FACE_LIGHT[face] * (1 + rng.uniform(-c['noise'], c['noise']) / 100))
                        if c['rim'] and w >= 3 and h >= 3 and (xx in (0, w - 1) or yy in (0, h - 1)):
                            col = scale(col, 0.85)
                    img[v + yy][u + xx] = col
            decor = c['decor'].get(face) or c['decor'].get('all')
            if decor:
                def put(x, y, color, face=face, u=u, v=v, w=w, h=h, mat=mat):
                    if 0 <= x < w and 0 <= y < h:
                        img[v + y][u + x] = tone(rgba(color), FACE_T[face] * 0.6) if mat else scale(rgba(color), FACE_LIGHT[face])
                decor(put, w, h)

    geo_bones = []
    for b in bones:
        entry = {'name': b['name'], 'pivot': b['pivot']}
        if b.get('parent'):
            entry['parent'] = b['parent']
        if b.get('rotation'):
            entry['rotation'] = b['rotation']
        cs = []
        for i, c in enumerate(cubes):
            if c['bone'] != b['name']:
                continue
            uv = {}
            for face, (w, h) in face_dims(c['size']).items():
                u, v = placed[(i, face)]
                uv[face] = {'uv': [u, v], 'uv_size': [int(w), int(h)]}
            cj = {'origin': c['origin'], 'size': c['size'], 'uv': uv}
            if c['inflate']:
                cj['inflate'] = c['inflate']
            cs.append(cj)
        if cs:
            entry['cubes'] = cs
        geo_bones.append(entry)

    write_json(out_geo, {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': identifier, 'texture_width': width, 'texture_height': height,
                'visible_bounds_width': bounds[0], 'visible_bounds_height': bounds[1],
                'visible_bounds_offset': [0, bounds[2], 0],
            },
            'bones': geo_bones,
        }],
    })
    write_png(out_tex, img)
    print(f'{name}: {len(cubes)} cubes, texture {width}x{height}')


# ---------------------------------------------------------------------------------------------
# Decor helpers (face-local pixel coords, y=0 is the top of the face)
# ---------------------------------------------------------------------------------------------

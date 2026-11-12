"""Tiny 3D item-model kit for Exofall's weapons.

A weapon is authored upright in a local frame: u = along the weapon (up), v = across (right), w = depth (towards the
viewer), with the grip at (v, u, w) = (0, 0, 0). Every box is then turned -45 degrees about Z around the vanilla
sprite grip, so the model sits on the same diagonal as a vanilla sword sprite and the vanilla handheld transforms
hold it correctly. A box can lean further (tilt = 22.5/45/67.5/90 degrees counter-clockwise, the only angles 1.21.1
element rotations allow on top of -45).

Each face gets its own patch of one texture atlas, painted per texel by a material function that knows the texel's
3D position, so patterns (bands, honeycomb, veins) line up across faces. `render` is a small software rasteriser
for previews: it uses the same element-rotation and display-transform maths as the game.
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

GRIP = np.array([4.0, 4.0, 8.0])  # where the vanilla sword sprite is held, in model units
MAIN_ANGLE = -45.0
ALLOWED = {-45.0, -22.5, 0.0, 22.5, 45.0}


def gradramp(n, *stops):
    """n colours (RGB tuples) interpolated through hex stops, dark -> light."""
    cs = [tuple(int(s[i:i + 2], 16) for i in (1, 3, 5)) for s in stops]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(cs) - 1)
        k = min(int(t), len(cs) - 2)
        f = t - k
        out.append(tuple(round(cs[k][j] + (cs[k + 1][j] - cs[k][j]) * f) for j in range(3)))
    return out


def rz(deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


class Box:
    def __init__(self, v0, v1, u0, u1, w0, w1, mat, tilt=0.0, pivot=None, glow=False, tag=None, faces=None):
        self.lo = np.array([min(v0, v1), min(u0, u1), min(w0, w1)], float)  # local x=v, y=u, z=w
        self.hi = np.array([max(v0, v1), max(u0, u1), max(w0, w1)], float)
        self.mat, self.tilt, self.glow, self.tag = mat, tilt, glow, tag
        self.pivot = np.array(pivot if pivot is not None else (0.0, 0.0), float)  # (v, u) the tilt turns about
        self.faces = faces  # None = all six; else a set of names

    def local_point(self, p):
        """Box-space point (before tilt) -> local weapon frame (after tilt)."""
        q = np.array([self.pivot[0], self.pivot[1], 0.0])
        return q + rz(self.tilt) @ (np.asarray(p, float) - q)


FACE_DEFS = {
    # name: (normal axis, sign, (col axis, col sign), (row axis, row sign))   row sign -1 = row grows as coord falls
    "north": (2, -1, (0, -1), (1, -1)),
    "south": (2, 1, (0, 1), (1, -1)),
    "west": (0, -1, (2, 1), (1, -1)),
    "east": (0, 1, (2, -1), (1, -1)),
    "up": (1, 1, (0, 1), (2, 1)),
    "down": (1, -1, (0, 1), (2, -1)),
}


class Model:
    def __init__(self, name, density=2, render_type=None):
        self.name, self.density, self.render_type = name, density, render_type
        self.boxes = []

    def add(self, *a, **k):
        b = Box(*a, **k)
        self.boxes.append(b)
        return b

    # ---- geometry -------------------------------------------------------------------------------------------
    def element_geometry(self, b):
        """from/to/origin/angle for a box, plus the 4x4-free mapping box point -> model point (pre element rotation)."""
        q = np.array([b.pivot[0], b.pivot[1], 0.0])
        # Outer turn: about the grip by -45. Inner: about q by tilt. Combined = turn by (-45 + tilt) about
        # O = outer(q), applied to the box shifted by (O - q) (derivation in the module docstring's spirit).
        o_local = q
        o_model = GRIP + rz(MAIN_ANGLE) @ o_local
        shift = o_model - o_local  # box point p (local) -> element point p + shift
        angle = MAIN_ANGLE + b.tilt
        if angle not in ALLOWED:
            raise ValueError(f"{self.name}: tilt {b.tilt} not allowed")
        frm, to = b.lo + shift, b.hi + shift
        if frm.min() < -16 or to.max() > 32:
            raise ValueError(f"{self.name}: box out of range {frm} {to} ({b.tag})")
        return frm, to, o_model, angle

    # ---- texture atlas ----------------------------------------------------------------------------------------
    def face_list(self, b):
        return [f for f in FACE_DEFS if b.faces is None or f in b.faces]

    def face_px(self, b, f):
        n_ax, _, (ca, _), (ra, _) = FACE_DEFS[f]
        size = b.hi - b.lo
        return max(1, round(size[ca] * self.density)), max(1, round(size[ra] * self.density))

    def pack(self):
        items = []
        for bi, b in enumerate(self.boxes):
            for f in self.face_list(b):
                w, h = self.face_px(b, f)
                items.append((bi, f, w, h))
        order = sorted(items, key=lambda it: -it[3])
        for side in (32, 64, 128, 256, 512):
            x = y = shelf = 0
            pos = {}
            ok = True
            for bi, f, w, h in order:
                if w > side:
                    ok = False
                    break
                if x + w > side:
                    x, y, shelf = 0, y + shelf, 0
                if y + h > side:
                    ok = False
                    break
                pos[(bi, f)] = (x, y, w, h)
                x += w
                shelf = max(shelf, h)
            if ok:
                return side, pos
        raise ValueError("atlas too big")

    def texel_point(self, b, f, cx, cy, w, h):
        """Local-frame 3D point at the centre of texel (cx, cy) of face f (after tilt)."""
        n_ax, n_sign, (ca, cs), (ra, rs) = FACE_DEFS[f]
        p = np.zeros(3)
        p[n_ax] = b.hi[n_ax] if n_sign > 0 else b.lo[n_ax]
        tc, tr = (cx + 0.5) / w, (cy + 0.5) / h
        p[ca] = b.lo[ca] + (b.hi[ca] - b.lo[ca]) * (tc if cs > 0 else 1 - tc)
        p[ra] = b.lo[ra] + (b.hi[ra] - b.lo[ra]) * (tr if rs > 0 else 1 - tr)
        return b.local_point(p), p

    def paint(self, materials, seed=1):
        side, pos = self.pack()
        img = np.zeros((side, side, 4), np.uint8)
        rng = random.Random(seed)
        for (bi, f), (x, y, w, h) in pos.items():
            b = self.boxes[bi]
            mat = materials[b.mat]
            n_ax, n_sign = FACE_DEFS[f][0], FACE_DEFS[f][1]
            normal = np.zeros(3)
            normal[n_ax] = n_sign
            normal = rz(b.tilt) @ normal
            for cy in range(h):
                for cx in range(w):
                    p, bp = self.texel_point(b, f, cx, cy, w, h)
                    edge = min(cx, cy, w - 1 - cx, h - 1 - cy)
                    ctx = dict(p=p, bp=bp, n=normal, face=f, cx=cx, cy=cy, w=w, h=h, edge=edge, box=b, rng=rng,
                               top=(cy == 0), bottom=(cy == h - 1), left=(cx == 0), right=(cx == w - 1))
                    img[y + cy, x + cx] = mat(ctx)
        self.atlas_side, self.atlas_pos, self.atlas = side, pos, img
        return img

    # ---- export -----------------------------------------------------------------------------------------------
    def to_json(self, texture, display):
        k = 16.0 / self.atlas_side
        elements = []
        for bi, b in enumerate(self.boxes):
            frm, to, origin, angle = self.element_geometry(b)
            faces = {}
            for f in self.face_list(b):
                x, y, w, h = self.atlas_pos[(bi, f)]
                faces[f] = {"uv": [round(x * k, 4), round(y * k, 4), round((x + w) * k, 4), round((y + h) * k, 4)],
                            "texture": "#t"}
            el = {"from": [round(c, 4) for c in frm], "to": [round(c, 4) for c in to],
                  "rotation": {"angle": angle, "axis": "z", "origin": [round(c, 4) for c in origin]},
                  "faces": faces}
            if b.glow:
                el["neoforge_data"] = {"block_light": 15, "sky_light": 15}
            if b.tag:
                el["name"] = b.tag
            elements.append(el)
        out = {"parent": "minecraft:item/handheld", "gui_light": "front",
               "textures": {"t": texture, "particle": texture}, "elements": elements, "display": display}
        if self.render_type:
            out["render_type"] = self.render_type
        return out

    def model_points(self):
        """All box corners in model space (after element rotation)."""
        pts = []
        for b in self.boxes:
            frm, to, origin, angle = self.element_geometry(b)
            R = rz(angle)
            for i in range(8):
                c = np.array([(to if i & 1 else frm)[0], (to if i & 2 else frm)[1], (to if i & 4 else frm)[2]])
                pts.append(origin + R @ (c - origin))
        return np.array(pts)

    def fit_display(self, box=15.0, rot=(0, 0, 0)):
        """translation/scale so the model's front view fits a box x box square centred in the slot."""
        pts = self.model_points()
        R = euler_xyz(rot)
        q = (R @ (pts / 16.0 - 0.5).T).T
        lo, hi = q.min(0), q.max(0)
        s = box / 16.0 / max(hi[0] - lo[0], hi[1] - lo[1])
        c = (lo + hi) / 2 * s
        return {"rotation": list(rot), "translation": [round(-c[0] * 16, 3), round(-c[1] * 16, 3), 0],
                "scale": [round(s, 4)] * 3}


def euler_xyz(rot):
    """Quaternionf.rotationXYZ(x, y, z) as a matrix: Rx * Ry * Rz."""
    x, y, z = (math.radians(a) for a in rot)
    Rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    Ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    return Rx @ Ry @ rz(math.degrees(z))


# ---- preview renderer ---------------------------------------------------------------------------------------------
def model_quads(model):
    """(corners in model units [4x3], texel uv corners [4x2] in atlas px, rgba atlas) for every face."""
    quads = []
    for bi, b in enumerate(model.boxes):
        frm, to, origin, angle = model.element_geometry(b)
        R = rz(angle)
        for f in model.face_list(b):
            x, y, w, h = model.atlas_pos[(bi, f)]
            n_ax, n_sign, (ca, cs), (ra, rs) = FACE_DEFS[f]
            corners, uvs = [], []
            for tc, tr in ((0, 0), (1, 0), (1, 1), (0, 1)):
                p = np.zeros(3)
                p[n_ax] = to[n_ax] if n_sign > 0 else frm[n_ax]
                p[ca] = frm[ca] + (to[ca] - frm[ca]) * (tc if cs > 0 else 1 - tc)
                p[ra] = frm[ra] + (to[ra] - frm[ra]) * (tr if rs > 0 else 1 - tr)
                corners.append(origin + R @ (p - origin))
                uvs.append((x + tc * w, y + tr * h))
            quads.append((np.array(corners), np.array(uvs, float), b.glow))
    return quads


def sprite_quads(png_path):
    """A vanilla generated item (front + back face only) as quads, for size comparison."""
    im = np.array(Image.open(png_path).convert("RGBA"))
    h, w = im.shape[:2]
    quads = []
    for z in (8.5, 7.5):
        c = np.array([[0, 16, z], [16, 16, z], [16, 0, z], [0, 0, z]], float)
        if z < 8:
            c = c[[1, 0, 3, 2]]
        quads.append((c, np.array([[0, 0], [w, 0], [w, h], [0, h]], float) if z > 8 else
                      np.array([[w, 0], [0, 0], [0, h], [w, h]], float), False))
    return quads, im


def render(quads, atlas, display=None, view=(0, 0, 0), size=256, extent=1.0, light=(-0.4, 0.8, 0.9), bg=(40, 40, 46), flat=False):
    """Orthographic render. Points go model/16 - 0.5 -> display transform -> view rotation; `extent` = half-width
    of the visible square in blocks."""
    img = np.zeros((size, size, 3), float)
    img[:] = bg
    zbuf = np.full((size, size), -1e9)
    T = np.zeros(3)
    R = np.eye(3)
    S = 1.0
    if display:
        T = np.array(display.get("translation", [0, 0, 0]), float) / 16
        R = euler_xyz(display.get("rotation", [0, 0, 0]))
        S = display.get("scale", [1, 1, 1])[0]
    V = euler_xyz(view)
    L = np.array(light, float)
    L /= np.linalg.norm(L)
    trans = []
    for corners, uvs, glow in quads:
        p = corners / 16.0 - 0.5
        p = (V @ (T + (R @ (p * S).T).T).T).T
        n = np.cross(p[1] - p[0], p[3] - p[0])
        if np.linalg.norm(n) < 1e-12:
            continue
        n /= np.linalg.norm(n)
        bright = 1.0 if (glow or flat) else 0.55 + 0.45 * max(0.0, float(n @ L))
        scr = (p[:, :2] / extent * 0.5 + 0.5) * size
        scr[:, 1] = size - scr[:, 1]
        for tri in ((0, 1, 2), (0, 2, 3)):
            raster(img, zbuf, scr[list(tri)], p[list(tri), 2], uvs[list(tri)], atlas, bright, trans)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def raster(img, zbuf, s, z, uv, atlas, bright, trans):
    size = img.shape[0]
    x0, y0 = np.floor(s.min(0)).astype(int)
    x1, y1 = np.ceil(s.max(0)).astype(int)
    x0, y0, x1, y1 = max(x0, 0), max(y0, 0), min(x1, size - 1), min(y1, size - 1)
    if x0 > x1 or y0 > y1:
        return
    (ax, ay), (bx, by), (cx, cy) = s
    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
    if abs(den) < 1e-9:
        return
    ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1] + 0.5
    l1 = ((by - cy) * (xs - cx) + (cx - bx) * (ys - cy)) / den
    l2 = ((cy - ay) * (xs - cx) + (ax - cx) * (ys - cy)) / den
    l3 = 1 - l1 - l2
    inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
    if not inside.any():
        return
    zz = l1 * z[0] + l2 * z[1] + l3 * z[2]
    tu = l1 * uv[0, 0] + l2 * uv[1, 0] + l3 * uv[2, 0]
    tv = l1 * uv[0, 1] + l2 * uv[1, 1] + l3 * uv[2, 1]
    ah, aw = atlas.shape[:2]
    tui = np.clip(tu.astype(int), 0, aw - 1)
    tvi = np.clip(tv.astype(int), 0, ah - 1)
    tex = atlas[tvi, tui].astype(float)
    alpha = tex[..., 3] / 255.0
    sub_z = zbuf[y0:y1 + 1, x0:x1 + 1]
    sub = img[y0:y1 + 1, x0:x1 + 1]
    m = inside & (alpha > 0.02) & (zz > sub_z)
    a = alpha[..., None]
    col = tex[..., :3] * bright
    sub[m] = (col * a + sub * (1 - a))[m]
    sub_z[m & (alpha > 0.6)] = zz[m & (alpha > 0.6)]


def write(model, materials, out_model_dir, out_tex_dir, display, seed=1):
    model.paint(materials, seed)
    os.makedirs(out_model_dir, exist_ok=True)
    os.makedirs(out_tex_dir, exist_ok=True)
    Image.fromarray(model.atlas).save(os.path.join(out_tex_dir, model.name + ".png"))
    # <name>_3d.json = the 3D model (held in hand); <name>.json shows the flat 32x sprite in the inventory, on the
    # ground and in item frames (NeoForge separate_transforms), like the vanilla trident.
    with open(os.path.join(out_model_dir, model.name + "_3d.json"), "w") as fh:
        json.dump(model.to_json("exofall:item/weapon/" + model.name, display), fh, indent=1)
    flat = {"parent": "minecraft:item/generated", "textures": {"layer0": "exofall:item/weapon_icon/" + model.name}}
    wrapper = {"loader": "neoforge:separate_transforms", "gui_light": "front",
               "base": {"parent": "exofall:item/" + model.name + "_3d"},
               "perspectives": {"gui": flat, "ground": flat, "fixed": flat}}
    with open(os.path.join(out_model_dir, model.name + ".json"), "w") as fh:
        json.dump(wrapper, fh, indent=1)

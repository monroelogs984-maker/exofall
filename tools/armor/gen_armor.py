#!/usr/bin/env python3
"""Exofall armour textures: worn layers (64x32 layer_1 + layer_2) and 16x16 item icons for the four meteor sets.

Shape and form come from vanilla Netherite (same silhouette, same per-pixel shading): each pixel's Netherite
brightness picks a step on the set's own material ramp, then the set paints its detail on top in face space (every
pixel knows which box face it is on and where on that face, from the vanilla box-UV layout).

    python3 tools/armor/gen_armor.py [preview_dir]
"""
import math, os, sys, zipfile, io
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAR = os.path.join(ROOT, ".gradle/repositories/ng_dummy_ng/net/minecraft/client/1.21.1/client-1.21.1-client-extra.jar")
TEX = os.path.join(ROOT, "src/main/resources/assets/exofall/textures")
jar = zipfile.ZipFile(JAR)

def vimg(path):
    return Image.open(io.BytesIO(jar.read("assets/minecraft/textures/" + path))).convert("RGBA")

def hx(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

def gradramp(n, *stops):
    st = [hx(s) for s in stops]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(st) - 1)
        k = min(int(t), len(st) - 2)
        f = t - k
        out.append(tuple(round(st[k][j] + (st[k + 1][j] - st[k][j]) * f) for j in range(3)))
    return out

def hsh(*xs):
    v = 0x9E3779B9
    for x in xs:
        v = ((v ^ (int(x) & 0xFFFFFFFF)) * 0x01000193) & 0xFFFFFFFF
        v ^= v >> 15
    return (v & 0xFFFF) / 65536.0

# ---- box-UV face lookup for the armour layers ---------------------------------------------------------------
# (name, u, v, w, h, d) as in the vanilla humanoid model
BOXES = [("head", 0, 0, 8, 8, 8), ("body", 16, 16, 8, 12, 4), ("arm", 40, 16, 4, 12, 4), ("leg", 0, 16, 4, 12, 4)]

def face_map():
    m = {}
    for name, u, v, w, h, d in BOXES:
        rects = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
                 "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for f, (x0, y0, fw, fh) in rects.items():
            for y in range(y0, y0 + fh):
                for x in range(x0, x0 + fw):
                    m[(x, y)] = (name, f, x - x0, y - y0, fw, fh)
    return m

FACES = face_map()

# ---- sets ---------------------------------------------------------------------------------------------------
class Ctx:
    """What a painter knows about one pixel."""
    def __init__(self, x, y, lum, edge, face, icon, layer):
        self.x, self.y, self.lum, self.edge, self.icon, self.layer = x, y, lum, edge, icon, layer
        self.box, self.face, self.fx, self.fy, self.fw, self.fh = face if face else (None, None, x, y, 16, 16)

def mireweave(c, R):
    """Swamp stealth cloth: dark bog-brown reed weave (faint, so the armour's form still reads), reed-green binding on
    the rims, a diagonal strap and belt, frayed hems, a dull amber clasp where the strap crosses the chest."""
    n = len(R["base"])
    bx, by = c.fx // 2, c.fy // 2
    horiz = (bx + by) % 2 == 0
    t = (c.fx % 2) if not horiz else (c.fy % 2)
    idx = 0.6 + c.lum * (n - 2) + (0.5 if t == 0 else -0.3) + (hsh(c.x, c.y, 3) - 0.5) * 0.4
    bind = R["bind"]
    if c.edge:
        return bind[max(0, min(len(bind) - 1, int(1 + c.lum * 4)))]
    if c.box == "body" and c.layer == 1 and c.face in ("front", "back") and abs(c.fx - (c.fy * 0.6 + 1)) < 0.6 and c.fy < 10:
        if c.face == "front" and 3 <= c.fx <= 4 and 3 <= c.fy <= 4:
            return R["clasp"][3 if (c.fx, c.fy) == (3, 3) else 1]
        return bind[2 if c.fy % 2 else 1]  # strap
    if c.box == "body" and c.layer == 2 and c.face in ("front", "back", "left", "right") and c.fy in (0, 1):
        return bind[1 + c.fy]  # belt
    if c.icon and c.lum > 0.15 and abs(c.x - (c.y * 0.6 + 3)) < 0.6 and 3 < c.y < 11 and R.get("strap_icon", True):
        pass
    if c.box in ("body", "leg") and c.face in ("front", "back", "left", "right") and c.fy == c.fh - 1 and hsh(c.x, 9) < 0.45:
        return R["base"][1]
    return R["base"][max(0, min(n - 1, int(round(idx))))]

def honey(c, R):
    """Crystallized honey: amber glass in hex-ish cells, each cell bright at its upper-left and deep at the lower-right,
    pale wax trim at the edges, a big royal-amber crystal on the chest and the helmet brow."""
    n = len(R["base"])
    if c.edge:
        return R["wax"][max(0, min(len(R["wax"]) - 1, int(c.lum * 5)))]
    # offset 3x3 cells with 1-px walls
    row = c.fy // 3
    cx = (c.fx + (1 if row % 2 else 0)) % 3
    cy = c.fy % 3
    idx = 0.5 + c.lum * (n - 2.5)
    if cx == 2 or cy == 2:
        idx -= 1.0
    elif (cx, cy) == (0, 0):
        idx += 1.0
    gem = (c.box == "body" and c.face == "front" and c.layer == 1 and 2 <= c.fx <= 5 and 2 <= c.fy <= 5
           and abs(c.fx - 3.5) + abs(c.fy - 3.5) <= 2.5) or \
          (c.box == "head" and c.face == "front" and 3 <= c.fx <= 4 and 0 <= c.fy <= 2)
    if gem:
        g = R["gem"]
        k = len(g) - 1 - (abs(c.fx - 3.5) + abs(c.fy - 3.5 if c.box == "body" else c.fy - 1.5)) * 1.2
        if (c.box == "body" and (c.fx, c.fy) == (3, 3)) or (c.box == "head" and (c.fx, c.fy) == (3, 1)):
            return (255, 250, 220)
        return g[max(0, min(len(g) - 1, int(round(k))))]
    return R["base"][max(0, min(n - 1, int(round(idx))))]

def carapace(c, R):
    """Reinforced Carapace: mantis-green chitin, big overlapping plates (a lit top row and a thin dark seam), a glossy
    streak down the front of each piece, dark-teal rims, and a raised ridge down the helmet's crown."""
    n = len(R["base"])
    if c.edge:
        return R["edge"][max(0, min(len(R["edge"]) - 1, int(1 + c.lum * 3)))]
    plate = {"head": 4, "body": 4, "arm": 3, "leg": 4}.get(c.box, 4)
    p = c.fy % plate
    idx = 0.8 + c.lum * (n - 2.5) + (0.8 if p == 0 else 0)
    if p == plate - 1 and c.face in ("front", "back", "left", "right"):
        idx -= 1.6
    if c.face == "front" and c.fw >= 4 and c.fx == c.fw // 2 - 1 and p < plate - 1:
        idx += 1.2
    if c.box == "head" and c.face in ("top",) and c.fx in (3, 4):
        idx = n - 2 - (c.fx - 3)  # crown ridge
    if c.icon:
        idx = 0.8 + c.lum * (n - 2.5) + (0.8 if c.y % 4 == 0 else -1.0 if c.y % 4 == 3 else 0)
    return R["base"][max(0, min(n - 1, int(round(idx))))]

def ancientstone(c, R):
    """Ancientstone: carved sandstone blocks (4x3 courses, offset, mortar lines, worn chips), a gold rim on every edge,
    a turquoise inlay band, and a scarab on the chest."""
    n = len(R["base"])
    if c.edge:
        return R["gold"][max(0, min(len(R["gold"]) - 1, int(c.lum * 6)))]
    sc = None
    if c.box == "body" and c.face == "front" and c.layer == 1:
        # scarab: 4 wide x 5 tall, turquoise shell with a gold midline
        sx, sy = c.fx - 2, c.fy - 2
        shape = [".TT.", "TGGT", "TGGT", "TGGT", ".TT."]
        if 0 <= sx < 4 and 0 <= sy < 5 and shape[sy][sx] != ".":
            sc = shape[sy][sx]
    if sc == "T":
        return R["turq"][3 if c.fx == 2 or c.fy == 3 else 2]
    if sc == "G":
        return R["gold"][4]
    if c.face in ("front", "back", "left", "right") and c.box != "head" and c.fy == 1:
        return R["turq"][2 if (c.fx % 2) else 1]  # inlay band under the top trim
    if c.box == "head" and c.face == "front" and c.fy == 2:
        return R["turq"][2 if c.fx % 2 else 1]
    course = c.fy // 3
    bx = (c.fx + (2 if course % 2 else 0)) % 4
    by = c.fy % 3
    idx = 0.8 + c.lum * (n - 3) + (hsh(c.x // 4, course, 5) - 0.5) * 1.0
    if by == 2 or bx == 3:
        idx = 1.5 + c.lum * 1.5  # mortar
    elif by == 0:
        idx += 1.0
    if hsh(c.x, c.y, 11) < 0.03:
        idx -= 2  # chip
    return R["base"][max(0, min(n - 1, int(round(idx))))]

SETS = {
    "mireweave": (mireweave, {
        "base": gradramp(10, "#100e0a", "#1c1a12", "#2a2818", "#3a3820", "#4a4828", "#5c5a32", "#6e6c3e"),
        "bind": gradramp(6, "#2a3414", "#44521e", "#62722a", "#82903a", "#a2ac52"),
        "clasp": gradramp(4, "#5a3a0c", "#8e5e16", "#c08a28", "#e8b848")}),
    "crystallized_honey": (honey, {
        "base": gradramp(10, "#3a1402", "#5e2604", "#843c06", "#aa5808", "#cc760e", "#e8981c", "#f8bc40", "#ffdc80"),
        "wax": gradramp(6, "#8a5a1a", "#b8862e", "#dcb04a", "#f0d070", "#fae8a8"),
        "gem": gradramp(6, "#7a2a02", "#b84c06", "#ee7a12", "#ffae30", "#ffd888")}),
    "reinforced_carapace": (carapace, {
        "base": gradramp(10, "#0a1e10", "#123018", "#1c4a20", "#28662a", "#368434", "#4aa040", "#6cbc54", "#a0dc80"),
        "edge": gradramp(5, "#0a1a1c", "#123034", "#1c4a4c", "#2a6866"),
        "lens": gradramp(5, "#0a2a30", "#145a5a", "#20908a", "#50d0c0", "#c0fff0")}),
    "ancientstone": (ancientstone, {
        "base": gradramp(10, "#4a3820", "#6a5232", "#8a6e46", "#a88a5c", "#c2a474", "#d6bc8c", "#e6d2a8", "#f4e6c8"),
        "gold": gradramp(7, "#4a2c08", "#7a4c10", "#b07c1c", "#dcaa34", "#f4d060", "#fff0b0"),
        "turq": gradramp(5, "#0a3a3c", "#14706a", "#28a89a", "#60dcc8", "#b0fff0")}),
}

def _icon_detail():
    d = {}
    M, H, C, A = (SETS[k][1] for k in ("mireweave", "crystallized_honey", "reinforced_carapace", "ancientstone"))
    # chest emblems, centred on the chestplate icon's breastbone (x 7-8)
    d[("mireweave", "chestplate")] = [(7, 6, M["clasp"][3]), (8, 6, M["clasp"][2]), (7, 7, M["clasp"][1]), (8, 7, M["clasp"][1])] + \
        [(x, y, M["bind"][2]) for x, y in ((4, 3), (5, 4), (6, 5), (9, 8), (10, 9))]
    d[("crystallized_honey", "chestplate")] = [(7, 5, H["gem"][4]), (8, 5, H["gem"][3]), (7, 6, (255, 250, 220)), (8, 6, H["gem"][4]),
        (7, 7, H["gem"][3]), (8, 7, H["gem"][2]), (7, 8, H["gem"][1]), (8, 8, H["gem"][1])]
    d[("crystallized_honey", "helmet")] = [(7, 4, H["gem"][4]), (8, 4, H["gem"][3])]
    d[("ancientstone", "chestplate")] = [(7, 5, A["turq"][3]), (8, 5, A["turq"][2]), (7, 6, A["gold"][4]), (8, 6, A["gold"][3]),
        (7, 7, A["turq"][2]), (8, 7, A["turq"][1]), (6, 6, A["turq"][2]), (9, 6, A["turq"][1])]
    d[("ancientstone", "helmet")] = [(x, 6, A["turq"][2 if x % 2 else 1]) for x in range(5, 11)]
    d[("ancientstone", "leggings")] = [(x, 3, A["turq"][2 if x % 2 else 1]) for x in range(4, 12)]
    d[("reinforced_carapace", "chestplate")] = [(7, 5, C["base"][8]), (7, 6, C["base"][7]), (7, 7, C["base"][7])]
    return d

def lum_of(img):
    px = img.load()
    vals = [sum(px[x, y][:3]) / 3 for y in range(img.height) for x in range(img.width) if px[x, y][3] > 0]
    lo, hi = min(vals), max(vals)
    return lambda p: (sum(p[:3]) / 3 - lo) / max(1, hi - lo)

def edge_at(px, w, h, x, y, icon):
    """A rim: a neighbour on the same face (or, for icons, anywhere) is transparent. Face borders don't count, they
    are just where two faces of a box meet."""
    me = FACES.get((x, y))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if not (0 <= nx < w and 0 <= ny < h):
            if icon:
                return True
            continue
        if not icon:
            other = FACES.get((nx, ny))
            if other is None or me is None or other[:2] != me[:2]:
                continue
        if px[nx, ny][3] == 0:
            return True
    return False

def paint(src, painter, R, layer=None, icon=False):
    img = src.copy()
    px, out = src.load(), img.load()
    L = lum_of(src)
    for y in range(src.height):
        for x in range(src.width):
            p = px[x, y]
            if p[3] == 0:
                continue
            face = None if icon else FACES.get((x, y))
            e = edge_at(px, src.width, src.height, x, y, icon)
            if face and not e:
                # also trim the face's own outer border where it is the armour's rim (top row of body/legs, etc.)
                name, f, fx, fy, fw, fh = face
                e = (f in ("front", "back", "left", "right") and (fy == 0 and name in ("body", "arm"))) or \
                    (name == "leg" and layer == 1 and fy == 7 and f != "bottom")  # top of the boots
            c = Ctx(x, y, L(p), e, face, icon, layer)
            col = painter(c, R)
            out[x, y] = (*col[:3], 255)
    if icon:
        # dark tinted outline like vanilla icons: darkest pixels of the source stay darkest of the ramp
        base = R.get("base")
        for y in range(img.height):
            for x in range(img.width):
                if px[x, y][3] and L(px[x, y]) < 0.12:
                    out[x, y] = (*base[0], 255)
    return img

ICON_DETAIL = {}

def main():
    ICON_DETAIL.update(_icon_detail())
    l1, l2 = vimg("models/armor/netherite_layer_1.png"), vimg("models/armor/netherite_layer_2.png")
    icons = {k: vimg(f"item/netherite_{k}.png") for k in ("helmet", "chestplate", "leggings", "boots")}
    prev = sys.argv[1] if len(sys.argv) > 1 else None
    for name, (painter, R) in SETS.items():
        a, b = paint(l1, painter, R, 1), paint(l2, painter, R, 2)
        os.makedirs(os.path.join(TEX, "models/armor"), exist_ok=True)
        a.save(os.path.join(TEX, f"models/armor/{name}_layer_1.png"))
        b.save(os.path.join(TEX, f"models/armor/{name}_layer_2.png"))
        for k, ic in icons.items():
            im = paint(ic, painter, R, icon=True)
            for (x, y, col) in ICON_DETAIL.get((name, k), []):
                if ic.getpixel((x, y))[3]:
                    im.putpixel((x, y), (*col, 255))
            im.save(os.path.join(TEX, f"item/{name}_{k}.png"))
        if prev:
            st = Image.new("RGBA", (64, 64))
            st.paste(a, (0, 0)); st.paste(b, (0, 32))
            st.save(os.path.join(prev, f"{name}_stack.png"))

main()

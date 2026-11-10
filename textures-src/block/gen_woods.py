"""Exofall woods: Stiltreed, Combwood, Ribwood, Veilpine. For each: bark (log side), log top, stripped side/top,
planks, door top/bottom, trapdoor, door item, and leaves for Stiltreed and Veilpine. Writes .px grids.
Colours are in texlib.PALETTES (<wood>_bark, <wood>_inner, <wood>_leaf, lichen)."""
import math, os, random, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from texlib import Noise, write_px_multi

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM = os.path.join(HERE, "..", "item")
N, S = 16, 10  # size, ramp steps
cl = lambda v: max(0, min(S - 1, int(round(v))))

def comb_wall(x, y):
    """Pixel honeycomb, 4x4 cells (plus-shaped interior reads as a hex cell), alternate rows offset by 2. Tiles at 16."""
    lx, ly = (x + (2 if (y // 4) % 2 else 0)) % 4, y % 4
    return lx == 3 or ly == 3 or (lx in (0, 2) and ly in (0, 2))

def hex_wall(x, y, ox=0.0, oy=0.0):
    """Real hexagonal comb (Voronoi of a near-hex lattice, cells ~5 px across) for faces that don't need to tile."""
    pts = []
    for i in range(-2, 6):
        for j in range(-2, 6):
            pts.append((i * 6 + (3 if j % 2 else 0) + ox, j * 5 + oy))
    d = sorted(math.hypot(x + 0.5 - px, y + 0.5 - py) for px, py in pts)
    return d[1] - d[0] < 1.05

def blank():
    return [[None] * N for _ in range(N)]

# ---------------------------------------------------------------- planks
def planks(w, seed):
    inner = f"{w}_inner"
    r = random.Random(seed)
    grain = Noise(seed, 16)
    g = blank()
    joints = [r.randrange(2, 14) for _ in range(4)]
    bases = [4.6 + r.random() * 1.4 for _ in range(4)]
    for y in range(N):
        b, by = y // 4, y % 4
        for x in range(N):
            v = bases[b] + (grain.at(x / 4.0, y * 1.7) - 0.5) * 2.4
            if by == 0: v += 1.0
            if by == 3: v = 1.6 + (0.8 if r.random() < 0.25 else 0)
            if x == joints[b] and by != 3: v = 2.2
            if x == (joints[b] + 1) % 16 and by != 3: v += 1.0
            if w == "stiltreed" and by in (1, 2) and x % 8 == (joints[b] + 4) % 8:
                v = 7.6 if by == 1 else 3.0                      # reed nodes in the split slats
            if w == "ribwood": v = 0.6 * v + 0.4 * (bases[b] + (1 if by == 0 else 0)) if by != 3 else v
            g[y][x] = (inner, cl(v))
    return g

# ---------------------------------------------------------------- bark
def bark(w, seed):
    bk = f"{w}_bark"
    r = random.Random(seed)
    n1, n2 = Noise(seed, 16), Noise(seed + 3, 8)
    g = blank()
    for y in range(N):
        for x in range(N):
            if w == "stiltreed":
                # long vertical fibres; one raised node ring per block
                v = 5.0 + (n1.at(x * 1.9, y / 5.0) - 0.5) * 3.4 + (1.0 if x % 4 == 1 else -0.4 if x % 4 == 3 else 0)
                if y == 11: v = 8.2 - 0.1 * x
                if y == 12: v = 1.6
                if y == 10: v += 0.8
            elif w == "combwood":
                v = 4.6 + (n1.at(x * 1.4, y / 4.0) - 0.5) * 2.6
                # faint hex imprint under the wax
                if not comb_wall(x, y): v -= 0.9
                v = cl(v)
            elif w == "ribwood":
                # horizontal rib ridges with a dark groove between, a few vertical cracks
                ry = y % 4
                v = [7.8, 6.4, 5.0, 2.0][ry] + (n2.at(x / 2.0, y / 2.0) - 0.5) * 1.6
                if (x == 4 or x == 11) and ry in (1, 2) and n1.at(x, y / 3.0) > 0.5: v = 3.0
            else:  # veilpine: tall irregular bark scales between broken vertical fissures
                edges = [0, 3, 7, 12, 16]
                c = max(i for i in range(4) if edges[i] <= x)
                cx = x - edges[c]
                cw = edges[c + 1] - edges[c]
                ph = [0, 5, 2, 9][c]
                hgt = [7, 8, 6, 8][c]
                yo = (y + ph) % hgt
                v = 5.6 - yo * 0.28 + (n2.at(x / 2.0, y / 2.0) - 0.5) * 1.8 - cx * 0.35
                if cx == 0: v += 0.9
                v += (n1.at(x * 2.0, y / 6.0) - 0.5) * 1.6       # vertical streaks
                if cx == cw - 1: v = 1.4 if n1.at(x, y / 3.0) > 0.25 else 3.0
                if yo == hgt - 1 and cx in (1, 2) and c % 2 == 0: v = 2.6   # rare short breaks
            g[y][x] = (bk, cl(v))
    if w == "combwood":   # glossy wax drips from the top
        for dx, y0, ln in ((2, 1, 6), (7, 9, 4), (12, 5, 7)):
            for k in range(ln):
                g[(y0 + k) % 16][dx] = (bk, 9 if k < ln - 1 else 7)
                if k < ln - 2: g[(y0 + k) % 16][(dx + 1) % 16] = (bk, 7)
            g[(y0 + ln) % 16][dx] = (bk, 2)
    if w == "veilpine":   # glowing lichen patches
        for (lx, ly, cells) in ((1, 3, ((0, 0, 1), (1, 0, 2), (0, 1, 0))), (9, 11, ((0, 0, 2), (0, 1, 1)))):
            for (ox, oy, i) in cells:
                g[(ly + oy) % 16][(lx + ox) % 16] = ("lichen", i)
    return g

def log_top(w, seed, stripped=False):
    bk, inner = f"{w}_bark", f"{w}_inner"
    n = Noise(seed, 8)
    g = blank()
    for y in range(N):
        for x in range(N):
            dx, dy = x - 7.5, y - 7.5
            rr = max(abs(dx), abs(dy)) * 0.55 + math.hypot(dx, dy) * 0.45
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0 and not stripped:
                g[y][x] = (bk, cl(4 + (n.at(x / 2.0, y / 2.0) - 0.5) * 3 - (1 if (x + y) % 3 == 0 else 0)))
                continue
            if edge == 0 and stripped:
                g[y][x] = (inner, 2); continue
            v = 5.4 + 1.3 * math.cos(rr * 2.3) + (n.at(x / 2.0, y / 2.0) - 0.5) * 0.8 - 0.06 * (dx + dy)
            if w == "stiltreed" and rr < 2.6:      # hollow reed core
                v = 0.6 if rr < 1.6 else 2.2
            if w == "combwood" and rr < 5.4:       # the hollow core is filled with comb
                v = (8.2 - 0.12 * (dx + dy)) if hex_wall(x, y, 7.5, 7.5) else 2.4 - 0.15 * (dx + dy)
            if w == "ribwood" and rr < 1.6:        # marrow
                v = 8.6
            if edge == 1 and not stripped: v -= 1.0
            g[y][x] = (inner, cl(v))
    return g

def stripped_side(w, seed):
    inner = f"{w}_inner"
    n = Noise(seed, 16)
    g = blank()
    for y in range(N):
        for x in range(N):
            v = 5.2 + (n.at(x * 1.6, y / 5.0) - 0.5) * 2.6 + (0.6 if x % 5 == 1 else 0)
            if w == "stiltreed" and y == 11: v += 1.6
            if w == "stiltreed" and y == 12: v -= 1.8
            if w == "ribwood" and y % 4 == 3 and x % 7 != 3: v -= 0.6
            g[y][x] = (inner, cl(v))
    return g

# ---------------------------------------------------------------- leaves
def leaves(w, seed):
    lf = f"{w}_leaf"
    r = random.Random(seed)
    mass = Noise(seed, 8)
    g = blank()
    if w == "stiltreed":
        # drooping fronds: blades hanging down and out, each with a lighter midrib
        for y in range(N):
            for x in range(N):
                if mass.at(x / 2.0, y / 2.0) > 0.52:
                    g[y][x] = (lf, cl(2 + (mass.at(x / 1.3, y / 1.3)) * 2))
        for b in range(7):
            x0, y0 = r.randrange(16), r.randrange(16)
            d = r.choice((-1, 1))
            for k in range(8):
                x = (x0 + int(d * k * 0.55)) % 16
                y = (y0 + k) % 16
                v = 7.5 - k * 0.45
                g[y][x] = (lf, cl(v + 1.2))
                g[y][(x - d) % 16] = (lf, cl(v - 1.0))
                if k < 4: g[y][(x + d) % 16] = (lf, cl(v - 0.4))
    else:
        # veilpine: dense sprays of short needles, a few lichen glints
        for y in range(N):
            for x in range(N):
                m = mass.at(x / 2.0, y / 2.0)
                if m > 0.36:
                    g[y][x] = (lf, cl(1.5 + m * 4.5 - 0.08 * (x + y) + 1.0))
        for s in range(16):
            x0, y0 = r.randrange(16), r.randrange(16)
            dx = r.choice((-1, 1))
            for k in range(3):
                x, y = (x0 + dx * k) % 16, (y0 + k) % 16
                g[y][x] = (lf, cl(8 - k * 1.2))
        for _ in range(3):
            x, y = r.randrange(16), r.randrange(16)
            if g[y][x] is not None: g[y][x] = ("lichen", 3)
    return g

# ---------------------------------------------------------------- doors
def door(w, seed, half):
    """half: 'top' or 'bottom'. Frame of bark, planks inside, a window motif per wood in the top half."""
    bk, inner = f"{w}_bark", f"{w}_inner"
    n = Noise(seed, 16)
    g = blank()
    for y in range(N):
        for x in range(N):
            frame = x in (0, 15) or (half == "top" and y == 0) or (half == "bottom" and y == 15)
            v = 5.0 + (n.at(x / 3.0, y * 1.3 + (0 if half == "top" else 16)) - 0.5) * 2.0
            if frame:
                g[y][x] = (bk, cl(6.6 if (x == 0 or y == 0) else 2.4)); continue
            if w == "stiltreed":          # vertical reed slats lashed with bark bands
                s = (x - 1) % 3
                v += 1.4 if s == 0 else -1.2 if s == 2 else 0
                if y in (3, 12):
                    g[y][x] = (bk, cl(5.5 + (1 if y == 3 else -0.5))); continue
                if half == "top" and s == 2 and 4 < y < 11: g[y][x] = None; continue
            elif w == "combwood":
                if half == "top" and 1 <= y <= 13:
                    wall = hex_wall(x, y, 7.5, 2.0) or x in (1, 14) or y in (1, 13)
                    if not wall: g[y][x] = None; continue
                    v = 7.4 - 0.1 * y
                if half == "bottom" and y in (2, 13): v -= 1.6
            elif w == "ribwood":
                if half == "top" and 2 <= y <= 13 and 2 <= x <= 13:
                    if y % 3 != 2: g[y][x] = None; continue
                    v = 7.6 - abs(x - 7.5) * 0.25
                if half == "top" and x in (1, 14): v = 6.4
                if half == "bottom" and y % 4 == 3: v -= 1.5
            else:  # veilpine: vertical boards, a round window
                if x in (5, 10): v -= 1.6
                if half == "top":
                    d = math.hypot(x - 7.5, y - 7.0)
                    if d < 3.2: g[y][x] = None; continue
                    if d < 4.2: v = 2.4
            if half == "bottom" and x == 13 and y in (1, 2): g[y][x] = (bk, 1); continue  # handle
            g[y][x] = (inner, cl(v))
    return g

def trapdoor(w, seed):
    top, bot = door(w, seed, "top"), door(w, seed + 1, "bottom")
    g = [row[:] for row in top]
    bk = f"{w}_bark"
    for x in range(N):
        g[15][x] = (bk, 2)
        g[0][x] = (bk, 6)
    for y in range(N):
        g[y][0] = (bk, 6); g[y][15] = (bk, 2)
    return g

def door_item(w, top, bot):
    """Narrow door icon: every other column of the full door, 2 rows -> 1, with a dark outline."""
    full = top + bot
    g = blank()
    bk = f"{w}_bark"
    for y in range(N):
        for i, x in enumerate(range(0, 16, 2)):
            c = full[y * 2][x] if full[y * 2][x] is not None else full[y * 2 + 1][x]
            g[y][4 + i] = c
    for y in range(N):
        g[y][3] = (bk, 1); g[y][12] = (bk, 1)
    for x in range(3, 13):
        g[0][x] = (bk, 1); g[15][x] = (bk, 1)
    g[0][3] = g[0][12] = g[15][3] = g[15][12] = None
    return g

WOODS = ["stiltreed", "combwood", "ribwood", "veilpine"]
LEAVES = {"stiltreed", "veilpine"}

if __name__ == "__main__":
    for k, w in enumerate(WOODS):
        seed = 100 + k * 17
        rs = {f"{w}_bark": S, f"{w}_inner": S}
        if w == "veilpine": rs["lichen"] = 4
        rl = dict(rs)
        if w in LEAVES: rl[f"{w}_leaf"] = S; rl["lichen"] = 4
        write_px_multi(os.path.join(HERE, f"{w}_planks.px"), f"{w} planks", rs, planks(w, seed))
        write_px_multi(os.path.join(HERE, f"{w}_log.px"), f"{w} bark", rs, bark(w, seed + 1))
        write_px_multi(os.path.join(HERE, f"{w}_log_top.px"), f"{w} log top", rs, log_top(w, seed + 2))
        write_px_multi(os.path.join(HERE, f"stripped_{w}_log.px"), f"stripped {w} side", rs, stripped_side(w, seed + 3))
        write_px_multi(os.path.join(HERE, f"stripped_{w}_log_top.px"), f"stripped {w} top", rs, log_top(w, seed + 2, stripped=True))
        top, bot = door(w, seed + 4, "top"), door(w, seed + 5, "bottom")
        write_px_multi(os.path.join(HERE, f"{w}_door_top.px"), f"{w} door top", rs, top)
        write_px_multi(os.path.join(HERE, f"{w}_door_bottom.px"), f"{w} door bottom", rs, bot)
        write_px_multi(os.path.join(HERE, f"{w}_trapdoor.px"), f"{w} trapdoor", rs, trapdoor(w, seed + 6))
        write_px_multi(os.path.join(ITEM, f"{w}_door.px"), f"{w} door item", rs, door_item(w, top, bot))
        if w in LEAVES:
            write_px_multi(os.path.join(HERE, f"{w}_leaves.px"), f"{w} leaves", rl, leaves(w, seed + 7))

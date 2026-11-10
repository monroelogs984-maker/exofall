"""Exofall stones: Chitinstone family, Glowmarl (.px grids) and animated iridescent Shellrock (PNG + mcmeta).
All textures tile at 16 px."""
import colorsys, json, math, os, random, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from texlib import Noise, write_px, write_png, ramp, hex2rgb, PALETTES, OUT

HERE = os.path.dirname(os.path.abspath(__file__))
N = 16
clamp = lambda v, lo=0, hi=9: max(lo, min(hi, int(round(v))))

# ---------------------------------------------------------------- Chitinstone
# Fossil-shell plates in 4 overlapping courses (like shingles): each plate is lit at its top, darkens toward its
# curved lower edge, and casts a short shadow on the plate below. Seams wander and break, so it reads as stone,
# not as a tiled roof.
def chitin_grid(seed, flat=False):
    r = random.Random(seed)
    walk = Noise(seed, 16)
    mott = Noise(seed + 1, 8)
    courses = 4
    seam = lambda L, x: L * 4 + int(round((walk.at(x / 3.0, (L % courses) * 3.1) - 0.5) * 3.2))
    plates = []  # per course: list of (start_x, width)
    for L in range(courses):
        off = r.randrange(16)
        w1 = r.choice([6, 7, 8, 9, 10])
        plates.append([(off, w1), ((off + w1) % 16, 16 - w1)])
    grid = [[0] * N for _ in range(N)]
    owner = [[None] * N for _ in range(N)]
    # paint lowest course first; upper courses overlap downward onto it
    for L in reversed(range(courses)):
        for (sx, w) in plates[L]:
            for i in range(w):
                x = (sx + i) % 16
                u = (i + 0.5) / w
                bulge = 1 if 0.2 < u < 0.8 else 0
                top, bot = seam(L, x), seam(L + 1, x) + bulge
                for y in range(top, bot):
                    owner[y % 16][x] = (L, sx, w, i, top, bot)
    for y in range(N):
        for x in range(N):
            L, sx, w, i, top, bot = owner[y][x]
            yy = y if y >= top % 16 or top >= 0 else y
            depth = (y - top) % 16
            h = max(1, bot - top)
            t = depth / max(1, h - 1)
            v = 5.6 - 2.8 * t + (mott.at(x / 2.0, y / 2.0) - 0.5) * 2.0 - 0.5 * (i / w)
            if flat:
                v = 5.0 + (mott.at(x / 2.0, y / 2.0) - 0.5) * 1.6 - 0.8 * t
            if depth == h - 1 and r.random() < 0.62:
                v = 1.4 + r.random() * 0.8      # lower edge of the plate
            if depth == 0 and r.random() < 0.55:
                v += 1.2                         # lit upper rim
            if i == 0 and (sx // 5) % 2 == 0 and r.random() < 0.5 and not flat:
                v -= 1.6                         # side joint between neighbouring plates (broken)
            grid[y][x] = clamp(v)
    return grid

def polished_grid(seed):
    mott = Noise(seed, 8)
    g = [[0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            v = 5.2 + (mott.at(x / 2.5, y / 2.5) - 0.5) * 1.6 - 0.05 * (x + y)
            # faint ghost of one fossil arc across the face
            d = math.hypot(x - 7.5, y - 13.0)
            if 8.6 < d < 9.6 and 2 < y < 12: v -= 0.8
            if 9.6 <= d < 10.4 and 2 < y < 12: v += 0.45
            if x == 0 or y == 0: v = 6.9
            if x == 15 or y == 15: v = 2.6
            if (x == 1 or y == 1) and 0 < x < 15 and 0 < y < 15: v = max(v, 6.4)
            if (x == 14 or y == 14) and 0 < x < 15 and 0 < y < 15: v = min(v, 3.6)
            g[y][x] = clamp(v)
    g[0][15] = g[15][0] = 4
    return g

def bricks_grid(seed):
    r = random.Random(seed)
    mott = Noise(seed, 8)
    g = [[0] * N for _ in range(N)]
    for y in range(N):
        row = y // 4
        for x in range(N):
            off = 4 if row % 2 else 0
            bx = (x + off) % 8
            by = y % 4
            brick = (row, ((x + off) // 8) % 2)
            base = 5.0 + (random.Random(hash(brick) + seed).random() - 0.5) * 1.6
            if by == 3 or bx == 7:
                v = 1.0 + (0.6 if r.random() < 0.3 else 0)    # mortar
            else:
                v = base + (mott.at(x / 2.0, y / 2.0) - 0.5) * 1.8
                if by == 0: v += 1.3
                if bx == 0: v += 0.7
                if by == 2: v -= 1.0
                if bx == 6: v -= 0.8
            g[y][x] = clamp(v)
    return g

def pillar_side_grid(seed):
    mott = Noise(seed, 8)
    g = [[0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            # gentle cylinder: lit left of centre
            v = 5.6 - abs(x - 6.0) * 0.32 + (mott.at(x / 3.0, y / 2.0) - 0.5) * 1.2
            if x in (4, 11): v -= 1.4           # flutes
            if x in (5, 12): v += 0.5
            if y == 15: v -= 2.2                  # one segment joint per block, like a jointed leg
            if y == 0: v += 1.0
            if y == 14: v -= 0.6
            if x == 0: v = 6.8
            if x == 15: v = 2.0
            g[y][x] = clamp(v)
    return g

def pillar_top_grid(seed):
    """Cut ammonite: a spiral of chambers inside a rim."""
    g = [[0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            dx, dy = x - 7.5, y - 7.5
            rr = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            turn = (rr - 1.25 * (a + math.pi) / math.pi) % 2.5   # spiral whorls 2.5 px apart
            v = 5.4 - 0.12 * (dx + dy)
            if turn < 0.7: v = 2.0                               # whorl wall
            chamber = int(((a + math.pi) / (2 * math.pi)) * 9)
            if rr < 1.2: v = 2.5
            if rr > 6.6: v = 6.4 - 0.25 * (dx + dy)               # rim
            if 6.6 >= rr > 6.0: v = 2.6
            if x in (0, 15) or y in (0, 15): v = 3.5 - 0.1 * (dx + dy)
            g[y][x] = clamp(v)
    return g

# ---------------------------------------------------------------- Glowmarl
def glowmarl_grid(seed):
    r = random.Random(seed)
    soft = Noise(seed, 8)
    fine = Noise(seed + 7, 16)
    g = [[0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            v = 5.0 + (soft.at(x / 2.0, y / 2.0) - 0.5) * 3.2 + (fine.at(x, y) - 0.5) * 1.0
            g[y][x] = clamp(v, 2, 7)
    # glowing grains: isolated 1-2 px flecks
    placed = 0
    while placed < 9:
        x, y = r.randrange(16), r.randrange(16)
        if all(g[(y + j) % 16][(x + i) % 16] < 8 for i in (-1, 0, 1) for j in (-1, 0, 1)):
            g[y][x] = 9
            if r.random() < 0.4: g[y][(x + 1) % 16] = 8
            placed += 1
    # a few pits
    return g

# ---------------------------------------------------------------- Shellrock (animated)
def shellrock():
    """Fused glassy facets in pearl white with a slow oil-slick shimmer drifting across them."""
    r = random.Random(11)
    pts = [(r.random() * 16, r.random() * 16) for _ in range(7)]
    def cell(x, y):
        best = []
        for i, (px, py) in enumerate(pts):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    best.append((math.hypot(x + 0.5 - px - ox, y + 0.5 - py - oy), i))
        best.sort()
        return best[0][1], best[1][0] - best[0][0]
    pearl = ramp("shellrock", 10)
    facet_val = [0.55 + r.random() * 0.4 for _ in pts]
    hue_n = Noise(5, 8)
    frames, F = [], 16
    for f in range(F):
        t = f / F
        rows = []
        for y in range(N):
            row = []
            for x in range(N):
                c, edge = cell(x, y)
                v = facet_val[c] + 0.03 * (8 - x - y) / 8
                if edge < 0.9: v -= 0.32          # fracture line between facets
                elif edge < 1.6: v += 0.08         # bright lip beside the fracture
                v = max(0.0, min(0.999, v))
                base = pearl[int(v * 9.99)]
                # hue drifts diagonally across the block and loops with t; each facet catches it slightly offset
                hue = ((x + y) / 32.0 + 0.35 * hue_n.at(x / 5.0, y / 5.0) + t + c * 0.05) % 1.0
                ir = colorsys.hls_to_rgb(hue, 0.74, 0.9)
                k = 0.10 + 0.14 * (0.5 + 0.5 * math.sin(2 * math.pi * (facet_val[c] + t)))
                k = k * (1.0 if edge >= 0.9 else 0.4)
                col = [base[j] * (1 - k) + ir[j] * 255 * k for j in range(3)]
                row.append((*col, 255))
            rows.append(row)
        frames.extend(rows)
    path = os.path.join(OUT, "block", "shellrock.png")
    write_png(path, frames, N, N * F)
    with open(path + ".mcmeta", "w") as fh:
        json.dump({"animation": {"frametime": 5, "interpolate": True}}, fh, indent=2)

if __name__ == "__main__":
    write_px(os.path.join(HERE, "chitinstone.px"), "chitin", "chitin", 10, chitin_grid(3), "Chitinstone: fossil-shell plates in overlapping courses")
    write_px(os.path.join(HERE, "polished_chitinstone.px"), "chitin", "chitin", 10, polished_grid(4), "Polished Chitinstone: bevelled face with a faint fossil arc")
    write_px(os.path.join(HERE, "chitinstone_bricks.px"), "chitin", "chitin", 10, bricks_grid(5), "Chitinstone Bricks")
    write_px(os.path.join(HERE, "chitinstone_pillar.px"), "chitin", "chitin", 10, pillar_side_grid(6), "Chitinstone Pillar side: fluted, jointed like a leg")
    write_px(os.path.join(HERE, "chitinstone_pillar_top.px"), "chitin", "chitin", 10, pillar_top_grid(7), "Chitinstone Pillar top: cut ammonite")
    write_px(os.path.join(HERE, "glowmarl.px"), "glowmarl", "glowmarl", 10, glowmarl_grid(8), "Glowmarl: soft mint sediment with glowing grains")
    shellrock()

"""Exofall bug nests (Warren spawners): one 16x16 side + top per meteor, writes PNGs directly.
Each is the meteor's material riddled with burrow holes: a dark mouth, a shadowed upper-left lip and a lit lower-right
rim (light from the top-left), with a glint of eyes or larvae deep in a few holes."""
import math, os, random, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
from texlib import Noise, write_png, hex2rgb, OUT

N = 16

def gradramp(n, *stops):
    st = [hex2rgb(s) for s in stops]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(st) - 1)
        k = min(int(t), len(st) - 2)
        f = t - k
        out.append(tuple(st[k][j] + (st[k + 1][j] - st[k][j]) * f for j in range(3)))
    return out

def pick(r, v):
    return r[max(0, min(len(r) - 1, int(round(v))))]

# Holes: (cx, cy, radius, glint?) chosen so none crosses a tile edge (the faces tile).
SIDE_HOLES = [(4.0, 4.5, 2.2, True), (11.5, 3.5, 1.6, False), (9.5, 9.5, 2.5, True), (3.5, 11.5, 1.7, False), (13.0, 12.5, 1.5, True)]
TOP_HOLES = [(8.0, 8.0, 3.0, True), (3.0, 3.5, 1.6, False), (12.5, 3.0, 1.5, True), (3.5, 12.5, 1.5, True), (12.5, 12.5, 1.7, False)]

STYLES = {
    # name: base ramp, mouth ramp, glint colour, grain ("noise", "strata", "grain", "comb")
    "mire_nest": (gradramp(10, "#1c160e", "#2e2414", "#42341c", "#584626", "#6c5830", "#826c40", "#9a8456"),
                  gradramp(4, "#07060a", "#110f0c", "#1c1912", "#2a2519"), (214, 230, 90), "noise"),
    "brood_comb": (gradramp(10, "#4a2004", "#6e3608", "#94520e", "#b86e16", "#d68c22", "#eaa838", "#f8c860", "#ffe49a"),
                   gradramp(4, "#1a0a02", "#2c1404", "#3e1e06", "#552a08"), (255, 244, 214), "comb"),
    "sand_burrow": (gradramp(10, "#6a4a2a", "#8a6638", "#a8844a", "#c2a060", "#d6b878", "#e6cc92", "#f2deae"),
                    gradramp(4, "#140c06", "#22160c", "#332212", "#4a321c"), (255, 176, 48), "strata"),
    "rotwood_hollow": (gradramp(10, "#141a1c", "#20282a", "#2e3836", "#3e4a44", "#505c50", "#66705e", "#80886e"),
                       gradramp(4, "#05080a", "#0a1014", "#10181c", "#182226"), (110, 240, 220), "grain"),
}

def texture(name, holes, seed, top):
    base, mouth, glint, grain = STYLES[name]
    r = random.Random(seed)
    n1, n2 = Noise(seed, 8), Noise(seed + 7, 4)
    px = [[None] * N for _ in range(N)]
    nb = len(base)
    for y in range(N):
        for x in range(N):
            v = nb * 0.5 + (n1.at(x / 2.0, y / 2.0) - 0.5) * 3.0 + (n2.at(x / 1.0, y / 1.0) - 0.5) * 1.6
            if grain == "strata" and not top:
                v += 1.2 * math.sin((y + 1.5 * n1.at(x / 4.0, 1.0)) * 1.1)
            elif grain == "grain" and not top:
                v += 1.4 * math.sin((x + 2.0 * n1.at(1.0, y / 4.0)) * 1.6)
            elif grain == "grain" and top:
                d = math.hypot(x - 7.5, y - 7.5)
                v += 1.2 * math.sin(d * 1.9)
            elif grain == "comb":
                # hex-ish comb walls: offset rows of 4x4 cells, wall = brighter wax
                row = y // 4
                cx = (x + (2 if row % 2 else 0)) % 4
                cy = y % 4
                if cx == 0 or cy == 0:
                    v += 2.2
                else:
                    v -= 0.8
            if grain == "noise" and r.random() < 0.06:
                v -= 2.5  # damp pits
            if r.random() < 0.04:
                v += 2.0  # specks
            px[y][x] = pick(base, v)
    # moss on rotwood, silk on mire
    if name == "rotwood_hollow":
        moss = gradramp(5, "#1c3a22", "#2c5a2e", "#40803a", "#62a44a", "#8ac460")
        for y in range(N):
            for x in range(N):
                m = n2.at(x / 2.0 + 3, y / 2.0 + 1)
                if m > 0.68 and (top or y < 8):
                    px[y][x] = pick(moss, (m - 0.68) * 14 + (1 if x < 8 else 0))
    if name == "mire_nest":
        silk = (206, 198, 160)
        for (sx, sy, ex, ey) in [(1, 8, 6, 9)] if not top else [(10, 6, 15, 8)]:
            steps = max(abs(ex - sx), abs(ey - sy))
            for t in range(steps + 1):
                xx = round(sx + (ex - sx) * t / steps)
                yy = round(sy + (ey - sy) * t / steps)
                if r.random() < 0.8:
                    px[yy % N][xx % N] = silk
    for k, (hx, hy, hr, eyes) in enumerate(holes):
        hx += r.uniform(-0.6, 0.6)
        hy += r.uniform(-0.6, 0.6)
        hr *= r.uniform(0.8, 1.15)
        lobes = [r.uniform(-0.45, 0.45) for _ in range(3)]
        ph = r.uniform(0, math.tau)
        for y in range(N):
            for x in range(N):
                dx, dy = x + 0.5 - hx, y + 0.5 - hy
                a = math.atan2(dy, dx)
                wob = 1 + lobes[0] * math.sin(2 * a + ph) * 0.5 + lobes[1] * math.sin(3 * a + ph) * 0.5 + lobes[2] * math.cos(a)
                d = math.hypot(dx, dy) / wob
                if d <= hr:
                    depth = 1 - d / hr
                    # deeper toward the lower-right inside (we see the far wall lit less)
                    v = (len(mouth) - 1) * (0.9 - depth) - (dx + dy) * 0.25
                    px[y][x] = pick(mouth, v)
                    if d > hr - 0.75 and dx + dy < -0.3:
                        px[y][x] = pick(mouth, 0)  # lip shadow on the upper-left inside
                elif d <= hr + 0.9:
                    if dx + dy > 0.4:
                        px[y][x] = pick(base, nb - 1 - (0 if d < hr + 0.5 else 1))  # lit rim, lower-right
                    elif dx + dy < -0.6:
                        px[y][x] = pick(base, 1)
        if eyes:
            ex, ey = int(hx - 0.2), int(hy)
            px[ey][ex] = glint
            if hr >= 2.0:
                px[ey][min(N - 1, ex + 1)] = tuple(c * 0.55 for c in glint)
    return [[(*c, 255) for c in row] for row in px]

for i, name in enumerate(STYLES):
    write_png(os.path.join(OUT, "block", f"{name}.png"), texture(name, SIDE_HOLES, 900 + i * 11, False), N, N)
    write_png(os.path.join(OUT, "block", f"{name}_top.png"), texture(name, TOP_HOLES, 950 + i * 11, True), N, N)

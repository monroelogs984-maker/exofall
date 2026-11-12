"""Material painters for Exofall weapons. Each factory returns ctx -> (r, g, b, a)."""
import math

from model3d import gradramp


def h(*xs):
    """Deterministic hash of numbers -> [0, 1)."""
    v = 0
    for x in xs:
        v = (v * 1000003) ^ (int(math.floor(x * 7919)) & 0xFFFFFFFF)
    v = (v ^ (v >> 13)) * 0x5BD1E995 & 0xFFFFFFFF
    return ((v ^ (v >> 15)) & 0xFFFF) / 65536.0


def pick(ramp, idx, a=255):
    i = max(0, min(len(ramp) - 1, int(round(idx))))
    return (*ramp[i], a)


def rim(ctx, top=2.0, side=1.2, bottom=-0.6):
    """Bevel: lit top/left edges, a lighter side rim, darker bottom/right edge."""
    if ctx["edge"] > 0:
        return 0.0
    if ctx["top"] or ctx["left"]:
        return top
    if ctx["bottom"]:
        return bottom
    return side * 0.6


def metal(ramp, base=0.5, streak=1.4, jit=0.6, grad=None, rim_amt=1.0, a=255):
    """Brushed metal: streaks run along the weapon (constant along u), bevelled edges."""
    n = len(ramp)

    def f(ctx):
        p = ctx["p"]
        s = (h(round(p[0] * 2), round(p[2] * 2)) - 0.5) * streak
        j = (h(p[0], p[1], p[2]) - 0.5) * jit
        g = 0.0
        if grad:
            u0, u1, amt = grad
            g = max(0.0, min(1.0, (p[1] - u0) / (u1 - u0))) * amt
        idx = base * (n - 1) + s + j + g + rim(ctx) * rim_amt
        return pick(ramp, idx, a)
    return f


def leather_wrap(ramp, pitch=1.5):
    """Spiral grip wrap: diagonal bands with dark seams."""
    n = len(ramp)

    def f(ctx):
        p = ctx["p"]
        t = (p[1] + (p[0] + p[2]) * 0.6) / pitch
        fr = t - math.floor(t)
        idx = (n - 1) * (0.25 + 0.5 * math.sin(fr * math.pi)) + (h(p[0], p[1], p[2]) - 0.5) * 0.6
        if fr < 0.14:
            idx = 0
        return pick(ramp, idx)
    return f


def gem(ramp, centre_fn=None, glow=False):
    """Faceted gem: bright upper-left facet, dark lower-right, a white glint near the top-left corner."""
    n = len(ramp)

    def f(ctx):
        cx, cy, w, hh = ctx["cx"], ctx["cy"], ctx["w"], ctx["h"]
        tx, ty = (cx + 0.5) / w, (cy + 0.5) / hh
        d = (tx + ty) / 2
        idx = (n - 1) * (0.95 - d * 0.75)
        if ctx["edge"] == 0:
            idx -= 1.2
        if cx == min(1, w - 1) and cy == min(1, hh - 1) and w > 2 and hh > 2:
            return (255, 255, 245, 255)
        return pick(ramp, idx)
    return f


def reed(ramp, node_pitch=5.0, node_off=0.0):
    """Segmented reed stalk: fibres along u, a dark node ring with a pale swelling just above it."""
    n = len(ramp)

    def f(ctx):
        p = ctx["p"]
        t = (p[1] - node_off) / node_pitch
        fr = t - math.floor(t)
        fib = (h(round(p[0] * 2), round(p[2] * 2)) - 0.5) * 1.6
        idx = (n - 1) * 0.5 + fib + rim(ctx) * 0.7
        if fr < 0.07:
            idx = 1
        elif fr < 0.16:
            idx = n - 2
        return pick(ramp, idx)
    return f


def honeycomb(ramp, wall, cell=1.6, glow_ramp=None):
    """Hexagonal comb cells on any face, in the face's own plane."""
    n = len(ramp)

    def f(ctx):
        p, nrm = ctx["p"], ctx["n"]
        ax = [i for i in range(3) if abs(nrm[i]) < 0.5]
        a, b = p[ax[0]], p[ax[1]]
        # axial hex coords
        q = (2 / 3 * a) / cell
        r = (-1 / 3 * a + math.sqrt(3) / 3 * b) / cell
        x, z = q, r
        y = -x - z
        rx, ry, rz_ = round(x), round(y), round(z)
        dx, dy, dz = abs(rx - x), abs(ry - y), abs(rz_ - z)
        if dx > dy and dx > dz:
            rx = -ry - rz_
        elif dy > dz:
            ry = -rx - rz_
        else:
            rz_ = -rx - ry
        dist = max(abs(rx - x), abs(ry - y), abs(rz_ - z))
        if dist > 0.36:
            return pick(wall, len(wall) * 0.55 + rim(ctx) + (h(rx, rz_) - 0.5))
        idx = (n - 1) * (0.35 + 0.5 * (1 - dist / 0.36)) + (h(rx, rz_) - 0.5) * 2
        return pick(ramp, idx)
    return f


def wing(membrane, vein, alpha=150):
    """Glasswing membrane: translucent, long veins along the blade (box-space u) and a few cross veins."""
    n = len(membrane)

    def f(ctx):
        bp, b = ctx["bp"], ctx["box"]
        lo, hi = b.lo, b.hi
        tv = (bp[0] - lo[0]) / max(1e-6, hi[0] - lo[0])
        tu = (bp[1] - lo[1]) / max(1e-6, hi[1] - lo[1])
        if ctx["face"] not in ("north", "south"):
            return pick(vein, len(vein) - 2, 235)  # the blade's thin edge is a solid vein
        if tv > 0.86:
            return pick(vein, 1, 245)  # leading-edge spine (outer side of the curve)
        if tv < 0.12:
            return pick(membrane, n - 1, 225)  # pale cutting edge
        st = getattr(b, "stigma", None)
        if st and st[0] <= tu <= st[1] and 0.6 < tv <= 0.86:
            return pick(vein, 1, 235)
        long_v = abs((tv * 3.0) % 1.0 - 0.5) < 0.09
        cross = abs((tu * b.cross) % 1.0 - 0.5) < 0.06 if getattr(b, "cross", 0) else False
        if long_v or cross:
            return pick(vein, 2, 230)
        idx = (n - 1) * (0.45 + 0.4 * tu) + (h(bp[0], bp[1]) - 0.5) * 1.0
        return pick(membrane, idx, alpha)
    return f


def eye(ramp, pupil):
    """Glowing amber eye: radial from the face centre, a vertical slit pupil, a glint."""
    n = len(ramp)

    def f(ctx):
        cx, cy, w, hh = ctx["cx"], ctx["cy"], ctx["w"], ctx["h"]
        x, y = (cx + 0.5) / w - 0.5, (cy + 0.5) / hh - 0.5
        r = math.hypot(x, y) * 2
        if ctx["face"] in ("south", "north") and abs(x) < 0.09 and abs(y) < 0.32:
            return (*pupil, 255)
        if cx == int(w * 0.3) and cy == int(hh * 0.25):
            return (255, 255, 230, 255)
        return pick(ramp, (n - 1) * (1 - r * 0.8))
    return f


def chitin(ramp, base=0.5, seg=None):
    """Glossy insect chitin: smooth gradient, sparse pale speckles, optional segment rings along u."""
    n = len(ramp)

    def f(ctx):
        p = ctx["p"]
        idx = base * (n - 1) + rim(ctx, top=1.6, side=0.8, bottom=-1.0) + (h(p[0], p[1], p[2]) - 0.5) * 0.7
        if seg:
            t = p[1] / seg
            if t - math.floor(t) < 0.12:
                idx -= 2
        if h(p[2], p[1], p[0]) > 0.97:
            idx += 2
        return pick(ramp, idx)
    return f


def flat(rgba):
    return lambda ctx: rgba


# ---- shared palettes ---------------------------------------------------------------------------------------------
BRONZE = gradramp(10, "#2c180c", "#5c3418", "#94602c", "#c89048", "#ecc27a", "#fff0c4")
PBRONZE = gradramp(12, "#2a1006", "#6a3410", "#a85c1c", "#d88a2c", "#f0b450", "#ffd888", "#fff4d0")
GOLD = gradramp(10, "#3c2406", "#7c4c0e", "#c08418", "#eeb434", "#ffe07a", "#fff8d8")
TURQ = gradramp(8, "#072e34", "#0e5c62", "#1c9892", "#40d0c0", "#a8fff0")
LEATHER = gradramp(6, "#1e0f0a", "#3c2014", "#5e3620", "#7e4e2e")
DARKWOOD = gradramp(8, "#1c120e", "#34221a", "#4e3424", "#6c4a30", "#8a6440")
HONEY = gradramp(10, "#4a2004", "#8a4606", "#c87410", "#f0a424", "#ffd060", "#fff0b8")
WAX = gradramp(8, "#5a3a0e", "#8e641c", "#bc8e30", "#e0b850", "#f8dc88")
AMBER = gradramp(8, "#3a1804", "#7a3a06", "#c06810", "#f09a20", "#ffcc60")
REED = gradramp(9, "#26281a", "#3e4222", "#5c6228", "#7e8432", "#a2a64a", "#cfcc7e")
MEMBRANE = gradramp(8, "#1e5a56", "#2e8a80", "#5cc4b4", "#9ae8d6", "#d8fff4")
VEIN = gradramp(5, "#10302c", "#1c4a44", "#2c6c62", "#4a9486", "#7cc0b0")
BOGEYE = gradramp(8, "#3a2206", "#6e420c", "#a8701a", "#d8a02c", "#f8d258", "#fff2a8")
MANTIS = gradramp(10, "#0e2a0e", "#1c4418", "#2e6622", "#46882c", "#68aa36", "#98cc56", "#d0ec96")
SILVER = gradramp(9, "#2a3434", "#4a5854", "#6e7e78", "#9aaaa2", "#c6d4cc", "#eef8f2")
EMERALD = gradramp(8, "#06301a", "#0c5a2c", "#14904a", "#2ec46c", "#7cf0a8", "#d0ffe2")
IRON = gradramp(8, "#161418", "#2c282e", "#46404a", "#625a66", "#857c8a")


def estoc(ramp, grad=None):
    """Narrow blade face: a bright ridge down the middle column(s), darker flanks, brushed streaks along u."""
    n = len(ramp)

    def f(ctx):
        p, w, cx = ctx["p"], ctx["w"], ctx["cx"]
        if ctx["face"] in ("up", "down"):
            return pick(ramp, n * 0.45)
        t = abs((cx + 0.5) / w - 0.5) * 2  # 0 at ridge, 1 at the edges
        idx = (n - 1) * (0.88 - 0.62 * t * t) + (h(round(p[0] * 2), round(p[2] * 2), cx) - 0.5) * 0.8
        if grad:
            u0, u1, amt = grad
            idx += max(0.0, min(1.0, (p[1] - u0) / (u1 - u0))) * amt
        return pick(ramp, idx)
    return f

COMBGLOW = gradramp(8, "#6a2a04", "#b05a08", "#e88a14", "#ffb830", "#ffe080", "#fff6c8")

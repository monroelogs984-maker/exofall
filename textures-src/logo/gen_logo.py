"""Mod-list logo (exofall_logo.png, 256x256 = 64x64 pixel art at 4x): a pearl Shellrock meteor half sunk in its
crater under a dark sky, glowing green through a crack, with the Crustbreaker leaning in front."""
import math, os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
N = 64
r = random.Random(7)
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
px = img.load()

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

# round badge: night sky
for y in range(N):
    for x in range(N):
        d = math.hypot(x - 31.5, y - 31.5)
        if d <= 31:
            c = lerp((14, 12, 30), (40, 30, 62), y / N)
            if r.random() < 0.012 and y < 34: c = (230, 226, 250)
            px[x, y] = (*c, 255)
# crater ground
for y in range(40, N):
    for x in range(N):
        if math.hypot(x - 31.5, y - 31.5) <= 31:
            t = (y - 40) / 24
            c = lerp((92, 72, 56), (52, 40, 34), t)
            if r.random() < 0.15: c = lerp(c, (120, 110, 104), 0.5)
            px[x, y] = (*c, 255)
for x in range(N):  # rim
    for y in (39, 40):
        if math.hypot(x - 31.5, y - 31.5) <= 31: px[x, y] = (128, 104, 80, 255)
# meteor: sphere centre (23, 41), radius 18, only the top part above ground
pearl = [(70, 66, 96), (110, 104, 136), (156, 151, 171), (196, 192, 207), (226, 223, 232), (244, 242, 246), (255, 253, 248)]
for y in range(N):
    for x in range(N):
        dx, dy = x - 23, y - 41
        if dx * dx + dy * dy <= 18 * 18 and y <= 41:
            nz = math.sqrt(max(0, 1 - (dx * dx + dy * dy) / 324))
            light = (-dx * 0.55 - dy * 0.75) / 18 * 0.6 + nz * 0.6
            k = max(0, min(len(pearl) - 1, int(round(light * (len(pearl) - 1) + (r.random() - 0.5) * 0.9))))
            c = pearl[k]
            if r.random() < 0.06: c = lerp(c, (242, 196, 232), 0.6) if r.random() < 0.5 else lerp(c, (182, 238, 242), 0.6)
            if 40 <= y <= 41: c = lerp(c, (60, 48, 40), 0.5)  # half buried edge
            px[x, y] = (*c, 255)
# glowing crack (Glowmarl green)
crack = [(19, 27), (20, 28), (21, 29), (21, 30), (22, 31), (23, 32), (24, 32), (25, 33), (25, 34), (26, 35), (27, 36), (27, 37)]
for (x, y) in crack:
    px[x, y] = (124, 240, 178, 255)
    if x + 1 < N: px[x + 1, y] = (60, 150, 120, 255)
# Crustbreaker 32x sprite in the lower right
cb = Image.open(os.path.join(ROOT, "src/main/resources/assets/exofall/textures/item/crustbreaker.png")).convert("RGBA")
img.alpha_composite(cb, (28, 6))
# badge outline
for y in range(N):
    for x in range(N):
        d = math.hypot(x - 31.5, y - 31.5)
        if 31 < d <= 32.2: px[x, y] = (10, 8, 16, 255)
img.resize((256, 256), Image.NEAREST).save(os.path.join(ROOT, "src/main/resources/exofall_logo.png"))

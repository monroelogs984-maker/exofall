"""Boat and chest-boat skins for the 4 alien woods. Only the SHAPE (alpha mask) of vanilla's oak boat textures is
used, to know which pixels the model's UVs touch; every colour comes from our own planks/stripped-log textures.
Hull -> planks, paddles -> stripped log, chest -> planks with a dark bark frame. Face edges are darkened."""
import io, os, zipfile
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
TEX = os.path.join(ROOT, "src/main/resources/assets/exofall/textures")
JAR = os.path.join(ROOT, ".gradle/repositories/ng_dummy_ng/net/minecraft/client/1.21.1/client-1.21.1-client-extra.jar")
jar = zipfile.ZipFile(JAR)

def mask(path):
    return Image.open(io.BytesIO(jar.read(path))).convert("RGBA").split()[3]

def paint(w, chest):
    m = mask(f"assets/minecraft/textures/entity/{'chest_boat' if chest else 'boat'}/oak.png")
    planks = Image.open(os.path.join(TEX, f"block/{w}_planks.png")).convert("RGB")
    inner = Image.open(os.path.join(TEX, f"block/stripped_{w}_log.png")).convert("RGB")
    bark = Image.open(os.path.join(TEX, f"block/{w}_log.png")).convert("RGB")
    W, H = m.size
    out = Image.new("RGBA", (W, H))
    a = m.load(); o = out.load()
    for y in range(H):
        for x in range(W):
            if a[x, y] < 128:
                continue
            if y >= 59:                      # chest
                c = planks.getpixel((x % 16, y % 16))
                k = 0.88
            elif x >= 62:                    # paddles
                c = inner.getpixel((x % 16, y % 16))
                k = 1.0
            else:                            # hull
                c = planks.getpixel((x % 16, y % 16))
                k = 1.0
            edge = any(not (0 <= x + dx < W and 0 <= y + dy < H) or a[x + dx, y + dy] < 128
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                if y >= 59:
                    c = bark.getpixel((x % 16, y % 16)); k = 0.55
                else:
                    k *= 0.72
            o[x, y] = tuple(int(v * k) for v in c) + (255,)
    if chest:                                 # latch: a small bright knob on the chest front
        for (x, y) in ((1, 60), (2, 60), (1, 61), (2, 61), (1, 62), (2, 62)):
            if a[x, y] >= 128:
                o[x, y] = (230, 230, 220, 255) if y == 60 else (150, 150, 140, 255)
    path = os.path.join(TEX, f"entity/{'chest_boat' if chest else 'boat'}/{w}.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)

for w in ("stiltreed", "combwood", "ribwood", "veilpine"):
    paint(w, False)
    paint(w, True)

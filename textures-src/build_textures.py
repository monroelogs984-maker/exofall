"""Rebuilds Exofall's textures from their .px scripts (needs the pixelart skill's px.py).
Run from anywhere:  python3 textures-src/build_textures.py
Each folder here mirrors assets/monumental/textures/<folder>/; every <name>.px becomes <name>.png there.
Generator scripts (gen_*.py) run first: some write .px files, others (entity skins, worn armour) write PNGs directly."""
import glob
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PX = os.path.expanduser("~/.claude/skills/pixelart/scripts/px.py")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "exofall", "textures")

count = 0
for folder in sorted(d for d in os.listdir(HERE) if os.path.isdir(os.path.join(HERE, d))):
    src = os.path.join(HERE, folder)
    for gen in sorted(glob.glob(os.path.join(src, "**", "gen_*.py"), recursive=True)):
        subprocess.run([sys.executable, gen], cwd=os.path.dirname(gen), check=True, stdout=subprocess.DEVNULL)
    for script in sorted(glob.glob(os.path.join(src, "**", "*.px"), recursive=True)):
        rel = os.path.relpath(script, src)[:-3] + ".png"
        out = os.path.join(OUT, folder, rel)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        subprocess.run([sys.executable, PX, "build", script, out], check=True, stdout=subprocess.DEVNULL)
        count += 1
print(f"exofall textures rebuilt: {count}")

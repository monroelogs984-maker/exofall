#!/usr/bin/env python3
"""Resources for the warded (unbreakable) arena blocks registered in ModBlocks.WARDED: each copies its source
block's blockstate and models, so it looks identical. Also writes lang names and the tags that keep them
intact (wither/dragon immune) and let plants keep growing on them (dirt).

usage: tools/gen_warded.py   (from the repo root)
"""
import json, os, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
JAR = os.path.join(ROOT, ".gradle/repositories/ng_dummy_ng/net/minecraft/client/1.21.1/client-1.21.1-client-extra.jar")
NS = "exofall"
jar = zipfile.ZipFile(JAR)
VLANG = json.loads(jar.read("assets/minecraft/lang/en_us.json"))
OLANG_PATH = os.path.join(RES, f"assets/{NS}/lang/en_us.json")
lang = json.load(open(OLANG_PATH))

# warded id suffix -> source id (same list and order as ModBlocks)
WARDED = {
    "chitinstone_bricks": "exofall:chitinstone_bricks", "polished_chitinstone": "exofall:polished_chitinstone",
    "chitinstone_pillar": "exofall:chitinstone_pillar", "mud": "minecraft:mud", "packed_mud": "minecraft:packed_mud",
    "mud_bricks": "minecraft:mud_bricks", "honeycomb_block": "minecraft:honeycomb_block",
    "honey_block": "minecraft:honey_block", "sand": "minecraft:sand", "red_sand": "minecraft:red_sand",
    "bone_block": "minecraft:bone_block", "moss_block": "minecraft:moss_block", "rooted_dirt": "minecraft:rooted_dirt",
    "mossy_stone_bricks": "minecraft:mossy_stone_bricks", "veilpine_log": "exofall:veilpine_log",
    "veilpine_leaves": "exofall:veilpine_leaves", "verdant_froglight": "minecraft:verdant_froglight",
    "ochre_froglight": "minecraft:ochre_froglight", "pearlescent_froglight": "minecraft:pearlescent_froglight",
    "shroomlight": "minecraft:shroomlight",
}
# Vanilla sets these render types in code, so a copy needs them in its model.
RENDER = {"honey_block": "minecraft:translucent"}
DIRT = ["mud", "moss_block", "rooted_dirt"]

def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2); f.write("\n")

def blockstate(src):
    ns, name = src.split(":")
    if ns == "minecraft":
        return json.loads(jar.read(f"assets/minecraft/blockstates/{name}.json"))
    return json.load(open(os.path.join(RES, f"assets/{ns}/blockstates/{name}.json")))

def first_model(bs):
    v = next(iter(bs["variants"].values()))
    return (v[0] if isinstance(v, list) else v)["model"]

for name, src in WARDED.items():
    ours = f"warded_{name}"
    bs = blockstate(src)
    if name in RENDER:
        base = first_model(bs)
        write(f"assets/{NS}/models/block/{ours}.json", {"parent": base, "render_type": RENDER[name]})
        for k, v in bs["variants"].items():
            for e in (v if isinstance(v, list) else [v]):
                e["model"] = f"{NS}:block/{ours}"
    write(f"assets/{NS}/blockstates/{ours}.json", bs)
    write(f"assets/{NS}/models/item/{ours}.json", {"parent": first_model(bs)})
    sns, sname = src.split(":")
    srcname = VLANG.get(f"block.minecraft.{sname}") if sns == "minecraft" else lang.get(f"block.{NS}.{sname}")
    lang[f"block.{NS}.{ours}"] = f"Warded {srcname}"

def merge_tag(rel, vals):
    path = os.path.join(RES, rel)
    keep = json.load(open(path)).get("values", []) if os.path.exists(path) else []
    write(rel, {"replace": False, "values": sorted(set(keep + vals))})

ids = [f"{NS}:warded_{n}" for n in WARDED]
for t in ("wither_immune", "dragon_immune"):
    merge_tag(f"data/minecraft/tags/block/{t}.json", ids + [f"{NS}:sigil_seal", f"{NS}:sigil_socket"])
merge_tag("data/minecraft/tags/block/dirt.json", [f"{NS}:warded_{n}" for n in DIRT])
merge_tag("data/minecraft/tags/block/leaves.json", [f"{NS}:warded_veilpine_leaves"])
merge_tag("data/minecraft/tags/block/logs.json", [f"{NS}:warded_veilpine_log"])
with open(OLANG_PATH, "w") as f:
    json.dump(dict(sorted(lang.items())), f, indent=2); f.write("\n")
print(f"{len(WARDED)} warded blocks")

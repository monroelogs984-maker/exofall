#!/usr/bin/env python3
"""Generate Exofall's block resources (blockstates, models, item models, loot, recipes, tags, lang)
by cloning vanilla files for look-alike blocks. Textures stay vanilla: these are placeholders until
real textures exist. Re-run after adding blocks; it only writes files it owns (listed in .generated).

usage: tools/gen_placeholder_assets.py   (from the repo root)
"""
import json, os, re, sys, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
JAR = os.path.join(ROOT, ".gradle/repositories/ng_dummy_ng/net/minecraft/client/1.21.1/client-1.21.1-client-extra.jar")
NS = "exofall"

jar = zipfile.ZipFile(JAR)
def vjson(path):
    try:
        return json.loads(jar.read(path))
    except KeyError:
        return None

# Files from the last run: removed first so renamed/deleted blocks leave nothing behind.
GEN_LIST = os.path.join(ROOT, "tools/.generated")
OWNED_PREV = set(open(GEN_LIST).read().split()) if os.path.exists(GEN_LIST) else set()
for rel in OWNED_PREV:
    path = os.path.join(RES, rel)
    if os.path.exists(path):
        os.remove(path)

written = []
def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    written.append(rel)

# ---------------------------------------------------------------- block list
# our id -> (vanilla source, kind). kind adds a render type or picks custom loot.
CLONES = {}
FAMILY_WORDS = set()
LOG_TAGS = {}  # vanilla log tag -> our log tag

def wood(name, src, leaves_src=None, stem=False):
    log, wd = ("stem", "hyphae") if stem else ("log", "wood")
    m = {
        f"{name}_log": f"{src}_{log}", f"{name}_wood": f"{src}_{wd}",
        f"stripped_{name}_log": f"stripped_{src}_{log}", f"stripped_{name}_wood": f"stripped_{src}_{wd}",
        f"{name}_planks": f"{src}_planks", f"{name}_stairs": f"{src}_stairs", f"{name}_slab": f"{src}_slab",
        f"{name}_fence": f"{src}_fence", f"{name}_fence_gate": f"{src}_fence_gate",
        f"{name}_door": f"{src}_door", f"{name}_trapdoor": f"{src}_trapdoor",
        f"{name}_button": f"{src}_button", f"{name}_pressure_plate": f"{src}_pressure_plate",
    }
    for ours, v in m.items():
        kind = "door" if ours.endswith("_door") else "trapdoor" if ours.endswith("_trapdoor") else None
        CLONES[ours] = (v, kind)
    if leaves_src:
        CLONES[f"{name}_leaves"] = (leaves_src, "leaves")
        FAMILY_WORDS.add(leaves_src.split("_")[0])
    FAMILY_WORDS.add(src)
    LOG_TAGS[f"minecraft:{src}_{'stems' if stem else 'logs'}"] = f"{NS}:{name}_logs"

wood("stiltreed", "mangrove", "azalea_leaves")
wood("combwood", "acacia")
wood("ribwood", "birch")
wood("veilpine", "warped", "spruce_leaves", stem=True)

CLONES.update({
    "shellrock": ("obsidian", None),
    "chitinstone": ("tuff", None),
    "polished_chitinstone": ("polished_tuff", None),
    "chitinstone_bricks": ("tuff_bricks", None),
    "chitinstone_brick_stairs": ("tuff_brick_stairs", None),
    "chitinstone_brick_slab": ("tuff_brick_slab", None),
    "chitinstone_brick_wall": ("tuff_brick_wall", None),
    "polished_chitinstone_stairs": ("polished_tuff_stairs", None),
    "polished_chitinstone_slab": ("polished_tuff_slab", None),
    "chitinstone_pillar": ("quartz_pillar", "pillar"),
    "glowmarl": ("calcite", None),
    "royal_amber": ("amethyst_cluster", "crystal"),
})
FAMILY_WORDS |= {"tuff", "quartz", "obsidian", "calcite", "amethyst"}
TEXTURE_OVERRIDES = {"chitinstone_pillar": {"end": "minecraft:block/chiseled_tuff_top", "side": "minecraft:block/chiseled_tuff"}}

# Plants drawn as crosses (or flat pads) from vanilla textures.
CROSS = {
    "lanternpod": "minecraft:block/cave_vines_lit", "snaretongue": "minecraft:block/crimson_roots",
    "weepvine": "minecraft:block/twisting_vines_plant", "nectar_bell": "minecraft:block/lily_of_the_valley",
    "waxcap": "minecraft:block/brown_mushroom", "dunespine": "minecraft:block/crimson_fungus",
    "nightbloom": "minecraft:block/blue_orchid", "rattlegrass": "minecraft:block/dead_bush",
    "mistcap": "minecraft:block/warped_fungus", "glowmoss_curtain": "minecraft:block/cave_vines_plant_lit",
}
DOUBLE = {"sapcane": ("minecraft:block/tall_seagrass_bottom", "minecraft:block/tall_seagrass_top"),
          "umbrella_fern": ("minecraft:block/lilac_bottom", "minecraft:block/lilac_top")}
PADS = {"bladderpad": "minecraft:block/big_dripleaf_top", "driftmoss": "minecraft:block/moss_block"}
CLIMBABLE = ["weepvine", "glowmoss_curtain"]

IDMAP = {f"minecraft:{v}": f"{NS}:{k}" for k, (v, _) in CLONES.items()}
IDMAP.update(LOG_TAGS)
IDMAP.update({"#" + k: "#" + v for k, v in LOG_TAGS.items()})

def remap(obj):
    """Swap exact vanilla ids for ours anywhere in a JSON tree (loot, recipes)."""
    if isinstance(obj, dict):
        return {k: remap(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [remap(v) for v in obj]
    if isinstance(obj, str):
        return IDMAP.get(obj, obj)
    return obj

def leftover_family(obj):
    text = json.dumps(obj)
    return any(re.search(rf'"#?minecraft:[a-z_]*{w}[a-z_]*"', text) for w in FAMILY_WORDS)

# ---------------------------------------------------------------- clones
RENDER = {"door": "minecraft:cutout", "trapdoor": "minecraft:cutout", "leaves": "minecraft:cutout_mipped", "crystal": "minecraft:cutout"}

def rename_model_ref(ref, src, ours):
    name = ref.split("/", 1)[1] if "/" in ref else ref
    if ref.startswith("minecraft:block/") and (name == src or name.startswith(src + "_")):
        return f"{NS}:block/{ours}{name[len(src):]}"
    return None

def clone(ours, src, kind):
    bs = vjson(f"assets/minecraft/blockstates/{src}.json")
    if bs is None:
        sys.exit(f"no vanilla blockstate {src}")
    models = set()
    def walk_bs(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k == "model" and isinstance(v, str):
                    new = rename_model_ref(v, src, ours)
                    if new:
                        models.add(v.split("/", 1)[1]); o[k] = new
                else:
                    walk_bs(v)
        elif isinstance(o, list):
            for v in o: walk_bs(v)
    walk_bs(bs)
    write(f"assets/{NS}/blockstates/{ours}.json", bs)

    item = vjson(f"assets/minecraft/models/item/{src}.json") or {"parent": f"minecraft:block/{src}"}
    if "parent" in item:
        new = rename_model_ref(item["parent"], src, ours)
        if new:
            models.add(item["parent"].split("/", 1)[1]); item["parent"] = new
    write(f"assets/{NS}/models/item/{ours}.json", item)

    for name in sorted(models):
        mdl = vjson(f"assets/minecraft/models/block/{name}.json")
        if mdl is None:
            sys.exit(f"no vanilla model {name}")
        if "parent" in mdl:
            new = rename_model_ref(mdl["parent"], src, ours)
            if new: mdl["parent"] = new
        if ours in TEXTURE_OVERRIDES:
            mdl.setdefault("textures", {}).update(TEXTURE_OVERRIDES[ours])
        if kind in RENDER:
            mdl["render_type"] = RENDER[kind]
        write(f"assets/{NS}/models/block/{ours}{name[len(src):]}.json", mdl)

    if kind in ("leaves", "crystal"):
        write(f"data/{NS}/loot_table/blocks/{ours}.json", self_loot(ours, silk_or_shears=(kind == "leaves")))
    else:
        loot = vjson(f"data/minecraft/loot_table/blocks/{src}.json")
        loot = remap(loot)
        loot.pop("random_sequence", None)
        write(f"data/{NS}/loot_table/blocks/{ours}.json", loot)

def self_loot(ours, silk_or_shears=False, lower_half=False):
    entry = {"type": "minecraft:item", "name": f"{NS}:{ours}"}
    conds = [{"condition": "minecraft:survives_explosion"}]
    if silk_or_shears:
        conds = [{"condition": "minecraft:any_of", "terms": [
            {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
            {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
                {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}]
    if lower_half:
        conds.append({"condition": "minecraft:block_state_property", "block": f"{NS}:{ours}", "properties": {"half": "lower"}})
    return {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry], "conditions": conds}]}

for ours, (src, kind) in CLONES.items():
    clone(ours, src, kind)

# ---------------------------------------------------------------- plants
def cross_model(ours, tex, suffix=""):
    write(f"assets/{NS}/models/block/{ours}{suffix}.json",
          {"parent": "minecraft:block/cross", "textures": {"cross": tex}, "render_type": "minecraft:cutout"})

for ours, tex in CROSS.items():
    cross_model(ours, tex)
    write(f"assets/{NS}/blockstates/{ours}.json", {"variants": {"": {"model": f"{NS}:block/{ours}"}}})
    write(f"assets/{NS}/models/item/{ours}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    write(f"data/{NS}/loot_table/blocks/{ours}.json", self_loot(ours))

for ours, (bottom, top) in DOUBLE.items():
    cross_model(ours, bottom, "_bottom"); cross_model(ours, top, "_top")
    write(f"assets/{NS}/blockstates/{ours}.json", {"variants": {
        "half=lower": {"model": f"{NS}:block/{ours}_bottom"}, "half=upper": {"model": f"{NS}:block/{ours}_top"}}})
    write(f"assets/{NS}/models/item/{ours}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": top}})
    write(f"data/{NS}/loot_table/blocks/{ours}.json", self_loot(ours, lower_half=True))

for ours, tex in PADS.items():
    write(f"assets/{NS}/models/block/{ours}.json", {
        "ambientocclusion": False, "textures": {"particle": tex, "texture": tex}, "render_type": "minecraft:cutout",
        "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "faces": {
            "down": {"uv": [16, 16, 0, 0], "texture": "#texture"}, "up": {"uv": [16, 0, 0, 16], "texture": "#texture"}}}]})
    write(f"assets/{NS}/blockstates/{ours}.json", {"variants": {
        "": [{"model": f"{NS}:block/{ours}"}, {"model": f"{NS}:block/{ours}", "y": 90},
             {"model": f"{NS}:block/{ours}", "y": 180}, {"model": f"{NS}:block/{ours}", "y": 270}]}})
    write(f"assets/{NS}/models/item/{ours}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    write(f"data/{NS}/loot_table/blocks/{ours}.json", self_loot(ours))

# ---------------------------------------------------------------- recipes
recipes = [n for n in jar.namelist() if n.startswith("data/minecraft/recipe/") and n.endswith(".json")]
srcs = {v: k for k, (v, _) in CLONES.items()}
for n in recipes:
    r = json.loads(jar.read(n))
    res = r.get("result", {})
    rid = res.get("id") if isinstance(res, dict) else None
    if rid not in IDMAP:
        continue
    out = remap(r)
    if leftover_family(out):
        continue  # needs an ingredient we don't clone (e.g. quartz block, boats)
    name = os.path.basename(n)[:-5]
    for v in sorted(srcs, key=len, reverse=True):
        name = name.replace(v, srcs[v])
    write(f"data/{NS}/recipe/{name}.json", out)

# Pillar: 2 bricks stacked, plus stonecutter.
write(f"data/{NS}/recipe/chitinstone_pillar.json", {"type": "minecraft:crafting_shaped", "category": "building",
      "pattern": ["#", "#"], "key": {"#": {"item": f"{NS}:chitinstone_bricks"}}, "result": {"id": f"{NS}:chitinstone_pillar", "count": 2}})
for src in ("chitinstone", "polished_chitinstone", "chitinstone_bricks"):
    write(f"data/{NS}/recipe/chitinstone_pillar_from_{src}_stonecutting.json", {"type": "minecraft:stonecutting",
          "ingredient": {"item": f"{NS}:{src}"}, "result": {"id": f"{NS}:chitinstone_pillar", "count": 1}})

# ---------------------------------------------------------------- tags
TAG_ALLOW = re.compile(r"^(mineable/.*|logs|logs_that_burn|planks|wooden_.*|fence_gates|fences|walls|stairs|slabs|doors|"
                       r"trapdoors|buttons|pressure_plates|leaves|completes_find_tree_tutorial|dragon_immune|wither_immune|"
                       r"dampens_vibrations|sword_efficient|replaceable_by_trees|crystal_sound_blocks|stone_buttons)$")
skipped = set()
for reg in ("block", "item"):
    adds = {}
    for n in jar.namelist():
        pre = f"data/minecraft/tags/{reg}/"
        if not (n.startswith(pre) and n.endswith(".json")):
            continue
        tag = n[len(pre):-5]
        vals = json.loads(jar.read(n)).get("values", [])
        for ours, (src, _) in CLONES.items():
            if f"minecraft:{src}" in vals:
                if TAG_ALLOW.match(tag) and not any(w in tag for w in FAMILY_WORDS):
                    adds.setdefault(tag, []).append(f"{NS}:{ours}")
                else:
                    skipped.add(f"{reg}:{tag}")
    # Vanilla only nests per-wood tags into logs (e.g. #mangrove_logs), so add ours the same way.
    for t in ("logs", "logs_that_burn"):
        adds.setdefault(t, []).extend("#" + v for v in LOG_TAGS.values())
    if reg == "block":
        adds.setdefault("climbable", []).extend(f"{NS}:{c}" for c in CLIMBABLE)
        adds.setdefault("replaceable", []).extend(f"{NS}:{c}" for c in ("driftmoss",))
        adds.setdefault("sword_efficient", []).extend(f"{NS}:{c}" for c in list(CROSS) + list(DOUBLE) + list(PADS))
        adds.setdefault("mineable/pickaxe", []).append(f"{NS}:shellrock")
    for tag, vals in adds.items():
        existing = os.path.join(RES, f"data/minecraft/tags/{reg}/{tag}.json")
        keep = []
        if os.path.exists(existing):
            keep = json.load(open(existing)).get("values", [])  # hand-written tag: merge
        write(f"data/minecraft/tags/{reg}/{tag}.json", {"replace": False, "values": sorted(set(keep + vals))})
        if keep:
            written.pop()  # shares a file with hand-written entries: never delete it on the next run
    for vt, ours in LOG_TAGS.items():
        name = ours.split(":")[1]
        w = name[:-5]
        write(f"data/{NS}/tags/{reg}/{name}.json", {"values": [f"{NS}:{w}_log", f"{NS}:{w}_wood", f"{NS}:stripped_{w}_log", f"{NS}:stripped_{w}_wood"]})
    write(f"data/{NS}/tags/{reg}/alien_soil.json" if reg == "block" else f"data/{NS}/tags/item/alien_soil.json",
          {"values": [f"{NS}:glowmarl"]})
print("tags skipped (not allowlisted):", ", ".join(sorted(skipped)) or "none")

# ---------------------------------------------------------------- lang
lang_path = os.path.join(RES, f"assets/{NS}/lang/en_us.json")
lang = json.load(open(lang_path)) if os.path.exists(lang_path) else {}
for k in [k for k in lang if k.startswith(f"block.{NS}.")]:
    del lang[k]
for ours in list(CLONES) + list(CROSS) + list(DOUBLE) + list(PADS):
    lang[f"block.{NS}.{ours}"] = " ".join(w.capitalize() for w in ours.split("_"))
with open(lang_path, "w") as f:
    json.dump(dict(sorted(lang.items())), f, indent=2); f.write("\n")

with open(GEN_LIST, "w") as f:
    f.write("\n".join(sorted(written)) + "\n")
print(f"wrote {len(written)} files")

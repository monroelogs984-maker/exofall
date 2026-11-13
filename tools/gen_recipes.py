#!/usr/bin/env python3
"""Shellrock Crafting Table (5x5) recipes for Exofall's meteor gear, from mob drops and boss drops.
Writes data/exofall/recipe/shellrock/<item>.json (deletes old ones first). Edit the tables below and re-run.

Armour: P = the meteor's main dungeon-mob drop, S = its other dungeon-mob drop, B = the boss drop, K = a meteor block.
"""
import json, os, glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/exofall/recipe/shellrock")

ARMOUR_SHAPES = {
    "helmet": ["KPPPK",
               "PSBSP",
               "K   K"],
    "chestplate": ["K   K",
                   "PKBKP",
                   "SPKPS",
                   "KPSPK",
                   " KPK "],
    "leggings": ["PKBKP",
                 "PS SP",
                 "K   K",
                 "K   K"],
    "boots": ["K   K",
              "PS SP"],
}
# bug armour for tamed pets: no boss drop (pets come before each meteor's boss)
BUG_ARMOUR_SHAPE = ["K   K",
                    "PSPSP",
                    "PPKPP",
                    " P P "]
SETS = {
    "mireweave":           dict(P="exofall:bogsilk_spool",  S="exofall:leech_fang",       B="exofall:bog_eye",          K="exofall:sapcane"),
    "crystallized_honey":  dict(P="exofall:propolis_gland", S="exofall:honeyguard_plate", B="exofall:royal_honey_drop", K="exofall:royal_amber"),
    "reinforced_carapace": dict(P="exofall:pouncer_shell",  S="exofall:assassin_hook",    B="exofall:mantis_eye",       K="exofall:shellrock"),
    "ancientstone":        dict(P="exofall:tomb_stinger",   S="exofall:antlion_mandible", B="exofall:terror_leg",       K="exofall:polished_chitinstone"),
}

OTHER = {
    # legendary weapons: each meteor's boss drops + its dungeon-mob drops + its wood for the haft
    "glasswing_scythe": (["SSSE ",
                          "S  LF",
                          "  C  ",
                          " L   ",
                          "L    "],
                         dict(S="exofall:leech_fang", E="exofall:bog_eye", F="exofall:lethal_proboscis",
                              L="exofall:stiltreed_log", C="exofall:bogsilk_spool")),
    "royal_grandhammer": (["GWRWG",
                           "GGPGG",
                           "  L  ",
                           "  L  ",
                           "  L  "],
                          dict(G="exofall:honeyguard_plate", W="exofall:crystallized_wing", R="exofall:royal_honey_drop",
                               P="exofall:propolis_gland", L="exofall:combwood_log")),
    "quadblade_of_the_sands": (["M   M",
                                " TVT ",
                                "  L  ",
                                " TLT ",
                                "M   M"],
                               dict(M="exofall:antlion_mandible", T="exofall:terror_leg", V="exofall:venomjaw", L="exofall:ribwood_log")),
    "serrated_longsword": (["   HH",
                            "  HCH",
                            " SEH ",
                            " LS  ",
                            "L    "],
                           dict(H="exofall:assassin_hook", C="exofall:mantis_greatclaw", E="exofall:mantis_eye",
                                S="exofall:pouncer_shell", L="exofall:veilpine_log")),
    # baubles
    "divebell_locket": ([" C C ",
                         "C   C",
                         " GBG ",
                         " BBB ",
                         "  G  "],
                        dict(C="exofall:bogsilk_spool", B="exofall:bubble_sac", G="minecraft:gold_ingot")),
    "ember_beetle_brooch": ([" AGA ",
                             "GMMMG",
                             " AGA "],
                            dict(A="exofall:antlion_mandible", G="minecraft:gold_ingot", M="minecraft:magma_cream")),
    "leafcutter_charm": ([" LHL ",
                          "LGEGL",
                          " LHL "],
                         dict(L="exofall:veilpine_leaves", H="exofall:assassin_hook", G="minecraft:gold_ingot", E="minecraft:emerald")),
    "stinger_fang_ring": ([" GGG ",
                           "G   G",
                           "G R G",
                           " GPG "],
                          dict(G="minecraft:gold_ingot", R="exofall:propolis_gland", P="exofall:honeyguard_plate")),
    "nursemaids_wax_charm": ([" HHH ",
                              "HRDRH",
                              " HHH "],
                             dict(H="minecraft:honeycomb", R="exofall:propolis_gland", D="exofall:royal_honey_drop")),
    "lantern_moth_pendant": ([" C C ",
                              "  C  ",
                              " GLG ",
                              " LML ",
                              "  G  "],
                             dict(C="minecraft:chain", G="minecraft:gold_ingot", L="exofall:lanternpod", M="exofall:glowmarl")),
}

def trim(pattern):
    """Drop empty outer rows and columns: the table trims the grid the same way, so untrimmed patterns never match."""
    rows = [r for r in pattern]
    while rows and not rows[0].strip(): rows.pop(0)
    while rows and not rows[-1].strip(): rows.pop()
    while all(r[0] == " " for r in rows): rows = [r[1:] for r in rows]
    while all(r[-1] == " " for r in rows): rows = [r[:-1] for r in rows]
    return rows

def recipe(pattern, key, result, group):
    pattern = trim(pattern)
    used = {c for row in pattern for c in row if c != " "}
    assert used == set(key), (result, used, set(key))
    assert all(len(r) == len(pattern[0]) <= 5 for r in pattern) and len(pattern) <= 5, result
    return {"type": "exofall:shellrock_crafting", "group": group, "pattern": pattern,
            "key": {c: {"item": key[c]} for c in sorted(key)}, "result": {"id": f"exofall:{result}", "count": 1}}

os.makedirs(OUT, exist_ok=True)
for f in glob.glob(os.path.join(OUT, "*.json")):
    os.remove(f)
n = 0
for s, mats in SETS.items():
    for piece, shape in ARMOUR_SHAPES.items():
        key = {c: mats[c] for c in {c for row in shape for c in row if c != " "}}
        json.dump(recipe(shape, key, f"{s}_{piece}", s), open(os.path.join(OUT, f"{s}_{piece}.json"), "w"), indent=2)
        n += 1
    key = {c: mats[c] for c in {c for row in BUG_ARMOUR_SHAPE for c in row if c != " "}}
    json.dump(recipe(BUG_ARMOUR_SHAPE, key, f"{s}_bug_armour", "bug_armour"), open(os.path.join(OUT, f"{s}_bug_armour.json"), "w"), indent=2)
    n += 1
for item, (shape, key) in OTHER.items():
    json.dump(recipe(shape, key, item, "meteor_gear"), open(os.path.join(OUT, f"{item}.json"), "w"), indent=2)
    n += 1
print(f"{n} shellrock recipes written")

#!/usr/bin/env python3
"""Generates Exofall mob models (GeckoLib geo.json), textures, glowmasks and animations.

    python3 tools/mobs/gen_mobs.py [mob ...]        # all mobs, or only the named ones
    python3 tools/mobs/preview.py <geo> <png> <out.png> [scale]   # front / side / 3/4 / top contact sheet

Body plans live in mobs_templates.py, one module per meteor (mobs_swamp/hive/desert/rainforest),
shared parts (palette, decor, bone chains, gait, grounding) in mobparts.py, the builder in mobkit.py.
Coordinates are Blockbench/Bedrock pixels: model faces north (-Z), 16px = 1 block. Legged models are
grounded automatically (lowest rest-pose point at y=0).
"""
import importlib
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

MODULES = ['mobs_swamp', 'mobs_hive', 'mobs_desert', 'mobs_rainforest']

if __name__ == '__main__':
    only = set(sys.argv[1:])
    for m in MODULES:
        try:
            mod = importlib.import_module(m)
        except ModuleNotFoundError as e:
            if e.name == m:
                continue
            raise
        for fn in mod.ALL:
            if not only or fn.__name__ in only:
                fn()

"""Boat item icons: one hand-drawn hull (and chest) grid, stamped with each wood's bark/inner ramps."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from texlib import write_px_multi

HERE = os.path.dirname(os.path.abspath(__file__))
BOAT = """
................
................
................
................
................
................
............PP..
...........PP...
o.........s....o
or.......s....ro
orrrrrrrsrrrrrro
.ohhhhhhhhhhhho.
.oHHHHHHHHHHHHo.
..ohhhhhhhhhho..
...oooooooooo...
................"""
CHEST = """
................
................
................
................
...kkkkk........
...kCCCk....PP..
...kcLck...PP...
...kcccc...s....
o..kcccc..s....o
or.kcccc.s....ro
orrrrrrrsrrrrrro
.ohhhhhhhhhhhho.
.oHHHHHHHHHHHHo.
..ohhhhhhhhhho..
...oooooooooo...
................"""
def cells(w, art):
    bark, inner = f"{w}_bark", f"{w}_inner"
    rows = art.strip("\n").split("\n")
    g = []
    for y, row in enumerate(rows):
        out = []
        for x, ch in enumerate(row):
            lit = 1 if x < 6 else (-1 if x > 10 else 0)   # light from the left
            m = {"o": (bark, 1), "r": (inner, 8 + max(0, lit)), "h": (inner, 5 + lit), "H": (inner, 3 + lit),
                 "s": (inner, 7), "P": (inner, 6), "k": (bark, 2), "c": (bark, 5 + lit), "C": (bark, 7), "L": (inner, 9)}.get(ch)
            out.append(m)
        g.append(out)
    return g

for w in ("stiltreed", "combwood", "ribwood", "veilpine"):
    rs = {f"{w}_bark": 10, f"{w}_inner": 10}
    write_px_multi(os.path.join(HERE, f"{w}_boat.px"), f"{w} boat item", rs, cells(w, BOAT))
    write_px_multi(os.path.join(HERE, f"{w}_chest_boat.px"), f"{w} chest boat item", rs, cells(w, CHEST))

#!/usr/bin/env python3
"""Shellrock Crafting Table screen background (256x256 sheet, panel 176x206), vanilla container style.
Slot positions mirror ShellrockTableMenu (GRID_X/Y, RESULT_X/Y, INV_Y)."""
import os
from PIL import Image

GRID_X, GRID_Y, RESULT_X, RESULT_Y, INV_Y = 12, 18, 142, 54, 124
W, H = 176, 206
BG, HI, SH, OUT = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
SLOT_D, SLOT_L, SLOT_F = (55, 55, 55), (255, 255, 255), (139, 139, 139)

im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
p = im.load()

def rect(x, y, w, h, c):
    for j in range(y, y + h):
        for i in range(x, x + w):
            p[i, j] = c + (255,)

# panel: black outline with clipped corners, 2 px white top-left bevel, 2 px grey bottom-right bevel
rect(1, 1, W - 2, H - 2, BG)
for i in range(2, W - 2):
    p[i, 0] = OUT + (255,); p[i, H - 1] = OUT + (255,)
for j in range(2, H - 2):
    p[0, j] = OUT + (255,); p[W - 1, j] = OUT + (255,)
for c in [(1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2)]:
    p[c] = OUT + (255,)
rect(2, 1, W - 5, 2, HI); rect(1, 2, 2, H - 5, HI)
rect(3, H - 3, W - 5, 2, SH); rect(W - 3, 3, 2, H - 5, SH)
p[W - 3, 2] = BG + (255,); p[2, H - 3] = BG + (255,)
p[3, 3] = HI + (255,); p[W - 4, H - 4] = SH + (255,)

def slot(x, y, size=18):
    rect(x, y, size, size, SLOT_F)
    rect(x, y, size - 1, 1, SLOT_D); rect(x, y, 1, size - 1, SLOT_D)
    rect(x + 1, y + size - 1, size - 1, 1, SLOT_L); rect(x + size - 1, y + 1, 1, size - 1, SLOT_L)

for gy in range(5):
    for gx in range(5):
        slot(GRID_X - 1 + gx * 18, GRID_Y - 1 + gy * 18)
slot(RESULT_X - 5, RESULT_Y - 5, 26)
for r in range(3):
    for c in range(9):
        slot(7 + c * 18, INV_Y - 1 + r * 18)
for c in range(9):
    slot(7 + c * 18, INV_Y + 57)

# arrow between grid and result (vanilla grey, 22x15)
ax, ay = 108, RESULT_Y + 1
for j in range(15):
    d = abs(j - 7)
    if 4 <= j <= 10:
        rect(ax, ay + j, 15, 1, SLOT_F)
    rect(ax + 14, ay + j, max(0, 8 - d), 1, SLOT_F)

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src/main/resources/assets/exofall/textures/gui/container/shellrock_crafting_table.png")
im.save(os.path.normpath(out))
print("wrote", os.path.normpath(out))

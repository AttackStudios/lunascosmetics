#!/usr/bin/env python3
"""
Builds the Cherry Cat theme: recolours vanilla GUI sprites into cherry-blossom pinks.

Grey UI (buttons, sliders, hotbar, inventories, menu backgrounds) goes through a
gradient map, so every bevel and border keeps its contrast, just in pink. Only
near-neutral pixels are mapped, which leaves item icons, furnace flames, arrows etc.
alone. Hearts and the XP bar are hue-shifted instead so their shading survives.

Output lands under assets/lunascosmetics, plus theme_index.txt which the mod reads
to know which vanilla ids have a themed twin.

usage: theme_sprites.py <extracted assets/minecraft/textures/gui dir> <mod resources dir>
"""
import colorsys
import os
import shutil
import sys

from PIL import Image

SRC = sys.argv[1]
RES = sys.argv[2]
OUT = os.path.join(RES, "assets/lunascosmetics")

# Deep plum -> rose -> blush -> petal white.
RAMP = [
    (0.00, (0x1c, 0x08, 0x14)),
    (0.20, (0x4a, 0x1a, 0x34)),
    (0.38, (0xa8, 0x48, 0x76)),
    (0.52, (0xdc, 0x7c, 0xa6)),
    (0.66, (0xf4, 0xae, 0xc8)),
    (0.80, (0xf8, 0xc3, 0xd6)),
    (0.92, (0xff, 0xe3, 0xed)),
    (1.00, (0xff, 0xf7, 0xfa)),
]

# Big flat panels (inventories) read loud in full-strength pink; they get a softer ramp.
SOFT_RAMP = [
    (0.00, (0x24, 0x10, 0x1c)),
    (0.30, (0x6e, 0x3a, 0x55)),
    (0.55, (0xb8, 0x7c, 0x99)),
    (0.70, (0xe2, 0xaf, 0xc4)),
    (0.78, (0xf3, 0xcd, 0xda)),
    (0.90, (0xfd, 0xe9, 0xf0)),
    (1.00, (0xff, 0xfa, 0xfc)),
]


def ramp(stops, l):
    for i in range(len(stops) - 1):
        a, ca = stops[i]
        b, cb = stops[i + 1]
        if l <= b:
            f = (l - a) / (b - a) if b > a else 0
            return tuple(round(ca[k] + (cb[k] - ca[k]) * f) for k in range(3))
    return stops[-1][1]


def gradient_map(img, stops, sat_limit=0.14):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if s > sat_limit and (max(r, g, b) - min(r, g, b)) > 24 and max(r, g, b) >= 64:
                continue  # coloured detail: keep it
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            nr, ng, nb = ramp(stops, lum)
            px[x, y] = (nr, ng, nb, a)
    return img


def hue_to_pink(img, target_hue=0.93, sat_boost=0.9):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if s < 0.15:
                continue
            l = min(1.0, l * 1.08 + 0.04)
            nr, ng, nb = colorsys.hls_to_rgb(target_hue, l, min(1.0, s * sat_boost))
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return img


index = []


def emit_sprite(rel, img):
    """rel: path under textures/gui/sprites without .png, e.g. widget/button"""
    dst = os.path.join(OUT, "textures/gui/sprites/theme", rel + ".png")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    img.save(dst)
    meta = os.path.join(SRC, "sprites", rel + ".png.mcmeta")
    if os.path.exists(meta):
        shutil.copy(meta, dst + ".mcmeta")
    index.append("sprite " + rel)


def emit_texture(rel, img):
    """rel: path under textures/gui with .png, e.g. container/inventory.png"""
    dst = os.path.join(OUT, "textures/theme/gui", rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    img.save(dst)
    index.append("texture " + rel)


def sprite(rel):
    return Image.open(os.path.join(SRC, "sprites", rel + ".png"))


# ---- widgets ---------------------------------------------------------------------------
WIDGETS = [
    "button", "button_highlighted", "button_disabled",
    "slider", "slider_highlighted", "slider_handle", "slider_handle_highlighted",
    "text_field", "text_field_highlighted",
    "scroller", "scroller_background",
    "tab", "tab_highlighted", "tab_selected", "tab_selected_highlighted",
    "checkbox", "checkbox_highlighted", "checkbox_selected", "checkbox_selected_highlighted",
    "slot_frame",
    "locked_button", "locked_button_highlighted", "locked_button_disabled",
    "unlocked_button", "unlocked_button_highlighted", "unlocked_button_disabled",
    "cross_button", "cross_button_highlighted",
    "page_forward", "page_forward_highlighted", "page_backward", "page_backward_highlighted",
]
for w in WIDGETS:
    rel = "widget/" + w
    if os.path.exists(os.path.join(SRC, "sprites", rel + ".png")):
        emit_sprite(rel, gradient_map(sprite(rel), RAMP, sat_limit=0.25))

# ---- HUD -------------------------------------------------------------------------------
for h in ["hotbar", "hotbar_selection", "hotbar_offhand_left", "hotbar_offhand_right",
          "experience_bar_background", "jump_bar_background", "effect_background",
          "effect_background_ambient", "crosshair"]:
    rel = "hud/" + h
    if os.path.exists(os.path.join(SRC, "sprites", rel + ".png")):
        emit_sprite(rel, gradient_map(sprite(rel), RAMP, sat_limit=0.6))

for h in ["experience_bar_progress", "jump_bar_progress"]:
    emit_sprite("hud/" + h, hue_to_pink(sprite("hud/" + h)))

HEART_DIR = os.path.join(SRC, "sprites/hud/heart")
for f in sorted(os.listdir(HEART_DIR)):
    if not f.endswith(".png"):
        continue
    name = f[:-4]
    # Plain red hearts only; poisoned/withered/frozen/absorbing keep their meaning.
    if name in ("full", "half", "full_blinking", "half_blinking",
                "hardcore_full", "hardcore_half", "hardcore_full_blinking", "hardcore_half_blinking"):
        emit_sprite("hud/heart/" + name, hue_to_pink(sprite("hud/heart/" + name), 0.94, 0.85))

# ---- inventory / container backgrounds (plain textures, not atlas sprites) --------------
CONTAINERS = ["inventory.png", "generic_54.png", "crafting_table.png", "furnace.png",
              "blast_furnace.png", "smoker.png", "dispenser.png", "hopper.png",
              "shulker_box.png", "anvil.png", "enchanting_table.png", "brewing_stand.png",
              "grindstone.png", "cartography_table.png", "stonecutter.png", "loom.png",
              "smithing.png", "beacon.png", "villager.png", "crafter.png",
              "creative_inventory/tab_items.png", "creative_inventory/tab_inventory.png",
              "creative_inventory/tab_item_search.png"]
for c in CONTAINERS:
    p = os.path.join(SRC, "container", c)
    if os.path.exists(p):
        emit_texture("container/" + c, gradient_map(Image.open(p), SOFT_RAMP, sat_limit=0.2))

# Creative tabs + slot highlight are atlas sprites in 1.21.11.
CRE = os.path.join(SRC, "sprites/container/creative_inventory")
if os.path.isdir(CRE):
    for f in sorted(os.listdir(CRE)):
        if f.endswith(".png") and f.startswith("tab_"):
            rel = "container/creative_inventory/" + f[:-4]
            emit_sprite(rel, gradient_map(sprite(rel), SOFT_RAMP, sat_limit=0.2))

# ---- menu backgrounds ------------------------------------------------------------------
for t in ["menu_background.png", "menu_list_background.png", "header_separator.png",
          "footer_separator.png", "tab_header_background.png",
          "inworld_menu_background.png", "inworld_menu_list_background.png",
          "inworld_header_separator.png", "inworld_footer_separator.png"]:
    p = os.path.join(SRC, t)
    if os.path.exists(p):
        emit_texture(t, gradient_map(Image.open(p), RAMP, sat_limit=1.0))

with open(os.path.join(OUT, "theme_index.txt"), "w") as fh:
    fh.write("\n".join(index) + "\n")
print(f"themed {len(index)} textures")

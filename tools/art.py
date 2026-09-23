#!/usr/bin/env python3
"""
Paints every texture Luna's Cosmetics ships (except the recoloured vanilla theme
sprites, see theme_sprites.py).

  pets:  cats (10 coats) + Mini Moosh, each with an eyes-closed twin for blinking
         and, where it has one, a fullbright "glow" layer
  hats:  sakura crown, kitty ears (3 colours), star halo, petal wings - their model
         JSON is generated here too, with auto-packed UVs
  gui:   the cat menu button, title-screen cat, petals, button ears, paw cursor, icons

Pets are painted with 3D-aware pattern functions: every texel knows where it sits on
the model, so stripes wrap around the body and calico patches cross face seams.

usage: art.py <mod resources dir>
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import lunamodel as lm  # noqa: E402

RES = sys.argv[1]
A = os.path.join(RES, "assets/lunascosmetics")
MODELS = os.path.join(A, "models/cosmetic")


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def mix(c1, c2, f):
    return tuple(round(c1[i] + (c2[i] - c1[i]) * f) for i in range(3)) + (c1[3] if len(c1) > 3 else 255,)


def shade(c, f):
    return tuple(max(0, min(255, round(c[i] * f))) for i in range(3)) + (c[3],)


def save(img, rel):
    p = os.path.join(A, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def hash01(*k):
    h = 2166136261
    for v in k:
        for ch in str(v):
            h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return (h % 100000) / 100000


def vnoise(x, y, z, seed=0, scale=3.0):
    """cheap smooth 3D value noise"""
    x, y, z = x / scale, y / scale, z / scale
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    xf, yf, zf = x - xi, y - yi, z - zi

    def s(t):
        return t * t * (3 - 2 * t)

    def r(a, b, c):
        return hash01(seed, a, b, c)

    acc = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (s(xf) if dx else 1 - s(xf)) * (s(yf) if dy else 1 - s(yf)) * (s(zf) if dz else 1 - s(zf))
                acc += w * r(xi + dx, yi + dy, zi + dz)
    return acc


# =========================================================================================
# CATS
# =========================================================================================
CAT = lm.load(os.path.join(MODELS, "cat.json"))
CAT_TEXELS = lm.texels(CAT)

CATS = {
    "snowball": dict(fur="#f6f2f4", shade="#e0d7de", belly="#ffffff", muzzle="#ffffff",
                     eye="#c9476a", pupil="#5e1a2e", nose="#f39ab5", ear="#f7a9c1", blush=True),
    "luna": dict(fur="#26222f", shade="#1a1722", belly="#2e2a38", muzzle="#302b3a",
                 eye="#f7ca46", pupil="#6b4a05", nose="#7a6480", ear="#6a5470", glow_eyes=True),
    "stargazer": dict(fur="#1f2b5e", shade="#18224c", belly="#283a7a", muzzle="#34468e",
                      eye="#a4ecff", pupil="#1b5a8c", nose="#d7b4ff", ear="#6c5cb4", glow_eyes=True),
    "sakura": dict(fur="#f8c7d7", shade="#eeaec3", belly="#fff1f6", muzzle="#fff6f9",
                   eye="#c5385f", pupil="#5c1030", nose="#e2587f", ear="#ff9bb9", blush=True),
    "marmalade": dict(fur="#eb9440", shade="#cf7630", belly="#f8dbb3", muzzle="#f9e6cb",
                      eye="#86bd36", pupil="#2d4a0c", nose="#e37d7d", ear="#f2a59c", stripe="#b85f20"),
    "tuxedo": dict(fur="#1e1e25", shade="#15151a", belly="#f7f7f7", muzzle="#f7f7f7",
                   eye="#7fcd45", pupil="#24400c", nose="#f0a2b2", ear="#e59aa8"),
    "calico": dict(fur="#fbf7f1", shade="#ece5dc", belly="#ffffff", muzzle="#ffffff",
                   eye="#d7a531", pupil="#5a3e06", nose="#f0a0a8", ear="#f4b0b4"),
    "siamese": dict(fur="#f3e7d0", shade="#e6d5b8", belly="#fbf3e4", muzzle="#4b372a",
                    eye="#5cb8f3", pupil="#1b4a80", nose="#3a2a22", ear="#4b372a", point="#4b372a"),
    "smokey": dict(fur="#8f929a", shade="#767980", belly="#c8cad0", muzzle="#d3d5da",
                   eye="#72d48d", pupil="#1d5a2c", nose="#c48c95", ear="#c99aa2", stripe="#5e6169"),
    "cocoa": dict(fur="#7b4f33", shade="#613c26", belly="#ba8b67", muzzle="#cba181",
                  eye="#f2b232", pupil="#6a4a08", nose="#4b2b1d", ear="#c28a7a"),
}


def cat_base(name, spec, t):
    fur, sh, belly, muzzle = (hexc(spec[k]) for k in ("fur", "shade", "belly", "muzzle"))
    x, y, z = t.pos
    part = t.part
    # fur grain: tiny per-texel variation so flat faces don't look plastic
    grain = 0.955 + 0.09 * hash01(name, t.tx, t.ty)
    c = fur
    # darker toward the underside of each cube, lighter on top
    if t.face == "down":
        c = sh
    elif t.face not in ("up",) and t.fy >= t.fh - 1 and t.fh >= 3:
        c = mix(fur, sh, 0.55)
    # belly + chest
    if part == "body" and (t.face == "down" or (t.face == "north" and t.fy >= 1)):
        c = belly
    if part == "snout":
        c = muzzle
    if part.startswith("leg"):
        # the far end of the leg is the paw
        if t.local[1] >= 3.5 or t.face == "down":
            c = mix(fur, belly, 0.6) if name not in ("tuxedo",) else belly
    if part == "tail_tip" and t.local[2] >= 3.5:
        c = mix(fur, sh, 0.4)

    # ---- coat patterns ---------------------------------------------------------------
    if "stripe" in spec and part in ("body", "tail", "tail_tip", "head") and t.face != "down":
        st = hexc(spec["stripe"])
        if part == "body":
            band = (z + 10) % 3.0
            if band < 1.0 and not (t.face == "north"):
                c = st
        elif part.startswith("tail"):
            if (t.local[2] % 2.0) < 1.0:
                c = st
        elif part == "head" and t.face == "up":
            # the classic tabby "M" on the forehead
            if t.fy >= 2 and t.fx in (1, 2, 3, 4) and ((t.fx in (1, 4) and t.fy >= 2) or (t.fx in (2, 3) and t.fy == 3)):
                c = st
        elif part == "head" and t.face in ("west", "east") and t.fy == 1 and t.fx in (1, 3):
            c = st
    if name == "tuxedo":
        if part == "head" and t.face == "north" and t.fy >= 3:
            c = belly
        if part == "body" and t.face in ("west", "east") and t.fy >= 3 and t.pos[2] < 1:
            c = belly
    if name == "calico" and t.face != "down" and part not in ("snout",):
        n1 = vnoise(x, y, z, seed=11, scale=2.6)
        n2 = vnoise(x, y, z, seed=23, scale=2.4)
        if n1 > 0.62:
            c = hexc("#e8913a")
        elif n2 > 0.64:
            c = hexc("#2c2429")
    if name == "siamese":
        pt = hexc(spec["point"])
        if part.startswith("ear") or part.startswith("tail") or part == "snout":
            c = pt
        elif part.startswith("leg"):
            f = min(1.0, max(0.0, (t.local[1] - 1.0) / 3.0))
            c = mix(c, pt, f)
        elif part == "head" and t.face == "north":
            # mask fades out from the snout up
            f = max(0.0, (t.fy - 0.5) / 3.5)
            c = mix(c, pt, min(1.0, f + (0.35 if t.fx in (2, 3) else 0)))
    if name == "sakura" and part == "body" and t.face in ("up", "west", "east"):
        # little cherry blossoms scattered over the back
        if blossom_at(t, seed=5):
            c = hexc("#ff7aa6") if blossom_at(t, seed=5) == 1 else hexc("#fff4c4")
    if name == "stargazer" and t.face != "down":
        r = hash01("star", t.tx, t.ty)
        if r > 0.93:
            c = hexc("#fff4c0")
        elif r > 0.88:
            c = hexc("#8fb8ff")
    if name == "luna" and part == "head" and t.face == "up":
        if crescent(t):
            c = hexc("#f7cf55")
    return shade(c, grain) if c[3] else c


BLOSSOMS = [(0.5, -4, -1.0), (-1.5, -4, 2.5), (1.5, -4, 4.8), (-3, -2.5, 0.5), (3, -2.5, -1.5), (3, -2, 3.5), (-3, -2, -2.8)]


def blossom_at(t, seed=0):
    """1 = petal, 2 = centre, 0 = none. Blossoms are 3D points; a texel near one lights up."""
    for bx, by, bz in BLOSSOMS:
        dx, dy, dz = t.pos[0] - bx, t.pos[1] - by, t.pos[2] - bz
        d2 = dx * dx + dy * dy + dz * dz
        if d2 < 0.3:
            return 2
        if d2 < 1.4:
            return 1
    return 0


def crescent(t):
    # head top is 6 wide x 5 deep, rows run back -> front; crescent near the front
    shape = {(2, 2), (3, 2), (1, 3), (4, 3), (2, 4), (3, 4)}
    shape = {(1, 2), (2, 1), (3, 1), (1, 3), (2, 4), (3, 4)}
    return (t.fx, t.fy) in shape


def cat_face(name, spec, img, blink=False, glow=None):
    """eyes, nose, ears, blush: painted straight onto the known face regions"""
    eye, pupil, nose, ear = (hexc(spec[k]) for k in ("eye", "pupil", "nose", "ear"))
    fur = hexc(spec["fur"])
    px = img.load()
    hx, hy = 35, 5   # head north face origin (uv 30,0 ; d=5)
    lid = shade(fur if name != "siamese" else hexc("#6a5040"), 0.8)
    for ex in (1, 4):
        if blink:
            px[hx + ex, hy + 1] = px[hx + ex, hy + 1]
            px[hx + ex, hy + 2] = shade(hexc(spec["pupil"]), 0.9)
            if name in ("luna", "stargazer", "tuxedo", "cocoa"):
                px[hx + ex, hy + 2] = hexc("#0e0c12")
        else:
            px[hx + ex, hy + 1] = eye
            px[hx + ex, hy + 2] = pupil
            if glow is not None and spec.get("glow_eyes"):
                glow[hx + ex, hy + 1] = eye
                glow[hx + ex, hy + 2] = shade(eye, 0.7)
    if spec.get("blush"):
        for bx in (0, 5):
            px[hx + bx, hy + 3] = mix(px[hx + bx, hy + 3], hexc("#ff8fb0"), 0.55)
    # snout north face: uv (52,0) d=1 -> (53,1) 4x2
    sx, sy = 53, 1
    px[sx + 1, sy] = nose
    px[sx + 2, sy] = nose
    px[sx + 1, sy + 1] = shade(hexc(spec["muzzle"]), 0.78)
    px[sx + 2, sy + 1] = shade(hexc(spec["muzzle"]), 0.78)
    # whisker dots on the snout sides
    for wx, wy in ((52, 1), (57, 1)):
        px[wx, wy + 1] = shade(hexc(spec["muzzle"]), 0.85)
    # ear inner: ear_left north face at (53,5) 2x2 ; inner side is +x (col 1)
    for (ox, inner) in ((53, 1), (59, 0)):
        for yy in (0, 1):
            px[ox + inner, 5 + yy] = ear if yy == 1 else mix(ear, fur, 0.35)


def paint_cat(name, spec):
    base = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    bp = base.load()
    gp = glow.load()
    for t in CAT_TEXELS:
        bp[t.tx, t.ty] = cat_base(name, spec, t)
        if name == "stargazer":
            c = bp[t.tx, t.ty]
            if c[:3] == shade(hexc("#fff4c0"), 1)[:3] or (hash01("star", t.tx, t.ty) > 0.93 and t.face != "down"):
                gp[t.tx, t.ty] = hexc("#fff4c0")
        if name == "luna" and t.part == "head" and t.face == "up" and crescent(t):
            gp[t.tx, t.ty] = hexc("#ffd966")
    blink = base.copy()
    cat_face(name, spec, base, blink=False, glow=gp)
    cat_face(name, spec, blink, blink=True)
    save(base, f"textures/cosmetic/cat/{name}.png")
    save(blink, f"textures/cosmetic/cat/{name}_blink.png")
    has_glow = glow.getbbox() is not None
    if has_glow:
        save(glow, f"textures/cosmetic/cat/{name}_glow.png")
    return base, has_glow


# =========================================================================================
# MINI MOOSH
# =========================================================================================
MOOSH = lm.load(os.path.join(MODELS, "moosh.json"))


def paint_moosh():
    pink, pink_sh, white = hexc("#f4a7ba"), hexc("#e48aa2"), hexc("#fff3f6")
    snout, nostril = hexc("#e57896"), hexc("#9c3f5a")
    hoof = hexc("#b85a76")
    leaf, leaf_dk, stem = hexc("#86dc5c"), hexc("#4f9d31"), hexc("#5aa83a")
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    for t in lm.texels(MOOSH):
        g = 0.96 + 0.07 * hash01("moo", t.tx, t.ty)
        c = pink
        if t.face == "down" or (t.face != "up" and t.fy >= t.fh - 1 and t.fh >= 3):
            c = pink_sh
        # cow patches: blobs in 3D so they wrap across faces
        if t.part in ("body", "head", "leg_back_left", "leg_back_right") and t.face != "down":
            if vnoise(*t.pos, seed=7, scale=2.3) > 0.58:
                c = white
        if t.part == "head" and t.face == "north":
            # the white splodge around one eye, like a cow's
            if (t.fx <= 2 and t.fy <= 3) and not (t.fx == 0 and t.fy == 0):
                c = white
        if t.part == "snout":
            c = snout
        if t.part.startswith("ear"):
            c = pink if t.face != "down" else hexc("#f07c9b")
        if t.part.startswith("leg"):
            if (t.part.startswith("leg_front") and t.local[1] >= 2) or \
               (t.part.startswith("leg_back") and t.local[2] <= -2) or t.face == "down":
                c = hoof
        if t.part == "sprout":
            c = stem
        if t.part.startswith("leaf"):
            c = leaf_dk if (t.face == "up" and t.fy == 1 and t.fx in (0, 1, 2)) else leaf
            if t.face == "down":
                c = leaf_dk
        if t.part == "tail":
            c = pink_sh if t.local[2] < 1 else hexc("#f07c9b")
        px[t.tx, t.ty] = shade(c, g)
    # face (head north at uv 22+6=28, 0+6=6 ; 7 wide x 6 tall)
    blink = img.copy()
    for im, closed in ((img, False), (blink, True)):
        p = im.load()
        hx, hy = 28, 6
        for ex in (1, 4):
            if closed:
                p[hx + ex, hy + 3] = hexc("#3a1a28")
                p[hx + ex + 1, hy + 3] = hexc("#3a1a28")
            else:
                for dx in (0, 1):
                    for dy in (0, 1):
                        p[hx + ex + dx, hy + 2 + dy] = hexc("#2b1520")
                glint_x = hx + ex + (0 if ex == 1 else 1)
                p[glint_x, hy + 2] = hexc("#ffffff")
        for bx in (0, 6):
            p[hx + bx, hy + 4] = hexc("#f47c9c")
        # snout north: uv (48,0) d=1 -> (49,1) 5x2 ; nostrils at cols 1 and 3
        p[49 + 1, 1] = nostril
        p[49 + 3, 1] = nostril
        p[49 + 1, 2] = shade(nostril, 1.25)
        p[49 + 3, 2] = shade(nostril, 1.25)
    save(img, "textures/cosmetic/pet/mini_moosh.png")
    save(blink, "textures/cosmetic/pet/mini_moosh_blink.png")
    return img


# =========================================================================================
# HATS / BACK - model JSON generated here with packed UVs
# =========================================================================================

def pack_uvs(model, tex_w, tex_h):
    """Shelf-pack every cube's uv region; mutates the model."""
    boxes = []
    for part, _, _ in lm.walk(model["parts"]):
        for c in part.get("cubes", []):
            w, h, d = (round(s) for s in c["size"])
            boxes.append((c, 2 * (w + d), d + h))
    boxes.sort(key=lambda b: -b[2])
    x = y = shelf = 0
    for c, bw, bh in boxes:
        if x + bw > tex_w:
            x, y, shelf = 0, y + shelf, 0
        if y + bh > tex_h:
            raise SystemExit(f"uv pack overflow {model.get('name')}")
        c["uv"] = [x, y]
        x += bw
        shelf = max(shelf, bh)


def write_model(name, model):
    with open(os.path.join(MODELS, name + ".json"), "w") as f:
        json.dump(model, f, indent=1)


def cube(frm, size):
    return {"from": list(frm), "size": list(size)}


def sakura_crown():
    parts = [{"name": "crown", "pivot": [0, 0, 0], "cubes": [
        cube((-4.5, -0.5, -4.5), (9, 1, 1)), cube((-4.5, -0.5, 3.5), (9, 1, 1)),
        cube((-4.5, -0.5, -3.5), (1, 1, 7)), cube((3.5, -0.5, -3.5), (1, 1, 7))],
        "children": []}]
    flowers = [("f_front", (-1.5, -2.2, -5.0), (3, 3, 1)),
               ("f_front_l", (-4.4, -1.4, -4.9), (2, 2, 1)),
               ("f_front_r", (2.4, -1.4, -4.9), (2, 2, 1)),
               ("f_side_l", (-4.9, -1.6, -1.0), (1, 2, 2)),
               ("f_side_r", (3.9, -1.6, 0.5), (1, 2, 2)),
               ("f_back", (-1.0, -1.5, 3.9), (2, 2, 1))]
    leaves = [("l1", (-2.6, -1.0, -4.8), (1, 1, 1)), ("l2", (1.6, -1.0, -4.8), (1, 1, 1)),
              ("l3", (-4.8, -0.9, 1.2), (1, 1, 1)), ("l4", (3.8, -0.9, -0.6), (1, 1, 1))]
    for n, f, s in flowers + leaves:
        parts[0]["children"].append({"name": n, "pivot": [0, 0, 0], "cubes": [cube(f, s)]})
    m = {"texture_size": [32, 32], "parts": parts}
    pack_uvs(m, 32, 32)
    write_model("sakura_crown", m)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for t in lm.texels(m):
        if t.part == "crown":
            c = hexc("#7a4b36") if (t.tx + t.ty) % 3 else hexc("#8f5b42")   # twig band
            if hash01("crown", t.tx, t.ty) > 0.82:
                c = hexc("#6ab84a")
        elif t.part.startswith("l"):
            c = hexc("#6fc94c")
        else:
            big = t.fw >= 3 or t.fh >= 3
            cx, cy = (t.fw - 1) / 2, (t.fh - 1) / 2
            dist = abs(t.fx - cx) + abs(t.fy - cy)
            if t.face in ("north", "south", "west", "east") and ((t.fw >= 2 and t.fh >= 2)):
                if big and dist < 0.6:
                    c = hexc("#ffe27a")
                elif (not big) and dist <= 1.0 and hash01(t.part, t.fx, t.fy) > 0.7:
                    c = hexc("#fff0f6")
                elif big and dist >= 2:
                    c = (0, 0, 0, 0)
                else:
                    c = hexc("#ff8fb4") if (t.fx + t.fy) % 2 else hexc("#ffb3cc")
            else:
                c = hexc("#ff9bbb")
        px[t.tx, t.ty] = c
    save(img, "textures/cosmetic/hat/sakura_crown.png")
    return m, img


def kitty_ears():
    ear = lambda side: {
        "name": f"ear_{side}", "pivot": [(-2.5 if side == "left" else 2.5), -0.5, -0.5],
        "rot": [0, 0, -12 if side == "left" else 12],
        "cubes": [cube((-1.5, -2, -0.5), (3, 2, 1)), cube((-1, -3, -0.5), (2, 1, 1)),
                  cube((-0.5, -4, -0.5), (1, 1, 1))]}
    parts = [{"name": "band", "pivot": [0, 0, 0], "cubes": [
        cube((-4.5, -0.5, -0.5), (9, 1, 1)), cube((-4.5, 0.5, -0.5), (1, 3, 1)), cube((3.5, 0.5, -0.5), (1, 3, 1))],
        "children": [ear("left"), ear("right")]}]
    m = {"texture_size": [32, 16], "parts": parts}
    pack_uvs(m, 32, 16)
    write_model("kitty_ears", m)
    out = {}
    for variant, (fur, inner, band) in {
        "pink": ("#f7a8c4", "#ffe0ec", "#e06a96"),
        "midnight": ("#26222f", "#e38aa8", "#5a4a6a"),
        "snow": ("#f7f4f6", "#f8a6c0", "#d7cfe0"),
    }.items():
        img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
        px = img.load()
        for t in lm.texels(m):
            if t.part == "band":
                c = hexc(band)
            else:
                c = hexc(fur)
                # inner ear: the middle of each tier's front face
                if t.face == "north" and ((t.cube == 0 and t.fx == 1) or (t.cube == 1 and t.fx in (0, 1) and False) or t.cube == 1):
                    c = hexc(inner)
                if t.face == "north" and t.cube == 0 and t.fy == 0 and t.fx in (0, 2):
                    c = hexc(fur)
            px[t.tx, t.ty] = shade(c, 0.97 + 0.05 * hash01(variant, t.tx, t.ty))
        save(img, f"textures/cosmetic/hat/kitty_ears_{variant}.png")
        out[variant] = img
    return m, out


def star_halo():
    segs = []
    n = 10
    for i in range(n):
        a = 2 * math.pi * i / n
        segs.append({"name": f"seg{i}", "pivot": [round(4.2 * math.cos(a), 3), 0, round(4.2 * math.sin(a), 3)],
                     "rot": [0, round(-math.degrees(a) + 90, 2), 0],
                     "cubes": [cube((-1.4, -0.5, -0.5), (3, 1, 1))]})
    stars = []
    for i, a in enumerate((0.3, 2.4, 4.4)):
        stars.append({"name": f"star{i}", "pivot": [round(4.2 * math.cos(a), 3), -0.8, round(4.2 * math.sin(a), 3)],
                      "cubes": [cube((-1, -1, -1), (2, 2, 2))]})
    m = {"texture_size": [32, 16], "parts": [{"name": "halo", "pivot": [0, -3.5, 0], "cubes": [],
                                             "children": segs + stars}]}
    pack_uvs(m, 32, 16)
    write_model("star_halo", m)
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = img.load()
    for t in lm.texels(m):
        if t.part.startswith("star"):
            c = hexc("#fff2a8") if (t.fx + t.fy) % 2 == 0 else hexc("#ffd84a")
        else:
            c = hexc("#fff6c8") if t.face in ("up", "north", "south") else hexc("#f7d765")
        px[t.tx, t.ty] = c
    save(img, "textures/cosmetic/hat/star_halo.png")
    save(img, "textures/cosmetic/hat/star_halo_glow.png")
    return m, img


def petal_wings():
    def wing(side):
        sgn = 1 if side == "left" else -1
        return {"name": f"wing_{side}", "pivot": [sgn * 1.0, 3.0, 2.6], "rot": [0, sgn * -18, 0],
                "cubes": [cube((0 if sgn > 0 else -10, -6, 0), (10, 12, 0))]}
    m = {"texture_size": [64, 16], "parts": [{"name": "wings", "pivot": [0, 0, 0], "cubes": [],
                                             "children": [wing("left"), wing("right")]}]}
    # zero-depth cubes: 2*(w+0) x (0+h) = 20 x 12 each
    m["parts"][0]["children"][0]["cubes"][0]["uv"] = [0, 0]
    m["parts"][0]["children"][1]["cubes"][0]["uv"] = [20, 0]
    write_model("petal_wings", m)
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    px = img.load()
    # petal-wing silhouette drawn once for the left wing (x grows away from the spine)
    shape = [
        "..........",
        "....###...",
        "..######..",
        ".########.",
        "##########",
        "#########.",
        "########..",
        "..######..",
        ".#######..",
        ".######...",
        "..####....",
        "...##.....",
    ]
    def col(x, y):
        edge = hexc("#e0668e")
        inner = hexc("#ffc1d6")
        mid = hexc("#ff9fc0")
        vein = hexc("#fff3f8")
        if shape[y][x] != "#":
            return (0, 0, 0, 0)
        neigh = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                    if not (0 <= x + dx < 10 and 0 <= y + dy < 12) or shape[y + dy][x + dx] != "#")
        if neigh:
            return edge
        if (x == y - 2 and x < 6) or (y == 8 and 2 <= x <= 5):
            return vein
        return inner if x > 3 else mid
    for side, u in (("left", 0), ("right", 20)):
        for y in range(12):
            for x in range(10):
                c = col(x, y)
                # north face (u..u+10) and south face (u+10..u+20); mirror so the silhouette
                # reads the same from both sides; right wing mirrors the left one
                xx = x if side == "left" else 9 - x
                px[u + xx, y] = c
                px[u + 10 + (9 - xx), y] = c
    save(img, "textures/cosmetic/back/petal_wings.png")
    return m, img


def starry_wings():
    """Night-sky wings: indigo at the spine fading to violet tips, with glowing stars."""
    W, H = 12, 14
    def wing(side):
        sgn = 1 if side == "left" else -1
        return {"name": f"wing_{side}", "pivot": [sgn * 1.0, 2.5, 2.6], "rot": [0, sgn * -18, 0],
                "cubes": [cube((0 if sgn > 0 else -W, -7, 0), (W, H, 0))]}
    m = {"texture_size": [64, 16], "parts": [{"name": "wings", "pivot": [0, 0, 0], "cubes": [],
                                             "children": [wing("left"), wing("right")]}]}
    m["parts"][0]["children"][0]["cubes"][0]["uv"] = [0, 0]
    m["parts"][0]["children"][1]["cubes"][0]["uv"] = [2 * W, 0]
    write_model("starry_wings", m)
    shape = [
        "............",
        "......####..",
        "....#######.",
        "..#########.",
        ".###########",
        "############",
        "###########.",
        "#########...",
        "..########..",
        ".#########..",
        ".########...",
        "..######....",
        "...####.....",
        "....##......",
    ]
    rnd = random.Random(42)
    stars = {}
    for y in range(H):
        for x in range(W):
            if shape[y][x] == "#" and rnd.random() < 0.07:
                stars[(x, y)] = rnd.choice(["#fffbe0", "#fff1a8", "#cfe3ff", "#ffffff"])
    for sx, sy in ((7, 4), (4, 9)):          # two bigger 4-point sparkles
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 <= sx + dx < W and 0 <= sy + dy < H and shape[sy + dy][sx + dx] == "#":
                stars[(sx + dx, sy + dy)] = "#ffffff" if (dx, dy) == (0, 0) else "#fff1a8"
    def edge(x, y):
        return any(not (0 <= x + dx < W and 0 <= y + dy < H) or shape[y + dy][x + dx] != "#"
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    def col(x, y):
        if shape[y][x] != "#":
            return (0, 0, 0, 0)
        if (x, y) in stars:
            return hexc(stars[(x, y)])
        if edge(x, y):
            return hexc("#140f3a")
        f = x / (W - 1)
        base = mix(hexc("#1c2466"), hexc("#6a4bc4"), f)
        if (x + 2 * y) % 7 == 0:
            base = mix(base, hexc("#3b6fd6"), 0.45)   # a faint milky-way swirl
        return base
    img = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    glow = Image.new("RGBA", (64, 16), (0, 0, 0, 0))
    px, gp = img.load(), glow.load()
    for side, u in (("left", 0), ("right", 2 * W)):
        for y in range(H):
            for x in range(W):
                c = col(x, y)
                xx = x if side == "left" else W - 1 - x
                px[u + xx, y] = c
                px[u + W + (W - 1 - xx), y] = c
                if (x, y) in stars:
                    gp[u + xx, y] = c
                    gp[u + W + (W - 1 - xx), y] = c
    save(img, "textures/cosmetic/back/starry_wings.png")
    save(glow, "textures/cosmetic/back/starry_wings_glow.png")
    return m, img


# =========================================================================================
# GUI sprites (hand-drawn pixel art)
# =========================================================================================
SPR = os.path.join("textures/gui/sprites")


def draw_ascii(rows, palette, scale=1):
    h, w = len(rows), max(len(r) for r in rows)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in palette and palette[ch] is not None:
                px[x, y] = palette[ch]
    if scale != 1:
        img = img.resize((w * scale, h * scale), Image.NEAREST)
    return img


# 16x16 cat face for the menu button. O outline, F fur, S shade, I inner ear, E eye,
# e eye glint, N nose, M mouth, B blush, W whisker
CAT_FACE = [
    "................",
    "..OO........OO..",
    ".OIFO......OFIO.",
    ".OIIFOOOOOOFIIO.",
    ".OFFFFFFFFFFFFO.",
    "OFFFFFFFFFFFFFFO",
    "OFFFFFFFFFFFFFFO",
    "OFFEeFFFFFFEeFFO",
    "OFFEEFFFFFFEEFFO",
    "OFBBFFFNNFFFBBFO",
    "WWFFFFFMMFFFFFWW",
    "OFFFFFMFFMFFFFFO",
    ".OFFFFFFFFFFFFO.",
    "..OSSFFFFFFSSO..",
    "...OOOOOOOOOO...",
    "................",
]


def face_variant(kind):
    rows = [list(r) for r in CAT_FACE]
    if kind in ("blink", "happy"):
        for y in (7, 8):
            for x in range(16):
                if rows[y][x] in "Ee":
                    rows[y][x] = "F"
        if kind == "blink":
            for x in (3, 4, 11, 12):
                rows[8][x] = "E"
        else:  # ^ ^
            for x, y in ((3, 8), (4, 7), (5, 8), (10, 8), (11, 7), (12, 8)):
                rows[y][x] = "E"
    if kind == "twitch":  # left ear folds down a notch
        rows[1] = list("............OO..")
        rows[2] = list("...........OFIO.")
        rows[3] = list(".OOFOOOOOOOFIIO.")
        rows[4] = list("OIIFFFFFFFFFFFFO")
    if kind == "meow":
        rows[10] = list("WWFFFFFMMFFFFFWW")
        rows[11] = list("OFFFFFMPPMFFFFFO")
        rows[12] = list(".OFFFFFMMFFFFFO.")
    return ["".join(r) for r in rows]


BUTTON_PALETTES = {
    # cherry cat (theme on)
    "cherry": {"O": hexc("#7a1f3d"), "F": hexc("#f06292"), "S": hexc("#d94a7b"), "I": hexc("#ffd1e0"),
               "E": hexc("#3a0f1f"), "e": hexc("#ffffff"), "N": hexc("#ffe0ea"), "M": hexc("#7a1f3d"),
               "B": hexc("#ff9fbf"), "W": hexc("#ffe6ef"), "P": hexc("#ff8fab")},
    # plain white kitty (theme off)
    "plain": {"O": hexc("#3b3440"), "F": hexc("#f4f1f4"), "S": hexc("#d8d0da"), "I": hexc("#f6a7c0"),
              "E": hexc("#2a2230"), "e": hexc("#ffffff"), "N": hexc("#f28cab"), "M": hexc("#3b3440"),
              "B": hexc("#f9c1d3"), "W": hexc("#c9c0cc"), "P": hexc("#f28cab")},
}

# Sitting cat for the title screen, 24x24, drawn without its tail (the tail is its own
# sprite so it can swish). Same letters as the face.
SITTING = [
    "........................",
    "......OO......OO........",
    ".....OIFO....OFIO.......",
    ".....OIIFOOOOFIIO.......",
    ".....OFFFFFFFFFFO.......",
    "....OFFFFFFFFFFFFO......",
    "....OFFEeFFFFEeFFO......",
    "....OFFEEFFFFEEFFO......",
    "....OFBBFFNNFFBBFO......",
    "...WWFFFFFMMFFFFFWW.....",
    ".....OFFFMFFMFFFO.......",
    "......OFFFFFFFFO........",
    ".....OFFFFFFFFFFO.......",
    "....OFFFFFFFFFFFFO......",
    "....OFFFFSSSSFFFFO......",
    "...OFFFFSFFFFSFFFFO.....",
    "...OFFFFSFFFFSFFFFO.....",
    "...OFFFFSFFFFSFFFFO.....",
    "...OFFFFFSFFSFFFFFO.....",
    "...OFFFFFSFFSFFFFFO.....",
    "...OSFFFFSFFSFFFFSO.....",
    "....OOSSOOOOOOSSOO......",
    ".....OOO......OOO.......",
    "........................",
]

TAIL = [
    "..OO....",
    ".OFFO...",
    ".OFFO...",
    "..OFFO..",
    "..OFFO..",
    "...OFFO.",
    "...OFFO.",
    "...OFFO.",
    "..OFFO..",
    ".OFFO...",
    "OFFO....",
    "OOO.....",
]


def sitting_variant(kind):
    rows = [list(r) for r in SITTING]
    if kind in ("blink", "sleep"):
        for y in (6, 7):
            for x in range(24):
                if rows[y][x] in "Ee":
                    rows[y][x] = "F"
        for x in (7, 8, 13, 14):
            rows[7][x] = "E"
    return ["".join(r) for r in rows]


PETALS = [
    [".PP.", "PLLP", "PLLP", ".PP."],
    ["..P.", ".PLP", "PLLP", ".PP."],
    [".P..", "PLP.", "PLLP", ".PLP", "..P."],
    [".PP..", "PLLP.", ".PLLP", "..PP."],
]
BLOSSOM = [
    "..P.P..",
    ".PLPLP.",
    "PLLCLLP",
    ".PCYCP.",
    "PLLCLLP",
    ".PLPLP.",
    "..P.P..",
]
PETAL_PAL = {"P": hexc("#f47aa2"), "L": hexc("#ffc6d9"), "C": hexc("#ffe7f0"), "Y": hexc("#ffd84a")}

EAR = ["..O..", ".OIO.", ".OIO.", "OIIIO", "OIIIO"]  # little ear that pops up over a hovered button

PAW_CURSOR = [
    "......OO..OO....",
    ".....OPPOOPPO...",
    "..OO.OPPOOPPO.OO",
    ".OPPOOPPOOPPOOPPO",
    ".OPPO.OOO.OOOOPPO",
    "..OO..........OO.",
    ".....OOOOOOO.....",
    "....OPPPPPPPO....",
    "...OPPPPPPPPPO...",
    "...OPPPPPPPPPO...",
    "...OPPPPPPPPPO...",
    "....OPPPPPPPO....",
    ".....OOOOOOO.....",
]
# Cursor with the hotspot top-left, like an arrow: a paw-tipped pointer.
POINTER = [
    "OO..............",
    "OWO.............",
    "OWWO............",
    "OWWWO...........",
    "OWWWWO..........",
    "OWWWWWO.........",
    "OWWWWWWO........",
    "OWWWWWWWO.......",
    "OWWWWWWWWO......",
    "OWWWWWOOOOO.....",
    "OWWOWWO..OO.OO..",
    "OWO.OWWO.OPOOPO.",
    "OO..OWWO.OPPPPO.",
    ".....OWWOOPPPPO.",
    ".....OOO..OPPO..",
    "...........OO...",
]
HAND = [
    ".....OO.........",
    "....OPPO........",
    "....OPPO.OO.....",
    "....OPPOOPPOOO..",
    "....OPPOOPPOPPO.",
    ".OO.OPPPPPPPPPO.",
    "OPPOOPPPPPPPPPO.",
    "OPPPOPPPPPPPPPO.",
    ".OPPPPPPPPPPPPO.",
    "..OPPPPPPPPPPO..",
    "..OPPLPPPPLPPO..",
    "...OPPPPPPPPO...",
    "....OPPPPPPO....",
    ".....OOOOOO.....",
    "................",
    "................",
]


def gui_sprites():
    for pal_name, pal in BUTTON_PALETTES.items():
        for kind in ("open", "blink", "happy", "twitch", "meow"):
            img = draw_ascii(face_variant(kind), pal)
            save(img, f"{SPR}/lunascosmetics/cat_{pal_name}_{kind}.png")
        for kind in ("open", "blink", "sleep"):
            save(draw_ascii(sitting_variant(kind), pal), f"{SPR}/lunascosmetics/sit_{pal_name}_{kind}.png")
        save(draw_ascii(TAIL, pal), f"{SPR}/lunascosmetics/tail_{pal_name}.png")
    for i, p in enumerate(PETALS):
        save(draw_ascii(p, PETAL_PAL), f"{SPR}/lunascosmetics/petal_{i}.png")
    save(draw_ascii(BLOSSOM, PETAL_PAL), f"{SPR}/lunascosmetics/blossom.png")
    ear_pal = {"O": hexc("#b83a66"), "I": hexc("#ff9fc0")}
    left = draw_ascii(EAR, ear_pal)
    save(left, f"{SPR}/lunascosmetics/ear_left.png")
    save(left.transpose(Image.FLIP_LEFT_RIGHT), f"{SPR}/lunascosmetics/ear_right.png")
    # cursors are loaded straight from the jar, not the atlas
    cur_pal = {"O": hexc("#7a1f3d"), "W": hexc("#fff4f8"), "P": hexc("#f7a1bf"), "L": hexc("#ffd3e2")}
    for name, art in (("pointer", POINTER), ("hand", HAND)):
        save(draw_ascii(art, cur_pal, scale=2), f"textures/cursor/{name}.png")
    # tab + UI icons (12x12)
    icons = {
        "icon_pets": (["..O......O..", ".OIO....OIO.", ".OFFOOOOFFO.", "OFFFFFFFFFFO",
                       "OFEFFFFFFEFO", "OFFFFNNFFFFO", "OFFFFMMFFFFO", ".OFFFFFFFFO.",
                       "..OOOOOOOO..", "............", "............", "............"], BUTTON_PALETTES["cherry"]),
        "icon_hats": (["............", "....OOOO....", "...OPPPPO...", "..OPPPPPPO..", "..OPPPPPPO..",
                       ".OOOOOOOOOO.", "OYYYYYYYYYYO", ".OOOOOOOOOO.", "............", "............",
                       "............", "............"], {"O": hexc("#7a1f3d"), "P": hexc("#f06292"), "Y": hexc("#ffd84a")}),
        "icon_back": (["............", ".OO......OO.", "OPPO....OPPO", "OPPPO..OPPPO", "OPPPPOOPPPPO",
                       ".OPPPOOPPPO.", ".OPPO..OPPO.", "..OO....OO..", "............", "............",
                       "............", "............"], {"O": hexc("#b83a66"), "P": hexc("#ffc1d6")}),
        "icon_custom": (["............", "OOOOO.......", "OYYYYOOOOOO.", "OYYYYYYYYYYO", "OYYYYYYYYYYO",
                         "OYYYYYYYYYYO", "OYYYYYYYYYYO", "OYYYYYYYYYYO", "OOOOOOOOOOOO", "............",
                         "............", "............"], {"O": hexc("#8a5a18"), "Y": hexc("#ffd36a")}),
        "icon_theme": (["....P.P.....", "...PLPLP....", "..PLLCLLP...", "...PCYCP....", "..PLLCLLP...",
                        "...PLPLP....", "....P.P.....", "............", "............", "............",
                        "............", "............"], PETAL_PAL),
        "icon_heart": (["............", ".OO...OO....", "OPPO.OPPO...", "OPLPOPPPO...", "OPPPPPPPO...",
                        ".OPPPPPO....", "..OPPPO.....", "...OPO......", "....O.......", "............",
                        "............", "............"], {"O": hexc("#7a1f3d"), "P": hexc("#f06292"), "L": hexc("#ffd1e0")}),
        "icon_none": (["............", "...OOOOOO...", "..O......O..", ".O.O......O.", ".O..O.....O.",
                       ".O...O....O.", ".O....O...O.", ".O.....O..O.", ".O......O.O.", "..O......O..",
                       "...OOOOOO...", "............"], {"O": hexc("#b83a66")}),
    }
    for name, (art, pal) in icons.items():
        save(draw_ascii(art, pal), f"{SPR}/lunascosmetics/{name}.png")


# =========================================================================================
def main():
    previews = []
    for name, spec in CATS.items():
        tex, _ = paint_cat(name, spec)
        previews.append((name, CAT, tex))
    moosh = paint_moosh()
    previews.append(("mini_moosh", MOOSH, moosh))
    crown_m, crown_t = sakura_crown()
    ears_m, ears_t = kitty_ears()
    halo_m, halo_t = star_halo()
    wings_m, wings_t = petal_wings()
    starry_m, starry_t = starry_wings()
    gui_sprites()
    # contact sheet of the pets at rest, for eyeballing
    sheet = Image.new("RGBA", (5 * 220, 3 * 220), (34, 30, 40, 255))
    lie = {"leg_front_left": {"rot": (-90, 0, 0)}, "leg_front_right": {"rot": (-90, 0, 0)},
           "leg_back_left": {"rot": (90, -15, 0)}, "leg_back_right": {"rot": (90, 15, 0)},
           "tail": {"rot": (-35, 20, 0)}, "tail_tip": {"rot": (-30, 25, 0)}}
    for i, (name, mdl, tex) in enumerate(previews):
        pose = lie if mdl is CAT else {}
        im = lm.render(mdl, tex, pose, yaw=-35, pitch=22, px=11, size=(220, 220))
        sheet.paste(im, ((i % 5) * 220, (i // 5) * 220))
    out = os.environ.get("PREVIEW_OUT")
    if out:
        sheet.save(out)
        hats = Image.new("RGBA", (5 * 220, 220), (34, 30, 40, 255))
        for i, (m, t) in enumerate(((crown_m, crown_t), (ears_m, ears_t["pink"]), (halo_m, halo_t), (wings_m, wings_t),
                                    (starry_m, starry_t))):
            hats.paste(lm.render(m, t, {}, yaw=-30, pitch=20, px=11, size=(220, 220)), (i * 220, 0))
        hats.save(out.replace(".png", "_hats.png"))
    print("art done")


if __name__ == "__main__":
    main()

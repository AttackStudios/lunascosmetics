#!/usr/bin/env python3
"""
Store-page art for Luna's Cosmetics: icon, banner, "meet the pets" lineup, and the
gallery (cropped in-game screenshots).

usage: media.py <mod resources dir> <minecraft ascii.png font> <screenshots dir> <out dir>
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
import lunamodel as lm  # noqa: E402

RES, FONT, SHOTS, OUT = sys.argv[1:5]
A = os.path.join(RES, "assets/lunascosmetics")
os.makedirs(os.path.join(OUT, "gallery"), exist_ok=True)

CAT = lm.load(os.path.join(A, "models/cosmetic/cat.json"))
MOOSH = lm.load(os.path.join(A, "models/cosmetic/moosh.json"))
LOAF = {"leg_front_left": {"rot": (-90, 0, 0)}, "leg_front_right": {"rot": (-90, 0, 0)},
        "leg_back_left": {"rot": (90, -15, 0)}, "leg_back_right": {"rot": (90, 15, 0)},
        "tail": {"rot": (-4, 72, 0)}, "tail_tip": {"rot": (0, 78, 0)}}   # curled round the side for renders
SIT = {"body": {"rot": (-60, 0, 0), "offset": (0, -2.9, -0.8)}, "head": {"rot": (60, 0, 0)},
       "leg_front_left": {"rot": (60, 0, 0), "offset": (0, 1.215, 2.104)},
       "leg_front_right": {"rot": (60, 0, 0), "offset": (0, 1.215, 2.104)},
       "leg_back_left": {"rot": (-30, -12, 0), "offset": (0, 0.08, 0.139)},
       "leg_back_right": {"rot": (-30, 12, 0), "offset": (0, 0.08, 0.139)},
       "tail": {"rot": (-8, 70, 0)}, "tail_tip": {"rot": (0, 75, 0)}}   # curled round for renders
PETS = [("snowball", "Snowball"), ("luna", "Luna"), ("stargazer", "Stargazer"), ("sakura", "Sakura"),
        ("marmalade", "Marmalade"), ("tuxedo", "Tuxedo"), ("calico", "Calico"), ("siamese", "Siamese"),
        ("smokey", "Smokey"), ("cocoa", "Cocoa"), ("mini_moosh", "Mini Moosh")]


def tex(name):
    if name == "mini_moosh":
        return Image.open(os.path.join(A, "textures/cosmetic/pet/mini_moosh.png"))
    return Image.open(os.path.join(A, f"textures/cosmetic/cat/{name}.png"))


def render_pet(name, pose_name, px, size, yaw=-30, pitch=14):
    model = MOOSH if name == "mini_moosh" else CAT
    pose = {} if name == "mini_moosh" else (SIT if pose_name == "sit" else LOAF)
    im = lm.render(model, tex(name), pose, yaw=yaw, pitch=pitch, px=px, size=size, bg=(0, 0, 0, 0))
    return im


# ---- pixel font (Minecraft's ascii.png) ------------------------------------------------------
_font = Image.open(FONT).convert("RGBA")
_glyphs = {}


def glyph(ch):
    if ch in _glyphs:
        return _glyphs[ch]
    code = ord(ch)
    if code > 255:
        code = ord("?")
    gx, gy = (code % 16) * 8, (code // 16) * 8
    g = _font.crop((gx, gy, gx + 8, gy + 8))
    w = 0
    for x in range(8):
        if any(g.getpixel((x, y))[3] > 0 for y in range(8)):
            w = x + 1
    if ch == " ":
        w = 3
    _glyphs[ch] = (g, w)
    return _glyphs[ch]


def text_size(s, scale):
    return sum((glyph(c)[1] + 1) * scale for c in s), 8 * scale


def draw_text(img, s, x, y, scale, color, shadow=None, center=False):
    w, _ = text_size(s, scale)
    if center:
        x -= w // 2
    if shadow:
        draw_text(img, s, x + scale, y + scale, scale, shadow)
    cx = x
    for c in s:
        g, gw = glyph(c)
        if gw:
            mask = g.split()[3].resize((8 * scale, 8 * scale), Image.NEAREST)
            layer = Image.new("RGBA", mask.size, color)
            img.paste(layer, (cx, y), mask)
        cx += (gw + 1) * scale
    return w


# ---- backgrounds ---------------------------------------------------------------------------
def gradient(w, h, top, bottom):
    g = Image.new("RGBA", (w, h))
    px = g.load()
    for y in range(h):
        f = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * f) for i in range(3)) + (255,)
        for x in range(w):
            px[x, y] = c
    return g


PETAL_SPRITES = [Image.open(os.path.join(A, f"textures/gui/sprites/lunascosmetics/petal_{i}.png")).convert("RGBA")
                 for i in range(4)]
BLOSSOM = Image.open(os.path.join(A, "textures/gui/sprites/lunascosmetics/blossom.png")).convert("RGBA")


def sprinkle(img, n, seed, big=6):
    rnd = random.Random(seed)
    for _ in range(n):
        s = BLOSSOM if rnd.random() < 0.12 else rnd.choice(PETAL_SPRITES)
        k = rnd.randint(big - 2, big + 3)
        p = s.resize((s.width * k, s.height * k), Image.NEAREST).rotate(rnd.choice([0, 90, 180, 270]), expand=True)
        a = p.split()[3].point(lambda v: int(v * rnd.uniform(0.45, 0.95)))
        p.putalpha(a)
        img.alpha_composite(p, (rnd.randint(-20, img.width), rnd.randint(-20, img.height)))


def shadow_under(img, cx, cy, rw, rh, alpha=70):
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse((cx - rw, cy - rh, cx + rw, cy + rh), fill=(60, 10, 35, alpha))
    img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(8)))


def rounded(img, r):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), r, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def trim(im):
    bb = im.getbbox()
    return im.crop(bb) if bb else im


def fit(im, mw, mh):
    im = trim(im)
    k = min(mw / im.width, mh / im.height)
    return im.resize((max(1, int(im.width * k)), max(1, int(im.height * k))), Image.NEAREST)


# ---- icon ----------------------------------------------------------------------------------
def icon():
    S = 512
    bg = gradient(S, S, (255, 214, 229), (240, 120, 160))
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((70, 60, 442, 432), fill=(255, 250, 252, 170))
    bg.alpha_composite(glow.filter(ImageFilter.GaussianBlur(40)))
    sprinkle(bg, 22, 3, big=7)
    cat = fit(render_pet("sakura", "sit", 20, (800, 800), yaw=-28, pitch=10), 380, 380)
    shadow_under(bg, S // 2, 452, 120, 18)
    bg.alpha_composite(cat, ((S - cat.width) // 2, 450 - cat.height))
    out = rounded(bg, 96)
    border = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle((4, 4, S - 5, S - 5), 94, outline=(214, 70, 120, 255), width=8)
    out.alpha_composite(border)
    out.save(os.path.join(OUT, "icon.png"))
    out.resize((128, 128), Image.LANCZOS).save(os.path.join(A, "icon.png"))


# ---- banner --------------------------------------------------------------------------------
def banner():
    W, H = 1920, 640
    bg = gradient(W, H, (58, 20, 40), (214, 90, 138))
    sprinkle(bg, 70, 7, big=7)
    # a soft stage for the pets
    shadow_under(bg, 1450, 562, 430, 22, 90)
    # evenly spaced, each fitted into its own slot so nobody overlaps
    order = [("snowball", "loaf"), ("mini_moosh", None), ("luna", "sit"), ("sakura", "sit"), ("marmalade", "loaf")]
    slot_w, x_start = 172, 1110
    for i, (name, pose) in enumerate(order):
        im = fit(render_pet(name, pose, 16, (600, 600), yaw=-28, pitch=12), slot_w - 22, 250)
        cx = x_start + i * slot_w
        bg.alpha_composite(im, (cx - im.width // 2, 566 - im.height))
    draw_text(bg, "Luna's Cosmetics", 90, 170, 10, (255, 244, 248, 255), shadow=(160, 40, 90, 255))
    draw_text(bg, "Living pets, hats & wings", 96, 300, 5, (255, 214, 229, 255), shadow=(90, 20, 55, 255))
    draw_text(bg, "+ a Cherry Cat theme for your whole game", 96, 360, 4, (255, 193, 214, 255),
              shadow=(90, 20, 55, 255))
    draw_text(bg, "100% free  -  Fabric & NeoForge", 96, 430, 4, (255, 232, 150, 255), shadow=(90, 20, 55, 255))
    bg.convert("RGB").save(os.path.join(OUT, "banner.png"))


# ---- meet the pets -----------------------------------------------------------------------------
def lineup():
    W, H = 1920, 1080
    bg = gradient(W, H, (255, 228, 238), (245, 160, 190))
    sprinkle(bg, 60, 11, big=6)
    draw_text(bg, "Meet the pets", W // 2, 50, 9, (122, 31, 61, 255), shadow=(255, 255, 255, 255), center=True)
    cols, cell_w, cell_h = 4, 440, 300
    x0, y0 = (W - cols * cell_w) // 2, 170
    for i, (name, label) in enumerate(PETS):
        r, c = divmod(i, cols)
        if r == 2:
            x0r = (W - 3 * cell_w) // 2
        else:
            x0r = x0
        cx = x0r + c * cell_w + cell_w // 2
        base = y0 + r * cell_h + 220
        card = Image.new("RGBA", (cell_w - 30, cell_h - 24), (0, 0, 0, 0))
        ImageDraw.Draw(card).rounded_rectangle((0, 0, card.width - 1, card.height - 1), 26,
                                               fill=(255, 250, 252, 170), outline=(240, 130, 170, 255), width=4)
        bg.alpha_composite(card, (cx - card.width // 2, y0 + r * cell_h))
        shadow_under(bg, cx, base, 110, 12)
        im = fit(render_pet(name, "loaf", 16, (560, 560)), 330, 175)
        bg.alpha_composite(im, (cx - im.width // 2, base - im.height))
        draw_text(bg, label, cx, base + 18, 4, (122, 31, 61, 255), center=True)
    bg.convert("RGB").save(os.path.join(OUT, "gallery", "00_meet_the_pets.png"))


# ---- gallery screenshots -------------------------------------------------------------------------
GALLERY = [
    ("g01_snowball.png", "01_snowball_on_head.png"),
    ("g02_moosh_wings.png", "02_mini_moosh_and_wings.png"),
    ("g03_sitting_crown.png", "03_sitting_cat_sakura_crown.png"),
    ("g04_stargazer_halo.png", "04_stargazer_star_halo.png"),
    ("g05_shoulder_ears.png", "05_shoulder_cat_kitty_ears.png"),
    ("g06_back_wings.png", "06_petal_wings.png"),
    ("g11_wardrobe_pets.png", "07_wardrobe.png"),
    ("g12_wardrobe_hats.png", "08_wardrobe_hats.png"),
    ("g13_wardrobe_settings.png", "09_settings.png"),
    ("title.png", "10_cherry_cat_title_screen.png"),
    ("g10_pause.png", "11_cherry_cat_menus.png"),
    ("g09_inventory.png", "12_pink_inventory.png"),
    ("g08_hud.png", "13_pink_hud.png"),
]


def gallery():
    for src, dst in GALLERY:
        p = os.path.join(SHOTS, src)
        if not os.path.exists(p):
            print("missing", src)
            continue
        im = Image.open(p).convert("RGB")
        w, h = im.size
        tw = min(w, int(h * 16 / 9))
        th = int(tw * 9 / 16)
        left = (w - tw) // 2
        top = (h - th) // 2
        im = im.crop((left, top, left + tw, top + th)).resize((1920, 1080), Image.LANCZOS)
        im.save(os.path.join(OUT, "gallery", dst))


icon()
banner()
lineup()
gallery()
print("media done")

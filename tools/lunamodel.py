"""
Shared helpers for Luna's Cosmetics model JSON (the same files the mod loads).

- texels(model): every texel of every cube face with its rest-pose 3D position, so
  textures can be painted with 3D-aware pattern functions.
- render(model, texture, pose): a tiny software rasteriser that reproduces
  ModelPart maths (translate pivot, rotate Z*Y*X, scale) for eyeballing poses.

UV layout matches net.minecraft.client.model.ModelPart.Cuboid exactly:
  top    (u+d, v)       w x d   columns +x, rows back(+z) -> front(-z)
  bottom (u+d+w, v)     w x d   same orientation
  west   (u, v+d)       d x h   columns +z -> -z   (-x side)
  north  (u+d, v+d)     w x h   columns -x -> +x   (front)
  east   (u+d+w, v+d)   d x h   columns -z -> +z   (+x side)
  south  (u+2d+w, v+d)  w x h   columns +x -> -x   (back)
  rows always run top (-y) -> bottom (+y) on the side faces.
"""
import json
import math

import numpy as np
from PIL import Image


def load(path):
    with open(path) as f:
        return json.load(f)


def walk(parts, parent_pivot=(0.0, 0.0, 0.0), chain=()):
    for p in parts:
        piv = tuple(parent_pivot[i] + p["pivot"][i] for i in range(3))
        yield p, piv, chain + (p["name"],)
        yield from walk(p.get("children", []), piv, chain + (p["name"],))


class Texel:
    __slots__ = ("part", "chain", "face", "tx", "ty", "fx", "fy", "fw", "fh", "pos", "local", "cube")

    def __repr__(self):
        return f"<{self.part}:{self.face} {self.fx},{self.fy} @{self.pos}>"


def face_regions(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d),
        "down": (u + d + w, v, w, d),
        "west": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


def _texel_local(face, fx, fy, frm, size):
    """centre of texel (fx,fy) on a face, in part-local coords"""
    x0, y0, z0 = frm
    w, h, d = size
    cx, cy = fx + 0.5, fy + 0.5
    if face == "north":
        return (x0 + cx, y0 + cy, z0)
    if face == "south":
        return (x0 + w - cx, y0 + cy, z0 + d)
    if face == "west":
        return (x0, y0 + cy, z0 + d - cx)
    if face == "east":
        return (x0 + w, y0 + cy, z0 + cx)
    if face == "up":
        return (x0 + cx, y0, z0 + d - cy)
    if face == "down":
        return (x0 + cx, y0 + h, z0 + d - cy)


def texels(model):
    out = []
    for part, piv, chain in walk(model["parts"]):
        for ci, c in enumerate(part.get("cubes", [])):
            u, v = c["uv"]
            w, h, d = c["size"]
            for face, (fu, fv, fw, fh) in face_regions(u, v, round(w), round(h), round(d)).items():
                for fy in range(int(fh)):
                    for fx in range(int(fw)):
                        t = Texel()
                        t.part, t.chain, t.face, t.cube = part["name"], chain, face, ci
                        t.tx, t.ty = int(fu + fx), int(fv + fy)
                        t.fx, t.fy, t.fw, t.fh = fx, fy, int(fw), int(fh)
                        t.local = _texel_local(face, fx, fy, c["from"], (w, h, d))
                        t.pos = tuple(piv[i] + t.local[i] for i in range(3))
                        out.append(t)
    return out


# ---- preview rasteriser ------------------------------------------------------------------

def _rot(pitch, yaw, roll):
    cx, sx = math.cos(pitch), math.sin(pitch)
    cy, sy = math.cos(yaw), math.sin(yaw)
    cz, sz = math.cos(roll), math.sin(roll)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def _quads(model, pose):
    """yield (4 corners in model space, face name, uv rect) per face, posed"""
    def rec(parts, M, t):
        for p in parts:
            pp = pose.get(p["name"], {})
            if pp.get("hidden"):
                continue
            piv = np.array(p["pivot"], float) + np.array(pp.get("offset", (0, 0, 0)), float)
            base = p.get("rot", (0, 0, 0))
            add = pp.get("rot", (0, 0, 0))
            R = _rot(*(math.radians(base[i] + add[i]) for i in range(3)))
            S = np.diag(pp.get("scale", (1, 1, 1)))
            M2 = M @ R @ S
            t2 = t + M @ piv
            for c in p.get("cubes", []):
                x0, y0, z0 = c["from"]
                w, h, d = c["size"]
                x1, y1, z1 = x0 + w, y0 + h, z0 + d
                u, v = c["uv"]
                regions = face_regions(u, v, round(w), round(h), round(d))
                # corners listed as (top-left, top-right, bottom-right, bottom-left) of the uv rect
                faces = {
                    "north": [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],
                    "south": [(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)],
                    "west": [(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)],
                    "east": [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)],
                    "up": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
                    "down": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
                }
                for name, pts in faces.items():
                    world = [t2 + M2 @ np.array(q, float) for q in pts]
                    yield world, name, regions[name]
            yield from rec(p.get("children", []), M2, t2)
    yield from rec(model["parts"], np.eye(3), np.zeros(3))


LIGHT = {"up": 1.0, "north": 0.85, "south": 0.7, "west": 0.78, "east": 0.78, "down": 0.55}


def render(model, tex, pose=None, yaw=35, pitch=20, px=12, size=(320, 320), bg=(40, 36, 44, 255),
           extra=None):
    """Orthographic render. yaw/pitch = camera orbit in degrees. extra: list of (model, tex, pose)."""
    pose = pose or {}
    items = [(model, tex, pose)] + (extra or [])
    img = np.zeros((size[1], size[0], 4), np.uint8)
    img[:, :] = bg
    zbuf = np.full((size[1], size[0]), -1e9)
    V = _rot(math.radians(pitch), math.radians(yaw), 0)
    for mdl, tx, ps in items:
        t = np.array(tx.convert("RGBA"))
        th, tw = t.shape[:2]
        for world, name, (fu, fv, fw, fh) in _quads(mdl, ps):
            if fw <= 0 or fh <= 0:
                continue
            P = [V @ q for q in world]
            # screen: x right, y down (model y is already down), depth = -z toward viewer
            a, b, c, d = P
            ex, ey = b - a, d - a
            # subdivide per texel
            for j in range(int(fh)):
                for i in range(int(fw)):
                    col = t[int(fv + j) % th, int(fu + i) % tw]
                    if col[3] < 16:
                        continue
                    p0 = a + ex * (i / fw) + ey * (j / fh)
                    p1 = a + ex * ((i + 1) / fw) + ey * (j / fh)
                    p2 = a + ex * ((i + 1) / fw) + ey * ((j + 1) / fh)
                    p3 = a + ex * (i / fw) + ey * ((j + 1) / fh)
                    shade = LIGHT[name]
                    rgb = (col[:3] * shade).astype(np.uint8)
                    _fill_quad(img, zbuf, [p0, p1, p2, p3], rgb, px, size)
    return Image.fromarray(img)


def _fill_quad(img, zbuf, pts, rgb, px, size):
    cx, cy = size[0] / 2, size[1] * 0.6
    xs = [p[0] * px + cx for p in pts]
    ys = [p[1] * px + cy for p in pts]
    zs = [-p[2] for p in pts]
    x0, x1 = int(max(0, math.floor(min(xs)))), int(min(size[0] - 1, math.ceil(max(xs))))
    y0, y1 = int(max(0, math.floor(min(ys)))), int(min(size[1] - 1, math.ceil(max(ys))))
    if x1 < x0 or y1 < y0:
        return
    z = sum(zs) / 4
    for tri in ((0, 1, 2), (0, 2, 3)):
        (ax, ay), (bx, by), (qx, qy) = [(xs[k], ys[k]) for k in tri]
        den = (by - qy) * (ax - qx) + (qx - bx) * (ay - qy)
        if abs(den) < 1e-9:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        l1 = ((by - qy) * (gx - qx) + (qx - bx) * (gy - qy)) / den
        l2 = ((qy - ay) * (gx - qx) + (ax - qx) * (gy - qy)) / den
        l3 = 1 - l1 - l2
        m = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
        sub = zbuf[y0:y1 + 1, x0:x1 + 1]
        m &= z > sub
        sub[m] = z
        img[y0:y1 + 1, x0:x1 + 1][m, :3] = rgb
        img[y0:y1 + 1, x0:x1 + 1][m, 3] = 255

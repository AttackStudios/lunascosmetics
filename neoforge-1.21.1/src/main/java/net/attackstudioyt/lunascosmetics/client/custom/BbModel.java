package net.attackstudioyt.lunascosmetics.client.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Blockbench project (.bbmodel) baked for rendering: the outliner's groups become a
 * bone tree (pivot = group origin), cubes and meshes become triangle soup per bone, and
 * the project's animations come along. Units are Blockbench's (16 = a block, +y up).
 *
 * <p>Adapted from the Verity mod's loader, extended with cube elements (from/to, per-face
 * UV with rotation, element rotation about its origin, inflate) and multiple textures.
 * Any group named "reference" is skipped, so a model can be built around a stand-in head
 * that never shows up in game.
 */
public final class BbModel {

    public final List<Bone> roots = new ArrayList<>();
    public final Map<String, Animation> animations = new LinkedHashMap<>();
    public final List<ResourceLocation> textures = new ArrayList<>();

    public Animation idleAnimation() {
        Animation idle = animations.get("idle");
        if (idle != null) {
            return idle;
        }
        for (Animation a : animations.values()) {
            if ("loop".equals(a.loop)) {
                return a;
            }
        }
        return null;
    }

    // ---- structure ---------------------------------------------------------------------

    public static final class Bone {
        public final String name;
        public final String uuid;
        public final Vector3f pivot;
        public final Vector3f baseRot;
        public final List<Bone> children = new ArrayList<>();
        public final List<Mesh> meshes = new ArrayList<>();

        Bone(String name, String uuid, Vector3f pivot, Vector3f baseRot) {
            this.name = name;
            this.uuid = uuid;
            this.pivot = pivot;
            this.baseRot = baseRot;
        }
    }

    /** Triangle soup for one element and one texture: positions (bb units), uv 0..1, flat normals. */
    public static final class Mesh {
        public final int texture;
        final float[] positions;
        final float[] uvs;
        final float[] normals;

        Mesh(int texture, float[] positions, float[] uvs, float[] normals) {
            this.texture = texture;
            this.positions = positions;
            this.uvs = uvs;
            this.normals = normals;
        }

        public void emit(PoseStack.Pose entry, VertexConsumer vc, int light) {
            int tris = positions.length / 9;
            for (int t = 0; t < tris; t++) {
                int p = t * 9;
                int u = t * 6;
                int n = t * 3;
                // Entity layers draw quads: each triangle goes out as a degenerate quad.
                vertex(entry, vc, p, u, n, light);
                vertex(entry, vc, p + 3, u + 2, n, light);
                vertex(entry, vc, p + 6, u + 4, n, light);
                vertex(entry, vc, p + 6, u + 4, n, light);
            }
        }

        private void vertex(PoseStack.Pose entry, VertexConsumer vc, int p, int u, int n, int light) {
            vc.addVertex(entry, positions[p] / 16.0f, positions[p + 1] / 16.0f, positions[p + 2] / 16.0f)
                    .setColor(255, 255, 255, 255)
                    .setUv(uvs[u], uvs[u + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(entry, normals[n], normals[n + 1], normals[n + 2]);
        }
    }

    // ---- animations ----------------------------------------------------------------------

    public static final class Animation {
        public final String name;
        public final String loop;
        public final float length;
        final Map<String, Channels> animators = new HashMap<>();

        Animation(String name, String loop, float length) {
            this.name = name;
            this.loop = loop;
            this.length = Math.max(0.05f, length);
        }

        /** Evaluate at t seconds into per-bone-uuid transforms {rx,ry,rz,px,py,pz,sx,sy,sz}. */
        public void sample(float t, Map<String, float[]> out) {
            for (Map.Entry<String, Channels> e : animators.entrySet()) {
                float[] tr = out.computeIfAbsent(e.getKey(), k -> new float[]{0, 0, 0, 0, 0, 0, 1, 1, 1});
                e.getValue().sample(t, tr);
            }
        }
    }

    static final class Channels {
        final List<Keyframe> rotation = new ArrayList<>();
        final List<Keyframe> position = new ArrayList<>();
        final List<Keyframe> scale = new ArrayList<>();

        void sample(float t, float[] out) {
            sampleChannel(rotation, t, out, 0);
            sampleChannel(position, t, out, 3);
            sampleChannel(scale, t, out, 6);
        }

        private static void sampleChannel(List<Keyframe> frames, float t, float[] out, int at) {
            if (frames.isEmpty()) {
                return;
            }
            Keyframe prev = frames.get(0);
            Keyframe next = null;
            for (Keyframe k : frames) {
                if (k.time <= t) {
                    prev = k;
                } else {
                    next = k;
                    break;
                }
            }
            if (next == null || prev.step) {
                out[at] = prev.x;
                out[at + 1] = prev.y;
                out[at + 2] = prev.z;
                return;
            }
            float f = (t - prev.time) / Math.max(1e-5f, next.time - prev.time);
            out[at] = prev.x + (next.x - prev.x) * f;
            out[at + 1] = prev.y + (next.y - prev.y) * f;
            out[at + 2] = prev.z + (next.z - prev.z) * f;
        }
    }

    record Keyframe(float time, float x, float y, float z, boolean step) {
    }

    // ---- parsing -------------------------------------------------------------------------

    /**
     * Parses a project and registers its textures. Must run on the render thread.
     *
     * @param key unique, filesystem-safe name used for the texture ids
     */
    public static BbModel parse(String jsonText, String key) {
        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        BbModel model = new BbModel();

        float texW = 16, texH = 16;
        if (root.has("resolution")) {
            JsonObject res = root.getAsJsonObject("resolution");
            texW = res.get("width").getAsFloat();
            texH = res.get("height").getAsFloat();
        }

        // Textures first: per-texture uv sizes can differ from the project resolution.
        List<float[]> texSizes = new ArrayList<>();
        if (root.has("textures")) {
            int i = 0;
            for (JsonElement el : root.getAsJsonArray("textures")) {
                JsonObject tex = el.getAsJsonObject();
                float uw = tex.has("uv_width") ? tex.get("uv_width").getAsFloat() : texW;
                float uh = tex.has("uv_height") ? tex.get("uv_height").getAsFloat() : texH;
                texSizes.add(new float[]{uw, uh});
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "dynamic/" + key + "_" + i++);
                try {
                    String source = tex.get("source").getAsString();
                    byte[] png = Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
                    NativeImage img = NativeImage.read(png);
                    Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
                } catch (Exception e) {
                    LunasCosmetics.LOGGER.warn("custom model texture failed: {}", e.toString());
                    id = ResourceLocation.withDefaultNamespace("textures/block/pink_wool.png");
                }
                model.textures.add(id);
            }
        }
        if (model.textures.isEmpty()) {
            model.textures.add(ResourceLocation.withDefaultNamespace("textures/block/pink_wool.png"));
            texSizes.add(new float[]{texW, texH});
        }

        Map<String, List<Mesh>> meshesByUuid = new HashMap<>();
        if (root.has("elements")) {
            for (JsonElement el : root.getAsJsonArray("elements")) {
                JsonObject e = el.getAsJsonObject();
                if (e.has("visibility") && !e.get("visibility").getAsBoolean()) {
                    continue;
                }
                String type = optString(e, "type");
                List<Mesh> meshes = "mesh".equals(type) ? bakeMesh(e, texSizes)
                        : (type == null || "cube".equals(type)) ? bakeCube(e, texSizes) : List.of();
                meshesByUuid.put(e.get("uuid").getAsString(), meshes);
            }
        }

        Map<String, JsonObject> groupInfo = new HashMap<>();
        if (root.has("groups")) {
            for (JsonElement el : root.getAsJsonArray("groups")) {
                if (el.isJsonObject()) {
                    JsonObject g = el.getAsJsonObject();
                    String uuid = optString(g, "uuid");
                    if (uuid != null) {
                        groupInfo.put(uuid, g);
                    }
                }
            }
        }

        if (root.has("outliner")) {
            Bone loose = new Bone("root", "root", new Vector3f(), new Vector3f());
            for (JsonElement node : root.getAsJsonArray("outliner")) {
                if (node.isJsonObject()) {
                    Bone b = parseBone(node.getAsJsonObject(), meshesByUuid, groupInfo);
                    if (b != null) {
                        model.roots.add(b);
                    }
                } else {
                    // elements at the top level, outside any group
                    List<Mesh> m = meshesByUuid.get(node.getAsString());
                    if (m != null) {
                        loose.meshes.addAll(m);
                    }
                }
            }
            if (!loose.meshes.isEmpty()) {
                model.roots.add(loose);
            }
        } else {
            Bone loose = new Bone("root", "root", new Vector3f(), new Vector3f());
            meshesByUuid.values().forEach(loose.meshes::addAll);
            model.roots.add(loose);
        }

        if (root.has("animations")) {
            for (JsonElement el : root.getAsJsonArray("animations")) {
                parseAnimation(el.getAsJsonObject(), model);
            }
        }
        return model;
    }

    private static void parseAnimation(JsonObject a, BbModel model) {
        Animation anim = new Animation(
                optString(a, "name") == null ? "unnamed" : a.get("name").getAsString(),
                optString(a, "loop") == null ? "once" : a.get("loop").getAsString(),
                a.has("length") ? a.get("length").getAsFloat() : 1.0f);
        if (a.has("animators")) {
            for (Map.Entry<String, JsonElement> entry : a.getAsJsonObject("animators").entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject animator = entry.getValue().getAsJsonObject();
                Channels channels = new Channels();
                if (animator.has("keyframes")) {
                    for (JsonElement kfEl : animator.getAsJsonArray("keyframes")) {
                        JsonObject kf = kfEl.getAsJsonObject();
                        String channel = optString(kf, "channel");
                        List<Keyframe> list = "rotation".equals(channel) ? channels.rotation
                                : "position".equals(channel) ? channels.position
                                : "scale".equals(channel) ? channels.scale : null;
                        if (list == null) {
                            continue;
                        }
                        JsonObject dp = kf.getAsJsonArray("data_points").get(0).getAsJsonObject();
                        list.add(new Keyframe(kf.get("time").getAsFloat(),
                                num(dp, "x"), num(dp, "y"), num(dp, "z"),
                                "step".equals(optString(kf, "interpolation"))));
                    }
                }
                channels.rotation.sort((k1, k2) -> Float.compare(k1.time, k2.time));
                channels.position.sort((k1, k2) -> Float.compare(k1.time, k2.time));
                channels.scale.sort((k1, k2) -> Float.compare(k1.time, k2.time));
                anim.animators.put(entry.getKey(), channels);
            }
        }
        model.animations.put(anim.name, anim);
    }

    private static Bone parseBone(JsonObject node, Map<String, List<Mesh>> meshes, Map<String, JsonObject> groupInfo) {
        String uuid = optString(node, "uuid");
        JsonObject props = node.has("origin") ? node : groupInfo.getOrDefault(uuid, node);
        String name = optString(node, "name");
        if (name == null) {
            name = optString(props, "name");
        }
        if (name != null && name.equalsIgnoreCase("reference")) {
            return null;
        }
        if (props.has("visibility") && !props.get("visibility").getAsBoolean()) {
            return null;
        }
        Bone bone = new Bone(name == null ? "bone" : name, uuid, readVec(props, "origin"), readVec(props, "rotation"));
        if (node.has("children")) {
            for (JsonElement child : node.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    Bone b = parseBone(child.getAsJsonObject(), meshes, groupInfo);
                    if (b != null) {
                        bone.children.add(b);
                    }
                } else {
                    List<Mesh> m = meshes.get(child.getAsString());
                    if (m != null) {
                        bone.meshes.addAll(m);
                    }
                }
            }
        }
        return bone;
    }

    // ---- cubes ---------------------------------------------------------------------------

    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    private static List<Mesh> bakeCube(JsonObject e, List<float[]> texSizes) {
        Vector3f from = readVec(e, "from");
        Vector3f to = readVec(e, "to");
        float inflate = e.has("inflate") ? e.get("inflate").getAsFloat() : 0;
        float x1 = Math.min(from.x, to.x) - inflate, x2 = Math.max(from.x, to.x) + inflate;
        float y1 = Math.min(from.y, to.y) - inflate, y2 = Math.max(from.y, to.y) + inflate;
        float z1 = Math.min(from.z, to.z) - inflate, z2 = Math.max(from.z, to.z) + inflate;
        Vector3f origin = readVec(e, "origin");
        Vector3f rot = readVec(e, "rotation");
        Matrix3f r = new Matrix3f().rotationZYX(
                (float) Math.toRadians(rot.z), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.x));

        Map<Integer, List<float[]>> posByTex = new HashMap<>();
        Map<Integer, List<float[]>> uvByTex = new HashMap<>();
        JsonObject faces = e.has("faces") ? e.getAsJsonObject("faces") : new JsonObject();
        for (String f : FACES) {
            if (!faces.has(f)) {
                continue;
            }
            JsonObject face = faces.getAsJsonObject(f);
            if (!face.has("texture") || face.get("texture").isJsonNull() || !face.has("uv")) {
                continue;
            }
            int tex = face.get("texture").getAsInt();
            if (tex < 0 || tex >= texSizes.size()) {
                tex = 0;
            }
            float[] ts = texSizes.get(tex);
            JsonArray uv = face.getAsJsonArray("uv");
            float u1 = uv.get(0).getAsFloat() / ts[0], v1 = uv.get(1).getAsFloat() / ts[1];
            float u2 = uv.get(2).getAsFloat() / ts[0], v2 = uv.get(3).getAsFloat() / ts[1];
            float[][] corners = faceCorners(f, x1, y1, z1, x2, y2, z2); // TL, TR, BR, BL seen from outside
            float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
            int turns = face.has("rotation") ? (face.get("rotation").getAsInt() / 90) & 3 : 0;
            float[][] q = new float[4][];
            float[][] qu = new float[4][];
            for (int i = 0; i < 4; i++) {
                Vector3f p = new Vector3f(corners[i][0], corners[i][1], corners[i][2]).sub(origin);
                r.transform(p).add(origin);
                q[i] = new float[]{p.x, p.y, p.z};
                qu[i] = uvs[(i - turns + 4) & 3];
            }
            List<float[]> pos = posByTex.computeIfAbsent(tex, k -> new ArrayList<>());
            List<float[]> uvl = uvByTex.computeIfAbsent(tex, k -> new ArrayList<>());
            // two outward-facing triangles: (TL, BL, BR) and (TL, BR, TR)
            for (int i : new int[]{0, 3, 2, 0, 2, 1}) {
                pos.add(q[i]);
                uvl.add(qu[i]);
            }
        }
        List<Mesh> out = new ArrayList<>();
        for (Integer tex : posByTex.keySet()) {
            out.add(finish(tex, posByTex.get(tex), uvByTex.get(tex)));
        }
        return out;
    }

    /** Corners (TL, TR, BR, BL) of a cube face as seen from outside, Blockbench/Minecraft convention. */
    private static float[][] faceCorners(String f, float x1, float y1, float z1, float x2, float y2, float z2) {
        return switch (f) {
            case "north" -> new float[][]{{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}};
            case "south" -> new float[][]{{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}};
            case "east" -> new float[][]{{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case "west" -> new float[][]{{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}};
            case "up" -> new float[][]{{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
            default -> new float[][]{{x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}, {x1, y1, z1}};
        };
    }

    // ---- meshes --------------------------------------------------------------------------

    private static List<Mesh> bakeMesh(JsonObject element, List<float[]> texSizes) {
        JsonObject vertices = element.getAsJsonObject("vertices");
        Vector3f origin = readVec(element, "origin");
        Vector3f rot = readVec(element, "rotation");
        Matrix3f r = new Matrix3f().rotationZYX(
                (float) Math.toRadians(rot.z), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.x));
        Map<String, float[]> verts = new HashMap<>();
        for (Map.Entry<String, JsonElement> v : vertices.entrySet()) {
            JsonArray a = v.getValue().getAsJsonArray();
            // mesh vertices are relative to the element origin
            Vector3f p = new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
            r.transform(p).add(origin);
            verts.put(v.getKey(), new float[]{p.x, p.y, p.z});
        }
        Map<Integer, List<float[]>> posByTex = new HashMap<>();
        Map<Integer, List<float[]>> uvByTex = new HashMap<>();
        for (Map.Entry<String, JsonElement> f : element.getAsJsonObject("faces").entrySet()) {
            JsonObject face = f.getValue().getAsJsonObject();
            if (!face.has("vertices")) {
                continue;
            }
            int tex = face.has("texture") && !face.get("texture").isJsonNull() ? face.get("texture").getAsInt() : 0;
            if (tex < 0 || tex >= texSizes.size()) {
                tex = 0;
            }
            float[] ts = texSizes.get(tex);
            JsonArray ids = face.getAsJsonArray("vertices");
            JsonObject faceUv = face.has("uv") ? face.getAsJsonObject("uv") : null;
            List<float[]> pos = posByTex.computeIfAbsent(tex, k -> new ArrayList<>());
            List<float[]> uvl = uvByTex.computeIfAbsent(tex, k -> new ArrayList<>());
            int n = ids.size();
            for (int i = 1; i + 1 < n; i++) {
                for (String id : new String[]{ids.get(0).getAsString(), ids.get(i).getAsString(), ids.get(i + 1).getAsString()}) {
                    pos.add(verts.get(id));
                    float[] u = new float[]{0, 0};
                    if (faceUv != null && faceUv.has(id)) {
                        JsonArray a = faceUv.getAsJsonArray(id);
                        u[0] = a.get(0).getAsFloat() / ts[0];
                        u[1] = a.get(1).getAsFloat() / ts[1];
                    }
                    uvl.add(u);
                }
            }
        }
        List<Mesh> out = new ArrayList<>();
        for (Integer tex : posByTex.keySet()) {
            out.add(finish(tex, posByTex.get(tex), uvByTex.get(tex)));
        }
        return out;
    }

    private static Mesh finish(int tex, List<float[]> pos, List<float[]> uv) {
        float[] positions = new float[pos.size() * 3];
        float[] uvs = new float[pos.size() * 2];
        for (int i = 0; i < pos.size(); i++) {
            float[] p = pos.get(i);
            positions[i * 3] = p[0];
            positions[i * 3 + 1] = p[1];
            positions[i * 3 + 2] = p[2];
            float[] u = uv.get(i);
            uvs[i * 2] = u[0];
            uvs[i * 2 + 1] = u[1];
        }
        int tris = pos.size() / 3;
        float[] normals = new float[tris * 3];
        Vector3f e1 = new Vector3f();
        Vector3f e2 = new Vector3f();
        for (int t = 0; t < tris; t++) {
            int p = t * 9;
            e1.set(positions[p + 3] - positions[p], positions[p + 4] - positions[p + 1], positions[p + 5] - positions[p + 2]);
            e2.set(positions[p + 6] - positions[p], positions[p + 7] - positions[p + 1], positions[p + 8] - positions[p + 2]);
            e1.cross(e2);
            if (e1.lengthSquared() > 1e-10f) {
                e1.normalize();
            } else {
                e1.set(0, 1, 0);
            }
            normals[t * 3] = e1.x;
            normals[t * 3 + 1] = e1.y;
            normals[t * 3 + 2] = e1.z;
        }
        return new Mesh(tex, positions, uvs, normals);
    }

    private static Vector3f readVec(JsonObject obj, String key) {
        Vector3f v = new Vector3f();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            JsonArray a = obj.getAsJsonArray(key);
            v.set(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
        }
        return v;
    }

    private static String optString(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    /** Blockbench keyframe values can be numbers or math-expression strings. */
    private static float num(JsonObject dp, String key) {
        if (!dp.has(key)) {
            return 0;
        }
        try {
            return dp.get(key).getAsFloat();
        } catch (Exception e) {
            try {
                return Float.parseFloat(dp.get(key).getAsString().trim());
            } catch (Exception e2) {
                return 0;
            }
        }
    }
}

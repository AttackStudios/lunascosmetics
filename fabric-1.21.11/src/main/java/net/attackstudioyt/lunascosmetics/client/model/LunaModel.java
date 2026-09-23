package net.attackstudioyt.lunascosmetics.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cosmetic model built from the mod's own JSON format (models/cosmetic/*.json), the same
 * files tools/art.py paints textures for. Parts are addressed by name, and every draw
 * carries its own {@link Pose}: the render queue calls {@link #setAngles} at flush time,
 * so the pose has to travel with the submission rather than live on the parts.
 */
@Environment(EnvType.CLIENT)
public class LunaModel extends Model<LunaModel.Pose> {
    private static final Map<String, LunaModel> CACHE = new HashMap<>();

    private final List<String> names = new ArrayList<>();
    private final List<ModelPart> parts = new ArrayList<>();
    private final Map<String, Integer> index = new HashMap<>();

    private LunaModel(ModelPart root, List<String> order) {
        super(root, RenderLayers::entityCutoutNoCull);
        for (String n : order) {
            ModelPart p = find(root, n);
            if (p != null) {
                index.put(n, parts.size());
                names.add(n);
                parts.add(p);
            }
        }
    }

    /** Loads (once) a model shipped in the jar, e.g. "cat". */
    public static LunaModel get(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            String path = "/assets/" + LunasCosmetics.MOD_ID + "/models/cosmetic/" + n + ".json";
            try (InputStream in = LunaModel.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("missing " + path);
                }
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                return build(json);
            } catch (Exception e) {
                throw new IllegalStateException("bad cosmetic model " + n, e);
            }
        });
    }

    static LunaModel build(JsonObject json) {
        JsonArray size = json.getAsJsonArray("texture_size");
        ModelData data = new ModelData();
        List<String> order = new ArrayList<>();
        for (JsonElement p : json.getAsJsonArray("parts")) {
            addPart(data.getRoot(), p.getAsJsonObject(), order);
        }
        ModelPart root = TexturedModelData.of(data, size.get(0).getAsInt(), size.get(1).getAsInt()).createModel();
        return new LunaModel(root, order);
    }

    private static void addPart(ModelPartData parent, JsonObject p, List<String> order) {
        String name = p.get("name").getAsString();
        ModelPartBuilder b = ModelPartBuilder.create();
        if (p.has("cubes")) {
            for (JsonElement c : p.getAsJsonArray("cubes")) {
                JsonObject cube = c.getAsJsonObject();
                JsonArray uv = cube.getAsJsonArray("uv");
                JsonArray from = cube.getAsJsonArray("from");
                JsonArray sz = cube.getAsJsonArray("size");
                b.uv(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                        .cuboid(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                                sz.get(0).getAsFloat(), sz.get(1).getAsFloat(), sz.get(2).getAsFloat());
            }
        }
        JsonArray pv = p.getAsJsonArray("pivot");
        float rx = 0, ry = 0, rz = 0;
        if (p.has("rot")) {
            JsonArray r = p.getAsJsonArray("rot");
            rx = (float) Math.toRadians(r.get(0).getAsFloat());
            ry = (float) Math.toRadians(r.get(1).getAsFloat());
            rz = (float) Math.toRadians(r.get(2).getAsFloat());
        }
        ModelPartData child = parent.addChild(name, b,
                ModelTransform.of(pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat(), rx, ry, rz));
        order.add(name);
        if (p.has("children")) {
            for (JsonElement ch : p.getAsJsonArray("children")) {
                addPart(child, ch.getAsJsonObject(), order);
            }
        }
    }

    private static ModelPart find(ModelPart part, String name) {
        if (part.hasChild(name)) {
            return part.getChild(name);
        }
        for (ModelPart child : part.traverse()) {
            if (child != part && child.hasChild(name)) {
                return child.getChild(name);
            }
        }
        return null;
    }

    public int indexOf(String name) {
        Integer i = index.get(name);
        return i == null ? -1 : i;
    }

    public int partCount() {
        return parts.size();
    }

    public Pose newPose() {
        return new Pose(this);
    }

    @Override
    public void setAngles(Pose pose) {
        super.setAngles(pose); // back to the rest pose
        if (pose == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            ModelPart p = parts.get(i);
            int o = i * Pose.STRIDE;
            float[] v = pose.values;
            p.pitch += v[o];
            p.yaw += v[o + 1];
            p.roll += v[o + 2];
            p.originX += v[o + 3];
            p.originY += v[o + 4];
            p.originZ += v[o + 5];
            p.xScale *= v[o + 6];
            p.yScale *= v[o + 7];
            p.zScale *= v[o + 8];
            p.visible = pose.visible[i];
        }
    }

    /**
     * Additive offsets from the rest pose, one slot per named part: rotation (radians),
     * origin shift (pixels) and multiplicative scale.
     */
    public static final class Pose {
        static final int STRIDE = 9;
        final LunaModel model;
        final float[] values;
        final boolean[] visible;

        Pose(LunaModel model) {
            this.model = model;
            this.values = new float[model.partCount() * STRIDE];
            this.visible = new boolean[model.partCount()];
            for (int i = 0; i < model.partCount(); i++) {
                values[i * STRIDE + 6] = 1;
                values[i * STRIDE + 7] = 1;
                values[i * STRIDE + 8] = 1;
                visible[i] = true;
            }
        }

        private int at(String part) {
            int i = model.indexOf(part);
            return i < 0 ? -1 : i * STRIDE;
        }

        public Pose rot(String part, float pitch, float yaw, float roll) {
            int o = at(part);
            if (o >= 0) {
                values[o] += pitch;
                values[o + 1] += yaw;
                values[o + 2] += roll;
            }
            return this;
        }

        public Pose rotDeg(String part, float pitch, float yaw, float roll) {
            return rot(part, (float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll));
        }

        public Pose move(String part, float x, float y, float z) {
            int o = at(part);
            if (o >= 0) {
                values[o + 3] += x;
                values[o + 4] += y;
                values[o + 5] += z;
            }
            return this;
        }

        public Pose scale(String part, float x, float y, float z) {
            int o = at(part);
            if (o >= 0) {
                values[o + 6] *= x;
                values[o + 7] *= y;
                values[o + 8] *= z;
            }
            return this;
        }

        public Pose hide(String part) {
            int i = model.indexOf(part);
            if (i >= 0) {
                visible[i] = false;
            }
            return this;
        }
    }
}

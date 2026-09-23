package net.attackstudioyt.lunascosmetics.client.custom;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.RenderCtx;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

/**
 * One of Luna's own Blockbench creations. Its "idle" animation (or first looping one)
 * plays forever. The origin of the Blockbench scene sits on the cosmetic's anchor: the
 * top-centre of the head for hats and pets, the back of the neck for back pieces.
 */
public class CustomCosmetic extends Cosmetic {
    public final String hash;
    public final String fileName;
    private final BbModel model;
    private final Map<String, float[]> pose = new HashMap<>();

    public CustomCosmetic(String hash, String fileName, String name, Slot slot, BbModel model) {
        super("custom:" + hash, name, "Made by you! (" + fileName + ")", slot, 0xFFFFD36A);
        this.hash = hash;
        this.fileName = fileName;
        this.model = model;
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    @Override
    public float cardScale() {
        return 1.4f;
    }

    @Override
    public void render(RenderCtx ctx) {
        pose.clear();
        BbModel.Animation idle = model.idleAnimation();
        if (idle != null) {
            idle.sample(ctx.brain().time % idle.length, pose);
        }
        MatrixStack m = ctx.sink().matrices();
        m.push();
        // entity model space is y-down and mirrored; Blockbench is y-up
        m.scale(-1, -1, 1);
        if (!ctx.perch().onHead() && slot == Slot.PET) {
            m.scale(0.62f, 0.62f, 0.62f);
        }
        int light = ctx.sink().light();
        for (BbModel.Bone root : model.roots) {
            renderBone(ctx, root, m, light);
        }
        m.pop();
    }

    private void renderBone(RenderCtx ctx, BbModel.Bone bone, MatrixStack m, int light) {
        m.push();
        float[] tr = pose.get(bone.uuid);
        float rx = bone.baseRot.x, ry = bone.baseRot.y, rz = bone.baseRot.z;
        float ox = 0, oy = 0, oz = 0, sx = 1, sy = 1, sz = 1;
        if (tr != null) {
            rx += tr[0];
            ry += tr[1];
            rz += tr[2];
            ox = tr[3] / 16f;
            oy = tr[4] / 16f;
            oz = tr[5] / 16f;
            sx = tr[6];
            sy = tr[7];
            sz = tr[8];
        }
        float px = bone.pivot.x / 16f, py = bone.pivot.y / 16f, pz = bone.pivot.z / 16f;
        m.translate(px + ox, py + oy, pz + oz);
        if (rz != 0) {
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
        }
        if (ry != 0) {
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        }
        if (rx != 0) {
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        }
        if (sx != 1 || sy != 1 || sz != 1) {
            m.scale(sx, sy, sz);
        }
        m.translate(-px, -py, -pz);
        for (BbModel.Mesh mesh : bone.meshes) {
            RenderLayer layer = RenderLayers.entityCutoutNoCull(model.textures.get(Math.min(mesh.texture, model.textures.size() - 1)));
            ctx.sink().custom(layer, (entry, vc) -> mesh.emit(entry, vc, light));
        }
        for (BbModel.Bone child : bone.children) {
            renderBone(ctx, child, m, light);
        }
        m.pop();
    }
}

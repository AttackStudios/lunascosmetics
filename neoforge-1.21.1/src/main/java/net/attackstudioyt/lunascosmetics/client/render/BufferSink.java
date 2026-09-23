package net.attackstudioyt.lunascosmetics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

/** Draws straight into a buffer source: entity layers and the wardrobe preview alike. */
public final class BufferSink implements CosmeticSink {
    private final PoseStack matrices;
    private final MultiBufferSource buffers;
    private final int light;

    public BufferSink(PoseStack matrices, MultiBufferSource buffers, int light) {
        this.matrices = matrices;
        this.buffers = buffers;
        this.light = light;
    }

    @Override
    public PoseStack matrices() {
        return matrices;
    }

    @Override
    public int light() {
        return light;
    }

    @Override
    public void model(LunaModel model, LunaModel.Pose pose, ResourceLocation texture, int argb) {
        model.render(pose, matrices, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
                light, OverlayTexture.NO_OVERLAY, argb);
    }

    @Override
    public void glow(LunaModel model, LunaModel.Pose pose, ResourceLocation texture, int argb) {
        model.render(pose, matrices, buffers.getBuffer(RenderType.eyes(texture)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
    }

    @Override
    public void custom(RenderType layer, BiConsumer<PoseStack.Pose, VertexConsumer> emitter) {
        emitter.accept(matrices.last(), buffers.getBuffer(layer));
    }
}

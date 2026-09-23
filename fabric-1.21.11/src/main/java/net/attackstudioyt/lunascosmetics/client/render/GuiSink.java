package net.attackstudioyt.lunascosmetics.client.render;

import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/** Draws immediately into a GUI picture-in-picture buffer. */
public final class GuiSink implements CosmeticSink {
    private final MatrixStack matrices;
    private final VertexConsumerProvider consumers;

    public GuiSink(MatrixStack matrices, VertexConsumerProvider consumers) {
        this.matrices = matrices;
        this.consumers = consumers;
    }

    @Override
    public MatrixStack matrices() {
        return matrices;
    }

    @Override
    public int light() {
        return LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    @Override
    public void model(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb) {
        model.setAngles(pose);
        model.render(matrices, consumers.getBuffer(RenderLayers.entityCutoutNoCull(texture)),
                light(), OverlayTexture.DEFAULT_UV, argb);
    }

    @Override
    public void glow(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb) {
        model.setAngles(pose);
        model.render(matrices, consumers.getBuffer(RenderLayers.eyes(texture)),
                light(), OverlayTexture.DEFAULT_UV, argb);
    }

    @Override
    public void custom(RenderLayer layer, BiConsumer<MatrixStack.Entry, VertexConsumer> emitter) {
        emitter.accept(matrices.peek(), consumers.getBuffer(layer));
    }
}

package net.attackstudioyt.lunascosmetics.client.render;

import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/** Submits to the frame's ordered render queue (entity feature rendering). */
public final class WorldSink implements CosmeticSink {
    private final MatrixStack matrices;
    private final OrderedRenderCommandQueue queue;
    private final int light;

    public WorldSink(MatrixStack matrices, OrderedRenderCommandQueue queue, int light) {
        this.matrices = matrices;
        this.queue = queue;
        this.light = light;
    }

    @Override
    public MatrixStack matrices() {
        return matrices;
    }

    @Override
    public int light() {
        return light;
    }

    @Override
    public void model(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb) {
        queue.getBatchingQueue(0).submitModel(model, pose, matrices, RenderLayers.entityCutoutNoCull(texture),
                light, OverlayTexture.DEFAULT_UV, argb, null, 0, null);
    }

    @Override
    public void glow(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb) {
        queue.getBatchingQueue(1).submitModel(model, pose, matrices, RenderLayers.eyes(texture),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, argb, null, 0, null);
    }

    @Override
    public void custom(RenderLayer layer, BiConsumer<MatrixStack.Entry, VertexConsumer> emitter) {
        queue.submitCustom(matrices, layer, emitter::accept);
    }
}

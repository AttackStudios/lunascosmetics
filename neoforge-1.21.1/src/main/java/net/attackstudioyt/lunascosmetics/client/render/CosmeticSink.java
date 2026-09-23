package net.attackstudioyt.lunascosmetics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

/**
 * Where cosmetic geometry goes. 1.21.1 draws immediately, so one implementation
 * ({@link BufferSink}) serves both the world and the wardrobe preview.
 */
public interface CosmeticSink {
    PoseStack matrices();

    int light();

    /** Normal lit cutout pass. {@code argb} tints (-1 = none). */
    void model(LunaModel model, LunaModel.Pose pose, ResourceLocation texture, int argb);

    /** Fullbright additive pass for glowing eyes, stars and the like. */
    void glow(LunaModel model, LunaModel.Pose pose, ResourceLocation texture, int argb);

    /** Raw geometry (custom Blockbench models). */
    void custom(RenderType layer, BiConsumer<PoseStack.Pose, VertexConsumer> emitter);
}

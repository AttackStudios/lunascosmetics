package net.attackstudioyt.lunascosmetics.client.render;

import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.function.BiConsumer;

/**
 * Where cosmetic geometry goes. In the world that's the deferred render queue; in the
 * wardrobe it's an immediate vertex consumer inside a GUI picture-in-picture pass. The
 * cosmetics themselves never know which.
 */
public interface CosmeticSink {
    MatrixStack matrices();

    int light();

    /** Normal lit cutout pass. {@code argb} tints (-1 = none). */
    void model(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb);

    /** Fullbright additive pass for glowing eyes, stars and the like. */
    void glow(LunaModel model, LunaModel.Pose pose, Identifier texture, int argb);

    /** Raw geometry (custom Blockbench models). */
    void custom(RenderLayer layer, BiConsumer<MatrixStack.Entry, VertexConsumer> emitter);
}

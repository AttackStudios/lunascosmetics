package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.render.CosmeticRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Nametags float above a pet sitting on the head instead of through it. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyVariable(method = "renderNameTag", at = @At("STORE"), ordinal = 0)
    private Vec3 lunascosmetics$liftName(Vec3 pos, Entity entity) {
        if (pos != null && entity instanceof AbstractClientPlayer player) {
            float lift = CosmeticRenderer.nameLift(player);
            if (lift > 0) {
                return pos.add(0, lift, 0);
            }
        }
        return pos;
    }
}

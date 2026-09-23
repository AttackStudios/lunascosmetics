package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.render.CosmeticFeatureRenderer;
import net.attackstudioyt.lunascosmetics.client.render.CosmeticRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hangs the cosmetics layer on every player renderer (wide and slim). */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
    private PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadow) {
        super(ctx, model, shadow);
    }

    /** Nametags float above a pet sitting on the head instead of through it. */
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void lunascosmetics$liftName(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta,
                                         CallbackInfo ci) {
        if (state.nameLabelPos != null && entity instanceof AbstractClientPlayerEntity player) {
            float lift = CosmeticRenderer.nameLift(player);
            if (lift > 0) {
                state.nameLabelPos = state.nameLabelPos.add(0, lift, 0);
            }
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lunascosmetics$addLayer(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.addFeature(new CosmeticFeatureRenderer((PlayerEntityRenderer) (Object) this));
    }
}

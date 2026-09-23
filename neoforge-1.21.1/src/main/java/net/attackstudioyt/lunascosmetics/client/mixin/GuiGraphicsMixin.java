package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Cherry Cat theme: pink twins for vanilla GUI sprites and textures. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(method = {
            "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V",
            "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIIII)V"
    }, at = @At("HEAD"), argsOnly = true)
    private ResourceLocation lunascosmetics$themeSprite(ResourceLocation sprite) {
        return Theme.sprite(sprite);
    }

    @ModifyVariable(method = "blit(Lnet/minecraft/resources/ResourceLocation;IIIIIIIFFII)V",
            at = @At("HEAD"), argsOnly = true)
    private ResourceLocation lunascosmetics$themeTexture(ResourceLocation texture) {
        return Theme.texture(texture);
    }
}

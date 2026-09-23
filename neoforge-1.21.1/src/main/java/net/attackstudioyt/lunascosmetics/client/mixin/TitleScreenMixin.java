package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.gui.Petals;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The title screen paints its panorama itself; petals go right on top of it. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderPanorama", at = @At("TAIL"))
    private void lunascosmetics$titlePetals(GuiGraphics ctx, float delta, CallbackInfo ci) {
        if (Theme.on()) {
            ctx.fillGradient(0, 0, width, height, 0x38FF8FB4, 0x50FFB3CC);
            Petals.menus().render(ctx, width, height, -999, -999, 1.0f);
        }
    }
}

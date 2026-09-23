package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.gui.Petals;
import net.attackstudioyt.lunascosmetics.client.gui.WardrobeScreen;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cherry Cat theme: a blush of pink and drifting petals behind every menu. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void lunascosmetics$petals(GuiGraphics ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Theme.on() || (Object) this instanceof WardrobeScreen || (Object) this instanceof TitleScreen) {
            return;
        }
        ctx.fillGradient(0, 0, width, height, 0x30FF8FB4, 0x40FFC1D6);
        Petals.menus().render(ctx, width, height, mouseX, mouseY, 0.85f);
    }
}

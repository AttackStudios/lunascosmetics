package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.gui.CatButton;
import net.attackstudioyt.lunascosmetics.client.gui.Ui;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cherry Cat theme: two little cat ears pop up over whichever button you hover. */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
    @Unique
    private float lunascosmetics$ears;
    @Unique
    private long lunascosmetics$last;

    @Inject(method = "renderWidget", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void lunascosmetics$ears(GuiGraphics ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractWidget self = (AbstractWidget) (Object) this;
        if (!Theme.on() || self.getWidth() < 40 || (Object) this instanceof CatButton) {
            return;
        }
        long now = System.nanoTime();
        float dt = lunascosmetics$last == 0 ? 0 : Mth.clamp((now - lunascosmetics$last) / 1e9f, 0, 0.1f);
        lunascosmetics$last = now;
        boolean on = self.isHoveredOrFocused() && self.active;
        lunascosmetics$ears += ((on ? 1 : 0) - lunascosmetics$ears) * Math.min(1, dt * 16);
        if (lunascosmetics$ears < 0.05f) {
            return;
        }
        // ears rise from behind the top edge, with a tiny overshoot
        float e = lunascosmetics$ears;
        float rise = e * 5 + Mth.sin(e * Mth.PI) * 1.2f;
        int y = self.getY() - Math.round(rise);
        ctx.enableScissor(self.getX(), self.getY() - 7, self.getX() + self.getWidth(), self.getY());
        Ui.sprite(ctx, "ear_left", self.getX() + 5, y, 5, 5);
        Ui.sprite(ctx, "ear_right", self.getX() + self.getWidth() - 10, y, 5, 5);
        ctx.disableScissor();
    }
}

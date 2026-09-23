package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.gui.CatButton;
import net.attackstudioyt.lunascosmetics.client.gui.Ui;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With the theme on, every button click comes with a tiny mew. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "playDownSound", at = @At("TAIL"))
    private void lunascosmetics$mew(SoundManager soundManager, CallbackInfo ci) {
        ClientConfig c = ClientConfig.get();
        if (c.theme && c.meowClicks && !((Object) this instanceof CatButton)) {
            Ui.play(SoundEvents.CAT_AMBIENT, 1.55f + Ui.rand() * 0.3f, 0.22f);
        }
    }
}

package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.gui.Ui;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With the theme on, every button click comes with a tiny mew. */
@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {
    @Inject(method = "playClickSound", at = @At("TAIL"))
    private static void lunascosmetics$mew(SoundManager soundManager, CallbackInfo ci) {
        ClientConfig c = ClientConfig.get();
        if (c.theme && c.meowClicks) {
            Ui.play(SoundEvents.ENTITY_CAT_AMBIENT, 1.55f + Ui.rand() * 0.3f, 0.22f);
        }
    }
}

package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.gui.CatButton;
import net.attackstudioyt.lunascosmetics.client.gui.MenuButtons;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The cat button just left of Options in the pause menu. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void lunascosmetics$addCat(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        if (!self.shouldShowMenu()) {
            return;
        }
        ButtonWidget options = MenuButtons.options(this.children());
        if (options == null) {
            return;
        }
        this.addDrawableChild(new CatButton(options.getX() - 24, options.getY(), this));
    }
}

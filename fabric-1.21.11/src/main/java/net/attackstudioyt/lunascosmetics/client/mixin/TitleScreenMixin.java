package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.gui.CatButton;
import net.attackstudioyt.lunascosmetics.client.gui.MenuButtons;
import net.attackstudioyt.lunascosmetics.client.gui.TitleCat;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The cat button left of the language globe on the Options row, and the logo cat. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void lunascosmetics$addCat(CallbackInfo ci) {
        ButtonWidget options = MenuButtons.options(this.children());
        int y = options != null ? options.getY() : this.height / 4 + 48 + 72 + 12;
        int x = options != null ? options.getX() - 24 - 24 : this.width / 2 - 148;
        this.addDrawableChild(new CatButton(x, y, (Screen) (Object) this));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void lunascosmetics$logoCat(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Theme.on()) {
            TitleCat.INSTANCE.render(ctx, this.width, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void lunascosmetics$pokeCat(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (Theme.on() && click.button() == 0 && TitleCat.INSTANCE.click(click.x(), click.y())) {
            cir.setReturnValue(true);
        }
    }
}

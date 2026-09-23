package net.attackstudioyt.lunascosmetics.client.mixin;

import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Cherry Cat theme: pink twins for vanilla GUI art, and paw cursors. */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @ModifyVariable(method = "drawGuiTexture*", at = @At("HEAD"), argsOnly = true)
    private Identifier lunascosmetics$themeSprite(Identifier sprite) {
        return Theme.sprite(sprite);
    }

    @ModifyVariable(method = "drawTexture*", at = @At("HEAD"), argsOnly = true)
    private Identifier lunascosmetics$themeTexture(Identifier texture) {
        return Theme.texture(texture);
    }

    @ModifyArg(method = "applyCursorTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/Window;setCursor(Lnet/minecraft/client/gui/cursor/Cursor;)V"))
    private Cursor lunascosmetics$pawCursor(Cursor cursor) {
        return Theme.cursor(cursor);
    }
}

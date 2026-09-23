package net.attackstudioyt.lunascosmetics.client.mixin;

import net.minecraft.client.gui.cursor.Cursor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Cursor.class)
public interface CursorAccessor {
    @Invoker("<init>")
    static Cursor create(String name, long handle) {
        throw new AssertionError();
    }
}

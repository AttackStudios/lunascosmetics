package net.attackstudioyt.lunascosmetics.client.gui;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Finds the vanilla "Options..." button so the cat can sit right next to it. */
public final class MenuButtons {
    private MenuButtons() {
    }

    public static ButtonWidget options(List<? extends Element> children) {
        String options = Text.translatable("menu.options").getString();
        for (Element e : children) {
            if (e instanceof ButtonWidget b && b.getMessage().getString().equals(options)) {
                return b;
            }
        }
        return null;
    }
}

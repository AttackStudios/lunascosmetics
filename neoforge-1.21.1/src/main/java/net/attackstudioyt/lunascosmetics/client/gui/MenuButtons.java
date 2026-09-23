package net.attackstudioyt.lunascosmetics.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Finds the vanilla "Options..." button so the cat can sit right next to it. */
public final class MenuButtons {
    private MenuButtons() {
    }

    public static Button options(List<? extends GuiEventListener> children) {
        String options = Component.translatable("menu.options").getString();
        for (GuiEventListener e : children) {
            if (e instanceof Button b && b.getMessage().getString().equals(options)) {
                return b;
            }
        }
        return null;
    }
}

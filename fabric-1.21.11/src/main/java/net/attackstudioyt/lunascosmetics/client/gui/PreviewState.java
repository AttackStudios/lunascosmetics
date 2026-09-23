package net.attackstudioyt.lunascosmetics.client.gui;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One live 3D picture in the wardrobe: either Luna herself wearing a loadout, or a single
 * cosmetic on a card.
 *
 * @param cosmeticId null = draw the whole player wearing {@code loadout}
 */
public record PreviewState(
        @Nullable String cosmeticId,
        Loadout loadout,
        Object brainKey,
        float yaw,
        float pitch,
        Identifier skin,
        boolean slim,
        float zoom,
        int x1, int y1, int x2, int y2,
        float scale,
        @Nullable ScreenRect scissorArea,
        @Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

    public static PreviewState of(@Nullable String cosmeticId, Loadout loadout, Object brainKey, float yaw, float pitch,
                                  Identifier skin, boolean slim, float zoom,
                                  int x1, int y1, int x2, int y2, float scale, @Nullable ScreenRect scissor) {
        return new PreviewState(cosmeticId, loadout, brainKey, yaw, pitch, skin, slim, zoom, x1, y1, x2, y2, scale, scissor,
                SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissor));
    }
}

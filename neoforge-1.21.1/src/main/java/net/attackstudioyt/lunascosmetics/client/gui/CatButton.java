package net.attackstudioyt.lunascosmetics.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The little cat that opens Luna's Cosmetics, next to Options. With the Cherry Cat theme
 * on it *is* a cherry-pink cat: it breathes, blinks, twitches an ear, perks up and wiggles
 * when you hover, and meows (with a puff of hearts) when you click. With the theme off
 * it's a normal button with a white kitty on it.
 */
public class CatButton extends Button {
    private final Screen parent;
    private float hover;
    private float nextBlink = 1.2f, blinkUntil;
    private float nextTwitch = 3f, twitchUntil;
    private float meowAt = -9;
    private float openAt = -1;
    private long last;
    private float t;
    private final List<float[]> hearts = new ArrayList<>(); // x, y, vx, vy, age

    public CatButton(int x, int y, Screen parent) {
        super(x, y, 20, 20, Component.translatable("lunascosmetics.title"), b -> {
        }, DEFAULT_NARRATION);
        this.parent = parent;
        this.setTooltip(Tooltip.create(Component.translatable("lunascosmetics.button.tooltip")));
    }

    @Override
    public void onPress() {
        if (openAt >= 0) {
            return;
        }
        meowAt = t;
        openAt = t + 0.28f;
        Ui.play(SoundEvents.CAT_AMBIENT, 1.35f + Ui.rand() * 0.25f, 0.6f);
        for (int i = 0; i < 6; i++) {
            float a = -Mth.PI / 2 + (Ui.rand() - 0.5f) * 2.2f;
            hearts.add(new float[]{getX() + 10, getY() + 6, Mth.cos(a) * (18 + Ui.rand() * 20),
                    Mth.sin(a) * (28 + Ui.rand() * 20), 0});
        }
        if (ClientConfig.get().theme) {
            Petals.menus().burst(getX() + 10, getY() + 10, 10);
        }
    }

    // the base click sound is replaced by the meow
    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Mth.clamp((now - last) / 1e9f, 0, 0.1f);
        last = now;
        t += dt;
        boolean hovered = this.isHoveredOrFocused();
        hover += ((hovered ? 1 : 0) - hover) * Math.min(1, dt * 12);

        if (t >= nextBlink) {
            blinkUntil = t + 0.12f;
            nextBlink = t + 2 + Ui.rand() * 3.5f;
        }
        if (t >= nextTwitch) {
            twitchUntil = t + 0.2f;
            nextTwitch = t + 3 + Ui.rand() * 5;
        }
        if (openAt >= 0 && t >= openAt) {
            openAt = -1;
            Minecraft.getInstance().setScreen(new WardrobeScreen(parent));
        }

        boolean theme = ClientConfig.get().theme;
        String pal = theme ? "cherry" : "plain";
        String face = "open";
        if (t - meowAt < 0.45f) {
            face = "meow";
        } else if (hover > 0.5f) {
            face = "happy";
        } else if (t < blinkUntil) {
            face = "blink";
        } else if (t < twitchUntil) {
            face = "twitch";
        }

        int cx = getX() + 10, cy = getY() + 10;
        if (!theme) {
            // the vanilla button background
            ctx.setColor(1, 1, 1, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            ctx.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight());
            ctx.setColor(1, 1, 1, 1);
        } else {
            // a soft glow behind the cat instead of a button
            int glow = Ui.alpha(0xFFFF9FC0, 0.25f + 0.35f * hover);
            Ui.round(ctx, getX() - 1, getY() - 1, getX() + 21, getY() + 21, 6, glow);
        }

        PoseStack m = ctx.pose();
        m.pushPose();
        float breathe = 1 + Mth.sin(t * 2.4f) * 0.025f;
        float pop = 1 + hover * 0.14f;
        float press = t - meowAt < 0.3f ? 1 - Mth.sin((t - meowAt) / 0.3f * Mth.PI) * 0.18f : 1;
        float bob = theme ? Mth.sin(t * 2.4f) * 0.6f - hover * 1.5f : 0;
        float wiggle = hover * Mth.sin(t * 11) * 0.09f;
        m.translate(cx, cy + bob, 0);
        m.mulPose(Axis.ZP.rotation(wiggle));
        m.scale(breathe * pop * (2 - press), pop * press * breathe, 1);
        int size = theme ? 18 : 16;
        Ui.sprite(ctx, "cat_" + pal + "_" + face, -size / 2, -size / 2, size, size, Ui.alpha(0xFFFFFFFF, this.alpha));
        m.popPose();

        // hearts float up and fade
        for (int i = hearts.size() - 1; i >= 0; i--) {
            float[] h = hearts.get(i);
            h[4] += dt;
            if (h[4] > 0.9f) {
                hearts.remove(i);
                continue;
            }
            h[0] += h[2] * dt;
            h[1] += h[3] * dt;
            h[3] += 20 * dt;
            float a = 1 - h[4] / 0.9f;
            Ui.sprite(ctx, "icon_heart", (int) h[0] - 4, (int) h[1] - 4, 9, 9, Ui.alpha(0xFFFFFFFF, a));
        }
    }
}

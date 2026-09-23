package net.attackstudioyt.lunascosmetics.client.gui;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The little cat that opens Luna's Cosmetics, next to Options. With the Cherry Cat theme
 * on it *is* a cherry-pink cat: it breathes, blinks, twitches an ear, perks up and wiggles
 * when you hover, and meows (with a puff of hearts) when you click. With the theme off
 * it's a normal button with a white kitty on it.
 */
public class CatButton extends ButtonWidget {
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
        super(x, y, 20, 20, net.minecraft.text.Text.translatable("lunascosmetics.title"), b -> {
        }, DEFAULT_NARRATION_SUPPLIER);
        this.parent = parent;
        this.setTooltip(Tooltip.of(net.minecraft.text.Text.translatable("lunascosmetics.button.tooltip")));
    }

    @Override
    public void onPress(AbstractInput input) {
        if (openAt >= 0) {
            return;
        }
        meowAt = t;
        openAt = t + 0.28f;
        Ui.play(SoundEvents.ENTITY_CAT_AMBIENT, 1.35f + Ui.rand() * 0.25f, 0.6f);
        for (int i = 0; i < 6; i++) {
            float a = -MathHelper.PI / 2 + (Ui.rand() - 0.5f) * 2.2f;
            hearts.add(new float[]{getX() + 10, getY() + 6, MathHelper.cos(a) * (18 + Ui.rand() * 20),
                    MathHelper.sin(a) * (28 + Ui.rand() * 20), 0});
        }
        if (ClientConfig.get().theme) {
            Petals.menus().burst(getX() + 10, getY() + 10, 10);
        }
    }

    // the base click sound is replaced by the meow
    @Override
    public void playDownSound(net.minecraft.client.sound.SoundManager soundManager) {
    }

    @Override
    protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : MathHelper.clamp((now - last) / 1e9f, 0, 0.1f);
        last = now;
        t += dt;
        boolean hovered = this.isSelected();
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
            MinecraftClient.getInstance().setScreen(new WardrobeScreen(parent));
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
            this.drawButton(ctx);
        } else {
            // a soft glow behind the cat instead of a button
            int glow = Ui.alpha(0xFFFF9FC0, 0.25f + 0.35f * hover);
            Ui.round(ctx, getX() - 1, getY() - 1, getX() + 21, getY() + 21, 6, glow);
        }

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        float breathe = 1 + MathHelper.sin(t * 2.4f) * 0.025f;
        float pop = 1 + hover * 0.14f;
        float press = t - meowAt < 0.3f ? 1 - MathHelper.sin((t - meowAt) / 0.3f * MathHelper.PI) * 0.18f : 1;
        float bob = theme ? MathHelper.sin(t * 2.4f) * 0.6f - hover * 1.5f : 0;
        float wiggle = hover * MathHelper.sin(t * 11) * 0.09f * (1 - Math.min(1, (t - meowAt) * 3) * 0);
        m.translate(cx, cy + bob);
        m.rotate(wiggle);
        m.scale(breathe * pop * (2 - press), pop * press * breathe);
        int size = theme ? 18 : 16;
        Ui.sprite(ctx, "cat_" + pal + "_" + face, -size / 2, -size / 2, size, size, Ui.alpha(0xFFFFFFFF, this.alpha));
        m.popMatrix();

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

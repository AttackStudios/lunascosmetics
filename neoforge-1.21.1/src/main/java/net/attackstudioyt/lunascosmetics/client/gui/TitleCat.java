package net.attackstudioyt.lunascosmetics.client.gui;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * A cherry cat perched on the corner of the Minecraft logo (theme only). Swishes its tail,
 * blinks, dozes off if you leave the mouse alone, and meows when poked.
 */
public final class TitleCat {
    public static final TitleCat INSTANCE = new TitleCat();

    private float t;
    private long last;
    private float nextBlink = 1, blinkUntil;
    private float idle;
    private int lastMx = -1, lastMy = -1;
    private float pokeAt = -9;
    private final List<float[]> fx = new ArrayList<>(); // x, y, vx, vy, age, kind(0 heart, 1 z)
    private float nextZ;

    private int x, y;
    private static final int SCALE = 2;

    public void render(GuiGraphics ctx, int screenWidth, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Mth.clamp((now - last) / 1e9f, 0, 0.1f);
        last = now;
        t += dt;
        if (mouseX != lastMx || mouseY != lastMy) {
            idle = 0;
            lastMx = mouseX;
            lastMy = mouseY;
        } else {
            idle += dt;
        }
        boolean asleep = idle > 15;
        if (t >= nextBlink) {
            blinkUntil = t + 0.12f;
            nextBlink = t + 2.2f + (float) Math.random() * 3.5f;
        }

        // sits on the top edge of the logo, towards its right end
        x = screenWidth / 2 + 78;
        y = 30 - 24 * SCALE + 12;

        PoseStack m = ctx.pose();
        // tail first, behind the body, swishing around its base
        float swish = Mth.sin(t * (asleep ? 0.8f : 2.2f)) * (asleep ? 0.12f : 0.45f);
        m.pushPose();
        m.translate(x + 15 * SCALE, y + 21 * SCALE, 0);
        m.mulPose(Axis.ZP.rotation(-0.4f + swish));
        m.scale(SCALE, SCALE, 1);
        Ui.sprite(ctx, "tail_cherry", -1, -11, 8, 12);
        m.popPose();

        String face = asleep ? "sleep" : (t < blinkUntil || t - pokeAt < 0.4f) ? "blink" : "open";
        float breathe = Mth.sin(t * (asleep ? 1.2f : 2.3f)) * (asleep ? 0.035f : 0.02f);
        float hop = t - pokeAt < 0.35f ? Mth.sin((t - pokeAt) / 0.35f * Mth.PI) * 5 : 0;
        m.pushPose();
        m.translate(x + 12 * SCALE, y + 24 * SCALE - hop, 0);
        m.scale(SCALE * (1 - breathe * 0.5f), SCALE * (1 + breathe), 1);
        Ui.sprite(ctx, "sit_cherry_" + face, -12, -24, 24, 24);
        m.popPose();

        if (asleep && t >= nextZ) {
            nextZ = t + 1.1f;
            fx.add(new float[]{x + 14 * SCALE, y + 4 * SCALE, 8, -14, 0, 1});
        }
        for (int i = fx.size() - 1; i >= 0; i--) {
            float[] f = fx.get(i);
            f[4] += dt;
            float life = f[5] == 1 ? 2.2f : 0.9f;
            if (f[4] > life) {
                fx.remove(i);
                continue;
            }
            f[0] += f[2] * dt + (f[5] == 1 ? Mth.sin(f[4] * 3) * 6 * dt : 0);
            f[1] += f[3] * dt;
            float a = 1 - f[4] / life;
            if (f[5] == 1) {
                float s = 0.7f + f[4] * 0.3f;
                m.pushPose();
                m.translate(f[0], f[1], 0);
                m.scale(s, s, 1);
                ctx.drawString(Minecraft.getInstance().font, "z", 0, 0, Ui.alpha(0xFFFFE3EE, Math.max(a, 0.03f)), true);
                m.popPose();
            } else {
                f[3] += 30 * dt;
                Ui.sprite(ctx, "icon_heart", (int) f[0] - 4, (int) f[1] - 4, 9, 9, Ui.alpha(0xFFFFFFFF, a));
            }
        }
    }

    /** @return true if the click landed on the cat */
    public boolean click(double mx, double my) {
        if (mx >= x + 4 && mx <= x + 20 * SCALE && my >= y + 2 * SCALE && my <= y + 24 * SCALE) {
            pokeAt = t;
            idle = 0;
            Ui.play(SoundEvents.CAT_AMBIENT, 1.2f + (float) Math.random() * 0.4f, 0.7f);
            for (int i = 0; i < 5; i++) {
                float a = -Mth.PI / 2 + ((float) Math.random() - 0.5f) * 2;
                fx.add(new float[]{x + 12 * SCALE, y + 8 * SCALE, Mth.cos(a) * 25, Mth.sin(a) * 35, 0, 0});
            }
            return true;
        }
        return false;
    }
}

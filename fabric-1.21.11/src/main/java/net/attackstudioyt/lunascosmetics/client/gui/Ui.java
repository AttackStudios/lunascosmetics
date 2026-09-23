package net.attackstudioyt.lunascosmetics.client.gui;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/** Shared drawing bits and the wardrobe's palette. */
public final class Ui {
    private Ui() {
    }

    public static final int PLUM = 0xFF3A1428;
    public static final int ROSE = 0xFFE0588A;
    public static final int PINK = 0xFFF7A1C0;
    public static final int BLUSH = 0xFFFFD6E5;
    public static final int PETAL = 0xFFFFF4F8;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GOLD = 0xFFFFD84A;

    private static final Random RNG = Random.create();

    public static Identifier sprite(String name) {
        return Identifier.of(LunasCosmetics.MOD_ID, "lunascosmetics/" + name);
    }

    public static void sprite(DrawContext ctx, String name, int x, int y, int w, int h) {
        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite(name), x, y, w, h);
    }

    public static void sprite(DrawContext ctx, String name, int x, int y, int w, int h, int argb) {
        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite(name), x, y, w, h, argb);
    }

    /** Pixel-rounded rectangle (corners cut by {@code r} steps). */
    public static void round(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 - x1 < 2 * r || y2 - y1 < 2 * r) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        ctx.fill(x1 + r, y1, x2 - r, y2, color);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (r - i - 0.5) * (r - i - 0.5)));
            ctx.fill(x1 + i, y1 + inset, x1 + i + 1, y2 - inset, color);
            ctx.fill(x2 - i - 1, y1 + inset, x2 - i, y2 - inset, color);
        }
    }

    /** Rounded rectangle with a border. */
    public static void panel(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int border, int fill) {
        round(ctx, x1, y1, x2, y2, r, border);
        round(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), fill);
    }

    public static int alpha(int argb, float a) {
        int base = (argb >>> 24) & 0xFF;
        return ((int) (base * Math.max(0, Math.min(1, a))) << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24);
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static float seconds() {
        return (System.nanoTime() % 1_000_000_000_000L) / 1e9f;
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
    }

    public static float rand() {
        return RNG.nextFloat();
    }
}

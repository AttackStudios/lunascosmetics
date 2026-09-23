package net.attackstudioyt.lunascosmetics.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Cherry blossom petals drifting across a screen. They tumble, sway on a breeze and scoot
 * away from the mouse. Real-time driven, so they look the same at any frame rate.
 */
public final class Petals {
    private static final Petals MENUS = new Petals(1.0f);

    public static Petals menus() {
        return MENUS;
    }

    private static final class Petal {
        float x, y, vx, vy, rot, spin, size, sway, phase;
        int kind;
        boolean blossom;
    }

    private static final int[] PETAL_W = {4, 4, 4, 5};
    private static final int[] PETAL_H = {4, 4, 5, 4};

    private final List<Petal> petals = new ArrayList<>();
    private final float density;
    private int w, h;
    private long last;

    public Petals(float density) {
        this.density = density;
    }

    /** Short burst of petals from a point (button clicks). */
    public void burst(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            Petal p = spawn(true);
            p.x = x;
            p.y = y;
            float a = Ui.rand() * Mth.TWO_PI;
            float s = 25 + Ui.rand() * 60;
            p.vx = Mth.cos(a) * s;
            p.vy = Mth.sin(a) * s - 30;
            petals.add(p);
        }
    }

    private Petal spawn(boolean anywhere) {
        Petal p = new Petal();
        p.x = Ui.rand() * (w + 60) - 30;
        p.y = anywhere ? Ui.rand() * h : -10 - Ui.rand() * 40;
        p.vx = 6 + Ui.rand() * 14;
        p.vy = 12 + Ui.rand() * 16;
        p.rot = Ui.rand() * Mth.TWO_PI;
        p.spin = (Ui.rand() - 0.5f) * 3;
        p.size = 1.1f + Ui.rand() * 1.1f;
        p.sway = 8 + Ui.rand() * 18;
        p.phase = Ui.rand() * 10;
        p.kind = (int) (Ui.rand() * 4);
        p.blossom = Ui.rand() < 0.08f;
        return p;
    }

    public void render(GuiGraphics ctx, int width, int height, int mouseX, int mouseY, float alpha) {
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Mth.clamp((now - last) / 1e9f, 0, 0.1f);
        last = now;
        int want = (int) Math.min(70, width * height / 7000f * density);
        if (width != w || height != h) {
            w = width;
            h = height;
            petals.removeIf(p -> p.x > w + 40 || p.y > h + 40);
            while (petals.size() < want) {
                petals.add(spawn(true));
            }
        }
        while (petals.size() < want) {
            petals.add(spawn(false));
        }
        float t = Ui.seconds();
        float breeze = Mth.sin(t * 0.4f) * 10;
        PoseStack m = ctx.pose();
        for (int i = petals.size() - 1; i >= 0; i--) {
            Petal p = petals.get(i);
            // shy of the cursor
            float dx = p.x - mouseX, dy = p.y - mouseY;
            float d2 = dx * dx + dy * dy;
            if (d2 < 900 && d2 > 0.01f) {
                float push = (900 - d2) / 900 * 160 * dt;
                float d = Mth.sqrt(d2);
                p.vx += dx / d * push * 6;
                p.vy += dy / d * push * 6;
            }
            // relax back toward a gentle fall
            p.vx += ((8 + breeze) - p.vx) * Math.min(1, dt * 0.8f);
            p.vy += (18 - p.vy) * Math.min(1, dt * 0.8f);
            p.x += (p.vx + Mth.sin(t * 1.3f + p.phase) * p.sway) * dt;
            p.y += p.vy * dt;
            p.rot += p.spin * dt;
            if (p.y > h + 20 || p.x > w + 40 || p.x < -60) {
                if (petals.size() > want) {
                    petals.remove(i);
                    continue;
                }
                Petal n = spawn(false);
                petals.set(i, n);
                continue;
            }
            m.pushPose();
            m.translate(p.x, p.y, 0);
            m.mulPose(Axis.ZP.rotation(p.rot));
            // a tumbling petal flips over: squash one axis
            float flip = 0.35f + 0.65f * Math.abs(Mth.sin(t * 1.7f + p.phase));
            m.scale(p.size * flip, p.size, 1);
            int size = p.blossom ? 7 : PETAL_W[p.kind];
            int ph = p.blossom ? 7 : PETAL_H[p.kind];
            String sprite = p.blossom ? "blossom" : "petal_" + p.kind;
            Ui.sprite(ctx, sprite, -size / 2, -ph / 2, size, ph, Ui.alpha(0xFFFFFFFF, alpha * 0.9f));
            m.popPose();
        }
    }
}

package net.attackstudioyt.lunascosmetics.client.cosmetic;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;

/**
 * A living pet. Cats lie on your head in a loaf with their paws out, or sit up on a
 * shoulder; Mini Moosh sits up either way. Everything that moves comes from the wearer's
 * {@link Brain} plus what the wearer is doing ({@link Inputs}).
 */
public class PetCosmetic extends Cosmetic {
    public enum Kind { CAT, MOOSH }

    private final Kind kind;
    private final String modelName;
    private final ResourceLocation texture;
    private final ResourceLocation blinkTexture;
    private final ResourceLocation glowTexture;
    private final boolean twinkle;

    public PetCosmetic(String id, String name, String description, int accent, Kind kind,
                       String folder, String texName, boolean glow, boolean twinkle) {
        super(id, name, description, Slot.PET, accent);
        this.kind = kind;
        this.modelName = kind == Kind.CAT ? "cat" : "moosh";
        this.texture = ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "textures/cosmetic/" + folder + "/" + texName + ".png");
        this.blinkTexture = ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "textures/cosmetic/" + folder + "/" + texName + "_blink.png");
        this.glowTexture = glow ? ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "textures/cosmetic/" + folder + "/" + texName + "_glow.png") : null;
        this.twinkle = twinkle;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public float cardScale() {
        return kind == Kind.MOOSH ? 1.25f : 1.0f;
    }

    @Override
    public void render(RenderCtx ctx) {
        LunaModel model = LunaModel.get(modelName);
        Brain b = ctx.brain();
        Inputs in = ctx.inputs();
        LunaModel.Pose pose = model.newPose();
        PoseStack m = ctx.sink().matrices();
        m.pushPose();
        boolean shoulder = !ctx.perch().onHead();
        if (shoulder) {
            m.scale(0.62f, 0.62f, 0.62f);
        }
        if (kind == Kind.CAT) {
            if (ctx.perch().sitting()) {
                poseCatSitting(pose, b, in);
            } else {
                poseCatLoaf(pose, b, in, ctx.gui());
            }
        } else {
            poseMoosh(pose, b, in);
        }
        ResourceLocation tex = b.eyesClosed() ? blinkTexture : texture;
        ctx.sink().model(model, pose, tex, -1);
        if (glowTexture != null) {
            int a = 255;
            if (twinkle) {
                a = (int) (175 + 80 * (0.5f + 0.5f * Mth.sin(b.time * 2.3f)));
            }
            if (b.eyesClosed() && !twinkle) {
                a = 150;
            }
            ctx.sink().glow(model, pose, glowTexture, (0xFF << 24) | (a << 16) | (a << 8) | a);
        }
        m.popPose();
    }

    // ---- cat on the head: a loaf with its paws stuck out front ----------------------------
    private static void poseCatLoaf(LunaModel.Pose p, Brain b, Inputs in, boolean gui) {
        float t = b.time;
        float sleep = b.sleep;
        float breathe = Mth.sin(t * (sleep > 0.5f ? 1.3f : 2.1f)) * (0.025f + 0.02f * sleep);
        float puff = 1 + b.flinch() * 0.1f;

        // Keep the cat roughly level when the wearer looks up or down: it grips on.
        float balance = gui ? 0 : Mth.clamp(-in.headPitch(), -60, 60) * 0.7f;
        float sway = Mth.sin(in.walkPhase() * 0.6662f) * 4f * in.moving();
        p.rotDeg("body", balance, 0, sway);
        p.scale("body", puff, puff + breathe, puff);

        // head: looks around, tilts curiously, rests on its paws when asleep
        float nod = in.fallSpeed() > 0.3f ? -12 : 0;
        p.rotDeg("head", b.lookPitch - balance * 0.3f + sleep * 22 + nod, b.lookYaw + sleep * 18, b.tilt);
        p.move("head", 0, sleep * 1.6f, sleep * -0.4f);

        // ears: twitches, flattened when startled or sneaking
        float flat = Math.max(b.flinch(), in.sneaking() ? 0.7f : 0) * -40;
        p.rotDeg("ear_left", flat, 0, -b.earLeft * 28);
        p.rotDeg("ear_right", flat, 0, b.earRight * 28);

        // paws forward; kneading alternates them
        float knead = b.kneading();
        float kL = knead * Mth.sin(t * 9) * 14;
        float kR = knead * Mth.sin(t * 9 + Mth.PI) * 14;
        float splay = in.fallSpeed() > 0.3f ? 18 : 0;
        float stretch = b.stretch() * 14;
        p.rotDeg("leg_front_left", -90 + kL + splay - stretch, 0, 0);
        p.rotDeg("leg_front_right", -90 + kR + splay - stretch, 0, 0);
        p.rotDeg("leg_back_left", 90 + stretch * 0.5f, -15, 0);
        p.rotDeg("leg_back_right", 90 + stretch * 0.5f, 15, 0);
        p.move("leg_front_left", 0, 0, -stretch * 0.05f);

        // tail: lazy swish, faster and straighter when running, curled round when asleep
        float amp = 22 + in.moving() * 18;
        float swish = Mth.sin(b.tailPhase) * amp * (1 - sleep);
        float follow = Mth.sin(b.tailPhase - 0.9f) * (amp + 10) * (1 - sleep);
        float droop = -38 + in.moving() * 26 + knead * 20;
        p.rotDeg("tail", droop, swish + sleep * 70, 0);
        p.rotDeg("tail_tip", -22 + sleep * 10, follow + sleep * 65, 0);
    }

    // ---- cat on a shoulder: sitting up, tail hanging ------------------------------------
    private static void poseCatSitting(LunaModel.Pose p, Brain b, Inputs in) {
        float t = b.time;
        float breathe = Mth.sin(t * 2.1f) * 0.025f;
        float sway = Mth.sin(in.walkPhase() * 0.6662f) * 5f * in.moving();
        p.rotDeg("body", -60, 0, sway);
        p.move("body", 0, -2.9f, -0.8f);
        p.scale("body", 1, 1 + breathe, 1);
        float sleep = b.sleep;
        p.rotDeg("head", 60 + b.lookPitch + sleep * 25, b.lookYaw, b.tilt);
        float flat = Math.max(b.flinch(), in.sneaking() ? 0.7f : 0) * -40;
        p.rotDeg("ear_left", flat, 0, -b.earLeft * 28);
        p.rotDeg("ear_right", flat, 0, b.earRight * 28);
        float knead = b.kneading();
        p.rotDeg("leg_front_left", 60 + knead * Mth.sin(t * 9) * 10, 0, 0);
        p.rotDeg("leg_front_right", 60 + knead * Mth.sin(t * 9 + Mth.PI) * 10, 0, 0);
        // front legs are shorter than a sitting cat's reach: plant the paws on the head.
        // (offsets are in the tilted body's space: this is 2.43 px straight down in the world)
        p.move("leg_front_left", 0, 1.215f, 2.104f);
        p.move("leg_front_right", 0, 1.215f, 2.104f);
        // haunches folded flat along the head, pointing forward (not up in the air)
        p.rotDeg("leg_back_left", -30, -12, 0);
        p.rotDeg("leg_back_right", -30, 12, 0);
        p.move("leg_back_left", 0, 0.08f, 0.139f);
        p.move("leg_back_right", 0, 0.08f, 0.139f);
        float swish = Mth.sin(b.tailPhase) * (18 + in.moving() * 14);
        p.rotDeg("tail", -50, 25 + swish, 0);
        p.rotDeg("tail_tip", -30, 30 + Mth.sin(b.tailPhase - 0.9f) * 26, 0);
    }

    // ---- Mini Moosh: bouncy, floppy-eared, sprout on top ---------------------------------
    private static void poseMoosh(LunaModel.Pose p, Brain b, Inputs in) {
        float t = b.time;
        float sleep = b.sleep;
        float breathe = Mth.sin(t * 2.4f) * 0.03f;
        float hop = b.bounce();
        float hopY = hop > 0 ? -Mth.sin(hop * Mth.PI) * 2.2f : 0;
        float squash = hop > 0 ? Mth.sin(hop * Mth.PI * 2) * 0.08f : 0;
        float sway = Mth.sin(in.walkPhase() * 0.6662f) * 6f * in.moving();
        float wobble = Mth.sin(t * 1.7f) * 3;
        p.move("body", 0, -1 + hopY, 0);
        p.rotDeg("body", 0, 0, sway + wobble * 0.4f);
        p.scale("body", 1 - squash * 0.5f, 1 + breathe + squash, 1 - squash * 0.5f);

        p.rotDeg("head", b.lookPitch * 0.8f + sleep * 18 - b.flinch() * 10, b.lookYaw * 0.8f, b.tilt * 0.8f);
        p.move("head", 0, sleep * 0.8f, 0);

        // floppy ears: springy flop that reacts to hops and steps
        float flop = Mth.sin(t * 3.1f) * 8 + hop * 25 + in.moving() * Mth.sin(in.walkPhase() * 1.3f) * 14;
        p.rotDeg("ear_left", 0, 0, -18 - flop - b.earLeft * 20 + sleep * 20);
        p.rotDeg("ear_right", 0, 0, 18 + flop + b.earRight * 20 - sleep * 20);

        // sprout sways like a little plant; leaves flutter
        float sproutSway = Mth.sin(t * 1.9f) * 9 + (hop > 0 ? Mth.sin(hop * 12) * 18 * (1 - hop) : 0);
        p.rotDeg("sprout", Mth.sin(t * 1.3f) * 4, 0, sproutSway);
        p.rotDeg("leaf_left", 0, 0, Mth.sin(t * 3.3f) * 7);
        p.rotDeg("leaf_right", 0, 0, -Mth.sin(t * 3.3f + 0.7f) * 7);

        // little legs: paddle when walking, kick on a hop
        float paddle = Mth.sin(in.walkPhase() * 0.6662f) * 20 * in.moving();
        p.rotDeg("leg_front_left", paddle - hop * 15, 0, 0);
        p.rotDeg("leg_front_right", -paddle - hop * 15, 0, 0);
        p.rotDeg("leg_back_left", 0, -8 + hop * 10, 0);
        p.rotDeg("leg_back_right", 0, 8 - hop * 10, 0);

        // tail wiggles fast, like a happy piglet
        p.rotDeg("tail", 25, Mth.sin(t * 9) * 35 * (1 - sleep), 0);
    }

    public static void applyYaw(PoseStack m, float deg) {
        m.mulPose(Axis.YP.rotationDegrees(deg));
    }
}

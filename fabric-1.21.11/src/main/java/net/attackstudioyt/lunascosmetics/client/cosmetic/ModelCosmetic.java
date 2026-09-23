package net.attackstudioyt.lunascosmetics.client.cosmetic;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.model.LunaModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Hats and back pieces built from a model JSON, with a small built-in behaviour. New
 * wearables usually only need a new {@link Motion} case, if that.
 */
public class ModelCosmetic extends Cosmetic {
    public enum Motion {
        /** sits still */
        NONE,
        /** kitty ears: twitch with the brain's ear timers, flatten when startled */
        EARS,
        /** halo: floats, bobs and slowly turns */
        HALO,
        /** wings: gentle idle flutter, beat harder while moving or falling */
        WINGS
    }

    private final String modelName;
    private final Identifier texture;
    private final Identifier glowTexture;
    private final Motion motion;
    private final float cardScale;
    private final float cardLift;

    public ModelCosmetic(String id, String name, String description, Slot slot, int accent,
                         String modelName, String texturePath, boolean glow, Motion motion,
                         float cardScale, float cardLift) {
        super(id, name, description, slot, accent);
        this.modelName = modelName;
        this.texture = Identifier.of(LunasCosmetics.MOD_ID, "textures/cosmetic/" + texturePath + ".png");
        this.glowTexture = glow ? Identifier.of(LunasCosmetics.MOD_ID, "textures/cosmetic/" + texturePath + "_glow.png") : null;
        this.motion = motion;
        this.cardScale = cardScale;
        this.cardLift = cardLift;
    }

    @Override
    public float cardScale() {
        return cardScale;
    }

    @Override
    public float cardLift() {
        return cardLift;
    }

    @Override
    public float liftOverPet(boolean petSitting) {
        return motion == Motion.HALO ? (petSitting ? 12 : 8) : 0;
    }

    @Override
    public void render(RenderCtx ctx) {
        LunaModel model = LunaModel.get(modelName);
        LunaModel.Pose pose = model.newPose();
        Brain b = ctx.brain();
        Inputs in = ctx.inputs();
        float t = b.time;
        switch (motion) {
            case EARS -> {
                float flat = Math.max(b.flinch(), in.sneaking() ? 0.6f : 0) * -35;
                pose.rotDeg("ear_left", flat, 0, -b.earLeft * 22);
                pose.rotDeg("ear_right", flat, 0, b.earRight * 22);
            }
            case HALO -> {
                pose.rot("halo", 0, t * 0.9f, 0);
                pose.move("halo", 0, MathHelper.sin(t * 2.1f) * 0.7f, 0);
                for (int i = 0; i < 3; i++) {
                    float s = 1 + 0.25f * MathHelper.sin(t * 3.2f + i * 2.1f);
                    pose.scale("star" + i, s, s, s);
                    pose.rot("star" + i, t * 2 + i, t * 1.4f, 0);
                }
            }
            case WINGS -> {
                float beat = 0.35f + in.moving() * 0.6f + Math.min(1, in.fallSpeed() * 2) * 0.8f;
                float speed = 2.2f + beat * 6;
                float a = MathHelper.sin(t * speed) * (8 + beat * 22);
                pose.rotDeg("wing_left", 0, a, 0);
                pose.rotDeg("wing_right", 0, -a, 0);
            }
            case NONE -> {
            }
        }
        ctx.sink().model(model, pose, texture, -1);
        if (glowTexture != null) {
            int v = (int) (200 + 55 * MathHelper.sin(t * 2.7f));
            ctx.sink().glow(model, pose, glowTexture, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }
}

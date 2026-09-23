package net.attackstudioyt.lunascosmetics.client.render;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.Wardrobe;
import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Perch;
import net.attackstudioyt.lunascosmetics.client.cosmetic.PetCosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.RenderCtx;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Puts a loadout on a posed player model: finds each slot's anchor, then lets the
 * cosmetic draw. Shared by the in-world feature renderer and the wardrobe preview.
 */
public final class CosmeticRenderer {
    private CosmeticRenderer() {
    }

    /** per player: {yaw, pitch, seconds since the view last moved} */
    private static final Map<UUID, float[]> LOOK = new HashMap<>();
    private static final Map<UUID, float[]> SOUND = new HashMap<>();

    /** In-world entry point, from the player feature renderer. */
    public static void renderWorld(PlayerEntityRenderState state, PlayerEntityModel model, MatrixStack matrices,
                                   OrderedRenderCommandQueue queue, int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || state.invisible || state.spectator) {
            return;
        }
        Entity e = client.world.getEntityById(state.id);
        if (!(e instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        boolean me = player == client.player;
        Loadout loadout = me ? Wardrobe.mine() : ClientConfig.get().showOthers ? Wardrobe.of(player.getUuid()) : Loadout.EMPTY;
        if (loadout.isEmpty()) {
            return;
        }
        float fall = player.isOnGround() || player.isTouchingWater() || player.getAbilities().flying
                ? 0 : (float) Math.max(0, -player.getVelocity().y - 0.1);
        Inputs in = new Inputs(MathHelper.clamp(state.limbSwingAmplitude, 0, 1), state.limbSwingAnimationProgress,
                state.pitch, fall, state.isInSneakingPose, state.hurt, idleLook(player));
        Brain brain = Brain.of(player.getUuid());
        brain.update(in);
        if (me) {
            petSounds(player, loadout, brain);
        }
        render(loadout, model, new WorldSink(matrices, queue, light), brain, in,
                !state.equippedHeadStack.isEmpty(), !state.equippedChestStack.isEmpty(), false);
    }

    /** Draws a loadout on an already-posed player model. */
    public static void render(Loadout loadout, PlayerEntityModel model, CosmeticSink sink, Brain brain, Inputs in,
                              boolean helmet, boolean chestplate, boolean gui) {
        MatrixStack m = sink.matrices();
        Perch perch = loadout.perch();
        float helmetLift = helmet ? 1.2f : 0;
        Cosmetic hat = Cosmetics.get(loadout.get(Slot.HAT));
        Cosmetic pet = Cosmetics.get(loadout.get(Slot.PET));
        Cosmetic back = Cosmetics.get(loadout.get(Slot.BACK));

        if (hat != null) {
            m.push();
            model.head.applyTransform(m);
            // floating hats (the halo) rise above a pet sitting on the head
            float over = pet != null && perch.onHead() ? hat.liftOverPet(perch.sitting()) : 0;
            m.translate(0, (-8 - helmetLift - over) / 16f, 0);
            hat.render(new RenderCtx(sink, brain, in, perch, gui));
            m.pop();
        }
        if (pet != null) {
            m.push();
            if (perch.onHead()) {
                model.head.applyTransform(m);
                // pets rest right on the head (sinking a hair into the skin's outer layer),
                // even over a hat: ears and flowers peeking through a cat look cute, a gap doesn't
                m.translate(0, (-8 - helmetLift) / 16f, 0);
            } else {
                model.body.applyTransform(m);
                float side = perch == Perch.LEFT_SHOULDER ? 6 : -6;
                m.translate(side / 16f, (chestplate ? -1.2f : 0) / 16f, 0);
            }
            pet.render(new RenderCtx(sink, brain, in, perch, gui));
            m.pop();
        }
        if (back != null) {
            m.push();
            model.body.applyTransform(m);
            if (chestplate) {
                m.translate(0, 0, 1.2f / 16f);
            }
            back.render(new RenderCtx(sink, brain, in, perch, gui));
            m.pop();
        }
    }

    /** How far (blocks) to raise this player's nametag so it clears a pet on their head. */
    public static float nameLift(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        Loadout l = player == client.player ? Wardrobe.mine()
                : ClientConfig.get().showOthers ? Wardrobe.of(player.getUuid()) : Loadout.EMPTY;
        if (l.get(Slot.PET) == null || !l.perch().onHead() || Cosmetics.get(l.get(Slot.PET)) == null) {
            return 0;
        }
        return l.perch().sitting() ? 0.95f : 0.5f;
    }

    /** True once the player's view has held still for a few seconds (lets pets doze). */
    private static boolean idleLook(AbstractClientPlayerEntity p) {
        float[] l = LOOK.computeIfAbsent(p.getUuid(), k -> new float[]{p.getYaw(), p.getPitch(), 0, System.nanoTime() / 1e9f});
        float now = System.nanoTime() / 1e9f;
        float dt = MathHelper.clamp(now - l[3], 0, 0.2f);
        l[3] = now;
        if (Math.abs(MathHelper.wrapDegrees(p.getYaw() - l[0])) > 3 || Math.abs(p.getPitch() - l[1]) > 3) {
            l[0] = p.getYaw();
            l[1] = p.getPitch();
            l[2] = 0;
        } else {
            l[2] += dt;
        }
        return l[2] > 4;
    }

    /** Local-only: sleepy purrs and Mini Moosh's squeak. Very quiet on purpose. */
    private static void petSounds(AbstractClientPlayerEntity p, Loadout loadout, Brain brain) {
        if (!ClientConfig.get().petSounds) {
            return;
        }
        Cosmetic pet = Cosmetics.get(loadout.get(Slot.PET));
        if (!(pet instanceof PetCosmetic pc)) {
            return;
        }
        float[] s = SOUND.computeIfAbsent(p.getUuid(), k -> new float[]{0, 0});
        if (pc.kind() == PetCosmetic.Kind.CAT) {
            if (brain.sleep > 0.9f && brain.time - s[0] > 3.2f) {
                s[0] = brain.time;
                p.getEntityWorld().playSoundClient(p.getX(), p.getY() + 2, p.getZ(), SoundEvents.ENTITY_CAT_PURR,
                        SoundCategory.PLAYERS, 0.35f, 1.0f, false);
            }
        } else {
            float b = brain.bounce();
            if (b > 0 && b < 0.15f && brain.time - s[1] > 1) {
                s[1] = brain.time;
                p.getEntityWorld().playSoundClient(p.getX(), p.getY() + 2, p.getZ(), SoundEvents.ENTITY_PIG_AMBIENT,
                        SoundCategory.PLAYERS, 0.18f, 1.9f, false);
            }
        }
    }
}

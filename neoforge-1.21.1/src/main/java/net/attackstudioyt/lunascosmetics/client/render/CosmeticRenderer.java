package net.attackstudioyt.lunascosmetics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Puts a loadout on a posed player model: finds each slot's anchor, then lets the
 * cosmetic draw. Shared by the in-world render layer and the wardrobe preview.
 */
public final class CosmeticRenderer {
    private CosmeticRenderer() {
    }

    /** per player: {yaw, pitch, seconds since the view last moved} */
    private static final Map<UUID, float[]> LOOK = new HashMap<>();
    private static final Map<UUID, float[]> SOUND = new HashMap<>();

    /** In-world entry point, from the player render layer. */
    public static void renderWorld(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, PoseStack ps,
                                   MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                                   float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || player.isInvisible() || player.isSpectator()) {
            return;
        }
        boolean me = player == client.player;
        Loadout loadout = me ? Wardrobe.mine() : ClientConfig.get().showOthers ? Wardrobe.of(player.getUUID()) : Loadout.EMPTY;
        if (loadout.isEmpty()) {
            return;
        }
        float fall = player.onGround() || player.isInWater() || player.getAbilities().flying
                ? 0 : (float) Math.max(0, -player.getDeltaMovement().y - 0.1);
        Inputs in = new Inputs(Mth.clamp(limbSwingAmount, 0, 1), limbSwing,
                player.getXRot(), fall, player.isCrouching(), player.hurtTime > 0, idleLook(player));
        Brain brain = Brain.of(player.getUUID());
        brain.update(in);
        if (me) {
            petSounds(player, loadout, brain);
        }
        render(loadout, model, new BufferSink(ps, buffers, light), brain, in,
                !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), false);
    }

    /** Draws a loadout on an already-posed player model. */
    public static void render(Loadout loadout, PlayerModel<?> model, CosmeticSink sink, Brain brain, Inputs in,
                              boolean helmet, boolean chestplate, boolean gui) {
        PoseStack m = sink.matrices();
        Perch perch = loadout.perch();
        float helmetLift = helmet ? 1.2f : 0;
        Cosmetic hat = Cosmetics.get(loadout.get(Slot.HAT));
        Cosmetic pet = Cosmetics.get(loadout.get(Slot.PET));
        Cosmetic back = Cosmetics.get(loadout.get(Slot.BACK));

        if (hat != null) {
            m.pushPose();
            model.head.translateAndRotate(m);
            // floating hats (the halo) rise above a pet sitting on the head
            float over = pet != null && perch.onHead() ? hat.liftOverPet(perch.sitting()) : 0;
            m.translate(0, (-8 - helmetLift - over) / 16f, 0);
            hat.render(new RenderCtx(sink, brain, in, perch, gui));
            m.popPose();
        }
        if (pet != null) {
            m.pushPose();
            if (perch.onHead()) {
                model.head.translateAndRotate(m);
                // pets rest right on the head (sinking a hair into the skin's outer layer),
                // even over a hat: ears and flowers peeking through a cat look cute, a gap doesn't
                m.translate(0, (-8 - helmetLift) / 16f, 0);
            } else {
                model.body.translateAndRotate(m);
                float side = perch == Perch.LEFT_SHOULDER ? 6 : -6;
                m.translate(side / 16f, (chestplate ? -1.2f : 0) / 16f, 0);
            }
            pet.render(new RenderCtx(sink, brain, in, perch, gui));
            m.popPose();
        }
        if (back != null) {
            m.pushPose();
            model.body.translateAndRotate(m);
            if (chestplate) {
                m.translate(0, 0, 1.2f / 16f);
            }
            back.render(new RenderCtx(sink, brain, in, perch, gui));
            m.popPose();
        }
    }

    /** How far (blocks) to raise this player's nametag so it clears a pet on their head. */
    public static float nameLift(AbstractClientPlayer player) {
        Minecraft client = Minecraft.getInstance();
        Loadout l = player == client.player ? Wardrobe.mine()
                : ClientConfig.get().showOthers ? Wardrobe.of(player.getUUID()) : Loadout.EMPTY;
        if (l.get(Slot.PET) == null || !l.perch().onHead() || Cosmetics.get(l.get(Slot.PET)) == null) {
            return 0;
        }
        return l.perch().sitting() ? 0.95f : 0.5f;
    }

    /** True once the player's view has held still for a few seconds (lets pets doze). */
    private static boolean idleLook(AbstractClientPlayer p) {
        float[] l = LOOK.computeIfAbsent(p.getUUID(), k -> new float[]{p.getYRot(), p.getXRot(), 0, System.nanoTime() / 1e9f});
        float now = System.nanoTime() / 1e9f;
        float dt = Mth.clamp(now - l[3], 0, 0.2f);
        l[3] = now;
        if (Math.abs(Mth.wrapDegrees(p.getYRot() - l[0])) > 3 || Math.abs(p.getXRot() - l[1]) > 3) {
            l[0] = p.getYRot();
            l[1] = p.getXRot();
            l[2] = 0;
        } else {
            l[2] += dt;
        }
        return l[2] > 4;
    }

    /** Local-only: sleepy purrs and Mini Moosh's squeak. Very quiet on purpose. */
    private static void petSounds(AbstractClientPlayer p, Loadout loadout, Brain brain) {
        if (!ClientConfig.get().petSounds) {
            return;
        }
        Cosmetic pet = Cosmetics.get(loadout.get(Slot.PET));
        if (!(pet instanceof PetCosmetic pc)) {
            return;
        }
        float[] s = SOUND.computeIfAbsent(p.getUUID(), k -> new float[]{0, 0});
        if (pc.kind() == PetCosmetic.Kind.CAT) {
            if (brain.sleep > 0.9f && brain.time - s[0] > 3.2f) {
                s[0] = brain.time;
                p.level().playLocalSound(p.getX(), p.getY() + 2, p.getZ(), SoundEvents.CAT_PURR,
                        SoundSource.PLAYERS, 0.35f, 1.0f, false);
            }
        } else {
            float b = brain.bounce();
            if (b > 0 && b < 0.15f && brain.time - s[1] > 1) {
                s[1] = brain.time;
                p.level().playLocalSound(p.getX(), p.getY() + 2, p.getZ(), SoundEvents.PIG_AMBIENT,
                        SoundSource.PLAYERS, 0.18f, 1.9f, false);
            }
        }
    }
}

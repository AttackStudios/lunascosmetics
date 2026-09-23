package net.attackstudioyt.lunascosmetics.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Perch;
import net.attackstudioyt.lunascosmetics.client.cosmetic.RenderCtx;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.client.render.BufferSink;
import net.attackstudioyt.lunascosmetics.client.render.CosmeticRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * One live 3D picture in the wardrobe, drawn immediately (the way vanilla draws the player
 * in the inventory): either Luna herself wearing a loadout, or a single cosmetic on a card.
 *
 * <p>Space after the setup is the same as the 1.21.11 picture-in-picture preview: origin at
 * the bottom-centre of the box, y down, model -z towards the viewer, 1 unit = {@code scale} px.
 */
public final class Preview {
    private Preview() {
    }

    private static PlayerModel<LivingEntity> wide;
    private static PlayerModel<LivingEntity> slim;

    /** @param cosmeticId null = draw the whole player wearing {@code loadout} */
    public static void draw(GuiGraphics g, @Nullable String cosmeticId, Loadout loadout, Object brainKey,
                            float yaw, float pitch, ResourceLocation skin, boolean slimSkin,
                            int x1, int y1, int x2, int y2, float scale) {
        Minecraft client = Minecraft.getInstance();
        Cosmetic c = null;
        if (cosmeticId != null) {
            c = Cosmetics.get(cosmeticId);
            if (c == null) {
                return;
            }
        }
        Brain brain = Brain.of(brainKey);
        brain.update(Inputs.STILL);

        g.enableScissor(x1, y1, x2, y2);
        PoseStack m = g.pose();
        m.pushPose();
        // bottom-centre of the box, a hair above the edge (the old PIP used 0.97 of the height)
        m.translate((x1 + x2) / 2f, y1 + (y2 - y1) * 0.97f, 50);
        // GUI space is y-down with +z towards the viewer; the -z scale turns model space's
        // "-z forward" towards us, so no extra 180 degree turn is needed (as in InventoryScreen,
        // where Z180 and the renderer's scale(-1,-1,1) cancel out)
        m.scale(scale, scale, -scale);
        Lighting.setupForEntityInInventory();
        BufferSink sink = new BufferSink(m, g.bufferSource(), LightTexture.FULL_BRIGHT);

        if (c == null) {
            renderPlayer(client, m, g, sink, brain, loadout, yaw, pitch, skin, slimSkin);
        } else {
            m.translate(0, -0.2f + c.cardLift() / 16f, 0);
            m.mulPose(Axis.XP.rotationDegrees(pitch));
            m.mulPose(Axis.YP.rotationDegrees(yaw));
            float z = c.cardScale();
            m.scale(z, z, z);
            if (c.slot == Slot.BACK) {
                // wings are anchored at the neck; show them from behind, centred
                m.translate(0, -0.45f, 0);
                m.mulPose(Axis.YP.rotationDegrees(180));
            }
            c.render(new RenderCtx(sink, brain, Inputs.STILL, Perch.HEAD, true));
        }
        g.flush();
        Lighting.setupFor3DItems();
        m.popPose();
        g.disableScissor();
    }

    private static void renderPlayer(Minecraft client, PoseStack m, GuiGraphics g, BufferSink sink, Brain brain,
                                     Loadout loadout, float yaw, float pitch, ResourceLocation skin, boolean slimSkin) {
        if (wide == null) {
            wide = new PlayerModel<>(client.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(client.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            wide.setAllVisible(true);
            slim.setAllVisible(true);
        }
        PlayerModel<LivingEntity> model = slimSkin ? slim : wide;
        // no entity here: stand her up by hand, gently breathing and glancing around with her pet
        for (ModelPart p : new ModelPart[]{model.head, model.hat, model.body, model.rightArm, model.leftArm,
                model.rightLeg, model.leftLeg}) {
            p.resetPose();
        }
        model.young = false;
        model.crouching = false;
        model.riding = false;
        model.attackTime = 0;
        model.head.yRot = Mth.sin(brain.time * 0.5f) * 12 * Mth.DEG_TO_RAD;
        model.head.xRot = Mth.sin(brain.time * 0.37f) * 5 * Mth.DEG_TO_RAD;
        AnimationUtils.bobArms(model.rightArm, model.leftArm, brain.time * 20);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);

        // same math as the Fabric preview: spin about her middle, feet on the box floor
        m.translate(0, -0.85f, 0);
        m.mulPose(Axis.XP.rotationDegrees(pitch));
        m.mulPose(Axis.YP.rotationDegrees(-yaw));
        m.translate(0, 0.85f - 1.601f, 0);
        model.renderToBuffer(m, g.bufferSource().getBuffer(RenderType.entityTranslucent(skin)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        CosmeticRenderer.render(loadout, model, sink, brain, Inputs.STILL, false, false, true);
    }
}

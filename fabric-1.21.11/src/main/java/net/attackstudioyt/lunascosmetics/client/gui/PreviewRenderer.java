package net.attackstudioyt.lunascosmetics.client.gui;

import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.anim.Inputs;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Perch;
import net.attackstudioyt.lunascosmetics.client.cosmetic.RenderCtx;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.client.render.CosmeticRenderer;
import net.attackstudioyt.lunascosmetics.client.render.GuiSink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Renders {@link PreviewState}s: Luna in her outfit, or one cosmetic spinning on a card. */
public class PreviewRenderer extends SpecialGuiElementRenderer<PreviewState> {
    private PlayerEntityModel wide;
    private PlayerEntityModel slim;
    private final PlayerEntityRenderState pose = new PlayerEntityRenderState();

    public PreviewRenderer(VertexConsumerProvider.Immediate immediate) {
        super(immediate);
    }

    @Override
    public Class<PreviewState> getElementClass() {
        return PreviewState.class;
    }

    @Override
    protected void render(PreviewState s, MatrixStack m) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
        Brain brain = Brain.of(s.brainKey());
        brain.update(Inputs.STILL);
        GuiSink sink = new GuiSink(m, this.vertexConsumers);

        if (s.cosmeticId() == null) {
            renderPlayer(client, s, m, sink, brain);
        } else {
            Cosmetic c = Cosmetics.get(s.cosmeticId());
            if (c != null) {
                m.translate(0, -0.2f + c.cardLift() / 16f, 0);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s.pitch()));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s.yaw()));
                float z = s.zoom() * c.cardScale();
                m.scale(z, z, z);
                if (c.slot == Slot.BACK) {
                    // wings are anchored at the neck; show them from behind, centred
                    m.translate(0, -0.45f, 0);
                    m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                }
                c.render(new RenderCtx(sink, brain, Inputs.STILL, Perch.HEAD, true));
            }
        }
        this.vertexConsumers.draw();
    }

    private void renderPlayer(MinecraftClient client, PreviewState s, MatrixStack m, GuiSink sink, Brain brain) {
        if (wide == null) {
            wide = new PlayerEntityModel(client.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
            slim = new PlayerEntityModel(client.getLoadedEntityModels().getModelPart(new net.minecraft.client.render.entity.model.EntityModelLayer(net.minecraft.util.Identifier.ofVanilla("player_slim"), "main")), true);
        }
        PlayerEntityModel model = s.slim() ? slim : wide;
        // stand her up, gently breathing and glancing around with her pet
        pose.age = brain.time * 20;
        pose.relativeHeadYaw = MathHelper.sin(brain.time * 0.5f) * 12;
        pose.pitch = MathHelper.sin(brain.time * 0.37f) * 5;
        pose.limbSwingAmplitude = 0;
        pose.hatVisible = true;
        pose.jacketVisible = true;
        pose.leftSleeveVisible = pose.rightSleeveVisible = true;
        pose.leftPantsLegVisible = pose.rightPantsLegVisible = true;
        model.setAngles(pose);

        // PIP space is already y-down with z facing the viewer, same as vanilla's skin preview
        m.translate(0, -0.85f, 0);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s.pitch()));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s.yaw()));
        m.translate(0, 0.85f - 1.601f, 0);
        model.render(m, this.vertexConsumers.getBuffer(model.getLayer(s.skin())),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        CosmeticRenderer.render(s.loadout(), model, sink, brain, Inputs.STILL, false, false, true);
    }

    @Override
    protected float getYOffset(int height, int windowScaleFactor) {
        return height * 0.97f;
    }

    @Override
    protected String getName() {
        return "luna's cosmetics preview";
    }
}

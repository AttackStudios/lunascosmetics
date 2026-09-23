package net.attackstudioyt.lunascosmetics.client;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.custom.CustomLibrary;
import net.attackstudioyt.lunascosmetics.client.gui.PreviewRenderer;
import net.attackstudioyt.lunascosmetics.client.gui.WardrobeScreen;
import net.attackstudioyt.lunascosmetics.client.sync.ClientSync;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Luna's Cosmetics, client side: a free wardrobe of living pets and cute wearables,
 * plus the optional Cherry Cat theme for the whole game.
 */
public class LunasCosmeticsClient implements ClientModInitializer {
    private static KeyBinding openKey;

    /** Essential-style self update: check GitHub once per launch, install on exit. */
    private static void startUpdater() {
        if (!ClientConfig.get().autoUpdate) {
            return;
        }
        var container = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(net.attackstudioyt.lunascosmetics.LunasCosmetics.MOD_ID).orElse(null);
        if (container == null) {
            return;
        }
        String version = container.getMetadata().getVersion().getFriendlyString();
        java.nio.file.Path jar = container.getOrigin().getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH
                ? container.getOrigin().getPaths().get(0) : null;
        net.attackstudioyt.lunascosmetics.update.Updater.start(version, "lunascosmetics-fabric-1.21.11-", jar,
                net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir(), (latest, msg) ->
                        net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                                net.minecraft.client.toast.SystemToast.add(
                                        net.minecraft.client.MinecraftClient.getInstance().getToastManager(),
                                        net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                                        net.minecraft.text.Text.literal("Luna's Cosmetics v" + latest + " is ready!"),
                                        net.minecraft.text.Text.literal("It'll install when you close the game."))));
    }

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        Theme.load();
        Cosmetics.registerBuiltins();
        CustomLibrary.ensureFolder();
        ClientSync.init();
        SpecialGuiElementRegistry.register(ctx -> new PreviewRenderer(ctx.vertexConsumers()));

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lunascosmetics.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));

        DevShots.init();
        startUpdater();

        boolean[] scanned = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!scanned[0]) {
                // textures can only be registered once the game is up
                scanned[0] = true;
                CustomLibrary.rescan(true);
            }
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new WardrobeScreen(null));
                }
            }
        });
    }
}

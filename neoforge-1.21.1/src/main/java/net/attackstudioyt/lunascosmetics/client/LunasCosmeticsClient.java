package net.attackstudioyt.lunascosmetics.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetics;
import net.attackstudioyt.lunascosmetics.client.custom.CustomLibrary;
import net.attackstudioyt.lunascosmetics.client.gui.CatButton;
import net.attackstudioyt.lunascosmetics.client.gui.MenuButtons;
import net.attackstudioyt.lunascosmetics.client.gui.TitleCat;
import net.attackstudioyt.lunascosmetics.client.gui.WardrobeScreen;
import net.attackstudioyt.lunascosmetics.client.render.CosmeticLayer;
import net.attackstudioyt.lunascosmetics.client.sync.ClientSync;
import net.attackstudioyt.lunascosmetics.client.theme.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Luna's Cosmetics, client side: a free wardrobe of living pets and cute wearables,
 * plus the optional Cherry Cat theme for the whole game.
 */
@Mod(value = LunasCosmetics.MOD_ID, dist = Dist.CLIENT)
public class LunasCosmeticsClient {
    private static final KeyMapping OPEN = new KeyMapping("key.lunascosmetics.open",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.misc");
    private static boolean scanned;

    public LunasCosmeticsClient(IEventBus modBus) {
        ClientConfig.load();
        Theme.load();
        Cosmetics.registerBuiltins();
        CustomLibrary.ensureFolder();
        ClientSync.init();

        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN));
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (PlayerSkin.Model skin : e.getSkins()) {
                if (e.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new CosmeticLayer(renderer));
                }
            }
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!scanned) {
                // textures can only be registered once the game is up
                scanned = true;
                CustomLibrary.rescan(true);
            }
            Theme.updateCursor();
            while (OPEN.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new WardrobeScreen(null));
                }
            }
        });

        // the cat button next to Options, on the title screen and in the pause menu
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            Screen screen = e.getScreen();
            boolean title = screen instanceof TitleScreen;
            boolean pause = screen instanceof PauseScreen p && p.showsPauseMenu();
            if (!title && !pause) {
                return;
            }
            Button options = MenuButtons.options(e.getListenersList());
            if (options == null) {
                return;
            }
            // title: left of the language globe; pause: right before Options
            int x = title ? options.getX() - 48 : options.getX() - 24;
            e.addListener(new CatButton(x, options.getY(), screen));
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> {
            Theme.updateCursor();
            if (e.getScreen() instanceof TitleScreen && Theme.on()) {
                TitleCat.INSTANCE.render(e.getGuiGraphics(), e.getScreen().width, e.getMouseX(), e.getMouseY());
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (e.getScreen() instanceof TitleScreen && Theme.on() && e.getButton() == 0
                    && TitleCat.INSTANCE.click(e.getMouseX(), e.getMouseY())) {
                e.setCanceled(true);
            }
        });

        DevShots.init();
        startUpdater();
    }

    /** Essential-style self update: check GitHub once per launch, install on exit. */
    private static void startUpdater() {
        if (!ClientConfig.get().autoUpdate) {
            return;
        }
        var file = net.neoforged.fml.ModList.get().getModFileById(LunasCosmetics.MOD_ID);
        if (file == null) {
            return;
        }
        String version = file.getMods().get(0).getVersion().toString();
        java.nio.file.Path jar = file.getFile().getFilePath();
        net.attackstudioyt.lunascosmetics.update.Updater.start(version, "lunascosmetics-neoforge-1.21.1-", jar,
                net.neoforged.fml.loading.FMLPaths.GAMEDIR.get(), (latest, msg) ->
                        Minecraft.getInstance().execute(() ->
                                net.minecraft.client.gui.components.toasts.SystemToast.add(
                                        Minecraft.getInstance().getToasts(),
                                        net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                        net.minecraft.network.chat.Component.literal("Luna's Cosmetics v" + latest + " is ready!"),
                                        net.minecraft.network.chat.Component.literal("It'll install when you close the game."))));
    }
}

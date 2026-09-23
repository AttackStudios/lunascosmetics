package net.attackstudioyt.lunascosmetics.client;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Perch;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.client.gui.WardrobeScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only: when LUNAS_DEVSHOTS is set, walks through the screens and saves a
 * screenshot of each, so changes can be checked without anyone at the keyboard.
 * Does nothing in normal play.
 */
final class DevShots {
    private DevShots() {
    }

    private record Step(int waitTicks, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int index, wait = -1;
    private static boolean started;

    static void init() {
        String mode = System.getenv("LUNAS_DEVSHOTS");
        if (mode == null) {
            return;
        }
        Minecraft c = Minecraft.getInstance();
        if (mode.equals("title")) {
            step(80, () -> shot("01_title"));
            step(10, () -> c.setScreen(new WardrobeScreen(c.screen)));
            step(60, () -> shot("02_wardrobe_pets"));
            step(5, () -> WardrobeScreen.devTab(1));
            step(40, () -> shot("03_wardrobe_hats"));
            step(5, () -> WardrobeScreen.devTab(2));
            step(40, () -> shot("04_wardrobe_back"));
            step(5, () -> WardrobeScreen.devTab(3));
            step(40, () -> shot("05_wardrobe_mine"));
            step(5, () -> WardrobeScreen.devTab(4));
            step(40, () -> shot("06_wardrobe_settings"));
            step(5, () -> c.setScreen(new TitleScreen()));
        } else if (mode.equals("friend")) {
            // stand in front of Luna and photograph her, to prove her cosmetics synced here
            for (int i = 0; i < 6; i++) {
                int n = i;
                step(n == 0 ? 120 : 45, () -> {
                    c.setScreen(null);
                    c.player.connection.sendCommand(
                            "execute at Luna run tp @s ^ ^ ^3.2 facing entity Luna eyes");
                });
                step(12, () -> shot("20_friend_view_" + n));
            }
        } else {
            // world: wait to be in game, then look at ourselves
            step(100, () -> {
                c.setScreen(null);
                c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                c.player.setXRot(0);
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "snowball"));
            });
            step(60, () -> shot("10_world_snowball_front"));
            step(5, () -> c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            step(40, () -> shot("11_world_snowball_back"));
            step(5, () -> {
                c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "mini_moosh").with(Slot.BACK, "petal_wings"));
            });
            step(50, () -> shot("12_world_moosh_wings"));
            step(5, () -> Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "luna").withPerch(Perch.HEAD_SIT)
                    .with(Slot.HAT, "star_halo")));
            step(50, () -> shot("13_world_luna_sit_halo"));
            step(5, () -> Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "marmalade").withPerch(Perch.LEFT_SHOULDER)
                    .with(Slot.HAT, "sakura_crown")));
            step(50, () -> shot("14_world_shoulder_crown"));
            step(5, () -> Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "stargazer").with(Slot.HAT, "kitty_ears_pink")));
            step(50, () -> shot("15_world_stargazer_ears"));
            step(5, () -> c.setScreen(new PauseScreen(true)));
            step(40, () -> shot("16_pause_menu"));
            step(5, () -> c.setScreen(new WardrobeScreen(null)));
            step(50, () -> shot("17_wardrobe_ingame"));
            step(5, () -> c.setScreen(null));
            step(5, () -> c.options.setCameraType(CameraType.FIRST_PERSON));
            step(30, () -> shot("18_hud"));
            step(5, () -> {
                c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "luna").withPerch(Perch.HEAD_SIT).with(Slot.HAT, "star_halo"));
            });
            step(40, () -> shot("19_final_luna_halo"));
        }
        boolean world = !mode.equals("title");
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft client = Minecraft.getInstance();
            if (index >= STEPS.size()) {
                return;
            }
            if (!started) {
                started = world ? client.player != null && client.level != null
                        : client.screen instanceof TitleScreen && client.getOverlay() == null;
                return;
            }
            if (wait < 0) {
                wait = STEPS.get(index).waitTicks;
            }
            if (--wait <= 0) {
                STEPS.get(index++).action.run();
                wait = -1;
            }
        });
    }

    private static void step(int wait, Runnable r) {
        STEPS.add(new Step(wait, r));
    }

    private static void shot(String name) {
        Minecraft c = Minecraft.getInstance();
        Screenshot.grab(c.gameDirectory, name + ".png", c.getMainRenderTarget(), t -> {
        });
    }
}

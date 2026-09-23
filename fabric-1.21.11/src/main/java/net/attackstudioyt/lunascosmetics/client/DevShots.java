package net.attackstudioyt.lunascosmetics.client;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Perch;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.client.gui.WardrobeScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;

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
        MinecraftClient c = MinecraftClient.getInstance();
        if (mode.equals("title")) {
            step(80, () -> shot("01_title"));
            step(10, () -> c.setScreen(new WardrobeScreen(c.currentScreen)));
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
        } else if (mode.equals("gallery")) {
            // pretty, uncluttered shots for the Modrinth gallery, in a little cherry grove
            step(100, () -> {
                c.setScreen(null);
                c.options.tutorialStep = net.minecraft.client.tutorial.TutorialStep.NONE;
                c.options.hudHidden = true;
                c.options.getFov().setValue(45);
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                for (String cmd : new String[]{"gamerule doDaylightCycle false", "time set 6000", "weather clear",
                        "gamemode creative", "tp @s 0 -60 0 0 0",
                        "execute at @s run place feature minecraft:cherry ^-6 ^ ^-8",
                        "execute at @s run place feature minecraft:cherry ^0 ^ ^-12",
                        "execute at @s run place feature minecraft:cherry ^7 ^ ^-9",
                        "execute at @s run place feature minecraft:cherry ^-13 ^ ^-15",
                        "execute at @s run place feature minecraft:cherry ^12 ^ ^-16",
                        "execute at @s run place feature minecraft:cherry ^3 ^ ^-20",
                        "execute at @s run place feature minecraft:cherry ^-6 ^ ^10",
                        "execute at @s run place feature minecraft:cherry ^6 ^ ^12",
                        "execute at @s run place feature minecraft:cherry ^0 ^ ^17"}) {
                    c.player.networkHandler.sendChatCommand(cmd);
                }
            });
            look(60, "g01_snowball", Loadout.EMPTY.with(Slot.PET, "snowball"), Perspective.THIRD_PERSON_FRONT);
            look(50, "g02_moosh_wings", Loadout.EMPTY.with(Slot.PET, "mini_moosh").with(Slot.BACK, "petal_wings"), Perspective.THIRD_PERSON_FRONT);
            look(50, "g03_sitting_crown", Loadout.EMPTY.with(Slot.PET, "marmalade").withPerch(Perch.HEAD_SIT).with(Slot.HAT, "sakura_crown"), Perspective.THIRD_PERSON_FRONT);
            look(50, "g04_stargazer_halo", Loadout.EMPTY.with(Slot.PET, "stargazer").with(Slot.HAT, "star_halo"), Perspective.THIRD_PERSON_FRONT);
            look(50, "g05_shoulder_ears", Loadout.EMPTY.with(Slot.PET, "calico").withPerch(Perch.LEFT_SHOULDER).with(Slot.HAT, "kitty_ears_pink"), Perspective.THIRD_PERSON_FRONT);
            look(50, "g06_back_wings", Loadout.EMPTY.with(Slot.PET, "sakura").with(Slot.BACK, "petal_wings"), Perspective.THIRD_PERSON_BACK);
            look(50, "g07_luna", Loadout.EMPTY.with(Slot.PET, "luna").withPerch(Perch.HEAD_SIT).with(Slot.HAT, "kitty_ears_midnight"), Perspective.THIRD_PERSON_FRONT);
            step(5, () -> {
                c.options.hudHidden = false;
                c.options.getFov().setValue(70);
                c.options.setPerspective(Perspective.FIRST_PERSON);
                c.player.networkHandler.sendChatCommand("gamemode survival");
                c.player.setPitch(20);
            });
            step(45, () -> c.inGameHud.getChatHud().clear(false));
            step(5, () -> shot("g08_hud"));
            step(5, () -> c.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(c.player)));
            step(40, () -> shot("g09_inventory"));
            step(5, () -> c.setScreen(new GameMenuScreen(true)));
            step(40, () -> shot("g10_pause"));
            step(5, () -> {
                c.player.networkHandler.sendChatCommand("gamemode creative");
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "sakura").withPerch(Perch.HEAD_SIT).with(Slot.HAT, "star_halo"));
                c.setScreen(new WardrobeScreen(null));
            });
            step(60, () -> shot("g11_wardrobe_pets"));
            step(5, () -> WardrobeScreen.devTab(1));
            step(40, () -> shot("g12_wardrobe_hats"));
            step(5, () -> WardrobeScreen.devTab(4));
            step(40, () -> shot("g13_wardrobe_settings"));
            step(5, () -> c.setScreen(null));
        } else if (mode.equals("wings")) {
            step(100, () -> {
                c.setScreen(null);
                c.options.hudHidden = true;
                c.options.getFov().setValue(45);
                c.player.networkHandler.sendChatCommand("time set noon");
                c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                c.player.setPitch(10);
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "stargazer").with(Slot.BACK, "starry_wings"));
            });
            step(60, () -> shot("w1_day_back"));
            step(5, () -> c.player.networkHandler.sendChatCommand("time set midnight"));
            step(60, () -> shot("w2_night_back"));
            step(5, () -> {
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                c.player.setPitch(-10);
            });
            step(50, () -> shot("w3_night_front"));
        } else if (mode.equals("relay")) {
            // repro: join a server without the mod, THEN paste the relay URL
            step(60, () -> {
                ClientConfig.get().relayUrl = System.getenv().getOrDefault("LUNAS_RELAY", "ws://127.0.0.1:8091");
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "snowball"));
            });
            step(200, () -> System.out.println("[relaytest] status: "
                    + net.attackstudioyt.lunascosmetics.client.sync.ClientSync.status()));
        } else if (mode.equals("friend")) {
            // stand in front of Luna and photograph her, to prove her cosmetics synced here
            for (int i = 0; i < 6; i++) {
                int n = i;
                step(n == 0 ? 120 : 45, () -> {
                    c.setScreen(null);
                    c.player.networkHandler.sendChatCommand(
                            (n % 2 == 0 ? "execute at Luna run tp @s ^ ^ ^3.2 facing entity Luna eyes"
                                    : "execute at Luna run tp @s ^3.2 ^0.4 ^ facing entity Luna eyes"));
                });
                step(12, () -> shot("20_friend_view_" + n));
            }
        } else {
            // world: wait to be in game, then look at ourselves
            step(100, () -> {
                c.setScreen(null);
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                c.player.setPitch(0);
                c.player.networkHandler.sendChatCommand("time set noon");
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "snowball"));
            });
            step(60, () -> shot("10_world_snowball_front"));
            step(5, () -> c.options.setPerspective(Perspective.THIRD_PERSON_BACK));
            step(40, () -> shot("11_world_snowball_back"));
            step(5, () -> {
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
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
            step(5, () -> c.setScreen(new GameMenuScreen(true)));
            step(40, () -> shot("16_pause_menu"));
            step(5, () -> c.setScreen(new WardrobeScreen(null)));
            step(50, () -> shot("17_wardrobe_ingame"));
            step(5, () -> c.setScreen(null));
            step(5, () -> c.options.setPerspective(Perspective.FIRST_PERSON));
            step(30, () -> shot("18_hud"));
            step(5, () -> {
                c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                Wardrobe.setMine(Loadout.EMPTY.with(Slot.PET, "marmalade").withPerch(Perch.HEAD_SIT));
            });
            step(40, () -> shot("19_final_luna_halo"));
        }
        boolean world = !mode.equals("title");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (index >= STEPS.size()) {
                return;
            }
            if (!started) {
                started = world ? client.player != null && client.world != null
                        : client.currentScreen instanceof TitleScreen && client.getOverlay() == null;
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

    private static void look(int wait, String name, Loadout loadout, Perspective perspective) {
        MinecraftClient c = MinecraftClient.getInstance();
        step(5, () -> {
            c.setScreen(null);
            c.getToastManager().clear();
            c.options.setPerspective(perspective);
            c.player.setPitch(perspective == Perspective.THIRD_PERSON_BACK ? 12 : -12);
            c.player.setYaw(0);
            c.inGameHud.getChatHud().clear(false);
            Wardrobe.setMine(loadout);
        });
        step(wait, () -> shot(name));
    }

    private static void step(int wait, Runnable r) {
        STEPS.add(new Step(wait, r));
    }

    private static void shot(String name) {
        MinecraftClient c = MinecraftClient.getInstance();
        ScreenshotRecorder.saveScreenshot(c.runDirectory, name + ".png", c.getFramebuffer(), 1, t -> {
        });
    }
}

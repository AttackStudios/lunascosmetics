package net.attackstudioyt.lunascosmetics.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Luna's settings + what she's wearing, in config/lunascosmetics/settings.json. */
public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig instance = new ClientConfig();

    /** the Cherry Cat theme: pink menus, petals, paw cursor, cat title screen */
    public boolean theme = true;
    /** buttons go "mew" when the theme is on */
    public boolean meowClicks = true;
    /** pets purr in their sleep, Mini Moosh squeaks when it bounces */
    public boolean petSounds = true;
    /** download new versions from GitHub and install them when the game closes */
    public boolean autoUpdate = true;
    /** see other players' cosmetics */
    public boolean showOthers = true;
    /** fallback relay for servers without the mod, e.g. wss://example/lunas */
    public String relayUrl = "";
    public String loadout = "{\"pet\":\"snowball\"}";

    public static ClientConfig get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("lunascosmetics/settings.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                ClientConfig c = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), ClientConfig.class);
                if (c != null) {
                    instance = c;
                    if (instance.relayUrl == null) {
                        instance.relayUrl = "";
                    }
                }
            }
        } catch (Exception e) {
            LunasCosmetics.LOGGER.warn("settings unreadable, using defaults: {}", e.toString());
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LunasCosmetics.LOGGER.warn("couldn't save settings: {}", e.toString());
        }
    }

    public Loadout loadout() {
        return Loadout.fromJson(loadout);
    }
}

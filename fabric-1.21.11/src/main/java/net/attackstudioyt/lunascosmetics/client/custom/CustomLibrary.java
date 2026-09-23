package net.attackstudioyt.lunascosmetics.client.custom;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Cosmetic;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Slot;
import net.attackstudioyt.lunascosmetics.net.ServerSync;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Luna's own cosmetics: any .bbmodel dropped into config/lunascosmetics/custom/. The
 * file name picks the slot - "hat_bow.bbmodel", "back_cape.bbmodel", "pet_bunny.bbmodel"
 * (no prefix = hat). Edits are picked up live. Models that arrive from other players are
 * cached by hash under cache/ and never show up in her own wardrobe.
 */
public final class CustomLibrary {
    private CustomLibrary() {
    }

    private static final Map<String, CustomCosmetic> LOCAL = new HashMap<>();
    private static final List<CustomCosmetic> LOCAL_ORDER = new ArrayList<>();
    private static final Map<String, Long> STAMPS = new HashMap<>();
    private static final Map<String, CustomCosmetic> REMOTE = new HashMap<>();
    private static final Map<String, Boolean> REMOTE_FAILED = new HashMap<>();
    private static final Map<String, byte[]> BYTES = new HashMap<>();
    private static long lastScan;
    private static Runnable onChange = () -> {
    };

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lunascosmetics/custom");
    }

    private static Path cacheDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lunascosmetics/cache");
    }

    public static void setOnChange(Runnable r) {
        onChange = r;
    }

    /** First run: create the folder with a how-to and an example she can open in Blockbench. */
    public static void ensureFolder() {
        try {
            Files.createDirectories(dir());
            Path readme = dir().resolve("HOW TO ADD COSMETICS.txt");
            if (!Files.exists(readme)) {
                copyResource("/assets/lunascosmetics/examples/README.txt", readme);
                copyResource("/assets/lunascosmetics/examples/hat_cherry_bow.bbmodel", dir().resolve("hat_cherry_bow.bbmodel"));
            }
        } catch (Exception e) {
            LunasCosmetics.LOGGER.warn("couldn't set up custom folder: {}", e.toString());
        }
    }

    private static void copyResource(String res, Path to) throws Exception {
        try (InputStream in = CustomLibrary.class.getResourceAsStream(res)) {
            if (in != null) {
                Files.write(to, in.readAllBytes());
            }
        }
    }

    /** Re-reads the folder if anything changed. Cheap enough to call every couple of seconds. */
    public static void rescan(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastScan < 2000) {
            return;
        }
        lastScan = now;
        Map<String, Long> seen = new HashMap<>();
        try (Stream<Path> files = Files.list(dir())) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".bbmodel"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            seen.put(p.getFileName().toString(), Files.getLastModifiedTime(p).toMillis() ^ Files.size(p));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            return;
        }
        if (!force && seen.equals(STAMPS)) {
            return;
        }
        STAMPS.clear();
        STAMPS.putAll(seen);
        LOCAL.clear();
        LOCAL_ORDER.clear();
        for (String file : seen.keySet().stream().sorted().toList()) {
            try {
                byte[] data = Files.readAllBytes(dir().resolve(file));
                String hash = ServerSync.sha1(data);
                BbModel model = BbModel.parse(new String(data, StandardCharsets.UTF_8), "c" + hash.substring(0, 12));
                String base = file.substring(0, file.length() - ".bbmodel".length());
                Slot slot = Slot.HAT;
                String lower = base.toLowerCase(Locale.ROOT);
                for (Slot s : Slot.values()) {
                    String prefix = s.name().toLowerCase(Locale.ROOT) + "_";
                    if (lower.startsWith(prefix)) {
                        slot = s;
                        base = base.substring(prefix.length());
                    }
                }
                CustomCosmetic c = new CustomCosmetic(hash, file, prettify(base), slot, model);
                LOCAL.put(c.id, c);
                LOCAL_ORDER.add(c);
                BYTES.put(hash, data);
            } catch (Exception e) {
                LunasCosmetics.LOGGER.warn("couldn't load custom cosmetic {}: {}", file, e.toString());
            }
        }
        onChange.run();
    }

    private static String prettify(String s) {
        StringBuilder out = new StringBuilder();
        for (String w : s.replace('-', ' ').replace('_', ' ').trim().split("\\s+")) {
            if (!w.isEmpty()) {
                out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
        }
        return out.toString().trim();
    }

    public static List<Cosmetic> local() {
        return new ArrayList<>(LOCAL_ORDER);
    }

    /** Bytes of one of her own models, for uploading to friends. */
    public static byte[] bytes(String hash) {
        return BYTES.get(hash);
    }

    /** Looks up a custom cosmetic by id: hers first, then anything friends have sent. */
    public static Cosmetic get(String id) {
        CustomCosmetic c = LOCAL.get(id);
        if (c != null) {
            return c;
        }
        if (!id.startsWith("custom:")) {
            return null;
        }
        String hash = id.substring("custom:".length());
        c = REMOTE.get(hash);
        if (c != null || REMOTE_FAILED.containsKey(hash)) {
            return c;
        }
        Path cached = cacheDir().resolve(hash + ".bbmodel");
        if (!Files.exists(cached)) {
            return null; // still on its way
        }
        try {
            byte[] data = Files.readAllBytes(cached);
            BbModel model = BbModel.parse(new String(data, StandardCharsets.UTF_8), "r" + hash.substring(0, 12));
            c = new CustomCosmetic(hash, hash, "Friend's cosmetic", Slot.HAT, model);
            REMOTE.put(hash, c);
            return c;
        } catch (Exception e) {
            REMOTE_FAILED.put(hash, true);
            return null;
        }
    }

    public static boolean hasCached(String hash) {
        return LOCAL.containsKey("custom:" + hash) || Files.exists(cacheDir().resolve(hash + ".bbmodel"));
    }

    /** A friend's model finished arriving. */
    public static void storeRemote(String hash, byte[] data) {
        if (!hash.equals(ServerSync.sha1(data))) {
            return;
        }
        try {
            Files.createDirectories(cacheDir());
            Files.write(cacheDir().resolve(hash + ".bbmodel"), data);
            REMOTE_FAILED.remove(hash);
        } catch (Exception e) {
            LunasCosmetics.LOGGER.warn("couldn't cache friend's model: {}", e.toString());
        }
    }
}

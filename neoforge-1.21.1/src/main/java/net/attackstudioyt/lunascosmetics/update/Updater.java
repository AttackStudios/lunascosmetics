package net.attackstudioyt.lunascosmetics.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.BiConsumer;
import java.util.zip.ZipFile;

/**
 * Keeps Luna's Cosmetics up to date by itself, like Essential does.
 *
 * <p>On launch it asks GitHub for the newest release, downloads the jar built for this
 * loader and Minecraft version into a staging folder (checking GitHub's SHA-256 digest and
 * that it really is this mod), then - once the game closes - a tiny helper process
 * ({@link Swap}) replaces the old jar with the new one. The running game is never touched,
 * so it works on Windows too, where a loaded jar can't be deleted.
 */
public final class Updater {
    private Updater() {
    }

    public static final String REPO = "AttackStudios/lunascosmetics";

    private static volatile String status = "Checking for updates...";
    private static volatile boolean started;

    public static String status() {
        return status;
    }

    /**
     * @param current     this mod's version, e.g. "1.1.0"
     * @param assetPrefix release asset name prefix for this build, e.g. "lunascosmetics-fabric-1.21.11-"
     * @param jar         the jar this mod was loaded from (null / not a file in a dev run = do nothing)
     * @param gameDir     the game directory (staging lives under it)
     * @param onReady     called with (newVersion, message) when an update is staged
     */
    public static void start(String current, String assetPrefix, Path jar, Path gameDir, BiConsumer<String, String> onReady) {
        if (started) {
            return;
        }
        started = true;
        if (jar == null || !Files.isRegularFile(jar) || !jar.toString().endsWith(".jar")) {
            status = "Auto-update is off in development builds";
            return;
        }
        Thread t = new Thread(() -> run(current, assetPrefix, jar, gameDir, onReady), "LunasCosmetics-Updater");
        t.setDaemon(true);
        t.start();
    }

    private static void run(String current, String assetPrefix, Path jar, Path gameDir, BiConsumer<String, String> onReady) {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "LunasCosmetics-Updater")
                    .timeout(Duration.ofSeconds(15)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                status = "Couldn't check for updates (" + res.statusCode() + ")";
                return;
            }
            JsonObject release = JsonParser.parseString(res.body()).getAsJsonObject();
            String latest = release.get("tag_name").getAsString().replaceFirst("^[vV]", "");
            if (compare(latest, current) <= 0) {
                status = "Up to date (v" + current + ")";
                return;
            }
            JsonObject asset = null;
            for (JsonElement el : release.getAsJsonArray("assets")) {
                JsonObject a = el.getAsJsonObject();
                String name = a.get("name").getAsString();
                if (name.startsWith(assetPrefix) && name.endsWith(".jar")) {
                    asset = a;
                    break;
                }
            }
            if (asset == null) {
                status = "v" + latest + " is out, but not for this Minecraft version yet";
                return;
            }
            String name = asset.get("name").getAsString();
            if (!name.matches("[A-Za-z0-9._+-]+")) {
                return;
            }
            status = "Downloading v" + latest + "...";
            Path staging = gameDir.resolve("lunascosmetics-update");
            Files.createDirectories(staging);
            Path part = staging.resolve(name + ".part");
            HttpRequest dl = HttpRequest.newBuilder(URI.create(asset.get("browser_download_url").getAsString()))
                    .header("User-Agent", "LunasCosmetics-Updater").timeout(Duration.ofMinutes(2)).build();
            HttpResponse<Path> file = http.send(dl, HttpResponse.BodyHandlers.ofFile(part));
            if (file.statusCode() != 200) {
                Files.deleteIfExists(part);
                status = "Update download failed (" + file.statusCode() + ")";
                return;
            }
            byte[] bytes = Files.readAllBytes(part);
            // GitHub publishes a SHA-256 for every asset; refuse anything that doesn't match
            if (asset.has("digest") && !asset.get("digest").isJsonNull()) {
                String want = asset.get("digest").getAsString().replaceFirst("^sha256:", "");
                String got = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!want.equalsIgnoreCase(got)) {
                    Files.deleteIfExists(part);
                    status = "Update failed its checksum - skipped";
                    return;
                }
            }
            if (!isThisMod(part)) {
                Files.deleteIfExists(part);
                status = "Downloaded file isn't Luna's Cosmetics - skipped";
                return;
            }
            Path staged = staging.resolve(name);
            Files.move(part, staged, StandardCopyOption.REPLACE_EXISTING);
            scheduleSwap(jar, staged);
            status = "v" + latest + " downloaded - it installs when you close the game";
            onReady.accept(latest, status);
            LunasCosmetics.LOGGER.info("Update v{} staged at {}", latest, staged);
        } catch (Exception e) {
            status = "Couldn't check for updates";
            LunasCosmetics.LOGGER.info("update check failed: {}", e.toString());
        }
    }

    /** The staged jar must be a real zip carrying this mod's id. */
    private static boolean isThisMod(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String meta : new String[]{"fabric.mod.json", "META-INF/neoforge.mods.toml"}) {
                var entry = zip.getEntry(meta);
                if (entry != null) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        return new String(in.readAllBytes()).contains("\"" + LunasCosmetics.MOD_ID + "\"");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** When the JVM exits, start the helper that swaps the jars once this process is gone. */
    private static void scheduleSwap(Path oldJar, Path newJar) {
        String java = ProcessHandle.current().info().command().orElse("java");
        long pid = ProcessHandle.current().pid();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                new ProcessBuilder(java, "-cp", newJar.toAbsolutePath().toString(),
                        Swap.class.getName(), Long.toString(pid),
                        oldJar.toAbsolutePath().toString(), newJar.toAbsolutePath().toString())
                        .redirectErrorStream(true)
                        .redirectOutput(newJar.resolveSibling("swap.log").toFile())
                        .start();
            } catch (Exception ignored) {
                // next launch will download again
            }
        }, "LunasCosmetics-Swap"));
    }

    /** Semantic-ish version compare: 1.10.0 > 1.9.2. */
    static int compare(String a, String b) {
        String[] x = a.split("[.+-]");
        String[] y = b.split("[.+-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0;
            int q = i < y.length ? num(y[i]) : 0;
            if (p != q) {
                return Integer.compare(p, q);
            }
        }
        return 0;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D.*", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}

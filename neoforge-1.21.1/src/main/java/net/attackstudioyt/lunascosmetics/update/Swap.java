package net.attackstudioyt.lunascosmetics.update;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Standalone helper, run with plain {@code java -cp <new jar>} after the game has closed:
 * waits for the game process to exit, then puts the new jar where the old one was.
 * Uses only the JDK - nothing from Minecraft or the mod loader is on its classpath.
 *
 * <p>args: gamePid oldJar newJar
 */
public final class Swap {
    private Swap() {
    }

    public static void main(String[] args) throws Exception {
        long pid = Long.parseLong(args[0]);
        Path oldJar = Path.of(args[1]);
        Path newJar = Path.of(args[2]);
        ProcessHandle.of(pid).ifPresent(h -> h.onExit().join());
        Path target = oldJar.resolveSibling(newJar.getFileName());
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                Files.copy(newJar, target, StandardCopyOption.REPLACE_EXISTING);
                if (!oldJar.equals(target)) {
                    Files.deleteIfExists(oldJar);
                }
                Files.deleteIfExists(newJar);
                System.out.println("Luna's Cosmetics updated: " + target);
                return;
            } catch (Exception e) {
                // Windows can hold the file a moment after exit
                Thread.sleep(1000);
            }
        }
        // couldn't remove the old jar: take the new one back out so the two never both load
        Files.deleteIfExists(target.equals(oldJar) ? Path.of("__none__") : target);
        System.out.println("Update could not be applied; will retry next launch.");
    }
}

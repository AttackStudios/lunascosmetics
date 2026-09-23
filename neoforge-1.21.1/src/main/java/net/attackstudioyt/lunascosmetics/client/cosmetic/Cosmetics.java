package net.attackstudioyt.lunascosmetics.client.cosmetic;

import net.attackstudioyt.lunascosmetics.client.custom.CustomLibrary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every cosmetic the wardrobe knows. Built-ins are registered here in display order;
 * Luna's own Blockbench files are merged in from {@link CustomLibrary}.
 */
public final class Cosmetics {
    private Cosmetics() {
    }

    private static final Map<String, Cosmetic> BUILTIN = new LinkedHashMap<>();

    public static void registerBuiltins() {
        // ---- pets --------------------------------------------------------------------
        cat("snowball", "Snowball", "A fluffy white cat with pink ears. Loves naps.", 0xFFF6F2F4, false, false);
        cat("luna", "Luna", "A black cat with a glowing golden moon. Sound familiar?", 0xFF26222F, true, false);
        cat("stargazer", "Stargazer", "Covered in twinkling stars. Watches them too.", 0xFF1F2B5E, true, true);
        cat("sakura", "Sakura", "Pink as a cherry blossom, with flowers in her fur.", 0xFFF8C7D7, false, false);
        cat("marmalade", "Marmalade", "A stripy ginger troublemaker.", 0xFFEB9440, false, false);
        cat("tuxedo", "Tuxedo", "Always dressed for a party.", 0xFF1E1E25, false, false);
        cat("calico", "Calico", "Three colours, zero patience.", 0xFFE8913A, false, false);
        cat("siamese", "Siamese", "Very chatty. Very blue-eyed.", 0xFFF3E7D0, false, false);
        cat("smokey", "Smokey", "A grey tabby who's seen things.", 0xFF8F929A, false, false);
        cat("cocoa", "Cocoa", "Warm, brown and extremely cuddly.", 0xFF7B4F33, false, false);
        add(new PetCosmetic("mini_moosh", "Mini Moosh", "A tiny pink moo-pig with a sprout on top. Boing!",
                0xFFF4A7BA, PetCosmetic.Kind.MOOSH, "pet", "mini_moosh", false, false));

        // ---- hats --------------------------------------------------------------------
        add(new ModelCosmetic("sakura_crown", "Sakura Crown", "A crown of cherry blossoms.", Slot.HAT,
                0xFFFF8FB4, "sakura_crown", "hat/sakura_crown", false, ModelCosmetic.Motion.NONE, 1.6f, 0));
        add(new ModelCosmetic("kitty_ears_pink", "Pink Kitty Ears", "Now you're the cat.", Slot.HAT,
                0xFFF7A8C4, "kitty_ears", "hat/kitty_ears_pink", false, ModelCosmetic.Motion.EARS, 1.6f, 0));
        add(new ModelCosmetic("kitty_ears_midnight", "Midnight Kitty Ears", "For night owls. Night cats.", Slot.HAT,
                0xFF26222F, "kitty_ears", "hat/kitty_ears_midnight", false, ModelCosmetic.Motion.EARS, 1.6f, 0));
        add(new ModelCosmetic("kitty_ears_snow", "Snow Kitty Ears", "Soft, white and very twitchy.", Slot.HAT,
                0xFFF7F4F6, "kitty_ears", "hat/kitty_ears_snow", false, ModelCosmetic.Motion.EARS, 1.6f, 0));
        add(new ModelCosmetic("star_halo", "Star Halo", "A ring of starlight that follows you around.", Slot.HAT,
                0xFFFFD84A, "star_halo", "hat/star_halo", true, ModelCosmetic.Motion.HALO, 1.5f, -2));

        // ---- back --------------------------------------------------------------------
        add(new ModelCosmetic("petal_wings", "Petal Wings", "Cherry-blossom fairy wings. Flutter flutter.", Slot.BACK,
                0xFFFFC1D6, "petal_wings", "back/petal_wings", false, ModelCosmetic.Motion.WINGS, 1.1f, 2));
        add(new ModelCosmetic("starry_wings", "Starry Wings", "Wings made of the night sky. The stars twinkle!", Slot.BACK,
                0xFF4B3BA8, "starry_wings", "back/starry_wings", true, ModelCosmetic.Motion.WINGS, 1.1f, 2));
    }

    private static void cat(String id, String name, String desc, int accent, boolean glow, boolean twinkle) {
        add(new PetCosmetic(id, name, desc, accent, PetCosmetic.Kind.CAT, "cat", id, glow, twinkle));
    }

    private static void add(Cosmetic c) {
        BUILTIN.put(c.id, c);
    }

    public static Cosmetic get(String id) {
        if (id == null) {
            return null;
        }
        Cosmetic c = BUILTIN.get(id);
        return c != null ? c : CustomLibrary.get(id);
    }

    /** Builtins for a slot, followed by Luna's own custom ones for it. */
    public static List<Cosmetic> forSlot(Slot slot) {
        List<Cosmetic> out = new ArrayList<>();
        for (Cosmetic c : BUILTIN.values()) {
            if (c.slot == slot) {
                out.add(c);
            }
        }
        return out;
    }

    public static List<Cosmetic> custom() {
        return CustomLibrary.local();
    }
}

package net.attackstudioyt.lunascosmetics.client.cosmetic;

/** Where a cosmetic lives on the body. One of each can be worn at once. */
public enum Slot {
    PET("Pets", "icon_pets"),
    HAT("Hats", "icon_hats"),
    BACK("Back", "icon_back");

    public final String title;
    public final String icon;

    Slot(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public static Slot parse(String s, Slot fallback) {
        for (Slot slot : values()) {
            if (slot.name().equalsIgnoreCase(s)) {
                return slot;
            }
        }
        return fallback;
    }
}

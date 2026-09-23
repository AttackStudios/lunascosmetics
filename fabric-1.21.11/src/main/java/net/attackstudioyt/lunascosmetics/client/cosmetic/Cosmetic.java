package net.attackstudioyt.lunascosmetics.client.cosmetic;

/**
 * Anything wearable. To add a new one: give it a model JSON + texture (tools/art.py
 * shows how) and register it in {@link Cosmetics#registerBuiltins()} - or, without any
 * code, drop a Blockbench file into config/lunascosmetics/custom/.
 */
public abstract class Cosmetic {
    public final String id;
    public final String name;
    public final String description;
    public final Slot slot;
    /** card accent colour (ARGB) */
    public final int accent;

    protected Cosmetic(String id, String name, String description, Slot slot, int accent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slot = slot;
        this.accent = accent;
    }

    public abstract void render(RenderCtx ctx);

    /** Scale and lift for the wardrobe card preview. */
    public float cardScale() {
        return 1.0f;
    }

    public float cardLift() {
        return 0.0f;
    }

    /** Extra lift (pixels) when a pet occupies the top of the head; only floating things want it. */
    public float liftOverPet(boolean petSitting) {
        return 0;
    }

    public boolean isCustom() {
        return false;
    }
}

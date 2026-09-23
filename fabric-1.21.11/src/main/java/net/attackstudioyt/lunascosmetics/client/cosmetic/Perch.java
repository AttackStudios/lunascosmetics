package net.attackstudioyt.lunascosmetics.client.cosmetic;

/** Where a pet rides. */
public enum Perch {
    HEAD("Lying on head"),
    HEAD_SIT("Sitting on head"),
    LEFT_SHOULDER("Left shoulder"),
    RIGHT_SHOULDER("Right shoulder");

    public final String label;

    Perch(String label) {
        this.label = label;
    }

    public boolean onHead() {
        return this == HEAD || this == HEAD_SIT;
    }

    public boolean sitting() {
        return this != HEAD;
    }

    public Perch next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Perch parse(String s) {
        for (Perch p : values()) {
            if (p.name().equalsIgnoreCase(s)) {
                return p;
            }
        }
        return HEAD;
    }
}

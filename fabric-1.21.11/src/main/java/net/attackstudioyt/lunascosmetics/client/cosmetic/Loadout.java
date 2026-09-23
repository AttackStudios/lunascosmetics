package net.attackstudioyt.lunascosmetics.client.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * What one player is wearing: at most one cosmetic per {@link Slot}, plus where the pet
 * rides. Travels over the wire as a tiny JSON object so new fields never break older
 * copies of the mod (unknown keys and unknown ids are simply ignored).
 */
public final class Loadout {
    public static final Loadout EMPTY = new Loadout();

    private final Map<Slot, String> worn = new EnumMap<>(Slot.class);
    private Perch perch = Perch.HEAD;

    public String get(Slot slot) {
        return worn.get(slot);
    }

    public Perch perch() {
        return perch;
    }

    public Loadout with(Slot slot, String id) {
        Loadout l = copy();
        if (id == null) {
            l.worn.remove(slot);
        } else {
            l.worn.put(slot, id);
        }
        return l;
    }

    public Loadout withPerch(Perch p) {
        Loadout l = copy();
        l.perch = p;
        return l;
    }

    public boolean isEmpty() {
        return worn.isEmpty();
    }

    public Map<Slot, String> worn() {
        return worn;
    }

    private Loadout copy() {
        Loadout l = new Loadout();
        l.worn.putAll(worn);
        l.perch = perch;
        return l;
    }

    public String toJson() {
        JsonObject o = new JsonObject();
        for (Map.Entry<Slot, String> e : worn.entrySet()) {
            o.addProperty(e.getKey().name().toLowerCase(), e.getValue());
        }
        if (perch != Perch.HEAD) {
            o.addProperty("perch", perch.name().toLowerCase());
        }
        return o.toString();
    }

    public static Loadout fromJson(String json) {
        Loadout l = new Loadout();
        if (json == null || json.isBlank()) {
            return l;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            for (Slot slot : Slot.values()) {
                String key = slot.name().toLowerCase();
                if (o.has(key) && o.get(key).isJsonPrimitive()) {
                    l.worn.put(slot, o.get(key).getAsString());
                }
            }
            if (o.has("perch")) {
                l.perch = Perch.parse(o.get("perch").getAsString());
            }
        } catch (Exception ignored) {
            // a garbled loadout just means "nothing"
        }
        return l;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Loadout l && l.worn.equals(worn) && l.perch == perch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(worn, perch);
    }
}

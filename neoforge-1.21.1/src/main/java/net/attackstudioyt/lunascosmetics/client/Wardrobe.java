package net.attackstudioyt.lunascosmetics.client;

import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.sync.ClientSync;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Who is wearing what: Luna's own loadout (saved) and everyone else's (synced). */
public final class Wardrobe {
    private Wardrobe() {
    }

    private static Loadout mine;
    private static final Map<UUID, Loadout> OTHERS = new HashMap<>();

    public static Loadout mine() {
        if (mine == null) {
            mine = ClientConfig.get().loadout();
        }
        return mine;
    }

    public static void setMine(Loadout loadout) {
        if (loadout.equals(mine)) {
            return;
        }
        mine = loadout;
        ClientConfig.get().loadout = loadout.toJson();
        ClientConfig.save();
        ClientSync.publish();
    }

    public static Loadout of(UUID player) {
        return OTHERS.getOrDefault(player, Loadout.EMPTY);
    }

    public static void putOther(UUID player, String json) {
        if (json == null || json.isEmpty()) {
            OTHERS.remove(player);
        } else {
            OTHERS.put(player, Loadout.fromJson(json));
        }
    }

    public static void clearOthers() {
        OTHERS.clear();
    }
}

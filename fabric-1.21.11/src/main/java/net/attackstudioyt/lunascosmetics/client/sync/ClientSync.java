package net.attackstudioyt.lunascosmetics.client.sync;

import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.Wardrobe;
import net.attackstudioyt.lunascosmetics.client.anim.Brain;
import net.attackstudioyt.lunascosmetics.client.cosmetic.Loadout;
import net.attackstudioyt.lunascosmetics.client.custom.CustomLibrary;
import net.attackstudioyt.lunascosmetics.net.LoadoutPayload;
import net.attackstudioyt.lunascosmetics.net.LoadoutsPayload;
import net.attackstudioyt.lunascosmetics.net.ModelChunkPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps everyone with the mod seeing each other's cosmetics.
 *
 * <ol>
 *   <li>If the server has Luna's Cosmetics too, loadouts ride its own channel. Nothing
 *       to set up.</li>
 *   <li>Otherwise, if a relay URL is set, a small WebSocket relay does the same job,
 *       with one "room" per server address.</li>
 * </ol>
 * Custom Blockbench models are sent once per session, addressed by hash.
 */
public final class ClientSync {
    private ClientSync() {
    }

    private static final Set<String> SENT_MODELS = new HashSet<>();
    private static final Map<String, byte[][]> INCOMING = new HashMap<>();
    private static int publishIn = -1;
    private static boolean serverHasMod;
    private static int relayCheck;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LoadoutsPayload.ID, (payload, ctx) -> {
            if (payload.full()) {
                Wardrobe.clearOthers();
            }
            UUID me = ctx.player() == null ? null : ctx.player().getUuid();
            net.attackstudioyt.lunascosmetics.LunasCosmetics.LOGGER.debug("[sync] {} loadout(s) from server (full={})",
                    payload.loadouts().size(), payload.full());
            for (Map.Entry<UUID, String> e : payload.loadouts().entrySet()) {
                if (!e.getKey().equals(me)) {
                    Wardrobe.putOther(e.getKey(), e.getValue());
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ModelChunkPayload.ID, (payload, ctx) ->
                acceptChunk(payload.hash(), payload.index(), payload.total(), payload.data()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SENT_MODELS.clear();
            INCOMING.clear();
            Wardrobe.clearOthers();
            serverHasMod = false;
            publishIn = 20; // give channel registration a second to land
            RelayClient.leave();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Wardrobe.clearOthers();
            SENT_MODELS.clear();
            INCOMING.clear();
            serverHasMod = false;
            publishIn = -1;
            RelayClient.leave();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (publishIn > 0 && --publishIn == 0) {
                serverHasMod = ClientPlayNetworking.canSend(LoadoutPayload.ID);
                if (!serverHasMod) {
                    RelayClient.join(room(client));
                }
                publish();
            }
            // keep the fallback relay in step with the settings (pasted URL, relay restarted...)
            if (publishIn <= 0 && !serverHasMod && ++relayCheck >= 60 && client.getNetworkHandler() != null) {
                relayCheck = 0;
                RelayClient.ensure(room(client));
            }
        });
    }

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static String status() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return "Not in a world";
        }
        if (serverHasMod) {
            return "Syncing through this server";
        }
        if (RelayClient.connected()) {
            return "Syncing through the relay";
        }
        if (ClientConfig.get().relayUrl.isBlank()) {
            return "Only you can see them here (server doesn't have the mod)";
        }
        return "Connecting to relay...";
    }

    private static String room(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        return info != null ? info.address.toLowerCase() : "singleplayer";
    }

    /** Tell everyone what Luna is wearing now. */
    public static void publish() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null || publishIn > 0) {
            return;
        }
        Loadout mine = Wardrobe.mine();
        String json = mine.toJson();
        if (serverHasMod) {
            // loadout first: the server only keeps models somebody is wearing
            ClientPlayNetworking.send(new LoadoutPayload(json));
            for (String hash : customHashes(mine)) {
                byte[] data = CustomLibrary.bytes(hash);
                if (data != null && SENT_MODELS.add(hash)) {
                    forEachChunk(data, (i, total, slice) ->
                            ClientPlayNetworking.send(new ModelChunkPayload(hash, i, total, slice)));
                }
            }
        } else {
            RelayClient.sendLoadout(client.player.getUuid(), json);
            for (String hash : customHashes(mine)) {
                byte[] data = CustomLibrary.bytes(hash);
                if (data != null && SENT_MODELS.add(hash)) {
                    forEachChunk(data, (i, total, slice) -> RelayClient.sendChunk(hash, i, total, slice));
                }
            }
        }
    }

    /** relay reconnected: our models need resending there */
    static void relayReconnected() {
        SENT_MODELS.clear();
        publish();
    }

    static void acceptChunk(String hash, int index, int total, byte[] data) {
        if (CustomLibrary.hasCached(hash) || total <= 0 || total > 80 || index < 0 || index >= total) {
            return;
        }
        byte[][] parts = INCOMING.computeIfAbsent(hash, k -> new byte[total][]);
        if (parts.length != total) {
            return;
        }
        parts[index] = data;
        for (byte[] p : parts) {
            if (p == null) {
                return;
            }
        }
        INCOMING.remove(hash);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        CustomLibrary.storeRemote(hash, out.toByteArray());
    }

    private static Set<String> customHashes(Loadout l) {
        Set<String> out = new HashSet<>();
        for (String id : l.worn().values()) {
            if (id.startsWith("custom:")) {
                out.add(id.substring(7));
            }
        }
        return out;
    }

    interface ChunkSink {
        void accept(int index, int total, byte[] slice);
    }

    private static void forEachChunk(byte[] data, ChunkSink sink) {
        if (data.length > ModelChunkPayload.MAX_BYTES) {
            return;
        }
        int total = Math.max(1, (data.length + ModelChunkPayload.CHUNK - 1) / ModelChunkPayload.CHUNK);
        for (int i = 0; i < total; i++) {
            int from = i * ModelChunkPayload.CHUNK;
            sink.accept(i, total, Arrays.copyOfRange(data, from, Math.min(data.length, from + ModelChunkPayload.CHUNK)));
        }
    }

    static void forgetPlayer(UUID id) {
        Wardrobe.putOther(id, "");
        Brain.forget(id);
    }
}

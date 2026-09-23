package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server's whole job: remember what each modded player is wearing, tell the other
 * modded players, and pass custom model files along. Players without the mod never
 * receive a byte of it.
 */
public final class ServerSync {
    private ServerSync() {
    }

    private static final Pattern CUSTOM_REF = Pattern.compile("custom:([0-9a-f]{40})");
    private static final int MAX_STORED_BYTES = 48_000_000;

    private static final Map<UUID, String> LOADOUTS = new HashMap<>();
    private static final Map<String, byte[]> MODELS = new HashMap<>();
    /** uploads in flight: player -> hash -> chunks */
    private static final Map<UUID, Map<String, byte[][]>> PARTIAL = new HashMap<>();
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(LoadoutPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            String json = payload.json();
            if (json.length() > 4000) {
                return;
            }
            LOADOUTS.put(player.getUuid(), json);
            broadcast(ctx.server(), new LoadoutsPayload(false, Map.of(player.getUuid(), json)), null);
            // Make sure everyone already has any custom models this loadout points at.
            for (String hash : customRefs(json)) {
                byte[] data = MODELS.get(hash);
                if (data != null) {
                    for (ServerPlayerEntity other : ctx.server().getPlayerManager().getPlayerList()) {
                        if (other != player && canReceive(other)) {
                            sendModel(other, hash, data);
                        }
                    }
                }
            }
            prune();
        });

        ServerPlayNetworking.registerGlobalReceiver(ModelChunkPayload.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            String hash = payload.hash();
            int total = payload.total();
            if (total <= 0 || total * (long) ModelChunkPayload.CHUNK > ModelChunkPayload.MAX_BYTES
                    || payload.index() < 0 || payload.index() >= total || MODELS.containsKey(hash)) {
                return;
            }
            byte[][] parts = PARTIAL.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                    .computeIfAbsent(hash, k -> new byte[total][]);
            if (parts.length != total) {
                return;
            }
            parts[payload.index()] = payload.data();
            for (byte[] p : parts) {
                if (p == null) {
                    return;
                }
            }
            PARTIAL.get(player.getUuid()).remove(hash);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] p : parts) {
                out.writeBytes(p);
            }
            byte[] data = out.toByteArray();
            if (!hash.equals(sha1(data))) {
                LunasCosmetics.LOGGER.warn("custom model from {} failed its hash check", player.getName().getString());
                return;
            }
            MODELS.put(hash, data);
            for (ServerPlayerEntity other : ctx.server().getPlayerManager().getPlayerList()) {
                if (other != player && canReceive(other)) {
                    sendModel(other, hash, data);
                }
            }
            prune();
        });

        // The client's channel registration lands a moment after join, so greetings wait
        // (up to 5 s) until the server knows the player can hear us.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PENDING.put(handler.getPlayer().getUuid(), 0));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) {
                return;
            }
            var it = PENDING.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(e.getKey());
                if (player == null || e.getValue() > 100) {
                    it.remove();
                } else if (canReceive(player)) {
                    it.remove();
                    greet(player);
                } else {
                    e.setValue(e.getValue() + 1);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            PARTIAL.remove(id);
            PENDING.remove(id);
            if (LOADOUTS.remove(id) != null) {
                broadcast(server, new LoadoutsPayload(false, Map.of(id, "")), null);
                prune();
            }
        });
    }

    private static void greet(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new LoadoutsPayload(true, new HashMap<>(LOADOUTS)));
        Set<String> needed = new HashSet<>();
        for (String json : LOADOUTS.values()) {
            needed.addAll(customRefs(json));
        }
        for (String hash : needed) {
            byte[] data = MODELS.get(hash);
            if (data != null) {
                sendModel(player, hash, data);
            }
        }
    }

    private static boolean canReceive(ServerPlayerEntity player) {
        return ServerPlayNetworking.canSend(player, LoadoutsPayload.ID);
    }

    private static void broadcast(MinecraftServer server, LoadoutsPayload payload, ServerPlayerEntity except) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p != except && canReceive(p)) {
                ServerPlayNetworking.send(p, payload);
            }
        }
    }

    private static void sendModel(ServerPlayerEntity player, String hash, byte[] data) {
        int total = Math.max(1, (data.length + ModelChunkPayload.CHUNK - 1) / ModelChunkPayload.CHUNK);
        for (int i = 0; i < total; i++) {
            int from = i * ModelChunkPayload.CHUNK;
            int to = Math.min(data.length, from + ModelChunkPayload.CHUNK);
            byte[] slice = java.util.Arrays.copyOfRange(data, from, to);
            ServerPlayNetworking.send(player, new ModelChunkPayload(hash, i, total, slice));
        }
    }

    /** Drop models nobody is wearing any more, and keep the store bounded. */
    private static void prune() {
        Set<String> live = new HashSet<>();
        for (String json : LOADOUTS.values()) {
            live.addAll(customRefs(json));
        }
        MODELS.keySet().retainAll(live);
        long total = 0;
        for (byte[] b : MODELS.values()) {
            total += b.length;
        }
        if (total > MAX_STORED_BYTES) {
            MODELS.clear();
        }
    }

    static Set<String> customRefs(String json) {
        Set<String> out = new HashSet<>();
        Matcher m = CUSTOM_REF.matcher(json);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

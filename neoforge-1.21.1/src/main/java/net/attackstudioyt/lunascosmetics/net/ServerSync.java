package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
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

    public static void init() {
        // NeoForge negotiates channels before login, so we can greet straight away.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && canReceive(p)) {
                greet(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) {
                return;
            }
            UUID id = p.getUUID();
            PARTIAL.remove(id);
            if (LOADOUTS.remove(id) != null) {
                broadcast(p.server, new LoadoutsPayload(false, Map.of(id, "")), p);
                prune();
            }
        });
    }

    static void onLoadout(LoadoutPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        String json = payload.json();
        if (json.length() > 4000) {
            return;
        }
        LOADOUTS.put(player.getUUID(), json);
        broadcast(player.server, new LoadoutsPayload(false, Map.of(player.getUUID(), json)), null);
        // Make sure everyone already has any custom models this loadout points at.
        for (String hash : customRefs(json)) {
            byte[] data = MODELS.get(hash);
            if (data != null) {
                for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                    if (other != player && canReceive(other)) {
                        sendModel(other, hash, data);
                    }
                }
            }
        }
        prune();
    }

    static void onChunk(ModelChunkPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        String hash = payload.hash();
        int total = payload.total();
        if (total <= 0 || total * (long) ModelChunkPayload.CHUNK > ModelChunkPayload.MAX_BYTES
                || payload.index() < 0 || payload.index() >= total || MODELS.containsKey(hash)) {
            return;
        }
        byte[][] parts = PARTIAL.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
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
        PARTIAL.get(player.getUUID()).remove(hash);
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
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player && canReceive(other)) {
                sendModel(other, hash, data);
            }
        }
        prune();
    }

    private static void greet(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new LoadoutsPayload(true, new HashMap<>(LOADOUTS)));
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

    private static boolean canReceive(ServerPlayer player) {
        return player.connection.hasChannel(LoadoutsPayload.TYPE);
    }

    private static void broadcast(MinecraftServer server, LoadoutsPayload payload, ServerPlayer except) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p != except && canReceive(p)) {
                PacketDistributor.sendToPlayer(p, payload);
            }
        }
    }

    private static void sendModel(ServerPlayer player, String hash, byte[] data) {
        int total = Math.max(1, (data.length + ModelChunkPayload.CHUNK - 1) / ModelChunkPayload.CHUNK);
        for (int i = 0; i < total; i++) {
            int from = i * ModelChunkPayload.CHUNK;
            int to = Math.min(data.length, from + ModelChunkPayload.CHUNK);
            PacketDistributor.sendToPlayer(player, new ModelChunkPayload(hash, i, total, Arrays.copyOfRange(data, from, to)));
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

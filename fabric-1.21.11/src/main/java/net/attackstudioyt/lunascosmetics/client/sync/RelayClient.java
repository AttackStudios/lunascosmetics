package net.attackstudioyt.lunascosmetics.client.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.attackstudioyt.lunascosmetics.client.ClientConfig;
import net.attackstudioyt.lunascosmetics.client.Wardrobe;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fallback sync for servers that don't run the mod: a tiny WebSocket relay (see
 * relay/server.js in the project) with one room per server address. JDK WebSocket, no
 * extra libraries. Everything received is handed to the client thread.
 */
public final class RelayClient {
    private RelayClient() {
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LunasCosmetics-Relay");
        t.setDaemon(true);
        return t;
    });

    private static volatile WebSocket socket;
    private static volatile String room;
    private static volatile boolean connecting;
    /** the relay URL the current connection (or attempt) is for */
    private static volatile String activeUrl = "";
    private static volatile int generation;
    private static CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);

    public static boolean connected() {
        return socket != null;
    }

    /**
     * Called every few seconds while on a server without the mod: connects as soon as a
     * relay URL is set, follows URL changes, and retries if the relay was down.
     */
    public static synchronized void ensure(String wantedRoom) {
        String url = ClientConfig.get().relayUrl == null ? "" : ClientConfig.get().relayUrl.trim();
        if (url.isEmpty()) {
            if (socket != null || room != null) {
                leave();
            }
            return;
        }
        if (!wantedRoom.equals(room) || !url.equals(activeUrl)) {
            join(wantedRoom);
        } else if (socket == null && !connecting) {
            connect(generation);
        }
    }

    public static synchronized void join(String newRoom) {
        leave();
        room = newRoom;
        connect(generation);
    }

    public static synchronized void leave() {
        generation++;
        room = null;
        WebSocket ws = socket;
        socket = null;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
    }

    private static synchronized void connect(int gen) {
        String url = ClientConfig.get().relayUrl;
        if (gen != generation || room == null || url == null || url.isBlank() || socket != null || connecting) {
            return;
        }
        connecting = true;
        activeUrl = url.trim();
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            connecting = false;
            return;
        }
        HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, new Listener(gen))
                .whenComplete((ws, err) -> {
                    connecting = false;
                    if (err != null || gen != generation) {
                        if (ws != null) {
                            ws.abort();
                        }
                        retry(gen);
                        return;
                    }
                    socket = ws;
                    JsonObject hello = new JsonObject();
                    hello.addProperty("type", "hello");
                    hello.addProperty("room", room);
                    send(hello);
                    MinecraftClient.getInstance().execute(ClientSync::relayReconnected);
                });
    }

    private static void retry(int gen) {
        SCHED.schedule(() -> connect(gen), 8, TimeUnit.SECONDS);
    }

    static void sendLoadout(UUID player, String json) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "loadout");
        o.addProperty("uuid", player.toString());
        o.addProperty("json", json);
        send(o);
    }

    static void sendChunk(String hash, int index, int total, byte[] data) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "chunk");
        o.addProperty("hash", hash);
        o.addProperty("index", index);
        o.addProperty("total", total);
        o.addProperty("data", Base64.getEncoder().encodeToString(data));
        send(o);
    }

    private static synchronized void send(JsonObject o) {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        String text = o.toString();
        sendChain = sendChain.thenCompose(v -> ws.sendText(text, true)).exceptionally(t -> null);
    }

    private static final class Listener implements WebSocket.Listener {
        private final int gen;
        private final StringBuilder buffer = new StringBuilder();

        Listener(int gen) {
            this.gen = gen;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                MinecraftClient.getInstance().execute(() -> handle(text));
            }
            ws.request(1);
            return null;
        }

        private void handle(String text) {
            if (gen != generation) {
                return;
            }
            try {
                JsonObject o = JsonParser.parseString(text).getAsJsonObject();
                String type = o.get("type").getAsString();
                MinecraftClient client = MinecraftClient.getInstance();
                UUID me = client.player == null ? null : client.player.getUuid();
                switch (type) {
                    case "snapshot" -> {
                        Wardrobe.clearOthers();
                        JsonObject all = o.getAsJsonObject("loadouts");
                        for (String key : all.keySet()) {
                            UUID id = UUID.fromString(key);
                            if (!id.equals(me)) {
                                Wardrobe.putOther(id, all.get(key).getAsString());
                            }
                        }
                    }
                    case "loadout" -> {
                        UUID id = UUID.fromString(o.get("uuid").getAsString());
                        if (!id.equals(me)) {
                            Wardrobe.putOther(id, o.get("json").getAsString());
                        }
                    }
                    case "leave" -> ClientSync.forgetPlayer(UUID.fromString(o.get("uuid").getAsString()));
                    case "chunk" -> ClientSync.acceptChunk(o.get("hash").getAsString(), o.get("index").getAsInt(),
                            o.get("total").getAsInt(), Base64.getDecoder().decode(o.get("data").getAsString()));
                    default -> {
                    }
                }
            } catch (Exception e) {
                LunasCosmetics.LOGGER.debug("bad relay message: {}", e.toString());
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            if (socket == ws) {
                socket = null;
                retry(gen);
            }
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            if (socket == ws) {
                socket = null;
                retry(gen);
            }
        }
    }
}

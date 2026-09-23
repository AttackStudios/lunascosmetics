package net.attackstudioyt.lunascosmetics.net;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration. Everything is optional, so a server or client without the mod
 * still connects fine - it just never sees these channels.
 */
public final class Networking {
    private Networking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToServer(LoadoutPayload.TYPE, LoadoutPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ServerSync.onLoadout(p, ctx)));
        r.playToClient(LoadoutsPayload.TYPE, LoadoutsPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientHooks.loadouts(p, ctx)));
        r.playBidirectional(ModelChunkPayload.TYPE, ModelChunkPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound()) {
                ServerSync.onChunk(p, ctx);
            } else {
                ClientHooks.chunk(p, ctx);
            }
        }));
    }

    /** Indirection so the dedicated server never loads client classes. */
    static final class ClientHooks {
        static void loadouts(LoadoutsPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
            if (FMLEnvironment.dist.isClient()) {
                net.attackstudioyt.lunascosmetics.client.sync.ClientSync.onLoadouts(p);
            }
        }

        static void chunk(ModelChunkPayload p, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
            if (FMLEnvironment.dist.isClient()) {
                net.attackstudioyt.lunascosmetics.client.sync.ClientSync.acceptChunk(p.hash(), p.index(), p.total(), p.data());
            }
        }
    }
}

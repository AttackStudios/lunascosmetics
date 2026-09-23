package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server -> client: other players' loadouts. {@code full} replaces everything the client
 * knows (sent on join); otherwise it's a delta. An empty string means "took it all off".
 */
public record LoadoutsPayload(boolean full, Map<UUID, String> loadouts) implements CustomPayload {
    public static final CustomPayload.Id<LoadoutsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(LunasCosmetics.MOD_ID, "loadouts"));
    public static final PacketCodec<RegistryByteBuf, LoadoutsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, LoadoutsPayload::full,
            PacketCodecs.map(HashMap::new, Uuids.PACKET_CODEC, PacketCodecs.string(4096)), LoadoutsPayload::loadouts,
            LoadoutsPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

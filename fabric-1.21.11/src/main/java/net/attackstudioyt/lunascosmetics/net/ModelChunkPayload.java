package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * One slice of a custom .bbmodel, both directions. Files are addressed by the SHA-1 of
 * their bytes, so everyone agrees on identity and a file is only ever sent once.
 * Client->server packets are capped near 32 KB, hence the chunking.
 */
public record ModelChunkPayload(String hash, int index, int total, byte[] data) implements CustomPayload {
    public static final int CHUNK = 28_000;
    public static final int MAX_BYTES = 2_000_000;

    public static final CustomPayload.Id<ModelChunkPayload> ID =
            new CustomPayload.Id<>(Identifier.of(LunasCosmetics.MOD_ID, "model_chunk"));
    public static final PacketCodec<RegistryByteBuf, ModelChunkPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(64), ModelChunkPayload::hash,
            PacketCodecs.VAR_INT, ModelChunkPayload::index,
            PacketCodecs.VAR_INT, ModelChunkPayload::total,
            PacketCodecs.byteArray(CHUNK + 64), ModelChunkPayload::data,
            ModelChunkPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

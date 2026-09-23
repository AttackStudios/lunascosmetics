package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * One slice of a custom .bbmodel, both directions. Files are addressed by the SHA-1 of
 * their bytes, so everyone agrees on identity and a file is only ever sent once.
 * Client->server packets are capped near 32 KB, hence the chunking.
 */
public record ModelChunkPayload(String hash, int index, int total, byte[] data) implements CustomPacketPayload {
    public static final int CHUNK = 28_000;
    public static final int MAX_BYTES = 2_000_000;

    public static final Type<ModelChunkPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "model_chunk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ModelChunkPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), ModelChunkPayload::hash,
            ByteBufCodecs.VAR_INT, ModelChunkPayload::index,
            ByteBufCodecs.VAR_INT, ModelChunkPayload::total,
            ByteBufCodecs.byteArray(CHUNK + 64), ModelChunkPayload::data,
            ModelChunkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server -> client: other players' loadouts. {@code full} replaces everything the client
 * knows (sent on join); otherwise it's a delta. An empty string means "took it all off".
 */
public record LoadoutsPayload(boolean full, Map<UUID, String> loadouts) implements CustomPacketPayload {
    public static final Type<LoadoutsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "loadouts"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LoadoutsPayload::full,
            ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.stringUtf8(4096)), LoadoutsPayload::loadouts,
            LoadoutsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

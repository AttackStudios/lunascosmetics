package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: "this is what I'm wearing now", as the loadout's compact JSON. */
public record LoadoutPayload(String json) implements CustomPacketPayload {
    public static final Type<LoadoutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LunasCosmetics.MOD_ID, "loadout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(4096), LoadoutPayload::json, LoadoutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

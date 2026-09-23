package net.attackstudioyt.lunascosmetics.net;

import net.attackstudioyt.lunascosmetics.LunasCosmetics;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: "this is what I'm wearing now", as the loadout's compact JSON. */
public record LoadoutPayload(String json) implements CustomPayload {
    public static final CustomPayload.Id<LoadoutPayload> ID =
            new CustomPayload.Id<>(Identifier.of(LunasCosmetics.MOD_ID, "loadout"));
    public static final PacketCodec<RegistryByteBuf, LoadoutPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.string(4096), LoadoutPayload::json, LoadoutPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

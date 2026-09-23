package net.attackstudioyt.lunascosmetics;

import net.attackstudioyt.lunascosmetics.net.LoadoutPayload;
import net.attackstudioyt.lunascosmetics.net.LoadoutsPayload;
import net.attackstudioyt.lunascosmetics.net.ModelChunkPayload;
import net.attackstudioyt.lunascosmetics.net.ServerSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. The mod is mostly client-side; this half only exists so a server
 * that also has it installed can relay everyone's loadouts (and custom models) between
 * the players who run it. Nothing here touches gameplay.
 */
public class LunasCosmetics implements ModInitializer {
    public static final String MOD_ID = "lunascosmetics";
    public static final Logger LOGGER = LoggerFactory.getLogger("Luna's Cosmetics");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(LoadoutPayload.ID, LoadoutPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoadoutsPayload.ID, LoadoutsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModelChunkPayload.ID, ModelChunkPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModelChunkPayload.ID, ModelChunkPayload.CODEC);
        ServerSync.init();
    }
}

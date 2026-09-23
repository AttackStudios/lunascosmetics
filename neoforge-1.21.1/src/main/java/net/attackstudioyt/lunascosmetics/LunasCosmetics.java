package net.attackstudioyt.lunascosmetics;

import net.attackstudioyt.lunascosmetics.net.Networking;
import net.attackstudioyt.lunascosmetics.net.ServerSync;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. The mod is mostly client-side; this half only exists so a server
 * that also has it installed can relay everyone's loadouts (and custom models) between
 * the players who run it. Nothing here touches gameplay.
 */
@Mod(LunasCosmetics.MOD_ID)
public class LunasCosmetics {
    public static final String MOD_ID = "lunascosmetics";
    public static final Logger LOGGER = LoggerFactory.getLogger("Luna's Cosmetics");

    public LunasCosmetics(IEventBus modBus) {
        modBus.addListener(Networking::register);
        ServerSync.init();
    }
}

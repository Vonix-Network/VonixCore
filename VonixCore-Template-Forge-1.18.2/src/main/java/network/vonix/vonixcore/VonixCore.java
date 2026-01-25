package network.vonix.vonixcore;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import network.vonix.vonixcore.config.EssentialsConfig;
import org.slf4j.Logger;

/**
 * VonixCore - Universal Minecraft Server Plugin
 * Forge 1.18.2 Edition
 */
@Mod(VonixCore.MODID)
public class VonixCore {

    public static final String MODID = "vonixcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static VonixCore instance;

    public VonixCore() {
        instance = this;

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EssentialsConfig.SPEC, "vonixcore-essentials.toml");

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[VonixCore] VonixCore Forge 1.18.2 initialized");
    }

    public static VonixCore getInstance() {
        return instance;
    }
}


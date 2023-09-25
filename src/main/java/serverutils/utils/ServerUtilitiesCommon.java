package serverutils.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.lib.lib.util.permission.PermissionAPI;
import serverutils.utils.data.Leaderboard;
import serverutils.utils.data.NodeEntry;
import serverutils.utils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.utils.data.ServerUtilitiesUniverseData;
import serverutils.utils.events.CustomPermissionPrefixesRegistryEvent;
import serverutils.utils.events.LeaderboardRegistryEvent;
import serverutils.utils.handlers.ServerUtilitiesPlayerEventHandler;
import serverutils.utils.handlers.ServerUtilitiesRegistryEventHandler;
import serverutils.utils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.utils.handlers.ServerUtilitiesWorldEventHandler;
import serverutils.utils.net.ServerUtilitiesNetHandler;
import serverutils.utils.ranks.ServerUtilitiesPermissionHandler;

public class ServerUtilitiesCommon {

    public static final Collection<NodeEntry> CUSTOM_PERM_PREFIX_REGISTRY = new HashSet<>();
    public static final Map<ResourceLocation, Leaderboard> LEADERBOARDS = new HashMap<>();
    public static final Map<String, String> KAOMOJIS = new HashMap<>();

    public void preInit(FMLPreInitializationEvent event) {
        ServerUtilitiesConfig.init(event);

        if (ServerUtilitiesConfig.ranks.enabled) {
            PermissionAPI.setPermissionHandler(ServerUtilitiesPermissionHandler.INSTANCE);
        }

        ServerUtilitiesNetHandler.init();

        if (!ForgeChunkManager.getConfig().hasCategory(ServerUtilities.MOD_ID)) {
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumTicketCount", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager
                .setForcedChunkLoadingCallback(ServerUtilities.INST, ServerUtilitiesLoadedChunkManager.INSTANCE);
        new CustomPermissionPrefixesRegistryEvent(CUSTOM_PERM_PREFIX_REGISTRY::add).post();

        // if (Loader.isModLoaded(ChiselsAndBits.MODID)) {
        // ChiselsAndBitsIntegration.init();
        // }
        //
        // if (Loader.isModLoaded(iChunUtil.MOD_ID)) {
        // IChunUtilIntegration.init();
        // }
        //
        // if (Loader.isModLoaded(KubeJS.MOD_ID)) {
        // KubeJSIntegration.init();
        // }

        KAOMOJIS.put("shrug", "\u00AF\\_(\u30C4)_/\u00AF");
        KAOMOJIS.put("tableflip", "(\u256F\u00B0\u25A1\u00B0)\u256F \uFE35 \u253B\u2501\u253B");
        KAOMOJIS.put("unflip", "\u252C\u2500\u252C\u30CE( \u309C-\u309C\u30CE)");

        // if (Loader.isModLoaded(Aurora.MOD_ID)) {
        // AuroraIntegration.init();
        // }

        MinecraftForge.EVENT_BUS.register(ServerUtilitiesConfig.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPlayerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesRegistryEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesServerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesWorldEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesUniverseData.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPermissions.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLeaderboards.INST);

        FMLCommonHandler.instance().bus().register(ServerUtilitiesServerEventHandler.INST);
    }

    public void init() {
        new LeaderboardRegistryEvent(leaderboard -> LEADERBOARDS.put(leaderboard.id, leaderboard)).post();
        ServerUtilitiesPermissions.registerPermissions();
    }

    public void postInit() {}

    public void imc(FMLInterModComms.IMCMessage message) {}
}

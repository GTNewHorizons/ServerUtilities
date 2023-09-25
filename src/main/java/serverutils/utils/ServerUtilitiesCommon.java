package serverutils.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
import serverutils.utils.data.FTBUtilitiesLoadedChunkManager;
import serverutils.utils.data.FTBUtilitiesUniverseData;
import serverutils.utils.data.Leaderboard;
import serverutils.utils.data.NodeEntry;
import serverutils.utils.events.CustomPermissionPrefixesRegistryEvent;
import serverutils.utils.events.LeaderboardRegistryEvent;
import serverutils.utils.handlers.FTBUtilitiesPlayerEventHandler;
import serverutils.utils.handlers.FTBUtilitiesRegistryEventHandler;
import serverutils.utils.handlers.FTBUtilitiesServerEventHandler;
import serverutils.utils.handlers.FTBUtilitiesWorldEventHandler;
import serverutils.utils.net.FTBUtilitiesNetHandler;
import serverutils.utils.ranks.FTBUtilitiesPermissionHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ServerUtilitiesCommon {

    public static final Collection<NodeEntry> CUSTOM_PERM_PREFIX_REGISTRY = new HashSet<>();
    public static final Map<ResourceLocation, Leaderboard> LEADERBOARDS = new HashMap<>();
    public static final Map<String, String> KAOMOJIS = new HashMap<>();

    public void preInit(FMLPreInitializationEvent event) {
        ServerUtilitiesConfig.init(event);

        if (ServerUtilitiesConfig.ranks.enabled) {
            PermissionAPI.setPermissionHandler(FTBUtilitiesPermissionHandler.INSTANCE);
        }

        FTBUtilitiesNetHandler.init();

        if (!ForgeChunkManager.getConfig().hasCategory(ServerUtilities.MOD_ID)) {
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumTicketCount", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager.setForcedChunkLoadingCallback(ServerUtilities.INST, FTBUtilitiesLoadedChunkManager.INSTANCE);
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
        MinecraftForge.EVENT_BUS.register(FTBUtilitiesPlayerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(FTBUtilitiesRegistryEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(FTBUtilitiesServerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(FTBUtilitiesWorldEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(FTBUtilitiesUniverseData.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPermissions.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLeaderboards.INST);

        FMLCommonHandler.instance().bus().register(FTBUtilitiesServerEventHandler.INST);
    }

    public void init() {
        new LeaderboardRegistryEvent(leaderboard -> LEADERBOARDS.put(leaderboard.id, leaderboard)).post();
        ServerUtilitiesPermissions.registerPermissions();
    }

    public void postInit() {}

    public void imc(FMLInterModComms.IMCMessage message) {}
}

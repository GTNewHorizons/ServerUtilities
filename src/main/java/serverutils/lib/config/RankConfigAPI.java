package serverutils.lib.config;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;

import serverutils.ServerUtilities;
import serverutils.events.RegisterRankConfigEvent;
import serverutils.events.RegisterRankConfigHandlerEvent;

public class RankConfigAPI {

    private static IRankConfigHandler handler = null;

    private static void setHandler(IRankConfigHandler h) {
        Preconditions.checkNotNull(h, "Permission handler can't be null!");
        ServerUtilities.LOGGER.warn("Replacing " + handler.getClass().getName() + " with " + h.getClass().getName());
        handler = h;
    }

    public static IRankConfigHandler getHandler() {
        if (handler == null) {
            handler = DefaultRankConfigHandler.INSTANCE;
            new RegisterRankConfigHandlerEvent(RankConfigAPI::setHandler).post();
            new RegisterRankConfigEvent(handler::registerRankConfig).post();
        }

        return handler;
    }

    public static ConfigValue get(MinecraftServer server, GameProfile profile, String node) {
        Preconditions.checkNotNull(profile, "GameProfile can't be null!");
        Preconditions.checkNotNull(node, "Config node can't be null!");
        return getHandler().getConfigValue(server, profile, node);
    }

    public static ConfigValue get(EntityPlayerMP player, String node) {
        Preconditions.checkNotNull(player, "Player can't be null!");
        Preconditions.checkNotNull(node, "Config node can't be null!");
        return get(player.mcServer, player.getGameProfile(), node);
    }

    public static ConfigValue getConfigValue(String node, boolean op) {
        RankConfigValueInfo info = getHandler().getInfo(node);
        return info == null ? ConfigNull.INSTANCE : op ? info.defaultOPValue : info.defaultValue;
    }
}

package serverutils.ranks;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import serverutils.lib.config.ConfigNull;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.DefaultRankConfigHandler;
import serverutils.lib.config.IRankConfigHandler;
import serverutils.lib.config.RankConfigValueInfo;
import serverutils.lib.util.permission.DefaultPermissionHandler;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.IPermissionHandler;
import serverutils.lib.util.permission.context.IContext;

public enum ServerUtilitiesPermissionHandler implements IPermissionHandler, IRankConfigHandler {

    INSTANCE;

    @Override
    public void registerNode(String node, DefaultPermissionLevel level, String desc) {
        DefaultPermissionHandler.INSTANCE.registerNode(node, level, desc);
    }

    @Override
    public Collection<String> getRegisteredNodes() {
        return DefaultPermissionHandler.INSTANCE.getRegisteredNodes();
    }

    @Override
    public boolean hasPermission(GameProfile profile, String node, @Nullable IContext context) {
        if (context != null && context.getWorld() != null) {
            if (context.getWorld().isRemote) {
                return DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL;
            }
        } else if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            return DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(node) == DefaultPermissionLevel.ALL;
        }

        if (profile.getId() == null) {
            if (profile.getName() == null) {
                return false;
            }

            profile = new GameProfile(EntityPlayer.func_146094_a(profile), profile.getName());
        }

        return switch (Ranks.INSTANCE.getPermissionResult(profile, node, true)) {
            case ALLOW -> true;
            case DENY -> false;
            default -> DefaultPermissionHandler.INSTANCE.hasPermission(profile, node, context);
        };
    }

    @Override
    public String getNodeDescription(String node) {
        return DefaultPermissionHandler.INSTANCE.getNodeDescription(node);
    }

    @Override
    public void registerRankConfig(RankConfigValueInfo info) {
        DefaultRankConfigHandler.INSTANCE.registerRankConfig(info);
    }

    @Override
    public Collection<RankConfigValueInfo> getRegisteredConfigs() {
        return DefaultRankConfigHandler.INSTANCE.getRegisteredConfigs();
    }

    @Override
    public ConfigValue getConfigValue(MinecraftServer server, GameProfile profile, String node) {
        ConfigValue value = ConfigNull.INSTANCE;

        if (Ranks.isActive()) {
            value = Ranks.INSTANCE.getPermission(profile, node, true);
        }

        return value.isNull() ? DefaultRankConfigHandler.INSTANCE.getConfigValue(server, profile, node) : value;
    }

    @Nullable
    @Override
    public RankConfigValueInfo getInfo(String node) {
        return DefaultRankConfigHandler.INSTANCE.getInfo(node);
    }
}

package serverutils.lib.config;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;

public interface IRankConfigHandler {

    void registerRankConfig(RankConfigValueInfo info);

    default void registerRankConfig(String id, ConfigValue defaultPlayerValue, ConfigValue defaultOPValue) {
        registerRankConfig(new RankConfigValueInfo(id, defaultPlayerValue, defaultOPValue));
    }

    default void registerRankConfig(String id, ConfigValue defaultPlayerValue) {
        registerRankConfig(new RankConfigValueInfo(id, defaultPlayerValue, null));
    }

    Collection<RankConfigValueInfo> getRegisteredConfigs();

    ConfigValue getConfigValue(MinecraftServer server, GameProfile profile, String node);

    @Nullable
    RankConfigValueInfo getInfo(String node);
}

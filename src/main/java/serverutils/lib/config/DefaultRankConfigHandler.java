package serverutils.lib.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;

import serverutils.lib.util.ServerUtils;

public enum DefaultRankConfigHandler implements IRankConfigHandler {

    INSTANCE;

    private static final Map<String, RankConfigValueInfo> MAP = new HashMap<>();
    private static Collection<RankConfigValueInfo> VALUES = Collections.unmodifiableCollection(MAP.values());

    @Override
    public void registerRankConfig(RankConfigValueInfo info) {
        Preconditions.checkNotNull(info, "RankConfigValueInfo can't be null!");
        Preconditions.checkArgument(!MAP.containsKey(info.node), "Duplicate rank config ID found: " + info.node);
        MAP.put(info.node, info);
    }

    @Override
    public Collection<RankConfigValueInfo> getRegisteredConfigs() {
        return VALUES;
    }

    @Override
    public ConfigValue getConfigValue(MinecraftServer server, GameProfile profile, String node) {
        RankConfigValueInfo info = RankConfigAPI.getHandler().getInfo(node);

        if (info != null) {
            return ServerUtils.isOP(server, profile) ? info.defaultOPValue : info.defaultValue;
        }

        return ConfigNull.INSTANCE;
    }

    @Override
    @Nullable
    public RankConfigValueInfo getInfo(String node) {
        Preconditions.checkNotNull(node, "Config node can't be null!");
        return MAP.get(node);
    }
}

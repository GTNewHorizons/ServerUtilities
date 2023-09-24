package serverutils.serverlib.lib.config;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author LatvianModder
 */
public interface IRankConfigHandler
{
	void registerRankConfig(RankConfigValueInfo info);

	Collection<RankConfigValueInfo> getRegisteredConfigs();

	ConfigValue getConfigValue(MinecraftServer server, GameProfile profile, String node);

	@Nullable
	RankConfigValueInfo getInfo(String node);
}
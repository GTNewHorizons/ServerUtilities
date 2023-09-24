package serverutils.serverlib.events;

import serverutils.serverlib.lib.config.ConfigValue;
import serverutils.serverlib.lib.config.RankConfigValueInfo;

import java.util.function.Consumer;

public class RegisterRankConfigEvent extends ServerLibEvent {
	private final Consumer<RankConfigValueInfo> callback;

	public RegisterRankConfigEvent(Consumer<RankConfigValueInfo> c)
	{
		callback = c;
	}

	public void register(RankConfigValueInfo info)
	{
		callback.accept(info);
	}

	public RankConfigValueInfo register(String id, ConfigValue defaultPlayerValue, ConfigValue defaultOPValue)
	{
		RankConfigValueInfo info = new RankConfigValueInfo(id, defaultPlayerValue, defaultOPValue);
		register(info);
		return info;
	}

	public RankConfigValueInfo register(String id, ConfigValue defaultValue)
	{
		RankConfigValueInfo info = new RankConfigValueInfo(id, defaultValue, null);
		register(info);
		return info;
	}
}
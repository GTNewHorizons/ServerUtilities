package serverutils.lib.events;

import java.util.function.Consumer;

import serverutils.lib.lib.config.ConfigValue;
import serverutils.lib.lib.config.RankConfigValueInfo;

public class RegisterRankConfigEvent extends ServerUtilitiesLibEvent {

	private final Consumer<RankConfigValueInfo> callback;

	public RegisterRankConfigEvent(Consumer<RankConfigValueInfo> c) {
		callback = c;
	}

	public void register(RankConfigValueInfo info) {
		callback.accept(info);
	}

	public RankConfigValueInfo register(String id, ConfigValue defaultPlayerValue, ConfigValue defaultOPValue) {
		RankConfigValueInfo info = new RankConfigValueInfo(id, defaultPlayerValue, defaultOPValue);
		register(info);
		return info;
	}

	public RankConfigValueInfo register(String id, ConfigValue defaultValue) {
		RankConfigValueInfo info = new RankConfigValueInfo(id, defaultValue, null);
		register(info);
		return info;
	}
}

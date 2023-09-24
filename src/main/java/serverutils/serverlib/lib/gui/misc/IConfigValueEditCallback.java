package serverutils.serverlib.lib.gui.misc;

import serverutils.serverlib.lib.config.ConfigValue;

@FunctionalInterface
public interface IConfigValueEditCallback
{
	void onCallback(ConfigValue value, boolean set);
}
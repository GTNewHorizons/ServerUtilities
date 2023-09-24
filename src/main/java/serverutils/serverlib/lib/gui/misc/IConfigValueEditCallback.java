package serverutils.serverlib.lib.gui.misc;

import serverutils.serverlib.lib.config.ConfigValue;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface IConfigValueEditCallback
{
	void onCallback(ConfigValue value, boolean set);
}
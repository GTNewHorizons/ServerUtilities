package serverutils.lib.lib.gui.misc;

import serverutils.lib.lib.config.ConfigValue;

@FunctionalInterface
public interface IConfigValueEditCallback {

    void onCallback(ConfigValue value, boolean set);
}

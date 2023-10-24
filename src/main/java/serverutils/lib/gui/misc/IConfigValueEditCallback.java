package serverutils.lib.gui.misc;

import serverutils.lib.config.ConfigValue;

@FunctionalInterface
public interface IConfigValueEditCallback {

    void onCallback(ConfigValue value, boolean set);
}

package serverutils.lib.mod.config;

import latmod.lib.annotations.Info;
import serverutils.lib.api.config.ConfigEntryBool;

public class ServerUtilitiesLibConfigCompat {

    @Info("When true: inventory buttons do not shift when potion effect(s) are active")
    public static final ConfigEntryBool compat_statusEffectHUD = new ConfigEntryBool("compat_statusEffectHUD", false);

}

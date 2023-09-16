package ftb.lib.mod.config;

import ftb.lib.api.config.ConfigEntryBool;
import latmod.lib.annotations.Info;

public class FTBLibConfigCompat {

    @Info("When true: inventory buttons do not shift when potion effect(s) are active")
    public static final ConfigEntryBool compat_statusEffectHUD = new ConfigEntryBool("compat_statusEffectHUD", false);

}

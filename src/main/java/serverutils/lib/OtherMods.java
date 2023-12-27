package serverutils.lib;

import cpw.mods.fml.common.Loader;

public class OtherMods {

    private static boolean isNEILoaded;
    private static boolean isVPLoaded;
    private static boolean isEnderIOLoaded;

    public static void init() {
        isNEILoaded = Loader.isModLoaded("NotEnoughItems");
        isVPLoaded = Loader.isModLoaded("visualprospecting");
        isEnderIOLoaded = Loader.isModLoaded("EnderIO");
    }

    public static boolean isNEILoaded() {
        return isNEILoaded;
    }

    public static boolean isVPLoaded() {
        return isVPLoaded;
    }

    public static boolean isEnderIOLoaded() {
        return isEnderIOLoaded;
    }
}

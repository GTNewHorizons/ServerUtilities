package serverutils.lib;

import cpw.mods.fml.common.Loader;

public class OtherMods {

    private static boolean isNEILoaded;
    private static boolean isNavigatorLoaded;
    private static boolean isEnderIOLoaded;
    private static boolean isWitcheryLoaded;
    private static boolean isHodgepodgeLoaded;
    private static boolean isDashboardLoaded;

    public static void init() {
        isNEILoaded = Loader.isModLoaded("NotEnoughItems");
        isNavigatorLoaded = Loader.isModLoaded("navigator");
        isEnderIOLoaded = Loader.isModLoaded("EnderIO");
        isWitcheryLoaded = Loader.isModLoaded("witchery");
        isHodgepodgeLoaded = Loader.isModLoaded("hodgepodge");
        isDashboardLoaded = Loader.isModLoaded("dashboard");
    }

    public static boolean isNEILoaded() {
        return isNEILoaded;
    }

    public static boolean isNavigatorLoaded() {
        return isNavigatorLoaded;
    }

    public static boolean isEnderIOLoaded() {
        return isEnderIOLoaded;
    }

    public static boolean isWitcheryLoaded() {
        return isWitcheryLoaded;
    }

    public static boolean isHodgepodgeLoaded() {
        return isHodgepodgeLoaded;
    }

    public static boolean isDashboardLoaded() {
        return isDashboardLoaded;
    }
}

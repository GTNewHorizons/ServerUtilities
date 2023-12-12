package serverutils.lib.util;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class CommonUtils {

    private static ListMultimap<String, ModContainer> packageOwners = null;

    @SuppressWarnings({ "unchecked", "ConstantConditions" })
    public static <T> T cast(@Nullable Object o) {
        return o == null ? null : (T) o;
    }

    @Nullable
    public static ModContainer getModContainerForClass(Class<?> clazz) {
        if (packageOwners == null) {
            try {
                LoadController instance = ReflectionHelper
                        .getPrivateValue(Loader.class, Loader.instance(), "modController");
                packageOwners = ReflectionHelper.getPrivateValue(LoadController.class, instance, "packageOwners");
            } catch (Exception ex) {
                packageOwners = ImmutableListMultimap.of();
            }
        }

        if (packageOwners.isEmpty()) {
            return null;
        }

        String pkg = clazz.getName().substring(0, clazz.getName().lastIndexOf('.'));
        return packageOwners.containsKey(pkg) ? packageOwners.get(pkg).get(0) : null;
    }

    public static boolean getClassExists(String className) {
        try {
            Class.forName(className, false, CommonUtils.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

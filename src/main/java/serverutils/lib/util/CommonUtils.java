package serverutils.lib.util;

import javax.annotation.Nullable;

public class CommonUtils {

    @SuppressWarnings("unchecked")
    public static <T> T cast(@Nullable Object o) {
        return o == null ? null : (T) o;
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

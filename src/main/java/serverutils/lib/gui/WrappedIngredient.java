package serverutils.lib.gui;

import javax.annotation.Nullable;

public final class WrappedIngredient {

    @Nullable
    public static Object unwrap(@Nullable Object object) {
        if (object instanceof WrappedIngredient) {
            return unwrap(((WrappedIngredient) object).wrappedIngredient);
        }

        return object;
    }

    public final Object wrappedIngredient;
    public boolean tooltip = false;

    public WrappedIngredient(@Nullable Object o) {
        wrappedIngredient = o;
    }

    public WrappedIngredient tooltip() {
        tooltip = true;
        return this;
    }
}

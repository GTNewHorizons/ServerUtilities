package latmod.lib.util;

import latmod.lib.MathHelperLM;

public final class IntBounds {

    public final int defValue, minValue, maxValue;

    public IntBounds(int def, int min, int max) {
        minValue = min;
        maxValue = max;
        defValue = getVal(def);
    }

    public IntBounds(int def) {
        this(def, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public int getVal(int v) {
        return MathHelperLM.clampInt(v, minValue, maxValue);
    }

    public IntBounds copy(int def) {
        return new IntBounds(def, minValue, maxValue);
    }
}

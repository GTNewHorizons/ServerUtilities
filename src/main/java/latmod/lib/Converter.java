package latmod.lib;

import java.util.Arrays;

/**
 * Made by LatvianModder
 */
public class Converter {

    public static int[] toInts(byte[] b) {
        if (b == null) return null;
        int ai[] = new int[b.length];
        for (int i = 0; i < ai.length; i++) ai[i] = b[i] & 0xFF;
        return ai;
    }

    public static int[] toInts(short[] b) {
        if (b == null) return null;
        int ai[] = new int[b.length];
        for (int i = 0; i < ai.length; i++) ai[i] = b[i];
        return ai;
    }

    public static byte[] toBytes(int[] b) {
        if (b == null) return null;
        byte ai[] = new byte[b.length];
        for (int i = 0; i < ai.length; i++) ai[i] = (byte) b[i];
        return ai;
    }

    public static Integer[] fromInts(int[] i) {
        if (i == null) return null;
        Integer ai[] = new Integer[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static int[] toInts(Integer[] i) {
        if (i == null) return null;
        int ai[] = new int[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static Float[] fromFloats(float[] i) {
        if (i == null) return null;
        Float ai[] = new Float[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static float[] toFloats(Float[] i) {
        if (i == null) return null;
        float ai[] = new float[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static Double[] fromDoubles(double[] i) {
        if (i == null) return null;
        Double ai[] = new Double[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static double[] toDoubles(Double[] i) {
        if (i == null) return null;
        double ai[] = new double[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static Byte[] fromBytes(byte[] i) {
        if (i == null) return null;
        Byte ai[] = new Byte[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static byte[] toBytes(Byte[] i) {
        if (i == null) return null;
        byte ai[] = new byte[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static float[] toFloats(double[] i) {
        if (i == null) return null;
        float ai[] = new float[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = (float) i[j];
        return ai;
    }

    public static double[] toDoubles(float[] i) {
        if (i == null) return null;
        double ai[] = new double[i.length];
        for (int j = 0; j < ai.length; j++) ai[j] = i[j];
        return ai;
    }

    public static void toBools(boolean[] bools, IntList idx, boolean isTrue) {
        Arrays.fill(bools, !isTrue);
        for (int i = 0; i < idx.size(); i++) bools[idx.get(i)] = isTrue;
    }

    public static void fromBools(boolean[] bools, IntList il, boolean isTrue) {
        il.clear();
        for (int i = 0; i < bools.length; i++) if (bools[i] == isTrue) il.add(i);
    }

    public static boolean canParseInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {}
        return false;
    }

    public static boolean canParseDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {}
        return false;
    }

    public static int toInt(String text, int def) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {}
        return def;
    }

    public static int nonNull(Integer i) {
        return (i == null) ? 0 : i;
    }

    public static double nonNull(Double d) {
        return (d == null) ? 0D : d;
    }
}

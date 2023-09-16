package latmod.lib.util;

import java.util.Random;

import latmod.lib.MathHelperLM;

/**
 * Originally made by Progressing.org <br>
 * updated by LatvianModder
 */
public class Noise {

    public static final Noise def = new Noise();

    private static final float perlin_cosTable[] = new float[720];

    static {
        double d = 0.5D * MathHelperLM.RAD;
        for (int i = 0; i < perlin_cosTable.length; i++) perlin_cosTable[i] = (float) MathHelperLM.cos(i * d);

    }

    private final float perlin[] = new float[4096];
    private static final int perlin_TWOPI = 720;
    private static final int perlin_PI = perlin_TWOPI >> 1;

    public Noise(Random r) {
        if (r == null) r = new Random();
        for (int i = 0; i < perlin.length; i++) perlin[i] = r.nextFloat();
    }

    public Noise() {
        this(null);
    }

    private float noise_fsc(double f) {
        return 0.5F * (1F - perlin_cosTable[((int) (f * perlin_PI) % perlin_TWOPI)]);
    }

    public float get(float x, float y, float z) {
        int i = (int) Math.abs(x);
        int j = (int) Math.abs(y);
        int k = (int) Math.abs(z);

        float f1 = x - i;
        float f2 = y - j;
        float f3 = z - k;

        float f6 = 0F;
        float f7 = 0.5F;

        for (int m = 0; m < 4; m++) {
            int n = i + (j << 4) + (k << 8);
            float f4 = noise_fsc(f1);
            float f5 = noise_fsc(f2);

            float f8 = perlin[(n & 0xFFF)];
            f8 += f4 * (perlin[(n + 1 & 0xFFF)] - f8);

            float f9 = perlin[(n + 16 & 0xFFF)];
            f9 += f4 * (perlin[(n + 16 + 1 & 0xFFF)] - f9);
            f8 += f5 * (f9 - f8);
            n += 256;

            f9 = perlin[(n & 0xFFF)];
            f9 += f4 * (perlin[(n + 1 & 0xFFF)] - f9);

            float f10 = perlin[(n + 16 & 0xFFF)];
            f10 += f4 * (perlin[(n + 16 + 1 & 0xFFF)] - f10);

            f9 += f5 * (f10 - f9);
            f8 += noise_fsc(f3) * (f9 - f8);
            f6 += f8 * f7;

            f7 *= 0.5F;

            i <<= 1;
            f1 *= 2D;
            if (f1 >= 1D) {
                i++;
                f1 -= 1D;
            }
            j <<= 1;
            f2 *= 2D;
            if (f2 >= 1D) {
                j++;
                f2 -= 1D;
            }
            k <<= 1;
            f3 *= 2D;
            if (f3 >= 1D) {
                k++;
                f3 -= 1D;
            }
        }

        return f6;
    }

    public float getD(double x, double y, double z) {
        return get((float) x, (float) y, (float) z);
    }

    public float getD(double x, double y) {
        return get((float) x, (float) y, 0F);
    }

    public float getD(double x) {
        return get((float) x, 0F, 0F);
    }

    public float get(float x, float y) {
        return get(x, y, 0F);
    }

    public float get(float x) {
        return get(x, 0F, 0F);
    }
}

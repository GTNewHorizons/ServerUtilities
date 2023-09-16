package latmod.lib.util;

import java.util.Random;

import latmod.lib.MathHelperLM;

/**
 * Made by LatvianModder
 */
public final class VecLM {

    public double x;
    public double y;
    public double z;

    public VecLM() {}

    public VecLM(double nx, double ny, double nz) {
        x = nx;
        y = ny;
        z = nz;
    }

    public VecLM(Random r, boolean sin) {
        this(r.nextFloat(), r.nextFloat(), r.nextFloat());
        if (sin) {
            scale(2D);
            add(-1D, -1D, -1D);
        }
    }

    public void set(double nx, double ny, double nz) {
        x = nx;
        y = ny;
        z = nz;
    }

    public void set(VecLM v) {
        set(v.x, v.y, v.z);
    }

    public void add(double ax, double ay, double az) {
        set(x + ax, y + ay, z + az);
    }

    public void add(VecLM v, double s) {
        add(v.x * s, v.y * s, v.z * s);
    }

    public void scale(double sx, double sy, double sz) {
        x *= sx;
        y *= sy;
        z *= sz;
    }

    public void scale(VecLM v, double s) {
        scale(v.x * s, v.y * s, v.z * s);
    }

    public void scale(double s) {
        scale(s, s, s);
    }

    public boolean isNull() {
        return x == 0D && y == 0D && z == 0D;
    }

    public boolean containsNaN() {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(y);
    }

    public boolean equals(Object o) {
        return (o instanceof VecLM && equalsPos((VecLM) o));
    }

    public boolean equalsPos(VecLM v) {
        return v.x == x && v.y == y && v.z == z;
    }

    public VecLM copy() {
        return new VecLM(x, y, z);
    }

    public double distSq(double x1, double y1, double z1) {
        return MathHelperLM.sq(x1 - x) + MathHelperLM.sq(y1 - y) + MathHelperLM.sq(z1 - z);
    }

    public double dist(double x1, double y1, double z1) {
        return MathHelperLM.sqrt(distSq(x1, y1, z1));
    }

    public double distSq(VecLM v) {
        return distSq(v.x, v.y, v.z);
    }

    public double dist(VecLM v) {
        return MathHelperLM.sqrt(distSq(v));
    }

    public double atanXZ(VecLM v) {
        if (v == null) return -1D;
        return Math.atan2(v.z - z, v.x - x);
    }

    public double atanY(VecLM v) {
        if (v == null) return -1D;
        return Math.atan(y - v.y);
    }

    public int getX() {
        return MathHelperLM.floor(x);
    }

    public int getY() {
        return MathHelperLM.floor(y);
    }

    public int getZ() {
        return MathHelperLM.floor(z);
    }

    public double length() {
        if (isNull()) return 0D;
        return MathHelperLM.dist(0D, 0D, 0D, x, y, z);
    }

    public VecLM normalize() {
        double d = length();
        if (d == 0D) return new VecLM(0D, 0D, 0D);
        return new VecLM(x / d, y / d, z / d);
    }
}

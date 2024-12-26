package serverutils.lib.util.misc;

public class PregeneratorCommandInfo {

    private final double xLoc;
    private final double zLoc;
    private final int radius;
    private final int dimensionID;
    private final int iteration;

    public PregeneratorCommandInfo(double xLoc, double zLoc, int radius, int dimensionID, int iteration) {
        this.xLoc = xLoc;
        this.zLoc = zLoc;
        this.radius = radius;
        this.iteration = iteration;
        this.dimensionID = dimensionID;
    }

    public PregeneratorCommandInfo(double xLoc, double zLoc, int radius, int dimensionID) {
        this(xLoc, zLoc, radius, dimensionID, -1);
    }

    public double getXLoc() {
        return xLoc;
    }

    public double getZLoc() {
        return zLoc;
    }

    public int getRadius() {
        return radius;
    }

    public int getIteration() {
        return this.iteration;
    }

    public int getDimensionID() {
        return this.dimensionID;
    }
}

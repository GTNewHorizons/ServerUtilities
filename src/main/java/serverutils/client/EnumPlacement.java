package serverutils.client;

public enum EnumPlacement {

    GROUPED(2),
    VERTICAL(10),
    HORIZONTAL(10);

    public final int maxInRow;

    EnumPlacement(int max) {
        this.maxInRow = max;
    }

    public int getMaxInRow() {
        return maxInRow;
    }
}

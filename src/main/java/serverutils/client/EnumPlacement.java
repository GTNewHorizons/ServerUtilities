package serverutils.client;

public enum EnumPlacement {

    GROUPED("grouped", 2),
    VERTICAL("vertical", 10),
    HORIZONTAL("horizontal", 10);

    public final String type;
    public final int maxInRow;

    EnumPlacement(String type, int max) {
        this.type = type;
        this.maxInRow = max;
    }

    public String getType() {
        return type;
    }

    public int getMaxInRow() {
        return maxInRow;
    }

    public static EnumPlacement stringToEnum(String placement) {
        return switch (placement.toLowerCase()) {
            case "vertical" -> VERTICAL;
            case "horizontal" -> HORIZONTAL;
            default -> GROUPED;
        };
    }
}

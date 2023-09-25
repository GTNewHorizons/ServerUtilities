package serverutils.lib.client;

import java.util.Locale;

public enum EnumSidebarButtonPlacement {
	DISABLED,
	TOP_LEFT,
	INVENTORY_SIDE,
	AUTO;

	public boolean top() {
		switch (this) {
			case TOP_LEFT:
				return true;
			case AUTO:
				return SidebarButton.NEI_NOT_LOADED.getAsBoolean();
			default:
				return false;
		}
	}

	public static EnumSidebarButtonPlacement string2placement(String placement) {
		switch (placement.toLowerCase(Locale.ENGLISH)) {
			case "disabled":
				return DISABLED;
			case "top_left":
				return TOP_LEFT;
			case "inventory_side":
				return INVENTORY_SIDE;
			default:
				return AUTO;
		}
	}
}
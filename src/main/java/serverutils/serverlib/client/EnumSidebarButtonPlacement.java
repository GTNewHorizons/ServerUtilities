package serverutils.serverlib.client;

import serverutils.serverlib.lib.OtherMods;
import cpw.mods.fml.common.Loader;

public enum EnumSidebarButtonPlacement
{
	DISABLED,
	TOP_LEFT,
	INVENTORY_SIDE,
	AUTO;

	public boolean top()
	{
		switch (this)
		{
			case TOP_LEFT:
				return true;
			case AUTO:
				return !Loader.isModLoaded(OtherMods.NEI);
			default:
				return false;
		}
	}
}
package serverutils.serverlib.lib.data;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.serverlib.lib.icon.Icon;

public abstract class AdminPanelAction extends Action {

	public AdminPanelAction(String mod, String id, Icon icon, int order) {
		super(
				new ResourceLocation(mod, id),
				new ChatComponentTranslation("admin_panel." + mod + "." + id),
				icon,
				order);
	}

	@Override
	public AdminPanelAction setTitle(IChatComponent t) {
		super.setTitle(t);
		return this;
	}

	@Override
	public AdminPanelAction setRequiresConfirm() {
		super.setRequiresConfirm();
		return this;
	}
}

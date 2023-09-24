package serverutils.serverlib.lib.data;

import serverutils.serverlib.lib.icon.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @author LatvianModder
 */
public abstract class AdminPanelAction extends Action
{
	public AdminPanelAction(String mod, String id, Icon icon, int order)
	{
		super(new ResourceLocation(mod, id), new TextComponentTranslation("admin_panel." + mod + "." + id), icon, order);
	}

	@Override
	public AdminPanelAction setTitle(ITextComponent t)
	{
		super.setTitle(t);
		return this;
	}

	@Override
	public AdminPanelAction setRequiresConfirm()
	{
		super.setRequiresConfirm();
		return this;
	}
}
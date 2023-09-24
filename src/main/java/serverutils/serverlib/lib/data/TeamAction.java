package serverutils.serverlib.lib.data;

import serverutils.serverlib.lib.icon.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * @author LatvianModder
 */
public abstract class TeamAction extends Action
{
	public TeamAction(String mod, String id, Icon icon, int order)
	{
		super(new ResourceLocation(mod, id), new TextComponentTranslation("team_action." + mod + "." + id), icon, order);
	}

	@Override
	public TeamAction setTitle(ITextComponent t)
	{
		super.setTitle(t);
		return this;
	}

	@Override
	public TeamAction setRequiresConfirm()
	{
		super.setRequiresConfirm();
		return this;
	}
}
package serverutils.serverlib.lib.icon;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public abstract class IconWithParent extends Icon
{
	public final Icon parent;

	public IconWithParent(Icon i)
	{
		parent = i;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void bindTexture()
	{
		parent.bindTexture();
	}
}
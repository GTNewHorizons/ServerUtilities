package serverutils.serverlib.lib.config;

import serverutils.serverlib.lib.icon.Color4I;
import net.minecraftforge.common.config.Config;

/**
 * @author LatvianModder
 */
public class ConfigRGB
{
	@Config.RangeInt(min = 0, max = 255)
	@Config.LangKey("item.fireworksCharge.red")
	public int red;

	@Config.RangeInt(min = 0, max = 255)
	@Config.LangKey("item.fireworksCharge.green")
	public int green;

	@Config.RangeInt(min = 0, max = 255)
	@Config.LangKey("item.fireworksCharge.blue")
	public int blue;

	public ConfigRGB(int r, int g, int b)
	{
		red = r & 0xFF;
		green = g & 0xFF;
		blue = b & 0xFF;
	}

	public ConfigRGB(Color4I col)
	{
		red = col.redi();
		green = col.greeni();
		blue = col.bluei();
	}

	public Color4I createColor(int a)
	{
		return Color4I.rgba(red, green, blue, a);
	}

	public Color4I createColor()
	{
		return createColor(255);
	}
}
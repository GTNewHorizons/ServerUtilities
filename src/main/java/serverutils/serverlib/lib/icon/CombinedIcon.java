package serverutils.serverlib.lib.icon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CombinedIcon extends Icon
{
	public static Icon getCombined(Collection<Icon> icons)
	{
		List<Icon> list = new ArrayList<>(icons.size());

		for (Icon icon : icons)
		{
			if (!icon.isEmpty())
			{
				list.add(icon);
			}
		}

		if (list.isEmpty())
		{
			return EMPTY;
		}
		else if (list.size() == 1)
		{
			return list.get(0);
		}

		return new CombinedIcon(list);
	}

	public final List<Icon> list;

	CombinedIcon(Collection<Icon> icons)
	{
		list = new ArrayList<>(icons.size());

		for (Icon icon : icons)
		{
			if (!icon.isEmpty())
			{
				list.add(icon);
			}
		}
	}

	CombinedIcon(Icon o1, Icon o2)
	{
		list = new ArrayList<>(2);
		list.add(o1);
		list.add(o2);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw(int x, int y, int w, int h)
	{
		for (Icon icon : list)
		{
			icon.draw(x, y, w, h);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void drawStatic(int x, int y, int w, int h)
	{
		for (Icon icon : list)
		{
			icon.drawStatic(x, y, w, h);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw3D()
	{
		for (Icon icon : list)
		{
			icon.draw3D();
		}
	}

	@Override
	public JsonElement getJson()
	{
		JsonArray json = new JsonArray();

		for (Icon o : list)
		{
			json.add(o.getJson());
		}

		return json;
	}

	public int hashCode()
	{
		return list.hashCode();
	}

	public boolean equals(Object o)
	{
		return o == this || o instanceof CombinedIcon && list.equals(((CombinedIcon) o).list);
	}
}